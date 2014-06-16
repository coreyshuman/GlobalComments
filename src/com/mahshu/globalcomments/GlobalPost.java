package com.mahshu.globalcomments;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Data model for a post.
 */
@ParseClassName("GlobalPost")
public class GlobalPost extends ParseObject {
	public void init() {
		put("upvotes", 0);
		put("downvotes", 0);
	}
	
  public String getText() {
    return getString("text");
  }

  public void setText(String value) {
    put("text", value);
  }

  public ParseUser getUser() {
    return getParseUser("user");
  }

  public void setUser(ParseUser value) {
    put("user", value);
  }

  public ParseGeoPoint getLocation() {
    return getParseGeoPoint("location");
  }

  public void setLocation(ParseGeoPoint value) {
    put("location", value);
  }
  
  public int getUpVotes() {
	  return getInt("upvotes");
  }
  
  public int getDownVotes() {
	  return getInt("downvotes");
  }

  public static ParseQuery<GlobalPost> getQuery() {
    return ParseQuery.getQuery(GlobalPost.class);
    //cts todo: add query range as input to this function
  }
   
}
