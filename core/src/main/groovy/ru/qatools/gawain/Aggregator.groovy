package ru.qatools.gawain
import groovy.transform.CompileStatic
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
        repo.with(aggKey, {
            strategy.process(it, event)
        })
    }
}
