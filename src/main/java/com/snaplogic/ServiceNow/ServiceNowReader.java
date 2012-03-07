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
import org.snaplogic.common.ComponentResourceErr;
import org.snaplogic.common.Field;
import org.snaplogic.common.Record;
import org.snaplogic.common.exceptions.SnapComponentException;
import org.snaplogic.snapi.ResDef;
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
		return "ServiceNow Reader";
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
			}
		};
	}

	@Override
	public void createResourceTemplate() {
		// Initialize User Defined Properties
		
		setPropertyDef("connection", new SimpleProp("ServiceNow Connection Resource URI",
				SimplePropType.SnapString, "Connection resource", true));
		
		setPropertyDef("query", new SimpleProp("ServiceNow Query",
				SimplePropType.SnapString, "ServiceNow Query", true));

		// Initialize Input View
		//ArrayList<Field> infields = new ArrayList<Field>();
		//infields.add(new Field("Domain", Field.SnapFieldType.SnapString, "Domain Name"));
		//addRecordInputViewDef("Input", infields, "Default Input View", false);
		
		// Initialize Output View
		//ArrayList<Field> fields = new ArrayList<Field>();
		//fields.add(new Field("active", Field.SnapFieldType.SnapString,
		//		"Active"));
		//fields.add(new Field("number", Field.SnapFieldType.SnapNumber,
			//	"ID"));
		//addRecordOutputViewDef("Output", fields, "Default Output View", false);
	}
	
	public void suggestResourceValues(ComponentResourceErr errObj) {
		List<Field> outputViewFields = new ArrayList<Field>();
		
		//loop add fields
		outputViewFields.add(new Field("active", Field.SnapFieldType.SnapString));
		outputViewFields.add(new Field("activity_due", Field.SnapFieldType.SnapString));
		outputViewFields.add(new Field("approval", Field.SnapFieldType.SnapString));
		outputViewFields.add(new Field("assigned_to", Field.SnapFieldType.SnapString));
		outputViewFields.add(new Field("number", Field.SnapFieldType.SnapString));
		
		addRecordOutputViewDef("Output", outputViewFields, "Some Output", true);
	}

	public void execute(Map<String, InputView> inputViews, Map<String, OutputView> outputViews) {

		String connection = getStringPropertyValue("connection");
		String queryString = getStringPropertyValue("query");
		
		if (connection == null) {
			throw new SnapComponentException("ServiceNow login details not set");
		}
		
		//Getting a handle to the connection component with the credentials and get those values.
		ResDef resdef = this.getLocalResourceObject(connection);
		String host = resdef.getPropertyValue("server").toString();
		String username = resdef.getPropertyValue("username").toString();
		String password = resdef.getPropertyValue("password").toString();
		
		
		//String host = "https://snaplogic.service-now.com/incident.do?JSON&";
		//String queryString = "/incident.do?JSON&sysparm_action=getRecords&sysparm_query=active=true^category=hardware";
		String authorizationString = "Basic "
				+ Base64.encodeBytes((username+":"+password).getBytes());
		
		URL url;
		OutputView outputView = outputViews.values().iterator().next();
		Record outRec;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			url = new URL(host + queryString);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			urlConn.setRequestProperty("Authorization", authorizationString);
			
			//read Json data from ServiceNow into Map object
			Map<String,Object> userData = mapper.readValue(urlConn.getInputStream(), Map.class);
			
			for( String recordKey : userData.keySet() ) { 	// should only have one top-level record attribute
				List incidents = (List)userData.get(recordKey);
				Iterator i = incidents.iterator();  //iterate over each incident
				while(i.hasNext()) {
					Map<String, String> incident = (Map<String, String>)i.next();
					outRec = outputView.createRecord();
					
					for( String attributeKey : incident.keySet() ) {
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
		
		outputView.completed();
	}
}
