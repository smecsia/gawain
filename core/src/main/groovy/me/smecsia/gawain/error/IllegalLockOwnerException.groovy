package me.smecsia.gawain.error
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class IllegalLockOwnerException extends GawainRuntimeException {
}
