package com.cisco.adt.data.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")

// Config access Class
public class ConfigProperties extends Properties implements Serializable {

	private static Logger logger = LoggerFactory.getLogger(ConfigProperties.class);

	public ConfigProperties() {
		// String rootPath = ConfigProperties.class.getResource("/").getPath();
		// String filePath = rootPath + "../conf/netconf-profiles.properties";

		String filePath = "/app/netconf-profiles.properties";

		try {

			InputStream globalInputStream = new FileInputStream(filePath);
			logger.debug("Reading netconf properties from file " + filePath);
			load(globalInputStream);
		} catch (Exception ex) {
			logger.info("Reading netconf properties from file error" + ex.getMessage());
		}

	}

}