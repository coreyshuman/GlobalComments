package com.mahshu.globalcomments;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import android.content.Context;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PostListAdapter extends ArrayAdapter<GlobalPostVote> {
	
	private static final double FEET_PER_KILOMETER = 1.0 / 3281.0;
	
	private final Context context;
	private final ListView parentView;
	private double maxSearchRadiusFt;
	private int maxSearchResults;
	private ParseGeoPoint curLoc;
	private String organizeBy;
	private ArrayList<GlobalPostVote> listData;
	private boolean listDataMux = false;
	public PostListAdapter(Context context, ListView parent, ArrayList<GlobalPostVote> data, double searchRadius, int maxResults) {
		super(context, R.layout.lv_commentlist_item, data);
		this.context = context;
		this.maxSearchRadiusFt = searchRadius;
		this.maxSearchResults = maxResults;
		this.listData = data;
		this.parentView = parent;
		this.organizeBy = "a";
	}
	
	public void updateSearchRadius(double r) {
		this.maxSearchRadiusFt = r;
	}
	
	public void updateLocation(ParseGeoPoint loc) {
		this.curLoc = loc;
	}
	
	public void setOrganizeBy(String s) {
		this.organizeBy = s;
	}
	
	public void getDataFromServer() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("myLocation", this.curLoc);
		params.put("radius", this.maxSearchRadiusFt * FEET_PER_KILOMETER);
		params.put("maxResults", this.maxSearchResults);
		params.put("orderBy", this.organizeBy);
		Log.d("GlobCom", "getDataFromServer");
		listDataMux = true;
		ParseCloud.callFunctionInBackground("getGlobalComments", params, new FunctionCallback<Object>() {
			@Override  
			public void done(Object obj, ParseException e) {
			    if (e == null) {
			    	int i = 0;
			      //Log.d("GlobCom", "click: " + result);
			    	//clear(); //remove all objects from listview
			    	if(!listData.isEmpty())
			    		listData.clear();
			    	@SuppressWarnings("unchecked")
					List<HashMap<String, Object>> result = (List<HashMap<String, Object>>)obj;
			    	for(HashMap<String, Object> data : result)
			    	{
			    		GlobalPostVote post = new GlobalPostVote(data);
			    		listData.add(post);
			    		Log.d("GlobCom", "list add");
			    		//insert(post, getCount());//insert new item
			    	}
			    	notifyDataSetChanged(); //update listview
			    }
			    else
			    	Log.d("GlobCom", "click: " + e.getMessage());
			    
			    listDataMux = false;
			  }
			});	
	}
	
	@Override
	public View getView(int position, View contextView, ViewGroup parent) {
		GlobalPostVote post = listData.get(position);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final int fPos = position;
		final String postId = post.getObjectId();
		final View postView = inflater.inflate(R.layout.lv_commentlist_item, parent, false);
		final TextView contentView = (TextView) postView.findViewById(R.id.lv_commentText);
        final TextView usernameView = (TextView) postView.findViewById(R.id.lv_usernameText);
        final ImageView upvoteView = (ImageView) postView.findViewById(R.id.lv_upvoteImage);
        final TextView upvoteCount = (TextView) postView.findViewById(R.id.lv_upvoteText);
        final ImageView downvoteView = (ImageView) postView.findViewById(R.id.lv_downvoteImage);
        final TextView downvoteCount = (TextView) postView.findViewById(R.id.lv_downvoteText);
        final TextView distanceView = (TextView) postView.findViewById(R.id.lv_distanceText);
        final ImageView directionView = (ImageView) postView.findViewById(R.id.lv_dirImage);
        final TextView dateView = (TextView) postView.findViewById(R.id.lv_dateText);
        
        contentView.setText(post.getText());
        SpannableString username = new SpannableString(post.getUser().getUsername() + ":");
        username.setSpan(new UnderlineSpan(), 0, username.length()-1, 0);
        usernameView.setText(username);
        upvoteCount.setText(Integer.toString(post.getUpVotes()));
        downvoteCount.setText(Integer.toString(post.getDownVotes()));
        dateView.setText(getFormattedTimeString(post.getCreatedAt()));
        directionView.setTag(Float.valueOf((float)calculateLatLngBearing(curLoc ,post.getLocation())));
        distanceView.setText(getStringGeoPointDistance(curLoc, post.getLocation()));
        final String vote = post.getVoteType();
        if (vote.contentEquals("u")) {
    		upvoteView.setImageResource(R.drawable.up_arrow);
    		upvoteView.setTag("u");
    		downvoteView.setTag("u");
    	}
    	else if(vote.contentEquals("d")) {
    		downvoteView.setImageResource(R.drawable.down_arrow);
    		upvoteView.setTag("d");
    		downvoteView.setTag("d");
    	}
    	else {
    		upvoteView.setTag("-");
    		downvoteView.setTag("-");
    	}
        
        upvoteView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(((String)upvoteView.getTag()).contentEquals("-"))
				{
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("userId", ParseUser.getCurrentUser().getObjectId());
					params.put("postId", postId);
					params.put("type", "u");
					Log.d("GlobCom", "sendrequest");
					ParseCloud.callFunctionInBackground("incrementVoteCount", params, new FunctionCallback<String>() {
						  public void done(String result, ParseException e) {
						    if (e == null) {
						      Log.d("GlobCom", "click: " + result);
						      upvoteCount.setText(result);
						      upvoteView.setImageResource(R.drawable.up_arrow);
						      upvoteView.setTag("u");
						      downvoteView.setTag("u");
						      //must update array
						      listData.get(fPos).setUpVotes(Integer.parseInt(result.trim()));
						      listData.get(fPos).setVoteType("u");
						    }
						    else
						    	Log.d("GlobCom", "click: " + e.getMessage());
						  }
						});	
				}
				else
				{
					Toast.makeText(context, "You have already voted on this comment.", Toast.LENGTH_LONG).show();
				}
			}
        });
        downvoteView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if(((String)upvoteView.getTag()).contentEquals("-")) 
				{
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("userId", ParseUser.getCurrentUser().getObjectId());
					params.put("postId", postId);
					params.put("type", "d");
					Log.d("GlobCom", "sendrequest");
					ParseCloud.callFunctionInBackground("incrementVoteCount", params, new FunctionCallback<String>() {
						  public void done(String result, ParseException e) {
						    if (e == null) {
						      Log.d("GlobCom", "click: " + result);
						      downvoteCount.setText(result);
						      downvoteView.setImageResource(R.drawable.down_arrow);
						      upvoteView.setTag("d");
						      downvoteView.setTag("d");
						    //must update array
						      listData.get(fPos).setUpVotes(Integer.parseInt(result.trim()));
						      listData.get(fPos).setVoteType("d");
						    }
						    else
						    	Log.d("GlobCom", "click: " + e.getMessage());
						  }
						});	
				}
				else
				{
					Toast.makeText(context, "You have already voted on this comment.", Toast.LENGTH_LONG).show();
				}
			}
        });
		
		return postView;
	}
	
    /*
     * Return distance between two coordinates in km
     */
    private double calculateGeoPointDistance(ParseGeoPoint pnt1, ParseGeoPoint pnt2)
    {
    	double R = 6371; //km
    	double lr1 = Math.toRadians(pnt1.getLatitude());
    	double lr2 = Math.toRadians(pnt2.getLatitude());
    	double diflat = Math.toRadians(pnt2.getLatitude() - pnt1.getLatitude());
    	double diflon = Math.toRadians(pnt2.getLongitude() - pnt1.getLongitude());
    	double a = Math.sin(diflat/2) * Math.sin(diflat/2) +
    			Math.cos(lr1) * Math.cos(lr2) *
    			Math.sin(diflon/2) * Math.sin(diflon/2);
    	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    	double dist = R * c;
    	return dist;
    }
    
    /*
     * Return bearing between two coordinates in degrees
     */
    private double calculateLatLngBearing(ParseGeoPoint pnt1, ParseGeoPoint pnt2)
    {
    	double latr1 = Math.toRadians(pnt1.getLatitude());
    	double latr2 = Math.toRadians(pnt2.getLatitude());
    	double lonr1 = Math.toRadians(pnt1.getLongitude());
    	double lonr2 = Math.toRadians(pnt2.getLongitude());
    	double y = Math.sin(lonr2 - lonr1) * Math.cos(latr2);
    	double x = Math.cos(latr1)*Math.sin(latr2) -
    			Math.sin(latr1)*Math.cos(latr2)*Math.cos(lonr2 - lonr1);
    	double bearing = Math.toDegrees(Math.atan2(y,x));
    	return bearing;
    }
    
    /*
     * Return STRING distance between two coordinates in miles or feet
     */
    private String getStringGeoPointDistance(ParseGeoPoint pnt1, ParseGeoPoint pnt2)
    {
    	NumberFormat formatter = NumberFormat.getNumberInstance();
    	double dist=calculateGeoPointDistance(pnt1, pnt2)*0.621371;
    	String distStr;
    	if(dist < 0.189394)
    	{
    		dist = dist * 5280;
    		formatter.setMaximumFractionDigits(0);
    		distStr = formatter.format(dist) + "ft";
    	}
    	else
    	{
    		formatter.setMaximumFractionDigits(1);
    		distStr = formatter.format(dist) + "mi";
    	}
    	return distStr;
    }
    
    
    private String getFormattedTimeString(Date date) {
    	String ret = "";
    	Date now = new Date();
    	long diff = now.getTime() - date.getTime();
    	int i = 10;
    	
    	if(diff < 1000) {
    		ret = "just now";
    	} else if(diff < (1000*60)) {
    		ret = "<1 min ago";
    	} else if(diff < (1000*60*60)) {
    		i = (int) (diff/(1000*60));
    		ret = Integer.toString(i)+ " min ago";
    	} else if(diff < (100*60*60*24)) {
    		i = (int) (diff/(1000*60*60));
    		ret = Integer.toString(i)+ " hr ago";
    	} else {
    		//i = Calendar.getInstance(Calendar.ZONE_OFFSET) + Calendar.getInstance(Calendar.DST_OFFSET) / 60000;
    		SimpleDateFormat sformat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
    		ret = sformat.format(date);
    	} 	
    	return ret;
    }
    
    public void updateTimeStamps() {
    	int i;
    	if(listDataMux)
    		return;
    	Log.d("GlobCom", "updateTime");
    	for(i=0 ; i<listData.size(); i++) {
    		View viewItem = (View)parentView.getChildAt(i);
    		if(viewItem != null) {
	    		GlobalPostVote post = listData.get(i);
	    		final TextView dateView = (TextView) viewItem.findViewById(R.id.lv_dateText);
	    		dateView.setText(getFormattedTimeString(post.getCreatedAt()));
    		}
    	}
    }
}