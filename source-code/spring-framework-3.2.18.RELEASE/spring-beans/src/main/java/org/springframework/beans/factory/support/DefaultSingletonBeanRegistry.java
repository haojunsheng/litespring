/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * 单例bean注册的通用实现
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Internal marker for a null singleton object:
	 * used as marker value for concurrent Maps (which don't support null values).
	 * null单例对象的标记
	 */
	protected static final Object NULL_OBJECT = new Object();


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Cache of singleton objects: bean name --> bean instance
	 * 单例对象缓存，key是bean名字，value是bean实例
	 * */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

	/** Cache of singleton factories: bean name --> ObjectFactory
	 * 单例工厂的缓存，key是bean名字，value是对象工厂
	 * */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

	/** Cache of early singleton objects: bean name --> bean instance
	 * 早期单例对象的缓存，key是bean名字，value是bean实例
	 * 可以用来检测循环引用
	 * */
	private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

	/** Set of registered singletons, containing the bean names in registration order
	 * 已经注册的单例的set，包含了bean名字，顺序是注册时的顺序
	 * */
	private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);

	/** Names of beans that are currently in creation (using a ConcurrentHashMap as a Set)
	 * 当前正在创建的bean的名字的缓存
	 * */
	private final Map<String, Boolean> singletonsCurrentlyInCreation = new ConcurrentHashMap<String, Boolean>(16);

	/** Names of beans currently excluded from in creation checks (using a ConcurrentHashMap as a Set)
	 * 创建过程中不需要进行检查的bean名字的缓存
	 * */
	private final Map<String, Boolean> inCreationCheckExclusions = new ConcurrentHashMap<String, Boolean>(16);

	/** List of suppressed Exceptions, available for associating related causes */
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons
	 * 是否正在销毁单例的标志
	 * */
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name --> disposable instance
	 * Disposable bean实例，key是bean的名字，value是disposable实例
	 * */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/** Map between containing bean names: bean name --> Set of bean names that the bean contains
	 * bean名字对应的bean所包含的bean的名字缓存
	 * */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

	/** Map between dependent bean names: bean name --> Set of dependent bean names
	 * bean名字对应的bean被依赖的bean的名字缓存
	 * key是被依赖的bean
	 * */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies
	 * bean名字对应的bean所依赖的bean的名字缓存
	 * value是被依赖的bean
	 * */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);


	//将指定的对象注册成单例
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "'beanName' must not be null");
		//同步
		synchronized (this.singletonObjects) {
			//先尝试获取有没有包含指定的bean名称
			Object oldObject = this.singletonObjects.get(beanName);
			//已经存在的话，抛异常
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			//不存在，添加进缓存
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 * 添加给定的单例对象进缓存中
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		// 需要同步
		synchronized (this.singletonObjects) {
			// 添加进单例对象缓存
			this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
			// 从单例工厂缓存中移除
			this.singletonFactories.remove(beanName);
			// 从早期单例对象缓存中移除
			this.earlySingletonObjects.remove(beanName);
			// 添加进已注册的单例的set中
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 * 添加单例工厂到缓存中去
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			//单例对象缓存中不包含指定的名字，才能添加
			if (!this.singletonObjects.containsKey(beanName)) {
				//单例工厂缓存中加入
				this.singletonFactories.put(beanName, singletonFactory);
				//早期单例对象缓存中移除
				this.earlySingletonObjects.remove(beanName);
				//添加进已注册的单例的set中
				this.registeredSingletons.add(beanName);
			}
		}
	}

	/**
	 * 获取单例bean
	 */
	public Object getSingleton(String beanName) {
		// 获取单例，允许早期引用
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 * 获取单例，可以指定是否允许早期引用
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 从单例对象缓存中获取
		Object singletonObject = this.singletonObjects.get(beanName);
		// 如果单例对象缓存中不存在，并且是正在创建中
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				// 从早期单例对象中获取
				singletonObject = this.earlySingletonObjects.get(beanName);
				// 早期单例对象不存在，并且允许早期引用
				if (singletonObject == null && allowEarlyReference) {
					// 从单例工厂中获取
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					// 单例工厂中存在，使用单例工厂获取对象
					if (singletonFactory != null) {
						singletonObject = singletonFactory.getObject();
						// 放入早期单例缓存
						this.earlySingletonObjects.put(beanName, singletonObject);
						// 单例工厂缓存中移除
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 * 获取单例对象，如果不存在，注册一个新的
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			// 单例对象缓存中获取
			Object singletonObject = this.singletonObjects.get(beanName);
			// 单例对象缓存中不存在，创建一个新的
			if (singletonObject == null) {
				// 单例正在销毁，抛异常
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while the singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				// 单例创建之前的检查
				beforeSingletonCreation(beanName);
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<Exception>();
				}
				try {
					// 从单例工厂中获取单例对象，调用的是createBean方法
					singletonObject = singletonFactory.getObject();
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 获取单例对象后的检查
					afterSingletonCreation(beanName);
				}
				// 将新创建的单例注册到缓存中，并删除加载bean过程中所记录的各种辅助状态
				addSingleton(beanName, singletonObject);
			}
			// 单例对象缓存中存在或者新的创建了，返回
			return (singletonObject != NULL_OBJECT ? singletonObject : null);
		}
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 * 从当前工厂中删除单例缓存
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			// 单例对象缓存中移除
			this.singletonObjects.remove(beanName);
			// 单例工厂缓存中移除
			this.singletonFactories.remove(beanName);
			// 早期单例对象缓存中移除
			this.earlySingletonObjects.remove(beanName);
			// 已注册的单例名字缓存中移除
			this.registeredSingletons.remove(beanName);
		}
	}

	public boolean containsSingleton(String beanName) {
		// 单例对象缓存中是否包含
		return (this.singletonObjects.containsKey(beanName));
	}

	// 单例的名字，直接从缓存的set中获取
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			// 缓存的已注册的单例的数量
			return this.registeredSingletons.size();
		}
	}


	// 设置单例正在创建中
	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.put(beanName, Boolean.TRUE);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.containsKey(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 * 指定的bean是否是正在创建中
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.containsKey(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 * 单例创建之前的回调
	 */
	protected void beforeSingletonCreation(String beanName) {
		// 创建过程中不需要检查的缓存中不存在，并且正在创建的缓存添加成功，才可以继续执行，否则抛异常
		// 记录bean正在加载的状态
		if (!this.inCreationCheckExclusions.containsKey(beanName) &&
				this.singletonsCurrentlyInCreation.put(beanName, Boolean.TRUE) != null) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		// 创建过程中不需要检查的缓存中不存在，并且从正在创建缓存中移除成功，才可以继续，否则抛异常
		if (!this.inCreationCheckExclusions.containsKey(beanName) &&
				!this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 * 添加进disposable bean缓存中
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean 被包含的bean
	 * @param containingBeanName the name of the containing (outer) bean 包含bean
	 * @see #registerDependentBean
	 * 注册两个bean的包含关系
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				containedBeans = new LinkedHashSet<String>(8);
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			containedBeans.add(containedBeanName);
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 * 注册依赖的bean到缓存中
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		// 获取beanName
		String canonicalName = canonicalName(beanName);
		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			dependentBeans.add(dependentBeanName);
		}
		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 * 检查是否给定的bean已经被注册到依赖bean缓存中去
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 * 获取依赖于指定的bean的所有bean的名字
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 * 获取指定的bean所以来的所有bean的名字
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	//销毁单例
	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in " + this);
		}
		//单例正在销毁标志设为true
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		//挨个销毁
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}
		//以下清除缓存
		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 * 销毁单例
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		//从当前工厂中删除单例缓存
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		//销毁相应的实例
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 * 销毁指定的bean，必须先销毁依赖于当前bean的实例
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		//先从依赖于当前bean的缓存中移除
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		//销毁bean
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		// Trigger destruction of contained beans...
		//包含bean的缓存中移除
		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
