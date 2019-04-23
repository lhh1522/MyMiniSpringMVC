package com.honghao.myspring.demo.service.impl;

/**
 * 核心业务逻辑
 */

import com.honghao.myspring.demo.service.IDemoService;
import com.honghao.myspring.framework.annotation.MyService;

@MyService
public class DemoService implements IDemoService {

	public String get(String name) {
		return "My name is " + name;
	}

}
