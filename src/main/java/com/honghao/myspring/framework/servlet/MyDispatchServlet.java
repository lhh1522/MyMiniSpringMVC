package com.honghao.myspring.framework.servlet;

import com.honghao.myspring.framework.annotation.MyAutowired;
import com.honghao.myspring.framework.annotation.MyController;
import com.honghao.myspring.framework.annotation.MyRequestMapping;
import com.honghao.myspring.framework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatchServlet extends HttpServlet {
    
    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String, Object> ioc = new HashMap<String, Object>();
    private List<HandlerMapping> handlerMappings = new ArrayList<HandlerMapping>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDisPatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("Status 500; \r\nDetail : \r\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDisPatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HandlerMapping handlerMapping = getHandlerMapping(req);

        if (handlerMapping == null) {
            resp.getWriter().write("Status 404; \r\n接口不存在");
            return;
        }

        Class<?>[] paramTypes = handlerMapping.getParamTypes();
        Object[] paramValues = new Object[paramTypes.length];
        Map<String, String[]> params = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "");
            if (!handlerMapping.getParamIndexMapping().containsKey(entry.getKey())) { continue; }
            int index = handlerMapping.getParamIndexMapping().get(entry.getKey());
            paramValues[index] = ConvertStrategy.convert(paramTypes[index], value);
        }

        if (handlerMapping.getParamIndexMapping().containsKey(HttpServletRequest.class.getName())) {
            int index = handlerMapping.getParamIndexMapping().get(HttpServletRequest.class.getName());
            paramValues[index] = req;
        }

        if (handlerMapping.getParamIndexMapping().containsKey(HttpServletResponse.class.getName())) {
            int index = handlerMapping.getParamIndexMapping().get(HttpServletResponse.class.getName());
            paramValues[index] = resp;
        }

        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(), paramValues);
        if (result == null || result instanceof Void) { return; }
        resp.getWriter().write(result.toString());

    }

    private HandlerMapping getHandlerMapping(HttpServletRequest req) {
        if (handlerMappings.isEmpty()) { return null; }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        for (HandlerMapping handlerMapping: handlerMappings) {
            Matcher matcher = handlerMapping.getUrl().matcher(url);
            if (matcher.matches()) {
                return handlerMapping;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) {
        // 1.加载配置文件
        loadConfig(config.getInitParameter("contextConfigLocation"));
        // 2.扫描相关类
        scanPackage(contextConfig.getProperty("scanPackage"));
        // 3.初始化扫描类，将其new出来后放入ioc容器
        doInstance();
        // 4.自动注入以来：DI
        doAutowired();
        // 5.初始化HandlerMapping
        doHandlerMapping();
    }

    private void doHandlerMapping() {
        if (ioc.isEmpty()) { return; }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Object controller = entry.getValue();
            if (!controller.getClass().isAnnotationPresent(MyController.class)) { continue; }
            String baseUrl = "/";
            if (controller.getClass().isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping baseMap = controller.getClass().getAnnotation(MyRequestMapping.class);
                baseUrl += baseMap.value();
            }

            Method[] methods = controller.getClass().getMethods();
            for (Method method: methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) { continue; }
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String urlRegex = (baseUrl + "/" + requestMapping.value())
                        .replaceAll("/+", "/");
                Pattern urlPattern = Pattern.compile(urlRegex);
                handlerMappings.add(new HandlerMapping(urlPattern, controller, method));

                System.out.println("Mapped :" + urlPattern + "," + method);
            }
        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) { return; }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field: fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) { continue; }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName))
                    beanName = field.getName();
                if (!ioc.containsKey(beanName)) {
                    throw new RuntimeException("不存在" + beanName);
                }
                System.out.println(beanName);

                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void doInstance() {
        if (classNames.isEmpty()) { return; }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    Object instance = clazz.newInstance();
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value().trim();
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    ioc.put(beanName, instance);
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String str) {
        char[] charArray = str.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    private void scanPackage(String scanPackage) {
        URL url = this.getClass().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                scanPackage(scanPackage + "." + file.getName());
            } else  {
                if (!file.getName().endsWith(".class")) { continue; }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                System.out.println(className);
                classNames.add(className);
            }
        }
    }

    private void loadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!(null == is)) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
