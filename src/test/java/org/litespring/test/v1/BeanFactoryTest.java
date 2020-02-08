package org.litespring.test.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Assert;
import org.junit.Test;
import org.litespring.beans.BeanDefinition;
import org.litespring.beans.factory.BeanCreationException;
import org.litespring.beans.factory.BeanDefinitionStoreException;
import org.litespring.beans.factory.BeanFactory;
import org.litespring.beans.factory.support.DefaultBeanFactory;
import org.litespring.service.v1.PetStoreService;

public class BeanFactoryTest {
    /**
     *  测试用例和空的类实现
     *  实现了根据xml文件的beanID生成相应实例的方法
     */
    @Test
    public void testGetBean() {
        // 从xml文件实例化工厂
        BeanFactory factory = new DefaultBeanFactory("petstore-v1.xml");
        // 根据beanID获取BeanDefinition
        BeanDefinition bd = factory.getBeanDefinition("petStore");

        assertEquals("org.litespring.service.v1.PetStoreService",bd.getBeanClassName());
        // 根据beanID获取类的实例
        PetStoreService petStore = (PetStoreService)factory.getBean("petStore");

        assertNotNull(petStore);
    }

    // 测试建Bean出错时抛出异常
    @Test
    public void testInvalidBean(){

        BeanFactory factory = new DefaultBeanFactory("petstore-v1.xml");
        try{
            factory.getBean("invalidBean");
        }catch(BeanCreationException e){
            System.out.println(e);
            return;
        }
        Assert.fail("expect BeanCreationException ");
    }
    // 测试 读取XML文件出错时抛出异常
    @Test
    public void testInvalidXML(){

        try{
            new DefaultBeanFactory("xxxx.xml");
        }catch(BeanDefinitionStoreException e){
            System.out.println(e);
            return;
        }
        Assert.fail("expect BeanDefinitionStoreException ");
    }
}