package me.smecsia.gawain.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import me.smecsia.gawain.jackson.GawainJacksonDeserializers.GawainIterableDeserializer;

/**
 * @author Ilya Sadykov
 */
class GawainJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.addDeserializers(new GawainJacksonDeserializers());
        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType type,
                                                                    BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                return new GawainIterableDeserializer(deserializer);
            }

            @Override
            public JsonDeserializer<?> modifyCollectionLikeDeserializer(DeserializationConfig config, CollectionLikeType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                return new GawainIterableDeserializer(deserializer);
            }
        });
    }


}
