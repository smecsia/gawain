package me.smecsia.gawain.elasticsearch.util

import groovy.transform.CompileStatic
import org.elasticsearch.Version
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.Node
import org.elasticsearch.plugins.Plugin

import static org.elasticsearch.node.internal.InternalSettingsPreparer.prepareEnvironment
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class EmbeddedESNode extends Node {
    public EmbeddedESNode(Settings preparedSettings, List<Class<? extends Plugin>> plugins = []) {
        super(prepareEnvironment(preparedSettings, null), Version.CURRENT, plugins);
    }
}
