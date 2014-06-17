package com.mahshu.globalcomments;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CommentListActivity extends FragmentActivity
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener,
						SensorEventListener
						{
	
	 private static final long UPDATE_INTERVAL = 5000; //ms
	 private static final int MAX_POST_SEARCH_RESULTS = 20;
	 private static final int TIME_UPDATE_INTERVAL = 1000*60*1;//1min
	
	
	private LocationRequest  lr;
	private LocationClient lc;
	private ListView postsView;
	private Button btnGotoMap;
	private Button btnGotoPost;
	private Location currentLocation;
	private Location lastLocation;
	private boolean doubleBackToExitPressedOnce = false;
	private Handler hTimeUpdateHandler;
	private Runnable mUpdateTimeTask;
	private boolean bTimeUpdateTaskRunning = false;
	private SensorManager mSensorManager;
	private Spinner orgSpin;
	Sensor accelerometer;
	Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
	Float azimuth;
	private long lastUpdateTick;
	
	// Adapter for the Parse query
	private PostListAdapter MyPostListAdapter;
	private ArrayList<GlobalPostVote> MyPostList;
	
	  
	// Fields for the map radius in feet
	private float radius;
	private float lastRadius;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	    MyPostList = new ArrayList<GlobalPostVote>();
	    radius =  GlobalCommentsApplication.getSearchDistance();
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
	    btnGotoPost = (Button)findViewById(R.id.cl_btnGotoPost);
	    
	    btnGotoMap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent nextIntent = new Intent(CommentListActivity.this, MapActivity.class);
				startActivity(nextIntent);
			}
	    });
	    
	    btnGotoPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent postIntent = new Intent(CommentListActivity.this, CreatePostActivity.class);
				startActivity(postIntent);
			}
	    });
	    
	    postsView = (ListView) this.findViewById(R.id.commentListView);
	    MyPostListAdapter = new PostListAdapter(this, postsView, MyPostList, lastRadius, MAX_POST_SEARCH_RESULTS);
	    postsView.setAdapter(MyPostListAdapter);
	    
	    hTimeUpdateHandler = new Handler();
		mUpdateTimeTask = new Runnable()
		{
			@Override 
		     public void run() {
		          UpdateTimeTask();
		          hTimeUpdateHandler.postDelayed(mUpdateTimeTask, TIME_UPDATE_INTERVAL);
		     }
		};
		
		orgSpin = (Spinner) findViewById(R.id.cl_orgSpin);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.organize, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		orgSpin.setAdapter(adapter);
		
		orgSpin.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				doListQuery();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		 	
	}
	
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu; this adds items to the action bar if it is present.
	    getMenuInflater().inflate(R.menu.main_menu, menu);

	    menu.findItem(R.id.action_settings).setOnMenuItemClickListener(new OnMenuItemClickListener() {
	      public boolean onMenuItemClick(MenuItem item) {
	        startActivity(new Intent(CommentListActivity.this, SettingsActivity.class));
	        return true;
	      }
	    });
	    return true;
	  }
	
	private void UpdateTimeTask() {
		if(bTimeUpdateTaskRunning) {
			MyPostListAdapter.updateTimeStamps();
			bTimeUpdateTaskRunning = false;
		}
	}
	
	private void StartTimeUpdateTask() {
		if(!bTimeUpdateTaskRunning){
			mUpdateTimeTask.run();
			bTimeUpdateTaskRunning = true;
		}
	}
	
	private void StopTimeUpdateTask() {
		hTimeUpdateHandler.removeCallbacks(mUpdateTimeTask);
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
      StopTimeUpdateTask();

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
    	if(GlobalCommentsApplication.isAppClosing()) {
    		Log.d("GlobCom", "close app");
    		GlobalCommentsApplication.clearAppClosing();
    		Intent newIntent = new Intent(CommentListActivity.this, LoginActivity.class);
    		startActivity(newIntent);
    		finish();
    	}
        super.onResume();
        // enable accel and mag
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        
        radius =  GlobalCommentsApplication.getSearchDistance();
	    lastRadius = radius;
        
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
        //posts.loadObjects();
    	  MyPostListAdapter.updateLocation(geoPointFromLocation(myLoc));
    	  MyPostListAdapter.updateSearchRadius(radius);
    	  MyPostListAdapter.getDataFromServer();
    	  MyPostListAdapter.setOrganizeBy(orgSpin.getSelectedItem().toString());
    	  //MyPostListAdapter.setOrganizeBy("age");
    	  StartTimeUpdateTask();
        Log.d("GlobCom", "CL updateListView");
      }
    }
    

    
    private void updateVisibleDirectionMarkers(float az)
    {
    	long tick = SystemClock.uptimeMillis();
    	if(tick - lastUpdateTick < 600)
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



