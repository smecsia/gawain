package ru.qatools.gawain

import org.junit.Test
import ru.qatools.gawain.beans.User
import ru.qatools.gawain.builders.BasicThreadPoolBuilder
import ru.qatools.gawain.builders.QueueBuilder
import ru.qatools.gawain.builders.RepoBuilder

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings('GroovyAssignabilityCheck')
class GawainTest {

    @Test
    public void testSimpleRoute() throws Exception {
        def gawain = Gawain.run {
            processor('input', { evt -> evt.processed = true }).to('all')

            aggregator 'all', key { 'all' },
                    aggregate { state, evt ->
                        state.events = state.events ?: []
                        state.events << evt
                    }
        }

        gawain.to('input', [id: 'event1'])
        gawain.to('input', [id: 'event2'])
        gawain.to('input', [id: 'event3'])

        await().atMost(2, SECONDS).until({ gawain.repo('all').keys() }, hasItem('all'))

        def state = gawain.repo('all')['all']
        assertThat(state.events as List, hasSize(3))
        assertThat(state.events.collect({ it.processed }), contains(true, true, true))
    }

    @Test
    public void testFilters() throws Exception {
        def gawain = Gawain.run {
            processor('filter', filter { it != 'Vasya' }, process { "${it}proc" }).to('users')
            aggregator 'users', key { it }, aggregate { state, evt -> state.name = evt }
        }
        ['Petya','Vasya','Masha'].each { gawain.to('filter', it) }
        await().atMost(2, SECONDS).until({ gawain.repo('users').keys() }, containsInAnyOrder('Petya', 'Masha'))
    }

    @Test
    public void testTimers() throws Exception {
        def gawain = Gawain.run {
            aggregator 'input', key { it.id },
                    aggregate { state, evt ->
                        state.ticks = 0
                        state.evt = evt
                    }
            doEvery(300, MILLISECONDS, {
                repo('input').withEach { key, state ->
                    state.ticks += 1
                }
            })
        }
        gawain.to('input', [id: 'hello'])
        gawain.to('input', [id: 'timers'])

        await().atMost(2, SECONDS).until({ gawain.repo('input').keys() }, containsInAnyOrder('hello', 'timers'))

        sleep(1000)
        def state1 = gawain.repo('input')['hello'] as Map
        def state2 = gawain.repo('input')['timers'] as Map
        assertThat(state1.ticks as int, greaterThanOrEqualTo(3))
        assertThat(state2.ticks as int, greaterThanOrEqualTo(3))
    }

    @Test
    public void testCustomRepoBuilder() throws Exception {
        def repo = mock(Repository.class)
        def gawain = Gawain.run {
            useRepoBuilder({ name, opts -> repo } as RepoBuilder)
            aggregator 'input', key { 'all' }, aggregate { state, evt -> state.evt = evt }
        }
        gawain.to('input', [id: 'all'])
        verify(repo, timeout(2000).times(1)).with(eq('all'), any(Repository.StateClosure))
    }

    @Test
    public void testCustomQueueBuilder() throws Exception {
        def queue = mock(GawainQueue.class)
        when(queue.take()).thenAnswer({ sleep(10000) })
        def gawain = Gawain.run {
            useQueueBuilder([build: { name, size -> queue }] as QueueBuilder)
            aggregator 'input', key { 'all' }, aggregate { state, evt -> state.evt = evt }
        }
        gawain.to('input', [id: 'all'])
        verify(queue, times(1)).add([id: 'all'])
    }

    @Test
    public void testCustomThreadpoolBuilder() throws Exception {
        def executor = mock(ExecutorService.class)
        def gawain = Gawain.run {
            useThreadPoolBuilder({ executor } as BasicThreadPoolBuilder)
            aggregator 'input', key { 'all' }, aggregate { state, evt -> state.evt = evt }
        }
        gawain.to('input', [id: 'all'])
        verify(executor, timeout(1000).times(1)).submit(any(Runnable) as Runnable)
    }

    @Test
    public void testBroadcaster() throws Exception {
        def gawain1 = Gawain.run("first") {
            processor 'input', process { broadcast('all', it) }
            aggregator 'all', key { 'all' }, aggregate { state, evt ->
                state.count = (state.count ?: 0) + 1
            }
        }
        def gawain2 = Gawain.run("second") {
            aggregator 'all', key { 'all' }, aggregate { state, evt ->
                state.count = (state.count ?: 0) + 1
            }
        }
        10.times { gawain1.to('input', [id: it]) }
        await().atMost(2, SECONDS).until({ gawain1.repo('all')['all'].count }, equalTo(10))
        assertThat(gawain2.repo('all').keys(), empty())
        5.times { gawain2.to('all', [id: it]) }
        await().atMost(2, SECONDS).until({ gawain2.repo('all')['all'].count }, equalTo(5))
    }

    @Test
    public void testCustomObjectAsEvent() throws Exception {
        def gawain = Gawain.run {
            processor('input', process { User user ->
                println("Hello, ${user.name}, ${user.email}")
            }).to('users')

            aggregator 'users', key { "${it.name}" }, aggregate { state, evt ->
                println("Collecting ${evt.name}")
                state.user = evt
            }
        }
        def vasya = new User(name: 'Vasya', email: 'vasya@mail.com')
        gawain.to('input', vasya)
        await().atMost(2, SECONDS).until({ gawain.repo('users').keys() }, hasItem('Vasya'))
        assertThat(gawain.repo('users')['Vasya'], equalTo([user: vasya]))
    }

    @Test
    public void testManyConsumers() throws Exception {
        def gawain = Gawain.run {
            processor('input', process { to('all', "${it}proc") }, [consumers: 10, processors: 10])
            aggregator('all', key { 'all' }, aggregate { state, evt ->
                state.all = (state.all ?: []) << evt
            }, [consumers: 10, processors: 10])
        }
        5.times { gawain.to('input', "event${it}") }
        await().atMost(2, SECONDS).until({ gawain.repo('all')['all'].all.size() }, equalTo(5))
        assertThat(gawain.repo('all')['all'].all, containsInAnyOrder(
                ((0..4 as List).collect { "event${it}proc" }).toArray()
        ))
    }

    @Test
    public void testMultipleSchedulers() throws Exception {
        def globalCounter = new AtomicInteger()
        def localCounter = new AtomicInteger()
        def repo = new ConcurrentHashMapRepository()
        def repoBuilder = { name, opts -> repo }
        Gawain.run("first") {
            useRepoBuilder(repoBuilder)
            doEvery(100, MILLISECONDS, { globalCounter.incrementAndGet() }, global: true)
            doEvery(100, MILLISECONDS, { localCounter.incrementAndGet() }, global: false)
        }
        Gawain.run("second") {
            useRepoBuilder(repoBuilder)
            doEvery(100, MILLISECONDS, { globalCounter.incrementAndGet() }, global: true)
            doEvery(100, MILLISECONDS, { localCounter.incrementAndGet() }, global: false)
        }
        sleep(1000)
        assertThat(globalCounter.get(), allOf(greaterThanOrEqualTo(9), lessThanOrEqualTo(11)))
        assertThat(localCounter.get(), allOf(greaterThanOrEqualTo(19), lessThanOrEqualTo(21)))
    }


}
