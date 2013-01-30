package de.mokind.providerquery;

import de.mokind.providerquery.db.NetworkDatabase;
import de.mokind.providerquery.util.PrefUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Class to request provider names for phone numbers.
 * @author monsterkind
 *
 */
public class NetworkRequester {
	
	private static final String[] MNC_O2 = {"07", "08", "11"};
	
	private static final String SMS_OUTGOING_TEXT = "NETZ ";
	private static final String SMS_OUTGOING_NUMBER = "4636";
	private static final String SMS_INCOMING_TEXT_1 = "Sehr geehrter Kunde, die angefragte Nummer ist im Netz von ";
	private static final String SMS_INCOMING_TEXT_2 = " aktiv (Angabe ohne Gewähr).";
	private static final String SMS_INCOMING_TEXT_ERROR = "leider";
	private static final String SMS_INCOMING_NUMBER = "66399";
	
	public static final String NO_PROVIDER_TEXT = "-";
	public static final String CURRENTLY_REQUESTING_TEXT = "gesendet...";
	public static final String QUEUED_FOR_REQUESTING_TEXT = "warte...";
	
	private static NetworkRequester me = null;
	
	private NetworkRequester (){
		super();
	}
	
	public static NetworkRequester getInstance(){
		if (me == null){
			me = new NetworkRequester();
		}
		return me;
	}
	
    /**
     * Request provider name for given phone number
     * @param context
     * @param phoneNumber
     */
    public synchronized boolean requestNetwork(Context context, String phoneNumber){
    	boolean doRequest = PrefUtils.isNumberCheckable(context, phoneNumber);
 
    	Log.d(PrefUtils.LOG_TAG, "requestNetwork()");
    	
    	if (!doRequest){
			Toast.makeText(context, "Nummer '" + phoneNumber + "' ist wahrscheinlich Service- oder Auslandsnummer\n(Siehe Einstellungen: Nummernerkennung)", Toast.LENGTH_LONG).show();
			return false;
		}
    	
    	if (doRequest && allowedToSendSMS(context)){
    		Log.d(PrefUtils.LOG_TAG, "requestNetwork(): waitingQueue.add(" + phoneNumber );
    		putStatus(context, phoneNumber, NetworkDatabase.STATUS_QUEUED);
    		doNextRequest(context);
    	}else{
    		clearQueue(context);
    		Log.d(PrefUtils.LOG_TAG, "doNextRequest(): pendingPhoneNumber == " + null + " waitingQueue.clear()");
    	}
    	return true;
    }
    
    /**
     * Clears the waiting queue - if one request is waiting for an answer in the 
     * working queue it will be processed. (only one request could be waiting in that queue)
     */
    public synchronized void clearQueue(Context context){
    	NetworkDatabase db = new NetworkDatabase(context);
    	String[] queuedNumbers = db.getAllNumbers(NetworkDatabase.STATUS_QUEUED);
    	for (String phoneNumber: queuedNumbers){
    		db.putStatus(phoneNumber, NetworkDatabase.STATUS_NO_PROVIDER);
    	}
    	context.getContentResolver().notifyChange(Contacts.CONTENT_URI, null);
    }
    
    private synchronized void doNextRequest(Context context){
    	Log.d(PrefUtils.LOG_TAG, "doNextRequest()");
    	if (allowedToSendSMS(context)){
    		
    		NetworkDatabase db = new NetworkDatabase(context);
        	String[] queuedNumbers = db.getAllNumbers(NetworkDatabase.STATUS_QUEUED);
        	String[] sendNumbers = db.getAllNumbers(NetworkDatabase.STATUS_SEND);
    		
        	if (sendNumbers != null){
        		Log.d(PrefUtils.LOG_TAG, "sendNumbers == ");
        		for (String s: sendNumbers){
        			Log.d(PrefUtils.LOG_TAG, s);
        		}
        	}else{
        		Log.d(PrefUtils.LOG_TAG, "sendNumbers == null");
        	}
        	
	    	if ((sendNumbers == null || sendNumbers.length == 0) && (queuedNumbers != null && queuedNumbers.length > 0)){
	    		
	    		// get apps preferences
	    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    		boolean showSMS = prefs.getBoolean("ShowSMS", false);
	    		
	    		String phoneNumber = queuedNumbers[0];
    			Log.d(PrefUtils.LOG_TAG, "requestNetwork(): queuedNumbers[0] = " + phoneNumber);
				putStatus(context, phoneNumber, NetworkDatabase.STATUS_SEND);
				if (showSMS && context != null){
					Toast.makeText(context, "Sende SMS an " + SMS_OUTGOING_NUMBER + " :\"" + SMS_OUTGOING_TEXT + phoneNumber + "\"", Toast.LENGTH_SHORT).show();
				}
		        SmsManager sms = SmsManager.getDefault();
		        sms.sendTextMessage(SMS_OUTGOING_NUMBER, null, SMS_OUTGOING_TEXT + phoneNumber, null, null);
	    	}
    	}else{
    		clearQueue(context);
    		Log.d(PrefUtils.LOG_TAG, "doNextRequest(): pendingPhoneNumber == " + null + " waitingQueue.clear()");
    	}
    }
    
