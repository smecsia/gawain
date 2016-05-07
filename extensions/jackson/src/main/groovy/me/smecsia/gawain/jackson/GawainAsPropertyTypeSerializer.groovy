package me.smecsia.gawain.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class GawainAsPropertyTypeSerializer extends AsPropertyTypeSerializer {

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen) throws IOException {
        writeTypePrefixForArray(value, jgen, value.getClass())
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        writeTypePrefixForCollection(typeId, jgen)
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen) throws IOException {
        jgen.writeEndArray()
        jgen.writeEndObject()
    }

    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
        writeTypePrefixForCollection(typeId, jgen)
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen) throws IOException {
        // scalar values don't require type prefix
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        // scalar values don't require type prefix
    }

    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen) throws IOException {
        // scalar values don't require type prefix
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
        // scalar values don't require type prefix
    }

    protected void writeTypePrefixForCollection(String typeId, JsonGenerator jgen) {
        if (typeId == null) {
            jgen.writeStartObject();
        } else if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
            jgen.writeStringField(_typePropertyName, typeId);
        }
        jgen.writeFieldName('@items')
        jgen.writeStartArray();
    }
}
