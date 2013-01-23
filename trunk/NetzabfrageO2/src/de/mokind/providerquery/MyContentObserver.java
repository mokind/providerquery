package de.mokind.providerquery;

import android.database.ContentObserver;
import android.os.Handler;
import de.mokind.providerquery.util.LoadList;

/**
	 * My {@link ContentObserver}
	 * @author monsterkind
	 * @deprecated
	 *
	 */
	class MyContentObserver extends ContentObserver {

/**
		 * 
		 */
		private final ContactListActivity contactListActivity;

		//		@SuppressLint("ParserError")
	    public MyContentObserver(ContactListActivity contactListActivity, Handler handler) {
	    	super(handler);
			this.contactListActivity = contactListActivity;
		}

		@Override
		public void onChange(final boolean selfChange) {
			LoadList.loadList(contactListActivity, 0, new Runnable() {
				
				public void run() {
					contactListActivity.runOnUiThread(new Runnable(){
						public void run() {
							contactListActivity.setDataArray(LoadList.getDataArray(contactListActivity, contactListActivity.getRowName(), 0));
							contactListActivity.getDataAdapter().notifyDataSetChanged();
							MyContentObserver.super.onChange(selfChange);				
						}
					});
					
				}
			});
			
			
			
		}
	 }