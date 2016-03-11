package ru.qatools.gawain

import org.hamcrest.Matcher
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static ru.qatools.gawain.Gawain.*
import static ru.qatools.gawain.Opts.opts
/**
 * @author Ilya Sadykov
 */
class GawainSingleTest {

    Collection<Integer> events = (0..100000).collect { new Random().nextInt(100) }

    @Test
    public void testSingleRunWithoutGawain() throws Exception {
        def results = [:]
        events.forEach { e ->
            if (!results.keySet().contains(e)) {
                results.put(e, [count: 0])
            }
            results[e].count++
        }
        println(results)
        assertThatTotalCount(results, equalTo(events.size()))
    }

    @Test
    public void testSingleRunWithResult() throws Exception {
        def results = doAggregation(
                events, key { it },
                aggregate { s, e -> s.count = (s.count ?: 0) + 1 },
                opts(consumers: 100, processors: 1, maxQueueSize: 1000000, benchmark: true)
        )
        println(results)
        assertThatTotalCount(results, equalTo(events.size()))
    }

    protected static void assertThatTotalCount(Map results, Matcher<Integer> matcher) {
        int total = 0
        results.each { k, v -> total += v.count }
        assertThat(total, matcher)
    }
}
