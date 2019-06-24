package com.cisco.adt.data.controllers.nso;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.data.model.nso.NSOServiceModel;
import com.cisco.stbarth.netconf.anc.Netconf.Datastore;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import com.cisco.stbarth.netconf.anc.XMLElement;
import com.cisco.stbarth.netconf.anc.XMLElement.XMLException;

/**
 * Utility methods to perform different operation over netconf using the ANC library
 */
public class ANCNetconfController {

	private static Logger logger = LoggerFactory.getLogger(ANCNetconfController.class);

	public static XMLElement getFromXPath(NetconfSession ncSession, boolean includeOperational, Datastore dataStore,
			String xpath) throws NetconfException {

		//logger.debug("getting config from xpath: " + xpath);

		XMLElement result = null;
		if (includeOperational) {
			result = ncSession.get(xpath);
		} else {
			result = ncSession.getConfig(dataStore, xpath);
		}
		//logger.debug("getFromXPath result: " + result.toXML());



		return result;

	}

	public static XMLElement getFromXPath(NetconfSession ncSession, boolean includeOperational, String xpath)
			throws NetconfException {
		if (includeOperational) {
			return getFromXPath(ncSession, true, null, xpath);
		} else {
			return getFromXPath(ncSession, false, Datastore.RUNNING, xpath);
		}
	}

	public static XMLElement getFromXML(NetconfSession ncSession, boolean includeOperational, String xml)
			throws IOException, XMLException, NetconfException {

		if (includeOperational) {
			return getFromXML(ncSession, true, null, xml);
		} else {
			return getFromXML(ncSession, false, Datastore.RUNNING, xml);
		}

	}

	public static XMLElement getFromXML(NetconfSession ncSession, boolean includeOperational, Datastore dataStore,
			String xml) throws IOException, XMLException, NetconfException {

		//logger.debug("XML filter: " + xml);

		String rootedXMLString = "<root>" + xml + "</root>";

		XMLElement rootedXMLElement = new XMLElement(rootedXMLString);

		ArrayList<XMLElement> xmlElements = new ArrayList<>();

		Stream<XMLElement> xmlElementStream = rootedXMLElement.stream();
		xmlElementStream.forEach((xmlElement) -> {
			xmlElements.add(xmlElement);
		});
		XMLElement result = null;

		if (includeOperational) {
			result = ncSession.get(xmlElements);
		} else {
			result = ncSession.getConfig(dataStore, xmlElements);
		}
		//logger.debug("getFromXML result: " + result.toXML());

		return result;

	}

	public static void sendConfig(NetconfSession ncSession, Datastore dataStore, String xml)
			throws IOException, XMLException, NetconfException {

		//logger.debug("sending config: " + xml);

		XMLElement xmlElement = new XMLElement(xml);
		ncSession.editConfig(dataStore, xmlElement);


	}

	public static void sendConfig(NetconfSession ncSession, String xml)
			throws IOException, XMLException, NetconfException {
		sendConfig(ncSession, Datastore.RUNNING, xml);
	}

	public static XMLElement sendAction(NetconfSession ncSession, String xml)
			throws IOException, XMLException, NetconfException {

		//logger.debug("caling action: " + xml);

		XMLElement xmlElement = new XMLElement(xml);
		XMLElement returnElement = ncSession.tailfAction(xmlElement);
		//logger.debug("sendAction result: " + returnElement.toXML());

		return returnElement;

	}

	public static XMLElement callRPC(NetconfSession ncSession, String xml)
			throws IOException, XMLException, NetconfException {

		//logger.debug("caling rpc: " + xml);

		XMLElement xmlElement = new XMLElement(xml);
		XMLElement returnElement = ncSession.call(xmlElement);
		//logger.debug("callRPC result: " + returnElement.toXML());

		return returnElement;
	}

	public static XMLElement sendActionToNSO(NetconfSession ncSession, NSOServiceModel action) {

		//logger.debug("caling karajan service to send cli through NSO to device");

		StringWriter xml = new StringWriter();
		try {
			JAXBContext context = JAXBContext.newInstance(action.getClass());
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(action, xml);
			//logger.debug("sendActionToNSO action:" + xml.toString());

		} catch (JAXBException e) {
			logger.debug("sendActionToNSO marshalling error:" + e.getMessage());
			return null;
		}

		try {
			XMLElement element = sendAction(ncSession, xml.toString());
			//logger.debug("sendActionToNSO result:" + element.toXML());
			return element;
		} catch (Exception e) {
			logger.debug("sendActionToNSO send action error:" + e.getMessage());
			return null;
		}

	}

}
