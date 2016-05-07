package me.smecsia.gawain.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;
import static com.fasterxml.jackson.annotation.PropertyAccessor.GETTER;

/**
 * @author Ilya Sadykov
 */
public class GawainObjectMapperProvider {

    public ObjectMapper provide() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(FIELD, ANY);
        mapper.setVisibility(GETTER, NONE);
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new GawainJacksonModule());
        final GawainTypeResolverBuilder typer = new GawainTypeResolverBuilder();
        typer.init(JsonTypeInfo.Id.CLASS, null).inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);
        return mapper;
    }
}
