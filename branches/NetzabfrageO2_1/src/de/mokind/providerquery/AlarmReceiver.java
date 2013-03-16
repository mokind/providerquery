package de.mokind.providerquery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import de.mokind.providerquery.db.NetworkDatabase;
import de.mokind.providerquery.util.PrefUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
	
	private static final int SEND_AT_ONCE = 20;
	
	private static PendingIntent pIntent = null; 

	@Override
	public void onReceive(Context context, Intent intent) {
		// get 10 numbers and request them..
		
		// check call log of billing period
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int billingPeriodStart = 1;
		try {
			billingPeriodStart = Integer.parseInt(prefs.getString("billing_period_start", "1"));
		} catch (NumberFormatException e) {
			Log.e(PrefUtils.LOG_TAG, "getList() Preference billing_period_start ", e);
		}
		Calendar cal = new GregorianCalendar();
		if (cal.get(Calendar.DAY_OF_MONTH) < billingPeriodStart){
			cal.add(Calendar.MONTH, -1);
		}
		cal.set(Calendar.DAY_OF_MONTH, billingPeriodStart);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Cursor cursor = context.getContentResolver().query( 
                android.provider.CallLog.Calls.CONTENT_URI, 
                new String[]{android.provider.CallLog.Calls.NUMBER, 
                		android.provider.CallLog.Calls.DURATION},
                android.provider.CallLog.Calls.TYPE + " = ? AND " + android.provider.CallLog.Calls.DATE + " > ? AND " + android.provider.CallLog.Calls.DURATION + " > 0 ", 
                new String[]{android.provider.CallLog.Calls.OUTGOING_TYPE + "", 
                		cal.getTimeInMillis() + ""}, 
                android.provider.CallLog.Calls.DATE + " DESC"); 
		
		NetworkDatabase db = new NetworkDatabase(context);
		
		ArrayList<String> phoneNumbers = new ArrayList<String>();
		
		if (cursor.getCount() > 0){
			int i = 0;
			// get phone number to request
			do{
				cursor.moveToNext();
				String phoneNumber = cursor.getString(0);
				String provider = db.getNetwork(phoneNumber);
				if (provider == null){
					i++;
					int status = db.getStatus(phoneNumber);
					if (status != NetworkDatabase.STATUS_QUEUED && status != NetworkDatabase.STATUS_SEND && !phoneNumbers.contains(phoneNumber)){
						phoneNumbers.add(phoneNumber);
					}
				}				
				
			}while (i < SEND_AT_ONCE && !cursor.isLast());
		}
		
		if (!cursor.isLast()){
			registerNextAlarm(context);
		}
		
		cursor.close();
		
		// concat message and send requests
		if (phoneNumbers.size() > 0){
			String message = "NetzabfrageO2:";
			for (String phoneNumber: phoneNumbers){
				message += "\n" + phoneNumber;
				NetworkRequester.getInstance().requestNetwork(context, phoneNumber);
			}
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
		
	}
	
	public static void registerNextAlarm(Context context){
		//get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		// set to 0:00 and next day...		
		cal.add(Calendar.DATE, 0);
		cal.set(Calendar.HOUR, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		registerAlarm(context, cal.getTimeInMillis());
	}
	
	public static void registerFirstAlarm(Context context){
		//get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		// set to 0:00 and next day...
		cal.add(Calendar.SECOND, 1);
		registerAlarm(context, cal.getTimeInMillis());
	}
	 
	private static void registerAlarm(Context context, long timeInMillis){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(MainActivity.PREFS_KEY_REQUEST_ALL, false)){

			Intent intent = new Intent(context, AlarmReceiver.class);

			pIntent = PendingIntent.getBroadcast(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// Get the AlarmManager service
		   	AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		   	am.set(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
		}
   	}
	
	public static void unRegisterAlarm(Context context){
		// Get the AlarmManager service
	   	AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	   	am.cancel(pIntent);
	}
	
}
