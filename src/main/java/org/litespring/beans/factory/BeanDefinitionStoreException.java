package org.litespring.beans.factory;

import org.litespring.beans.BeansException;

/**
 * Exception thrown when a BeanFactory encounters an invalid bean definition:
 * e.g. in case of incomplete or contradictory bean metadata.
 */
public class BeanDefinitionStoreException extends BeansException {

    public BeanDefinitionStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public BeanDefinitionStoreException(String msg) {
        super(msg);
    }
}