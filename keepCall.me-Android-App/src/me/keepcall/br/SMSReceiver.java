package me.keepcall.br;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

	private static Map<String, Integer> notificationMap = new HashMap<String, Integer>();
	private Pattern phonePattern = Pattern.compile("[0-9]{11,}");
	private Pattern diaMesPattern = Pattern.compile("[0-9]{2}/[0-9]{2}");
	private Pattern horaMinutoPattern = Pattern.compile("[0-9]{2}:[0-9]{2}");
	private Pattern qtdPattern = Pattern.compile(" [0-9]{1,2} |<[0-9]{1,2}>");
	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm yyyy", Locale.US);
	private SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.US);
	private String currentYear;

	public static void clearNotificationMap() {
		notificationMap.clear();
	}

	public Map<String, Integer> getNotificationMap() {
		return notificationMap;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if ("me.keepcall.NOTIFICATION_CLEAR".equals(intent.getAction())) {
				notificationMap.clear();
			} else {
				Object messages[] = (Object[]) intent.getExtras().get("pdus");
				for (int n = 0; n < messages.length; n++)
					processSMS(SmsMessage.createFromPdu((byte[]) messages[n]), context);
			}
		} catch (Exception e) {
			Log.e("keepCall.me", "", e);
		}
	}

	public void processSMS(SmsMessage smsMessage, Context context) throws ParseException {
		List<Attempt> attempts = new ArrayList<Attempt>();
		currentYear = sdfYear.format(new Date());
		readFromAll(smsMessage.getDisplayMessageBody(), attempts);
		if (attempts.size() != 0) {
			abortBroadcast();
			readContacts(context, attempts);
			insertCallLog(context, attempts);
			notifyUser(context, attempts);
		}
	}

	// A Claro manda uma mensagem pra cada número que ligou
	// Voce recebeu 1 ligacao de 06284559198, que nao deixou recado na
	// Secretaria Claro. Ultima ligacao: 08/04, 12:34.\nClaro
	// Voce recebeu 2 ligacoes de 06284559198, que nao deixou recado na
	// Secretaria Claro. Ultima ligacao: 08/04, 13:03.\nClaro
	// Seu celular tem 1 ligação perdida de 06292691135, 02/06, 13:16. CLARO
	//
	// A Vivo manda uma mensagem pra cada número que ligou
	// "Vivo Avisa:  o numero 06293634966 tentou ligar para voce, dia 08/04 as 14:27. Vivo. Conectados vivemos melhor."
	// "Vivo Avisa:  o numero 06284559198 tentou ligar 2 vezes e a ultima vez foi, dia 08/04 as 14:28. Vivo. Conectados vivemos melhor."
	//
	// Tim agrupa tudo em uma mensagem
	// "Voce recebeu ligacoes de: <08/04> 0416293634966 <1>  13:47hs"
	// "Voce recebeu ligacoes de: <08/04> 0416284559198 <1>  13:52hs 0416293634966 <1>  13:52hs 0416239429652 <1>  13:52hs"
	//
	// Oi agrupa tudo em uma mensagem
	// "Seu Oi recebeu ligações de: 06293634966 08/04 16:45 <1>"
	// "Seu Oi recebeu ligações de: 06293634966 08/04 16:45 <1> 06288634960 08/04 17:00 <2>"
	private void readFromAll(String body, List<Attempt> attempts) throws ParseException {
		List<String> phones = getPhonesAll(body);
		if (phones.size() != 0) {
			Matcher qtdMatcher = qtdPattern.matcher(body);
			Matcher diaMesMatcher = diaMesPattern.matcher(body);
			Matcher horaMinutoMatcher = horaMinutoPattern.matcher(body);
			String diaMes = null;
			int qtd = 1;
			for (int i = 0; i < phones.size(); i++) {
				String phone = phones.get(i);
				if (diaMesMatcher.find())
					diaMes = diaMesMatcher.group();
				if (qtdMatcher.find())
					qtd = Integer.parseInt(qtdMatcher.group().trim().replace("<", "").replace(">", ""));
				if (diaMes != null && horaMinutoMatcher.find()) {
					long date = sdf.parse(diaMes + " " + horaMinutoMatcher.group() + " " + currentYear).getTime();
					for (int j = 0; j < qtd; j++)
						attempts.add(new Attempt(phone, date));
				}
			}
		}
	}

	private List<String> getPhonesAll(String body) {
		List<String> phones = new ArrayList<String>();
		Matcher m = phonePattern.matcher(body);
		while (m.find())
			phones.add(m.group());
		return phones;
	}

	private void readContacts(Context context, List<Attempt> attempts) {
		for (Attempt attempt : attempts) {
			String[] projection = { Phone.DISPLAY_NAME, Phone.NUMBER };
			Cursor people = context.getContentResolver().query(Phone.CONTENT_URI, projection, null, null, null);
			String name = attempt.getPhone();
			String lastEight = attempt.getPhone().substring(attempt.getPhone().length() - 8);
			while (people.moveToNext()) {
				if (people.getString(1).replaceAll("\\W", "").endsWith(lastEight)) {
					name = people.getString(0);
					break;
				}
			}
			attempt.setName(name);
			people.close();
		}
	}

	private void insertCallLog(Context context, List<Attempt> attempts) {
		for (Attempt attempt : attempts) {
			ContentValues values = new ContentValues();
			values.put(Calls.NUMBER, attempt.getPhone());
			values.put(Calls.DATE, attempt.getDate());
			values.put(Calls.TYPE, Calls.MISSED_TYPE);
			values.put(Calls.CACHED_NAME, attempt.getName());
			values.put(CallLog.Calls.DURATION, 0);
			context.getContentResolver().insert(Calls.CONTENT_URI, values);
		}
	}

	private void notifyUser(Context context, List<Attempt> attempts) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(android.R.drawable.stat_notify_missed_call);
		builder.setAutoCancel(true);
		builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Clear.class), PendingIntent.FLAG_UPDATE_CURRENT));
		builder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, new Intent("me.keepcall.NOTIFICATION_CLEAR"), PendingIntent.FLAG_CANCEL_CURRENT));
		createText(attempts, builder);
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, builder.build());
	}

	private void createText(List<Attempt> attempts, NotificationCompat.Builder builder) {
		for (Attempt attempt : attempts) {
			Integer qtd = notificationMap.get(attempt.getName());
			notificationMap.put(attempt.getName(), qtd == null ? 1 : ++qtd);
		}
		int qtdAll = 0;
		for (Integer qtd : notificationMap.values())
			qtdAll += qtd;
		if (qtdAll == 1) {
			builder.setContentTitle("Chamada perdida");
			builder.setContentText(notificationMap.keySet().iterator().next());
		} else {
			String fromChain = "";
			int count = 0;
			for (Entry<String, Integer> entry : notificationMap.entrySet()) {
				fromChain += entry.getValue() + " " + entry.getKey() + ", ";
				count += entry.getValue();
			}
			builder.setContentTitle(count + " chamadas perdidas");
			builder.setContentText(fromChain.substring(0, fromChain.lastIndexOf(", ")));
		}
	}
}
