package com.honghao.myspring.framework.servlet;

import com.honghao.myspring.framework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HandlerMapping {
    private Pattern url;
    private Object controller;
    private Method method;
    private Class<?>[] paramTypes;
    private Map<String,Integer> paramIndexMapping;

    public HandlerMapping(Pattern url, Object controller, Method method) {
        this.url = url;
        this.controller = controller;
        this.method = method;

        paramTypes = method.getParameterTypes();
        paramIndexMapping = new HashMap<String, Integer>();
        initParamIndexMapping(method);
    }

    private void initParamIndexMapping(Method method) {
        Annotation[][] pa = method.getParameterAnnotations();
        for (int i = 0; i < pa.length; i ++) {
            for (Annotation anno: pa[i]) {
                if (anno instanceof MyRequestParam) {
                    String paramName = ((MyRequestParam) anno).value().trim();
                    if (!"".equals(paramName))
                        paramIndexMapping.put(paramName, i);
                }
            }
        }

        if (paramTypes == null) {
            paramTypes = method.getParameterTypes();
        }

        for (int i = 0; i < paramTypes.length; i ++) {
            Class<?> paramType = paramTypes[i];
            if (paramType == HttpServletRequest.class || paramType == HttpServletResponse.class) {
                paramIndexMapping.put(paramType.getName(), i);
            }
        }
    }

    public Pattern getUrl() {
        return url;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }
}
