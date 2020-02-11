package org.litespring.beans.factory.support;

import org.apache.commons.beanutils.BeanUtils;
import org.litespring.beans.PropertyValue;
import org.litespring.beans.SimpleTypeConverter;
import org.litespring.beans.factory.BeanCreationException;
import org.litespring.beans.BeanDefinition;
import org.litespring.beans.factory.config.BeanPostProcessor;
import org.litespring.beans.factory.config.ConfigurableBeanFactory;
import org.litespring.beans.factory.config.DependencyDescriptor;
import org.litespring.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.litespring.util.ClassUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// BeanFactory的默认实现
// 拆分了getBean
// 在DefaultBeanFactory 引入SimpleTypeConverter
public class DefaultBeanFactory extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory, BeanDefinitionRegistry {
    // beanDefinitionMap 存放beanID 和 BeanDefinition的映射
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>(64);
    private ClassLoader beanClassLoader;
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    SimpleTypeConverter converter = new SimpleTypeConverter();

    public DefaultBeanFactory() {

    }

    public void addBeanPostProcessor(BeanPostProcessor postProcessor){
        this.beanPostProcessors.add(postProcessor);
    }
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    // 根据beanID获取BeanDefinition
    public BeanDefinition getBeanDefinition(String beanID) {
        return this.beanDefinitionMap.get(beanID);
    }

    public void registerBeanDefinition(String beanID, BeanDefinition bd) {
        this.beanDefinitionMap.put(beanID, bd);
    }

    public Object resolveDependency(DependencyDescriptor descriptor) {
        Class<?> typeToMatch = descriptor.getDependencyType();
        for (BeanDefinition bd : this.beanDefinitionMap.values()) {
            //确保BeanDefinition 有Class对象
            resolveBeanClass(bd);
            Class<?> beanClass = bd.getBeanClass();
            if (typeToMatch.isAssignableFrom(beanClass)) {
                return this.getBean(bd.getID());
            }
        }
        return null;
    }

    /**
     *
     * @param bd
     */
    public void resolveBeanClass(BeanDefinition bd) {
        if (bd.hasBeanClass()) {
            return;
        } else {
            try {
                bd.resolveBeanClass(this.getBeanClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("can't load class:" + bd.getBeanClassName());
            }
        }
    }

    // 根据beanID，通过反射的形式获取类的实例
    public Object getBean(String beanID) {
        BeanDefinition bd = this.getBeanDefinition(beanID);
        if (bd == null) {
            return null;
        }
        if (bd.isSingleton()) {
            Object bean = this.getSingleton(beanID);
            if (bean == null) {
                bean = createBean(bd);
                this.registerSingleton(beanID, bean);
            }
            //使用apache提供的方法
            //populateBeanUseCommonUtils(bd,bean);
            return bean;
        }
        return createBean(bd);
    }

    private void populateBeanUseCommonUtils(BeanDefinition bd, Object bean) {
        List<PropertyValue> pvs = bd.getPropertyValues();
        if (null == pvs || pvs.isEmpty()) {
            return;
        }
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this);
        try {
            for (PropertyValue pv : pvs) {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(originalValue);
                BeanUtils.setProperty(bean, propertyName, resolvedValue);
            }
        } catch (Exception e) {
            throw new BeanCreationException("Failed to obtain BeanInfo for class [" + bd.getBeanClassName() + "]", e);
        }
    }

    private Object createBean(BeanDefinition bd) {
        //创建实例
        Object bean = instantiateBean(bd);
        //设置属性
        populateBean(bd, bean);
        return bean;
    }

    private Object instantiateBean(BeanDefinition bd) {
        if (bd.hasConstructorArgumentValues()) {
            ConstructorResolver resolver = new ConstructorResolver(this);
            return resolver.autowireConstructor(bd);
        } else {
            ClassLoader cl = this.getBeanClassLoader();
            String beanClassName = bd.getBeanClassName();
            try {
                Class<?> clz = cl.loadClass(beanClassName);
                return clz.newInstance();
            } catch (Exception e) {
                throw new BeanCreationException("create bean for " + beanClassName + " failed", e);
            }
        }
    }

    protected void populateBean(BeanDefinition bd, Object bean) {
        for(BeanPostProcessor processor : this.getBeanPostProcessors()){
            if(processor instanceof InstantiationAwareBeanPostProcessor){
                ((InstantiationAwareBeanPostProcessor)processor).postProcessPropertyValues(bean, bd.getID());
            }
        }
        // pvs 是在xml文件中配置的property
        // pds 是PetStoreService中的定义
        // 所以要把pvs和pds进行遍历，(双重循环), 符合要求，则使用set方法进行注入
        List<PropertyValue> pvs = bd.getPropertyValues();

        if (pvs == null || pvs.isEmpty()) {
            return;
        }

        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this);

        try {
            for (PropertyValue pv : pvs) {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(originalValue);

                BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());

                PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    if (pd.getName().equals(propertyName)) {
                        Object convertedValue = converter.convertIfNecessary(resolvedValue, pd.getPropertyType());
                        pd.getWriteMethod().invoke(bean, convertedValue);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            throw new BeanCreationException("Failed to obtain BeanInfo for class [" + bd.getBeanClassName() + "]", ex);
        }
    }

    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    public ClassLoader getBeanClassLoader() {
        return (this.beanClassLoader != null ? this.beanClassLoader : ClassUtils.getDefaultClassLoader());
    }
}