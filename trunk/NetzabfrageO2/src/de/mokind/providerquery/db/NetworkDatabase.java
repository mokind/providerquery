package de.mokind.providerquery.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import de.mokind.providerquery.R;

/**
 * Access to network database
 * @author monsterkind
 *
 */
public class NetworkDatabase  {
	
	private static HashMap<String, Integer> colorMap = null;
	private static HashMap<String, Integer> imageMap = null;
	private static HashMap<String, Integer> imageSmallMap = null;
	
	public static final int STATUS_ERROR = -1;	// an error occurred (should not be written to db)
	public static final int STATUS_OK = 0;	// provider determined
	public static final int STATUS_NO_PROVIDER = 1;	// no provider determination possible
	public static final int STATUS_QUEUED = 2;			// currently queued for sending request
	public static final int STATUS_SEND = 3;			// request send, awaiting answer
	
	private static final int DATABASE_VERSION = 2;
	private static final String PROVIDER_DB_NAME = "provider_db";
    private static final String PROVIDER_TABLE_NAME = "provider_table";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_NETWORK = "network";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String PROVIDER_TABLE_CREATE =
                "CREATE TABLE IF NOT EXISTS " + PROVIDER_TABLE_NAME + " (" +
                		" _id integer primary key autoincrement, " + 
                		KEY_NUMBER + " text unique , " +
                		KEY_NETWORK + " text, " +
                		KEY_STATUS + " integer, " +
                		KEY_LAST_UPDATE + " timestamp);";
    
    private class MyOpenHelper extends SQLiteOpenHelper{
    	
    	private MyOpenHelper(Context context) {
			super(context, PROVIDER_DB_NAME, null, DATABASE_VERSION);
		}

