package com.mahshu.globalcomments;

// This file allows you to include private application data in your project without exposing them in GitHub.
// Change the name of this Enum to "PrivateAppData" and add your private info below as needed.
// Make sure that this file is NOT included when you push updates to GitHub.


// CTS TODO: add private key obfuscation
public enum PrivateAppData_Template {
	PARSE_APP_ID("YOUR_PARSE_APPLICATION_ID"),
	PARSE_CLIENT_KEY("YOUR_PARSE_CLIENT_KEY");
	
	private String strVal;
	private PrivateAppData_Template(String toString) {
		strVal = toString;
	}
	
	@Override
	public String toString() {
		return strVal;
	}
}
