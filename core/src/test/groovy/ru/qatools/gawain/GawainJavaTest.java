package ru.qatools.gawain;

import org.junit.Test;

import java.util.*;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.qatools.gawain.Gawain.*;
import static ru.qatools.gawain.Opts.GLOBAL;
import static ru.qatools.gawain.Opts.opts;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
public class GawainJavaTest {

    @Test
    public void testSimpleRoute() throws Exception {
        final Gawain gawain = Gawain.run(r -> {
            r.processor("input",
                    filter(evt -> !"event3".equals(evt)),
                    process((evt) -> evt + "proc")
            ).to("all");

            r.aggregator("all", key(evt -> "all"),
                    (state, evt) -> {
                        if (!state.keySet().contains("events")) {
                            state.put("events", new ArrayList<>());
                            state.put("timer", 0);
                        }
                        ((List) state.get("events")).add(evt);
                    }
            );

            r.doEvery(300, MILLISECONDS, () ->
                    r.repo("all").withEach((key, state) ->
                            state.put("timer", (int) state.get("timer") + 1)), opts(GLOBAL, true)
            );
        });

        gawain.to("input", "event1");
        gawain.to("input", "event2");
        gawain.to("input", "event3");
        gawain.to("input", "event4");

        await().atMost(2, SECONDS).until(() -> gawain.repo("all").keys(), hasItem("all"));
        await().atMost(2, SECONDS).until(() -> ((Collection<String>) gawain.repo("all").
                get("all").get("events")), hasSize(3));
        Map state = gawain.repo("all").get("all");
        final Collection<String> events = (Collection<String>) state.get("events");
        assertThat(events, hasSize(3));
        assertThat(events, containsInAnyOrder("event1proc", "event2proc", "event4proc"));
        sleep(600);
        state = gawain.repo("all").get("all");
        assertThat((int) state.get("timer"), greaterThanOrEqualTo(2));
    }

    @Test
    public void testChangeEventType() throws Exception {
        final List<User> users = new ArrayList<>();
        final Gawain gawain = Gawain.run(r -> {
            r.processor("male",
                    filter(evt -> !"Johnson".equals(evt)),
                    process(evt -> new String[]{"Mr. " + evt})).to("users");
            r.processor("female", process(evt -> new String[]{"Mrs. " + evt})).to("users");
            r.processor("users", process(evt -> {
                return new User(((String[]) evt)[0]);
            })).to("output");
            r.processor("output", process(evt -> users.add((User) evt)));
        });

        gawain.to("male", "Johnson");
        gawain.to("male", "Ivanov");
        gawain.to("male", "Petrov");
        gawain.to("female", "Malina");

        await().atMost(2, SECONDS).until(users::size, equalTo(3));
        assertThat(users, containsInAnyOrder(
                asList("Mr. Ivanov", "Mr. Petrov", "Mrs. Malina").stream()
                        .map(User::new).collect(toList()).toArray()
        ));
    }


    @Test
    public void testDoAggregation() throws Exception {
        List<Integer> events = rangeClosed(1, 100000)
                .map(i -> new Random().nextInt(100))
                .boxed().collect(toList());
        Map<String, Map> res = Gawain.doAggregation(events,
                key(String::valueOf),
                aggregate((AggregationStrategy<Integer>) (state, event) ->
                        state.put("count", state.get("count") != null ? (int) state.get("count") + 1 : 1)),
                opts("consumers", 100, "processors", 1, "benchmark", true));
        res.entrySet().forEach(e ->
                System.out.println(e.getKey() + ": " + e.getValue().get("count"))
        );

    }

    public static class User {
        final String name;

        public User(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            User user = (User) o;

            return !(name != null ? !name.equals(user.name) : user.name != null);

        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
