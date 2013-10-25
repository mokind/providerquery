package de.mokind.providerquery.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PrefUtils {
	
	public static final String LOG_TAG = "NetzabfrageO2";
	
	private static final String NUMBER_CHECK_REG_EX = "(^49|^0049|^0){1}(15|16|17|[2-7]|8[1-9]|9[1-9]){1}.*";

	public static boolean isNumberCheckable(Context context, String phoneNumber){

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if ( prefs.getBoolean("NumberRegExCheck", true)){
			String pureNumbers = phoneNumber.replaceAll("\\D", "");
			String regEx = NUMBER_CHECK_REG_EX;
			if (pureNumbers.matches(regEx)){
				Log.d(PrefUtils.LOG_TAG, "NumberRegExCheck '"+phoneNumber+"' match");
			}else{
				Log.d(PrefUtils.LOG_TAG, "NumberRegExCheck '"+phoneNumber+"' NO MATCH");
				return false;
			}
		}
		return true;
	}
	
	public static String trimNumber(Context context, String phoneNumber) {
		if (phoneNumber != null) {
			phoneNumber = skipDualCode(context, phoneNumber);
			phoneNumber = phoneNumber.replace("+", "00");
			phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
		}
		return phoneNumber;
	}

	public static String skipDualCode(Context context, String phoneNumber) {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("NumberSkipDualCode", true)) {
			if (phoneNumber.startsWith("*1") || phoneNumber.startsWith("*2")) {
				Toast.makeText(
						context,
						"Convert from " + phoneNumber + " to "
								+ phoneNumber.substring(2), Toast.LENGTH_SHORT)
						.show();
				phoneNumber = phoneNumber.substring(2);
			}
		} else {
			// Toast.makeText(context, "Dual disabled for " + phoneNumber,
			// Toast.LENGTH_SHORT).show();
		}
		return phoneNumber;
	}
}
