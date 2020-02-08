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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface.
 * Reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code &lt;beans&gt;} doesn't need to be the root
 * element of the XML document: This class will parse all bean definition elements
 * in the XML file, not regarding the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	private Environment environment;

	/**
	 * 解析器当前上下文
	 */
	private XmlReaderContext readerContext;

	private BeanDefinitionParserDelegate delegate;


	@Deprecated
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");
		// 获取xml的root元素
		Element root = doc.getDocumentElement();
		// 从xml根节点开始解析文件，执行注册BeanDefinition
		doRegisterBeanDefinitions(root);
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
	 * source metadata from the supplied {@link Element}.
	 */
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}

	private Environment getEnvironment() {
		return (this.environment != null ? this.environment : getReaderContext().getReader().getEnvironment());
	}


	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 * 使用给定的root元素，进行BeanDefinition的注册
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		/**
		 * 这里处理的是<beans ... profile="dev"/>中的profile
		 * 如果当前环境配置的profile不包含此profile，就直接return
		 * 不再解析此<beans/>
		 */
		String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
		if (StringUtils.hasText(profileSpec)) {
			// 可能会有多个profile
			String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
					profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			// 所有的profile都无效，不进行注册
			if (!getEnvironment().acceptsProfiles(specifiedProfiles)) {
				return;
			}
		}

		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		/**
		 * BeanDefinition解析器代理
		 * BeanDefinitionParserDelegate负责解析Bean定义
		 * <beans/>内部可以定义<beans/>，所以这个方法的root不一定就是xml的根节点，
		 * 也可以是嵌套在里面的<beans/>节点
		 */
		// parent记录老的解析器代理对象
		BeanDefinitionParserDelegate parent = this.delegate;
		// 创建BeanDefinitionParserDelegate对象，并设置到delegate属性
		this.delegate = createDelegate(this.readerContext, root, parent);

		// 允许解析xml之前进行处理
		preProcessXml(root);
		// 解析BeanDefinition
		parseBeanDefinitions(root, this.delegate);
		// 允许解析xml之后进行处理
		postProcessXml(root);

		// 设置delegate为老的BeanDefinitionParserDelegate对象
		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = createHelper(readerContext, root, parentDelegate);
		if (delegate == null) {
			// 创建对象
			delegate = new BeanDefinitionParserDelegate(readerContext, getEnvironment());
			// 初始化默认
			delegate.initDefaults(root, parentDelegate);
		}
		return delegate;
	}

	@Deprecated
	protected BeanDefinitionParserDelegate createHelper(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		return null;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 * 解析顶级的元素，import，alias，bean
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		/**
		 * 处理默认的命名空间：
		 * <import/> <alias/> <bean/> <beans/>
		 * 其他的都属于自定义的，比如：
		 * <mvc/> <task/> <context/> <aop/>
		 */
		if (delegate.isDefaultNamespace(root)) {
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// 子元素是默认的标签
						parseDefaultElement(ele, delegate);
					}
					else {
						// 子元素是自定义的标签
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			// 解析自定义的标签
			delegate.parseCustomElement(root);
		}
	}

	// 解析默认的标签
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// import标签解析
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// alias标签解析
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// bean标签解析
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
		// beans标签解析
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			/**
			 * 嵌套的<beans/>标签，需要递归
			 */
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * 解析import元素，加载import里面配置文件中的BeanDefinition
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// resource属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 将resource中指定的值的占位符解析
		location = getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		// 给定的地址是绝对地址还是相对地址
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		// 绝对地址，递归调用Bean解析过程，进行另一次解析
		if (absoluteLocation) {
			try {
				// 添加配置文件地址的Resource到actualResources中，并加载BeanDefinition
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// No URL -> considering resource location as relative to the current file.
			// 相对地址，先计算出绝对路径得到Resource，然后解析
			try {
				int importCount;
				// 创建相对地址的Resource
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				// 如果存在，加载BeanDefinition，并添加到actualResources中
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					// 不存在，获取根路径
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		// 发送import已经处理完的事件
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 * 处理ailas标签
	 */
	protected void processAliasRegistration(Element ele) {
		// name属性
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// alias属性
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				// 注册alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 发送alias注册事件
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * 解析bean标签
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		/**
		 * <bean/>节点中的信息提取出来，封装到BeanDefinitionHolder中
		 * BeanDefinitionHolder中包含BeanDefinition，beanName，alias
		 */
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// 默认标签下的子节点有自定义属性，还需要再次对自定义标签进行解析
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 注册Bean，注册最后装饰过的实例
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 发送Bean已经注册的事件
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
