package ru.qatools.gawain

import groovy.transform.CompileStatic
import ru.qatools.gawain.error.LockWaitTimeoutException

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Aggregator extends Processor {
    AggregationStrategy strategy
    AggregationKey key
    Repository repo

    @Override
    def processNext(event) {
        def aggKey = key.calculate(event)
        try {
            repo.with(aggKey, { String key, Map state ->
                strategy.process(state, event)
            })
        } catch (LockWaitTimeoutException e) {
            LOG.error("[${router.name}][${name}] Failed to aquire lock for ${key} " +
                    " within timeout during processing event ${event}. Forcing unlock for key!")
            repo.unlock(aggKey)
            router.to(name, event)
        }
    }
}
