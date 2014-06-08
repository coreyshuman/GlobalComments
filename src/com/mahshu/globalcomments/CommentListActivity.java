package com.mahshu.globalcomments;

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
import com.parse.ParseACL;
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
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CommentListActivity extends FragmentActivity
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener
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
	private Button btnGotoMap;
	private EditText SearchRange;
	private Location currentLocation;
	private Location lastLocation;
	private String selectedObjectId;
	
	  // Adapter for the Parse query
	  private ParseQueryAdapter<GlobalPost> posts;
	  
	  // Fields for the map radius in feet
	  private float radius;
	  private float lastRadius;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mapIsLoaded = false;
	    mapIsDoneInit = false;
	    radius = 5000;	//cts debug - hardcode
	    lastRadius = radius;
	    Log.d("GlobCom", "commentlist onCreate");
	    setContentView(R.layout.activity_commentlist);
	    lr = LocationRequest.create();
	    lr.setInterval(UPDATE_INTERVAL);
	    lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    lr.setFastestInterval(1000*60); //cts debug
	    lc = new LocationClient(this.getApplicationContext(), this, this);
	    lc.connect();
	    
	    btnGotoMap = (Button)findViewById(R.id.cl_btnGotoMap);
	    
	    btnGotoMap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent nextIntent = new Intent(CommentListActivity.this, MapActivity.class);
				startActivity(nextIntent);
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
	        TextView contentView = (TextView) view.findViewById(R.id.lv_commentText);
	        TextView usernameView = (TextView) view.findViewById(R.id.lv_usernameText);
	        contentView.setText(post.getText());
	        usernameView.setText(post.getUser().getUsername());
	        return view;
	      }
	    };
	    
	    // Disable automatic loading when the adapter is attached to a view.
	    posts.setAutoload(false);

	    // Disable pagination, we'll manage the query limit ourselves
	    posts.setPaginationEnabled(false);

	    // Attach the query adapter to the view
	    ListView postsView = (ListView) this.findViewById(R.id.commentListView);
	    postsView.setAdapter(posts);	    

	}
 
    private ParseGeoPoint geoPointFromLocation(Location loc) {
        return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
      }
	
	@Override
    public void onLocationChanged(Location loc) {
		currentLocation = loc;
		lastLocation = loc;
		LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
		doListQuery();

	}

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
    	Log.d("GlobCom", "MV connectionFailed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    	Log.d("GlobCom", "MV onConnected");
        lc.requestLocationUpdates(lr, this);

    }

    @Override
    public void onDisconnected() {
    	Log.d("GlobCom", "MV onDisconnect");
    }
    
    /*
     * Set up a query to update the list view
     */
    private void doListQuery() {
      Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
      // If location info is available, load the data
      if (myLoc != null) {
        // Refreshes the list view with new data based
        // usually on updated location data.
        posts.loadObjects();
      }
    }
    
}
