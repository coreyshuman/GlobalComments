package com.mahshu.globalcomments;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class CreatePostActivity extends Fragment
						implements GooglePlayServicesClient.ConnectionCallbacks,
						GooglePlayServicesClient.OnConnectionFailedListener,
						com.google.android.gms.location.LocationListener
						{
	GoogleMap map;
	LatLng latlng;
	private LocationRequest  lr;
	private LocationClient lc;
	MapFragment mapFragment;
	ImageView iv;
	private static View view;
	
	public CreatePostActivity() {
		
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Log.d("GlobCom", "Post onCreate");
	    lr = LocationRequest.create();
	    lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    lc = new LocationClient(this.getActivity().getApplicationContext(), this, this);
	    lc.connect();
	}

	@Override
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
	
	@Override
    public void onLocationChanged(Location l2) {
		Log.d("GlobCom", "Post locationChanged");
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                new LatLng(l2.getLatitude(), l2.getLongitude()), 15);
        map.animateCamera(cameraUpdate);
     // create marker
        MarkerOptions marker = new MarkerOptions().position(new LatLng(l2.getLatitude(), l2.getLongitude())).title("You");
     // Changing marker icon
        //marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

         
        // adding marker
        map.addMarker(marker);
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
    	Log.d("GlobCom", "Post connectionFailed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        lc.requestLocationUpdates(lr, this);

    }

    @Override
    public void onDisconnected() {

    }
}
