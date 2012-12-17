package com.tolmms.simpleim;

import java.util.Vector;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.CannotLogOutException;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;
import com.tolmms.simpleim.tools.Tools;

public class LoggedUser extends Activity {
	Vector<UserInfo> user_list;
	
	
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
			
			//TODO mettere per da vero!
//			if (!iMService.isUserLoggedIn()) {
//				startActivity(new Intent(LoggedUser.this, MainActivity.class));
//				LoggedUser.this.finish();
//			}
		}
		
	};
	
	
	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(LoggedUser.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		super.onResume();
	}
	
	/**********************************/
	
	
	private class EfficientAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		
		public EfficientAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return user_list.size();
		}

		public Object getItem(int position) {
			return user_list.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public boolean isEnabled(int position) {
			return user_list.get(position).isOnline();
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
			
			UserInfo currentUser = user_list.get(position);
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
	
	EfficientAdapter myadapt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logged_user);
		
		
		user_list = TemporaryStorage.user_list;
		
		ListView l1 = (ListView) findViewById(R.id.ListView01);
		myadapt = new EfficientAdapter(this);
		l1.setAdapter(myadapt);
		
		
		
		
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
				
				// set dialog message
				alertDialogBuilder.setCancelable(false).setTitle(getString(R.string.it_chat_all_button))
					.setPositiveButton(getString(R.string.it_send_message), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							String msg = userInput.getText().toString();
							
							if (msg.isEmpty())
								Toast.makeText(LoggedUser.this, getString(R.string.it_error_cannot_send_empty_message), Toast.LENGTH_SHORT).show();
							else 
								iMService.sendMessageToAll(msg);
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
				startActivity(new Intent(LoggedUser.this, MapActivity.class));
			}
		});
	}
	

	
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        if (MainActivity.DEBUG)
	        	Toast.makeText(LoggedUser.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_USER_STATE_CHANGED))
	        	return;
	        
	        
	        myadapt.notifyDataSetChanged();
	    }
	};
	
	
	

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
					Looper.prepare(); // TODO mi da errore se lo cancello
					
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
								startActivity(new Intent(LoggedUser.this, MainActivity.class));
								LoggedUser.this.finish();
							}
						});
				}

			};
			
			sendMessageTh.start();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	

}
