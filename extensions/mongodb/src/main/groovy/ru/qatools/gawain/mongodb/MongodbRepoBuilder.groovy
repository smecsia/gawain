package ru.qatools.gawain.mongodb

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository
import ru.qatools.gawain.builders.RepoBuilder

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
