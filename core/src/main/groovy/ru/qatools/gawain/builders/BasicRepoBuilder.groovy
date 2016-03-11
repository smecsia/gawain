package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.ConcurrentHashMapRepository
import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository

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
