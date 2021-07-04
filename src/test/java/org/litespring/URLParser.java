package org.litespring;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 俊语
 * @date 2021/7/4 下午2:46
 */
public class URLParser {
    private HttpServletRequest request;
    public URLParser(HttpServletRequest request){
        this.request=request;
    }
    public void parse(){
        String startRow = request.getParameter("startRow");
        String endRow = request.getParameter("endRow");
    }
}
