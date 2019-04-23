package com.honghao.myspring.framework.servlet;

import java.util.HashMap;
import java.util.Map;

public class ConvertStrategy {
    private static Map<Class, MyConverter> converterMap = new HashMap<Class, MyConverter>();
    static {
        IntegerConverter integerConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter();
        converterMap.put(Integer.class, integerConverter);
        converterMap.put(int.class, integerConverter);
        converterMap.put(Double.class, doubleConverter);
        converterMap.put(double.class, doubleConverter);
    }

    public static Object convert(Class clazz, String str) {
        if (clazz == String.class) return str;
        if (!converterMap.containsKey(clazz)) throw new RuntimeException("不支持String到" + clazz.getName() +
                "的类型转换");
        return converterMap.get(clazz).convert(str);
    }
}
