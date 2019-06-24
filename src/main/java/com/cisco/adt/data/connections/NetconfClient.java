package com.cisco.adt.data.connections;

import java.io.Serializable;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.bpmn.netconf.NetconfReadConfig;
import com.cisco.adt.data.config.ConfigProperties;
import com.cisco.stbarth.netconf.anc.NetconfException.ProtocolException;
import com.cisco.stbarth.netconf.anc.NetconfSSHClient;
import com.cisco.stbarth.netconf.anc.NetconfSession;

@SuppressWarnings("serial")

/**
 * Builds a netconf session based on the nc_... input variables or on the netconf profile
 * Authentication on nso can be done in 2 ways in the plugin: using specific credentials (nc_host, nc_port, nc_user, nc_pass) as input
 * variables, or using a predefined profile in the netconf-profiles.properties file
 */
public class NetconfClient implements Serializable {

	private static Logger logger = LoggerFactory.getLogger(NetconfReadConfig.class);

	private static NetconfSSHClient getClient(String nc_host, String nc_port, String nc_user, String nc_pass) {
		NetconfSSHClient instance = new NetconfSSHClient(nc_host, Integer.parseInt(nc_port), nc_user);
		instance.setPassword(nc_pass);
		instance.setStrictHostKeyChecking(false);
		return instance;
	}

	private NetconfClient() {
	}

	public static NetconfSession getSession(DelegateExecution arg0) throws ProtocolException {

		String nc_host = "";
		String nc_port = "";
		String nc_user = "";
		String nc_pass = "";

		if (arg0 != null) {
			if (arg0.getVariable("nc_profile") != null) {
				String ncProfile = (String) arg0.getVariable("nc_profile");
				ConfigProperties configProperties = new ConfigProperties();
				nc_host = configProperties.getProperty(ncProfile + "_host");
				nc_port = configProperties.getProperty(ncProfile + "_port", "2022");
				nc_user = configProperties.getProperty(ncProfile + "_user");
				nc_pass = PassCrypt.decrypt(configProperties.getProperty(ncProfile + "_pass"));
			} else if (arg0.getVariable("nc_host") != null) {
				nc_host = (String) arg0.getVariable("nc_host");
				nc_port = (String) arg0.getVariable("nc_port");
				nc_user = (String) arg0.getVariable("nc_user");
				nc_pass = (String) arg0.getVariable("nc_pass");
			}
		}

		if ((nc_host != null) && (nc_host.length() > 0)) {
			return getClient(nc_host, nc_port, nc_user, nc_pass).createSession();
		} else {
			logger.debug("unable to get netconf credentials");
			return null;
		}
	}

}