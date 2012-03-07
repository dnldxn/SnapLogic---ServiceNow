package com.snaplogic.ServiceNow;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.snaplogic.cc.Capabilities;
import org.snaplogic.cc.Capability;
import org.snaplogic.cc.ComponentAPI;
import org.snaplogic.cc.InputView;
import org.snaplogic.cc.OutputView;
import org.snaplogic.cc.prop.SimpleProp;
import org.snaplogic.cc.prop.SimpleProp.SimplePropType;
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

		setPropertyDef("connection", new SimpleProp(
				"ServiceNow Connection Resource URI",
				SimplePropType.SnapString, "Connection resource", true));

		setPropertyDef(
				"query",
				new SimpleProp(
						"ServiceNow Query",
						SimplePropType.SnapString,
						"ServiceNow Encoded Query Strings. See: http://wiki.service-now.com/index.php?title=Embedded:Encoded_Query_Strings",
						true));
	}

	/**
	 * List of JSON fields that can be returned by the ServiceNow server.
	 * http:// wiki.service-now.com/index.php?title=JSON_Web_Service#
	 * JSON_object_format
	 */
	private final String[] serviceNowJsonFields = { "closed_by", "category",
			"escalation", "state", "location", "reassignment_count",
			"time_worked", "order", "due_date", "number", "upon_approval",
			"sla_due", "follow_up", "notify", "business_stc", "caused_by",
			"rejection_goto", "assignment_group", "incident_state",
			"opened_at", "wf_activity", "calendar_duration", "group_list",
			"caller_id", "comments", "priority", "sys_id", "sys_updated_by",
			"variables", "delivery_task", "sys_updated_on", "parent", "active",
			"opened_by", "expected_start", "sys_meta", "watch_list", "company",
			"upon_reject", "work_notes", "sys_created_by", "cmdb_ci",
			"approval_set", "user_input", "sys_created_on", "contact_type",
			"rfc", "approval_history", "activity_due", "severity",
			"subcategory", "work_end", "closed_at", "close_notes",
			"variable_pool", "business_duration", "knowledge", "approval",
			"sys_mod_count", "problem_id", "calendar_stc", "work_start",
			"sys_domain", "sys_response_variables", "correlation_id",
			"sys_class_name", "short_description", "impact", "description",
			"correlation_display", "urgency", "assigned_to", "made_sla",
			"delivery_plan" };

	public void suggestResourceValues(ComponentResourceErr errObj) {

		List<Field> outputViewFields = new ArrayList<Field>();

		// Add JSON fields
		for (String attribute : serviceNowJsonFields) {
			outputViewFields.add(new Field(attribute,
					Field.SnapFieldType.SnapString));
		}

		addRecordOutputViewDef("Output", outputViewFields, "Some Output", true);
	}

	public void execute(Map<String, InputView> inputViews,
			Map<String, OutputView> outputViews) {

		// Get user defined properties
		String connection = getStringPropertyValue("connection");
		String query = getStringPropertyValue("query");

		if (connection == null) {
			throw new SnapComponentException("ServiceNow login details not set");
		}

		// Getting a handle to the connection component and get the credential
		// values.
		ResDef resdef = this.getLocalResourceObject(connection);
		String host = resdef.getPropertyValue("server").toString();
		String username = resdef.getPropertyValue("username").toString();
		String password = resdef.getPropertyValue("password").toString();

		OutputView outputView = outputViews.values().iterator().next();
		Record outRec;

		try {
			URL url = new URL(
					host
							+ "/incident.do?JSON&sysparm_action=getRecords&sysparm_query="
							+ query);
			List incidents = this.getIncidents(url, username, password);

			Iterator i = incidents.iterator(); // iterate over each incident
			while (i.hasNext()) {
				Map<String, String> incident = (Map<String, String>) i.next();
				outRec = outputView.createRecord();

				for (String attributeKey : incident.keySet()) {
					outRec.set(attributeKey, incident.get(attributeKey));
				}
				outputView.writeRecord(outRec);
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

	/**
	 * Helper method
	 * 
	 * @param host
	 * @param query
	 * @param authorizationString
	 * @return
	 * @throws org.codehaus.jackson.JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private List getIncidents(URL url, String username, String password)
			throws org.codehaus.jackson.JsonParseException,
			JsonMappingException, IOException {

		// Open connection to server
		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		String authorizationString = "Basic "
				+ Base64.encodeBytes((username + ":" + password).getBytes());
		urlConn.setRequestProperty("Authorization", authorizationString);

		// read Json data from ServiceNow into Map object
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> userData = mapper.readValue(
				urlConn.getInputStream(), Map.class);

		/**
		 * The following loop is not technically necessary, there should only be
		 * one top-level attribute called "records"
		 */
		List incidents = new ArrayList();
		for (String recordKey : userData.keySet()) {
			incidents.addAll((List) userData.get(recordKey));
			return incidents;
		}

		return incidents;
	}
}
