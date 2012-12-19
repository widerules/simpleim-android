package com.tolmms.simpleim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;
import com.tolmms.simpleim.tools.Tools;

public class LoggedUser extends Activity {
//	Vector<UserInfo> user_list;
	EfficientAdapter myadapt;
	
	
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
				Log.d("LoggedUser", "chiamato onServiceDisconnected");
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
				Log.d("LoggedUser", "chiamato onServiceConnected");
			
			if (!iMService.isUserLoggedIn()) {
//				startActivity(new Intent(LoggedUser.this, MainActivity.class));
				LoggedUser.this.finish();
			}
		}
		
	};
	
	/**********************************/
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_USER_STATE_CHANGED))
	        	return;
	        
	        if (MainActivity.DEBUG)
	        	Log.d("LoggedUser", "received broadcasted intent!: "+action);
	        
	        myadapt.notifyDataSetChanged();
	    }
	};
	
	
	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(LoggedUser.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		myadapt.notifyDataSetChanged();
		
		super.onResume();
	}
	
	@Override
	public void onBackPressed() {
		return;
    }
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logged_user);
		
		setTitle(getTitle() + " ("+ TemporaryStorage.myInfo.getUsername() + ")");
		
		ListView l1 = (ListView) findViewById(R.id.ListView01);
		myadapt = new EfficientAdapter(this);
		l1.setAdapter(myadapt);
		
		l1.setSelected(false);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		
		l1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				
				String selectedUser = TemporaryStorage.user_list.get(arg2).getUsername();
				
				Intent a = new Intent(LoggedUser.this, ChatActivity.class);
				a.setAction(ChatActivity.MESSAGE_TO_A_USER);
				a.putExtra(ChatActivity.USERNAME_TO_CHAT_WITH_EXTRA, selectedUser);
				startActivity(a);
			}
		});
		
		
		((Button) findViewById(R.id.btn_chatall)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater li = LayoutInflater.from(LoggedUser.this);
				View promptsView = li.inflate(R.layout.chat_all, null);
 
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoggedUser.this);
 
				// set prompts.xml to alertdialog builder
				alertDialogBuilder.setView(promptsView);
 
				final EditText userInput = (EditText) promptsView.findViewById(R.id.et_message_to_all);
				userInput.setText("");
				
				// set dialog message
				alertDialogBuilder.setCancelable(false).setTitle(getString(R.string.it_chat_all_button))
					.setPositiveButton(getString(R.string.it_send_message), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							if (userInput.getText().toString().trim().isEmpty()) {
								Toast.makeText(LoggedUser.this, getString(R.string.it_error_cannot_send_empty_message), Toast.LENGTH_SHORT).show();
								return;
							}
							
							Thread sendMessageTh = new Thread() {
								private Handler h = new Handler();
								private boolean success = false;

								String the_message = userInput.getText().toString().trim();

								@Override
								public void run() {
									Looper.prepare(); /* if i delete it, then it gives me an error */

									success = iMService.sendMessageToAll(the_message);

									if (!success)			
										h.post(new Runnable() {

											@Override
											public void run() {
												Tools.showMyDialog(getString(R.string.it_error_send_all), LoggedUser.this);
											}
										});						
								}

							};

							sendMessageTh.start();		
							
					}})
					.setNegativeButton(getString(R.string.it_cancel), new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
					    	dialog.cancel();
					    }
					});
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
 
				// show it
				alertDialog.show();
			}
		});
		
		((Button) findViewById(R.id.btn_map)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (iMService.isMapActivated()) {
					startActivity(new Intent(LoggedUser.this, MapActivity.class));
					return;
				}
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoggedUser.this);

				
				// set dialog message
				alertDialogBuilder.setCancelable(false).setTitle(getString(R.string.it_view_on_map_button))
					.setNeutralButton(getString(R.string.it_btn_ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							dialog.dismiss();					
					}}).setMessage(getString(R.string.it_activate_map_description));

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
				
			}
		});
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_logged_user, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menu_logout:
			Thread sendMessageTh = new Thread() {
				private Handler h = new Handler();
				private String errorMsg = "";

				@Override
				public void run() {
					Looper.prepare(); /* if i delete it, then it gives me an error */
					
					try {
						iMService.exit();
					} catch (UserNotLoggedInException e) {
						errorMsg = getString(R.string.it_error_user_not_logged_in);
						if (MainActivity.DEBUG)
							errorMsg += ": " + e.getMessage();
						//unlikely to be here!!! but.. i put the error handling logic
					}

					if (!errorMsg.isEmpty())
						h.post(new Runnable() {

							@Override
							public void run() {
								Tools.showMyDialog(errorMsg, LoggedUser.this);
							}
						});
					else
						h.post(new Runnable() {

							@Override
							public void run() {
//								startActivity(new Intent(LoggedUser.this, MainActivity.class));
								LoggedUser.this.finish();
							}
						});
				}

			};
			
			sendMessageTh.start();
	        return true;
	        
	    case R.id.menu_settings:
	    	LayoutInflater li = LayoutInflater.from(LoggedUser.this);
			View promptsView = li.inflate(R.layout.map_settings, null);

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoggedUser.this);

			// set prompts.xml to alertdialog builder
			alertDialogBuilder.setView(promptsView);

			final EditText et_my_time = (EditText) promptsView.findViewById(R.id.et_sec_my_data);
			final EditText et_other_time = (EditText) promptsView.findViewById(R.id.et_sec_others);
			final Switch sw_map = (Switch) promptsView.findViewById(R.id.sw_send_my_gps_data);
			Tools.InputFilterMinMax filter = new Tools.InputFilterMinMax(1, 100);
			
			
			
			sw_map.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						et_my_time.setText(String.valueOf(iMService.getMyRefreshTime()));
						et_other_time.setText(String.valueOf(iMService.getOthersRefreshTime()));
						
						et_my_time.setEnabled(true);
						et_other_time.setEnabled(true);
					} else {
						et_my_time.setEnabled(false);
						et_other_time.setEnabled(false);
					}
				}
			});
			
			et_my_time.setFilters(new InputFilter[] { filter });
			et_other_time.setFilters(new InputFilter[] { filter });
			
			sw_map.setChecked(iMService.isMapActivated());
			et_my_time.setText(String.valueOf(iMService.getMyRefreshTime()));
			et_other_time.setText(String.valueOf(iMService.getOthersRefreshTime()));
			
				
			// set dialog message
			alertDialogBuilder.setCancelable(false).setTitle(getString(R.string.it_location_settings))
				.setPositiveButton(getString(R.string.it_btn_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						String str_my_time = et_my_time.getText().toString();
						String str_other_time = et_other_time.getText().toString();
						boolean activate = sw_map.isChecked();
						
						if (activate)
							iMService.activateMap(Integer.valueOf(str_my_time), Integer.valueOf(str_other_time));
						else
							iMService.deactivateMap();						
				}})
				.setNegativeButton(getString(R.string.it_cancel), new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog,int id) {
				    	dialog.cancel();
				    }
				});

			// create alert dialog
			AlertDialog alertDialog = alertDialogBuilder.create();

			// show it
			alertDialog.show();
			
			
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	/*
	 * private stuff
	 */
	private class EfficientAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		
		public EfficientAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
