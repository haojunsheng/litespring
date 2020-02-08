# litespring

# 前言

刘欣老师的这个版本最好使用Java-7来做。

# 0 单元测试

# 1. 概述

http://repo.spring.io/libs-release/org/springframework/spring/

https://github.com/dachengxi/spring-framework-3.2.18.RELEASE

Spring 3.2.18

| 包名                  | 含义               | 备注 |
| --------------------- | ------------------ | ---- |
| beans                 | 存放所有的bean     |      |
| beans.factory         | 存放所有的工厂接口 |      |
| beans.factory.support | 所有工厂的实现     |      |
| beans.factory.config; | 可配置文件         |      |
| core                  |                    |      |
| core.io               |                    |      |
|                       |                    |      |

p means package，c means class，i means interface, f means function

- beans(p)
  - factory(p)：存放所有的工厂接口  
    - support(p)：实现
      - DefaultBeanFactory(c):BeanFactory的默认实现
      - GenericBeanDefinition(c):GenericBeanDefinition is a one-stop shop for standard bean definition purposes.
    - BeanFactory(I):The root interface for accessing a Spring bean container
  - BeanDefinition(I):BeanDefinition中保存了我们的Bean信息

## 1.1 介绍Spring IoC, AOP

Spring 的本质系列(1) -- [依赖注入](https://mp.weixin.qq.com/s?__biz=MzAxOTc0NzExNg==&mid=2665513179&idx=1&sn=772226a5be436a0d08197c335ddb52b8&scene=21#wechat_redirect)

IoC（Inversion of Control，控制反转）和DI（(Dependency Injection，依赖注入）是一个含义。

[![img](https://github.com/anapodoton/ImageHost/raw/master/img/20190908124732.png?raw=trueraw=true)](https://github.com/anapodoton/ImageHost/blob/master/img/20190908124732.png?raw=trueraw=true)

Spring本质系列(2)-[AOP](https://mp.weixin.qq.com/s?__biz=MzAxOTc0NzExNg==&mid=2665513187&idx=1&sn=f603eee3e798e79ce010c9d58cd2ecf3&scene=21#wechat_redirect)

AOP（Aspect Oriented Programming）

[![img](https://github.com/anapodoton/ImageHost/raw/master/img/20190908124824.png?raw=trueraw=true)](https://github.com/anapodoton/ImageHost/blob/master/img/20190908124824.png?raw=trueraw=true)

## 1.2 介绍TDD开发方式， 重构的方法

## 1.3 第一个测试用例

给定一个xml配置的文件（内含bean的定义），能够 从中获取：

1. Bean的定义
2. Bean 的实例

**testcase-1**

| commit                                                 | 功能                                                    | 备注                                                |
| ------------------------------------------------------ | ------------------------------------------------------- | --------------------------------------------------- |
| 测试用例和空的类实现                                   | 实现了根据xml文件的beanID生成相应实例的框架             |                                                     |
| 实现DefaultBeanFactory                                 | 实现了根据xml文件的beanID生成相应实例的框架             |                                                     |
| 引入Exception                                          | 分为BeanCreationException和BeanDefinitionStoreException |                                                     |
| 重构，提取BeanDefinitonRegistry接口                    | 实现XmlBeanDefinitionReader                             | SRP，单一职责原则，需要将DefaultBeanFactory进行拆解 |
| 实现ApplicationContext和ClassPathXmlApplicationContext | 传入配置文件和beanID即可获得bean实例                    |                                                     |
| 实现Resource的抽象                                     | ClassPathResource和FileSystemResource实现               |                                                     |
| 提取出resource后的重构代码                             | XmlBeanDefinitionReader修改                             |                                                     |
| 实现FileSystemXMLApplicationContext                    | FileSystemXMLApplicationContext实现                     |                                                     |
| 实现scope                                              | 根据scope的类型设计实例的生成方式                       |                                                     |

1. 首先实现了DefaultBeanFactory，然后引入异常处理。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190908125102.png)

2. 然后由 [SRP，单一职责原则]，需要将DefaultBeanFactory进行拆解。将读取解析xml文件的功能拆分出去。首先想到的是在接口BeanFactory中新增registerBeanDefinition(),其对应实现为XMLBeanDefinitionReader 。这样DefaultBeanFactory负责生成实例，XMLBeanDefinitionReader负责读取并解析xml。如下图所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908151908.png?raw=true)

3. 但是现在还存在问题，为了安全考虑，我们不想把getBeanDefinition()和registerBeanDefinition()暴露给用户使用，所以我们重新定义了BeanDefinitionRegistry接口，如下图所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908152802.png?raw=true)

4. 事实上，上面还是显得略为麻烦，我们希望传入beanID就可以得到bean的实例，下面我们将定义一个接口，ApplicationContext，其对应实现为ClassPathXmlApplicationContext。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190908155522.png)

5. 下面我们继续进行抽象，我们需要判断传入的配置文件是否真的存在。我们既可以从ClassPathResource读取配置文件（借助于ClassLoader），也可以从FileSystemResource(借助于FileSystem)配置文件，所以我们定义一个Resource接口，及其两个实现ClassPathResource和FileSystemResource，如下图所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908162024.png?raw=true)

6. 下面我们需要把Resource提取出来，进行代码的重构，主要修改XmlBeanDefinitionReader。

7. 下面我们需要实现FileSystemXMLApplicationContext，关系图如下所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908162935.png?raw=true)

8. 但是我们发现FileSystemXMLApplicationContext和ClassPathXmlApplicationContext有很多重复的方法，回想起我们学习过的设计模式中的  【模板方法】，可以完美的解决该问题。我们可以定义一个抽象类AbstractApplicationContext，FileSystemXMLApplicationContext和ClassPathXmlApplicationContext为其子类，关系如下所示：

   ![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908170723.png?raw=true)

   ![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908170723.png?raw=true)

9. 上面的实现中，我们的ClassLoader都是获取的默认的，不支持用户来传入，下面我们来进行优化，支持用户传入一个ClassLoader。为此，我们定义了一个接口，ConfigurableBeanFactory。关系如下图所示：

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190908211924.png)

