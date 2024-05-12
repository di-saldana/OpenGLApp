package com.japg.mastermoviles.opengl10;

import static android.widget.Toast.LENGTH_LONG;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OpenGLActivity extends AppCompatActivity {
	private ImageButton leftBtn;
	private ImageButton rightBtn;
	private OpenGLRenderer renderer;
	private View layout;
	private GLSurfaceView glSurfaceView;
	private boolean rendererSet = false;
	private float dstX = 1f;
	private float dstY = 1f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		layout = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
		leftBtn = layout.findViewById(R.id.left_btn);
		rightBtn = layout.findViewById(R.id.right_btn);
		leftBtn.setOnClickListener(v -> glSurfaceView.queueEvent(() -> renderer.rotateHeadLeft()));
		rightBtn.setOnClickListener(v -> glSurfaceView.queueEvent(() -> renderer.rotateHeadRight()));

		glSurfaceView = new GLSurfaceView(this);
		renderer = new OpenGLRenderer(this);
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

		//final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
				|| Build.FINGERPRINT.startsWith("generic")
				|| Build.FINGERPRINT.startsWith("unknown")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator")
				|| Build.MODEL.contains("Android SDK built for x86");

		if (supportsEs2) {
			// Request OpenGL 2.0 compatible context.
			glSurfaceView.setEGLContextClientVersion(2);
			// Para que funcione en el emulador
			glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
			// Asigna nuestro renderer.
			glSurfaceView.setRenderer(renderer);
			rendererSet = true;
			Toast.makeText(this, "OpenGL ES 2.0 soportado", LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Este dispositivo no soporta OpenGL ES 2.0", LENGTH_LONG).show();
			return;
		}

		glSurfaceView.setOnTouchListener((v, event) -> {
			if (event != null) {
				final float normalizedX = (event.getX() / (float) v.getWidth()) * 2 - 1;
				final float normalizedY = -((event.getY() / (float) v.getHeight()) * 2 - 1);

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					glSurfaceView.queueEvent(() -> renderer.handleTouchPress(normalizedX, normalizedY));

					dstX = normalizedX;
					dstY = normalizedY;

				} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
					glSurfaceView.queueEvent(() -> renderer.handleTouchDrag(normalizedX, normalizedY));

					if (event.getPointerCount() == 2) {
						float secondFingerX = (event.getX(1) / (float) v.getWidth()) * 2 - 1;
						float secondFingerY = -((event.getY(1) / (float) v.getHeight()) * 2 - 1);
						float distanceNew = getDistance(normalizedX, normalizedY, secondFingerX, secondFingerY);
						float distanceOld = getDistance(dstX, dstY, secondFingerX, secondFingerY);

						if (Math.abs(distanceOld) < Math.abs(distanceNew)) {
							float zoomFactor = (distanceNew - distanceOld) * 10;
							glSurfaceView.queueEvent(() -> renderer.handleZoomOut(zoomFactor));
						}

						if (Math.abs(distanceOld) > Math.abs(distanceNew)) {
							float zoomFactor = (distanceOld - distanceNew) * 10;
							glSurfaceView.queueEvent(() -> renderer.handleZoomIn(zoomFactor));
						}
						dstX = normalizedX;
						dstY = normalizedY;
					}
				}
				return true;

			} else {
				return false;
			}
		});

		setContentView(glSurfaceView);
		FrameLayout parent = (FrameLayout) glSurfaceView.getParent();
		parent.addView(layout, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
	}

	@Override
    protected void onPause() {
    	super.onPause();
    	if (rendererSet) {
    		glSurfaceView.onPause();
    	}
    }
    		
    @Override
    protected void onResume() {
    	super.onResume();
    	if (rendererSet) {
    		glSurfaceView.onResume();
    	}
    }

	private float getDistance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	// TODO: Borrar
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
   
}
