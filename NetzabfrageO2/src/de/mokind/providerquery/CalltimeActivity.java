package de.mokind.providerquery;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;
import de.mokind.R;
import de.mokind.providerquery.db.NetworkDatabase;
import de.mokind.providerquery.util.LoadList;
import de.mokind.providerquery.util.Sum;

/**
 * Activity to show call times summarized over network providers 
 * 
 * @author monsterkind
 *
 */
public class CalltimeActivity extends Activity {
	
	//Constants
	public static final String PREFS_KEY_REQUEST_ALL = "prefs_request_all";
	
	
	//Members
	private Handler handler = new Handler();
	private ListView list = null;
	private TextView billingPediodView;
	private ViewBinder binder = new MyViewBinder();
	
	/**
	 * The view binder
	 * @author monsterkind
	 *
	 */
	private class MyViewBinder implements ViewBinder{

		public boolean setViewValue(View view, Object data, String textRepresentation) {

			if (data instanceof String && view instanceof TextView){
				((TextView)view).setText(textRepresentation);
			}else if (data instanceof Sum && view instanceof TextView) {
				Sum sum = (Sum)data;
				if (sum.isShowProgress()){
					((TextView)view).setText(sum.getMinutes() + " von " + sum.getMinutesMax() + " Minuten");
				}else{
					((TextView)view).setText(sum.getMinutes() + " Minuten");
				}
			}else if (data instanceof Sum && view instanceof ProgressBar){
				ProgressBar prog = (ProgressBar)view;
				Sum sum = (Sum)data;
				prog.setVisibility(sum.isShowProgress() ? View.VISIBLE : View.GONE );
				prog.setMax(sum.getMinutesMax());
				prog.setProgress(sum.getMinutes());
			}else if (view.getId() == R.id.calltime_provider_logo){
				view.setVisibility(View.GONE);
			}else if (view.getId() == R.id.calltime_sum_icon){
				if(LoadList.ROW_FREE_MINUTES.equals(textRepresentation)){
					((ImageView)view).setImageResource(R.drawable.icon_minutepack_2);
					view.setVisibility(View.VISIBLE);
				}else if (LoadList.ROW_FLATRATE.equals(textRepresentation)){
					((ImageView)view).setImageResource(R.drawable.icon_flatrate);
					view.setVisibility(View.VISIBLE);
				}else{
					int image = NetworkDatabase.getLogoSmall(textRepresentation);
					if (image != -1){
						((ImageView)view).setImageResource(image);
						view.setVisibility(View.VISIBLE);
					}else {
						if(LoadList.ROW_LANDLINE.equals(textRepresentation)){
							((ImageView)view).setImageResource(R.drawable.logo_landline_small);
							view.setVisibility(View.VISIBLE);
						}else{
							view.setVisibility(View.GONE);
						}
					}
				}
			}
			return true;
		}
	}
	
	/**
	 * My {@link ContentObserver}
	 * @author monsterkind
	 *
	 */
	private class MyContentObserver extends ContentObserver {

//		@SuppressLint("ParserError")
	    public MyContentObserver(Handler handler) {
	    	super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			setAdapter(list);
			billingPediodView.setText("Rechnungszeitraum: von " + DateFormat.getDateInstance().format(LoadList.getBillingPeriodStart(CalltimeActivity.this).getTime()) + " bis heute");
	        super.onChange(selfChange);
		}
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
//		onStart();
	}
	
	@Override
	protected void onStart(){//(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
		super.onStart();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.calltime_list);
		billingPediodView = (TextView)findViewById(R.id.billing_period);
		list = (ListView)findViewById(R.id.calltime_list);
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(getBaseContext(), ((TextView)view.findViewById(R.id.calltime_name)).getText(), Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(CalltimeActivity.this, ContactListActivity.class);
				TextView editText = ((TextView)view.findViewById(R.id.calltime_name));
			    String message = editText.getText().toString();
			    intent.putExtra(ContactListActivity.EXTRA_ROWNAME, message);
			    startActivity(intent);
			}
        });

		
		// list

		billingPediodView.setText("Rechnungszeitraum: von " + DateFormat.getDateInstance().format(LoadList.getBillingPeriodStart(this).getTime()) + " bis heute");
		
		setAdapter(list);
		
		MyContentObserver contentObserver = new MyContentObserver(handler);

	    this.getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
	}
	
	private void setAdapter(ListView list){
		SimpleAdapter dataAdapter = new SimpleAdapter(this, 
				LoadList.loadList(this), 
				R.layout.calltime_entry, 
				new String [] {LoadList.KEY_NAME, LoadList.KEY_MINUTES, LoadList.KEY_PROGRESS, LoadList.KEY_NAME, LoadList.KEY_NAME}, 
				new int[]{R.id.calltime_name, R.id.calltime_value,R.id.calltime_progressbar,R.id.calltime_provider_logo,R.id.calltime_sum_icon});
		dataAdapter.setViewBinder(binder);
		list.setAdapter(dataAdapter);
	}
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
        	Intent intent = new Intent(this, MainPrefsActivity.class);
        	startActivity(intent);
            return true;
        case R.id.delete:
        	{
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	builder.setMessage("Wirklich alle gefundenen Netz-Einträge löschen?")
	        	       .setCancelable(true)
	        	       .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	        	   NetworkDatabase db = new NetworkDatabase( CalltimeActivity.this);
	        	        	   db.deleteData();
	        	           }
	        	       })
	        	       .setNegativeButton("Nein", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	                dialog.cancel();
	        	           }
	        	       });
	        	AlertDialog alert = builder.create();
	        	alert.show();
	        	return true;
        	}
        case R.id.doRequest:
	        {
	        	AlertDialog.Builder builder = new AlertDialog.Builder(CalltimeActivity.this);
	        	builder.setMessage("ACHTUNG: Abfrage-SMS sind nur im O2-Netz kostenlos. Und davon werden gleich mehrere verschickt.")
	        	       .setCancelable(true)
	        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	        	   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CalltimeActivity.this);
	        	        	   Editor editor = prefs.edit();
	        	        	   editor.putBoolean(PREFS_KEY_REQUEST_ALL, true);
	        	        	   editor.commit();
	        	        	   AlarmReceiver.registerFirstAlarm(CalltimeActivity.this);
	        	           }
	        	       })
	        	       .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	                dialog.cancel();
	        	           }
	        	       });
	        	AlertDialog alert = builder.create();
	        	alert.show();
	        	
	        	return true;
	        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
	
}
