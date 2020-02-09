package org.litespring.beans.factory.support;

import org.litespring.beans.factory.config.RuntimeBeanReference;
import org.litespring.beans.factory.config.TypedStringValue;

/**
 * Helper class for use in bean factory implementations,
 * resolving values contained in bean definition objects
 * into the actual values applied to the target bean instance.
 * <p>
 * 用于把beanID生成相应的实例
 */
public class BeanDefinitionValueResolver {
    private final DefaultBeanFactory beanFactory;

    public BeanDefinitionValueResolver(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Given a PropertyValue, return a value, resolving any references to other
     * beans in the factory if necessary. The value could be:
     * <li>A BeanDefinition, which leads to the creation of a corresponding
     * new bean instance. Singleton flags and names of such "inner beans"
     * are always ignored: Inner beans are anonymous prototypes.
     * <li>A RuntimeBeanReference, which must be resolved.
     * <li>A ManagedList. This is a special collection that may contain
     * RuntimeBeanReferences or Collections that will need to be resolved.
     * <li>A ManagedSet. May also contain RuntimeBeanReferences or
     * Collections that will need to be resolved.
     * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
     * or Collection that will need to be resolved.
     * <li>An ordinary object or {@code null}, in which case it's left alone.
     *
     * @param value the value object to resolve
     * @return the resolved object
     */
    public Object resolveValueIfNecessary(Object value) {
        if (value instanceof RuntimeBeanReference) {
            RuntimeBeanReference ref = (RuntimeBeanReference) value;
            String refName = ref.getBeanName();
            Object bean = this.beanFactory.getBean(refName);
            return bean;
        } else if (value instanceof TypedStringValue) {
            return ((TypedStringValue) value).getValue();
        } else {
            throw new RuntimeException("the value " + value + " has not implemented");
        }
    }
}