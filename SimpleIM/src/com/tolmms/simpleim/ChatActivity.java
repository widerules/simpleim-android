package com.tolmms.simpleim;

import java.util.Vector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tolmms.simpleim.communication.CannotSendBecauseOfWrongUserInfo;
import com.tolmms.simpleim.datatypes.MessageRepresentation;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.exceptions.UserToChatWithIsNotRecognizedException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;
import com.tolmms.simpleim.tools.Tools;

public class ChatActivity extends Activity {
	public static final String USERNAME_TO_CHAT_WITH_EXTRA = "com.tolmms.simpleim.ChatActivity.USERNAME_EXTRA";

	public static final String MESSAGE_TO_ALL_ACTION = "com.tolmms.simpleim.ChatActivity.MESSAGE_TO_ALL_ACTION";
	public static final String MESSAGE_TO_A_USER = "com.tolmms.simpleim.ChatActivity.MESSAGE_TO_A_USER";
	

	private String username_to_chat;
	private ImageView iv_status_user;
	private ListView l;
	
	boolean isOnline;
	
	private Vector<MessageRepresentation> user_messages;
	
	private ChatListAdapter cla;
	
	
	
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
			
			
			iMService.unsetCurrentUserChat();
			
			iMService = null;
			
			if (MainActivity.DEBUG)
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
			
			iMService.setCurrentUserChat(username_to_chat);
		}
		
	};
	
	
	/**********************************/
	
	
	/* Adapter for retriving the messages */
	private class ChatListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		
		public ChatListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return user_messages.size();
		}

		public Object getItem(int position) {
			return user_messages.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		/*
		 * render the listview not selectable
		 */
		public boolean isEnabled(int position) {
			return false;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview_row_chat, null);
				holder = (TextView) convertView.findViewById(R.id.tv_row_chat);
				convertView.setTag(holder);
			} else {
				holder = (TextView) convertView.getTag();
			}
			
			MessageRepresentation m = user_messages.get(position);
			
			String msgString = "";
			
			if (m.getMessageInfo().getSource().equals(TemporaryStorage.getUserInfoByUsername(username_to_chat))) {
				msgString += username_to_chat;
			} else {
				msgString += getString(R.string.it_chat_self_name);
			}
			
			msgString += ": " + m.getMessageInfo().getMessage();
			
			holder.setText(msgString);

			return convertView;
		}
	}
	
	
	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(statusChangesMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(messagesMessageReceiver);
		
//		iMService.unsetCurrentUserChat();
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(ChatActivity.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(statusChangesMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(messagesMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT));
		
		
		updateChatView();
		updateUserStatus(TemporaryStorage.getUserInfoByUsername(username_to_chat).getStatus());
		
//		iMService.setCurrentUserChat(username_to_chat);
		
		super.onResume();
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		Intent myIntent = getIntent();
		
		if (!myIntent.getAction().equals(MESSAGE_TO_A_USER))
			this.finish();
		
		username_to_chat = (String) myIntent.getExtras().get(USERNAME_TO_CHAT_WITH_EXTRA);
		
		if (username_to_chat == null) {
//			in order to chat with someone I need to have a username
			this.finish();
		}
		
		user_messages = TemporaryStorage.getMessagesByUsername(username_to_chat);
		
		((TextView) findViewById(R.id.tv_friend_username_chat)).setText(username_to_chat);
		iv_status_user = (ImageView) findViewById(R.id.iv_user_status_chat);
		l = (ListView) findViewById(R.id.lv_chat_list);
		
		cla = new ChatListAdapter(this);
		
		l.setAdapter(cla);
		l.setSmoothScrollbarEnabled(true);
		
		
		Log.d("aaaaaaaaaaaaa", TemporaryStorage.getUserInfoByUsername(username_to_chat).toString());
		
		updateUserStatus(TemporaryStorage.getUserInfoByUsername(username_to_chat).getStatus());
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(statusChangesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_USER_STATE_CHANGED));
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(messagesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT));	
		
		
		
		((Button) findViewById(R.id.bt_send)).setOnClickListener(new OnClickListener() {
			TextView tv_send_msg = (TextView) findViewById(R.id.et_message_to_send);
			@Override
			public void onClick(View v) {
				if (!isOnline) {
					Tools.showMyDialog(getString(R.string.it_error_cannot_chat_with_offline_user), ChatActivity.this);
					return;
				}
				
				String the_message = tv_send_msg.getText().toString().trim();
				
				if (the_message.isEmpty()) {
					Toast.makeText(ChatActivity.this, getString(R.string.it_error_cannot_send_empty_message), Toast.LENGTH_SHORT).show();
					return;
				}
				
				try {
					iMService.sendMessage(username_to_chat, the_message);
				} catch (UserNotLoggedInException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UserToChatWithIsNotRecognizedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CannotSendBecauseOfWrongUserInfo e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tv_send_msg.setText("");
			}
		});
	}
	
	
	private BroadcastReceiver statusChangesMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        String intentUsername;
	        String newUserState;
	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_USER_STATE_CHANGED))
	        	return;
	        
	        intentUsername = intent.getStringExtra(IAppManager.INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA);
	        if (!intentUsername.equals(username_to_chat))
	        	return;
	        
	        newUserState = intent.getStringExtra(IAppManager.INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA);
	        updateUserStatus(newUserState);
	        
	        if (MainActivity.DEBUG)
	        	Toast.makeText(ChatActivity.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	    }
	};
	
	
	private BroadcastReceiver messagesMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        String intentUsername;
	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT))
	        	return;
	        
	        intentUsername = intent.getStringExtra(IAppManager.INTENT_ACTION_MESSAGES_RECEIVED_SENT_USERNAME_EXTRA);
	        
	        //se l'intent che ricievo si rifferisce al utente con cui adesso non chatto.. ritorno
	        if (!intentUsername.equals(username_to_chat))
	        	return;
	        
	        updateChatView();
	        
	        
	        if (MainActivity.DEBUG)
	        	Toast.makeText(ChatActivity.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	    }
	};

	private void updateUserStatus(String userStatus) {
        if (UserInfo.ONLINE_STATUS.equals(userStatus)) {
        	isOnline = true;
        	iv_status_user.setImageResource(R.drawable.ic_status_online);
        } else {
        	isOnline = false;
        	iv_status_user.setImageResource(R.drawable.ic_status_offline);
        }
	}
	
	private void updateChatView() {
		cla.notifyDataSetChanged();
		if (l.getCount() > 0)
			l.smoothScrollToPosition(l.getCount() - 1);
		else
			l.smoothScrollToPosition(l.getCount());
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
	        iMService.exit();
	        startActivity(new Intent(ChatActivity.this, MainActivity.class));
	        ChatActivity.this.finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

}
