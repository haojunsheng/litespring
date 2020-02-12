package org.litespring.aop;

import org.aopalliance.intercept.MethodInterceptor;

public interface Advice extends MethodInterceptor {
    Pointcut getPointcut();
}