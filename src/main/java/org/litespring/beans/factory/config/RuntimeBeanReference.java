package org.litespring.beans.factory.config;

/**
 * Immutable placeholder class used for a property value object when it's
 * a reference to another bean in the factory, to be resolved at runtime.
 *
 */
public class RuntimeBeanReference {
    private final String beanName;
    /**
     * Create a new RuntimeBeanReference to the given bean name,
     * without explicitly marking it as reference to a bean in
     * the parent factory.
     * @param beanName name of the target bean
     */
    public RuntimeBeanReference(String beanName) {
        this.beanName = beanName;
    }
    public String getBeanName() {
        return this.beanName;
    }
}