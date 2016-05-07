package me.smecsia.gawain

import groovy.transform.CompileStatic
import org.junit.Test

import java.util.concurrent.atomic.AtomicInteger

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings('GroovyAssignabilityCheck')
@CompileStatic
class GawainCompileStaticTest {

    @Test
    public void testSimpleRoute() throws Exception {
        def gawain = Gawain.run {
            processor('input', { evt -> evt['processed'] = true; evt }).to('all')

            aggregator 'all', key { 'all' },
                    aggregate { state, evt ->
                        state['events'] = state['events'] ?: []
                        (state['events'] as List) << evt
                    }
        }

        gawain.to('input', [id: 'event1'])
        gawain.to('input', [id: 'event2'])
        gawain.to('input', [id: 'event3'])

        await().atMost(2, SECONDS).until({ gawain.repo('all').keys() }, hasItem('all'))

        def state = gawain.repo('all')['all']
        assertThat(state.events as List, hasSize(3))
        assertThat(state.events.collect({ it['processed'] }) as List<Boolean>, contains(true, true, true))
    }

    @Test
    public void testTimerSkipIfNotCompleted() throws Exception {
        AtomicInteger counter = new AtomicInteger(0)
        Gawain.run {
            doEvery(10, MILLISECONDS) {
                sleep(100)
                counter.incrementAndGet()
            }
        }
        sleep(300)
        assertThat(counter.get(), lessThan(4))

    }
}
