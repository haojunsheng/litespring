package org.litespring.test.v1;

import static org.junit.Assert.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.litespring.beans.BeanDefinition;
import org.litespring.beans.factory.BeanCreationException;
import org.litespring.beans.factory.BeanDefinitionStoreException;
import org.litespring.beans.factory.support.DefaultBeanFactory;
import org.litespring.beans.factory.xml.XmlBeanDefinitionReader;
import org.litespring.core.io.ClassPathResource;
import org.litespring.service.v1.PetStoreService;

public class BeanFactoryTest {
    DefaultBeanFactory factory = null;
    XmlBeanDefinitionReader reader = null;
    @Before
    public void setUp(){
        factory = new DefaultBeanFactory();
        reader = new XmlBeanDefinitionReader(factory);

    }
    /**
     *  测试用例和空的类实现
     *  实现了根据xml文件的beanID生成相应实例的方法
     */
    @Test
    public void testGetBean() {
        reader.loadBeanDefinitions(new ClassPathResource("petstore-v1.xml"));
        BeanDefinition bd = factory.getBeanDefinition("petStore");
        //默认是单例
        assertTrue(bd.isSingleton());

        assertFalse(bd.isPrototype());

        assertEquals(BeanDefinition.SCOPE_DEFAULT,bd.getScope());

        assertEquals("org.litespring.service.v1.PetStoreService",bd.getBeanClassName());
        // 根据beanID获取类的实例
        PetStoreService petStore = (PetStoreService)factory.getBean("petStore");
        assertNotNull(petStore);
        // 判断两次生成的实例是否一样
        PetStoreService petStore1 = (PetStoreService)factory.getBean("petStore");
        assertTrue(petStore.equals(petStore1));
    }

    // 测试建Bean出错时抛出异常
    @Test
    public void testInvalidBean(){
        reader.loadBeanDefinitions(new ClassPathResource("petstore-v1.xml"));
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
            reader.loadBeanDefinitions(new ClassPathResource("xxxx.xml"));
        }catch(BeanDefinitionStoreException e){
            System.out.println(e);
            return;
        }
        Assert.fail("expect BeanDefinitionStoreException ");
    }
}