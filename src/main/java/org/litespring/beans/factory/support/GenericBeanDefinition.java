package org.litespring.beans.factory.support;

import org.litespring.beans.BeanDefinition;

/**
 * GenericBeanDefinition is a one-stop shop for standard bean definition purposes.
 * Like any bean definition, it allows for specifying a class plus optionally
 * constructor argument values and property values. Additionally, deriving from a
 * parent bean definition can be flexibly configured through the "parentName" property.
 *
 * <p>In general, use this {@code GenericBeanDefinition} class for the purpose of
 * registering user-visible bean definitions (which a post-processor might operate on,
 * potentially even reconfiguring the parent name). Use {@code RootBeanDefinition} /
 * {@code ChildBeanDefinition} where parent/child relationships happen to be pre-determined.
 *
 */
public class GenericBeanDefinition implements BeanDefinition {
    private String beanID;
    private String beanClassName;
    public GenericBeanDefinition(String beanID, String beanClassName) {

        this.beanID = beanID;
        this.beanClassName = beanClassName;
    }
    public String getBeanClassName() {

        return this.beanClassName;
    }
}