package org.litespring.beans.factory.support;

import org.litespring.beans.BeanDefinition;
import org.litespring.beans.ConstructorArgument;
import org.litespring.beans.PropertyValue;

import java.util.ArrayList;
import java.util.List;

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
 */
public class GenericBeanDefinition implements BeanDefinition {
    private String id;
    private String beanClassName;
    private boolean singleton = true;
    private boolean prototype = false;
    private String scope = SCOPE_DEFAULT;
    private ConstructorArgument constructorArgument = new ConstructorArgument();
    List<PropertyValue> propertyValues = new ArrayList<PropertyValue>();
    private Class<?> beanClass;
    //表明这个Bean定义是不是我们litespring自己合成的。
    private boolean isSynthetic = false;

    public GenericBeanDefinition() {

    }

    public GenericBeanDefinition(String id, String beanClassName) {
        this.id = id;
        this.beanClassName = beanClassName;
    }

    public GenericBeanDefinition(Class<?> clz) {
        this.beanClass = clz;
        this.beanClassName = clz.getName();
    }

    public boolean isSynthetic() {
        return isSynthetic;
    }

    public void setSynthetic(boolean isSynthetic) {
        this.isSynthetic = isSynthetic;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public boolean isPrototype() {
        return this.prototype;
    }

    public String getScope() {
        return this.scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
        this.singleton = SCOPE_SINGLETON.equals(scope) || SCOPE_DEFAULT.equals(scope);
        this.prototype = SCOPE_PROTOTYPE.equals(scope);
    }

    public String getBeanClassName() {

        return this.beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public List<PropertyValue> getPropertyValues() {
        return this.propertyValues;
    }

    public ConstructorArgument getConstructorArgument() {
        return this.constructorArgument;
    }

    public String getID() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasConstructorArgumentValues() {
        return !this.constructorArgument.isEmpty();
    }

    /**
     * 生成bean的字节码
     * @param classLoader
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> resolveBeanClass(ClassLoader classLoader) throws ClassNotFoundException {
        String className = getBeanClassName();
        if (className == null) {
            return null;
        }
        Class<?> resolvedClass = classLoader.loadClass(className);
        this.beanClass = resolvedClass;
        return resolvedClass;
    }

    /**
     * 获取beanClass
     * @return
     * @throws IllegalStateException
     */
    public Class<?> getBeanClass() throws IllegalStateException {
        if (this.beanClass == null) {
            throw new IllegalStateException(
                    "Bean class name [" + this.getBeanClassName() + "] has not been resolved into an actual Class");
        }
        return this.beanClass;
    }

    public boolean hasBeanClass() {
        return this.beanClass != null;
    }
}