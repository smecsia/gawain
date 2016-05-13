package me.smecsia.gawain

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
/**
 * @author Ilya Sadykov
 */
@Slf4j('LOG')
@CompileStatic
class Processor {
    Gawain router
    String name
    List<Closure<Object>> outputs = []
    Filter filter
    volatile boolean stopped = false
    private ProcessingStrategy strategy

    protected processNext(event) {
        strategy.process(event)
    }

    protected stop() {
        stopped = true
    }

    protected run(GawainQueueConsumer consumer, String idx = "0") {
        while (!stopped) {
            def event = consumer.consume()
            LOG.debug("[{}][{}#{}] processing event {}", router.name, name, idx, event)
            try {
                if (!filter || filter.filter(event)) {
                    try {
                        def outEvent = processNext(event)
                        outputs.each { it(outEvent) }
                    } catch (e) {
                        LOG.error("[{}][{}] failed to process event {}", router.name, name, event, e)
                    }
                } else {
                    LOG.debug("[{}][{}] event filtered: {}", router.name, name, event)
                }
            } catch (e) {
                LOG.error("[{}][{}] failed to filter/process event {}", router.name, name, event, e)
            }
        }
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