//			return user_list.size();
			return TemporaryStorage.user_list.size();
		}

		public Object getItem(int position) {
//			return user_list.get(position);
			return TemporaryStorage.user_list.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public boolean isEnabled(int position) {
//			return user_list.get(position).isOnline();
			return TemporaryStorage.user_list.get(position).isOnline();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview, null);
				holder = new ViewHolder();
				holder.iv_status_user = (ImageView) convertView.findViewById(R.id.iv_user_status);
				holder.tv_friend_username = (TextView) convertView.findViewById(R.id.tv_friend_username);
				holder.iv_chat_icon = (ImageView) convertView.findViewById(R.id.iv_chat_icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			UserInfo currentUser = TemporaryStorage.user_list.get(position);
			if (currentUser.isOnline()) {
				holder.iv_status_user.setImageResource(R.drawable.ic_status_online);
				holder.iv_chat_icon.setImageResource(R.drawable.ic_chat);
			} else {
				holder.iv_status_user.setImageResource(R.drawable.ic_status_offline);
				holder.iv_chat_icon.setImageResource(R.drawable.ic_chat_no);
			}
			
			holder.tv_friend_username.setText(currentUser.getUsername());

			return convertView;
		}
	}
	
	private static class ViewHolder {
		ImageView iv_status_user;
		TextView tv_friend_username;
		ImageView iv_chat_icon;
	}	

}
