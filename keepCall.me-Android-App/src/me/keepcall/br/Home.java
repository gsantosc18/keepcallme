package me.keepcall.br;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Home extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
			SharedPreferences settings = getPreferences(MODE_PRIVATE);
			String dateStr = settings.getString("date", null);
			if (dateStr == null) {
				dateStr = sdf.format(new Date());
				Editor editor = settings.edit();
				editor.putString("date", dateStr);
				editor.commit();
			}
			((TextView) findViewById(R.id.text)).setText("Rodando desde\r\n" + dateStr);
		} catch (Exception e) {
			Log.e("keepCall.me", "", e);
		}
	}
}
