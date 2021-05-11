package com.cisco.adt.data.controllers.intersight;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

/**
 * Utility methods to perform different operation over rest to intersight
 */
public class IntersightController {
	
	private static long LAST_TOKEN_TIME = 0;
	private static String LAST_REQUEST_TOKEN = "";


    /**
     * Retrieve the token to be used for authenticate API calls. Every token is valid for 10 minutes,
     * so we only re-generate the token if the old token is older than 9 minutes
     * 
     * @param clientId
     * @param clientSecret
     * @return
     */
    public static String authenticate(String clientId, String clientSecret ) {
    	
    	
    	if (LAST_REQUEST_TOKEN.length() > 0) {
    		if (System.currentTimeMillis() - LAST_TOKEN_TIME < 540000) {
    			return LAST_REQUEST_TOKEN;
    		}
    	}
    	
    	JSONObject jsonBody = new JSONObject();
    	jsonBody.put("grant_type", "client_credentials");
    
    	String url = "https://intersight.com/iam/token";
    	
        HttpRequestWithBody getToken = Unirest.post(url);
        HttpResponse<JsonNode> tokenJson = getToken.basicAuth(clientId, clientSecret)
        	.header("Content-Type", "application/json")
        	.body(jsonBody.toString())
        	.asJson();  
       String token = tokenJson.getBody().getObject().get("access_token").toString(); 
       
       LAST_TOKEN_TIME = System.currentTimeMillis();
       LAST_REQUEST_TOKEN = token;
       
       return token;
    }  
    
    public static JSONArray getList(String clientId, String clientSecret, String path, String parameters) throws URISyntaxException {    	
    	String token = authenticate(clientId, clientSecret);
    	
    	if (parameters != null) {
    		if (parameters.startsWith("?")) {
    			parameters = parameters.substring(1);
    		}
    	}

    	
    	URI uri  = new URI("https", "intersight.com", "/api/v1" + path, parameters, null);
    	    	
    	String url = uri.toASCIIString();

    	    	
    	GetRequest call = Unirest.get(url);
        return call.header("Authorization", "Bearer " + token)
        	.asJson().getBody().getArray();
    }
    
    public static JSONObject get(String clientId, String clientSecret, String path, String moid, String parameters) {    	
    	
    	String token = authenticate(clientId, clientSecret);
    	    	
    	String url = "https://intersight.com/api/v1" + path + "/" + moid;
    	
        GetRequest call = Unirest.get(url);
        
        JSONObject responseObject = call.header("Authorization", "Bearer " + token)
            	.asJson().getBody().getObject();

        if ((parameters == null) || !parameters.startsWith("$select=")) {
        	return responseObject;
        }
        
        String keysString = parameters.substring(8);
        String[] keys = keysString.split(",");
        
        JSONObject filteredObject = new JSONObject();
        for (String key : keys) {
        	filteredObject.put(key, responseObject.get(key));
        }
        
        return filteredObject;
        
    }
    
    
    public static JSONObject post(String clientId, String clientSecret, String path, String body, String moid) {    	
    	String token = authenticate(clientId, clientSecret);
    	
    	String url = "https://intersight.com/api/v1" + path;
    	if (moid != null) {
    		url += "/" + moid;
    	}
    	
    	HttpRequestWithBody call = Unirest.post(url);
        return call.header("Authorization", "Bearer " + token)
            	.header("Content-Type", "application/json")
            	.body(body)
            	.asJson().getBody().getObject();
    }
    
    public static boolean delete(String clientId, String clientSecret, String path, String moid) {    	
    	String token = authenticate(clientId, clientSecret);
    	
    	String url = "https://intersight.com/api/v1" + path;
    	if (moid != null) {
    		url += "/" + moid;
    	}
    	
    	HttpRequestWithBody call = Unirest.delete(url);
    	return call.header("Authorization", "Bearer " + token)
            	.header("Content-Type", "application/json")
            	.asEmpty().isSuccess();
    }
	
	
}
