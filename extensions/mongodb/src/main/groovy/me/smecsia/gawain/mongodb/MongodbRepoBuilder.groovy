package me.smecsia.gawain.mongodb

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository
import me.smecsia.gawain.builders.RepoBuilder

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class MongodbRepoBuilder extends AbstractMongoBuilder implements RepoBuilder {

    @Override
    Repository build(String name, Opts opts) {
        new MongodbRepo(mongoClient, dbName, name, opts)
    }
}
