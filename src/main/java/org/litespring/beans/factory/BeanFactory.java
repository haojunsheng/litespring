package org.litespring.beans.factory;

import org.litespring.beans.BeanDefinition;

/**
 * The root interface for accessing a Spring bean container.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Spring
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup. Spring's Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code org.springframework.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * 以下是标准生命周期
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:<br>
 * 1. BeanNameAware's {@code setBeanName}<br>
 * 2. BeanClassLoaderAware's {@code setBeanClassLoader}<br>
 * 3. BeanFactoryAware's {@code setBeanFactory}<br>
 * 4. EnvironmentAware's {@code setEnvironment}
 * 5. EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * 6. ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)<br>
 * 7. ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)<br>
 * 8. MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)<br>
 * 9. ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)<br>
 * 10. ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)<br>
 * 11. {@code postProcessBeforeInitialization} methods of BeanPostProcessors<br>
 * 12. InitializingBean's {@code afterPropertiesSet}<br>
 * 13. a custom init-method definition<br>
 * 14. {@code postProcessAfterInitialization} methods of BeanPostProcessors
 *
 * BeanFactory关闭时的生命周期方法
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:<br>
 * 1. {@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * 2. DisposableBean's {@code destroy}<br>
 * 3. a custom destroy-method definition
 *
 */public interface BeanFactory {
    /**
     * Return an instance, which may be shared or independent, of the specified bean.
     * <p>This method allows a Spring BeanFactory to be used as a replacement for the
     * Singleton or Prototype design pattern. Callers may retain references to
     * returned objects in the case of Singleton beans.
     * <p>Translates aliases back to the corresponding canonical bean name.
     * Will ask the parent factory if the bean cannot be found in this factory instance.
     * 根据名字获取Bean的实例
     */
    Object getBean(String beanID);
}