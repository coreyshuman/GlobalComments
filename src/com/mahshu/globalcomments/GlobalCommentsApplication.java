package com.mahshu.globalcomments;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mahshu.globalcomments.PrivateAppData;

public class GlobalCommentsApplication extends Application {
	
	private static final String SEARCH_DISTANCE = "search_distance";
	
	private static SharedPreferences preferences;
	private static boolean appClosing = false;
	
	@Override
	public void onCreate() {
		super.onCreate();

		GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		
		// initialize subclasses
		ParseObject.registerSubclass(GlobalPost.class);
		ParseObject.registerSubclass(Vote.class);
		
		// Add your initialization code here
		Parse.initialize(this, PrivateAppData.PARSE_APP_ID.toString(), PrivateAppData.PARSE_CLIENT_KEY.toString());
		preferences = getSharedPreferences("com.parse.anywall", Context.MODE_PRIVATE);

		ParseACL defaultACL = new ParseACL();
	    
		// If you would like all objects to be private by default, remove this line.
		defaultACL.setPublicReadAccess(true);
		
		ParseACL.setDefaultACL(defaultACL, true);
//		ParseUser.logOut();//cts debug
		
	}
	
	public static float getSearchDistance() {
		return preferences.getFloat(SEARCH_DISTANCE, 2000);
	}

	public static void setSearchDistance(float value) {
		preferences.edit().putFloat(SEARCH_DISTANCE, value).commit();
	}

	public static boolean isAppClosing() {
		return appClosing;
	}
	
	public static void setAppClosing() {
		appClosing = true;
	}
	
	public static void clearAppClosing() {
		appClosing = false;
	}
}

