package me.smecsia.gawain.mongodb

import me.smecsia.gawain.jackson.JacksonStateSerializer
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo

/**
 * @author Ilya Sadykov
 */
class JacksonSerializerTest {

    @Test
    public void testSerializeMap() throws Exception {
        def serializer = new JacksonStateSerializer()
        def json = serializer.serialize([
                name     : 'Vasya',
                timestamp: 100L
        ])
        assertThat(json, containsString('["java.util.LinkedHashMap",{'))
        def user = serializer.deserialize(json) as Map

        assertThat(user.name as String, equalTo('Vasya'))
        assertThat(user.timestamp as long, equalTo(100L))
    }

    @Test
    public void testDeserializeLong() throws Exception {
        def json = '''["java.util.LinkedHashMap", {
                        "timestamp":["java.lang.Long",{"$longValue": "100000"}],
                        "date":["java.util.Date", {"$longValue": "100"}],
                        "shortForm": 1000
                       }]'''
        def map = new JacksonStateSerializer().deserialize(json)
        assertThat(map.timestamp as long, equalTo(100000L))
        assertThat(map.shortForm as long, equalTo(1000L))
        assertThat(map.date as Date, equalTo(new Date(100L)))
    }
}
