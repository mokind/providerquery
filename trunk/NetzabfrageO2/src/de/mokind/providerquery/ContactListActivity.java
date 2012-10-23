package de.mokind.providerquery;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.ImageView;
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
		
		private HashMap<String, String> nameCache = new HashMap<String, String>();
		private HashMap<String, Uri> photoCache = new HashMap<String, Uri>();

		public boolean setViewValue(View view, Object data, String textRepresentation) {

			if (data instanceof String && view instanceof ImageView){
				Uri photoUri = photoCache.get(textRepresentation);
				if (photoUri == null){
					photoUri = getContactPhotoByNumber(textRepresentation);
					photoCache.put(textRepresentation, photoUri);
				}
				if (photoUri != null){
					((ImageView)view).setImageURI(photoUri);
				}else{				
					((ImageView)view).setImageResource(R.drawable.ic_menu_allfriends);
				}
			}else if (data instanceof String && view instanceof TextView){
				
				String name = nameCache.get(textRepresentation);
				if (name == null){
					name = getContactDisplayNameByNumber(textRepresentation);
					nameCache.put(textRepresentation, name);
				}
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
        
        // list
        dataArray = LoadList.getDataArray(this, rowName);
        
 		dataAdapter = new SimpleAdapter(this, 
 				dataArray, 
 				R.layout.contact_entry, 
 				new String [] {LoadList.KEY_NAME, LoadList.KEY_NAME, LoadList.KEY_MINUTES}, 
 				new int[]{R.id.contact_photo, R.id.contact_name_text, R.id.contact_call_time,});
 		dataAdapter.setViewBinder(new MyViewBinder());
 		
 		contactList.setAdapter(dataAdapter);
 		
 		MyContentObserver contentObserver = new MyContentObserver(new Handler());

 	    this.getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
       
    }
    
    private Uri getContactPhotoByNumber(String number) {
    	Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    	
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(uri, new String[] {ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_ID }, null, null, null);
        try {
            if (cur != null && cur.getCount() > 0) {
				cur.moveToFirst();
				// get photo
				int photoID = cur.getInt(cur.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_ID));
				Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoID);
				return photoUri;
            }
        } finally {
			if (cur != null) {
				cur.close();
			}
        }
        return null;
    }
    
    private String getContactDisplayNameByNumber(String number) {
    	String name = null;
    	Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_ID }, null, null, null);
        try {
            if (cur != null && cur.getCount() > 0) {
                cur.moveToFirst();
                name = cur.getString(cur.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return name;
    }
    
}