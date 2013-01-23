package de.mokind.providerquery;

import java.text.DateFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
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

public class CalltimeFragment extends Fragment {

    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int pageNumber;
    
	//Members
	private Handler handler = new Handler();
	private ListView list = null;
	private TextView billingPediodView;
	private ViewBinder binder = new MyViewBinder();

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static CalltimeFragment create(int pageNumber) {
    	CalltimeFragment fragment = new CalltimeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments().getInt(ARG_PAGE);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.calltime_list, container, false);

  		billingPediodView = (TextView)rootView.findViewById(R.id.billing_period);
  		list = (ListView)rootView.findViewById(R.id.calltime_list);
    	
  		if (getActivity() != null){
  			init();
  		}
  		
        return rootView;
    }
    
    private void init(){
    	list.setOnItemClickListener(new OnItemClickListener() {
  			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  				Toast.makeText(getActivity(), ((TextView)view.findViewById(R.id.calltime_name)).getText(), Toast.LENGTH_SHORT).show();
  				Intent intent = new Intent(getActivity(), ContactListActivity.class);
  				TextView editText = ((TextView)view.findViewById(R.id.calltime_name));
  			    String message = editText.getText().toString();
  			    intent.putExtra(ContactListActivity.EXTRA_ROWNAME, message);
  			    intent.putExtra(ContactListActivity.EXTRA_MONTHOFFSET, getMonthOffset());
  			    startActivity(intent);
  			}
          });

  		
  		// list

  		billingPediodView.setText(getBillingText());
  		
  		setAdapter(list, getActivity());
  		
  		MyContentObserver contentObserver = new MyContentObserver(handler);

  		getActivity().getContentResolver().registerContentObserver (Contacts.CONTENT_URI, true, contentObserver);
    }
    
    /**
     * Better to initiate data here, because activity is attached and so context can be given to
     */    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (list != null){
        	init();
        }
    }
    
	
    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return pageNumber;
    }

  	
  	/**
  	 * The view binder
  	 * @author monsterkind
  	 *
  	 */
  	private class MyViewBinder implements ViewBinder{
  		
  		public boolean setViewValue(View view, Object data, String textRepresentation) {
  			boolean currentPeriod = getMonthOffset() == 0;
  			if (view instanceof TextView){
  				((TextView)view).setTextColor(currentPeriod?Color.WHITE:Color.GRAY);
  			}
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

//  		@SuppressLint("ParserError")
  	    public MyContentObserver(Handler handler) {
  	    	super(handler);
  		}

  		@Override
  		public void onChange(boolean selfChange) {
  			if (getActivity() == null){
  				return;
  			}
  			setAdapter(list, getActivity());
  			billingPediodView.setText(getBillingText());
  	        super.onChange(selfChange);
  		}
  	 }

  	private String getBillingText(){
  		int monthOffset = getMonthOffset();
  		return "von " + 
  				DateFormat.getDateInstance().format(LoadList.getBillingPeriodStart(getActivity(), monthOffset).getTime()) + 
  				" bis " + (monthOffset==0?"heute":DateFormat.getDateInstance().format(LoadList.getBillingPeriodEnd(getActivity(), monthOffset).getTime()));
  	}
  	
  	private int getMonthOffset(){
  		return pageNumber - MainActivity.BILLING_PAGE_COUNT + 1;
  	}
  	
  	
  	private void setAdapter(final ListView list, final Context context){
  		LoadList.loadList(context, getMonthOffset(), new Runnable(){
  			public void run() {
  				list.post(new Runnable(){
  					public void run() {
  						final SimpleAdapter dataAdapter = new SimpleAdapter(context, 
  								LoadList.getDataArray(context, null, getMonthOffset()), 
  								R.layout.calltime_entry, 
  								new String [] {LoadList.KEY_NAME, LoadList.KEY_MINUTES, LoadList.KEY_PROGRESS, LoadList.KEY_NAME}, 
  								new int[]{R.id.calltime_name, R.id.calltime_value,R.id.calltime_progressbar,R.id.calltime_sum_icon});
  						dataAdapter.setViewBinder(binder);
  						list.setAdapter(dataAdapter);
  					}
  				});
  			}
  		});
  	}
  	
  	
  	

    
    
    
}
