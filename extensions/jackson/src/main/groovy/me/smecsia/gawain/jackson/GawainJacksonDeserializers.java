package me.smecsia.gawain.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import jodd.util.StringUtil;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static java.lang.Long.parseLong;

/**
 * @author Ilya Sadykov
 */
class GawainJacksonDeserializers extends Deserializers.Base {

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                                                    DeserializationConfig config,
                                                    BeanDescription beanDesc) throws JsonMappingException {
        if (Long.class.isAssignableFrom(type.getRawClass()) || (type.isPrimitive() && Long.TYPE.equals(type.getRawClass()))) {
            return new GawainLongDeserializer();
        }
        if (type.getRawClass().equals(Date.class)) {
            return new GawainDateDeserializer();
        }
        return super.findBeanDeserializer(type, config, beanDesc);
    }

    @Override
    public JsonDeserializer<?> findArrayDeserializer(ArrayType type, DeserializationConfig config, BeanDescription beanDesc,
                                                     TypeDeserializer elementTypeDeserializer,
                                                     JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
        if (type.getContentType().getRawClass().equals(String.class)) {
            return new GawainIterableDeserializer(StringArrayDeserializer.instance);
        }
        if (type.getContentType().isPrimitive()) {
            return new GawainIterableDeserializer(PrimitiveArrayDeserializers.forType(type.getContentType().getRawClass()));
        }
        return super.findArrayDeserializer(type, config, beanDesc, elementTypeDeserializer, elementDeserializer);
    }

    static class GawainIterableDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {
        final JsonDeserializer<?> delegate;

        GawainIterableDeserializer(JsonDeserializer<?> delegate) {
            super(Object.class);
            this.delegate = delegate;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (StringUtil.equals(p.getText(), "@items")) {
                p.nextToken();
            }
            final Object result = delegate.deserialize(p, ctxt);
            p.nextToken();
            return result;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            return new GawainIterableDeserializer(((ContextualDeserializer) delegate).createContextual(ctxt, property));
        }
    }

    private static class GawainLongDeserializer extends StdDeserializer<Long> {
        NumberDeserializers.LongDeserializer longDeserializer =
                new NumberDeserializers.LongDeserializer(Long.class, 0L);

        GawainLongDeserializer() {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final boolean longToken = Objects.equals(p.getText(), "$longValue");
            if (longToken || p.hasToken(START_OBJECT)) {
                p.nextToken();
                long value = parseLong(longToken ? p.getText() : p.nextTextValue());
                p.nextToken();
                return value;
            }
            return longDeserializer.deserialize(p, ctxt);
        }
    }

    private static class GawainDateDeserializer extends StdDeserializer<Date> {
        DateDeserializers.DateDeserializer dateDeserializer = new DateDeserializers.DateDeserializer();

        GawainDateDeserializer() {
            super(Date.class);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final boolean longToken = Objects.equals(p.getText(), "$longValue");
            if (p.hasToken(START_OBJECT) || longToken) {
                p.nextToken();
                long value = parseLong(longToken ? p.getText() : p.nextTextValue());
                p.nextToken();
                return new Date(value);
            }
            return dateDeserializer.deserialize(p, ctxt);
        }
    }
}
