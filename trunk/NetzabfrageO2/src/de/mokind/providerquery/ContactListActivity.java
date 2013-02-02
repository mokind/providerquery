package de.mokind.providerquery;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import de.mokind.R;
import de.mokind.providerquery.util.LoadList;
import de.mokind.providerquery.util.PrefUtils;
import de.mokind.providerquery.util.Sum;

/**
 * Main Activity class shows contact list and has main menu
 * 
 * @author monsterkind
 *
 */
public class ContactListActivity extends Activity {
	
	public static final String EXTRA_ROWNAME = "ContactListActivity.RowName";
	public static final String EXTRA_MONTHOFFSET = "ContactListActivity.MonthOffset";
	
	private String rowName = null;
	private int monthOffset;
	
	public String getRowName() {
		return rowName;
	}

	ArrayList< HashMap<String, Object> > dataArray = new ArrayList< HashMap<String, Object> >();
	
	/*********** Types **********************/
	
	public synchronized ArrayList<HashMap<String, Object>> getDataArray() {
		return dataArray;
	}

	public synchronized void setDataArray(ArrayList<HashMap<String, Object>> dataArray) {
		if (dataArray == null){
			this.dataArray = new ArrayList<HashMap<String,Object>>(0);
		}else{
			this.dataArray = dataArray;
		}
	}
	
	/*
	 * Member variables
	 */
    private SimpleAdapter dataAdapter;

	public SimpleAdapter getDataAdapter() {
		return dataAdapter;
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
					name = name.replaceAll("\n", "");
					((TextView)view).setText(name + "\n(" + textRepresentation + ")");
				}else{				
					((TextView)view).setText(textRepresentation);
				}
			}else if (data instanceof Sum) {
				Sum sum = (Sum)data;
				if (view.getId() == R.id.contact_call_time){
					((TextView)view).setText(sum.getMinutes() + " Minuten");
				}else if (view.getId() == R.id.contact_SMS){
					((TextView)view).setText(sum.getSmsCount() + " SMS");
				}
				
			}
			return true;
		}
	}
	
	/****************************************/
 
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_list);
        rowName = getIntent().getStringExtra(EXTRA_ROWNAME);
        monthOffset = getIntent().getIntExtra(EXTRA_MONTHOFFSET, 0);
        ListView contactList = (ListView) findViewById(R.id.contact_list);
        ((TextView) findViewById(R.id.contact_list_header)).setText(rowName);
        
        // list
        Log.d(PrefUtils.LOG_TAG, "getDataArray("+ rowName+") [START]");
        setDataArray(LoadList.getDataArray(this, rowName, monthOffset));
        Log.d(PrefUtils.LOG_TAG, "getDataArray("+ rowName+") [DONE]");
        
        Log.d(PrefUtils.LOG_TAG, "prepare adapter and binder [START]");
 		dataAdapter = new SimpleAdapter(this, 
 				getDataArray(), 
 				R.layout.contact_entry, 
 				new String [] {LoadList.KEY_NAME, LoadList.KEY_NAME, LoadList.KEY_MINUTES, LoadList.KEY_SMS}, 
 				new int[]{R.id.contact_photo, R.id.contact_name_text, R.id.contact_call_time, R.id.contact_SMS});
 		dataAdapter.setViewBinder(new MyViewBinder());
 		
 		contactList.setAdapter(dataAdapter);
 		Log.d(PrefUtils.LOG_TAG, "adapter and binder set [DONE]");
// 		MyContentObserver contentObserver = new MyContentObserver(this, new Handler());

// 	    this.getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
 		
 		contactList.setOnItemClickListener(new OnItemClickListener() {
  			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  				
  				@SuppressWarnings("unchecked")
				HashMap<String, Object> item = (HashMap<String, Object>)dataAdapter.getItem(position);
  				
  				String number = (String) item.get(LoadList.KEY_NAME);
  				
  				Uri contactUri = Uri.parse(String.format("tel: %s", number));
  				Intent addContactIntent = new Intent(
  				                ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, contactUri); 
  				startActivity(addContactIntent);
  			}
          });
       
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
				if (photoID == 0){
					return null;
				}
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