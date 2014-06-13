package com.mahshu.globalcomments;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Data model for a Vote.
 */
@ParseClassName("Vote")
public class Vote extends ParseObject {
  public String getPostId() {
    return getString("postId");
  }

  public String getType() {
    return getString("type");
  }
  
  public void setType(String value) {
	    put("type", value);
	  }

  public String getUserId() {
    return getString("userId");
  }
  
  public String getObjectId() {
	return getString("objectId");
  }
  

  public static ParseQuery<Vote> getQuery() {
    return ParseQuery.getQuery(Vote.class);
    //cts todo: add query range as input to this function
  }
}
