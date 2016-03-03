package ru.qatools.gawain.impl

import org.junit.Test
import ru.qatools.gawain.Serializer

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
class FSTSerializerTest {

    @Test
    public void testSerializeDeserialize() throws Exception {
        Serializer<Map> serializer = new FSTSerializer()
        def bytes = serializer.toBytes([name: 'Vasya'])
        assertThat(serializer.fromBytes(bytes), equalTo([name: 'Vasya'] as Map))
    }
}
