package org.litespring.context;

import org.litespring.beans.factory.BeanFactory;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 * <p>
 * 包含BeanFactory的所有功能，增加了支持不同信息源，可以访问资源，支持应用事件机制等
 */
public interface ApplicationContext extends BeanFactory {

}