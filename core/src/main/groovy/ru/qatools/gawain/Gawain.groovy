package ru.qatools.gawain

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.qatools.gawain.builders.*
import ru.qatools.gawain.error.UnknownProcessorException
import ru.qatools.gawain.impl.*
import ru.qatools.gawain.java.GawainRun
import ru.qatools.gawain.java.Router

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import static java.lang.System.currentTimeMillis
import static ru.qatools.gawain.util.Util.opt

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Gawain<E> implements Router<E> {
    static final Logger LOGGER = LoggerFactory.getLogger(Gawain.class)
    static final Opts DEFAULT_OPTS = new Opts()
    public static final String DEFAULT_NAME = "router"
    final String name
    private volatile boolean started
    private Map<String, Processor> processors = [:]
    private Map<String, List<ExecutorService>> threadpools = [:]
    private Map<String, GawainQueue<E>> queues = [:]
    private Map<String, Opts> opts = [:]
    private Map<String, Broadcaster> broadcasters = [:]
    private QueueBuilder queueBuilder = new BasicQueueBuilder()
    private RepoBuilder repoBuilder = new BasicRepoBuilder()
    private BroadcastBuilder bcBuilder = new BasicBroadcastBuilder()
    private ThreadPoolBuilder threadPoolBuilder = new BasicThreadPoolBuilder()
    private Scheduler scheduler

    private Gawain(String name) {
        this.name = name
    }

    /**
     * Begin the main loop for consumers according to their configuration
     */
    @Override
    public synchronized void start() {
        if (!started) {
            LOGGER.info("[${name}] Starting router")
            scheduler?.start()
            processors.values().each { p ->
                Integer tCount = opts(p.name).consumers
                LOGGER.info("[${name}][${p.name}] Starting ${tCount} consumers...")
                ExecutorService tp = threadPoolBuilder.build(tCount)
                tCount.times { idx ->
                    LOGGER.info("[${name}][${p.name}#${idx}] Starting consumer...")
                    tp.submit {
                        p.run(queues[p.name].buildConsumer(), "${idx}")
                    }
                }
                threadpools[p.name] = (threadpools[p.name] ?: []) as List<ExecutorService>
                threadpools[p.name] << tp
            }
            started = true
        }
    }

    /**
     * Terminates the execution of all actively running processings
     */
    @Override
    public synchronized void stop() {
        if (started) {
            LOGGER.info("[${name}] Stopping router")
            scheduler?.terminate()
            threadpools.each { k, tps -> tps.each { it.shutdownNow() } }
            started = false
        }
    }

    // DSL

    public to(String name, E event) {
        opt(queues[name]).orElseThrow({
            new UnknownProcessorException("No processor with name '${name}' found for event ${event}")
        }).add(event)
    }

    public broadcast(String name, E event) {
        getOrCreateBroadcaster(name).broadcast(event)
    }

    public useBroadcastBuilder(BroadcastBuilder builder) {
        this.bcBuilder = builder
    }

    public useQueueBuilder(QueueBuilder builder) {
        this.queueBuilder = builder
    }

    public useRepoBuilder(RepoBuilder builder) {
        this.repoBuilder = builder
    }

    public useScheduler(Scheduler scheduler) {
        this.scheduler = scheduler
    }

    public useThreadPoolBuilder(BasicThreadPoolBuilder builder) {
        this.threadPoolBuilder = builder
    }

    public Repository repo(String name) {
        processors[name] instanceof Aggregator ? ((Aggregator) processors[name]).repo : null
    }

    public doEvery(int frequency, TimeUnit unit, Runnable task, Opts opts = new Opts()) {
        doEvery(frequency, unit, { task.run() }, opts)
    }

    public doEvery(Map opts = [:], int frequency, TimeUnit unit, Closure task) {
        doEvery(frequency, unit, task, new Opts(opts))
    }

    public doEvery(int frequency, TimeUnit unit, Closure task, Opts opts) {
        scheduler = scheduler ?: new SchedulerImpl(name, repoBuilder.build('__scheduler__', opts))
        scheduler.addJob(frequency, unit, task, opts)
    }

    public synchronized Aggregator aggregator(String name, Filter filter, AggregationKey<E> key,
                                              AggregationStrategy<E> strategy, Opts opts = DEFAULT_OPTS) {
        this.broadcasters[name] = getOrCreateBroadcaster(name)
        this.opts[name] = opts
        this.queues[name] = buildQueue(name, opts)
        this.processors[name] = new Aggregator(
                name: name,
                router: this,
                filter: filter,
                repo: repoBuilder.build(name, opts),
                executor: buildExecutor(opts),
                strategy: strategy,
                key: key
        )
    }

    public synchronized Processor processor(String name, Filter filter, ProcessingStrategy<E> strategy, Opts opts) {
        this.broadcasters[name] = getOrCreateBroadcaster(name)
        this.opts[name] = opts
        this.queues[name] = buildQueue(name, opts)
        this.processors[name] = new Processor(
                name: name,
                router: this,
                filter: filter,
                executor: buildExecutor(opts),
                strategy: strategy,
        )
    }

    public Aggregator aggregator(String name, AggregationKey<E> key = { String.valueOf(it) }, Opts opts) {
        aggregator(name, null, key, aggregate({ s, E e -> s['value'] = key.calculate(e) }), opts)
    }

    public Aggregator aggregator(Map opts = [:], String name, AggregationKey<E> key = { String.valueOf(it) }) {
        aggregator(name, key, aggregate({ s, E e -> s['value'] = key.calculate(e) }), new Opts(opts))
    }

    public Aggregator aggregator(String name, AggregationKey<E> key, AggregationStrategy<E> strategy, Opts opts) {
        aggregator(name, null, key, strategy, opts)
    }

    public Aggregator aggregator(Map opts = [:], String name, AggregationKey<E> key, AggregationStrategy<E> strategy) {
        aggregator(name, key, strategy, new Opts(opts))
    }

    public Processor processor(String name, Filter filter, ProcessingStrategy<E> strategy) {
        processor(name, filter, strategy, DEFAULT_OPTS)
    }

    public Processor processor(String name, ProcessingStrategy<E> strategy, Opts opts) {
        processor(name, null, strategy, opts)
    }

    public Processor processor(String name, Closure strategy, Opts opts) {
        processor(name, process(strategy), opts)
    }

    public Processor processor(Map opts = [:], String name, Closure strategy) {
        processor(name, strategy, new Opts(opts))
    }

    public Processor processor(Map opts = [:], String name, ProcessingStrategy<E> strategy) {
        processor(name, strategy, new Opts(opts))
    }

    public Processor processor(Map opts = [:], String name) {
        processor(name, { it } as ProcessingStrategy<E>, new Opts(opts))
    }

    // Protected

    protected Broadcaster getOrCreateBroadcaster(String target) {
        broadcasters[target] ?: (broadcasters[target] = bcBuilder.build(target, this, opts(target)))
    }

    protected Opts opts(String target) {
        opts[target] ?: DEFAULT_OPTS
    }

    protected ExecutorService buildExecutor(Opts opts) {
        threadPoolBuilder.build(opts.processors)
    }

    protected GawainQueue buildQueue(String name, Opts opts) {
        queueBuilder.build(name, opts.maxQueueSize)
    }

    // Static DSL

    static <E> Gawain<E> run(String name, Closure strategy = {}, boolean startConsumers = true) {
        def instance = new Gawain(name)
        instance.with(strategy)
        if (startConsumers) {
            instance.start()
        }
        instance
    }

    static <E> Gawain<E> run(Closure strategy = {}) {
        run(DEFAULT_NAME, strategy)
    }

    /**
     * Perform the aggregation of events according to the key & strategy
     * This method blocks until aggregation is finished for all events and then returns results
     */
    static <E> Map<String, Map> doAggregation(Collection events, AggregationKey<E> key,
                                              AggregationStrategy<E> strategy,
                                              Opts opts = DEFAULT_OPTS, Closure customize = {}) {
        def latch = new CountDownLatch(events.size())
        def router = run(DEFAULT_NAME, {
            def r = (it as Gawain<E>)
            it.with(customize)
            r.aggregator('input', key, strategy, opts).to('out')
            r.processor 'out', { latch.countDown() }
        }, opts['benchmark'] == null)
        events.each { router.to('input', it) }
        final long startedTime = currentTimeMillis()
        if (opts['benchmark']) {
            router.start()
        }
        latch.await()
        if (opts['benchmark']) {
            LOGGER.info("Aggregation has been finished in {}ms", currentTimeMillis() - startedTime)
        }
        router.stop()
        router.repo('input').values()
    }

    // for Java
    static <E> Map<String, Map> doAggregation(Collection events, AggregationKey<E> key,
                                              AggregationStrategy<E> strategy,
                                              Opts opts = DEFAULT_OPTS, GawainRun customize) {
        doAggregation(events, key, strategy, opts, { customize.run(it as Gawain<E>) })
    }

    static <E> Gawain<E> run(GawainRun strategy) {
        run(DEFAULT_NAME, { strategy.run(it as Gawain<E>) })
    }

    static AggregationStrategy aggregate(Closure strategy = {}) {
        new AggregationStrategyImpl(callback: strategy)
    }

    static ProcessingStrategy process(Closure strategy = {}) {
        new ProcessingStrategyImpl(callback: strategy)
    }

    static AggregationKey key(Closure callback = {}) {
        new AggregationKeyImpl(callback: callback)
    }

    static Filter filter(Closure callback = {}) {
        new FilterImpl(callback: callback)
    }

    // Syntax sugar for java

    static <E> ProcessingStrategy<E> process(ProcessingStrategy<E> strategy) {
        strategy
    }

    static <E> AggregationKey<E> key(AggregationKey<E> key) {
        key
    }

    static <E> AggregationStrategy<E> aggregate(AggregationStrategy<E> strategy) {
        strategy
    }

    static <E> Filter<E> filter(Filter<E> filter) {
        filter
    }

    // Other helpers etc

    static long now() {
        currentTimeMillis()
    }

    static boolean timePassedSince(long time, long timestamp) {
        now() - timestamp > time
    }

}
