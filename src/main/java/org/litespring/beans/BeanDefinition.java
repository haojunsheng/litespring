package org.litespring.beans;

import java.util.List;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * BeanDefinition中保存了我们的Bean信息，比如：
 * 这个Bean指向的是哪个类
 * 是否是单例的
 * 是否懒加载
 * 这个Bean依赖了哪些Bean等等。
 *
 * 定义了Bean的样子，添加了SCOPE_SINGLETON，SCOPE_PROTOTYPE和SCOPE_DEFAULT字段
 * 新增了2个判断方法，setScope和getScope方法
 */
public interface BeanDefinition {
    /**
     * Scope identifier for the standard singleton scope: "singleton".
     * <p>Note that extended bean factories might support further scopes.
     * @see #setScope
     */
    public static final String SCOPE_SINGLETON = "singleton";
    /**
     * Scope identifier for the standard prototype scope: "prototype".
     * <p>Note that extended bean factories might support further scopes.
     * @see #setScope
     */
    public static final String SCOPE_PROTOTYPE = "prototype";
    public static final String SCOPE_DEFAULT = "";

    /**
     * Return whether this a <b>Singleton</b>, with a single, shared instance
     * returned on all calls.
     * @see #SCOPE_SINGLETON
     */
    public boolean isSingleton();
    /**
     * Return whether this a <b>Prototype</b>, with an independent instance
     * returned for each call.
     * @see #SCOPE_PROTOTYPE
     */
    public boolean isPrototype();
    String getScope();
    void setScope(String scope);
    /**
     * Return the current bean class name of this bean definition.
     * <p>Note that this does not have to be the actual class name used at runtime, in
     * case of a child definition overriding/inheriting the class name from its parent.
     * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
     * rather only use it for parsing purposes at the individual bean definition level.
     */
    String getBeanClassName();
    /**
     * Return the property values to be applied to a new instance of the bean.
     * <p>The returned instance can be modified during bean factory post-processing.
     * @return the MutablePropertyValues object (never {@code null})
     * Bean中的属性值
     */
    List<PropertyValue> getPropertyValues();
    /**
     * Return the constructor argument values for this bean.
     * <p>The returned instance can be modified during bean factory post-processing.
     * @return the ConstructorArgumentValues object (never {@code null})
     * 构造器参数
     */
    ConstructorArgument getConstructorArgument();

    /**
     *
     * @return beanID
     */
    String getID();

    /**
     *
     * @return !this.constructorArgument.isEmpty();
     */
    boolean hasConstructorArgumentValues();

    /**
     * 生成bean的字节码
     * @param classLoader
     * @return
     * @throws ClassNotFoundException
     */
    Class<?> resolveBeanClass(ClassLoader classLoader) throws ClassNotFoundException;

    /**
     * 获取bean的字节码
     * @return
     * @throws IllegalStateException
     */
    Class<?> getBeanClass() throws IllegalStateException ;
    boolean hasBeanClass();

    /**
     *  是否是合成的
     * @return
     */
    boolean isSynthetic();
}