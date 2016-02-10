package ru.qatools.gawain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
public class GawainJavaTest {

    @Test
    public void testSimpleRoute() throws Exception {
        final Gawain gawain = Gawain.run(r -> {
            r.processor("input", evt -> evt + "proc").to("all");

            r.aggregator("all", evt -> "all", (state, evt) -> {
                if (!state.keySet().contains("events")) {
                    state.put("events", new ArrayList<>());
                }
                ((List) state.get("events")).add(evt);
                return state;
            });
        });

        gawain.to("input", "event1");
        gawain.to("input", "event2");
        gawain.to("input", "event3");

        await().atMost(2, SECONDS).until(() -> gawain.repo("all").keys(), hasItem("all"));
        await().atMost(2, SECONDS).until(() -> ((Collection<String>) gawain.repo("all").
                get("all").get("events")), hasSize(3));
        Map state = gawain.repo("all").get("all");
        final Collection<String> events = (Collection<String>) state.get("events");
        assertThat(events, hasSize(3));
        assertThat(events, containsInAnyOrder("event1proc", "event2proc", "event3proc"));
    }
}
