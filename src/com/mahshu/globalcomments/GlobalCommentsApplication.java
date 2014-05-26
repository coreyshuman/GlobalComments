package com.mahshu.globalcomments;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

import android.app.Application;
import android.util.Log;

import com.mahshu.globalcomments.PrivateAppData;

public class GlobalCommentsApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		// Add your initialization code here
		Parse.initialize(this, PrivateAppData.PARSE_APP_ID.toString(), PrivateAppData.PARSE_CLIENT_KEY.toString());
		Log.d("GlobCom", "Parse Init");
		Log.d("GlobCom", "Parse App: " .concat(PrivateAppData.PARSE_APP_ID.toString()));

		ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
	    
		// If you would like all objects to be private by default, remove this line.
		defaultACL.setPublicReadAccess(true);
		
		ParseACL.setDefaultACL(defaultACL, true);
		
		// test that parse is working
		ParseObject testObject = new ParseObject("TestObject");
		testObject.put("foo", "bar");
		testObject.saveInBackground();
	}

}

