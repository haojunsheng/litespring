package org.litespring.aop.aspectj;

import java.lang.reflect.Method;

import org.litespring.aop.Advice;
import org.litespring.aop.Pointcut;
import org.litespring.aop.config.AspectInstanceFactory;

/**
 * Base class for AOP Alliance {@link org.aopalliance.aop.Advice} classes
 * wrapping an AspectJ aspect or an AspectJ-annotated advice method.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
public abstract class AbstractAspectJAdvice implements Advice {
    protected Method adviceMethod;
    protected AspectJExpressionPointcut pointcut;
    protected AspectInstanceFactory adviceObjectFactory;

    /**
     * Create a new AbstractAspectJAdvice for the given advice method.
     *
     * @param adviceMethod the AspectJ-style advice method
     * @param pointcut     the AspectJ expression pointcut
     * @param adviceObjectFactory the factory for aspect instances
     */
    public AbstractAspectJAdvice(Method adviceMethod,
                                 AspectJExpressionPointcut pointcut,
                                 AspectInstanceFactory adviceObjectFactory) {
        this.adviceMethod = adviceMethod;
        this.pointcut = pointcut;
        this.adviceObjectFactory = adviceObjectFactory;
    }

    /**
     * Invoke the advice method.
     *
     * @throws Throwable in case of invocation failure
     */
    public void invokeAdviceMethod() throws Throwable {

        adviceMethod.invoke(adviceObjectFactory);
    }

    /**
     * Overridden in around advice to return proceeding join point.
     */
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    public Method getAdviceMethod() {
        return adviceMethod;
    }
}