package org.litespring.context;

import org.litespring.beans.factory.config.ConfigurableBeanFactory;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * 包含BeanFactory的所有功能，增加了支持不同信息源，可以访问资源，支持应用事件机制等
 */
public interface ApplicationContext extends ConfigurableBeanFactory {

}