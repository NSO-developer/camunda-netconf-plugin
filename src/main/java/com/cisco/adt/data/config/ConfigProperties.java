package com.cisco.adt.data.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

@SuppressWarnings("serial")

// Config access Class
public class ConfigProperties extends Properties implements Serializable {

	private static ConfigProperties instance;

	public static ConfigProperties getInstance() {
		if (instance == null) {
			instance = new ConfigProperties();
		}

		return instance;
	}

	private ConfigProperties() {
		try {
			InputStream globalInputStream = new FileInputStream(
					System.getenv("CATALINA_HOME") + "/webapps/karajan/WEB-INF/classes/config.properties");
			System.out.println(System.getenv("CATALINA_HOME") + "/webapps/karajan/WEB-INF/classes/config.properties");
			load(globalInputStream);
		} catch (Exception ex) {
			InputStream globalInputStream;
			try {
				globalInputStream = new FileInputStream("/camunda/webapps/karajan/WEB-INF/classes/config.properties");
				load(globalInputStream);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void reload() {
		instance = new ConfigProperties();
	}

}