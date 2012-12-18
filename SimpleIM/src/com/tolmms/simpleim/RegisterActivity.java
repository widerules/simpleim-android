package com.tolmms.simpleim;

import java.net.UnknownHostException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.exceptions.UserIsAlreadyLoggedInException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.tools.Tools;

public class RegisterActivity extends Activity {
	private String username;
	private String password;
	private String password2;

	private EditText et_username;
	private EditText et_password;
	private EditText et_password2;
	private View view_register_form;
	private View view_register_status;
	private TextView tv_register_status_message;
	
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
			
			if (MainActivity.DEBUG)
				Log.d("RegisterActivity", "chiamato onServiceDisconnected");
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
				Log.d("RegisterActivity", "chiamato onServiceConnected");
			
			if (iMService.isUserLoggedIn()) {
				startActivity(new Intent(RegisterActivity.this, LoggedUser.class));
				RegisterActivity.this.finish();
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
		bindService(new Intent(RegisterActivity.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}
	
	/**********************************/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
	
		et_username = (EditText) findViewById(R.id.et_username_register);
		et_password = (EditText) findViewById(R.id.et_password_register);
		et_password2 = (EditText) findViewById(R.id.et_password_confirm_register);
		
		et_password2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.et_password_confirm_register || id == EditorInfo.IME_NULL) {
					attemptRegister();
					return true;
				}
				return false;
			}
		});

		view_register_form = findViewById(R.id.register_form);
		view_register_status = findViewById(R.id.register_status);
		
		tv_register_status_message = (TextView) findViewById(R.id.register_status_message);

		((Button)findViewById(R.id.btn_register)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptRegister();
			}
		});
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_register, menu);
		return true;
	}

	private void attemptRegister() {
		if (attempting)
			return;

		// Reset errors.
		et_username.setError(null);
		et_password.setError(null);
		et_password2.setError(null);

		// Store values at the time of the login attempt.
		username = et_username.getText().toString();
		password = et_password.getText().toString();
		password2 = et_password2.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for username
		if (!cancel && TextUtils.isEmpty(username)) {
			et_username.setError(getString(R.string.it_error_field_required));
			focusView = et_username;
			cancel = true;
		} 
		
		// Check for password.
		if (!cancel && TextUtils.isEmpty(password)) {
			et_password.setError(getString(R.string.it_error_field_required));
			focusView = et_password;
			cancel = true;
		}
		
		if (!cancel && TextUtils.isEmpty(password2)) {
			et_password2.setError(getString(R.string.it_error_field_required));
			focusView = et_password2;
			cancel = true;
		}
		
		if (!cancel && !password.equals(password2)) {
			et_password2.setError(getString(R.string.it_error_passwords_are_not_equal));
			focusView = et_password2;
			cancel = true;
		}
		
		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			tv_register_status_message.setText(R.string.it_register_progress_message);
			showProgress(true);
			
			if (iMService == null) {
				Tools.showMyDialog(getString(R.string.it_error_no_service), this);
				showProgress(false);
			} else if (!iMService.isNetworkConnected()) {
				Tools.showMyDialog(getString(R.string.it_error_no_network), this);
				showProgress(false);
			} else {
				
				Thread registerThread = new Thread() {
					private Handler handler = new Handler();
					private String errorMsg = "";
					
					@Override
					public void run() {
						try {
							iMService.registerUser(username, password);
						} catch (UnknownHostException e) {
							errorMsg = getString(R.string.it_error_server_not_reachable);
							if (MainActivity.DEBUG)
								errorMsg += ": " + e.getMessage();
						} catch (UsernameAlreadyExistsException e) {
							errorMsg = getString(R.string.it_error_username_already_exists);
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
						}
						
						if (errorMsg.equals("")) {
							handler.post(new Runnable() {

								@Override
								public void run() {
									new AlertDialog.Builder(RegisterActivity.this).
									setMessage(getString(R.string.it_successfull_registration)).
									setCancelable(false).
									setNeutralButton("Ok", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
//											startActivity(new Intent(RegisterActivity.this, MainActivity.class));
											RegisterActivity.this.finish();
											dialog.cancel();
										}
									}).create().show();
								}
							});

							return;
						}

						handler.post(new Runnable() {

							@Override
							public void run() {
								Tools.showMyDialog(errorMsg, RegisterActivity.this);
								showProgress(false);									
							}
						});

					}
				};

				registerThread.start();
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

			view_register_status.setVisibility(View.VISIBLE);
			view_register_status.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							view_register_status.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			view_register_form.setVisibility(View.VISIBLE);
			view_register_form.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							view_register_form.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			view_register_status.setVisibility(show ? View.VISIBLE : View.GONE);
			view_register_form.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
}
