package com.tolmms.simpleim.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Tools {
	
	public static void showMyDialog(String message, Context context) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		
		alertDialogBuilder.setMessage(message).setCancelable(false).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel(); // or dismiss?			
			}
		});
		
		alertDialogBuilder.create().show();
	}

}
