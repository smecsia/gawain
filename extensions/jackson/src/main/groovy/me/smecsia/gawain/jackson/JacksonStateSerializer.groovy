package me.smecsia.gawain.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import me.smecsia.gawain.serialize.ToJsonStateSerializer

import static me.smecsia.gawain.util.StringUtil.isEmpty
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class JacksonStateSerializer implements ToJsonStateSerializer {
    private final ObjectMapper objectMapper

    JacksonStateSerializer(GawainObjectMapperProvider provider = new GawainObjectMapperProvider()) {
        this.objectMapper = provider.provide()
    }

    @Override
    String serialize(Map notSerializedObject) {
        objectMapper.writeValueAsString(notSerializedObject)
    }

    @Override
    Map deserialize(String serializedObject) {
        isEmpty(serializedObject) ?  null : objectMapper.readValue(serializedObject, Map)
    }
}
