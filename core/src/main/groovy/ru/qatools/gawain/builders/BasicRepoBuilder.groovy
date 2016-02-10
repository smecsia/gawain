package ru.qatools.gawain.builders

import ru.qatools.gawain.ConcurrentHashMapRepository
import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository

/**
 * @author Ilya Sadykov
 */
class BasicRepoBuilder implements RepoBuilder {

    @Override
    Repository build(String name, Opts opts) {
        new ConcurrentHashMapRepository(maxLockWaitSec: opts.maxLockWaitSec)
    }
}
