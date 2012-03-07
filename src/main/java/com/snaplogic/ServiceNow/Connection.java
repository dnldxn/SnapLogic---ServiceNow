package com.snaplogic.ServiceNow;

import java.util.Map;

import org.snaplogic.cc.Capabilities;
import org.snaplogic.cc.Capability;
import org.snaplogic.cc.ComponentAPI;
import org.snaplogic.cc.InputView;
import org.snaplogic.cc.OutputView;
import org.snaplogic.cc.prop.SimpleProp;
import org.snaplogic.cc.prop.SimpleProp.SimplePropType;

public class Connection extends ComponentAPI {

	@Override
	public String getAPIVersion() {
		return "1.0";
	}

	@Override
	public String getComponentVersion() {
		return "1.0";
	}

	public String getLabel() {

		return "ServiceNow Connection";
	}

	public Capabilities getCapabilities() {

		return new Capabilities() {
			{
				put(Capability.INPUT_VIEW_LOWER_LIMIT, 0);
				put(Capability.INPUT_VIEW_UPPER_LIMIT, 0);
				put(Capability.OUTPUT_VIEW_LOWER_LIMIT, 0);
				put(Capability.OUTPUT_VIEW_UPPER_LIMIT, 0);
			}
		};
	}

	public void createResourceTemplate() {

		setPropertyDef("server", new SimpleProp("ServiceNow Server",
				SimplePropType.SnapString, "Server", true));

		setPropertyDef("username", new SimpleProp("ServiceNow Username",
				SimplePropType.SnapString, "Username", true));

		setPropertyDef("password", new SimpleProp("ServiceNow Password",
				SimplePropType.SnapString, "Salesforce password",
				new org.snaplogic.snapi.PropertyConstraint(
						org.snaplogic.snapi.PropertyConstraint.Type.OBFUSCATE,
						0), false));
	}
	
	@Override
	public void execute(Map<String, InputView> arg0,
			Map<String, OutputView> arg1) {
		
	}
}
