package me.smecsia.gawain.builders

import groovy.transform.CompileStatic
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository

/**
 * @author Ilya Sadykov
 */
@CompileStatic
trait RepoBuilder {
    abstract Repository build(String name, Opts opts)
}