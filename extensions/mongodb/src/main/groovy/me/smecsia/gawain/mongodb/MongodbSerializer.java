package me.smecsia.gawain.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import me.smecsia.gawain.Opts;
import me.smecsia.gawain.serialize.ToBytesStateSerializer;
import me.smecsia.gawain.serialize.ToStringStateSerializer;
import org.bson.Document;
import org.bson.types.Binary;
import ru.qatools.mongodb.Deserializer;
import ru.qatools.mongodb.Serializer;
import ru.qatools.mongodb.util.SerializeUtil;

import java.util.List;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
public class MongodbSerializer implements Serializer, Deserializer {
    private static final String OBJECT_FIELD = "object";
    private final Serializer serializer;
    private final Deserializer deserializer;
    private final me.smecsia.gawain.serialize.Serializer gawainSerializer;

    MongodbSerializer(Opts opts) {
        gawainSerializer = opts.getStateSerializer();
        serializer = serialize();
        deserializer = deserialize();
    }

    @Override
    public BasicDBObject toDBObject(Object object) {
        return serializer.toDBObject(object);
    }

    @Override
    public <T> T fromDBObject(Document input, Class<T> expectedClass) throws Exception {
        return deserializer.fromDBObject(input, expectedClass);
    }

    private Serializer serialize() {
        if (gawainSerializer instanceof ToBytesStateSerializer) {
            return (object) -> new BasicDBObject(OBJECT_FIELD, gawainSerializer.serialize(object));
        } else if (gawainSerializer instanceof ToStringStateSerializer) {
            return (object) -> new BasicDBObject(OBJECT_FIELD, JSON.parse(
                    (String) gawainSerializer.serialize(object)));
        } else {
            return SerializeUtil::objectToBytes;
        }
    }

    private <T> Deserializer deserialize() {
        if (gawainSerializer instanceof ToBytesStateSerializer) {
            return this::objectFromBytes;
        } else if (gawainSerializer instanceof ToStringStateSerializer) {
            return this::objectFromString;
        }
        return SerializeUtil::objectFromBytes;
    }

    @SuppressWarnings("unchecked")
    private <T> T objectFromString(Document input, Class<T> expected)
            throws Exception { //NOSONAR
        Object object = input.get(OBJECT_FIELD);
        if (object instanceof List) {
            final BasicDBList list = new BasicDBList();
            list.addAll((List) object);
            return (T) gawainSerializer.deserialize(list.toString());
        }
        return (T) gawainSerializer.deserialize(input.toJson());
    }

    private <T> T objectFromBytes(Document input, Class<T> expectedClass) throws Exception {
        return (T) gawainSerializer.deserialize(((Binary) input.get(OBJECT_FIELD)).getData());
    }
}

