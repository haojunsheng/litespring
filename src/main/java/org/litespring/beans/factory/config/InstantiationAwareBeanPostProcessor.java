package org.litespring.beans.factory.config;

import org.litespring.beans.BeansException;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible, or to derive from
 * {@link InstantiationAwareBeanPostProcessorAdapter} in order to be shielded
 * from extensions to this interface.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 * 对bean的实例化做一些操作
 * 先执行自身的两个实例方法，再执行父接口的初始化方法
 * <p>
 * 先实例化（本接口的方法），再初始化（父类接口的方法）
 * @since 1.2
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
     * The returned bean object may be a proxy to use instead of the target bean,
     * effectively suppressing default instantiation of the target bean.
     * <p>If a non-null object is returned by this method, the bean creation process
     * will be short-circuited. The only further processing applied is the
     * {@link #postProcessAfterInitialization} callback from the configured
     * {@link BeanPostProcessor BeanPostProcessors}.
     * <p>This callback will only be applied to bean definitions with a bean class.
     * In particular, it will not be applied to beans with a "factory-method".
     * <p>Post-processors may implement the extended
     * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
     * to predict the type of the bean object that they are going to return here.
     *
     * @param beanClass the class of the bean to be instantiated
     * @param beanName  the name of the bean
     * @return the bean object to expose instead of a default instance of the target bean,
     * or {@code null} to proceed with default instantiation
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.factory.support.AbstractBeanDefinition#hasBeanClass
     * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName
     * Bean实例化前调用
     * 返回的可能是一个代理，用来替换目标bean
     * <p>
     * 如果该方法返回一个不为null的对象，bean的创建过程就会断路，之后就只有BeanPostProcessor的
     * postProcessorAfterInitialization方法会被调用
     * <p>
     * 在调用doCreateBean之前调用
     * <p>
     * 该方法在对象实例化之前调用，可以在这里应用代理，来代替原来的Bean，
     * 如果这里bean被代替了，就只剩postProcessorAfterInitialization方法会被调用
     */
    Object beforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;

    /**
     * Perform operations after the bean has been instantiated, via a constructor or factory method,
     * but before Spring property population (from explicit properties or autowiring) occurs.
     * <p>This is the ideal callback for performing field injection on the given bean instance.
     * See Spring's own {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor}
     * for a typical example.
     *
     * @param bean     the bean instance created, with properties not having been set yet
     * @param beanName the name of the bean
     * @return {@code true} if properties should be set on the bean; {@code false}
     * if property population should be skipped. Normal implementations should return {@code true}.
     * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
     * instances being invoked on this bean instance.
     *                                                  Bean实例化之后调用
     *                                                  这时候Bean已经被实例化，但是属性都还没被设置
     */
    boolean afterInstantiation(Object bean, String beanName) throws BeansException;

    /**
     * Post-process the given property values before the factory applies them
     * to the given bean. Allows for checking whether all dependencies have been
     * satisfied, for example based on a "Required" annotation on bean property setters.
     * <p>Also allows for replacing the property values to apply, typically through
     * creating a new MutablePropertyValues instance based on the original PropertyValues,
     * adding or removing specific values.
     *
     * @param bean     the bean instance created, but whose properties have not yet been set
     * @param beanName the name of the bean
     * @return the actual property values to apply to to the given bean
     * (can be the passed-in PropertyValues instance), or {@code null}
     * to skip property population
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.MutablePropertyValues
     * 该方法可以对属性值进行修改，这个时候属性值还未被设置
     */
    void postProcessPropertyValues(Object bean, String beanName) throws BeansException;
}