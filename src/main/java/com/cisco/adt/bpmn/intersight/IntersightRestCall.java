package com.cisco.adt.bpmn.intersight;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.controllers.intersight.IntersightController;
import com.cisco.adt.data.model.bpmn.TaskResult;
import com.cisco.adt.util.Utils;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Plugin for performing http rest calls using the Java Unirest library to
 * Intersight
 */
public class IntersightRestCall implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(IntersightRestCall.class);


	/**
	 * Method called when task is executed by the process
	 * 
	 * @param execution
	 */
	@Override
	public void execute(DelegateExecution execution) {

		Unirest.config().verifySsl(false);
		
		
		String clientId = (String) execution.getVariable("intersight_clientid");
		String clientSecret = (String) execution.getVariable("intersight_clientsecret");

		String operation = (String) execution.getVariable("operation");
		String path = (String) execution.getVariable("path");
				
		String moid = null;
		if (execution.getVariable("moid") != null) {
			moid = (String) execution.getVariable("moid");
		}

		String payload = null;
		if (execution.getVariable("payload") != null) {
			payload = (String) execution.getVariable("payload");
		}
		
		String parameters = null;
		if (execution.getVariable("parameters") != null) {
			parameters = (String) execution.getVariable("parameters");
		}
		

		String contained = "";
		if (execution.getVariable("contained") != null) {
			contained = (String) execution.getVariable("contained");
		}

		logger.debug("Request: " + path + ", Method: " + operation + ", Test string contained: " + contained);
		TaskResult taskResult = new TaskResult();

		String output = "";

		try {
			switch (operation) {
			case "get_list":
				output = IntersightController.getList(clientId, clientSecret, path, parameters).toString();
				break;
			case "get":
				output = IntersightController.get(clientId, clientSecret, path, moid, parameters).toString();
				break;
			case "post":
				output = IntersightController.post(clientId, clientSecret, path, payload, moid).toString();
				break;
			case "delete":
				output = "" + IntersightController.delete(clientId, clientSecret, path, moid);
				break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			taskResult.setCode(ReturnCodes.ERROR);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			return;
		}
		
		if (contained.length() != 0) {
			boolean testResult = false;
			if (output != null) {
				testResult = output.contains(contained);
			}

			taskResult.setCode(ReturnCodes.OK);
			taskResult.setValue("" + testResult);
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug("Result: " + ReturnCodes.OK + ", " + testResult);
		} else {
			taskResult.setCode(ReturnCodes.OK);
			taskResult.setValue(output);
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug("Result: " + ReturnCodes.OK + ", " + taskResult);
		}

	}

}
