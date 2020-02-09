# litespring

# 前言

刘欣老师的这个版本最好使用Java-7来做。

# 0 单元测试

# 1. 概述

http://repo.spring.io/libs-release/org/springframework/spring/

https://github.com/dachengxi/spring-framework-3.2.18.RELEASE

Spring 3.2.18

p means package，c means class，i means interface, f means function,a means abstract class

- beans(p)
  - factory(p)：存放所有的工厂接口  
    - support(p)：实现
      - DefaultBeanFactory(c):BeanFactory的默认实现
      - GenericBeanDefinition(c):GenericBeanDefinition is a one-stop shop for standard bean definition purposes.
      - BeanDefinitionRegistry(I):Interface for registries that hold bean definitions
      - DefaultSingletonBeanRegistry(C):单例bean注册的通用实现
    - xml
      - XmlBeanDefinitionReader：Bean definition reader for XML bean definitions.
    - config
      - ConfigurableBeanFactory(I):提供Factory的配置功能
      - SingletonBeanRegistry(I):单例类注册
      - RuntimeBeanReference(C):Immutable placeholder class used for a property value object when it's a reference to another bean in the factory, to be resolved at runtime.
      - TypedStringValue(C):Holder for a typed String value. Can be added to bean definitions in order to explicitly specify a target type for a String value
    - BeanFactory(I):The root interface for accessing a Spring bean container
    - BeanCreationException(C):Exception thrown when a BeanFactory encounters an error when attempting to create a bean from a bean definition.
    - BeanDefinitionStoreException(C): Exception thrown when a BeanFactory encounters an invalid bean definition:
    - BeanDefinitionValueResolver(C):用于把beanID生成相应的实例
  - BeanDefinition(I):BeanDefinition中保存了我们的Bean信息
  - BeansException(C):Abstract superclass for all exceptions thrown in the beans package and subpackages.
  - PropertyValue(C):Object to hold information and value for an individual bean property.
- context
  - ApplicationContext(I):包含BeanFactory的所有功能，增加了支持不同信息源，可以访问资源，支持应用事件机制等
  - support
    - AbstractApplicationContext(a):ApplicationContext接口的通用实现
    - ClassPathXmlApplicationContext:解析xml文件并生成bean实例
    - FileSystemXmlApplicationContext:解析xml文件并生成bean实例
- core
  - io
    - Resource(I):Interface for a resource descriptor that abstracts from the actual type of underlying resource, such as a file or class path resource.
    - ClassPathResource:Uses either a given ClassLoader or a given Class for loading resources.
    - FileSystemResource:对java.io.File类型资源的封装，只要是跟File打交道的，基本上都可以使用FileSystemResource。

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

5. 下面我们继续进行抽象，我们需要**判断传入的配置文件是否真的存在**(请注意，这个是支路，不是主干)。我们既可以从ClassPathResource读取配置文件（借助于ClassLoader），也可以从FileSystemResource(借助于FileSystem)配置文件，所以我们定义一个Resource接口，及其两个实现ClassPathResource和FileSystemResource，如下图所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908162024.png?raw=true)

6. 下面我们需要把Resource提取出来，进行代码的重构，主要修改XmlBeanDefinitionReader。

7. 下面我们需要回到主干，接着看ApplicationContext相关的。实现FileSystemXMLApplicationContext和，关系图如下所示：

![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908162935.png?raw=true)

8. 但是我们发现FileSystemXMLApplicationContext和ClassPathXmlApplicationContext有很多重复的方法，回想起我们学习过的设计模式中的  【模板方法】，可以完美的解决该问题。我们可以定义一个抽象类AbstractApplicationContext，FileSystemXMLApplicationContext和ClassPathXmlApplicationContext为其子类，关系如下所示：

   ![](https://github.com/anapodoton/ImageHost/blob/master/img/20190908170723.png?raw=true)

   

9. 上面的实现中，我们的ClassLoader都是获取的默认的，不支持用户来传入，下面我们来进行优化，支持用户传入一个ClassLoader。我们最开始是想把setBeanClassLoader放在BeanFactory接口中，但是这样不是很好，频繁使用的接口，最好只有get方法，尽量少的set方法。所以，我们定义了一个接口，ConfigurableBeanFactory。关系如下图所示：

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

再然后让DefaultBeanFactory 继承DefaultSingletonBeanRegistry ，在getBean的时候，判断bd如果是单例，并且不存在，则创建，否则直接使用bd实例。

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

# 2. setter-injection

在上面，我们用构造函数的方式实现了类的注入，正如我们所知道的，spring IOC的常见注入方式分为3中，分别是构造函数，setter方式和注解的方式，这节课我们来学习注解的方式。

| 注入方式 | 优点 | 缺点 |
| -------- | ---- | ---- |
| 构造函数 |      |      |
| setter   |      |      |
| 注解     |      |      |

1. 实现PropertyValue相关的代码(获取Bean的定义)

   我们要表达petstore-v2.xml中的property属性，为此引入了PropertyValue来表示property属性。
   
   ![](https://raw.githubusercontent.com/haojunsheng/ImageHost/master/20200209151219.png)

我们使用PropertyValue来表示property属性。值得注意的是，值分为两种，一种是引用，另外一种是值，所以我们需要getPropertyValues方法把reference进行转换。

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190909182040.png)

![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190909181258.png)

在BeanDefinition中定义了getPropertyValues接口，在GenericBeanDefinition中实现了该方法。

最后在XmlBeanDefinitionReader完成对petstore-v2.xml的解析。

2. 实现BeanDefinitionResolver

   Resolver的含义是把名称变成一个实例的过程。我们新增了BeanDefinitionResolver，用于把beanID生成相应的实例。

3. 实现setter注入

   拆分了getBean，独立出来createBean，createBean分为instantiateBean和populateBean，前者用来创建实例，后者用来设置属性。其中populateBean函数比较重要，使用了反射的机制，来调用相应的bean的set方法。
   
4. 实现了TypeConverter，并在DefaultBeanFactory中调用

   由于我们在xml中定义的值都属于字符串的值，但是我们实际需要的可能是Interger,Boolean,Date或者其他类型的。

   我们首先创建CustomNumberEditorTest和CustomBooleanEditorTest，并且实现CustomNumberEditor和CustomBooleanEditor。

   ![](https://raw.githubusercontent.com/Anapodoton/ImageHost/master/img/20190910115828.png)

然后定义TypeConverter 接口，并定义其实现SimpleTypeConverter。



5. 总结

在这里，我们主要学习了Setter 注入。

首先，我们引入了新的概念PropertyValue，包含RuntimeBeanReference和TypedStringValue。

然后，我们用BeanDefinitionResolver去resolve相应的bean，生成实例。

最后，我们用TypeConverter将字符的值转化为整形，Boolean值等类型。



| 设计模式                              | 定义 | 备注                                                         |
| ------------------------------------- | ---- | ------------------------------------------------------------ |
| 开闭原则（open-close）                |      | AbstractApplicationContext满足了对修改封闭，对扩展开放的原则 |
| 单一职责原则（single responsibility） |      | XmlBeanDefinitionReader负责解析xml                           |
| 替换原则（liskov）                    |      | 敏捷软件开发，原则，模式与实践                               |
| 接口隔离原则（interface seperation）  |      | DefaultBeanFactory                                           |
| 依赖隔离                              |      |                                                              |