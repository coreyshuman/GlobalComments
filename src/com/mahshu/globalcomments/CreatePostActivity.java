package com.mahshu.globalcomments;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
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
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
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

public class CreatePostActivity extends FragmentActivity
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener
						{
	
	private final static int MAX_POST_LENGTH = 100;
	
	private GoogleMap map;
	private LocationRequest  lr;
	private LocationClient lc;
	private SupportMapFragment MapFragment;
	private boolean mapIsLoaded;
	private Location lastLocation = null;
	private Location currentLocation = null;
	private Marker curLocMarker = null;
	private EditText postMessage;
	private TextView messageCount;
	private Button btnPost;
	private Button btnCancel;
	private int lastMsgCnt = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mapIsLoaded = false;
	    Log.d("GlobCom", "Post onCreate");
	    setContentView(R.layout.activity_createpost);
	    lr = LocationRequest.create();
	    lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    lc = new LocationClient(this.getApplicationContext(), this, this);
	    lc.connect();
	    
	    postMessage = (EditText) findViewById(R.id.post_message);
	    messageCount = (TextView)findViewById(R.id.post_count);
	    messageCount.setText("0 / " + Integer.toString(MAX_POST_LENGTH));
	    btnPost = (Button)findViewById(R.id.post_btnPost);
	    btnCancel = (Button)findViewById(R.id.post_btnCancel);
	    
	    postMessage.addTextChangedListener(new TextWatcher() {
	    	public void afterTextChanged(Editable s) {
	    		if(s.length() != lastMsgCnt) {
	    			lastMsgCnt = s.length();
		    		messageCount.setText(s.length() + " / " + MAX_POST_LENGTH);
		    		Spannable spanText = Spannable.Factory.getInstance().newSpannable(s.toString());
		    		int cursorPos = postMessage.getSelectionEnd();
		    		if(s.length() > MAX_POST_LENGTH) {
		    			messageCount.setTextColor(Color.RED);
		    			spanText.setSpan(new BackgroundColorSpan(0xFF993333), MAX_POST_LENGTH, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    		}
		    		else if(s.length() > MAX_POST_LENGTH - 10) {
		    			messageCount.setTextColor(Color.rgb(200, 140, 140));
		    			spanText.setSpan(new BackgroundColorSpan(0x00000000), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    		}
		    		else {
		    			messageCount.setTextColor(Color.BLACK);
		    			spanText.setSpan(new BackgroundColorSpan(0x00000000), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    		}
		    		postMessage.setText(spanText);
		    		postMessage.setSelection(cursorPos);
	    		}
	    	}
	    	
	    	public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
	    btnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
	    });
	    btnPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int postLength;
				
				postLength = postMessage.getText().length();
				if(postLength > MAX_POST_LENGTH) {
					//create error
					AlertDialog.Builder alertBuilder = new AlertDialog.Builder(CreatePostActivity.this);
					alertBuilder
						.setTitle("Post Error")
						.setMessage("Post cannot be longer than " + Integer.toString(MAX_POST_LENGTH) + " characters.")
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								
							}
						});
					alertBuilder.show();
					//AlertDialog alertDialog = alertBuilder.create();
					//alertDialog.show();
				}
				else
				{
					Location myLoc = (currentLocation == null) ? lastLocation : currentLocation;
			        if (myLoc == null) {
			          Toast.makeText(CreatePostActivity.this,
			              "Current location loading, please try again after your location appears on the map.", Toast.LENGTH_LONG).show();
			          return;
			        }
			        // Create a post.
		            GlobalPost post = new GlobalPost();
		            post.init();
		            final ParseGeoPoint curPoint = geoPointFromLocation(myLoc);
		            // Set the location to the current user's location
		            post.setLocation(curPoint);
		            post.setText(postMessage.getText().toString());
		            post.setUser(ParseUser.getCurrentUser());
		            ParseACL acl = new ParseACL();
		            // Give public read access
		            acl.setPublicReadAccess(true);
		            post.setACL(acl);
		            // Save the post
		            post.saveInBackground(new SaveCallback() {
		              @Override
		              public void done(ParseException e) {
		            	  postMessage.setText(""); //cts debug - clear message after posting
		              }
		            });
		            
		            Toast.makeText(CreatePostActivity.this, "Comment Posted!", Toast.LENGTH_SHORT).show();
		            finish();
				}
			}
	    	
	    });
	    
	    try {
            // Loading map
            initilizeMap();
 
        } catch (Exception e) {
            e.printStackTrace();
        }
	    
	    if(mapIsLoaded){
	        map.getUiSettings().setAllGesturesEnabled(false);
	        map.getUiSettings().setMyLocationButtonEnabled(false);
	        map.setMyLocationEnabled(true);
	        map.getUiSettings().setZoomControlsEnabled(false);
	    }
	}

    private void initilizeMap() {
        if (MapFragment == null) {
        	MapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        	map = MapFragment.getMap();
 
            // check if map is created successfully or not
            if (map == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
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
	/*
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Log.d("GlobCom", "Post onCreateView");
		if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
		
		try {
			view = inflater.inflate(R.layout.activity_createpost, container,
                    false);

            mapFragment = ((MapFragment) this.getActivity()
                    .getFragmentManager().findFragmentById(R.id.map));

            map = mapFragment.getMap();
            map.getUiSettings().setAllGesturesEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.setMyLocationEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(false);
		} catch (InflateException e) {
            Toast.makeText(getActivity(), "Problems inflating the view !",
                    Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(getActivity(), "Google Play Services missing !",
                    Toast.LENGTH_LONG).show();
        }
		
		return view;
	}
	*/
	
	@Override
    public void onLocationChanged(Location loc) {
		LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
		Log.d("GlobCom", "Post locationChanged");
		lastLocation = loc;
		currentLocation = loc;
		if(mapIsLoaded){
	        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, 15);
	        map.animateCamera(cameraUpdate);
	        if(curLocMarker == null){
		     // create marker
		        MarkerOptions marker = new MarkerOptions().position(pos).title("Your location");
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
		}
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
    	Log.d("GlobCom", "Post connectionFailed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    	Log.d("GlobCom", "Post onConnected");
        lc.requestLocationUpdates(lr, this);

    }

    @Override
    public void onDisconnected() {
    	Log.d("GlobCom", "Post onDisconnect");
    }
}
