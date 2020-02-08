package org.litespring.test.v1;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.litespring.core.io.ClassPathResource;
import org.litespring.core.io.FileSystemResource;
import org.litespring.core.io.Resource;

// 判断传入的配置文件是否真的存在
// 我们既可以从ClassPathResource读取配置文件，也可以从FileSystemResource配置文件
public class ResourceTest {
    @Test
    public void testClassPathResource() throws Exception {

        Resource r = new ClassPathResource("petstore-v1.xml");

        InputStream is = null;

        try {
            is = r.getInputStream();
            // 注意：这个测试其实并不充分！！
            Assert.assertNotNull(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }

    @Test
    public void testFileSystemResource() throws Exception {

        Resource r = new FileSystemResource("/Users/anapodoton/learning/github/mylitespring/src/test/resources/petstore-v1.xml");
        InputStream is = null;
        try {
            is = r.getInputStream();
            // 注意：这个测试其实并不充分！！
            Assert.assertNotNull(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}