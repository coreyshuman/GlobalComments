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
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.SensorManager;
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
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends FragmentActivity
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener
						{
	
	private static final long UPDATE_INTERVAL = 5000; //ms
	 private static final int MAX_SEARCH_DISTANCE = 100; //km
	 private static final int MAX_MAP_SEARCH_RESULTS = 20;
	 
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
	private Button btnGotoPost;
	private EditText SearchRange;
	private Location currentLocation;
	private Location lastLocation;
	private String selectedObjectId;
	
	// Represents the circle around a map
	  private Circle mapCircle;

	  // Fields for the map radius in feet
	  private float radius;
	  private float lastRadius;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mapIsLoaded = false;
	    mapIsDoneInit = false;
	    radius = 1000;	//cts debug - hardcode
	    lastRadius = radius;
	    Log.d("GlobCom", "MapView onCreate");
	    setContentView(R.layout.activity_mapview);
	    lr = LocationRequest.create();
	    lr.setInterval(UPDATE_INTERVAL);
	    lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    lr.setFastestInterval(1000*60); //cts debug
	    lc = new LocationClient(this.getApplicationContext(), this, this);
	    lc.connect();
	    
	    try {
            // Loading map
            initilizeMap();
 
        } catch (Exception e) {
            e.printStackTrace();
        }
	    
	    if(mapIsLoaded){
	        map.getUiSettings().setAllGesturesEnabled(true);
	        map.getUiSettings().setMyLocationButtonEnabled(true);
	        map.setMyLocationEnabled(true);
	        map.getUiSettings().setZoomControlsEnabled(true);
	    }
	    
	    btnGotoPost = (Button)findViewById(R.id.mv_postBtn);
	    SearchRange = (EditText)findViewById(R.id.mv_searchRange);
	    SearchRange.setText(Float.toString(radius));
	    
	    btnGotoPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent postIntent = new Intent(MapActivity.this, CreatePostActivity.class);
				startActivity(postIntent);
			}
	    });

	    SearchRange.addTextChangedListener(new TextWatcher() {
	    	public void afterTextChanged(Editable s) {
	    		if(s.length() > 0)
	    			radius = Float.parseFloat(s.toString());
	    		if(radius > 100000){
	    			radius = 100000;
	    			SearchRange.setText(Float.toString(radius));
	    		}
	    		
	    		Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
	    		LatLng pos = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
	    		updateCircle(pos);
	    		
	    	}
	    	
	    	public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
	}

    private void initilizeMap() {
        if (MapFragment == null) {
        	MapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        	map = MapFragment.getMap();
 
            // check if map is created successfully or not
            if (map == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_LONG)
                        .show();
            }
            else
            {
            	mapIsLoaded = true;
            
            }
        }
    }
 
    private ParseGeoPoint geoPointFromLocation(Location loc) {
        return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
      }
	
	@Override
    public void onLocationChanged(Location loc) {
		currentLocation = loc;
		lastLocation = loc;
		LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
		if(mapIsLoaded && !mapIsDoneInit){
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, 15);
	        map.animateCamera(cameraUpdate);
	        mapIsDoneInit = true;
		}
		if(mapIsLoaded){
			updateCircle(pos);
			updateMap();
		}
