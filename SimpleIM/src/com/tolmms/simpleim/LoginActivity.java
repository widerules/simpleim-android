package com.tolmms.simpleim;

import java.net.UnknownHostException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.exceptions.NotEnoughResourcesException;
import com.tolmms.simpleim.exceptions.UserIsAlreadyLoggedInException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.tools.Tools;

/**
 * Activity which displays a login screen to the user
 */
public class LoginActivity extends Activity {
	// Values for email and password at the time of the login attempt.
	private String username;
	private String password;

	// UI references.
	private EditText et_username;
	private EditText et_password;
	private View view_login_form;
	private View view_login_status;
	private TextView tv_login_status_message;
	
	private boolean attempting = false;
	
	/***********************************/
	/* stuff for service */
	/***********************************/
	private IAppManager iMService;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
            // unexpectedly disconnected - that is, its process crashed.
            // Because it is running in our same process, we should never see this happen
			
			iMService = null;
			Toast.makeText(LoginActivity.this, "ERROR. service disconnected", Toast.LENGTH_SHORT).show();
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
			
			iMService = ((IMService.IMBinder) service).getService();
			
			if (MainActivity.DEBUG)
				Toast.makeText(LoginActivity.this, "chiamato onServiceConnected", Toast.LENGTH_SHORT).show();
			
			
			// TODO bisogna mettere questo controllo in main activity forse
			if (iMService.isUserLoggedIn()) {
				startActivity(new Intent(LoginActivity.this, LoggedUser.class));
				LoginActivity.this.finish();
			}
		}
		
	};
	
	
	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(LoginActivity.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}
	
	/**********************************/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.activity_login);
		
	
		et_username = (EditText) findViewById(R.id.et_username);
		et_username.setText(username);

		et_password = (EditText) findViewById(R.id.et_password);
		et_password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
						if (id == R.id.et_login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		view_login_form = findViewById(R.id.login_form);
		view_login_status = findViewById(R.id.login_status);
		tv_login_status_message = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}


	
	
	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (attempting)
			return;

		// Reset errors.
		et_username.setError(null);
		et_password.setError(null);

		// Store values at the time of the login attempt.
		username = et_username.getText().toString();
		password = et_password.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for username
		if (!cancel && TextUtils.isEmpty(username)) {
			et_username.setError(getString(R.string.it_error_field_required));
			focusView = et_username;
			cancel = true;
		} 
		/*
		 * questo controllo deve andare nel register activity
		 * else if (!username.contains("@")) {
			et_username.setError(getString(R.string.error_invalid_email));
			focusView = et_username;
			cancel = true;
		}
		*/
		
		// Check for password.
		if (!cancel && TextUtils.isEmpty(password)) {
			et_password.setError(getString(R.string.it_error_field_required));
			focusView = et_password;
			cancel = true;
		} 
		/*
		 * questo controllo deve andare nel register activity
		 * else if (password.length() < 4) {
			et_password.setError(getString(R.string.it_error_invalid_password));
			focusView = et_password;
			cancel = true;
		}*/

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			tv_login_status_message.setText(R.string.it_login_progress_signing_in);
			showProgress(true);
			
			if (iMService == null) {
				Tools.showMyDialog(getString(R.string.it_error_no_service), this);
				showProgress(false);
			} else if (!iMService.isNetworkConnected()) {
				Tools.showMyDialog(getString(R.string.it_error_no_network), this);
				showProgress(false);
			} else {
				
				Thread loginThread = new Thread() {
					private Handler handler = new Handler();
					private String errorMsg = "";
					
					@Override
					public void run() {
						Looper.prepare(); //TODO mi da errore se lo cancello quando cerco di loggarmi la seconda volta (dopo essermi sloggato)
						try {
							iMService.loginUser(username, password);
						} catch (UnknownHostException e) {
							errorMsg = getString(R.string.it_error_server_not_reachable);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						} catch (UsernameOrPasswordException e) {
							errorMsg = getString(R.string.it_error_check_username_or_password);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						} catch (CommunicationException e) {
							errorMsg = getString(R.string.it_error_communication_error);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						} catch (UserIsAlreadyLoggedInException e) {
							errorMsg = getString(R.string.it_error_user_already_logged_in);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						} catch (NotEnoughResourcesException e) {
							errorMsg = getString(R.string.it_error_not_enough_resources);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						}
						
						if (errorMsg.equals("")) {
							handler.post(new Runnable() {
								
								@Override
								public void run() {
									Intent i = new Intent(LoginActivity.this, LoggedUser.class);
//									guarda sul foglio del issue
//									i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(i);
									LoginActivity.this.finish();
								}
							});
							return;
						}
						
						handler.post(new Runnable() {
							
							@Override
							public void run() {
								Tools.showMyDialog(errorMsg, LoginActivity.this);
								et_password.setText("");
								showProgress(false);									
							}
						});
						
					}
				};
				
				loginThread.start();
			}
		}
	}	
	
	

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		
		attempting = show;
		
		
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			view_login_status.setVisibility(View.VISIBLE);
			view_login_status.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							view_login_status.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			view_login_form.setVisibility(View.VISIBLE);
			view_login_form.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							view_login_form.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			view_login_status.setVisibility(show ? View.VISIBLE : View.GONE);
			view_login_form.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
}
