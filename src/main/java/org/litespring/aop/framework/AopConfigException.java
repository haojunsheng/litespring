package org.litespring.aop.framework;

/**
 * Exception that gets thrown on illegal AOP configuration arguments.
 *
 * @author Rod Johnson
 * @since 13.03.2003
 */
@SuppressWarnings("serial")
public class AopConfigException extends RuntimeException {
    /**
     * Constructor for AopConfigException.
     *
     * @param msg the detail message
     */
    public AopConfigException(String msg) {
        super(msg);
    }

    /**
     * Constructor for AopConfigException.
     *
     * @param msg   the detail message
     * @param cause the root cause
     */
    public AopConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}