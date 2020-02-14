package org.litespring.aop;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Advice继承MethodInterceptor,拥有Object invoke(MethodInvocation invocation)的能力
 */
public interface Advice extends MethodInterceptor {
    // 获取切入点,如placeOrder
    Pointcut getPointcut();
}