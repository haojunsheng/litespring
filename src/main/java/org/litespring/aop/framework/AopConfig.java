package org.litespring.aop.framework;

import org.litespring.aop.Advice;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interface to be implemented by classes that hold the configuration
 * of a factory of AOP proxies. This configuration includes the
 * Interceptors and other advice, and Advisors, and the proxied interfaces.
 *
 * <p>Any AOP proxy obtained from Spring can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * 承载生成代理对象所需要的必要信息，比如相关目标类、Advice、Advisor等
 * @since 13.03.2003
 */
public interface AopConfig {

    Class<?> getTargetClass();

    Object getTargetObject();
    /**
     * Are we proxying the full target class instead of specified interfaces?
     */
    boolean isProxyTargetClass();

    Class<?>[] getProxiedInterfaces();

    boolean isInterfaceProxied(Class<?> intf);

    List<Advice> getAdvices();

    /**
     * Add the given AOP Alliance Advice at the specified position in the advice chain.
     * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
     * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
     * method in this wrapped form.
     * <p>Note: The given advice will apply to all invocations on the proxy,
     * even to the {@code toString()} method! Use appropriate advice implementations
     * or specify appropriate pointcuts to apply to a narrower set of methods.
     *
     * @param advice advice to add at the specified position in the advice chain
     * @throws AopConfigException in case of invalid advice
     */
    void addAdvice(Advice advice);

    List<Advice> getAdvices(Method method/*,Class<?> targetClass*/);

    void setTargetObject(Object obj);
}