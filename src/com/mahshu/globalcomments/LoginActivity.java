package com.mahshu.globalcomments;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private EditText usernameView;
	private EditText passwordView;
	private TextView errorView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);
		Log.d("GlobCom", "LoginActivity");
		
		// determine which page to load
		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser != null) {
			// send to commentview page
			Log.d("GlobCom", "Goto CommentListActivity");
			//Toast.makeText(this, "User: " + currentUser.getUsername(), Toast.LENGTH_LONG).show();
			Intent nextIntent = new Intent(this, CommentListActivity.class);
			startActivity(nextIntent);
			finish();
		} 
		
		// get login form data
		usernameView = (EditText)findViewById(R.id.username);
		passwordView = (EditText)findViewById(R.id.password1);
		errorView = (TextView)findViewById(R.id.login_error_message);
		
		// init error message
		errorView.setTextColor(Color.RED);
		errorView.setText("");
		
		// setup log in button event
		findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				boolean ret = true;
				errorView.setText("");
				StringBuilder errorMessage = new StringBuilder("Login Error: ");
				
				if(isEmpty(usernameView)) {
					errorMessage.append("Please enter a username.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				if(isEmpty(passwordView)) {
					errorMessage.append("Please enter a password.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				// If we got here, send credentials to server
				final ProgressDialog dlg = new ProgressDialog(LoginActivity.this);
				dlg.setTitle("Contacting Server.");
				dlg.setMessage("Logging in. Please wait.");
				dlg.show();
				// Call Parse login method
				ParseUser.logInInBackground(usernameView.getText().toString(), passwordView.getText().toString(), new LogInCallback() {
					
					@Override
					public void done(ParseUser user, ParseException e) {
						dlg.dismiss();
						if(e != null) {
							errorView.setText(e.getMessage());
						} else {
							Intent mapIntent = new Intent(LoginActivity.this, CommentListActivity.class);
							startActivity(mapIntent);
							finish(); //cts debug
						}
					}
				});
				
			}
		});
		
		findViewById(R.id.login_createLink).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent signupIntent = new Intent(v.getContext(), SignUpActivity.class);
				startActivity(signupIntent);
			}
		});
	}


	private boolean isEmpty(EditText val) {
		if(val.getText().toString().trim().length() == 0)
			return true;
		else
			return false;
	}
}