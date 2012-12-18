package com.tolmms.simpleim;

import java.util.ArrayList;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.OverlayItem;

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
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.services.IMService;
import com.tolmms.simpleim.storage.TemporaryStorage;
import com.tolmms.simpleim.tools.Tools;

public class MapActivity extends Activity {
	private static final int DEFAULT_ZOOM = 5;
	private MapView mMapView;
	private MapController mMapController;
	private MinimapOverlay miniMapOverlay;
	
	private ItemizedIconOverlay<OverlayItem> othersPositionOverlay;
	private ItemizedIconOverlay<OverlayItem> myPositionOverlay;
	
	private int refresh_rate_seconds = 10;
	private Handler handler;
	private Runnable runnable;
	
	
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
				Log.d("MapActivity", "chiamato onServiceDisconnected");
			
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
				Log.d("MapActivity", "chiamato onServiceConnected");
			
			if (!iMService.isUserLoggedIn() || !iMService.isMapActivated()) {
//				startActivity(new Intent(MapActivity.this, MainActivity.class));
				MapActivity.this.finish();
			}
			
			refresh_rate_seconds = Math.min(iMService.getMyRefreshTime(), iMService.getOthersRefreshTime());
			
		}
		
	};
	/**********************************/
	
	private BroadcastReceiver myPositionChangesMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_USER_POSITION_CHANGED))
	        	return;
	        
	        updateMyPositionOnTheMap();
	        
	        if (MainActivity.DEBUG)
	        	Log.d("MapActivity", "received broadcasted intent!: "+action);
	        
	    }
	};
	
	
	private BroadcastReceiver otherPositionMessageReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();	        
	        
	        if (!action.equals(IAppManager.INTENT_ACTION_OTHER_POSITION_CHANGED))
	        	return;
	        
	        updateOthersPositionOnTheMap();
	        
	        if (MainActivity.DEBUG)
	        	Log.d("MapActivity", "received broadcasted intent!: "+action);
	        
	    }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setTileSource(TileSourceFactory.MAPNIK);
		mMapView.setBuiltInZoomControls(true);
		
		mMapController = mMapView.getController();
		mMapController.setZoom(DEFAULT_ZOOM);
		
		initializeMyPositionOverlay();
		initializeOtherPositionOverlay();
		
		miniMapOverlay = new MinimapOverlay(this, this.mMapView.getTileRequestCompleteHandler());
		this.mMapView.getOverlays().add(miniMapOverlay);
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(myPositionChangesMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_USER_POSITION_CHANGED));
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(otherPositionMessageReceiver, 
				new IntentFilter(IAppManager.INTENT_ACTION_OTHER_POSITION_CHANGED));
		
		
		handler = new Handler();
		
		runnable = new Runnable() 
		{

		    public void run() 
		    {
		    	updateMyPositionOnTheMap();
				updateOthersPositionOnTheMap();
				
				if (MainActivity.DEBUG)
					Log.d("MapActivity", "made update of the map");
				
				handler.postDelayed(this, refresh_rate_seconds * 1000);
		    }
		};
		
		runnable.run();
		
		mMapController.animateTo(new GeoPoint(TemporaryStorage.myInfo.getLatitude(), TemporaryStorage.myInfo.getLongitude(), TemporaryStorage.myInfo.getAltitude()));
		mMapController.setCenter(new GeoPoint(TemporaryStorage.myInfo.getLatitude(), TemporaryStorage.myInfo.getLongitude(), TemporaryStorage.myInfo.getAltitude()));
	}
	
	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(myPositionChangesMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(otherPositionMessageReceiver);
		
		handler.removeCallbacks(runnable);
		
		super.onPause();		
	}
	
	@Override
	protected void onResume() {
		bindService(new Intent(MapActivity.this, IMService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(myPositionChangesMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_USER_POSITION_CHANGED));
		
		LocalBroadcastManager.
		getInstance(this).
		registerReceiver(otherPositionMessageReceiver, new IntentFilter(IAppManager.INTENT_ACTION_OTHER_POSITION_CHANGED));
		
		
		mMapController.animateTo(new GeoPoint(TemporaryStorage.myInfo.getLatitude(), TemporaryStorage.myInfo.getLongitude(), TemporaryStorage.myInfo.getAltitude()));
		mMapController.setCenter(new GeoPoint(TemporaryStorage.myInfo.getLatitude(), TemporaryStorage.myInfo.getLongitude(), TemporaryStorage.myInfo.getAltitude()));
		runnable.run();
		
		super.onResume();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menu_logout:
	    	Thread exitTh = new Thread() {
				private Handler h = new Handler();
				private String errorMsg = "";

				@Override
				public void run() {
					Looper.prepare(); /* it gives me an error if I delete it */
					
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
								Tools.showMyDialog(errorMsg, MapActivity.this);
							}
						});
					else
						h.post(new Runnable() {

							@Override
							public void run() {
//								startActivity(new Intent(MapActivity.this, MainActivity.class));
								MapActivity.this.finish();
							}
						});
				}

			};
			
			exitTh.start();
	        return true;			
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	
	/*
	 * private stuff
	 */
	private void updateMyPositionOnTheMap() {
		GeoPoint myPos = new GeoPoint(TemporaryStorage.myInfo.getLatitude(), TemporaryStorage.myInfo.getLongitude(), TemporaryStorage.myInfo.getAltitude());
		
		myPositionOverlay.removeAllItems();
		myPositionOverlay.addItem(new OverlayItem(getString(R.string.it_chat_self_name), getString(R.string.it_chat_self_name), myPos));
		
		mMapView.getOverlays().set(0, myPositionOverlay);
		mMapView.invalidate();
		
//		mMapController.setCenter(myPos);
//		mMapController.animateTo(myPos);
	}
	
	private void updateOthersPositionOnTheMap() {
		othersPositionOverlay.removeAllItems();
		
		for (UserInfo u : TemporaryStorage.user_list) {
			if (MainActivity.DEBUG)
				Log.d("mapactivity update others", u.toString());
			if (!u.hasLocationData())
				continue;
			
			OverlayItem temp = new OverlayItem(u.getUsername(), u.getUsername(), new GeoPoint(u.getLatitude(), u.getLongitude(), u.getAltitude()));
			
			if (u.isOnline()) {
				temp.setMarker(getResources().getDrawable(R.drawable.ic_status_online));
			} else {
				temp.setMarker(getResources().getDrawable(R.drawable.ic_status_offline));
			
			}
			othersPositionOverlay.addItem(temp);
		}
		
		mMapView.getOverlays().set(1, othersPositionOverlay);
		mMapView.invalidate();
	}

	private void initializeMyPositionOverlay() {
		ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gesture_listner = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
			@Override
			public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
				if (item == null)
					return false;
				Toast.makeText(MapActivity.this, getString(R.string.it_chat_self_name), Toast.LENGTH_SHORT).show();

				return true;
			}

			@Override
			public boolean onItemLongPress(final int index, final OverlayItem item) {
				if (item == null)
					return false;
				return false;
			}
		};
		
		myPositionOverlay = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), gesture_listner);	
		mMapView.getOverlays().add(myPositionOverlay);
	}
	
	private void initializeOtherPositionOverlay() {
		ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gesture_listner = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
			@Override
			public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
				if (item == null)
					return false;
				
				Toast.makeText(MapActivity.this, item.mTitle, Toast.LENGTH_SHORT).show();
				return true;
			}

			@Override
			public boolean onItemLongPress(final int index, final OverlayItem item) {
				if (item == null)
					return false;
				return false;
			}
		};
		othersPositionOverlay = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), gesture_listner);
		
		mMapView.getOverlays().add(othersPositionOverlay);
	}
}
