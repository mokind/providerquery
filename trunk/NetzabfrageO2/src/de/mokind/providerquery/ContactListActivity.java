package de.mokind.providerquery;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import de.mokind.providerquery.util.LoadList;

/**
 * Main Activity class shows contact list and has main menu
 * 
 * @author monsterkind
 *
 */
public class ContactListActivity extends Activity {
	
	private ArrayList< HashMap<String, Object> > dataArray = new ArrayList< HashMap<String, Object> >();
	
	/*
	 * constants
	 */
	
    private static final String[] PHONE_NUMBER_PROJECTION = new String[] {
        Phone._ID,
        Phone.NUMBER
    };
    
	private static final String[] CONTACTS_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME
    };
	
	

	
	/*********** Types **********************/
	
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
			dataArray = LoadList.loadList(ContactListActivity.this);
			dataAdapter.notifyDataSetChanged();
	        super.onChange(selfChange);
		}
	 }
	
	
	/**
	 * The view binder
	 * @author monsterkind
	 *
	 */
	private class MyViewBinder implements ViewBinder{

		public boolean setViewValue(View view, Object data, String textRepresentation) {

			if (data instanceof String && view instanceof TextView){
				((TextView)view).setText(textRepresentation);
			}
//			}else if (data instanceof Sum && view instanceof TextView) {
//				Sum sum = (Sum)data;
//				if (sum.showProgress){
//					((TextView)view).setText(sum.minutes + " von " + sum.minutesMax + " Minuten");
//				}else{
//					((TextView)view).setText(sum.minutes + " Minuten");
//				}
//			}else if (data instanceof Sum && view instanceof ProgressBar){
//				ProgressBar prog = (ProgressBar)view;
//				Sum sum = (Sum)data;
//				prog.setVisibility(sum.showProgress ? View.VISIBLE : View.GONE );
//				prog.setMax(sum.minutesMax);
//				prog.setProgress(sum.minutes);
//			}else if (view.getId() == R.id.calltime_provider_logo){
//				view.setVisibility(View.GONE);
//			}else if (view.getId() == R.id.calltime_sum_icon){
//				if(LoadList.ROW_FREE_MINUTES.equals(textRepresentation)){
//					((ImageView)view).setImageResource(R.drawable.icon_minutepack_2);
//					view.setVisibility(View.VISIBLE);
//				}else if (LoadList.ROW_FLATRATE.equals(textRepresentation)){
//					((ImageView)view).setImageResource(R.drawable.icon_flatrate);
//					view.setVisibility(View.VISIBLE);
//				}else{
//					int image = NetworkDatabase.getLogoSmall(textRepresentation);
//					if (image != -1){
//						((ImageView)view).setImageResource(image);
//						view.setVisibility(View.VISIBLE);
//					}else {
//						if(LoadList.ROW_LANDLINE.equals(textRepresentation)){
//							((ImageView)view).setImageResource(R.drawable.logo_landline_small);
//							view.setVisibility(View.VISIBLE);
//						}else{
//							view.setVisibility(View.GONE);
//						}
//					}
//				}
//			}
			return true;
		}
	}
	
	/****************************************/
 
	/*
	 * Member variables
	 */
    private SimpleAdapter dataAdapter;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_list);
        
        ListView contactList = (ListView) findViewById(R.id.contact_list);
        
//        Cursor groupCursor = managedQuery( Contacts.CONTENT_URI,
//        		CONTACTS_PROJECTION, 
//        		Contacts.HAS_PHONE_NUMBER + "=1 AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1", 
//        		null, 
//        		Contacts.DISPLAY_NAME);
        
        
     // list
        dataArray = LoadList.loadList(this);
 		dataAdapter = new SimpleAdapter(this, 
 				dataArray, 
 				R.layout.contact_entry, 
 				new String [] {LoadList.KEY_NAME, LoadList.KEY_MINUTES}, 
 				new int[]{R.id.contact_name_text, R.id.contact_call_time,});
 		dataAdapter.setViewBinder(new MyViewBinder());
 		
 		contactList.setAdapter(dataAdapter);
 		
 		MyContentObserver contentObserver = new MyContentObserver(new Handler());

 	    this.getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
        		
//        		
//        		new ContactListAdapter(this,
//        		groupCursor, 
//        		R.layout.contact_entry, 
//        		new String[] { Contacts.DISPLAY_NAME },
//        		new int[] { R.id.contact_name_text}, 
//        		R.layout.contact_numbers,
//        		new String[] { Phone.NUMBER },
//        		new int[] { R.id.contact_number_text });

        
       
    }

    
    


    
}