package de.mokind.providerquery;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class MainPrefsActivity extends PreferenceActivity  {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        
        Preference romaingPref = findPreference("Roaming");
        if (romaingPref != null){
	        romaingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					if (preference.getPreferenceManager().getSharedPreferences().getBoolean(preference.getKey(), false)){
						AlertDialog.Builder builder = new AlertDialog.Builder(MainPrefsActivity.this);
			        	builder.setMessage("ACHTUNG: In Romaing-Netzen kann das Senden der Abfrage-SMS Kosten verursachen.")
			        	       .setCancelable(true)
			        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			        	           public void onClick(DialogInterface dialog, int id) {
			        	        	   dialog.cancel();
			        	           }
			        	       });
			        	AlertDialog alert = builder.create();
			        	alert.show();
					}
					return false;
				}
			});
        }
        
        
        Preference netCheckPref = findPreference("OverrideNetCheck");
        if (netCheckPref != null){
        	netCheckPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					if (preference.getPreferenceManager().getSharedPreferences().getBoolean(preference.getKey(), false)){
						AlertDialog.Builder builder = new AlertDialog.Builder(MainPrefsActivity.this);
			        	builder.setMessage("ACHTUNG: In anderen Netzen als O2 kann das Senden der Abfrage-SMS Kosten verursachen. Und macht idR keine Sinn. Setzen Sie diese Option nur, wenn Sie wissen dass sie im O2-Netz sind und die App etwas anderes vermutet.")
			        	       .setCancelable(true)
			        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			        	           public void onClick(DialogInterface dialog, int id) {
			        	        	   dialog.cancel();
			        	           }
			        	       });
			        	AlertDialog alert = builder.create();
			        	alert.show();
					}
					return false;
				}
			});
        }
    }
	

}
