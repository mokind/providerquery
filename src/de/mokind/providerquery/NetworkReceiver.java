package de.mokind.providerquery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

public class NetworkReceiver extends BroadcastReceiver{
	
	/**
     * Receive network answer and store it
     */
	@Override
	public void onReceive(Context context, Intent intent) {
		//---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;      
        if (bundle != null)
        {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            
    		// get apps preferences
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    		boolean consumeSMS = prefs.getBoolean("ConsumeSMS", true);
            
            for (int i=0; i<msgs.length; i++){
            	msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);   
            	if (NetworkRequester.getInstance().receiveSMS(context, msgs[i])){
            		if (consumeSMS){
            			abortBroadcast();
            		}
            	}
            }            
        }        
	}  

}
