package de.mokind.providerquery;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.mokind.R;
import de.mokind.providerquery.db.NetworkDatabase;
import de.mokind.providerquery.util.PrefUtils;

public class CallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		// get apps preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean autoRequest = prefs.getBoolean("AutoRequest", true);
		boolean reRequest = prefs.getBoolean("ReRequest", true);
		
		String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		
		boolean requestable = PrefUtils.isNumberCheckable(context, phoneNumber);

		// get provider
		String provider = null;
		NetworkDatabase db = new NetworkDatabase(context);
		provider = db.getNetwork(phoneNumber);
		boolean showProvider = true;
		
		if (NetworkRequester.NO_PROVIDER_TEXT.equals(provider)){

		}else if (provider != null){
			if (reRequest){
				long lastAccess = db.getLastAccess(phoneNumber);
				int reRequestDelay = 5;
				try {
					reRequestDelay = prefs.getInt("ReRequestDelay", 5);
				} catch (Exception e) {
					Log.d(this.getClass().getName(), "onReceive("+context+", "+intent+") Wrong format ReRequestDelay, set to default" + 5);
					prefs.edit().putInt("ReRequestDelay", 5);
					prefs.edit().commit();
				}
				if (new Date((new Date()).getTime() - lastAccess).getDate() > reRequestDelay){
					showProvider = NetworkRequester.getInstance().requestNetwork(context, phoneNumber);
				}
			}
		}else{
			if (autoRequest && requestable){
				NetworkRequester.getInstance().requestNetwork(context, phoneNumber);
			}
		}
		if (showProvider){
			CallReceiver.showProvider(context, phoneNumber, provider, requestable);
		}
	}
	
	/**
	 * Shows toast with provider info
	 * @param context
	 * @param provider
	 */
	public static void showProvider(Context context, String number, String provider, boolean requestable){
		
		String txt = null;
		if (!requestable){
			txt = "Auslands-, bzw. Servicenummer";
		}else{
			if (NetworkRequester.NO_PROVIDER_TEXT.equals(provider)){
				txt = "Festnetz (vermutlich)";
			}else if (provider != null){
				txt = provider;
			}else{
				txt = "Netz (noch) unbekannt...";
			}
		}
		if (number != null){
			txt = number + "\n" + txt;
		}

		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.toast_layout, new LinearLayout(context));
		
		ImageView logo = (ImageView) layout.findViewById(R.id.provider_logo);
		logo.setImageResource(NetworkDatabase.getLogo(provider));

		TextView text = (TextView) layout.findViewById(R.id.toast_layout_text);
		text.setText(txt);
		text.setTextColor(NetworkDatabase.getColor(provider));
		TextView disclaimer = (TextView) layout.findViewById(R.id.toast_layout_disclaimer);
		disclaimer.setText("Angaben ohne Gewähr");
		Toast toast = new Toast(context);
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

}
