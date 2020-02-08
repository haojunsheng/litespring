package org.litespring.beans;

/**
 * Abstract superclass for all exceptions thrown in the beans package
 * and subpackages.
 *
 */
public class BeansException extends RuntimeException {
    public BeansException(String msg) {
        super(msg);	}

    public BeansException(String msg, Throwable cause) {
        super(msg, cause);
    }
}