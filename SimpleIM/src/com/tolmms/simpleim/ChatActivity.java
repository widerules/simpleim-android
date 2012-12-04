package com.tolmms.simpleim;

import java.util.Vector;

import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;
import com.tolmms.simpleim.tools.Tools;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {
	
	protected static final String USERNAME_TO_CHAT_WITH_EXTRA = "com.tolmms.simpleim.ChatActivity.USERNAME_EXTRA";

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
			Toast.makeText(ChatActivity.this, "ERROR. service disconnected", Toast.LENGTH_SHORT).show();
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
				Toast.makeText(ChatActivity.this, "chiamato onServiceConnected", Toast.LENGTH_SHORT).show();
			
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
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(statusChangesMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(messagesMessageReceiver);
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(ChatActivity.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(statusChangesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		LocalBroadcastManager.getInstance(this).registerReceiver(messagesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT));
		
		
		super.onResume();
	}
	
	/**********************************/

	
	String username_to_chat;
	ImageView iv_status_user;
	boolean isOnline;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		Button b = (Button) findViewById(R.id.bt_send);
		ListView l = (ListView) findViewById(R.id.lv_chat_list);
		
		Intent myIntent = getIntent();
		
		if ((username_to_chat = (String) myIntent.getExtras().get(USERNAME_TO_CHAT_WITH_EXTRA)) == null) {
//			in order to chat with someone I need to have a username
			this.finish();
		}
		
		
		((TextView) findViewById(R.id.tv_friend_username_chat)).setText(username_to_chat);
		iv_status_user = (ImageView) findViewById(R.id.iv_user_status_chat);
		
		updateUserStatus();
		
		
		LocalBroadcastManager.getInstance(this).registerReceiver(statusChangesMessageReceiver, 
								new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		LocalBroadcastManager.getInstance(this).registerReceiver(messagesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT));
		
		
				
		
//		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this, R.layout.listview_row_chat, msgs);
//		
//		l.setAdapter(aa);
//		l.setSmoothScrollbarEnabled(true);
//		
//		aa.notifyDataSetChanged();
//		
//		ll.smoothScrollToPosition(ll.getCount() - 1);
		
		
		b.setOnClickListener(new OnClickListener() {
			TextView tv_send_msg = (TextView) findViewById(R.id.et_message_to_send);
			@Override
			public void onClick(View v) {
				if (!isOnline)
					Tools.showMyDialog(getString(R.string.it_error_cannot_chat_with_offline_user), ChatActivity.this);
				
				
				String the_message = tv_send_msg.getText().toString();
				
				
				iMService.sendMessage(username_to_chat, the_message);
				
				
//				iMService.sendMessage()
			}
		});
	}
	
	
	private BroadcastReceiver statusChangesMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        String intentUsername;
	        String intentState;
	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_USER_STATE_CHANGED))
	        	return;
	        
	        intentUsername = intent.getStringExtra(IAppManager.INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA);
	        
	        if (!intentUsername.equals(username_to_chat))
	        	return;
	        
	        
	        intentState = intent.getStringExtra(IAppManager.INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA);
	        
	        if (UserInfo.ONLINE_STATUS.equals(intentState)) {
	        	isOnline = true;
	        	iv_status_user.setImageResource(R.drawable.ic_status_online);
	        } else {
	        	isOnline = false;
	        	iv_status_user.setImageResource(R.drawable.ic_status_offline);
	        }
	        
	        if (MainActivity.DEBUG)
	        	Toast.makeText(ChatActivity.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	    }
	};
	
	
	private BroadcastReceiver messagesMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        String intentUsername;
	        String intentState;
	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT))
	        	return;
	        
	        intentUsername = intent.getStringExtra(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT_USERNAME_EXTRA);
	        
	        //se l'intent che ricievo si rifferisce al utente con cui adesso non chatto.. ritorno
	        if (!intentUsername.equals(username_to_chat))
	        	return;
	        
	        
//	        pesco ultimi TOT messaggi
	        
	        
	        if (MainActivity.DEBUG)
	        	Toast.makeText(ChatActivity.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	    }
	};


	private void updateUserStatus() {
		isOnline = TemporaryStorage.getUserInfoByUsername(username_to_chat).isOnline();

		if (isOnline)
			iv_status_user.setImageResource(R.drawable.ic_status_online);
		else
			iv_status_user.setImageResource(R.drawable.ic_status_offline);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_chat, menu);
		return true;
	}

}
