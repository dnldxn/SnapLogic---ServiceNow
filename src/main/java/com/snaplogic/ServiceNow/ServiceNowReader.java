package com.snaplogic.ServiceNow;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.snaplogic.cc.Capabilities;
import org.snaplogic.cc.Capability;
import org.snaplogic.cc.ComponentAPI;
import org.snaplogic.cc.InputView;
import org.snaplogic.cc.OutputView;
import org.snaplogic.cc.prop.SimpleProp;
import org.snaplogic.cc.prop.SimpleProp.SimplePropType;
import org.snaplogic.codehaus.jackson.JsonFactory;
import org.snaplogic.codehaus.jackson.JsonParseException;
import org.snaplogic.common.Field;
import org.snaplogic.common.Record;
import org.snaplogic.util.Base64;

public class ServiceNowReader extends ComponentAPI {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAPIVersion() {
		return "1.0";
	}

	public String getLabel() {
		return "Binary XML Validator";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComponentVersion() {
		return "1.0";
	}

	@Override
	public Capabilities getCapabilities() {
		return new Capabilities() {
			{
				put(Capability.INPUT_VIEW_LOWER_LIMIT, 0);
				put(Capability.INPUT_VIEW_UPPER_LIMIT, 0);
				put(Capability.OUTPUT_VIEW_LOWER_LIMIT, 1);
				put(Capability.OUTPUT_VIEW_UPPER_LIMIT, 1);
				put(Capability.INPUT_VIEW_ALLOW_BINARY, true);
			}
		};
	}

	@Override
	public void createResourceTemplate() {
		// Initialize User Defined Properties
		setPropertyDef("xsdfilename", new SimpleProp("XSD File name",
				SimplePropType.SnapString, "The URI of the XSD file", null,
				true));

		// Initialize Input View
		//ArrayList<Field> infields = new ArrayList<Field>();
		//infields.add(new Field("Domain", Field.SnapFieldType.SnapString, "Domain Name"));
		//addRecordInputViewDef("Input", infields, "Default Input View", false);
		
		// Initialize Output View
		ArrayList<Field> fields = new ArrayList<Field>();
		fields.add(new Field("active", Field.SnapFieldType.SnapString,
				"Active"));
		fields.add(new Field("number", Field.SnapFieldType.SnapNumber,
				"ID"));
		addRecordOutputViewDef("Output", fields, "Default Output View", false);
	}

	public void execute(Map<String, InputView> inputViews, Map<String, OutputView> outputViews) {

		debug("Starting execute...");
		
		String host = "https://snaplogic.service-now.com/incident.do?JSON&";
		String urlStringRead = "sysparm_action=getRecords&sysparm_query=active=true^category=hardware";
		
		JsonFactory f = new JsonFactory();
		URL url;
		
		OutputView outputView = outputViews.values().iterator().next();
		Record outRec;
		
		try {
			url = new URL(host + urlStringRead);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			String authorizationString = "Basic "
					+ Base64.encodeBytes("admin:admin".getBytes());
			urlConn.setRequestProperty("Authorization", authorizationString);
			
			ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
			Map<String,Object> userData = mapper.readValue(urlConn.getInputStream(), Map.class);
			
			for( String recordKey : userData.keySet() ) { 	// should only have one top-level record attribute
				List incidents = (List)userData.get(recordKey);
				Iterator i = incidents.iterator();  //iterate over each incident
				while(i.hasNext()) {
					Map<String, String> incident = (Map<String, String>)i.next();
					outRec = outputView.createRecord();
					for( String attributeKey : incident.keySet() ) {
						//Object attributeValue = incident.get(attributeKey);
						
						outRec.set(attributeKey, incident.get(attributeKey));
						
					}
					outputView.writeRecord(outRec);
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		debug("Finished Validating...");
	}
}
