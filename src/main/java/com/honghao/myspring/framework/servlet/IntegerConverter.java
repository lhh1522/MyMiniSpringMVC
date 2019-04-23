package com.honghao.myspring.framework.servlet;

public class IntegerConverter implements MyConverter {
    @Override
    public Object convert(String str) {
        return Integer.valueOf(str);
    }
}
