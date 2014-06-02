package com.mahshu.globalcomments;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignUpActivity extends Activity {
	private EditText emailView;
	private EditText usernameView;
	private EditText password1View;
	private EditText password2View;
	private TextView errorView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_signup);
	    
	    Log.d("GlobCom", "SignupActivity");
		
		// get signup form data
	    emailView = (EditText)findViewById(R.id.email);
		usernameView = (EditText)findViewById(R.id.username);
		password1View = (EditText)findViewById(R.id.password1);
		password2View = (EditText)findViewById(R.id.password2);
		errorView = (TextView)findViewById(R.id.login_error_message);
		
		// init error message
		errorView.setTextColor(Color.RED);
		errorView.setText("");
		
		findViewById(R.id.signup).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				boolean ret = true;
				errorView.setText("");
				StringBuilder errorMessage = new StringBuilder("Error: ");
				
				if(isEmpty(emailView)) {
					errorMessage.append("Please enter a valid email address.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				// cts todo: use form error validation as well here
				if(isEmpty(usernameView)) {
					errorMessage.append("Please enter a username.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				if(isEmpty(password1View)) {
					errorMessage.append("Please enter a password.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				if(isEmpty(password2View)) {
					errorMessage.append("Please repeat password.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				if(password1View.getText().toString().equals(password2View.getText().toString())) {
					// pass
				} else {
					errorMessage.append("Passwords do not match.");
					errorView.setText(errorMessage);
					ret = false;
					return;
				}
				
				// If we got here, send credentials to server
				final ProgressDialog dlg = new ProgressDialog(SignUpActivity.this);
				dlg.setTitle("Contacting Server.");
				dlg.setMessage("Creating new account. Please wait.");
				dlg.show();
				// Create parse new user account call
				ParseUser user = new ParseUser();
				user.setUsername(usernameView.getText().toString());
				user.setPassword(password1View.getText().toString());
				user.setEmail(emailView.getText().toString());
				
				// other options could be added here
				// user.put("phone", "123-456-7890");
				
				user.signUpInBackground(new SignUpCallback() {
					
					public void done(ParseException e) {
						dlg.dismiss();
						if(e != null) {
							errorView.setText(e.getMessage());
						} else {
							Toast.makeText(SignUpActivity.this, "New account created successfully!", Toast.LENGTH_LONG).show();
							Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
							startActivity(loginIntent);
							finish(); // close signup activity
						}
					
					};
				});
				
				
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
