package com.cisco.adt;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;

import com.tailf.conf.Conf;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfXMLParam;
import com.tailf.dp.DpCallbackException;
import com.tailf.maapi.Maapi;
import com.tailf.maapi.MaapiUserSessionFlag;
import com.tailf.navu.NavuAction;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;
import com.tailf.ncs.NcsMain;
import com.tailf.ncs.ns.Ncs;

public class Utils {
	private static Logger LOGGER = Logger.getLogger(Utils.class);

	public enum SecurityType {
		Telnet, SSH;
	}

	public enum DeviceType {
		CLI, Netconf
	}

	public enum NedType {
		CiscoIOS, CiscoIOSXR
	}

	private static int transactionID = -1;
	private static Boolean _readMode;

	public final static String DEVICE_TYPE_CISCO_IOS = "cisco-ios";
	public final static String DEVICE_TYPE_CISCO_IOS_XR = "cisco-ios-xr";
	public final static String DEVICE_TYPE_ALU_SR = "alu-sr";
	public final static String DEVICE_TYPE_JUNOS = "juniper-junos";
	public final static String DEVICE_TYPE_REDBACK_SE = "redback-se";


	public static String getShowCommand(NavuContainer device, String command) throws NavuException {

		String response = "";
		NavuContainer devExec = device.container("live-status").container("ios-stats", "exec");

		NavuAction showCommand = devExec.action("show");
		ConfXMLParam[] result = showCommand.call("<args>" + command + "</args>");

		for (ConfXMLParam x : result) {
			response += x.getValue();
		}
		return response;
	}

	public static String execCommand(NavuContainer device, String deviceType, String command, Boolean configurationMode)
			throws Exception {


		LOGGER.info(command);

		String response = "";
		NavuAction showCommand = null;
		ConfXMLParam[] result;

		switch (deviceType) {
			case DEVICE_TYPE_CISCO_IOS:
				if (configurationMode) {
					showCommand = device.container("config").container("ios", "EXEC").action("exec");
				} else {
					showCommand = device.container("live-status").container("ios-stats", "exec").action("any");
				}

				result = showCommand.call("<args>" + command + "</args>");

				for (ConfXMLParam x : result) {
					response += x.getValue();
				}
				break;
			case DEVICE_TYPE_CISCO_IOS_XR:
				if (configurationMode) {
					showCommand = device.container("config").container("cisco-ios-xr", "EXEC").action("exec");
				} else {
					showCommand = device.container("live-status").container("cisco-ios-xr-stats", "exec").action("any");
				}

				result = showCommand.call("<args>" + command + "</args>");

				for (ConfXMLParam x : result) {
					response += x.getValue();
				}
				break;
			case DEVICE_TYPE_JUNOS:
				showCommand = device.container("rpc").container("jrpc", "rpc-request-shell-execute").action("request-shell-execute");
				result = showCommand.call("<command>" + command + "</command>");

				for (ConfXMLParam x : result) {
					response += x.getValue();
				}
				break;
			case DEVICE_TYPE_ALU_SR:
				showCommand = device.container("live-status").container("alu-stats", "exec").action("any");

				result = showCommand.call("<args>" + command + "</args>");

				for (ConfXMLParam x : result) {
					response += x.getValue();
				}
				break;
			case DEVICE_TYPE_REDBACK_SE:

				showCommand = device.container("live-status").container("redback-se-stats", "exec").action("any");

				result = showCommand.call("<args>" + command + "</args>");

				for (ConfXMLParam x : result) {
					response += x.getValue();
				}
				break;
		}


		LOGGER.info(response);


		return response;
	}

	public static String execCopy(NavuContainer device, String sourcePath, String targetPath) throws NavuException {

		String response = "";
		NavuAction showCommand = device.container("live-status").container("ios-stats", "exec").action("copy");

		ConfXMLParam[] result = showCommand
				.call(String.format("<args>%s %s | prompts ENTER | runn</args>", sourcePath, targetPath));

		for (ConfXMLParam x : result) {
			response += x.getValue();
		}
		System.out.println(response);
		return response;
	}

	public static NavuNode getRootNode(Boolean readOnly, Maapi m) throws IOException, ConfException {

		_readMode = readOnly;
		int mode = readOnly ? Conf.MODE_READ : Conf.MODE_READ_WRITE;
		transactionID = m.startTrans(Conf.DB_RUNNING, mode);
		return new NavuContainer(m, transactionID, Ncs.hash);

	}


	public static NavuNode getOperationalRootNode( Maapi m) throws IOException, ConfException {

		transactionID = m.startTrans(Conf.DB_OPERATIONAL,  Conf.MODE_READ);
		return new NavuContainer(m, transactionID, Ncs.hash);

	}

	public static void finalizeTransaction(Maapi m) throws IOException, ConfException {
		if (transactionID > -1) {

			if (!_readMode)
				m.applyTrans(transactionID, false);

			m.finishTrans(transactionID);
		}
	}

	public static NavuContainer getDevice(String deviceName, NavuNode ncsRoot) throws NavuException {
		NavuList deviceList = ncsRoot.container("ncs", "devices").list("device");
		return deviceList.elem(deviceName);
	}

	public static Maapi getMaapi() throws DpCallbackException, UnknownHostException, IOException {
		Socket socket = new Socket(NcsMain.getInstance().getNcsHost(), NcsMain.getInstance().getNcsPort());
		return getMaapi(socket);
	}

	public static Maapi getMaapi(Socket socket) throws DpCallbackException {
		Maapi maapi = null;
		try {
			maapi = new Maapi(socket);
			maapi.startUserSession("admin", maapi.getSocket().getInetAddress(), "system", new String[] { "" },
					MaapiUserSessionFlag.PROTO_TCP);
		} catch (Exception e) {
			throw new DpCallbackException(e);
		}
		return maapi;
	}

	public static String getDeviceType(String deviceName, NavuNode operationalRoot) throws NavuException {

		NavuNode device = operationalRoot.container("devices").list("device").elem(deviceName);

		List<ConfXMLParam> deviceCapabilitiesList = device.list("capability").encodeValues();

		String deviceCapabilities = deviceCapabilitiesList.toString();


		if (deviceCapabilities.contains("cisco-ios-xr")) {
			return DEVICE_TYPE_CISCO_IOS_XR;
		}
		if (deviceCapabilities.contains("cisco-ios")) {
			return DEVICE_TYPE_CISCO_IOS;
		}
		if (deviceCapabilities.contains("junos")) {
			return DEVICE_TYPE_JUNOS;
		}
		if (deviceCapabilities.contains("alu-sr")) {
			return DEVICE_TYPE_ALU_SR;
		}
		if (deviceCapabilities.contains("redback-se")) {
			return DEVICE_TYPE_REDBACK_SE;
		}
		return null;
	}

}
