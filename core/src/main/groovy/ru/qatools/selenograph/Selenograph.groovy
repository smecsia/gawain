package ru.qatools.selenograph

import ru.qatools.gawain.Gawain

import static java.util.concurrent.TimeUnit.SECONDS
import static ru.qatools.gawain.Gawain.timePassedSince

/**
 * @author Ilya Sadykov
 */
class Selenograph {


    public static final long MAX_SESSION_AGE = 10000L
    public static final long MAX_HUB_AGE = 30000L

    public static void main(String[] args) {
        Gawain.run {
            processor("router", { event ->
                to(event.sessionId ? "sessions" : "hubs", event)
            })

            aggregator("hubs",
                    filter { hubHost },
                    key { "${hubHost}${hubPort}" },
                    aggregate { state, evt ->
                        state.address = "${evt.hubHost}:${evt.hubPort}"
                        state.browsers = evt.browsers
                        state.alive = true
                        state.timestamp = now()
                    })

            aggregator("sessions",
                    filter { sessionId },
                    key { "${hubHost}${hubPort}" },
                    aggregate { state, evt ->
                        state.with { (running, stopping) = [running ?: [:], stopping ?: [:]] }
                        if (evt.start) {
                            state.running[evt.sessionId] = now()
                        } else if (!state.running.remove(evt.sessionId)) {
                            state.stopping[evt.sessionId] = now()
                        }
                    })

            doEvery(30, SECONDS, {
                repo("sessions").withEach { k, session ->
                    (session.running + session.stopping)
                            .findAll { timePassedSince(MAX_SESSION_AGE, it.value) }
                            .keySet().each { sessionId ->
                        println "Removing expired session ${sessionId}..."
                        session.running.remove(sessionId)
                        session.stopping.remove(sessionId)
                    }
                }
                repo("hubs").withEach { k, hub ->
                    if (timePassedSince(MAX_HUB_AGE, hub.timestamp)) {
                        println "Hub ${hub.address} is down"
                        hub.alive = false
                    }
                }
            })
        }
    }
}
