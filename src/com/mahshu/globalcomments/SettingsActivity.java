package com.mahshu.globalcomments;


import com.parse.ParseUser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

public class SettingsActivity extends Activity {

	private RadioButton rbDist1;
	private RadioButton rbDist2;
	private RadioButton rbDist3;
	private RadioButton rbDist4;
	private Button btnLogOut;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_settings);
	    
	    float searchDistance = GlobalCommentsApplication.getSearchDistance();
	    
	    rbDist1 = (RadioButton)findViewById(R.id.set_dist300);
	    rbDist2 = (RadioButton)findViewById(R.id.set_dist2000);
	    rbDist3 = (RadioButton)findViewById(R.id.set_dist5000);
	    rbDist4 = (RadioButton)findViewById(R.id.set_dist10000);
	    btnLogOut = (Button)findViewById(R.id.set_btnLogOut);
	    
	    rbDist1.setChecked(false);
	    rbDist2.setChecked(false);
	    rbDist3.setChecked(false);
	    rbDist4.setChecked(false);
	    
	    if(searchDistance > 5000f) {
	    	rbDist4.setChecked(true);
	    } else if(searchDistance > 2000f){
	    	rbDist3.setChecked(true);
	    } else if(searchDistance > 300f){
	    	rbDist2.setChecked(true);
	    } else {
	    	rbDist1.setChecked(true);
	    }
	    
	    rbDist1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked){
					rbDist2.setChecked(false);
					rbDist3.setChecked(false);
					rbDist4.setChecked(false);
					setSearchDistance();
				}			
			}    	
	    });
	    rbDist2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked){
					rbDist1.setChecked(false);
					rbDist3.setChecked(false);
					rbDist4.setChecked(false);
					setSearchDistance();
				}			
			}    	
	    });
	    rbDist3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked){
					rbDist2.setChecked(false);
					rbDist1.setChecked(false);
					rbDist4.setChecked(false);
					setSearchDistance();
				}			
			}    	
	    });
	    rbDist4.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked){
					rbDist2.setChecked(false);
					rbDist3.setChecked(false);
					rbDist1.setChecked(false);
					setSearchDistance();
				}			
			}    	
	    });
	    
	    btnLogOut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
				        	ParseUser.logOut();
							GlobalCommentsApplication.setAppClosing();
							finish();
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				            
				            break;
				        }
				    }
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setMessage("Are you sure you want to log out?").setPositiveButton("Yes", dialogClickListener)
				    .setNegativeButton("No", dialogClickListener).show();
				
			}
	    	
	    });
	}
	
	private void setSearchDistance() {
		if(rbDist1.isChecked()) {
			GlobalCommentsApplication.setSearchDistance(300f);
		} else if(rbDist2.isChecked()) {
			GlobalCommentsApplication.setSearchDistance(2000f);
		} else if(rbDist3.isChecked()) {
			GlobalCommentsApplication.setSearchDistance(5000f);
		}else {
			GlobalCommentsApplication.setSearchDistance(10000f);
		}
	}

}
