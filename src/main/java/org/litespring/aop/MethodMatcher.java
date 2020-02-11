package org.litespring.aop;


import java.lang.reflect.Method;

/**
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * <p>A MethodMatcher may be evaluated <b>statically</b> or at <b>runtime</b> (dynamically).
 * Static matching involves method and (possibly) method attributes. Dynamic matching
 * also makes arguments for a particular call available, and any effects of running
 * previous advice applying to the joinpoint.
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 3-arg
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method will never be invoked.
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(java.lang.reflect.Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])}
 * method will be invoked <i>immediately before each potential execution of the related advice</i>,
 * to decide whether the advice should run. All previous advice, such as earlier interceptors
 * in an interceptor chain, will have run, so any state changes they have produced in
 * parameters or ThreadLocal state will be available at the time of evaluation.
 *
 * @author Rod Johnson
 * @see Pointcut
 * @see ClassFilter
 * <p>
 * 该接口定义了静态方法匹配器和动态方法匹配器。
 * 静态方法匹配器：仅对方法签名（包括方法名和入参类型及顺序）进行匹配。
 * 动态方法匹配器：在运行期检查方法入参的值。
 * 静态方法匹配器仅会判别一次，动态方法匹配器因为每次调用方法的入参都可能不一样，所以每次都需要判断。
 * 动态方法匹配不常用。
 * 方法匹配器类型由isRuntime()返回值决定，返回false表示是静态方法匹配器；true是动态方法匹配器。
 * @since 11.11.2003
 */
public interface MethodMatcher {
    /**
     * Perform static checking whether the given method matches. If this
     * returns {@code false} or if the {@link #isRuntime()} method
     * returns {@code false}, no runtime check (i.e. no.
     * {@link #matches(java.lang.reflect.Method, Class, Object[])} call) will be made.
     *
     * @param method      the candidate method
     * @param targetClass the target class (may be {@code null}, in which case
     *                    the candidate class must be taken to be the method's declaring class)
     * @return whether or not this method matches statically
     */
    boolean matches(Method method/*, Class<?> targetClass*/);
}