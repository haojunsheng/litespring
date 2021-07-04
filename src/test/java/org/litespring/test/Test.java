package org.litespring.test;

import org.easymock.MockControl;
import org.junit.Before;
import org.litespring.URLParser;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 俊语
 * @date 2021/7/4 下午2:45
 */
public class Test {

    @Before
    public void setUp() {

    }

    @org.junit.Test
    public void parseTest() {
        //step 1: 创建mock 对象
        MockControl mockControl = MockControl.createControl(HttpServletRequest.class);
        HttpServletRequest request = (HttpServletRequest) mockControl.getMock();

        //step2: 设置并记录mock对象的行为
        request.getParameter("startRow");
        mockControl.setReturnValue("10");
        request.getParameter("endRow");
        mockControl.setReturnValue("20");

        // step3: 转换为回放模式
        mockControl.replay();

        // step 4: 测试代码
        URLParser parser = new URLParser(request);
        parser.parse();
    }
}
