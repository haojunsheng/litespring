/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * 简单的别名注册实现
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/** Map from alias to canonical name
	 * 存储别名和bean名称对应关系的map
	 * key是别名，value是bean名
	 * */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<String, String>(16);


	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		// 别名和bean的名字相同，不需要注册别名，从缓存中删除别名
		if (alias.equals(name)) {
			this.aliasMap.remove(alias);
		}
		// 别名和bean名字不同
		else {
			// 别名不能被覆盖
			if (!allowAliasOverriding()) {
				// 如果已经注册过，抛异常说明别名已经被注册了，不允许覆盖
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null && !registeredName.equals(name)) {
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			// 别名可以被覆盖，先检查别名的循环依赖
			checkForAliasCircle(name, alias);
			// 添加进缓存
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * Default is {@code true}.
	 * 别名是否可以被覆盖
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	public void removeAlias(String alias) {
		//移除
		String name = this.aliasMap.remove(alias);
		if (name == null) {
			throw new IllegalStateException("No alias '" + alias + "' registered");
		}
	}

	public boolean isAlias(String name) {
		//返回是否有包含指定名字的key
		return this.aliasMap.containsKey(name);
	}

	public String[] getAliases(String name) {
		List<String> result = new ArrayList<String>();
		//ConcurrentHashMap，可以读写同时进行，所以这里读的时候要加锁
		synchronized (this.aliasMap) {
			//找到所有的别名
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Transitively retrieve all aliases for the given name.
	 * @param name the target name to find aliases for
	 * @param result the resulting aliases list
	 * 找到所有的别名存放到result中
	 */
	private void retrieveAliases(String name, List<String> result) {
		//递归查找一个name对应的所有别名
		for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
			String registeredName = entry.getValue();
			if (registeredName.equals(name)) {
				String alias = entry.getKey();
				result.add(alias);
				retrieveAliases(alias, result);
			}
		}
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 *  使用StringValueResolver来解析类名和别名后，进行循环依赖的校验
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<String, String>(this.aliasMap);
			for (String alias : aliasCopy.keySet()) {
				String registeredName = aliasCopy.get(alias);
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias.equals(resolvedName)) {
					this.aliasMap.remove(alias);
				}
				else if (!resolvedAlias.equals(alias)) {
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null && !existingName.equals(resolvedName)) {
						throw new IllegalStateException(
								"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
								"') for name '" + resolvedName + "': It is already registered for name '" +
								registeredName + "'.");
					}
					checkForAliasCircle(resolvedName, resolvedAlias);
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				}
				else if (!registeredName.equals(resolvedName)) {
					this.aliasMap.put(alias, resolvedName);
				}
			}
		}
	}

	/**
	 * Determine the raw name, resolving aliases to canonical names.
	 * @param name the user-specified name
	 * @return the transformed name
	 * 根据指定名字查找原始类名
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}

	/**
	 * Check whether the given name points back to given alias as an alias
	 * in the other direction, catching a circular reference upfront and
	 * throwing a corresponding IllegalStateException.
	 * @param name the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 * 检查别名循环依赖
	 */
	protected void checkForAliasCircle(String name, String alias) {
		//别名和bean名相同，抛异常
		if (alias.equals(canonicalName(name))) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

}
