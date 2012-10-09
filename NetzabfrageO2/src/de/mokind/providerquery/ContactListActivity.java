package de.mokind.providerquery;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import de.mokind.R;
import de.mokind.providerquery.util.LoadList;
import de.mokind.providerquery.util.Sum;

/**
 * Main Activity class shows contact list and has main menu
 * 
 * @author monsterkind
 *
 */
public class ContactListActivity extends Activity {
	
	public static final String EXTRA_ROWNAME = "ContactListActivity.RowName";
	
	private String rowName = null;
	
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
				String name = getContactDisplayNameByNumber(textRepresentation);
				if (name != null){
					((TextView)view).setText(name + "\n(" + textRepresentation + ")");
				}else{				
					((TextView)view).setText(textRepresentation);
				}

			}else if (data instanceof Sum && view instanceof TextView) {
				Sum sum = (Sum)data;
				((TextView)view).setText(sum.getMinutes() + " Minuten");
			}
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
        rowName = getIntent().getStringExtra(EXTRA_ROWNAME);
        
        ListView contactList = (ListView) findViewById(R.id.contact_list);
        ((TextView) findViewById(R.id.contact_list_header)).setText(rowName);
        
        
//        Cursor groupCursor = managedQuery( Contacts.CONTENT_URI,
//        		CONTACTS_PROJECTION, 
//        		Contacts.HAS_PHONE_NUMBER + "=1 AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1", 
//        		null, 
//        		Contacts.DISPLAY_NAME);
        
        
     // list
        dataArray = LoadList.getDataArray(this, rowName);
 		dataAdapter = new SimpleAdapter(this, 
 				dataArray, 
 				R.layout.contact_entry, 
 				new String [] {LoadList.KEY_NAME, LoadList.KEY_MINUTES}, 
 				new int[]{R.id.contact_name_text, R.id.contact_call_time,});
 		dataAdapter.setViewBinder(new MyViewBinder());
 		
 		contactList.setAdapter(dataAdapter);
 		
 		MyContentObserver contentObserver = new MyContentObserver(new Handler());

 	    this.getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
       
    }

    
    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = null;

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }


    
}