指的注意的是，Spring的实现和我们是有所不同的，Spring还提供ResourceLoader的功能，具体见下图所示：

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190908212423.png)

10. 实现scope。Spring中的scope的含义如下：

    | scope类别      | 含义                   |
    | -------------- | ---------------------- |
    | singleton      | 单一实例               |
    | prototype      | 每次都重新生成一个实例 |
    | request        |                        |
    | session        |                        |
    | global session |                        |

    ![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908220737.png?raw=true)

下面引入单例的实现：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908222038.png?raw=true)

我们首先重新定义了BeanDefinition，并且让GenericBeanDefinition实现了BeanDefinition 接口，，实现了接口中的方法。

```java
// 定义了Bean的样子，添加了SCOPE_SINGLETON，SCOPE_PROTOTYPE和SCOPE_DEFAULT字段
//  新增了2个判断方法，setScope和getScope方法
public interface BeanDefinition {
    public static final String SCOPE_SINGLETON = "singleton";
    public static final String SCOPE_PROTOTYPE = "prototype";
    public static final String SCOPE_DEFAULT = "";

    public boolean isSingleton();
    public boolean isPrototype();
    String getScope();
    void setScope(String scope);

    String getBeanClassName();
}
```

然后定义了SingletonBeanRegistry接口，让DefaultSingletonBeanRegistry 继承SingletonBeanRegistry，完成了beanName和singletonObject的映射。

再然后让DefaultBeanFactory 继承DefaultSingletonBeanRegistry ，在getBean的时候，判断bd如果是单例，并且不存在，则创建，否则直接创建bd实例。

最后，不要忘记在XmlBeanDefinitionReader的构造方法中设置SCOPE_ATTRIBUTE。

11. 总结

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908223028.png?raw=true)

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908223201.png?raw=true)



| 类/接口                      | 功能                                                         | 备注                |
| ---------------------------- | ------------------------------------------------------------ | ------------------- |
| BeanDefinition               | 封装了Bean的所有的定义                                       |                     |
| GenericBeanDefinition        | BeanDefinition的其中一个具体实现                             |                     |
| BeanDefinitionRegistry       | 用于存放BeanDefinition                                       |                     |
| XmlBeanDefinitionReader      | 解析xml，让beanID和BeanDefinition一一对应                    |                     |
| BeanFactory                  | 根据beanID生成相应的实例                                     |                     |
| ConfigurableBeanFactory      | BeanFactory的子例，用于setBeanClassLoader和getBeanClassLoader | 为了设置ClassLoader |
| SingletonBeanRegistry        | 支持单例                                                     | 为了支持scope       |
| DefaultSingletonBeanRegistry | SingletonBeanRegistry的实现                                  |                     |
| **DefaultBeanFactory**       | 最核心，逻辑                                                 | 集大成者            |

为了方便客户的使用，我们抽象出了ApplicationContext

| 类/接口                         | 功能                             | 备注               |
| ------------------------------- | -------------------------------- | ------------------ |
| ApplicationContext              |                                  | 方便客户使用       |
| AbstractApplicationContext      |                                  | 使用模板的设计模式 |
| ClassPathXmlApplicationContext  | AbstractApplicationContext的子类 |                    |
| FileSystemXmlApplicationContext | AbstractApplicationContext的子类 |                    |
| Resource                        | 定义了资源的抽象                 |                    |
| ClassPathResource               | 从路径获取资源                   |                    |
| FileSystemResource              | 从文件获取资源                   |                    |