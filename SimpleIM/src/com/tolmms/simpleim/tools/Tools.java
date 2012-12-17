package com.tolmms.simpleim.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;

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

	
	public static class InputFilterMinMax implements InputFilter {
		 
		private int min, max;
	 
		public InputFilterMinMax(int min, int max) {
			this.min = min;
			this.max = max;
		}
	 
		public InputFilterMinMax(String min, String max) {
			this.min = Integer.parseInt(min);
			this.max = Integer.parseInt(max);
		}
	 
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {	
			try {
				int input = Integer.parseInt(dest.toString() + source.toString());
				if (isInRange(min, max, input))
					return null;
			} catch (NumberFormatException nfe) { }		
			return "";
		}
	 
		private boolean isInRange(int a, int b, int c) {
			return b > a ? c >= a && c <= b : c >= b && c <= a;
		}
	}
}
