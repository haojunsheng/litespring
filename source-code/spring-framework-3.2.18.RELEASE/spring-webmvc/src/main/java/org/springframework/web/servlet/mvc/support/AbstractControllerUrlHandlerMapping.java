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

package org.springframework.web.servlet.mvc.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.servlet.handler.AbstractDetectingUrlHandlerMapping;

/**
 * Base class for {@link org.springframework.web.servlet.HandlerMapping} implementations
 * that derive URL paths according to conventions for specific controller types.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see ControllerClassNameHandlerMapping
 * @see ControllerBeanNameHandlerMapping
 * 将实现了Controller接口或者注释了@Controller的bean作为Handler
 * 可以通过设置excludedClasses和excludedPackages将不包含的bean或者不包含的包下的所有的bean排除在外
 */
public abstract class AbstractControllerUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping  {

	private ControllerTypePredicate predicate = new AnnotationControllerTypePredicate();

	private Set<String> excludedPackages = Collections.singleton("org.springframework.web.servlet.mvc");

	private Set<Class<?>> excludedClasses = Collections.emptySet();


	/**
	 * Set whether to activate or deactivate detection of annotated controllers.
	 */
	public void setIncludeAnnotatedControllers(boolean includeAnnotatedControllers) {
		this.predicate = (includeAnnotatedControllers ?
				new AnnotationControllerTypePredicate() : new ControllerTypePredicate());
	}

	/**
	 * Specify Java packages that should be excluded from this mapping.
	 * Any classes in such a package (or any of its subpackages) will be
	 * ignored by this HandlerMapping.
	 * <p>Default is to exclude the entire "org.springframework.web.servlet.mvc"
	 * package, including its subpackages, since none of Spring's out-of-the-box
	 * Controller implementations is a reasonable candidate for this mapping strategy.
	 * Such controllers are typically handled by a separate HandlerMapping,
	 * e.g. a {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping},
	 * alongside this ControllerClassNameHandlerMapping for application controllers.
	 */
	public void setExcludedPackages(String... excludedPackages) {
		this.excludedPackages = (excludedPackages != null) ?
				new HashSet<String>(Arrays.asList(excludedPackages)) : new HashSet<String>();
	}

	/**
	 * Specify controller classes that should be excluded from this mapping.
	 * Any such classes will simply be ignored by this HandlerMapping.
	 */
	public void setExcludedClasses(Class<?>... excludedClasses) {
		this.excludedClasses = (excludedClasses != null) ?
				new HashSet<Class<?>>(Arrays.asList(excludedClasses)) : new HashSet<Class<?>>();
	}


	/**
	 * This implementation delegates to {@link #buildUrlsForHandler},
	 * provided that {@link #isEligibleForMapping} returns {@code true}.
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		Class<?> beanClass = getApplicationContext().getType(beanName);
		// 调用isEligibleForMapping方法判断是不是支持的类型
		if (isEligibleForMapping(beanName, beanClass)) {
			// 模板方法，在子类实现
			return buildUrlsForHandler(beanName, beanClass);
		}
		else {
			return null;
		}
	}

	/**
	 * Determine whether the specified controller is excluded from this mapping.
	 * @param beanName the name of the controller bean
	 * @param beanClass the concrete class of the controller bean
	 * @return whether the specified class is excluded
	 * @see #setExcludedPackages
	 * @see #setExcludedClasses
	 */
	protected boolean isEligibleForMapping(String beanName, Class<?> beanClass) {
		if (beanClass == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean type could not be determined");
			}
			return false;
		}
		// 排除在excludedClasses中配置的类
		if (this.excludedClasses.contains(beanClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean class is explicitly excluded: " + beanClass.getName());
			}
			return false;
		}
		String beanClassName = beanClass.getName();
		// 排除在exclededPackages里配置的包下的类
		for (String packageName : this.excludedPackages) {
			if (beanClassName.startsWith(packageName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
							"because its bean class is defined in an excluded package: " + beanClass.getName());
				}
				return false;
			}
		}
		// 是否实现了Controller或者注释了@Controller
		return isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that is supported by this mapping strategy.
	 * @param beanClass the class to introspect
	 */
	protected boolean isControllerType(Class<?> beanClass) {
		return this.predicate.isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that dispatches to multiple action methods.
	 * @param beanClass the class to introspect
	 */
	protected boolean isMultiActionControllerType(Class<?> beanClass) {
		return this.predicate.isMultiActionControllerType(beanClass);
	}


	/**
	 * Abstract template method to be implemented by subclasses.
	 * @param beanName the name of the bean
	 * @param beanClass the type of the bean
	 * @return the URLs determined for the bean
	 */
	protected abstract String[] buildUrlsForHandler(String beanName, Class<?> beanClass);

}
