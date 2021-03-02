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

/**
 *  Plugin for reading configuration/operational data over netconf
 *  The path being read - config only or including operational - can be specified as an xpath string or as an xml filter
 *  Will fill a @{@link TaskResult} object back to the workflow process, containing the result code (OK or not), a detail in case of error,
 *  as well as value containing the result of the read operation in case it was successful.
 *  If a "contained" string is specified, it the task will check if the string is contained in the result and return true/false
 */
public class NetconfReadConfig implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(NetconfReadConfig.class);

	/**
	 * Method called when task is executed by the process
	 * As input variables:
	 * - xpath, containing the xpath to be read or the xml filter
	 * - oper - specifies if operational data should be included or not
	 * - contained - if present, will check if the string is contained in the result
	 *
	 * @param execution
	 */
	@Override
	public void execute(DelegateExecution execution) {

		String reqString = (String) execution.getVariable("xpath");
		boolean includeOperational = false;
		if (execution.getVariable("oper") != null) {
			includeOperational = (Boolean) execution.getVariable("oper");
		}

		String contained = "";
		if (execution.getVariable("contained") != null) {
			contained = (String) execution.getVariable("contained");
		}

		logger.debug("Request: " + reqString + ", Operational: " + includeOperational + ", Test string contained: "
				+ contained);

		TaskResult taskResult = new TaskResult();

		NetconfSession ncSession = null;
		XMLElement configData = null;
		try {
			ncSession = NetconfClient.getSession(execution);
			if (ncSession == null) {
				taskResult.setCode(ReturnCodes.ERROR_NC_SESSION);
				execution.setVariableLocal("taskResult", taskResult);
				logger.debug(ReturnCodes.ERROR_NC_SESSION + ", Could not create netconf session");
				return;
			} else {
				if (reqString.trim().startsWith("<")) {
					configData = ANCNetconfController.getFromXML(ncSession, includeOperational, reqString);
				} else {
					configData = ANCNetconfController.getFromXPath(ncSession, includeOperational, reqString);
				}
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

		if (configData == null) {
			logger.debug("Result: " + ReturnCodes.OK + ", Empty result");
		} else {
			String configString = configData.toXML();
			if (contained.length() != 0) {
				boolean testResult = false;
				if (configString != null) {
					testResult = configString.contains(contained);
				}
				taskResult.setValue("" + testResult);
				logger.debug("Result: " + ReturnCodes.OK + ", " + testResult);
			} else {
				taskResult.setValue(configString);
				logger.debug("Result: " + ReturnCodes.OK + ", " + configString);
			}
		}
		taskResult.setCode(ReturnCodes.OK);
		execution.setVariableLocal("taskResult", taskResult);

	}

}
