/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors}
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading through extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overwritten in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 * ApplicationContext接口的通用实现
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext, DisposableBean {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 * MessageSource的名字
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 * LifecycleProcessor的名字
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 * ApplicationEventMulticaster的名字
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Unique id for this context, if any
	 * 当前上下文唯一id
	 * */
	private String id = ObjectUtils.identityToString(this);

	/** Display name
	 * 展示名
	 * */
	private String displayName = ObjectUtils.identityToString(this);

	/** Parent context
	 * 父应用上下文
	 * */
	private ApplicationContext parent;

	/** BeanFactoryPostProcessors to apply on refresh
	 * 刷新时候要应用的BeanFactoryPostProcessor列表
	 * */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors =
			new ArrayList<BeanFactoryPostProcessor>();

	/** System time in milliseconds when this context started
	 * 当前上下文启动的时间
	 * */
	private long startupDate;

	/** Flag that indicates whether this context is currently active
	 * 当前上下文是否活跃的标志
	 * */
	private boolean active = false;

	/** Flag that indicates whether this context has been closed already
	 * 当前上下文关闭的标志
	 * */
	private boolean closed = false;

	/** Synchronization monitor for the "active" flag */
	private final Object activeMonitor = new Object();

	/** Synchronization monitor for the "refresh" and "destroy" */
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered */
	private Thread shutdownHook;

	/** ResourcePatternResolver used by this context */
	private ResourcePatternResolver resourcePatternResolver;

	/** LifecycleProcessor for managing the lifecycle of beans within this context
	 * 管理bean生命周期的LifecycleProcessor
	 * */
	private LifecycleProcessor lifecycleProcessor;

	/** MessageSource we delegate our implementation of this interface to */
	private MessageSource messageSource;

	/** Helper class used in event publishing
	 * 应用事件广播器
	 * */
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Statically specified listeners
	 * ApplicationListener列表
	 * */
	private Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<ApplicationListener<?>>();

	/** Environment used by this context; initialized by {@link #createEnvironment()} */
	private ConfigurableEnvironment environment;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 * @param id the unique id of the context
	 */
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * {@inheritDoc}
	 * <p>If {@code null}, a new environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * {@inheritDoc}
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 * 将给定的事件发布给所有的监听器
	 */
	public void publishEvent(ApplicationEvent event) {
		Assert.notNull(event, "Event must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Publishing event in " + getDisplayName() + ": " + event);
		}
		// 获取应用事件广播器，广播事件
		getApplicationEventMulticaster().multicastEvent(event);
		// 父上下文也广播事件
		if (this.parent != null) {
			this.parent.publishEvent(event);
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 * 返回当前上下文用的应用事件广播器
	 */
	private ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private LifecycleProcessor getLifecycleProcessor() {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	public void setParent(ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}


	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		else {
			this.applicationListeners.add(listener);
		}
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	// 刷新
	public void refresh() throws BeansException, IllegalStateException {
		// 先加锁
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			/**
			 * 创建Bean容器前的准备工作
			 *
			 * 准备工作：
			 * 记录下容器启动的时间
			 * 标记已启动状态
			 * 处理配置文件中的占位符
			 */
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			/**
			 * 创建Bean容器，加载并注册Bean
			 *
			 * 这一步完成后，配置文件会解析成一个个的Bean定义，注册到BeanFactory中。
			 * 这里Bean没有被初始化，只是配置信息都提取出来了，
			 * 注册也只是将这些信息都保存到注册中心，
			 * 就是一个beanName -> beanDefinition的映射
			 */
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			/**
			 * 设置BeanFactory的类加载器
			 * 添加几个BeanPostProcessor
			 * 手动注册几个特殊的bean
			 */
			prepareBeanFactory(beanFactory);

			try {
				/**
				 * 如果一个Bean实现了BeanFactoryPostProcessor这个接口
				 * 容器初始化之后，Spring会调用里面的postProcessBeanFactory方法
				 */
				// Allows post-processing of the bean factory in context subclasses.
				/**
				 * 留给子类实现
				 * 到这里的时候，所有Bean都已经加载，注册完成了
				 * 但是还没有初始化
				 */
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				/**
				 * 调用实现了BeanFactoryPostProcessor接口的各个实现类的
				 * postProcessBeanFactory方法
				 * 这里调用ConfigurationClassPostProcessor的方法来解析@Configuration
				 * 注解的类，就是解析注解方式的配置类
				 *
				 * PropertyPlaceholderConfigurer也是在这里被调用
				 */
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				/**
				 * 注册BeanPostProcessor的实现类
				 * 和BeanFactoryPostProcessor不一样！
				 *
				 * 该接口的两个方法postProcessBeforeInitialization和postProcessAfterInitialization
				 * 分别在Bean初始化之前和初始化之后得到执行。
				 *
				 * 到这里的时候Bean还没初始化。
				 */
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				// 初始化消息源，用来进行国际化处理
				initMessageSource();

				// Initialize event multicaster for this context.
				// 初始化应用事件广播器
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				/**
				 * 具体子类可以在这里初始化一些特殊Bean，在初始化Singleton Beans之前
				 * 模板方法，钩子方法
				 */
				onRefresh();

				// Check for listener beans and register them.
				// 在所有的已经注册的bean中查找监听器bean，并将这些bean注册到消息广播器中
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				/**
				 * 初始化所有的singleton beans
				 * lazy-init的除外
				 */
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				/**
				 * 完成刷新，通知生命周期处理器，同时发布ContextRefreshEvent事件
				 * 广播事件，ApplicationContext初始化完成
				 */
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				/**
				 * 销毁已经初始化的singleton beans
				 * 以免有些bean一直占用资源
				 */
				destroyBeans();

				// Reset 'active' flag.
				// 取消刷新，重置active标识
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 * 准备刷新的上下文环境，设置开始时间，设置active标志，初始化属性资源，比如对系统属性或者环境变量进行准备以及验证
	 */
	protected void prepareRefresh() {
		// 启动时间
		this.startupDate = System.currentTimeMillis();

		// active标志设为true
		synchronized (this.activeMonitor) {
			this.active = true;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Refreshing " + this);
		}

		// Initialize any placeholder property sources in the context environment
		// 初始化占位符属性资源
		initPropertySources();

		// Validate that all properties marked as required are resolvable
		// see ConfigurablePropertyResolver#setRequiredProperties
		// 校验所有被标记为必须的属性
		getEnvironment().validateRequiredProperties();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 * 初始化BeanFactory，并进行xml文件读取
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		/**
		 * 关闭旧的BeanFactory
		 * 创建新的BeanFactory
		 * 加载Bean定义
		 * 注册Bean等
		 */
		refreshBeanFactory();
		// 返回刚才创建的BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (logger.isDebugEnabled()) {
			logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
		}
		return beanFactory;
	}

	/**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 * 对BeanFactory进行各种功能填充
	 * 在这之前的一步，已经完成了对配置的解析，
	 * ApplicationContext的扩展功能将在这里开始进行
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		/**
		 * 设置BeanFactory的类加载器
		 * BeanFactory需要加载类，也就需要类加载器
		 */
		beanFactory.setBeanClassLoader(getClassLoader());
		// 设置表达式语言处理器
		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver());
		// 为BeanFactory增加一个默认的propertyEditor
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		/**
		 * 添加BeanPostProcessor
		 * 实现了Aware接口的beans在初始化的时候，这个processor负责回调
		 * 比如，我们会为了获取ApplicationContext而实现ApplicationContextAware接口
		 */
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		/**
		 * 设置自动忽略装配的接口
		 * 上一步注册BeanPostProcessor，
		 * 在invokeAwareInterfaces方法中间接调用的Aware类型已经不是普通的bean了，
		 * 所以要在bean的依赖注入的时候忽略他们
		 *
		 * 如果某个bean依赖于以下几个接口的实现类
		 * 在自动装配的时候忽略他们
		 * Spring会通过其他方式来处理这些依赖
		 */
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.
		/**
		 * 下面是为几个特殊的bean赋值，如果有bean依赖以下几个，会注入这边相应的值
		 *
		 * 当前的ApplicationContext持有一个BeanFactory
		 *
		 * ApplicationContext还继承了ResourceLoader、ApplicationEventPublisher、MessageSource
		 * 所以这里可以赋值为this，this就是一个ApplicationContext
		 *
		 * MessageSource被注册成了一个普通的Bean
		 */
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		// 增加对AspectJ的支持
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		/**
		 * 注册默认的环境bean
		 * 我们也可以选择覆盖
		 */
		// 如果没有定义environment这个bean，Spring会默认注册一个
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}

		// 如果没有定义systemProperties这个bean，Spring会默认注册一个
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}

		// 如果没有定义systemEnvironment这个bean，Spring会默认注册一个
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 * 实例化和调用所有已经注册的BeanFactoryPostProcessor bean，必须在单例bean实例化前调用
	 * 在bean实例化之前就可以获得配置信息，从而能够正确的解析bean描述文件中的变量引用
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		/**
		 * 如果有BeanDefinitionRegistryPostProcessor，就先执行
		 */

		/**
		 * processedBeans存储已经处理过的
		 */
		Set<String> processedBeans = new HashSet<String>();
		/**
		 * 如果传进来的beanFactory是BeanDefinitionRegistry类型的
 		 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
			List<BeanDefinitionRegistryPostProcessor> registryPostProcessors =
					new LinkedList<BeanDefinitionRegistryPostProcessor>();
			/**
			 * 先从beanFactoryPostProcessors中获取BeanFactoryPostProcessor，
			 * 如果有的话，就遍历进行处理
			 */
			for (BeanFactoryPostProcessor postProcessor : getBeanFactoryPostProcessors()) {
				/**
				 * 如果是BeanDefinitionRegistryPostProcessor，
				 * 就先执行postProcessBeanDefinitionRegistry方法
				 *
				 * 如果是其他的，就添加到regularPostProcessors列表中，
				 * 到了下面再处理
				 */
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryPostProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryPostProcessor.postProcessBeanDefinitionRegistry(registry);
					registryPostProcessors.add(registryPostProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}
			/**
			 * 上面for循环处理的是beanFactoryPostProcessors中的BeanFactoryPostProcessor
			 * 下面处理的是BeanFactory中的BeanDefinitionRegistryPostProcessor
			 *
			 * 直接去BeanFactory中获取BeanDefinitionRegistryPostProcessor类型的Bean
			 */
			Map<String, BeanDefinitionRegistryPostProcessor> beanMap =
					beanFactory.getBeansOfType(BeanDefinitionRegistryPostProcessor.class, true, false);
			List<BeanDefinitionRegistryPostProcessor> registryPostProcessorBeans =
					new ArrayList<BeanDefinitionRegistryPostProcessor>(beanMap.values());

			/**
			 * 将BeanDefinitionRegistryPostProcessor进行排序，
			 * 实现了PriorityOrdered的先执行
			 */
			OrderComparator.sort(registryPostProcessorBeans);
			for (BeanDefinitionRegistryPostProcessor postProcessor : registryPostProcessorBeans) {
				postProcessor.postProcessBeanDefinitionRegistry(registry);
			}

			/**
			 * 上面已经将BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
			 * 接下来就是调用BeanFactoryPostProcessor的postProcessBeanFactory方法
			 */
			invokeBeanFactoryPostProcessors(registryPostProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(registryPostProcessorBeans, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
			// 添加到这个list中，表示已经处理过
			processedBeans.addAll(beanMap.keySet());
		}
		/**
		 * 如果BeanFactory不是BeanDefinitionRegistry类型的
		 * 就看这个BeanFactory中的beanFactoryPostProcessors中是不是有注册的BeanFactoryPostProcessor
		 * 如果有就调用BeanFactoryPostProcessor的postProcessBeanFactory方法
		 */
		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(getBeanFactoryPostProcessors(), beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		/**
		 * 往下是BeanFactoryPostProcessor的处理
 		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		/**
		 * 实现了PriorityOrdered的先调用
		 * 其次是实现了Ordered的接口
		 * 最后是没有实现Ordered接口的BeanFactoryPostProcessor
		 */
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		/**
		 * 实现了PriorityOrdered的先执行
		 */
		OrderComparator.sort(priorityOrderedPostProcessors);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		/**
		 * 实现了Ordered的后执行
		 */
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		OrderComparator.sort(orderedPostProcessors);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		/**
		 * 最后执行没有实现任何Ordered接口的
		 */
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Instantiate and invoke all registered BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 * 注册BeanPostProcessor，真正调用是在getBean的时候
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 从beanFactory中找到所有的BeanPostProcessor的名字
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		/**
		 * 实现了PriorityOrdered的BeanPostProcessor先注册
		 * 其次是实现了Ordered的注册
		 * 最后是没有实现Ordered的注册
		 */
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		OrderComparator.sort(priorityOrderedPostProcessors);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		OrderComparator.sort(orderedPostProcessors);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		OrderComparator.sort(internalPostProcessors);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector());
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 * 初始化消息源，主要是提取配置中定义的messageSource
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		//在配置文件中，消息源bean的名字必须为messageSource
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {//配置文件中用户没有自定义消息源，使用默认的DelegatingMessageSource
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
						"': using default [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 * 初始化应用事件广播器
	 */
	protected void initApplicationEventMulticaster() {
		// 拿到当前的BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		/**
		 * 如果说Spring的配置文件中存在名字为applicationEventMulticaster的Bean，
		 * 也就是说我们自己定义了一个事件广播器，就会使用我们自己定义的。
		 * 配置文件中不存在自定义的广播器，就使用默认的SimpleApplicationEventMulticaster。
		 */
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
						APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
						"': using default [" + this.applicationEventMulticaster + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor.
	 * Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 * 初始化生命周期处理器
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LifecycleProcessor with name '" +
						LIFECYCLE_PROCESSOR_BEAN_NAME +
						"': using default [" + this.lifecycleProcessor + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 * 注册监听器
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		/**
		 * 先注册硬编码方式的监听器
		 */
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		// 我们自己的监听器
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			/**
			 * 先获取广播器，之前的步骤初始化广播器已经实例化过了
			 * 然后将这些监听器添加到广播器中
			 */
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}
	}

	/**
	 * Subclasses can invoke this method to register a listener.
	 * Any beans in the context that are listeners are automatically added.
	 * <p>Note: This method only works within an active application context,
	 * i.e. when an ApplicationEventMulticaster is already available. Generally
	 * prefer the use of {@link #addApplicationListener} which is more flexible.
	 * @param listener the listener to register
	 * @deprecated as of Spring 3.0, in favor of {@link #addApplicationListener}
	 */
	@Deprecated
	protected void addListener(ApplicationListener<?> listener) {
		getApplicationEventMulticaster().addApplicationListener(listener);
	}

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 * 完成BeanFactory的初始化工作，实例化剩下的单例bean
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		/**
		 * 初始化转换服务，名字为conversionService的Bean
		 */
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		// 初始化LoadTimeWeaverAware类型的Bean，是AspectJ相关的
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		/**
		 * 冻结所有的bean定义，注册的bean将不被修改或者任何进一步的处理
		 * Spring已经开始预初始化singleton bean了，这时候就不能出现bean的定义解析、加载、注册
		 */
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 初始化单例Bean，非惰性的
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 * 完成刷新，调用生命周期的处理器
	 */
	protected void finishRefresh() {
		// Initialize lifecycle processor for this context.
		//初始化生命周期处理器
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		//启动所有实现了LifeCycle接口的bean
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		//完成ApplicationContext初始化的时候，需要发布ContextRefreshedEvent事件
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		LiveBeansView.registerApplicationContext(this);
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		synchronized (this.activeMonitor) {
			this.active = false;
		}
	}


	/**
	 * Register a shutdown hook with the JVM runtime, closing this context
	 * on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see #close()
	 * @see #doClose()
	 */
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread() {
				@Override
				public void run() {
					doClose();
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * DisposableBean callback for destruction of this instance.
	 * Only called when the ApplicationContext itself is running
	 * as a bean in another BeanFactory or ApplicationContext,
	 * which is rather unusual.
	 * <p>The {@code close} method is the native way to
	 * shut down an ApplicationContext.
	 * @see #close()
	 * @see org.springframework.beans.factory.access.SingletonBeanFactoryLocator
	 */
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	protected void doClose() {
		boolean actuallyClose;
		synchronized (this.activeMonitor) {
			actuallyClose = this.active && !this.closed;
			this.closed = true;
		}

		if (actuallyClose) {
			if (logger.isInfoEnabled()) {
				logger.info("Closing " + this);
			}

			LiveBeansView.unregisterApplicationContext(this);

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			try {
				getLifecycleProcessor().onClose();
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
			}

			// Destroy all cached singletons in the context's BeanFactory.
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			synchronized (this.activeMonitor) {
				this.active = false;
			}
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	public boolean isActive() {
		synchronized (this.activeMonitor) {
			return this.active;
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(requiredType);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return getBeanFactory().getBean(name, args);
	}

	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isPrototype(name);
	}

	public boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isTypeMatch(name, targetType);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name);
	}

	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	public String[] getBeanNamesForType(Class<?> type) {
		return getBeanFactory().getBeanNamesForType(type);
	}

	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext) ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext) ?
			((AbstractApplicationContext) getParent()).messageSource : getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 * 子类来实现，进行真正的配置的加载
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(": startup date [").append(new Date(getStartupDate()));
		sb.append("]; ");
		ApplicationContext parent = getParent();
		if (parent == null) {
			sb.append("root of context hierarchy");
		}
		else {
			sb.append("parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private class BeanPostProcessorChecker implements BeanPostProcessor {

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean != null && !(bean instanceof BeanPostProcessor) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}
	}


	/**
	 * BeanPostProcessor that detects beans which implement the ApplicationListener interface.
	 * This catches beans that can't reliably be detected by getBeanNamesForType.
	 * Bean实例化后会调用这个postProcessAfterInitialization将监听器添加到广播其中去
	 */
	private class ApplicationListenerDetector implements MergedBeanDefinitionPostProcessor {

		private final Map<String, Boolean> singletonNames = new ConcurrentHashMap<String, Boolean>(64);

		public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
			if (beanDefinition.isSingleton()) {
				this.singletonNames.put(beanName, Boolean.TRUE);
			}
		}

		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof ApplicationListener) {
				// potentially not detected as a listener by getBeanNamesForType retrieval
				Boolean flag = this.singletonNames.get(beanName);
				if (Boolean.TRUE.equals(flag)) {
					// singleton bean (top-level or inner): register on the fly
					addApplicationListener((ApplicationListener<?>) bean);
				}
				else if (flag == null) {
					if (logger.isWarnEnabled() && !containsBean(beanName)) {
						// inner bean with other scope - can't reliably process events
						logger.warn("Inner bean '" + beanName + "' implements ApplicationListener interface " +
								"but is not reachable for event multicasting by its containing ApplicationContext " +
								"because it does not have singleton scope. Only top-level listener beans are allowed " +
								"to be of non-singleton scope.");
					}
					this.singletonNames.put(beanName, Boolean.FALSE);
				}
			}
			return bean;
		}
	}

}
