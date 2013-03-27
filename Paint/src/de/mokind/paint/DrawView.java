package de.mokind.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class DrawView extends ImageView {

	private float x = -1;
	private float y= -1;
	private float size= -1;
	
	private Bitmap drawBitmap = null;
	
	private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	public DrawView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DrawView(Context context) {
		super(context);
		init();
	}

    @Override
    public void onDraw(Canvas canvas){
        if (drawBitmap == null){
        	drawBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        	Canvas myCanvas = new Canvas(drawBitmap);
        	myCanvas.drawARGB(255, 255, 255, 255);
        }else{
        	// TODO: draw bitmap here
        	canvas.drawBitmap(drawBitmap, 0, 0, p);
        }
        super.onDraw(canvas);
    }
    
    public void setPaintColorARGB(int alpha, int red, int green, int blue){
    	p.setARGB(alpha, red, green, blue);
    }
    
    public void clearBackround(){
    	if (drawBitmap != null){
    		drawBitmap = Bitmap.createBitmap(drawBitmap.getWidth(), drawBitmap.getHeight(), Bitmap.Config.RGB_565);
        	Canvas myCanvas = new Canvas(drawBitmap);
        	myCanvas.drawARGB(255, 255, 255, 255);
        	
        	invalidate();
    	}    	
    }
    
    private void init(){
    	
    	// set paint
    	p.setARGB(255, 235, 0, 0);
    	p.setStrokeJoin(Join.ROUND);
    	p.setStrokeCap(Cap.ROUND);
    	
    	// set touch listener
    	setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				if (event.getAction() == MotionEvent.ACTION_DOWN || 
					event.getAction() == MotionEvent.ACTION_UP ||
					event.getAction() == MotionEvent.ACTION_MOVE)
				{
					
                    float newsize = ((event.getSize() * 15) * (event.getSize() * 15)) + 12;
                    float newx = event.getX();
                    float newy = event.getY();
                    
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                    	size = newsize;
                        x = newx;
                        y = newy;
                    }
                    
                    if (drawBitmap != null && x > -1){
                    	Canvas myCanvas = new Canvas(drawBitmap);
                    	p.setStrokeWidth(size);
                    	p.setStrokeMiter(size);
                    	myCanvas.drawLine(x, y, newx, newy, p);
                    }
                    
                    size = newsize;
                    x = newx;
                    y = newy;
                    
                }
				invalidate();	
				return true;
			}
		});
    }


}
