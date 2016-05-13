package me.smecsia.gawain

import me.smecsia.gawain.beans.User
import me.smecsia.gawain.builders.BasicThreadPoolBuilder
import me.smecsia.gawain.builders.QueueBuilder
import me.smecsia.gawain.builders.RepoBuilder
import me.smecsia.gawain.error.UnknownProcessorException
import me.smecsia.gawain.serialize.ToStringStateSerializer
import org.junit.Test
import org.mockito.ArgumentCaptor

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
            processor('input', { evt -> evt.processed = true; evt }).to('all')

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
        ['Petya', 'Vasya', 'Masha'].each { gawain.to('filter', it) }
        await().atMost(2, SECONDS).until({ gawain.repo('users').keys() }, containsInAnyOrder('Petyaproc', 'Mashaproc'))
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
    public void testCustomScheduler() throws Exception {
        def scheduler = mock(Scheduler)
        Gawain.run {
            useScheduler(scheduler)
            doEvery 300, MILLISECONDS, {}
            doEvery 100, SECONDS, {}
        }
        verify(scheduler).addJob(eq(300), eq(MILLISECONDS), any(Closure), any(Opts))
        verify(scheduler).addJob(eq(100), eq(SECONDS), any(Closure), any(Opts))
    }

    @Test
    public void testCustomRepoBuilder() throws Exception {
        def repo = mock(Repository)
        def gawain = Gawain.run {
            useRepoBuilder({ name, opts -> repo } as RepoBuilder)
            aggregator 'input', key { 'all' }, aggregate { state, evt -> state.evt = evt }
        }
        gawain.to('input', [id: 'all'])
        verify(repo, timeout(2000).times(1)).with(eq('all'), any(Repository.StateClosure))
    }

    @Test
    public void testCustomQueueBuilder() throws Exception {
        def queue = mock(GawainQueue)
        def consumer = mock(GawainQueueConsumer)
        when(queue.buildConsumer()).thenReturn(consumer)
        when(consumer.consume()).thenAnswer({ sleep(10000) })
        def gawain = Gawain.run {
            useQueueBuilder([build: { name, size -> queue }] as QueueBuilder)
            aggregator 'input', key { 'all' }, aggregate { state, evt -> state.evt = evt }
        }
        gawain.to('input', [id: 'all'])
        verify(queue, times(1)).add([id: 'all'])
    }

    @Test
    public void testCustomThreadpoolBuilder() throws Exception {
        def executor = mock(ExecutorService)
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
                println("Hello, ${user.name}, ${user.email}"); user
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
            processor('input', process { to('all', "${it}proc") }, consumers: 10, processors: 10)
            aggregator('all', key { 'all' }, aggregate { state, evt ->
                state.all = (state.all ?: []) << evt
            }, consumers: 10, processors: 10)
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

    @Test
    public void testChangeTypeOfEvent() throws Exception {
        def gawain = Gawain.run {
            processor('input', process { [name: it] }).to('second')
            processor('second', process { [object: it] }).to('all')
            aggregator('all', key { 'all' }, aggregate { s, e ->
                s.events = s.events ?: []
                s.events << e
            })
        }
        gawain.to('input', 'Vasya')
        gawain.to('input', 'Petya')
        gawain.to('input', 'Masha')
        await().atMost(2, SECONDS).until({ gawain.repo('all')['all'].events.size() }, equalTo(3))
        def state = gawain.repo('all')['all']
        assertThat(state.events.collect { it.object.name }, containsInAnyOrder('Vasya', 'Petya', 'Masha'))
    }

    @Test
    public void testWithoutAggStrategy() throws Exception {
        def gawain = Gawain.run {
            processor('input').to('storage')
            aggregator('storage', consumers: 10)
        }
        gawain.to('input', 'Vasya')
        gawain.to('input', 'Petya')
        gawain.to('input', 'Masha')
        await().atMost(2, SECONDS).until({ gawain.repo('storage').keys() }, hasSize(3))
        assertThat(gawain.repo('storage').keys(), containsInAnyOrder('Vasya', 'Petya', 'Masha'))
    }

    @Test
    public void testRouterByType() throws Exception {
        def gawain = Gawain.run {
            processor 'router', { evt ->
                switch (evt) {
                    case String: to('strings', evt); break;
                    case Integer: to('integers', evt); break;
                    default: to('trash', evt); break;
                }
            }
            aggregator 'strings'
            aggregator 'integers'
            aggregator 'trash'
        }
        gawain.to('router', 'String1')
        gawain.to('router', 5)
        gawain.to('router', true)
        gawain.to('router', 'String2')
        await().atMost(2, SECONDS).until({ gawain.repo('strings').keys() }, hasSize(2))
        await().atMost(2, SECONDS).until({ gawain.repo('integers').keys() }, hasSize(1))
        await().atMost(2, SECONDS).until({ gawain.repo('trash').keys() }, hasSize(1))
        assertThat(gawain.repo('strings').keys(), containsInAnyOrder('String1', 'String2'))

    }

    @Test
    public void testFailOnMissingQueueFalse() throws Exception {
        def gawain = Gawain.run {
            failOnMissingQueue(false)
            processor 'existing'
        }
        gawain.to('not-existing', 'message')
    }

    @Test(expected = UnknownProcessorException)
    public void testFailOnMissingQueue() throws Exception {
        def gawain = Gawain.run {
            processor 'existing'
        }
        gawain.to('not-existing', 'message')
    }

    @Test
    public void testOverrideSerializer() throws Exception {
        def serializer = mock(ToStringStateSerializer)
        def repoBuilder = mock(RepoBuilder)
        def opts = new Opts(stateSerializer: serializer, maxQueueSize: 10)
        Gawain.run {
            defaultOpts(opts)
            useRepoBuilder(repoBuilder)
            aggregator 'input', key { 'all' }, aggregate { s, e ->
                println("got event ${e}")
            }
        }
        ArgumentCaptor<Opts> captor = new ArgumentCaptor<>()
        verify(repoBuilder).build(eq('input'), captor.capture())
        assertThat(captor.getValue().stateSerializer, is(serializer))
    }
}
