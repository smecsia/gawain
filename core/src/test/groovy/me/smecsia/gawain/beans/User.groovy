package me.smecsia.gawain.beans

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@Canonical
class User {
    String name, email
}