/*
		if(mapIsLoaded){
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, 15);
	        map.animateCamera(cameraUpdate);
	        if(curLocMarker == null){
		     // create marker
		        MarkerOptions marker = new MarkerOptions().position(pos).title("You");
		     // Changing marker icon
		        //marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));
		        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		        // adding marker
		        curLocMarker = map.addMarker(marker);
	        }
	        else
	        {
	        	// update position
	        	curLocMarker.setPosition(pos);
	        }
*/
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
    	    updateCircle(myLatLng);
        }

        updateMap();
    }
    
    
    private void updateMap() {
    	
    	Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
        if (myLoc == null) {
            cleanUpMarkers(new HashSet<String>()); 
            return;
          }
        
    	final ParseGeoPoint curPoint = geoPointFromLocation(myLoc);
        ParseQuery<GlobalPost> mapQuery = GlobalPost.getQuery();
        // Set up additional query filters
        mapQuery.whereWithinKilometers("location", curPoint, MAX_SEARCH_DISTANCE);
        mapQuery.include("user");
        mapQuery.orderByDescending("createdAt"); //cts todo - other search by options (age, distance, popularity)
        mapQuery.setLimit(MAX_MAP_SEARCH_RESULTS);    	
        mapQuery.findInBackground(new FindCallback<GlobalPost>() {
            @Override
            public void done(List<GlobalPost> objects, ParseException e) {
                if (e != null) {
                    if (true) {//cts todo - add debug switch
                      Log.d("GlobCom", "An error occurred while querying for map posts.", e);
                    }
                    return;
                }
                // Posts to show on the map
                Set<String> toKeep = new HashSet<String>();
                // Loop through the results of the search
                for (GlobalPost post : objects) {
                  // Add this post to the list of map pins to keep
                  toKeep.add(post.getObjectId());
                  // Check for an existing marker for this post
                  Marker oldMarker = mapMarkers.get(post.getObjectId());
                  // Set up the map marker's location
                  MarkerOptions markerOpts =
                      new MarkerOptions().position(new LatLng(post.getLocation().getLatitude(), post
                          .getLocation().getLongitude()));
                  // Set up the marker properties based on if it is within the search radius
                  if (post.getLocation().distanceInKilometersTo(curPoint) > radius * METERS_PER_FEET
                      / METERS_PER_KILOMETER) {
                    // Check for an existing out of range marker
                    if (oldMarker != null) {
                      if (oldMarker.getSnippet() == null) {
                        // Out of range marker already exists, skip adding it
                        continue;
                      } else {
                        // Marker now out of range, needs to be refreshed
                        oldMarker.remove();
                      }
                    }
                    // Display a red marker with a predefined title and no snippet
                    markerOpts =
                        markerOpts.title(getResources().getString(R.string.post_tooFar)).icon(
                        		BitmapDescriptorFactory.fromResource(R.drawable.marker_red));
                  } else {
                    // Check for an existing in range marker
                    if (oldMarker != null) {
                      if (oldMarker.getSnippet() != null) {
                        // In range marker already exists, skip adding it
                        continue;
                      } else {
                        // Marker now in range, needs to be refreshed
                        oldMarker.remove();
                      }
                    }
                    // Display a green marker with the post information
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm");
                    markerOpts =
                        markerOpts.title(post.getText()).snippet(post.getUser().getUsername() + " " + sdf.format(post.getCreatedAt()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_green));
                  }
                  // Add a new marker
                  Marker marker = map.addMarker(markerOpts);
                  mapMarkers.put(post.getObjectId(), marker);
                  if (post.getObjectId().equals(selectedObjectId)) {
                    marker.showInfoWindow();
                    selectedObjectId = null;
                  }
                }
                // Clean up old markers.
                cleanUpMarkers(toKeep);
            }
        });
            
    }
    
	private void cleanUpMarkers(Set<String> markersToKeep) {
		for (String objId : new HashSet<String>(mapMarkers.keySet())) {
			if (!markersToKeep.contains(objId)) {
			    Marker marker = mapMarkers.get(objId);
			    marker.remove();
			    mapMarkers.get(objId).remove();
			    mapMarkers.remove(objId);
			}
		}
	}

	/*
	 * Displays a circle on the map representing the search radius
	 */
	private void updateCircle(LatLng myLatLng) {
	  if (mapCircle == null) {
	    mapCircle =
	        map.addCircle(
	            new CircleOptions().center(myLatLng).radius(radius * METERS_PER_FEET));
	    int baseColor = Color.DKGRAY;
	    mapCircle.setStrokeColor(baseColor);
	    mapCircle.setStrokeWidth(2);
	    mapCircle.setFillColor(Color.argb(50, Color.red(baseColor), Color.green(baseColor),
	        Color.blue(baseColor)));
	  }
	  mapCircle.setCenter(myLatLng);
	  mapCircle.setRadius(radius * METERS_PER_FEET); // Convert radius in feet to meters.
	}
    
}
