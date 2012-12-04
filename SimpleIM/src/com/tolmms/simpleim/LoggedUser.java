package com.tolmms.simpleim;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;

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
			Toast.makeText(LoggedUser.this, "ERROR. service disconnected", Toast.LENGTH_SHORT).show();
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
				Toast.makeText(LoggedUser.this, "chiamato onServiceConnected", Toast.LENGTH_SHORT).show();
			
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
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(LoggedUser.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			UserInfo currentUser = user_list.get(position);
			if (currentUser.isOnline()) {
				holder.iv_status_user.setImageResource(R.drawable.ic_status_online);
			} else {
				holder.iv_status_user.setImageResource(R.drawable.ic_status_offline);
			}
			
			holder.tv_friend_username.setText(currentUser.getUsername());

			return convertView;
		}
	}
	
	private static class ViewHolder {
		ImageView iv_status_user;
		TextView tv_friend_username;
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
		
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_LIST_RECIEVED));
		
	}
	
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        Toast.makeText(LoggedUser.this, "received broadcasted intent!: "+action, Toast.LENGTH_LONG).show();
	        
	        myadapt.notifyDataSetChanged();
	    }
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_logged_user, menu);
		return true;
	}

}
