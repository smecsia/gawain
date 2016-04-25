package me.smecsia.gawain.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Date;

import static java.lang.Long.parseLong;

/**
 * @author Ilya Sadykov
 */
class GawainDeserializers extends Deserializers.Base {

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
        if (Long.class.isAssignableFrom(type.getRawClass()) || (type.isPrimitive() && Long.TYPE.equals(type.getRawClass()))) {
            return new MongoLongDeserializer();
        }
        if (type.getRawClass().equals(Date.class)) {
            return new MongoDateDeserializer();
        }
        return super.findBeanDeserializer(type, config, beanDesc);
    }

    private static class MongoLongDeserializer extends StdDeserializer<Long> {
        NumberDeserializers.LongDeserializer longDeserializer =
                new NumberDeserializers.LongDeserializer(Long.class, 0L);

        MongoLongDeserializer() {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                p.nextToken();
                long value = parseLong(p.nextTextValue());
                p.nextToken();
                return value;
            }
            return longDeserializer.deserialize(p, ctxt);
        }
    }

    private static class MongoDateDeserializer extends StdDeserializer<Date> {
        DateDeserializers.DateDeserializer dateDeserializer = new DateDeserializers.DateDeserializer();

        MongoDateDeserializer() {
            super(Date.class);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                p.nextToken();
                p.nextToken();
                long value = parseLong(p.getText());
                p.nextToken();
                return new Date(value);
            }
            return dateDeserializer.deserialize(p, ctxt);
        }
    }
}
