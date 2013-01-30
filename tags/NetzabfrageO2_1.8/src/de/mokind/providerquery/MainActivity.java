package de.mokind.providerquery;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.mokind.R;
import de.mokind.providerquery.db.NetworkDatabase;

public class MainActivity extends FragmentActivity {
	
    //Constants
	public static final String PREFS_KEY_REQUEST_ALL = "prefs_request_all";
	public static final int BILLING_PAGE_COUNT = 5;

    private ViewPager pager;

    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        setContentView(R.layout.main);

        // Instantiate a ViewPager and a PagerAdapter.
        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(BILLING_PAGE_COUNT - 1); // last page -> want to scroll to the left
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
	        	        	   NetworkDatabase db = new NetworkDatabase( MainActivity.this);
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
	        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	        	builder.setMessage("ACHTUNG: Abfrage-SMS sind nur im O2-Netz kostenlos. Und davon werden gleich mehrere verschickt.")
	        	       .setCancelable(true)
	        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	        	   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
	        	        	   Editor editor = prefs.edit();
	        	        	   editor.putBoolean(PREFS_KEY_REQUEST_ALL, true);
	        	        	   editor.commit();
	        	        	   AlarmReceiver.registerFirstAlarm(MainActivity.this);
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

    /**
     * A simple pager adapter that represents 5 {@link ScreenSlidePageFragment} objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return CalltimeFragment.create(position);
        }

        @Override
        public int getCount() {
            return BILLING_PAGE_COUNT;
        }
    }
    
    
    
    
}
