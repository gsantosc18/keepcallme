package me.keepcall.br;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.util.Log;

public class Clear extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			SMSReceiver.clearNotificationMap();
			Intent contentIntent = new Intent(Intent.ACTION_VIEW);
			contentIntent.setType(Calls.CONTENT_TYPE);
			startActivity(contentIntent);
		} catch (Exception e) {
			Log.e("keepCall.me", "", e);
		}
	}
}
