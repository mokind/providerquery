package de.mokind.providerquery.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PrefUtils {
	
	private static final String NUMBER_CHECK_REG_EX = "(^49|^0049|^0){1}(15|16|17|[2-7]|8[1-9]|9[1-9]){1}.*";

	public static boolean isNumberCheckable(Context context, String phoneNumber){
		
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if ( prefs.getBoolean("NumberRegExCheck", true)){
			String pureNumbers = phoneNumber.replaceAll("\\D", "");
			String regEx = NUMBER_CHECK_REG_EX;
			if (pureNumbers.matches(regEx)){
				Log.d(PrefUtils.class.getCanonicalName(), "NumberRegExCheck '"+phoneNumber+"' match");
			}else{
				Log.d(PrefUtils.class.getCanonicalName(), "NumberRegExCheck '"+phoneNumber+"' NO MATCH");
				return false;
			}
		}
		return true;
	}
	
	
	
}
