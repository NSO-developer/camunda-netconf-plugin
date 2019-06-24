package com.cisco.adt;

import java.util.Properties;

import com.tailf.conf.ConfBool;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfObject;
import com.tailf.conf.ConfTag;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.ConfXMLParamValue;
import com.tailf.dp.DpActionTrans;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.annotations.ActionCallback;
import com.tailf.dp.annotations.ServiceCallback;
import com.tailf.dp.proto.ActionCBType;
import com.tailf.dp.proto.ServiceCBType;
import com.tailf.dp.services.ServiceContext;
import com.tailf.maapi.Maapi;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuNode;
import org.apache.log4j.Logger;

public class karajanRFS {


	private static Logger LOGGER = Logger.getLogger(karajanRFS.class);


	/**
	 * Create callback method. This method is called when a service instance
	 * committed due to a create or update event.
	 *
	 * This method returns a opaque as a Properties object that can be null. If not
	 * null it is stored persistently by Ncs. This object is then delivered as
	 * argument to new calls of the create method for this service (fastmap
	 * algorithm). This way the user can store and later modify persistent data
	 * outside the service model that might be needed.
	 *
	 * @param context
	 *            - The current ServiceContext object
	 * @param service
	 *            - The NavuNode references the service node.
	 * @param ncsRoot
	 *            - This NavuNode references the ncs root.
	 * @param opaque
	 *            - Parameter contains a Properties object. This object may be used
	 *            to transfer additional information between consecutive calls to
	 *            the create callback. It is always null in the first call. I.e.
	 *            when the service is first created.
	 * @return Properties the returning opaque instance
	 * @throws ConfException
	 */
	@ServiceCallback(servicePoint = "karajan-servicepoint", callType = ServiceCBType.CREATE)
	public Properties create(ServiceContext context, NavuNode service, NavuNode ncsRoot, Properties opaque)
			throws ConfException {
		return opaque;
	}

	/**
	 * cliCommand action implementation
	 */
	@ActionCallback(callPoint = "karajan-cli-command", callType = ActionCBType.ACTION)
	public ConfXMLParam[] cliCommand(DpActionTrans trans, ConfTag name, ConfObject[] kp, ConfXMLParam[] params)
			throws DpCallbackException {
		try {
			String nsPrefix = "karajan";

			String deviceName = "";
			String command = "";
			boolean configMode = false;

			for (ConfXMLParam param : params) {
				switch (param.getTag()) {
				case "device":
					deviceName = param.getValue().toString();
					break;
				case "command":
					command = param.getValue().toString();
					break;
				case "configmode":
					configMode = ((ConfBool) param.getValue()).booleanValue();
					break;
				}
			}

			Maapi maapi = Utils.getMaapi();
			NavuNode ncsRoot = Utils.getRootNode(true, maapi);
			NavuContainer device = Utils.getDevice(deviceName, ncsRoot);
			NavuNode operationalRoot = Utils.getOperationalRootNode(maapi);
			String deviceType = Utils.getDeviceType(deviceName, operationalRoot);

			LOGGER.info(deviceName + " - Device type: " + deviceType);

			if (deviceType == null ) {
				return new ConfXMLParam[] { new ConfXMLParamValue(nsPrefix, "success", new ConfBool(false)),
						new ConfXMLParamValue(nsPrefix, "message", new ConfBuf("Device not supported")) };
			}

			try {
				String response = Utils.execCommand(device, deviceType, command, configMode);
				Utils.finalizeTransaction(maapi);

				if ((response == null) || response.length() == 0) {
					return new ConfXMLParam[] { new ConfXMLParamValue(nsPrefix, "success", new ConfBool(false)),
							new ConfXMLParamValue(nsPrefix, "message", new ConfBuf("Error executing command on device")) };
				}

				return new ConfXMLParam[] { new ConfXMLParamValue(nsPrefix, "success", new ConfBool(true)),
						new ConfXMLParamValue(nsPrefix, "message", new ConfBuf(response)) };
			} catch (Exception ex) {
				return new ConfXMLParam[]{new ConfXMLParamValue(nsPrefix, "success", new ConfBool(false)),
						new ConfXMLParamValue(nsPrefix, "message", new ConfBuf(ex.getMessage()))};
			}

		} catch (Exception e) {
			throw new DpCallbackException(" failed", e);
		}
	}

}
