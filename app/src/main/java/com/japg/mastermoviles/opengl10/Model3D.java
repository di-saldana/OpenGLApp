package com.japg.mastermoviles.opengl10;

import static android.opengl.GLES10.glBindTexture;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glDrawArrays;
import static android.opengl.GLES10.glGetIntegerv;
import static android.opengl.GLES20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;
import static javax.microedition.khronos.opengles.GL10.GL_TEXTURE_2D;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLES;

import android.content.Context;

import com.japg.mastermoviles.opengl10.util.LoggerConfig;
import com.japg.mastermoviles.opengl10.util.Resource3DSReader;
import com.japg.mastermoviles.opengl10.util.ShaderHelper;
import com.japg.mastermoviles.opengl10.util.TextResourceReader;
import com.japg.mastermoviles.opengl10.util.TextureHelper;

import java.nio.Buffer;

public class Model3D {
    private final Context context;
    private int uMVPMatrixLocation;
    private int uMVMatrixLocation;
    private int uColorLocation;
    private int uTextureUnitLocation;
    private int aPositionLocation;
    private int aNormalLocation;
    private int aUVLocation;
    private final float[] modelViewProjectionMatrix = new float[16];
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int NORMAL_COMPONENT_COUNT = 3;
    private static final int UV_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private final Resource3DSReader modelData;
    private int textureId;
    private final int textureResource;
    private float rotationX;
    private float rotationY;
    private float rotationZ;
    private float destinationX;
    private float destinationY;
    private final float[] modelMatrix = new float[16];

    public Model3D(Context context, int modelResource, int textureResource, float initialRotationX, float initialRotationY, float initialRotationZ){
        this.context = context;
        this.rotationX = initialRotationX;
        this.rotationY = initialRotationY;
        this.rotationZ = initialRotationZ;
        this.textureResource = textureResource;
        this.modelData = new Resource3DSReader();
        this.modelData.read3DSFromResource(context, modelResource);
    }

    public void setDestination(float destinationX, float destinationY){
        this.destinationX = destinationX;
        this.destinationY = destinationY;
    }

    public void updatePosition(float speed){
        float differenceX = this.destinationX - this.rotationX;
        float differenceY = this.destinationY - this.rotationY;

        this.rotationX += differenceX * speed;
        this.rotationY += differenceY * speed;
    }

    public void rotateY(float rotationY){
        this.destinationY += rotationY;
    }

    public void zoom(float rotationZ){
        this.rotationZ += rotationZ;
    }

    public void loadTexture(){
        String vertexShaderSource;
        String fragmentShaderSource;

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        int[]	maxVertexTextureImageUnits = new int[1];
        int[]	maxTextureImageUnits       = new int[1];

        glGetIntegerv(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, maxVertexTextureImageUnits, 0);
        glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureImageUnits, 0);

        if (maxVertexTextureImageUnits[0]>0) {
            vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader);
            fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader);
        } else {
            vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader2);
            fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader2);
        }

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        int program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);

        uMVPMatrixLocation = glGetUniformLocation(program, "u_MVPMatrix");
        uMVMatrixLocation = glGetUniformLocation(program, "u_MVMatrix");
        uColorLocation = glGetUniformLocation(program, "u_Color");
        uTextureUnitLocation = glGetUniformLocation(program, "u_TextureUnit");
        aPositionLocation = glGetAttribLocation(program, "a_Position");
        glEnableVertexAttribArray(aPositionLocation);
        aNormalLocation = glGetAttribLocation(program, "a_Normal");
        glEnableVertexAttribArray(aNormalLocation);
        aUVLocation = glGetAttribLocation(program, "a_UV");
        glEnableVertexAttribArray(aUVLocation);
        this.textureId = TextureHelper.loadTexture(context, this.textureResource);
    }

    public void drawModel(float[] projectionMatrix) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, 0f, 0.0f, rotationZ);
        rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelMatrix, 0);
        glUniformMatrix4fv(uMVPMatrixLocation, 1, false, modelViewProjectionMatrix, 0);
        glUniformMatrix4fv(uMVMatrixLocation, 1, false, modelMatrix, 0);
        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1f(this.uTextureUnitLocation, 0);

        for (int i=0; i<this.modelData.numMeshes; i++) {
            final Buffer pos = this.modelData.dataBuffer[i].position(0);
            glVertexAttribPointer(this.aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, modelData.dataBuffer[i]);

            this.modelData.dataBuffer[i].position(POSITION_COMPONENT_COUNT);
            glVertexAttribPointer(aNormalLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, modelData.dataBuffer[i]);

            this.modelData.dataBuffer[i].position(POSITION_COMPONENT_COUNT+ NORMAL_COMPONENT_COUNT);
            glVertexAttribPointer(aUVLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, modelData.dataBuffer[i]);
            glDrawArrays(GL_TRIANGLES, 0, this.modelData.numVertices[i]);
        }
    }
}
