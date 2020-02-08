# 为什么是Spring？

大家都说“不要重复发明轮子”， 那是没有意义的，确实是这样。 但是我们可以再造一遍， 在造的过程中来学习别人优秀的设计和优雅的代码。

那为什么要造Spring的轮子？

很简单， Spring 几乎就是Java 后端开发的代名词，统治了十几年，人家还在与时俱进，如果没有意外， 估计还要继续统治下去，这样的软件和系统，绝对值得Java 后端程序员投资，深入学习。

# 到底怎么造？

这个问题一度让我非常纠结。

我已经把Spring 核心的IoC和AOP看了一遍，说实话，很痛苦。 难道带着大家去通读源码，然后再仿照着写？

可是Spring实在是太庞大了，仅仅是核心IoC和AOP代码，即使是早期版本，依然是一篇黑暗森林： 每个阅读源码的程序员都像一个带枪的猎人，小心翼翼地在丛林中穿行，一不小心就......

如果这么做，大家很有可能陷入Spring细节的枝枝蔓蔓中， 会从入门到放弃。

还有一种办法，那就是写个极简版本，几百行代码就把最最核心的东西实现了。这样的代码网上也不少， 但是似乎和学习Spring的目的不符啊！

我们学习Spring到底学习什么？

我想**主要是看人家是怎么设计的，对于一些概念是怎么抽象的，代码是怎么组织的......** 基于这个前提，带着大家再去读源码是一件低效率的事情，不能这么做！

后来我想到了一个办法： **从零开始，一步步逼近Spring 。**

从最简单的结构（也就是基于XML的BeanFactory，只有缺省构造函数的Bean）开始，一步步扩展到setter注入， 有参数的构造函数，注解，auto-scan ， 用cglib实现AOP，用java动态代理实现AOP......

在这个过程中，不能天马行空地写代码，脑海中始终有一根弦：一定要时刻注意着和Spring 的核心思想，核心类保持一致，具体的做法就是：**不断地重构**。

这样以来，我们最终的代码会和Spring的核心类非常类似，只是去掉了一些处理各种各样异常情况的枝枝蔓蔓， Spring的主干会得以保留，即逼近Spring，而不是Copy Spring。

在实现的过程中，你会看到Spring为什么是这么设计的，为什么要做出Resource, BeanDefinitionRegistry，InjectedElement,BeanPostProcessor......这样的抽象。 换句话说，**我们要理解why, 而不仅仅是How。  我觉得这也是造Spring轮子的最大价值所在。**

特别值得一提的是，我会采用**测试驱动（TDD)的方案**，完全站在使用者的角度，用测试先行的方式驱动出所有的代码。对单元测试和重构不熟悉的同学，可以看看是怎么完成TDD这个过程的，TDD是如何和设计结合的。

总之，利用这种从零开始，一步步逼近Spring的办法，相当于重现了Spring框架的诞生过程（当然具体的思路和原作者不一定相同）既使得造轮子过程不那么枯燥，又能理解Spring源码的核心思想。

我希望生成的代码“**简约而不简单**”，既能体现Spring的设计思想，又不会过分复杂。

# 为什么没有SpringMVC?

我发现，仅仅是**逼近**Spring 的IoC和AOP的核心功能，就已经非常复杂了。

考虑到SpringMVC更为复杂，尤其是和Web相关的，很多细节的处理代码，这次还是放弃。

# 课程提纲

## 第一周：概述

课程简介

介绍Spring IoC, AOP

介绍TDD开发方式， 重构的方法。

## 第二周：Basic BeanFactory

最简单的结构，基于XML的BeanFactory

缺省构造函数的Bean

Resource，BeanDefinitionRegistry 的抽象

Excpeiton的处理

BeanDefinition接口

单一职责的应用

## 第三周：实现setter注入

Bean的scope问题，SingletonBeanRegistry接口

PropertyValue，RuntimeBeanReference，BeanDefinitionValueResolver的抽象

使用Introspector

TypeConverter 实现从字符串到特定类型的转换

从createBean到initiateBean和populateBean的拆分。

## 第四周： 实现构造函数注入

引入ConstructorArgument

如何找到合适的构造器： ConstructorResolver

TestCase的整理，引入TestSuite

## 第五、六周：实现注解和auto-scan

注解的讲解

使用ASM读取类的Metadata

读取一个包下所有的class 作为Resource 

新的BeanDefinition实现类引入

给auto-scan的Bean 命名： BeanNameGenerator

新的抽象： DependencyDescriptor，InjectionMetadata ，InjectedElement

用AutowiredAnnotationProcessor实现注入

Bean的生命周期

BeanPostProcessor接口及其实现和调用

## 第七、八、九周：实现AOP

准备工作：讲解CGLib和Java动态代理的原理，讲解PointCut， Advice....等AOP概念

实现Pointcut 和 MethodMatcher，MethodLocatingFactory

给定一个对象及相应的方法和一系列拦截器(beforeAdvice,afterAdvice)，需要实现正确的调用次序

实现CGLibProxyFactory

实现“合成”Bean

实现对resolveInnerBean

使用AspectJAutoProxyCreator 进行封装

用Java 动态代理实现AOP

课程总结。