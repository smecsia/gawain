package ru.qatools.gawain

import org.junit.Test
import ru.qatools.gawain.builders.BasicRepoBuilder

import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
import static ru.qatools.gawain.Scheduler.LOCK_KEY

/**
 * @author Ilya Sadykov
 */
class SchedulerTest {
    @Test
    public void testStartMasterAndSlave() throws Exception {
        def repo = new BasicRepoBuilder().build('test', new Opts(maxLockWaitMs: 100))
        def schedulers = (0..1).collect { new Scheduler("${it}", repo, 500, 500) }
        def seq100ms = new AtomicInteger()
        def seq200ms = new AtomicInteger()
        schedulers.each {
            it.addJob(100, MILLISECONDS, { seq100ms.incrementAndGet() }, new Opts(global: true))
            it.addJob(200, MILLISECONDS, { seq200ms.incrementAndGet() })
            it.start()
            sleep(100)
        }
        sleep(1500)
        assertThat(seq100ms.get(), allOf(greaterThan(10), lessThanOrEqualTo(20)))
        assertThat(seq200ms.get(), allOf(greaterThan(10), lessThanOrEqualTo(20)))
        assertThat(schedulers.first().master, equalTo(true))
        schedulers.first().terminate()
        sleep(1000)
        assertThat(schedulers[0].master, equalTo(false))
        assertThat(schedulers[1].master, equalTo(true))
        assertThat(seq100ms.get(), allOf(greaterThanOrEqualTo(20), lessThanOrEqualTo(30)))
        assertThat(seq200ms.get(), allOf(greaterThanOrEqualTo(20), lessThanOrEqualTo(30)))
        repo.put(LOCK_KEY, [lastUpdated: 0])
        sleep(100)
        repo.put(LOCK_KEY, [lastUpdated: 0])
        sleep(900)
        assertThat(schedulers[1].master, equalTo(false))
        assertThat(schedulers[0].master, equalTo(true))
        assertThat(seq100ms.get(), allOf(greaterThanOrEqualTo(30), lessThanOrEqualTo(40)))
        assertThat(seq200ms.get(), allOf(greaterThanOrEqualTo(30), lessThanOrEqualTo(40)))
    }
}
