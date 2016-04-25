package me.smecsia.gawain.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import me.smecsia.gawain.serialize.ToStringStateSerializer

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class JacksonStateSerializer implements ToStringStateSerializer {
    private final ObjectMapper objectMapper

    JacksonStateSerializer(ObjectMapperProvider provider = new ObjectMapperProvider()) {
        this.objectMapper = provider.provide()
    }

    @Override
    String serialize(Map notSerializedObject) {
        objectMapper.writeValueAsString(notSerializedObject)
    }

    @Override
    Map deserialize(String serializedObject) {
        objectMapper.readValue(serializedObject, Map)
    }
}
