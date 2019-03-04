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
import com.cisco.stbarth.netconf.anc.XMLElement;
import com.cisco.stbarth.netconf.anc.XMLElement.XMLException;

public class NetconfSendAction implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(NetconfSendAction.class);

	@Override
	public void execute(DelegateExecution execution) {


		String actionXml = (String) execution.getVariable("actionXml");


		logger.debug("Request: " + actionXml);

		TaskResult taskResult = new TaskResult();

		NetconfSession ncSession = null;
		XMLElement actionResult = null;
		try {
			ncSession = NetconfClient.getSession(execution);
			if (ncSession == null) {
				taskResult.setCode(ReturnCodes.ERROR_NC_SESSION);
				execution.setVariableLocal("taskResult", taskResult);
				logger.debug(ReturnCodes.ERROR_NC_SESSION + ", Could not create netconf session");
				return;
			} else {
				actionResult = ANCNetconfController.sendAction(ncSession, actionXml);
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

		if (actionResult == null) {
			logger.debug("Result: " + ReturnCodes.OK + ", Empty result");
			taskResult.setCode(ReturnCodes.OK);
		} else {
			taskResult.setValue(actionResult.toXML());
			taskResult.setCode(ReturnCodes.OK);
			logger.debug(ReturnCodes.OK + ", " + actionResult.toXML());
		}

		execution.setVariable("taskResult", taskResult);

	}

}
