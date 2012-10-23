/**
 * 
 */
package de.mokind.providerquery.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import de.mokind.providerquery.NetworkRequester;
import de.mokind.providerquery.db.NetworkDatabase;

/**
 * Utility class to get the user wanted a view on the data
 * 
 * @author monsterkind
 *
 */
public abstract class LoadList {
	
	public static final String MY_PROVIDER = "o2";
	public static final String[] OTHER_PROVIDERS = new String[]{"T-Mobile", "Vodafone", "E-Plus"};
	public static final String ROW_LANDLINE = "Festnetz (oder anderes Netz)";
	public static final String ROW_UNKNOWN = "Unbekannt";
	public static final String ROW_FREE_MINUTES = "Freiminuten";
	public static final String ROW_FLATRATE = "Flatrate";
	
	public static final String KEY_NAME = "name";
	public static final String KEY_MINUTES = "duration";
	public static final String KEY_PROGRESS = "progress";

	private static Map<String, Sum> data = null;
	
	/**
	 * Calculates the start of the billing period as a {@link Calendar}
	 * @param context
	 * @return
	 */
	public static Calendar getBillingPeriodStart(Context context){
		return getBillingPeriodStart(context, 0);
	}
	
	/**
	 * Calculates the start of the billing period as a {@link Calendar}
	 * @param context
	 * @int monthOffset month offset (0 for current billing period)
	 * @return
	 */
	public static Calendar getBillingPeriodStart(Context context, int monthOffset){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int billingPeriodStart = 1;
		try {
			billingPeriodStart = Integer.parseInt(prefs.getString("billing_period_start", "5"));
		} catch (NumberFormatException e) {
			Log.e(LoadList.class.getClass().getName(), "getList() Preference billing_period_start ", e);
		}
		Calendar cal = new GregorianCalendar();
		if (cal.get(Calendar.DAY_OF_MONTH) < billingPeriodStart){
			cal.add(Calendar.MONTH, -1 + monthOffset);
		}
		cal.set(Calendar.DAY_OF_MONTH, billingPeriodStart);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	/**
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList< HashMap<String, Object> > loadList(Context context){
		// get apps preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int free_minutes = 0;
		try {
			free_minutes = Integer.parseInt(prefs.getString("free_minutes", "100"));
		} catch (NumberFormatException e) {
			Log.e(LoadList.class.getName(), "getList() Preference minute_pack ", e);
		}
		boolean ownFlatrate = prefs.getBoolean("flat_o2", true);
		boolean landlineFlatrate = prefs.getBoolean("flat_other", false);
		
				
		// init data
		data = new LinkedHashMap<String, Sum>();

		// get database
		NetworkDatabase db = new NetworkDatabase(context);
		
		// create provider rows
		String[] providers =  db.getKnownProviders();
		for (String provider: providers){
			if (NetworkRequester.NO_PROVIDER_TEXT.equals(provider)){
				data.put(provider, new Sum (ROW_LANDLINE, 0, 0));	
			} else{
				data.put(provider, new Sum (provider, 0, 0));
			}
		}
		// add unknown row
		data.put(ROW_UNKNOWN, new Sum (ROW_UNKNOWN, 0, 0));
	
		// get call log data
		Calendar cal = getBillingPeriodStart(context);
		new Date(cal.getTimeInMillis());
		
		Cursor c = context.getContentResolver().query( 
                android.provider.CallLog.Calls.CONTENT_URI, 
                new String[]{android.provider.CallLog.Calls.NUMBER, android.provider.CallLog.Calls.DURATION},
                android.provider.CallLog.Calls.TYPE + " = ? AND " + android.provider.CallLog.Calls.DATE + " > ? AND " + android.provider.CallLog.Calls.DURATION + " > 0 ",
                new String[]{android.provider.CallLog.Calls.OUTGOING_TYPE + "", cal.getTimeInMillis() + ""}, 
                android.provider.CallLog.Calls.DATE + " DESC"); 
		
		// sum up provider data with database contents
		if (c.getCount() > 0){
			do {
				c.moveToNext();
				String number = c.getString(0);
				int seconds = c.getInt(1) + 1; // + 1 is the value that O2 takes for billing
				// TODO: Add 'Taktung'
				int minutes = seconds / 60 + (seconds%60 > 0 ? 1 : 0);
				String provider = db.getNetwork(number);
				if (provider == null){
					provider = ROW_UNKNOWN;
				}
				Sum sum = data.get(provider);
				if (sum != null){
					sum.setMinutes(minutes + sum.getMinutes());
					Sum numberSum = sum.getChildren().get(number);
					if (numberSum == null){
						numberSum = new Sum(number, 0, -1);
						sum.getChildren().put(number, numberSum);
					}
					numberSum.setMinutes(numberSum.getMinutes() + minutes);							
				}else{
					Log.e(LoadList.class.getName(), "getList() Provider unknown '" + provider + "'");
				}
				
			}while (!c.isLast());
		}
		
		// sum up flatrates and minute packs
		Sum freeSum = null;
		Sum flatSum = null;
		
		if (ownFlatrate || landlineFlatrate || free_minutes > 0){
			if (free_minutes > 0){
				freeSum = new Sum (ROW_FREE_MINUTES, 0, free_minutes);
			}
			if (ownFlatrate || landlineFlatrate){
				flatSum = new Sum (ROW_FLATRATE, 0, 0);
			}			
			
			ArrayList<String> otherProvider = new ArrayList<String>(Arrays.asList(OTHER_PROVIDERS));
			for (Sum providerSum: data.values()){
				if (otherProvider.contains(providerSum.getName()) && freeSum != null){
					// other providers
					freeSum.setMinutes( freeSum.getMinutes() + providerSum.getMinutes());
					freeSum.getChildren().putAll(providerSum.getChildren());
				}else if (MY_PROVIDER.equals(providerSum.getName())){
						// my provider
						if (ownFlatrate){
							flatSum.setMinutes(flatSum.getMinutes() + providerSum.getMinutes());
							flatSum.getChildren().putAll(providerSum.getChildren());
						}else if (freeSum != null){
							freeSum.setMinutes(freeSum.getMinutes() + providerSum.getMinutes());
							freeSum.getChildren().putAll(providerSum.getChildren());
						}
				}else if (ROW_UNKNOWN.equals(providerSum.getName())){
					// unknown -> discard
				}else if (landlineFlatrate){
					// landline flat
					flatSum.setMinutes(flatSum.getMinutes() + providerSum.getMinutes());
					flatSum.getChildren().putAll(providerSum.getChildren());
				}else if (freeSum != null){
					// landline minute pack
					freeSum.setMinutes(freeSum.getMinutes() + providerSum.getMinutes());
					freeSum.getChildren().putAll(providerSum.getChildren());
				}
			}
		}
		
		// only for sorting
		Map<String, Sum> tempdata = data;
		data = new LinkedHashMap<String, Sum>();
		
		if (freeSum != null){
			// add free minutes row
			data.put(freeSum.getName(), freeSum);
		}
		if (flatSum != null){
			// add flat rate row
			data.put(flatSum.getName(), flatSum);
		}
		data.putAll(tempdata);

		return getDataArray(context, null);
	}
	
	/**
	 * create data for list adapter
	 * @param context
	 * @param provider
	 * @return
	 */
	public static ArrayList< HashMap<String, Object> > getDataArray(Context context, String provider){
		if (data == null){
			loadList(context);
		}
		ArrayList< HashMap<String, Object> > dataArray = new ArrayList< HashMap<String, Object> >();
		Map<String, Sum> myData = data;
		if (provider != null){
			if (provider.equals(ROW_LANDLINE)){
				provider = NetworkRequester.NO_PROVIDER_TEXT;
			}
			Sum providerSum = myData.get(provider);
			if (providerSum != null){
				myData = providerSum.getChildren();
			}
		}
		if (myData != null){
			ArrayList<Sum> dataList = new ArrayList<Sum>(myData.values().size());
			dataList.addAll(myData.values());
			if (provider != null){
				Collections.sort(dataList);
			}
			for (Sum sum: dataList){
				HashMap<String, Object> row = new HashMap<String, Object>();
				row.put(KEY_NAME, sum.getName());
				row.put(KEY_MINUTES, sum);
				row.put(KEY_PROGRESS, sum);
				dataArray.add(row);
			}
		}
		return dataArray;
	}

}
