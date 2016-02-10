package ru.qatools.gawain.mongodb

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import ru.qatools.gawain.Broadcaster
import ru.qatools.gawain.Gawain
import ru.qatools.gawain.Opts
import ru.qatools.gawain.builders.BroadcastBuilder

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class MongodbBroadcastBuilder extends AbstractMongoBuilder implements BroadcastBuilder {

    @Override
    Broadcaster build(String consumer, Gawain router, Opts opts) {
        new MongodbBroadcaster(mongoClient, dbName, consumer, router, opts)
    }
}
