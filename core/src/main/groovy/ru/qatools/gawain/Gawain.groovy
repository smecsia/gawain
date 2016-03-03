package ru.qatools.gawain

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.qatools.gawain.builders.*
import ru.qatools.gawain.error.UnknownProcessorException
import ru.qatools.gawain.impl.AggregationKeyImpl
import ru.qatools.gawain.impl.AggregationStrategyImpl
import ru.qatools.gawain.impl.FilterImpl
import ru.qatools.gawain.impl.ProcessingStrategyImpl
import ru.qatools.gawain.java.GawainRun
import ru.qatools.gawain.java.Router

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import static java.lang.System.currentTimeMillis
import static java.util.concurrent.Executors.newFixedThreadPool
import static ru.qatools.gawain.util.Util.opt

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Gawain<E> implements Router<E> {
    static final Logger LOGGER = LoggerFactory.getLogger(Gawain.class)
    static final Opts DEFAULT_OPTS = new Opts()
    public static final String DEFAULT_NAME = "router"
    String name
    private Map<String, Processor> processors = [:]
    private Map<String, Opts> opts = [:]
    private Map<String, Broadcaster> broadcasters = [:]
    private Repository timersRepo
    private QueueBuilder queueBuilder = new BasicQueueBuilder()
    private RepoBuilder repoBuilder = new BasicRepoBuilder()
    private BroadcastBuilder bcBuilder = new BasicBroadcastBuilder()
    private ThreadPoolBuilder threadPoolBuilder = new BasicThreadPoolBuilder()
    private Scheduler scheduler

    private Gawain() {

    }

    private init() {
        scheduler?.start()
        processors.values().each { p ->
            Integer tCount = opts(p.name).consumers
            LOGGER.info("[${name}][${p.name}] Starting ${tCount} consumers...")
            def tp = newFixedThreadPool(tCount)
            tCount.times { idx ->
                LOGGER.info("[${name}][${p.name}#${idx}] Starting consumer...")
                tp.submit { p.run("${idx}") }
            }
        }
    }

    // DSL

    def to(String name, E event) {
        opt(processors[name]).orElseThrow({
            new UnknownProcessorException("No processor with name '${name}' found for event ${event}")
        }).add(event)
    }

    def broadcast(String name, E event) {
        getOrCreateBroadcaster(name).broadcast(event)
    }

    def useBroadcastBuilder(BroadcastBuilder builder) {
        this.bcBuilder = builder
    }

    def useQueueBuilder(QueueBuilder builder) {
        this.queueBuilder = builder
    }

    def useRepoBuilder(RepoBuilder builder) {
        this.repoBuilder = builder
    }

    def useScheduler(Scheduler scheduler) {
        this.scheduler = scheduler
    }

    def useThreadPoolBuilder(BasicThreadPoolBuilder builder) {
        this.threadPoolBuilder = builder
    }

    Repository repo(String name) {
        processors[name] instanceof Aggregator ? ((Aggregator) processors[name]).repo : null
    }

    def doEvery(int frequency, TimeUnit unit, Runnable task, Opts opts = new Opts()) {
        doEvery(frequency, unit, { task.run() }, opts)
    }

    def doEvery(Map opts = [:], int frequency, TimeUnit unit, Closure task) {
        doEvery(frequency, unit, task, new Opts(opts))
    }

    def doEvery(int frequency, TimeUnit unit, Closure task, Opts opts) {
        scheduler = scheduler ?: new Scheduler(name, repoBuilder.build('__scheduler__', opts))
        scheduler.addJob(frequency, unit, task, opts)
    }

    Aggregator aggregator(String name, Filter filter, AggregationKey<E> key, AggregationStrategy<E> strategy, Opts opts = DEFAULT_OPTS) {
        this.broadcasters[name] = getOrCreateBroadcaster(name)
        this.opts[name] = opts
        this.processors[name] = new Aggregator(
                name: name,
                router: this,
                filter: filter,
                repo: repoBuilder.build(name, opts),
                queue: buildQueue(name, opts),
                executor: buildExecutor(opts),
                strategy: strategy,
                key: key
        )
    }

    Processor processor(String name, Filter filter, ProcessingStrategy<E> strategy, Opts opts = DEFAULT_OPTS) {
        this.broadcasters[name] = getOrCreateBroadcaster(name)
        this.opts[name] = opts
        this.processors[name] = new Processor(
                name: name,
                router: this,
                filter: filter,
                queue: buildQueue(name, opts),
                executor: buildExecutor(opts),
                strategy: strategy,
        )
    }

    Aggregator aggregator(String name, AggregationKey<E> key, AggregationStrategy<E> strategy, Opts opts = DEFAULT_OPTS) {
        aggregator(name, null, key, strategy, opts)
    }

    Aggregator aggregator(String name, AggregationKey<E> key, AggregationStrategy<E> strategy, Map opts) {
        aggregator(name, key, strategy, new Opts(opts))
    }

    Processor processor(String name, ProcessingStrategy<E> strategy, Opts opts = DEFAULT_OPTS) {
        processor(name, null, strategy, opts)
    }

    Processor processor(String name, Closure strategy, Opts opts = DEFAULT_OPTS) {
        processor(name, process(strategy), opts)
    }

    Processor processor(String name, Closure strategy, Map opts) {
        processor(name, strategy, new Opts(opts))
    }

    Processor processor(String name, ProcessingStrategy<E> strategy, Map opts) {
        processor(name, strategy, new Opts(opts))
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

    static <E> Gawain<E> run(String name, Closure strategy = {}) {
        def instance = new Gawain(name: name)
        instance.with(strategy)
        instance.init()
        instance
    }

    static <E> Gawain<E> run(Closure strategy = {}) {
        run(DEFAULT_NAME, strategy)
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
