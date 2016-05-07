package me.smecsia.gawain.util

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class StringUtil {

    static boolean isEmpty(String string) {
        string == null || string.isEmpty()
    }
}
