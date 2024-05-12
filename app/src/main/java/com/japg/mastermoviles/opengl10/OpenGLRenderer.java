package com.japg.mastermoviles.opengl10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.japg.mastermoviles.opengl10.util.LoggerConfig;
import com.japg.mastermoviles.opengl10.util.Resource3DSReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.frustumM;

public class OpenGLRenderer implements Renderer {
	private static final String TAG = "OpenGLRenderer";
	
	// Para paralela
	//private static final float TAM = 1.0f;
	// Para perspectiva
	//private static final float TAM = 1.0f;
	
	private static final int BYTES_PER_FLOAT = 4;
	
	private final Context context;
	private int program;
	
	// Nombre de los uniform
	private static final String U_MVPMATRIX 		= "u_MVPMatrix";
	private static final String U_MVMATRIX 			= "u_MVMatrix";
	private static final String U_COLOR 			= "u_Color";
	private static final String U_TEXTURE 			= "u_TextureUnit";

	// Nombre de los attribute
	private static final String A_POSITION = "a_Position";
	private static final String A_NORMAL   = "a_Normal";
	private static final String A_UV       = "a_UV";

	// Handles para los shaders
	private int uMVPMatrixLocation;
	private int uMVMatrixLocation;
	private int uColorLocation;
	private int uTextureUnitLocation;
	private int aPositionLocation;
	private int aNormalLocation;
	private int aUVLocation;
	
	private int	texture;
	
	// Rotación alrededor de los ejes
	private float rX = 0f;
	private float rY = 0f;
	
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int NORMAL_COMPONENT_COUNT = 3;
	private static final int UV_COMPONENT_COUNT = 2;
	// C?lculo del tama?o de los datos (3+3+2 = 8 floats)
	private static final int STRIDE =
			(POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT;
		
	// Matrices de proyección y de vista
	private final float[] projectionMatrix = new float[16];
	private final float[] modelMatrix = new float[16];
	private final float[] MVP = new float[16];

	Resource3DSReader obj3DS;

	// ADDED
	private final Model3D halo;
	private final Model3D body;
	
	float[] tablaVertices = {
		// Abanico de triángulos, x, y, R, G, B
		 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
		-0.5f,-0.8f, 0.7f, 0.7f, 0.7f,
		 0.5f,-0.8f, 0.7f, 0.7f, 0.7f,
		 0.5f, 0.8f, 1.0f, 1.0f, 1.0f,
		-0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
		-0.5f,-0.8f, 0.7f, 0.7f, 0.7f,
		
		// L?nea 1, x, y, R, G, B
		-0.5f, 0f, 1.0f, 0.0f, 0.0f,
		 0.5f, 0f, 1.0f, 0.0f, 0.0f
	};

	void frustum(float[] m, int offset, float l, float r, float b, float t, float n, float f)
	{
		frustumM(m, offset, l, r, b, t, n, f);
		// Corrección del bug de Android
		m[8] /= 2;
	}
	
    void perspective(float[] m, int offset, float fovy, float aspect, float n, float f)
    {	final float d = f-n;
    	final float angleInRadians = (float) (fovy * Math.PI / 180.0);
    	final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));
        
    	m[0] = a/aspect;
        m[1] = 0f;
        m[2] = 0f;
        m[3] = 0f;

        m[4] = 0f;
        m[5] = a;
        m[6] = 0f;
        m[7] = 0f;

        m[8] = 0;
        m[9] = 0;
        m[10] = (n - f) / d;
        m[11] = -1f;

        m[12] = 0f;
        m[13] = 0f;
        m[14] = -2*f*n/d;
        m[15] = 0f;

    }
	
	void perspective2(float[] m, int offset, float fovy, float aspect, float n, float f)
	{	float fH, fW;
		
		fH = (float) Math.tan( fovy / 360 * Math.PI ) * n;
		fW = fH * aspect;
		frustum(m, offset, -fW, fW, -fH, fH, n, f);
		
	}

	void frustum2(float[] m, int offset, float l, float r, float b, float t, float n, float f)
	{
		float d1 = r-l;
		float d2 = t-b;
		float d3 = f-n;

		m[0] = 2*n/d1;
		m[1] = 0f;
		m[2] = 0f;
		m[3] = 0f;
		
		m[4] = 0f;
		m[5] = 2*n/d2;
		m[6] = 0f;
		m[7] = 0f;
		
		m[8] = (r+l)/d1;
		m[9] = (t+b)/d2;
		m[10] = (n-f)/d3;
		m[11] = -1f;
		
		m[12] = 0f;
		m[13] = 0f;
		m[14] = -2*f*n/d3;
		m[15] = 0f;
	}
	
	public OpenGLRenderer(Context context) {
		this.context = context;
		
		// Lee un archivo 3DS desde un recurso
		halo = new Model3D(context, R.raw.angel_halo, R.drawable.halo_texture, 0, 0, -5);
		body = new Model3D(context, R.raw.body, R.drawable.body_texture, 0, 0, -5);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		halo.loadTexture();
		body.loadTexture();
	}
	
	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Establecer el viewport de  OpenGL para ocupar toda la superficie.
		glViewport(0, 0, width, height);
		final float aspectRatio = width > height ?
				(float) width / (float) height :
				(float) height / (float) width;
		if (width > height) {
				// Landscape
				//orthoM(projectionMatrix, 0, -aspectRatio*TAM, aspectRatio*TAM, -TAM, TAM, -100.0f, 100.0f);
				perspective(projectionMatrix, 0, 45f, aspectRatio, 0.01f, 1000f);
				//frustum(projectionMatrix, 0, -aspectRatio*TAM, aspectRatio*TAM, -TAM, TAM, 1f, 1000.0f);
		} else {
				// Portrait or square
				//orthoM(projectionMatrix, 0, -TAM, TAM, -aspectRatio*TAM, aspectRatio*TAM, -100.0f, 100.0f);
				perspective(projectionMatrix, 0, 45f, 1f/aspectRatio, 0.01f, 1000f);
				//frustum(projectionMatrix, 0, -TAM, TAM, -aspectRatio*TAM, aspectRatio*TAM, 1f, 1000.0f);
		}
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		// Clear the rendering surface.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glLineWidth(2.0f);

		// Pasamos la textura
		glActiveTexture(GL_TEXTURE0);

		// Dibujamos el objeto
		halo.drawModel(projectionMatrix);
		body.drawModel(projectionMatrix);

		halo.updatePosition(0.06f);
		body.updatePosition(0.06f);
	}

	public void handleTouchPress(float normalizedX, float normalizedY) {
		if (LoggerConfig.ON) {
			Log.w(TAG, "Touch Press ["+normalizedX+", "+normalizedY+"]");
		}
		halo.setDestination(-normalizedY * 20f, normalizedX * 20f);
	}

	public void handleTouchDrag(float normalizedX, float normalizedY) {
		if (LoggerConfig.ON) {
			Log.w(TAG, "Touch Drag ["+normalizedX+", "+normalizedY+"]");
		}
		halo.setDestination(-normalizedY * 180f, normalizedX * 180f);
		body.setDestination(-normalizedY * 180f, normalizedX * 180f);
	}

	public void handleZoomIn(float normalizedZ) {
		halo.zoom(-normalizedZ);
		body.zoom(-normalizedZ);
	}

	public void handleZoomOut(float normalizedZ){
		halo.zoom(normalizedZ);
		body.zoom(normalizedZ);
	}

	public void rotateHeadLeft(){
		halo.rotateY(-20);
	}

	public void rotateHeadRight(){
		halo.rotateY(20);
	}
}