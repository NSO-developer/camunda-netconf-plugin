package com.cisco.adt.data.config;

import java.io.FileInputStream;
import java.io.IOException;
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
			load(globalInputStream);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

	}

	public static void reload() {
		instance = new ConfigProperties();
	}

}