    public synchronized boolean receiveSMS(Context context, SmsMessage msg){
    	if (SMS_INCOMING_NUMBER.equals(msg.getOriginatingAddress())){
    		// get apps preferences
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    		boolean showSMS = prefs.getBoolean("ShowSMS", false);
    		
    		NetworkDatabase db = new NetworkDatabase(context);
        	String[] sendNumbers = db.getAllNumbers(NetworkDatabase.STATUS_SEND);

        	boolean gotAnswer = false;
        	if (sendNumbers != null && sendNumbers.length > 0){
	        	
	    		// get necessary information
	    		String phoneNumber = sendNumbers[0];
	    		Log.d(PrefUtils.LOG_TAG, "pendingPhoneNumber read out, set to null (was " + phoneNumber + ")");
	    		String msgBody = msg.getMessageBody().toString();
	            
	            // extract provider name
	            String provider = "?";
	            if (!msgBody.toLowerCase().contains(SMS_INCOMING_TEXT_ERROR)){
	            	gotAnswer = true;
	            	provider = msgBody.substring(SMS_INCOMING_TEXT_1.length(), msgBody.length() - SMS_INCOMING_TEXT_2.length());
	            }else{
	            	gotAnswer = true;
	            	provider = NO_PROVIDER_TEXT;
	            }
	            
	            if (gotAnswer){
		            putNumber (context, phoneNumber, provider);
		            
		            // display the new SMS message 
		            if (showSMS){
		            	String str = "";
		                str += "SMS von " + msg.getOriginatingAddress();                     
		                str += " :\"";
		                str += msgBody;
		                str += "\"";   
		            	Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
		            }else{
		            	CallReceiver.showProvider(context, phoneNumber, provider, true);
		            }
	            }else{
	            	putStatus(context, phoneNumber, NetworkDatabase.STATUS_QUEUED);
	            }
	            context.getContentResolver().notifyChange(Contacts.CONTENT_URI, null);
        	}
            
            // do next Request
        	if (gotAnswer){ // if no answer limit of o2 is reached - no request immmediatly
        		doNextRequest(context);
        	}
            return true;
    	}
    	return false;
    }

    private static boolean allowedToSendSMS(Context context){
    	
    	boolean allowedToSendSMS = false;
    	
    	// get apps preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	
		// check MNC (mobile network code) for O2 network
		TelephonyManager telMan = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		String simMCCMNC = telMan.getSimOperator();
		String netMCCMNC = telMan.getNetworkOperator();
		boolean isNetO2 = false;
		for (String MNC: MNC_O2){
			if (simMCCMNC.endsWith(MNC) || netMCCMNC.endsWith(MNC)){
				isNetO2 = true;
				break;
			}
		}
		String name = telMan.getSimOperatorName().equals(telMan.getNetworkOperatorName())?telMan.getSimOperatorName():(telMan.getSimOperatorName() + "(" + telMan.getNetworkOperatorName() + ")");
		if (!isNetO2){
			if (!prefs.getBoolean("OverrideNetCheck", false)){
				Toast.makeText(context, name + " scheint kein O2-Netz zu sein. SMS-Anfrage wird nicht gesendet.\nIn den Einstellungen kann das Senden erwzungen werden.", Toast.LENGTH_SHORT).show();
				allowedToSendSMS = false;
				return allowedToSendSMS;
			}
		}

		boolean forceRoaming = prefs.getBoolean("Roaming", false);
        
		// determine roaming
		boolean isRoaming = false;
		if (!forceRoaming){
			isRoaming = telMan.isNetworkRoaming();
			allowedToSendSMS = (isRoaming == false);
		}else{
			
			allowedToSendSMS = true;
		}
		
		if (allowedToSendSMS && !isNetO2){
			Toast.makeText(context, name + " scheint "+(isRoaming?"Roaming-Netz":"kein O2-Netz") + " zu sein. SMS-ANFRAGE WIRD TROTZDEM GESENDET! Bitte Einstellungen prüfen.", Toast.LENGTH_SHORT).show();
		}
		
		return allowedToSendSMS;
    }
    
    private void putNumber(Context context, String phoneNumber, String provider){ 
    	NetworkDatabase db = new NetworkDatabase(context);
    	db.putNetwork(phoneNumber, provider);
        context.getContentResolver().notifyChange(Contacts.CONTENT_URI, null);
    }
    
    private void putStatus(Context context, String phoneNumber, int status){
    	NetworkDatabase db = new NetworkDatabase(context);
    	db.putStatus(phoneNumber, status);
        context.getContentResolver().notifyChange(Contacts.CONTENT_URI, null);
    }
	
}
