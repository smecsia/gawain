package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository

/**
 * @author Ilya Sadykov
 */
@CompileStatic
trait RepoBuilder {
    abstract Repository build(String name, Opts opts)
}