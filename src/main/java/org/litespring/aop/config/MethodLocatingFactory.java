package org.litespring.aop.config;

import java.lang.reflect.Method;

import org.litespring.beans.BeanUtils;
import org.litespring.beans.factory.BeanFactory;
import org.litespring.util.StringUtils;

/**
 * 根据bean的名字和方法名，定位到Method.
 */
public class MethodLocatingFactory {

    private String targetBeanName;

    private String methodName;

    private Method method;

    /**
     * Set the name of the bean to locate the {@link Method} on.
     * <p>This property is required.
     *
     * @param targetBeanName the name of the bean to locate the {@link Method} on
     */
    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
    }

    /**
     * Set the name of the {@link Method} to locate.
     * <p>This property is required.
     *
     * @param methodName the name of the {@link Method} to locate
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        if (!StringUtils.hasText(this.targetBeanName)) {
            throw new IllegalArgumentException("Property 'targetBeanName' is required");
        }
        if (!StringUtils.hasText(this.methodName)) {
            throw new IllegalArgumentException("Property 'methodName' is required");
        }

        Class<?> beanClass = beanFactory.getType(this.targetBeanName);
        if (beanClass == null) {
            throw new IllegalArgumentException("Can't determine type of bean with name '" + this.targetBeanName + "'");
        }

        this.method = BeanUtils.resolveSignature(this.methodName, beanClass);

        if (this.method == null) {
            throw new IllegalArgumentException("Unable to locate method [" + this.methodName +
                    "] on bean [" + this.targetBeanName + "]");
        }
    }

    public Method getObject() throws Exception {
        return this.method;
    }
}