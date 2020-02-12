package org.litespring.aop.framework;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Spring's implementation of the AOP Alliance
 * {@link org.aopalliance.intercept.MethodInvocation} interface,
 * implementing the extended
 * {@link org.springframework.aop.ProxyMethodInvocation} interface.
 *
 * <p>Invokes the target object using reflection. Subclasses can override the
 * {@link #invokeJoinpoint()} method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 *
 * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
 * repeatedly (once per clone), using the {@link #invocableClone()} method.
 * It is also possible to attach custom attributes to the invocation,
 * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
 *
 * <p><b>NOTE:</b> This class is considered internal and should not be
 * directly accessed. The sole reason for it being public is compatibility
 * with existing framework integrations (e.g. Pitchfork). For any other
 * purposes, use the {@link ProxyMethodInvocation} interface instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
public class ReflectiveMethodInvocation implements MethodInvocation {
    protected final Object targetObject; //petStoreService

    protected final Method targetMethod; //placeOrder方法

    protected Object[] arguments;

    /**
     * List of MethodInterceptor
     */
    protected final List<MethodInterceptor> interceptors;

    /**
     * Index from 0 of the current interceptor we're invoking.
     * -1 until we invoke: then the current interceptor.
     */
    private int currentInterceptorIndex = -1;
    /**
     * Construct a new ReflectiveMethodInvocation with the given arguments.
     * @param target the target object to invoke
     * @param method the method to invoke
     * @param arguments the arguments to invoke the method with
     * @param interceptors interceptors that should be applied,
     * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
     * MethodMatchers included in this struct must already have been found to have matched
     * as far as was possibly statically. Passing an array might be about 10% faster,
     * but would complicate the code. And it would work only for static pointcuts.
     */
    public ReflectiveMethodInvocation(
            Object target, Method method, Object[] arguments,
            List<MethodInterceptor> interceptors) {
        this.targetObject = target;
        this.targetMethod = method;
        this.arguments = arguments;
        this.interceptors = interceptors;
    }

    public final Object getThis() {
        return this.targetObject;
    }

    /**
     * Return the method invoked on the proxied interface.
     * May or may not correspond with a method invoked on an underlying
     * implementation of that interface.
     */
    public final Method getMethod() {
        return this.targetMethod;
    }

    public final Object[] getArguments() {
        return (this.arguments != null ? this.arguments : new Object[0]);
    }

    public Object proceed() throws Throwable {
        //	所有的拦截器已经调用完成
        if (this.currentInterceptorIndex == this.interceptors.size() - 1) {
            return invokeJoinpoint();
        }

        this.currentInterceptorIndex++;

        MethodInterceptor interceptor =
                this.interceptors.get(this.currentInterceptorIndex);

        return interceptor.invoke(this);
    }

    /**
     * Invoke the joinpoint using reflection.
     * Subclasses can override this to use custom invocation.
     *
     * @return the return value of the joinpoint
     * @throws Throwable if invoking the joinpoint resulted in an exception
     */
    protected Object invokeJoinpoint() throws Throwable {
        return this.targetMethod.invoke(this.targetObject, this.arguments);
    }

    public AccessibleObject getStaticPart() {
        return this.targetMethod;
    }
}
