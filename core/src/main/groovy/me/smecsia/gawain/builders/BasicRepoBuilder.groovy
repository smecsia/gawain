package me.smecsia.gawain.builders

import groovy.transform.CompileStatic
import me.smecsia.gawain.ConcurrentHashMapRepository
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicRepoBuilder implements RepoBuilder {

    @Override
    Repository build(String name, Opts opts) {
        new ConcurrentHashMapRepository(maxLockWaitMs: opts.maxLockWaitMs)
    }
}
