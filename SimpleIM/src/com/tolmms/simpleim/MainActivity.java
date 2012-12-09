package com.tolmms.simpleim;

import com.tolmms.simpleim.services.IMService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	public static final boolean DEBUG = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button btn_login = (Button) findViewById(R.id.btn_login);
		Button btn_register = (Button) findViewById(R.id.btn_register);
		
		
		
		//TODO importante... devo mettere questo se no il service viene 
		// riavviato ogni volta che qualcuno si connette al servizio
		startService(new Intent(this, IMService.class));
		
		
		
		btn_login.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				startActivity(new Intent(MainActivity.this, LoggedUser.class));
				startActivity(new Intent(MainActivity.this, LoginActivity.class));
				
			}
		});
		
		btn_register.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, RegisterActivity.class));
			}
		});
		
		
	}


}
