package com.honghao.myspring.framework.servlet;

public class DoubleConverter implements MyConverter {
    @Override
    public Object convert(String str) {
        return Double.valueOf(str);
    }
}
