/**
 * 
 */
package de.mokind.providerquery.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import de.mokind.providerquery.MainActivity;
import de.mokind.providerquery.NetworkRequester;
import de.mokind.providerquery.db.NetworkDatabase;

/**
 * Utility class to get the user wanted a view on the data
 * 
 * @author monsterkind
 *
 */
public abstract class LoadList {
	
	public static final String PROVIDER_O2 = "o2";
	public static final String PROVIDER_EPLUS = "E-Plus";
	public static final String PROVIDER_D1 = "T-Mobile";
	public static final String PROVIDER_D2 = "Vodafone";
	public static final String[] PROVIDERS = new String[]{PROVIDER_O2, PROVIDER_D1, PROVIDER_D2, PROVIDER_EPLUS};
	public static final String ROW_LANDLINE = "Festnetz (oder anderes Netz)";
	public static final String ROW_UNKNOWN = "Unbekannt";
	public static final String ROW_UNCHECKED = "Ausland, bzw. Servicenummern";
	public static final String ROW_FREE_MINUTES = "Freiminuten";
	public static final String ROW_FLATRATE = "Flatrate";
	
	public static final String KEY_NAME = "name";
	public static final String KEY_MINUTES = "duration";
	public static final String KEY_PROGRESS = "progress";
	public static final String KEY_SMS = "sms";

	private static SparseArray<Map<String, Sum>> dataArray = null;
	
//	
//	private static boolean running = false;
//	private static boolean doRun = false;
	

	/**
	 * Calculates the start of the billing period as a {@link Calendar}
	 * @param context
	 * @int monthOffset month offset (0 for current billing period)
	 * @return
	 */
	public static Calendar getBillingPeriodEnd(Context context, int monthOffset){
		return getBillingPeriod(context, monthOffset, false);
	}
	
	/**
	 * Calculates the start of the billing period as a {@link Calendar}
	 * @param context
	 * @int monthOffset month offset (0 for current billing period)
	 * @return
	 */
	public static Calendar getBillingPeriodStart(Context context, int monthOffset){
		return getBillingPeriod(context, monthOffset, true);
	}
	
