package com.cisco.adt.data.connections;

import java.io.Serializable;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.cisco.adt.data.config.ConfigProperties;
import com.tailf.jnc.NetconfSession;
import com.tailf.jnc.SSHConnection;
import com.tailf.jnc.SSHSession;

@SuppressWarnings("serial")

// Connection to NSO
public class NSOConnection implements Serializable {

	private static NSOConnection instance;

	public static NSOConnection getInstance() {
		// if (instance == null) {
		instance = new NSOConnection();
		// }

		return instance;
	}

	private NSOConnection() {

	}

	public NetconfSession getNetconfSession() {
		return this.getNetconfSession(null);
	}

	public NetconfSession getNetconfSession(DelegateExecution arg0) {

		String nso_host = "";
		String nso_port = "";
		String nso_user = "";
		String nso_pass = "";

		if (arg0 != null) {
			if (arg0.getVariable("nso_host") != null) {
				nso_host = (String) arg0.getVariable("nso_host");
				nso_port = (String) arg0.getVariable("nso_port");
				nso_user = (String) arg0.getVariable("nso_user");
				nso_pass = (String) arg0.getVariable("nso_pass");
			}

		}

		if (nso_host.length() == 0) {

			ConfigProperties configProperties = ConfigProperties.getInstance();
			nso_host = configProperties.getProperty("nso_host");
			nso_port = configProperties.getProperty("nso_port");
			nso_user = configProperties.getProperty("nso_user");
			nso_pass = configProperties.getProperty("nso_pass");

		}
		try {
			SSHConnection c = new SSHConnection(nso_host, Integer.parseInt(nso_port));

			c.authenticateWithPassword(nso_user, nso_pass);
			SSHSession ssh = new SSHSession(c);
			NetconfSession netconfSession = new NetconfSession(ssh);

			return netconfSession;
		} catch (Exception e) {
			e.printStackTrace();
			instance = null;
			return null;
		}

	}

}