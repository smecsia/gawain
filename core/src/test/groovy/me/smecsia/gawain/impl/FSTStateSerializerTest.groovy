package me.smecsia.gawain.impl

import me.smecsia.gawain.serialize.FSTStateSerializer
import org.junit.Test
import me.smecsia.gawain.serialize.Serializer

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
class FSTStateSerializerTest {

    @Test
    public void testSerializeDeserialize() throws Exception {
        Serializer<Map> serializer = new FSTStateSerializer()
        def bytes = serializer.serialize([name: 'Vasya'])
        assertThat(serializer.deserialize(bytes), equalTo([name: 'Vasya'] as Map))
    }
}
