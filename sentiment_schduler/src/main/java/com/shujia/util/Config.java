/*********************************************************************
 * 
 * CHINA TELECOM CORPORATION CONFIDENTIAL
 * ______________________________________________________________
 * 
 *  [2015] - [2020] China Telecom Corporation Limited, 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of China Telecom Corporation and its suppliers,
 * if any. The intellectual and technical concepts contained 
 * herein are proprietary to China Telecom Corporation and its 
 * suppliers and may be covered by China and Foreign Patents,
 * patents in process, and are protected by trade secret  or 
 * copyright law. Dissemination of this information or 
 * reproduction of this material is strictly forbidden unless prior 
 * written permission is obtained from China Telecom Corporation.
 **********************************************************************/
package com.shujia.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;


/**
 * 配置文件类
 * @author qinxiao
 * 2016年5月9日
 */
public abstract class Config {
	private static Logger LOGGER = LoggerFactory.getLogger(Config.class);
	// 内置Config存储对象    键值对
	private static Properties ps = new Properties();
	static{
			loadDefaultConfig();
	}
	/**
	 * 加载配置文件<br>
	 * 默认加载default.properpties<br>
	 * 启动参数中指定-c参数时加载外部配置文件覆盖默认配置 <br>
	 * -c [config_file_path]
	 * @throws IOException 
	 */
	private static void loadDefaultConfig(){
		try {
			// 从Classpath中加载默认配置文件
			InputStream is = Config.class.getClassLoader().getResourceAsStream("default.properties");
			if (null == is) {
				LOGGER.warn("[CONFIG-INFO] No File Found: classpath:default.properties");
			}else{
				LOGGER.info("[CONFIG-INFO] Loading default config file: default.properties");	
				ps.load(new InputStreamReader(is, "UTF-8"));
				//解析el表达式
				parseEL(ps);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 加载用户自定义配置文件
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception
	 */
	public static void loadCustomConfig(String fileName){
		try {
			LOGGER.info("[CONFIG-INFO] Loading custom config file: " + fileName);
			ps.load(new InputStreamReader(new FileInputStream(new File(fileName)), "UTF-8"));
			//解析el表达式
			parseEL(ps);
			//写入hadoop conf			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 解析el表达式
	 * @param ps
	 */
	private static void parseEL(Properties ps) {
		for(Entry<Object, Object> entry : ps.entrySet()){
			String value = ps.getProperty((String) entry.getKey());
			while(true){
				int start = value.indexOf("$");
				int end = value.indexOf("}");
				if(start==-1||end==-1){
					break;
				}
				String el = value.substring(start, end+1);
				String param = el.substring(el.indexOf("{")+1, el.indexOf("}")).trim();
				String sproperty = System.getProperty(param);
				String cproperty = ps.getProperty(param);
				if(cproperty!=null){							
					value = value.replace(el, cproperty);
				}else if(sproperty!=null){
					value = value.replace(el, sproperty);
				}else{
					throw new RuntimeException("can not find property "+param+" from System and current Properties");
				}
			}
			ps.setProperty((String) entry.getKey(), value);
		}
	}
	/**
	 * 控制台输出，按key排序所有配置信息
	 */
	public static void dump() {
		LOGGER.info("******************** SYSTEM CONFIGURATION ********************");
		List<String> keys = new ArrayList<String>();
		for (Object key : ps.keySet()) {
			keys.add(key.toString());
		}
		Collections.sort(keys);

		for (Object key : keys) {
			String k = key.toString();
			LOGGER.info(String.format("%-30s: [%s]", k, getString(k)));
		}
		LOGGER.info("********************  CONFIGURATION DUMP  ********************\n");
	}
	/**
	 * 配置数据是否为空
	 * @return
	 */
	public static boolean isEmpty() {
		return ps.isEmpty();
	}
	/**
	 * 设置配置参数
	 */
	public static void set(String key,String value) {
		ps.setProperty(key, value);
	}
	/**
	 * 取String类型配置参数，无配置时输出告警
	 */
	public static String getString(String key) {
		return getString(key, null);
	}
	public static String getString(String key,String defaultValue) {
		//获取key的值
		if (null == key)
			return "";
		String v = ps.getProperty(key,defaultValue);
		if (null != v) {
			return v.trim();
		} else {
			LOGGER.info("[CONFIG-WARN] No configuration found: " + key);
			return "";
		}
	}
	/**
	 * 取Integer类型配置参数
	 */
	public static int getInt(String key) {
		return Integer.valueOf(getString(key));
	}
	/**
	 * 取Integer类型配置参数
	 */
	public static int getInt(String key,String defaultValue) {
		return Integer.valueOf(getString(key,defaultValue));
	}

	/**
	 * 取Boolean类型配置参数
	 */
	public static boolean getBoolean(String key) {
		return Boolean.valueOf(getString(key));
	}
	/**
	 * 取Boolean类型配置参数
	 */
	public static boolean getBoolean(String key,String defaultValue) {
		return Boolean.valueOf(getString(key,defaultValue));
	}

	/**
	 * 取Long类型配置参数
	 */
	public static long getLong(String key) {
		return Long.valueOf(getString(key));
	}
	/**
	 * 取Long类型配置参数
	 */
	public static long getLong(String key,String defaultValue) {
		return Long.valueOf(getString(key,defaultValue));
	}
	/**
	 * 取Float类型配置参数
	 */
	public static float getFloat(String key) {
		return Float.valueOf(getString(key));
	}
	/**
	 * 取Float类型配置参数
	 */
	public static float getFloat(String key,String defaultValue) {
		return Float.valueOf(getString(key,defaultValue));
	}
	/**
	 * 取Float类型配置参数
	 */
	public static double getDouble(String key) {
		return Double.valueOf(getString(key));
	}
	/**
	 * 取Float类型配置参数
	 */
	public static double getDouble(String key,String defaultValue) {
		return Double.valueOf(getString(key,defaultValue));
	}
	/**
	 * 获取约束目录下home目录
	 * @return
	 */
	public static String getDefaultCustomConfigPath(){
		File file = new File(System.getProperty("user.dir"));
		return file.getParentFile().getAbsolutePath()+File.separator+"conf"+File.separator+"default.properties";
	}
}
