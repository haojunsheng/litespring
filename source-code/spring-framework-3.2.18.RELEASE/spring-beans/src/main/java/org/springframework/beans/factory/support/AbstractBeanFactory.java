/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
 * implementations, providing the full capabilities of the
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
 * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
 * handling, aliases, bean definition merging for child bean definitions,
 * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @since 15 April 2001
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * BeanFactory的抽象实现
 * 提供对单例的缓存，可以判断是单例还是原型，管理，别名，bean definition的合并，bean的销毁等。
 * 继承了FactoryBeanRegistrySupport
 * 实现了ConfigurableBeanFactory
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/** Parent bean factory, for bean inheritance support
	 * 父bean工厂
	 * */
	private BeanFactory parentBeanFactory;

	/** ClassLoader to resolve bean class names with, if necessary
	 * 用来处理类型的类加载器
	 * */
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** ClassLoader to temporarily resolve bean class names with, if necessary
	 * 临时的类加载器
	 * */
	private ClassLoader tempClassLoader;

	/** Whether to cache bean metadata or rather reobtain it for every access
	 * 是否缓存bean元数据，不缓存的话，每次访问都会重新获取
	 * */
	private boolean cacheBeanMetadata = true;

	/** Resolution strategy for expressions in bean definition values
	 * bean定义中的表达式的处理策略
	 * */
	private BeanExpressionResolver beanExpressionResolver;

	/** Spring ConversionService to use instead of PropertyEditors
	 * 转换服务接口，用来代替PropertyEditors
	 * */
	private ConversionService conversionService;

	/** Custom PropertyEditorRegistrars to apply to the beans of this factory
	 * 要应用在当前factory上的PropertyEditorRegistrars
	 * */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars =
			new LinkedHashSet<PropertyEditorRegistrar>(4);

	/** A custom TypeConverter to use, overriding the default PropertyEditor mechanism
	 * 自定义的类型转换器，覆盖了默认的PropertyEditor
	 * */
	private TypeConverter typeConverter;

	/** Custom PropertyEditors to apply to the beans of this factory
	 * 自定义的PropertyEditors
	 * */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors =
			new HashMap<Class<?>, Class<? extends PropertyEditor>>(4);

	/** String resolvers to apply e.g. to annotation attribute values */
	private final List<StringValueResolver> embeddedValueResolvers = new LinkedList<StringValueResolver>();

	/** BeanPostProcessors to apply in createBean
	 * 创建bean时候的要应用的BeanPostProcessors
	 * */
	private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

	/** Indicates whether any InstantiationAwareBeanPostProcessors have been registered
	 * 标记是否InstantiationAwareBeanPostProcessors已经被注册
	 * */
	private boolean hasInstantiationAwareBeanPostProcessors;

	/** Indicates whether any DestructionAwareBeanPostProcessors have been registered
	 * 标记是否DestructionAwareBeanPostProcessors已经被注册
	 * */
	private boolean hasDestructionAwareBeanPostProcessors;

	/** Map from scope identifier String to corresponding Scope
	 * 作用域缓存
	 * */
	private final Map<String, Scope> scopes = new HashMap<String, Scope>(8);

	/** Security context used when running with a SecurityManager */
	private SecurityContextProvider securityContextProvider;

	/** Map from bean name to merged RootBeanDefinition
	 * 合并的RootBeanDefinition的缓存，key是beanName
	 * */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions =
			new ConcurrentHashMap<String, RootBeanDefinition>(64);

	/** Names of beans that have already been created at least once
	 * 已经创建过的bean的名字缓存
	 * */
	private final Map<String, Boolean> alreadyCreated = new ConcurrentHashMap<String, Boolean>(64);

	/** Names of beans that are currently in creation
	 * 正在创建的bean的名字
	 * */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation =
			new NamedThreadLocal<Object>("Prototype beans currently in creation");


	/**
	 * Create a new AbstractBeanFactory.
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Create a new AbstractBeanFactory with the given parent.
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 * @see #getBean
	 * 指定父bean工厂的构造器
	 */
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	// BeanFactory接口的实现
	// 都是调用doGetBean来获取
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args arguments to use if creating a prototype using explicit arguments to a
	 * static factory method. It is invalid to use a non-null args value in any other case.
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 * 返回一个Bean实例
	 */
	public <T> T getBean(String name, Class<T> requiredType, Object... args) throws BeansException {
		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * @param name the name of the bean to retrieve 要获取的Bean的名字
	 * @param requiredType the required type of the bean to retrieve 要获取的Bean的类型
	 * @param args arguments to use if creating a prototype using explicit arguments to a
	 * static factory method. It is invalid to use a non-null args value in any other case.
	 *             创建Bean时要传递的参数，仅限创建Bean时使用
	 * @param typeCheckOnly whether the instance is obtained for a type check,
	 * not for actual use 是否仅仅是检查类型
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 * 返回一个Bean的实例
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
			throws BeansException {
		/**
		 * 将指定的bean名字转换成真正的bean名字
		 * 有两种情况：
		 * 1. 一种是FactoryBean，也就是带&前缀的，需要去掉前缀
		 * 2. 一种是别名的问题，需要根据别名获取beanName
		 */
		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		/**
		 * 根据name从缓存中获取单例
		 * 创建bean的时候会存在依赖注入的情况，
		 * 为避免循环依赖，Spring会不等bean创建完成，
		 * 就将创建bean的ObjectFactory加入到缓存中
		 * 一旦下个bean创建的时候需要依赖上个bean，直接使用ObjectFactory
		 */
		Object sharedInstance = getSingleton(beanName);
		// 缓存中存在，并且参数为null
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			/**
			 * 为指定的bean实例获取对象
			 * 缓存中得到的是bean的原始状态，
			 * 并不一定是我们想要的bean，
			 * 需要调用这个方法返回真正的bean
			 *
			 * 如果是普通Bean，直接返回shareInstance
			 * 如果是FactoryBean，返回它创建的那个实例对象
			 */
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// 缓存中不存在
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			// 原型bean正在创建，抛异常，原型模式下不解决循环依赖
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			// 获取父bean工厂
			BeanFactory parentBeanFactory = getParentBeanFactory();
			/**
			 * 父bean工厂存在，并且当前bean工厂不包含bean定义，
			 * 要去父工厂去查找
			 * 包含bean定义方法由子类实现
			 */
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
                // 恢复原始BeanName
				String nameToLookup = originalBeanName(name);
				// 递归到BeanFactory中寻找
				if (args != null) {
					// Delegation to parent with explicit args.
					// 返回父容器查询的结果
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			// 如果不是仅仅做类型检查，而是创建bean，需要记录下
			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			/**
			 * 到这里的话，要准备创建Bean了
			 * 对于singleton bean来说，容器中还没创建过此Bean
			 * 对于prototype bean来说，本来就是要创建一个新的Bean
			 */

			try {
				/**
				 * 从容器中获取beanName对应的GenericBeanDefinition对象，
				 * 并将其合并转换为RootBeanDefinition
                 *
                 * 从xml配置文件中获取的Bean信息都是存储在GenericBeanDefinition中的，
                 * 但是所有的Bean后续处理都是RootBeanDefinition，所以需要转换，转换的时候如果父类bean
                 * 不为空的话，需要一起合并父类的属性
				 */
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

				// 检查给定的合并的BeanDefinition
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 如果有依赖，需要递归实例化依赖的bean
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dependsOnBean : dependsOn) {
						// 初始化被依赖项
						getBean(dependsOnBean);
						// 注册依赖bean到缓存中去，注册依赖关系
						registerDependentBean(dependsOnBean, beanName);
					}
				}

				// Create bean instance.
				// 实例化完了依赖的bean之后，可以实例化bean本身了
				// 先是单例模式Bean
				if (mbd.isSingleton()) {
					sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
						public Object getObject() throws BeansException {
							try {
								// 这里会在下面过程中被调用，创建bean实例
								return createBean(beanName, mbd, args);
							}
							catch (BeansException ex) {
								// Explicitly remove instance from singleton cache: It might have been put there
								// eagerly by the creation process, to allow for circular reference resolution.
								// Also remove any beans that received a temporary reference to the bean.
								destroySingleton(beanName);
								throw ex;
							}
						}
					});

					/**
					 * 为指定的bean实例获取对象
					 * 缓存中得到的是bean的原始状态，
					 * 并不一定是我们想要的bean，
					 * 需要调用这个方法返回真正的bean
					 *
					 * 如果是普通Bean，直接返回shareInstance
					 * 如果是FactoryBean，返回它创建的那个实例对象
					 */
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					// 原型模式的bean创建一个新实例
					Object prototypeInstance = null;
					try {
						// 创建之前调用，设置原型bean正在创建中缓存
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						// 创建之后调用
						// 从正在创建缓存中移除
						afterPrototypeCreation(beanName);
					}
					// 获取bean实例
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				else {
					/**
					 * 其他scope类型的bean，需要委托给相应的实现类来处理
					 */
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
					}
					try {
						// 获取实例
						Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
							public Object getObject() throws BeansException {
								beforePrototypeCreation(beanName);
								try {
									return createBean(beanName, mbd, args);
								}
								finally {
									afterPrototypeCreation(beanName);
								}
							}
						});
						// 获取bean
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; " +
								"consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				// 创建失败后的清理工作
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 检查需要的类型是否跟创建的bean的类型一样
		if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
			try {
				// 转换为需要的类型
				return getTypeConverter().convertIfNecessary(bean, requiredType);
			}
			catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to convert bean '" + name + "' to required type [" +
							ClassUtils.getQualifiedName(requiredType) + "]", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	// 是否包含指定的bean
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		// 包含单例bean，或者包含BeanDefinition
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			//不是&开头的，是一个工厂bean
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// Not found -> check parent.
		//没发现，去父工厂获取
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	//是否单例
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		//获取单例实例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			//FactoryBean
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			}
			else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}
		//缓存中包含
		else if (containsSingleton(beanName)) {
			return true;
		}

		else {
			// No singleton instance found -> check bean definition.
			//没发现单例实例，检查BeanDefinition
			//父工厂
			BeanFactory parentBeanFactory = getParentBeanFactory();
			//去父工厂查询
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.isSingleton(originalBeanName(name));
			}
			//父工厂也没有，查找BeanDefinition中是否有
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// In case of FactoryBean, return singleton status of created object if not a dereference.
			//BeanDefinition是单例的
			if (mbd.isSingleton()) {
				//是FactoryBean
				if (isFactoryBean(beanName, mbd)) {
					//是工厂bean的引用，直接返回true
					if (BeanFactoryUtils.isFactoryDereference(name)) {
						return true;
					}
					//如果不是&开头，要去先获取工厂bean，然后判断是否是单例
					FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
					return factoryBean.isSingleton();
				}
				else {//不是一个FactoryBean
					return !BeanFactoryUtils.isFactoryDereference(name);
				}
			}
			else {
				return false;
			}
		}
	}

	//是否是原型bean
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		//当前工厂不存在，就去父工厂去查找
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		//原型bean
		if (mbd.isPrototype()) {
			// In case of FactoryBean, return singleton status of created object if not a dereference.
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}
		else {//单例或者其他作用域bean
			// Singleton or scoped - not a prototype.
			// However, FactoryBean may still produce a prototype object...
			if (BeanFactoryUtils.isFactoryDereference(name)) {
				return false;
			}
			if (isFactoryBean(beanName, mbd)) {
				final FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				if (System.getSecurityManager() != null) {
					return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
						public Boolean run() {
							return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean).isPrototype()) ||
									!factoryBean.isSingleton());
						}
					}, getAccessControlContext());
				}
				else {
					return ((factoryBean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factoryBean).isPrototype()) ||
							!factoryBean.isSingleton());
				}
			}
			else {
				return false;
			}
		}
	}

	//检查类型是否匹配
	public boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		Class<?> typeToMatch = (targetType != null ? targetType : Object.class);

		// Check manually registered singletons.
		//先从缓存中查找单例实例
		Object beanInstance = getSingleton(beanName, false);
		//缓存中已经存在单例实例
		if (beanInstance != null) {
			//实例是FactoryBean
			if (beanInstance instanceof FactoryBean) {
				//不是以&开头，说明获取到的单例实例是一个FactoryBean
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					//返回FactoryBean的getObject方法返回的对象的类型
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && ClassUtils.isAssignable(typeToMatch, type));
				}
				else {
					return ClassUtils.isAssignableValue(typeToMatch, beanInstance);
				}
			}
			else {
				return !BeanFactoryUtils.isFactoryDereference(name) &&
						ClassUtils.isAssignableValue(typeToMatch, beanInstance);
			}
		}
		//没有实例
		else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return false;
		}

		else {
			// No singleton instance found -> check bean definition.
			//没发现单例，检查BeanDefinition
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.isTypeMatch(originalBeanName(name), targetType);
			}

			// Retrieve corresponding bean definition.
			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			Class<?>[] typesToMatch = (FactoryBean.class.equals(typeToMatch) ?
					new Class<?>[] {typeToMatch} : new Class<?>[] {FactoryBean.class, typeToMatch});

			// Check decorated bean definition, if any: We assume it'll be easier
			// to determine the decorated bean's type than the proxy's type.
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return typeToMatch.isAssignableFrom(targetClass);
				}
			}

			Class<?> beanType = predictBeanType(beanName, mbd, typesToMatch);
			if (beanType == null) {
				return false;
			}

			// Check bean class whether we're dealing with a FactoryBean.
			if (FactoryBean.class.isAssignableFrom(beanType)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// If it's a FactoryBean, we want to look at what it creates, not the factory class.
					beanType = getTypeForFactoryBean(beanName, mbd);
					if (beanType == null) {
						return false;
					}
				}
			}
			else if (BeanFactoryUtils.isFactoryDereference(name)) {
				// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
				// type but we nevertheless are being asked to dereference a FactoryBean...
				// Let's check the original bean class and proceed with it if it is a FactoryBean.
				beanType = predictBeanType(beanName, mbd, FactoryBean.class);
				if (beanType == null || !FactoryBean.class.isAssignableFrom(beanType)) {
					return false;
				}
			}

			return typeToMatch.isAssignableFrom(beanType);
		}
	}

	//获取类型
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			}
			else {
				return beanInstance.getClass();
			}
		}
		else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return null;
		}

		else {
			// No singleton instance found -> check bean definition.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// No bean definition found in this factory -> delegate to parent.
				return parentBeanFactory.getType(originalBeanName(name));
			}

			RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

			// Check decorated bean definition, if any: We assume it'll be easier
			// to determine the decorated bean's type than the proxy's type.
			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
					return targetClass;
				}
			}

			Class<?> beanClass = predictBeanType(beanName, mbd);

			// Check bean class whether we're dealing with a FactoryBean.
			if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					// If it's a FactoryBean, we want to look at what it creates, not at the factory class.
					return getTypeForFactoryBean(beanName, mbd);
				}
				else {
					return beanClass;
				}
			}
			else {
				return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
			}
		}
	}

	//获取别名
	@Override
	public String[] getAliases(String name) {
		//先获取真正的beanName
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<String>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		//以&开头的话，获取到的beanName也要添加前缀
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		//从别名缓存中找到别名
		String[] retrievedAliases = super.getAliases(beanName);
		for (String retrievedAlias : retrievedAliases) {
			String alias = (factoryPrefix ? FACTORY_BEAN_PREFIX : "") + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		//父工厂中查找
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		return StringUtils.toStringArray(aliases);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	// HierarchicalBeanFactory接口的实现
	//---------------------------------------------------------------------

	// 获取父bean工厂
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	// 对ConfigurableBeanFactory接口的实现
	//---------------------------------------------------------------------

	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	public void setTempClassLoader(ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	public void setBeanExpressionResolver(BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return this.conversionService;
	}

	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * Return the set of PropertyEditorRegistrars.
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.isAssignable(PropertyEditor.class, propertyEditorClass);
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		}
		else {
			// Build default TypeConverter, registering custom editors.
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	public String resolveEmbeddedValue(String value) {
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			if (result == null) {
				return null;
			}
			result = resolver.resolveStringValue(result);
		}
		return result;
	}

	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		this.beanPostProcessors.remove(beanPostProcessor);
		this.beanPostProcessors.add(beanPostProcessor);
		if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
			this.hasInstantiationAwareBeanPostProcessors = true;
		}
		if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
			this.hasDestructionAwareBeanPostProcessors = true;
		}
	}

	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		// 此状态在注册后置处理器的时候设置
		return this.hasInstantiationAwareBeanPostProcessors;
	}

	/**
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return this.hasDestructionAwareBeanPostProcessors;
	}

	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		this.scopes.put(scopeName, scope);
	}

	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	/**
	 * Delegate the creation of the access control context to the
	 * {@link #setSecurityContextProvider SecurityContextProvider}.
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.hasInstantiationAwareBeanPostProcessors = this.hasInstantiationAwareBeanPostProcessors ||
					otherAbstractFactory.hasInstantiationAwareBeanPostProcessors;
			this.hasDestructionAwareBeanPostProcessors = this.hasDestructionAwareBeanPostProcessors ||
					otherAbstractFactory.hasDestructionAwareBeanPostProcessors;
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		}
		else {
			setTypeConverter(otherFactory.getTypeConverter());
		}
	}

	/**
	 * Return a 'merged' BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * <p>This {@code getMergedBeanDefinition} considers bean definition
	 * in ancestors as well.
	 * @param name the name of the bean to retrieve the merged definition for
	 * (may be an alias)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		String beanName = transformedBeanName(name);

		// Efficiently check whether bean definition exists in this factory.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// Resolve merged bean definition locally.
		return getMergedLocalBeanDefinition(beanName);
	}

	//是否是工厂bean
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		else if (containsSingleton(beanName)) {
			// null instance registered
			return false;
		}

		// No singleton instance found -> check bean definition.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}

		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	@Override
	public boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified prototype bean is currently in creation
	 * (within the current thread).
	 * @param beanName the name of the bean
	 * 原型bean是否正在创建
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		// 正在创建的bean缓存中获取
		Object curVal = this.prototypesCurrentlyInCreation.get();
		return (curVal != null &&
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * Callback before prototype creation.
	 * <p>The default implementation register the prototype as currently in creation.
	 * @param beanName the name of the prototype about to be created
	 * @see #isPrototypeCurrentlyInCreation
	 * 原型模式bean创建之前调用
	 * 设置原型bean正在创建中缓存
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		}
		else if (curVal instanceof String) {
			Set<String> beanNameSet = new HashSet<String>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		}
		else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * Callback after prototype creation.
	 * <p>The default implementation marks the prototype as not in creation anymore.
	 * @param beanName the name of the prototype that has been created
	 * @see #isPrototypeCurrentlyInCreation
	 * 原型bean创建之后调用
	 * 从正在创建缓存中移除
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		}
		else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to the given bean definition.
	 * @param beanName the name of the bean definition
	 * @param beanInstance the bean instance to destroy
	 * @param mbd the merged bean definition
	 * 销毁给定的bean实例
	 */
	protected void destroyBean(String beanName, Object beanInstance, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(beanInstance, beanName, mbd, getBeanPostProcessors(), getAccessControlContext()).destroy();
	}

	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope '" + scopeName + "'");
		}
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Return the bean name, stripping out the factory dereference prefix if necessary,
	 * and resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed bean name
	 * 返回bean名字，去除前缀，别名转换成bean名
	 */
	protected String transformedBeanName(String name) {
		// canonicalName在SimpleAliasRegistry中实现，返回bean名字
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * Determine the original bean name, resolving locally defined aliases to canonical names.
	 * @param name the user-specified name
	 * @return the original bean name
	 * 找到原始bean名字
	 */
	protected String originalBeanName(String name) {
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * Initialize the given BeanWrapper with the custom editors registered
	 * with this factory. To be called for BeanWrappers that will create
	 * and populate bean instances.
	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
	 * Can be overridden in subclasses.
	 * @param bw the BeanWrapper to initialize
	 * 初始化给定的BeanWrapper，设置转换服务，注册自定义的编辑器
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * <p>To be called for BeanWrappers that will create and populate bean
	 * instances, and for SimpleTypeConverter used for constructor argument
	 * and factory method type conversion.
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		PropertyEditorRegistrySupport registrySupport =
				(registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
		if (registrySupport != null) {
			registrySupport.useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				}
				catch (BeanCreationException ex) {
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						if (isCurrentlyInCreation(bce.getBeanName())) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
				Class<?> requiredType = entry.getKey();
				Class<? extends PropertyEditor> editorClass = entry.getValue();
				registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
			}
		}
	}


	/**
	 * Return a merged RootBeanDefinition, traversing the parent bean definition
	 * if the specified bean corresponds to a child bean definition.
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 * 返回一个合并过得RootBeanDefinition
	 * 将存储XML配置文件的GernericBeanDefinition转换为RootBeanDefinition，如果是一个子bean，会合并父类的相关属性
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		// 先从缓存中查找
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null) {
			return mbd;
		}
		// 缓存中没有，继续调用该方法合并，如果返回的BeanDefinition是子类bean，则合并父类相关属性
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * Return a RootBeanDefinition for the given top-level bean, by merging with
	 * the parent if the given bean's definition is a child bean definition.
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 * 返回一个RootBeanDefinition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {

		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * Return a RootBeanDefinition for the given bean, by merging with the
	 * parent if the given bean's definition is a child bean definition.
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @param containingBd the containing bean definition in case of inner bean,
	 * or {@code null} in case of a top-level bean
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 * 返回一个RootBeanDefinition，如果给定的Bean定义是一个子Bean定义，就会跟父Bean定义合并
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		// 同步
		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null;

			// Check with full lock now in order to enforce the same merged instance.
			if (containingBd == null) {
				// 缓存中获取
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null) {
				if (bd.getParentName() == null) {
					// Use copy of given root bean definition.
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					}
					else {
						mbd = new RootBeanDefinition(bd);
					}
				}
				else {
					// Child bean definition: needs to be merged with parent.
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							pbd = getMergedBeanDefinition(parentBeanName);
						}
						else {
							if (getParentBeanFactory() instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(parentBeanName);
							}
							else {
								throw new NoSuchBeanDefinitionException(bd.getParentName(),
										"Parent name '" + bd.getParentName() + "' is equal to bean name '" + beanName +
										"': cannot be resolved without an AbstractBeanFactory parent");
							}
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// Deep copy with overridden values.
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// Set default singleton scope, if not configured before.
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
				}

				// A bean contained in a non-singleton bean cannot be a singleton itself.
				// Let's correct this on the fly here, since this might be the result of
				// parent-child merging for the outer bean, in which case the original inner bean
				// definition will not have inherited the merged outer bean's singleton status.
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// Only cache the merged bean definition if we're already about to create an
				// instance of the bean, or at least have already created an instance before.
				if (containingBd == null && isCacheBeanMetadata() && isBeanEligibleForMetadataCaching(beanName)) {
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}

			return mbd;
		}
	}

	/**
	 * Check the given merged bean definition,
	 * potentially throwing validation exceptions.
	 * @param mbd the merged bean definition to check
	 * @param beanName the name of the bean
	 * @param args the arguments for bean creation, if any
	 * @throws BeanDefinitionStoreException in case of validation failure
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, Object[] args)
			throws BeanDefinitionStoreException {

		// check if bean definition is not abstract
		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}

		// Check validity of the usage of the args parameter. This can
		// only be used for prototypes constructed via a factory method.
		if (args != null && !mbd.isPrototype()) {
			throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
					"Can only specify arguments for the getBean method when referring to a prototype bean definition");
		}
	}

	/**
	 * Remove the merged bean definition for the specified bean,
	 * recreating it on next access.
	 * @param beanName the bean name to clear the merged definition for
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		this.mergedBeanDefinitions.remove(beanName);
	}

	/**
	 * Resolve the bean class for the specified bean definition,
	 * resolving a bean class name into a Class reference (if necessary)
	 * and storing the resolved Class in the bean definition for further use.
	 * @param mbd the merged bean definition to determine the class for
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the resolved bean class (or {@code null} if none)
	 * @throws CannotLoadBeanClassException if we failed to load the class
	 */
	protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {
		try {
			if (mbd.hasBeanClass()) {
				return mbd.getBeanClass();
			}
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
					public Class<?> run() throws Exception {
						return doResolveBeanClass(mbd, typesToMatch);
					}
				}, getAccessControlContext());
			}
			else {
				return doResolveBeanClass(mbd, typesToMatch);
			}
		}
		catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		}
		catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		}
		catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {
		if (!ObjectUtils.isEmpty(typesToMatch)) {
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
				String className = mbd.getBeanClassName();
				return (className != null ? ClassUtils.forName(className, tempClassLoader) : null);
			}
		}
		return mbd.resolveBeanClass(getBeanClassLoader());
	}

	/**
	 * Evaluate the given String as contained in a bean definition,
	 * potentially resolving it as an expression.
	 * @param value the value to check
	 * @param beanDefinition the bean definition that the value comes from
	 * @return the resolved value
	 * @see #setBeanExpressionResolver
	 */
	protected Object evaluateBeanDefinitionString(String value, BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}
		Scope scope = (beanDefinition != null ? getRegisteredScope(beanDefinition.getScope()) : null);
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}


	/**
	 * Predict the eventual bean type (of the processed bean instance) for the
	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
	 * Does not need to handle FactoryBeans specifically, since it is only
	 * supposed to operate on the raw bean type.
	 * <p>This implementation is simplistic in that it is not able to
	 * handle factory methods and InstantiationAwareBeanPostProcessors.
	 * It only predicts the bean type correctly for a standard bean.
	 * To be overridden in subclasses, applying more sophisticated type detection.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition to determine the type for
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type of the bean, or {@code null} if not predictable
	 */
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 * @param beanName the name of the bean
	 * @param mbd the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
		return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @return the type for the bean if determinable, or {@code null} else
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 */
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		if (!mbd.isSingleton()) {
			return null;
		}
		try {
			FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
			return getTypeForFactoryBean(factoryBean);
		}
		catch (BeanCreationException ex) {
			// Can only happen when getting a FactoryBean.
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring bean creation exception on FactoryBean type check: " + ex);
			}
			onSuppressedException(ex);
			return null;
		}
	}

	/**
	 * Mark the specified bean as already created (or about to be created).
	 * <p>This allows the bean factory to optimize its caching for repeated
	 * creation of the specified bean.
	 * @param beanName the name of the bean
	 * 记录指定的bean已经创建
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.containsKey(beanName)) {
			this.alreadyCreated.put(beanName, Boolean.TRUE);
		}
	}

	/**
	 * Perform appropriate cleanup of cached metadata after bean creation failed.
	 * @param beanName the name of the bean
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		this.alreadyCreated.remove(beanName);
	}

	/**
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 * @param beanName the name of the bean
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.containsKey(beanName);
	}

	/**
	 * Remove the singleton instance (if any) for the given bean name,
	 * but only if it hasn't been used for other purposes than type checking.
	 * @param beanName the name of the bean
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.containsKey(beanName)) {
			removeSingleton(beanName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Get the object for the given bean instance, either the bean
	 * instance itself or its created object in case of a FactoryBean.
	 * @param beanInstance the shared bean instance
	 * @param name name that may include factory dereference prefix
	 * @param beanName the canonical bean name
	 * @param mbd the merged bean definition
	 * @return the object to expose for the bean
	 * 为指定的bean实例获取对象
	 * 主要是检测bean是否是FactoryBean，如果是，需要调用该bean对应的FactoryBean实例中的getObject方法作为返回值
	 */
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		// 如果name中是以&为前缀的，但是又不是FactoryBean类型的，抛异常
		if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
        /**
         * 如果不是一个FactoryBean，说明是一个正常的Bean，直接返回
         *
         * 如果是一个FactoryBean，而且该Bean还以&开头，
         * 说明用户想获取工厂实例，而不是工厂的getObject方法返回的值，
         * 直接返回该FactoryBean实例
         *
         * 如果是一个FactoryBean，而且该Bean不以&开头，需要使用getObject获取真正的bean实例
         */
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd == null) {
			// 尝试从缓存中加载bean
			object = getCachedObjectForFactoryBean(beanName);
		}
		// 缓存中不存在，可以确定beanInstance一定是FactoryBean
		if (object == null) {
			// Return bean instance from factory.
			// 这里一定是一个FactoryBean了
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			// containsBeanDefinition在所有的已经加载的类中检测是否定义beanName
			if (mbd == null && containsBeanDefinition(beanName)) {
				// 将存储XML配置文件的GernericBeanDefinition转换为RootBeanDefinition，如果是一个子bean，会合并父类的相关属性
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			// 是否是用户定义的，而不是应用程序本身定义的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			// 获取实例，使用FactoryBean获取Bean对象
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * Determine whether the given bean name is already in use within this factory,
	 * i.e. whether there is a local bean or alias registered under this name or
	 * an inner bean created with this name.
	 * @param beanName the name to check
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 * @param bean the bean instance to check
	 * @param mbd the corresponding bean definition
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean != null &&
				(DisposableBeanAdapter.hasDestroyMethod(bean, mbd) || hasDestructionAwareBeanPostProcessors()));
	}

	/**
	 * Add the given bean to the list of disposable beans in this factory,
	 * registering its DisposableBean interface and/or the given destroy method
	 * to be called on factory shutdown (if applicable). Only applies to singletons.
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 * @param mbd the bean definition for the bean
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 * 可以执行自定义的destory-method方法
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				// 单例模式下，注册需要销毁的bean，会处理实现了DisposableBean接口的bean
				registerDisposableBean(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
			else {
				// A bean with a custom scope...
				// 自定义的scope处理
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
		}
	}


	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * Does not consider any hierarchy this factory may participate in.
	 * Invoked by {@code containsBean} when no cached singleton instance is found.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 * 检查bean工厂是否包含给定名字的bean定义，由子类实现
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 * @param beanName the name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name (never {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
	 * if the bean definition cannot be resolved
	 * @throws BeansException in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 * 得到Bean定义
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given bean definition.
	 * The bean definition will already have been merged with the parent
	 * definition in case of a child definition.
	 * <p>All the other methods in this class invoke this method, although
	 * beans may be cached after being instantiated by this method. All bean
	 * instantiation within this class is performed by this method.
	 * @param beanName the name of the bean bean名字
	 * @param mbd the merged bean definition for the bean 已经合并了父类属性的BeanDefinition对象
	 * @param args arguments to use if creating a prototype using explicit arguments to a
	 * static factory method. This parameter must be {@code null} except in this case.
     *             用于构造方法或者工厂方法创建Bean实例对象的参数
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 * 用给定的definition创建一个bean实例
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, Object[] args)
			throws BeanCreationException;

}
