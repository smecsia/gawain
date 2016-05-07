package me.smecsia.gawain.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver
import com.fasterxml.jackson.databind.type.TypeFactory
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class GawainTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

    GawainTypeResolverBuilder() {
        super(NON_FINAL)
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config,
                                              JavaType baseType, Collection<NamedType> subtypes) {
        return  new GawainAsPropertyTypeSerializer(
                new ClassNameIdResolver(baseType, TypeFactory.defaultInstance()), null,
                CLASS.getDefaultPropertyName());
    }
}
