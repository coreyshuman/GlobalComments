package com.mahshu.globalcomments;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CommentListActivity extends FragmentActivity
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener,
						SensorEventListener
						{
	
	private static final long UPDATE_INTERVAL = 5000; //ms
	 private static final int MAX_SEARCH_DISTANCE = 100; //km
	 private static final int MAX_POST_SEARCH_RESULTS = 20;
	 
	 /*
	  * Constants for handling location results
	  */
	 // Conversion from feet to meters
	 private static final float METERS_PER_FEET = 0.3048f;
	
	 // Conversion from kilometers to meters
	 private static final int METERS_PER_KILOMETER = 1000;
	
	 // Initial offset for calculating the map bounds
	 private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;
	
	 // Accuracy for calculating the map bounds
	 private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;
	
	
	private GoogleMap map;
	 private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
	private LatLng latlng;
	private LocationRequest  lr;
	private LocationClient lc;
	private SupportMapFragment MapFragment;
	private boolean mapIsLoaded;
	private boolean mapIsDoneInit;
	private Marker curLocMarker = null;
	private ListView postsView;
	private Button btnGotoMap;
	private EditText SearchRange;
	private Button SearchUpdate;
	private Location currentLocation;
	private Location lastLocation;
	private String selectedObjectId;
	private boolean doubleBackToExitPressedOnce = false;
	
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
	Float azimuth;
	private long lastUpdateTick;
	
	  // Adapter for the Parse query
	  private ParseQueryAdapter<GlobalPost> posts;
	  
	  // Fields for the map radius in feet
	  private float radius;
	  private float lastRadius;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	    mapIsLoaded = false;
	    mapIsDoneInit = false;
	    radius = 5000;	//cts debug - hardcode
	    lastRadius = radius;
	    lastUpdateTick = 0;
	    Log.d("GlobCom", "commentlist onCreate");
	    setContentView(R.layout.activity_commentlist);
	    lr = LocationRequest.create();
	    lr.setInterval(UPDATE_INTERVAL);
	    lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    lr.setFastestInterval(1000*60); //cts debug
	    lc = new LocationClient(this.getApplicationContext(), this, this);
	    lc.connect();
	    
	    mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    
	    btnGotoMap = (Button)findViewById(R.id.cl_btnGotoMap);
	    
	    btnGotoMap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent nextIntent = new Intent(CommentListActivity.this, MapActivity.class);
				startActivity(nextIntent);
			}
	    });
	    
	    SearchRange = (EditText) findViewById(R.id.cl_distText);
	    SearchUpdate = (Button) findViewById(R.id.cl_updateDistBtn);
	    SearchRange.setText(Float.toString(radius));
	    
	    SearchUpdate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				radius = Float.parseFloat(SearchRange.getText().toString());
	    		if(radius > 100000){
	    			radius = 100000;
	    			SearchRange.setText(Float.toString(radius));
	    		}	   
	    		// CTS DEBUG ----------------------------------
	    		HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("myLocation", geoPointFromLocation(lastLocation));
				params.put("radius", radius * METERS_PER_FEET / METERS_PER_KILOMETER);
				params.put("maxResults", MAX_POST_SEARCH_RESULTS);
				Log.d("GlobCom", "sendrequest");
				ParseCloud.callFunctionInBackground("getGlobalComments", params, new FunctionCallback<Object>() {
					@Override  
					public void done(Object result, ParseException e) {
					    if (e == null) {
					      //Log.d("GlobCom", "click: " + result);
					      
					    }
					    else
					    	Log.d("GlobCom", "click: " + e.getMessage());
					  }
					});	
				// CTS DEBUG END ------------------------------
	    		//doListQuery();				
			}	    	
	    });

	    // Set up a customized query
	    ParseQueryAdapter.QueryFactory<GlobalPost> factory =
	        new ParseQueryAdapter.QueryFactory<GlobalPost>() {
	          public ParseQuery<GlobalPost> create() {
	            Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
	            ParseQuery<GlobalPost> query = GlobalPost.getQuery();
	            query.include("user");
	            query.orderByDescending("createdAt");
	            query.whereWithinKilometers("location", geoPointFromLocation(myLoc), radius
	                * METERS_PER_FEET / METERS_PER_KILOMETER);
	            query.setLimit(MAX_POST_SEARCH_RESULTS);
	            return query;
	          }
	        };

	    // Set up the query adapter
	    posts = new ParseQueryAdapter<GlobalPost>(this, factory) {
	      @Override
	      public View getItemView(GlobalPost post, View view, ViewGroup parent) {
	        if (view == null) {
	          view = View.inflate(getContext(), R.layout.lv_commentlist_item, null);
	        }
	        final TextView contentView = (TextView) view.findViewById(R.id.lv_commentText);
	        final TextView usernameView = (TextView) view.findViewById(R.id.lv_usernameText);
	        final ImageView upvoteView = (ImageView) view.findViewById(R.id.lv_upvoteImage);
	        final TextView upvoteCount = (TextView) view.findViewById(R.id.lv_upvoteText);
	        final ImageView downvoteView = (ImageView) view.findViewById(R.id.lv_downvoteImage);
	        final TextView downvoteCount = (TextView) view.findViewById(R.id.lv_downvoteText);
	        final TextView distanceView = (TextView) view.findViewById(R.id.lv_distanceText);
	        final ImageView directionView = (ImageView) view.findViewById(R.id.lv_dirImage);
	        final TextView dateView = (TextView) view.findViewById(R.id.lv_dateText);
	        
	        

	        //directionView.setRotation((float) calculateLatLngBearing(geoPointFromLocation(currentLocation) ,post.getLocation()));
	        directionView.setTag(Float.valueOf((float)calculateLatLngBearing(geoPointFromLocation(currentLocation) ,post.getLocation())));
	        distanceView.setText(getStringGeoPointDistance(geoPointFromLocation(currentLocation), post.getLocation()));
	        
	        //cts debug - doing the extra queries here SUCKS!!! Need to come back and make this more efficient (use cloud code)
	        ParseQuery<Vote> query = Vote.getQuery();
	        query.whereEqualTo("userId", post.getUser().getObjectId());
	        query.whereEqualTo("postId", post.getObjectId());
	        query.getFirstInBackground(new GetCallback<Vote>() {
	        	@Override
	        	public void done(Vote object, ParseException e) {
	        		Log.d("GlobCom", "get vote done");
	        		if(e != null) {
	        			Log.d("GlobCom", "get vote fail");
	        		} 
	        		else if(object != null){
	        			Log.d("GlobCom", "get vote success");
	        	    	
		        	    	if (object.getType() == "u") {
		        	    		upvoteView.setImageResource(R.drawable.up_arrow);
		        	    	}
		        	    	else if(object.getType() == "d") {
		        	    		downvoteView.setImageResource(R.drawable.down_arrow);
		        	    	}
	        	    	
	        	    }
	        		
	        	  }

	        });

	        
	        
	        contentView.setText(post.getText());
	        usernameView.setText(post.getUser().getUsername() + ":");
	        upvoteCount.setText(Integer.toString(post.getUpVotes()));
	        downvoteCount.setText(Integer.toString(post.getDownVotes()));
	        dateView.setText(post.getUpdatedAt().toString());
	        final String postId = post.getObjectId();
	        upvoteView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
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
						    }
						    else
						    	Log.d("GlobCom", "click: " + e.getMessage());
						  }
						});		
				}
	        });
	        downvoteView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
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
						    }
						    else
						    	Log.d("GlobCom", "click: " + e.getMessage());
						  }
						});		
				}
	        });
	        
	        
	        
	        return view;
	      }
	      
	      
	    };
	    
	    // Disable automatic loading when the adapter is attached to a view.
	    posts.setAutoload(false);

	    // Disable pagination, we'll manage the query limit ourselves
	    posts.setPaginationEnabled(false);

	    // Attach the query adapter to the view
	    postsView = (ListView) this.findViewById(R.id.commentListView);
	    postsView.setAdapter(posts);	    

	}
 
    private ParseGeoPoint geoPointFromLocation(Location loc) {
        return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
      }
    
    /*
     * Called when the Activity is no longer visible at all. Stop updates and disconnect.
     */
    @Override
    public void onStop() {
      // If the client is connected
      if (lc.isConnected()) {
    	  lc.removeLocationUpdates(this); //stop periodic updates
      }

      // After disconnect() is called, the client is considered "dead".
      lc.disconnect();

      super.onStop();
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {
      super.onStart();

      // Connect to the location services client
      lc.connect();
    }
    
    /*
     * Called when the Activity is resumed. Updates the view.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Checks the last saved location to show cached data if it's available
        if (lastLocation != null) {
    	    LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());      
    	  
        }
        // enable accel and mag
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        
        //update list
        doListQuery();
    }
    
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
    
    public void onSensorChanged(SensorEvent event) {
    	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    	      mGravity = event.values.clone();
    	    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
    	      mGeomagnetic = event.values.clone();
    	    if (mGravity != null && mGeomagnetic != null) {
    	      float Rf[] = new float[9];
    	      float If[] = new float[9];
    	      boolean success = SensorManager.getRotationMatrix(Rf, If, mGravity, mGeomagnetic);
    	      if (success) {
    	        float orientation[] = new float[3];
    	        SensorManager.getOrientation(Rf, orientation);
    	        azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll

    	        updateVisibleDirectionMarkers(azimuth);
    	        
    	      }
    	    }
    }
	
	@Override
    public void onLocationChanged(Location loc) {
		currentLocation = loc;
		if (lastLocation != null
		        && geoPointFromLocation(loc)
		            .distanceInKilometersTo(geoPointFromLocation(lastLocation)) < 0.01) {
		      // If the location hasn't changed by more than 10 meters, ignore it.
		      return;
		    }
		Log.d("GlobCom", "CL locationChanged");
		lastLocation = loc;
		
		doListQuery();

	}

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
    	Log.d("GlobCom", "CL connectionFailed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    	Log.d("GlobCom", "CL onConnected");
        lc.requestLocationUpdates(lr, this); //periodic updates

    }

    @Override
    public void onDisconnected() {
    	Log.d("GlobCom", "CL onDisconnect");
    }
    
    /*
     * Set up a query to update the list view
     */
    private void doListQuery() {
      Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
      if (myLoc != null) {
        // Refreshes the list view
        posts.loadObjects();
        Log.d("GlobCom", "CL updateListView");
      }
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
    
    private void updateVisibleDirectionMarkers(float az)
    {
    	long tick = SystemClock.uptimeMillis();
    	if(tick - lastUpdateTick < 1000)
    		return;
    	lastUpdateTick = tick;
    	if(postsView.getChildCount() > 0) {
        	int firstItem = postsView.getFirstVisiblePosition() - postsView.getHeaderViewsCount();
        	int itemCnt = (postsView.getChildCount()-firstItem < 6) ? postsView.getChildCount()-firstItem : 6;
        	int i;
        	for(i=firstItem; i<firstItem+itemCnt; i++)
        	{
    	        View viewItem = (View)postsView.getChildAt(i);
    	        ImageView directionView = (ImageView) viewItem.findViewById(R.id.lv_dirImage);
    	        float offset = (Float)directionView.getTag();
    	        directionView.setRotation(-az*360/(2*3.14159f) + offset);
        	}
        }
    }
    
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;                       
            }
        }, 2000);
    } 
    
}
