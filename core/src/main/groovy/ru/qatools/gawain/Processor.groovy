package ru.qatools.gawain

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Processor {
    final Logger LOG = LoggerFactory.getLogger(this.class)
    Gawain router
    String name
    List<Closure<Object>> outputs = []
    GawainQueue queue
    ExecutorService executor
    Filter filter
    volatile boolean stopped = false
    private ProcessingStrategy strategy
    long terminationWaitMs = 100

    protected processNext(event) {
        strategy.process(event)
    }

    protected stop() {
        stopped = true
    }

    protected add(event) {
        queue.add(event)
    }

    protected run(String idx = "0") {
        while (!stopped) {
            def event = queue.take()
            LOG.debug("[{}][{}#{}] processing event {}", router.name, name, idx, event)
            try {
                if (!filter || filter.filter(event)) {
                    executor.submit {
                        try {
                            def outEvent = processNext(event)
                            outputs.each { it(outEvent) }
                        } catch (e) {
                            LOG.error("[{}][{}] failed to process event {}", router.name, name, event, e)
                        }
                    }
                } else {
                    LOG.debug("[{}][{}] event filtered: {}", router.name, name, event)
                }
            } catch (e) {
                LOG.error("[{}][{}] failed to filter/process event {}", router.name, name, event, e)
            }
        }
        executor.awaitTermination(terminationWaitMs, TimeUnit.MILLISECONDS)
    }

    public Processor to(String... names) {
        names.each { String name ->
            outputs << { event -> router.to(name, event) }
        }; this
    }

    public Processor broadcast(String... names) {
        names.each { String name ->
            outputs << { event -> router.broadcast(name, event) }
        }; this
    }
}