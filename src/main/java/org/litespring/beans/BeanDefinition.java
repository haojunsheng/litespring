package org.litespring.beans;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * BeanDefinition中保存了我们的Bean信息，比如：
 * 这个Bean指向的是哪个类
 * 是否是单例的
 * 是否懒加载
 * 这个Bean依赖了哪些Bean等等。
 */
public interface BeanDefinition {

    String getBeanClassName();

}