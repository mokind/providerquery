package de.mokind.paint;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;



public class MainActivity extends Activity {

	private static final int[] imageIDs = new int[]{
		R.drawable._boot_13946,
		R.drawable._baum_13980,
		R.drawable._drachen_14004,
		R.drawable._regenschirm_22783,
		R.drawable._entchen_17636,
		R.drawable._tannenbaum_18521,
		R.drawable._auto_22848,
		R.drawable._verkehrsschilder_11150,
		R.drawable._eule_17634,
		R.drawable._schwein_17789,
		R.drawable._raupe_17688,
		R.drawable._pilz_mit_punkten_18353,
		R.drawable._geschenk_12430,
		R.drawable._lebkuchenmaennchen_8667,
		R.drawable._baer_26995,
		R.drawable._kaninchen_18373,
		R.drawable._schildkroete_17882,
		R.drawable._haeuschen_23127,
		R.drawable._frosch_17552,
		R.drawable._brot_14963,
		R.drawable._blume_18532,
		R.drawable._flugzeug_10213,
		R.drawable._eisenbahn_23294,
		R.drawable._schaaf_17791,
		R.drawable._schnecke_17587,
		R.drawable._biene_17635,
		R.drawable._01a_bewoelkt_mit_sonne_11300,
		R.drawable._zauberer_karneval_26453,
		R.drawable._elefant_17581,
		R.drawable._eiscreme_23299,
		R.drawable._hund_17890,
		R.drawable._giraffe_17684,
		R.drawable._kuh_17792,
		R.drawable._huhn_18371,
		R.drawable._esel_17683,
		R.drawable._sandkasten_26751,
		R.drawable._fahrrad_23331,
		R.drawable._affe_17524,
		R.drawable._bastelecke_26740,
		R.drawable._vase_mit_blumen_18537,
		R.drawable._eichel_und_eichenblatt_18550,};
	
	private int paintIndex = (int)(Math.random() * (imageIDs.length - 1));
	private DrawView draw = null;
	
	private static final int TIMER_SECONDS_MAX = 900; // 900 Sekunden = 15 Minuten Spiel
	private int timerSeconds = 0;
	private boolean timerPaused = false;
	private Timer timer = null;
	
	// TODO: will man das wirklich: JA!
	private SparseArray<Bitmap> paintJobs = new SparseArray<Bitmap>(imageIDs.length);
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activiy_layout);

        // get view instance
        draw = (DrawView) findViewById(R.id.widget_view);
        draw.setVisibility(View.VISIBLE);
        final View scroller = findViewById(R.id.ColorScroller);
        
        final int backgroundAlpha = 224;

        draw.setPaintColorARGB(255, 235, 0, 0); // set red
		scroller.setBackgroundColor(Color.argb(backgroundAlpha, 235, 0, 0));
        
        // set color button listener
        int[] imageButtonIDs = new int[]{R.id.Color1, R.id.Color2, R.id.Color3, R.id.Color4, R.id.Color5, R.id.Color6, R.id.Color7, R.id.Color8, R.id.Color9, R.id.Color10, R.id.Color11};
        for (int imageButtonID: imageButtonIDs){
        	ImageButton button = (ImageButton) findViewById(imageButtonID);
        	button.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					switch (v.getId()){
					case R.id.Color7:
					default:
						draw.setPaintColorARGB(255, 255, 216, 0); // set yellow
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 255, 216, 0));
						break;
					case R.id.Color5:
						draw.setPaintColorARGB(255, 38, 201, 0); // set green
						scroller.setBackgroundColor(Color.argb(backgroundAlpha,  38, 201, 0));
						break;
					case R.id.Color4:
						draw.setPaintColorARGB(255, 56, 84, 253); // set blue
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 0, 19, 235));
						break;
					case R.id.Color3:
						draw.setPaintColorARGB(255, 214, 0, 255); // set violett
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 178, 0, 255));
						break;
					case R.id.Color2:
						draw.setPaintColorARGB(255, 0, 235, 235); // set cyan
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 0, 235, 235));
						break;
					case R.id.Color6:
						draw.setPaintColorARGB(255, 235, 0, 0); // set red
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 235, 0, 0));
						break;
					case R.id.Color10:
						draw.setPaintColorARGB(255, 128, 128, 128); // set grey
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 128, 128, 128));
						break;
					case R.id.Color8:
						draw.setPaintColorARGB(255, 255, 146, 73); // set orange
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 255, 146, 73));
						break;
					case R.id.Color1:
						draw.setPaintColorARGB(255, 255, 100, 200); // set pink
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 255, 100, 200));
						break;
					case R.id.Color9:
						draw.setPaintColorARGB(255, 119, 65, 28); // set brown
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 152, 83, 33));
						break;
					case R.id.Color11:
						draw.setPaintColorARGB(255, 255, 255, 255); // set white
						scroller.setBackgroundColor(Color.argb(backgroundAlpha, 255, 255, 255));
						break;
					}					
				}
			});
        	
        	initTimer();
        }
        
        // set switch button listener 
        ImageButton buttonNext = (ImageButton) findViewById(R.id.next);
        buttonNext.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateDrawable(paintIndex + 1);
			}
			
        });

        ImageButton buttonPrev = (ImageButton) findViewById(R.id.previous);
        buttonPrev.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateDrawable(paintIndex - 1);
			}
        });
        
        ImageButton buttonClear = (ImageButton) findViewById(R.id.clear);
        buttonClear.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				draw.clearBackround();
				return true;
			}
		});
        
        // set BLUME :))
        updateDrawable(20);
    }
	
	private void updateDrawable(int newPaintIndex){
		// correct index
		if (newPaintIndex >= imageIDs.length){
			newPaintIndex = 0;
		}
		if (newPaintIndex < 0){
			newPaintIndex = imageIDs.length - 1;
		}
		
		// save bitmap
		Bitmap bitmap = draw.getDrawBitmap();
		paintJobs.put(paintIndex, bitmap);
		// set new index
		paintIndex = newPaintIndex;
		// set old bitmap
		draw.setDrawBitmap(paintJobs.get(paintIndex));
		if (draw != null){
			draw.setImageDrawable(getResources().getDrawable(imageIDs[paintIndex]));
		}
	}

	private void initTimer() {
	
		timerPaused = false;
		if (timer != null){
        	timer.cancel();
        }
		timerSeconds = 2;
		
		final ProgressBar progressBarView = (ProgressBar) findViewById(R.id.progress);
		progressBarView.setMax(TIMER_SECONDS_MAX);
		progressBarView.setProgress(timerSeconds);
		progressBarView.setIndeterminate(true);
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!timerPaused){
					if (timerSeconds < TIMER_SECONDS_MAX){
						timerSeconds++;
						progressBarView.setIndeterminate(false);
						progressBarView.setProgress(timerSeconds);
					}else{
						MainActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressBarView.setIndeterminate(true);
								// start alarm tone
								final Ringtone alarmTone = RingtoneManager.getRingtone(getApplicationContext(),  RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
								alarmTone.play();
								timer.schedule(new TimerTask() {
									// Close App
									@Override
									public void run() {
										MainActivity.this.runOnUiThread(new Runnable(){
											public void run(){
												alarmTone.stop();
												Ringtone notificationTone = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
												notificationTone.play();
												draw.setVisibility(View.INVISIBLE);
												MainActivity.this.finish();
											}
										});
									}
								}, 5000);
							}
						});
						cancel();
					}
					
				}
			}
		}, timerSeconds * 1000, 1000);
	}
		

	@Override
	protected void onPause() {
		super.onPause();
		timerPaused = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		timerPaused = false;
	}

}