	/**
	 * Calculates the start/end of the billing period as a {@link Calendar}
	 * @param context
	 * @int monthOffset month offset (0 for current billing period)
	 * @return
	 */
	public static Calendar getBillingPeriod(Context context, int monthOffset, boolean start){
		Log.d(PrefUtils.LOG_TAG, "getBillingPeriod(" + context + "," + monthOffset + "," + start + ")");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int billingPeriodStart = 1;
		try {
			billingPeriodStart = Integer.parseInt(prefs.getString("billing_period_start", "5"));
		} catch (NumberFormatException e) {
			Log.e(PrefUtils.LOG_TAG, "getList() Preference billing_period_start ", e);
		}
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, monthOffset);
		if (cal.get(Calendar.DAY_OF_MONTH) < billingPeriodStart){
			cal.add(Calendar.MONTH, -1);
		}
		cal.set(Calendar.DAY_OF_MONTH, billingPeriodStart);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (!start){
			cal.add(Calendar.DATE, -1);
			cal.add(Calendar.MONTH, 1);
		}
		return cal;
	}
	
	private static Map<String, Sum> getData(int monthOffset){
		if (dataArray == null){
			dataArray = new SparseArray<Map<String, Sum>>(MainActivity.BILLING_PAGE_COUNT);
		}
		Map<String, Sum>  data = dataArray.get(monthOffset);
		if (data == null){
			data = new LinkedHashMap<String, Sum>();
			putData(data, monthOffset);
		}
		return data;
	}
	
	private static void putData(Map<String, Sum> object, int monthOffset){
		if (dataArray == null){
			dataArray = new SparseArray<Map<String, Sum>>(MainActivity.BILLING_PAGE_COUNT);
		}
		dataArray.put(monthOffset, object); 
	}
	
	/**
	 * 
	 * @param context
	 * @return
	 */
	public static void loadList(final Context context, final int monthOffset, final Runnable whenDone){
		Log.d(PrefUtils.LOG_TAG, "loadList("+monthOffset+")");
		if (context == null){
			Log.d(PrefUtils.LOG_TAG, "context == null !!");
			return;
		}
			
		// init data
		final Map<String, Sum>  data = getData(monthOffset);
		new Thread(new Runnable(){
			public void run() {
				
				synchronized(data){
				
					// get apps preferences
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					int free_minutes = 0;
					try {
						free_minutes = Integer.parseInt(prefs.getString("free_minutes", "100"));
					} catch (NumberFormatException e) {
						Log.e(PrefUtils.LOG_TAG, "getList() Preference minute_pack ", e);
					}
					boolean flatrateO2 = prefs.getBoolean("flat_o2", true);
					boolean flatrateEplus = prefs.getBoolean("flat_eplus", false);
					boolean flatrateD1 = prefs.getBoolean("flat_d1", false);
					boolean flatrateD2 = prefs.getBoolean("flat_d2", false);
					boolean landlineFlatrate = prefs.getBoolean("flat_other", false);
					
							
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
					// add unchecked row
					data.put(ROW_UNCHECKED, new Sum (ROW_UNCHECKED, 0, 0));
					// add unknown row
					data.put(ROW_UNKNOWN, new Sum (ROW_UNKNOWN, 0, 0));
				
					// get call log data
					Calendar calStart = getBillingPeriodStart(context, monthOffset);
					Calendar calEnd = getBillingPeriodEnd(context, monthOffset);
					
					// sum up provider data with database contents
					Cursor c = context.getContentResolver().query( 
			                android.provider.CallLog.Calls.CONTENT_URI, 
			                new String[]{android.provider.CallLog.Calls.NUMBER, android.provider.CallLog.Calls.DURATION},
			                android.provider.CallLog.Calls.TYPE + " = ? AND " + android.provider.CallLog.Calls.DATE + " > ? AND " + android.provider.CallLog.Calls.DATE + " <= ? AND " + android.provider.CallLog.Calls.DURATION + " > 0 ",
			                new String[]{android.provider.CallLog.Calls.OUTGOING_TYPE + "", calStart.getTimeInMillis() + "", calEnd.getTimeInMillis() + ""}, 
			                android.provider.CallLog.Calls.DATE + " DESC"); 
					
					if (c.getCount() > 0){
						do {
							c.moveToNext();
							String number = c.getString(0);
							int seconds = c.getInt(1) + 1; // + 1 is the value that O2 takes for billing
							// 'Taktung' = 60 seconds
							int minutes = seconds / 60 + (seconds%60 > 0 ? 1 : 0);
							String provider = null;
							if (!PrefUtils.isNumberCheckable(context, number)){
								provider = ROW_UNCHECKED;
							}else{
								provider = db.getNetwork(number);
							}
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
								Log.e(PrefUtils.LOG_TAG, "getList() Provider unknown '" + provider + "'");
							}
							
						}while (!c.isLast());
					}
					
					// SMS
//					c = context.getContentResolver().query( 
//							Uri.parse("content://sms/sent"), 
//			                new String[]{"address"},
//			                " date > ? AND date <= ? ",
//			                new String[]{calStart.getTimeInMillis() + "", calEnd.getTimeInMillis() + ""}, 
//			                " date DESC"); 
//					if (c.getCount() > 0){
//						do {
//							c.moveToNext();
//							String number = c.getString(0);
//							String provider = null;
//							if (!PrefUtils.isNumberCheckable(context, number)){
//								provider = ROW_UNCHECKED;
//							}else{
//								provider = db.getNetwork(number);
//							}
//							if (provider == null){
//								provider = ROW_UNKNOWN;
//							}
//							Sum sum = data.get(provider);
//							if (sum != null){
//								sum.setSmsCount(1 + sum.getSmsCount());
//								Sum numberSum = sum.getChildren().get(number);
//								if (numberSum == null){
//									numberSum = new Sum(number, 0, -1);
//									sum.getChildren().put(number, numberSum);
//								}
//								numberSum.setSmsCount(numberSum.getSmsCount() + 1);							
//							}else{
//								Log.e(PrefUtils.LOG_TAG, "getList() Provider unknown '" + provider + "'");
//							}
//							
//						}while (!c.isLast());
//					}
					
					// sum up flatrates and minute packs
					Sum freeSum = null;
					Sum flatSum = null;
					
					if (flatrateO2 || flatrateEplus || flatrateD1 || flatrateD2 || landlineFlatrate || free_minutes > 0){
						if (free_minutes > 0){
							freeSum = new Sum (ROW_FREE_MINUTES, 0, free_minutes);
						}
						if (flatrateO2 || flatrateEplus || flatrateD1 || flatrateD2 || landlineFlatrate){
							flatSum = new Sum (ROW_FLATRATE, 0, 0);
						}			
						
						ArrayList<String> provider = new ArrayList<String>(Arrays.asList(PROVIDERS));
						for (Sum providerSum: data.values()){
							if (provider.contains(providerSum.getName())){
									if (PROVIDER_O2.equals(providerSum.getName()) 	 && flatrateO2 ||
										PROVIDER_EPLUS.equals(providerSum.getName()) && flatrateEplus || 
										PROVIDER_D1.equals(providerSum.getName()) 	 && flatrateD1 ||
										PROVIDER_D2.equals(providerSum.getName())    && flatrateD2)
									{
										// flatrate
										flatSum.setMinutes(flatSum.getMinutes() + providerSum.getMinutes());
										flatSum.getChildren().putAll(providerSum.getChildren());
									}else if (freeSum != null){
										// minute pack
										freeSum.setMinutes(freeSum.getMinutes() + providerSum.getMinutes());
										freeSum.getChildren().putAll(providerSum.getChildren());
									}
							}else if (ROW_UNKNOWN.equals(providerSum.getName()) || ROW_UNCHECKED.equals(providerSum.getName())){
								// unknown/unchecked -> discard
							}else if (landlineFlatrate){
								// landline flat
								flatSum.setMinutes(flatSum.getMinutes() + providerSum.getMinutes());
								flatSum.getChildren().putAll(providerSum.getChildren());
							}else if (freeSum != null){
								// minute pack
								freeSum.setMinutes(freeSum.getMinutes() + providerSum.getMinutes());
								freeSum.getChildren().putAll(providerSum.getChildren());
							}
						}
					}
					
					// only for sorting
					Map<String, Sum> tempdata = new LinkedHashMap<String, Sum>(data);
					data.clear();
					
					if (freeSum != null){
						// add free minutes row
						data.put(freeSum.getName(), freeSum);
					}
					if (flatSum != null){
						// add flat rate row
						data.put(flatSum.getName(), flatSum);
					}
					data.putAll(tempdata);
					
					if (whenDone != null){
						whenDone.run();
					}
					
					
				} // synchronized(data)
			}
			
		}).start();
		
		
	}
	
	/**
	 * create data for list adapter
	 * @param context
	 * @param provider
	 * @return
	 */
	public static ArrayList< HashMap<String, Object> > getDataArray(Context context, String provider, int monthOffset){
		Log.d(PrefUtils.LOG_TAG, "getDataArray(" + context + "," + provider + "," + monthOffset + ")");
		Map<String, Sum> data = getData(monthOffset);
		if (data == null){
			return null;
		}
		synchronized(data){
			
			ArrayList< HashMap<String, Object> > dataArray = new ArrayList< HashMap<String, Object> >();
			Map<String, Sum> myData = new LinkedHashMap<String, Sum>(data);
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
					row.put(KEY_SMS, sum);
					dataArray.add(row);
				}
			}
			return dataArray;
		}
	}
	
	public static void releaseReferences(){
		Log.d(PrefUtils.LOG_TAG, "releaseReferences(");
		dataArray = null;
	}

}
