package com.mahshu.globalcomments;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.util.Log;

import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

public class GlobalPostVote {
	HashMap<String, Object> data;
	SimpleDateFormat dateFormat;
	
	public GlobalPostVote(HashMap<String, Object> _data)
	{
		data = _data;
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
	}
	  public String getText() {
		    return (String)data.get("text");
		  }

		  public ParseUser getUser() {
		    return (ParseUser)data.get("user");
		  }

		  public ParseGeoPoint getLocation() {
		    return (ParseGeoPoint)data.get("location");
		  }
		  
		  public int getUpVotes() {
			  return (Integer)data.get("upvotes");
		  }
		  
		  public int getDownVotes() {
			  return (Integer)data.get("downvotes");
		  }
		  
		  public Date getUpdatedAt() {
			  return (Date)data.get("updatedAt");
		  }
		  
		  public Date getCreatedAt() {
			  return (Date)data.get("createdAt");
		  }
		  
		  public String getObjectId() {
			  return (String)data.get("objectId");
		  }
		  
		  /*
		   * Returns "" - no vote, "u" - upvote, "d" - downvote
		   */
		  public String getVoteType() {
			  return (String)data.get("vote");
		  }
		  
		  // following sets are used to temporarily update UI after casting a vote
		  // this does not modify permanent data
		  public void setVoteType(String v) {
			  data.put("vote", v);
		  }
		  
		  public void setUpVotes(int v) {
			  data.put("upvotes", v);
		  }
		  
		  public void setDownVotes(int v) {
			  data.put("downvotes", v);
		  }
}
