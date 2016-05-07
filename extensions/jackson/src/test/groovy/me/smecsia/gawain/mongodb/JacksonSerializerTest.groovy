package me.smecsia.gawain.mongodb

import groovy.transform.EqualsAndHashCode
import me.smecsia.gawain.jackson.JacksonStateSerializer
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class JacksonSerializerTest {

    @EqualsAndHashCode
    static class User {
        String name = 'Vasya'
    }

    @Test
    public void testSerializeMap() throws Exception {
        def serializer = new JacksonStateSerializer()
        def json = serializer.serialize([
                '@class'      : 'java.util.HashMap',
                name          : 'Vasya',
                timestamp     : 100L,
                date          : new Date(),
                subobject     : new User(),
                submap        : ['a': 10],
                sublist       : [[name: 'Petya'], [name: 'Dasha']],
                anotherSublist: ['aaa', 'bbb'],
                stringArray   : ['vasya'] as String[],
                intArray      : [10, 20] as int[],
        ])
        assertThat(json, containsString('{"@class":"java.util.LinkedHashMap",'))
        assertThat(json, containsString('{"@class":"java.util.ArrayList",'))
        def user = serializer.deserialize(json) as Map

        assertThat(user, instanceOf(LinkedHashMap))
        assertThat(user['@class'], equalTo('java.util.HashMap'))
        assertThat(user.name as String, equalTo('Vasya'))
        assertThat(user.timestamp as long, equalTo(100L))
        assertThat(user.subobject, equalTo(new User()))
        assertThat(user.sublist, equalTo([[name: 'Petya'], [name: 'Dasha']]))
        assertThat(user.submap, equalTo(['a': 10]))
        assertThat(user.anotherSublist, equalTo(['aaa', 'bbb']))
        assertThat(user.stringArray, instanceOf(String[].class))
        assertThat((user.stringArray as String[]).toList(), contains('vasya'))
        assertThat(user.intArray, instanceOf(int[].class))
        assertThat((user.intArray as int[]).toList(), contains(10, 20))
    }

    @Test
    public void testDeserializeLong() throws Exception {
        def json = '''{"@class":"java.util.LinkedHashMap",
                        "timestamp":{"@class": "java.lang.Long", "$longValue": "100000"},
                        "date":{"@class": "java.util.Date","$longValue": "100"},
                        "shortForm": 1000
                       }'''
        def map = new JacksonStateSerializer().deserialize(json)
        assertThat(map.timestamp as long, equalTo(100000L))
        assertThat(map.shortForm as long, equalTo(1000L))
        assertThat(map.date as Date, equalTo(new Date(100L)))
    }
}
