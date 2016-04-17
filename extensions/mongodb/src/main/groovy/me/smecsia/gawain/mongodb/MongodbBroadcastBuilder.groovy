package me.smecsia.gawain.mongodb

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts
import me.smecsia.gawain.builders.BroadcastBuilder

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
