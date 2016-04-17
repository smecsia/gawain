package me.smecsia.gawain.builders

import groovy.transform.CompileStatic
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface BroadcastBuilder {

    Broadcaster build(String name, Gawain gawain, Opts opts)

}