		@Override
    	public void onCreate(SQLiteDatabase db) {
    		db.execSQL(PROVIDER_TABLE_CREATE);
    	}

    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		// TODO Auto-generated method stub

    	}
    }
    
    private MyOpenHelper mHelper = null;

    public NetworkDatabase(Context context) {
    	mHelper = new MyOpenHelper(context);
	}
	
    /**
     * Gets color according to the given provider string
     * @param provider
     * @return Color.BLACK is returned when provider is unknown
     */
    public static int getColor(String provider){
    	if (colorMap == null){
    		colorMap = new HashMap<String, Integer>(4);
    		colorMap.put("o2", Color.parseColor("#0E1F7E"));
    		colorMap.put("T-Mobile", Color.MAGENTA);
    		colorMap.put("Vodafone", Color.parseColor("#F50000"));
    		colorMap.put("E-Plus", Color.parseColor("#005A3A"));
    	}
    	Integer color = colorMap.get(provider);
    	if (color == null){
    		return Color.BLACK;
    	}else{
    		return color.intValue();
    	}
    }
    
    /**
     * Gets logo image according to the given provider string
     * @param provider
     * @return R.drawable.logo_landline is returned when provider is unknown
     */
    public static int getLogo(String provider){
    	if (imageMap == null){
    		imageMap = new HashMap<String, Integer>(4);
    		imageMap.put("o2", R.drawable.logo_o2);
    		imageMap.put("T-Mobile", R.drawable.logo_tmobile);
    		imageMap.put("Vodafone", R.drawable.logo_vodafone);
    		imageMap.put("E-Plus", R.drawable.logo_eplus);
    	}
    	Integer color = imageMap.get(provider);
    	if (color == null){
    		return R.drawable.logo_landline;
    	}else{
    		return color.intValue();
    	}
    }
    
    /**
     * Gets small logo image according to the given provider string
     * @param provider
     * @return -1 is returned when provider is unknown
     */
    public static int getLogoSmall(String provider){
    	if (imageSmallMap == null){
    		imageSmallMap = new HashMap<String, Integer>(4);
    		imageSmallMap.put("o2", R.drawable.logo_o2_small);
    		imageSmallMap.put("T-Mobile", R.drawable.logo_tmobile_small);
    		imageSmallMap.put("Vodafone", R.drawable.logo_vodafone_small);
    		imageSmallMap.put("E-Plus", R.drawable.logo_eplus_small);
    	}
    	Integer color = imageSmallMap.get(provider);
    	if (color == null){
    		return -1;
    	}else{
    		return color.intValue();
    	}
    }
    
	/**
	 * direct access to database records
	 * @param number telephone number in String format
	 * @return
	 */
	public String getNetwork(String number){
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cur = db.query(PROVIDER_TABLE_NAME, new String[]{KEY_NETWORK}, KEY_NUMBER + " = ? ", new String[]{number}, null, null, null);
		if (cur.getCount() < 1){
			cur.close();
			db.close();
			return null;
		}else{
			cur.moveToFirst();
			String ret = cur.getString(0);
			cur.close();
			db.close();
			return ret;
		}
	}
	
	/**
	 * Gives the date of the last access of the given numbers record
	 * @param number telephone number in String format
	 * @return time in millis of the last access is only valid when record exists, otherwise null will be returned
	 */
	public long getLastAccess(String number){
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cur = db.query(PROVIDER_TABLE_NAME, new String[]{KEY_LAST_UPDATE}, KEY_NUMBER + " = ? ", new String[]{number}, null, null, null);
		if (cur.getCount() < 1){
			cur.close();
			db.close();
			return 0;
		}else{
			cur.moveToFirst();
			long ret = cur.getLong(0);
			cur.close();
			db.close();
			return ret;
		}
	}
	
	/**
	 * direct access to database records
	 * @param number
	 * @param network
	 */
	public void putNetwork(String number, String network){
		SQLiteDatabase db = mHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_NUMBER, number);
		values.put(KEY_NETWORK, network);
		values.put(KEY_STATUS, STATUS_OK);
		values.put(KEY_LAST_UPDATE, (new Date()).getTime());
		
		// check, if number already in database
		if (db.replace(PROVIDER_TABLE_NAME, null, values) == -1){
			db.insert(PROVIDER_TABLE_NAME, null, values);
		}
		db.close();
		
	}
	
	/**
	 * direct access to database records
	 * @param number
	 * @return
	 */
	public int getStatus(String number){
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cur = db.query(PROVIDER_TABLE_NAME, new String[]{KEY_STATUS}, KEY_NUMBER + " = ? ", new String[]{number}, null, null, null);
		if (cur.getCount() < 1){
			cur.close();
			db.close();
			return STATUS_ERROR;
		}else{
			cur.moveToFirst();
			int ret = cur.getInt(0);
			cur.close();
			db.close();
			return ret;
		}
	}
		
	/**
	 * direct access to database records
	 * @param number
	 * @param status
	 */
	public void putStatus(String number, int status){
		SQLiteDatabase db = mHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_NUMBER, number);
		values.put(KEY_STATUS, status);
		values.put(KEY_LAST_UPDATE, (new Date()).getTime());
		
		// check, if number already in database
		if (db.replace(PROVIDER_TABLE_NAME, null, values) == -1){
			db.insert(PROVIDER_TABLE_NAME, null, values);
		}
		db.close();
	}

	/**
	 * Deletion of all data
	 */
	public void deleteData(){
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.delete(PROVIDER_TABLE_NAME, null, null);
		db.close();
	}
	
	/**
	 * Gets distinct list of the known providers
	 * @return
	 */
	public String[] getKnownProviders(){
		String sql = SQLiteQueryBuilder.buildQueryString(true, PROVIDER_TABLE_NAME, new String[]{KEY_NETWORK}, null, null, null, null, null);
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cur = db.rawQuery(sql, null);
		
		if (cur.getCount() == 0){
			cur.close();
			db.close();
			return new String[0];
		}
		
		ArrayList<String> providers = new ArrayList<String>(cur.getCount());
		
		do{
			cur.moveToNext();
			if (cur.getString(0) != null){
				providers.add(cur.getString(0));
			}
		}while(!cur.isLast());
		
		cur.close();
		db.close();
		
		return providers.toArray(new String[providers.size()]);
	}
	
	/**
	 * Returns all numbers 
	 * @param status
	 * @return
	 */
	public String[] getAllNumbers(int status){
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cur = db.query(PROVIDER_TABLE_NAME, new String[]{KEY_NUMBER}, KEY_STATUS + " = ? ", new String[]{status + ""}, null, null, null);
		
		if (cur.getCount() == 0){
			cur.close();
			db.close();
			return new String[0];
		}
		
		ArrayList<String> numbers = new ArrayList<String>(cur.getCount());
		
		do{
			cur.moveToNext();
			if (cur.getString(0) != null){
				numbers.add(cur.getString(0));
			}
		}while(!cur.isLast());
		
		cur.close();
		db.close();
		
		return numbers.toArray(new String[numbers.size()]);
	}
}
