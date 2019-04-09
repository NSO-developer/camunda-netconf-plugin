package com.cisco.adt.bpmn.rest;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.model.bpmn.TaskResult;
import com.cisco.adt.util.Utils;
import com.cisco.stbarth.netconf.anc.NetconfException;

import kong.unirest.*;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class UnirestHttpRestCall implements JavaDelegate {

    private Logger logger = LoggerFactory.getLogger(UnirestHttpRestCall.class);
    private String httpMethod;
    private Object requestObj;

    @Override
    public void execute(DelegateExecution execution) throws NetconfException {


        Unirest.config().verifySsl(false);

        String reqString = (String) execution.getVariable("reqString");
        String contained = "";
        if (execution.getVariable("contained") != null) {
            contained = (String) execution.getVariable("contained");
        }

        logger.debug("Request: " + reqString + ", Test string contained: "
                + contained);
        TaskResult taskResult = new TaskResult();


        try {

            BufferedReader reader = new BufferedReader(new StringReader(reqString));
            String line = reader.readLine();
            while (line != null) {


                String method = line.substring(0, line.indexOf("("));
                if (method.contains(".")) {
                    method = method.substring(method.indexOf(".") + 1);
                }
                method = method.trim();

                logger.debug("Method: " + method);

                String content = null;
                try {
                    content = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
                } catch (Exception e) {
                }

                if (content.startsWith("\"")) {
                    content = content.substring(1);
                }
                if (content.endsWith("\"")) {
                    content = content.substring(0, content.length() - 1);
                }

                logger.debug("Content: " + content);

                Method unirestMethod = null;

                if (httpMethod == null) {
                    if (!isMethodValid(method)) {
                        taskResult.setCode(ReturnCodes.ERROR);
                        taskResult.setDetail("First line should be the http method call (get, put,...)");
                        execution.setVariableLocal("taskResult", taskResult);
                        return;
                    }
                    httpMethod = method;
                    unirestMethod = Unirest.class.getMethod(method, String.class);
                    requestObj = unirestMethod.invoke(null, content);
                } else {
                    switch (method) {
                        case "header":
                            String[] headerEntry = null;
                            if (content.contains("\", \"")) {
                                headerEntry = content.split("\", \"");
                            } else {
                                headerEntry = content.split("\",\"");
                            }
                            if (isGetMethod(httpMethod)) {
                                requestObj = ((GetRequest) requestObj).header(headerEntry[0], headerEntry[1]);
                            } else {
                                requestObj = ((HttpRequestWithBody) requestObj).header(headerEntry[0], headerEntry[1]);
                            }
                            break;
                        case "body":
                            requestObj = ((HttpRequestWithBody) requestObj).body(content);
                            break;
                    }
                }


                line = reader.readLine();
            }
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
           e.printStackTrace();
            taskResult.setCode(ReturnCodes.ERROR);
            taskResult.setDetail(Utils.getRootException(e).getMessage());
            execution.setVariableLocal("taskResult", taskResult);
            return;
        }

        HttpResponse<String> httpResponse;
        try {
            if (isGetMethod(httpMethod)) {
                httpResponse = ((GetRequest) requestObj).asString();

            } else {
                httpResponse = ((HttpRequestWithBody) requestObj).asString();
            }
            if (contained.length() != 0) {
                boolean testResult = false;
                if (httpResponse.getBody() != null) {
                    testResult = httpResponse.getBody().contains(contained);
                }

                taskResult.setCode(ReturnCodes.OK);
                taskResult.setValue("" + testResult);
                execution.setVariableLocal("taskResult", taskResult);
                logger.debug("Result: " + ReturnCodes.OK + ", " + testResult);
            } else {
                taskResult.setCode("HTTP_" + httpResponse.getStatus());
                taskResult.setDetail(httpResponse.getStatusText());
                taskResult.setValue(httpResponse.getBody());
                execution.setVariableLocal("taskResult", taskResult);
                logger.debug("Result: " + ReturnCodes.OK + ", " + taskResult);
            }

        } catch (UnirestException e) {
            taskResult.setCode(ReturnCodes.ERROR);
            taskResult.setDetail(Utils.getRootException(e).getMessage());
            execution.setVariableLocal("taskResult", taskResult);
            return;        }

    }


    private boolean isGetMethod(String method) {
        switch (method) {
            case "get":
            case "head":
                return true;
        }
        return false;
    }

    private boolean isMethodValid(String method) {
        String[] methods = {"delete", "get", "head", "options", "patch", "post", "put"};
        return Arrays.stream(methods).anyMatch(method::equals);
    }

}
