package com.cisco.adt.bpmn.netconf;

import java.io.IOException;

import com.cisco.adt.util.Utils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.connections.NetconfClient;
import com.cisco.adt.data.controllers.nso.ANCNetconfController;
import com.cisco.adt.data.model.bpmn.TaskResult;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfException.ProtocolException;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import com.cisco.stbarth.netconf.anc.XMLElement.XMLException;

/**
 *  Plugin for sending configuration data over netconf
 *  The configuzration to be sent is specified as an xml string
 *  Will fill a @{@link TaskResult} object back to the workflow process, containing the result code (OK or not) and a detail in case of error
 */
public class NetconfSendConfig implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(NetconfSendConfig.class);

	/**
	 * Method called when task is executed by the process
	 * As input variables:
	 * - configXml containing the configuration to be sent - as xml string
	 * @param execution
	 */
	@Override
	public void execute(DelegateExecution execution) {

		String configStr = (String) execution.getVariable("configXml");

		logger.debug("Request: " + configStr );

		TaskResult taskResult = new TaskResult();

		NetconfSession ncSession = null;
		try {
			ncSession = NetconfClient.getSession(execution);
			if (ncSession == null) {
				taskResult.setCode(ReturnCodes.ERROR_NC_SESSION);
				execution.setVariableLocal("taskResult", taskResult);
				logger.debug(ReturnCodes.ERROR_NC_SESSION + ", Could not create netconf session");
				return;
			} else {
				ANCNetconfController.sendConfig(ncSession, configStr);
			}
		} catch (ProtocolException e) {
			taskResult.setCode(ReturnCodes.ERROR_NC_PROTOCOL);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug(ReturnCodes.ERROR_NC_PROTOCOL + ", " + Utils.getRootException(e).getMessage());
			return;
		} catch (IOException e) {
			taskResult.setCode(ReturnCodes.ERROR_NC_TRANSPORT);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug(ReturnCodes.ERROR_NC_TRANSPORT + ", " + Utils.getRootException(e).getMessage());
			return;
		} catch (XMLException e) {
			taskResult.setCode(ReturnCodes.ERROR_NC_XML);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug(ReturnCodes.ERROR_NC_XML + ", " + Utils.getRootException(e).getMessage());
			return;
		} catch (NetconfException e) {
			taskResult.setCode(ReturnCodes.ERROR_NC);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug(ReturnCodes.ERROR_NC + ", " + Utils.getRootException(e).getMessage());
			return;
		} finally {
			if (ncSession != null) {
				try {
					ncSession.getClient().close();
				} catch (Exception e) {
				}
			}
		}

		taskResult.setCode(ReturnCodes.OK);
		execution.setVariableLocal("taskResult", taskResult);
		logger.debug(ReturnCodes.OK);

	}

}
