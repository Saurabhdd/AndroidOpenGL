package com.astromedicomp.Android18;

import android.content.Context;

import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.OnDoubleTapListener;

import android.opengl.GLSurfaceView;
import android.opengl.GLES32;
import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig; //egl = embedded egl (cause opengl-es calles internally to embedded opengl)

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GLESView extends GLSurfaceView implements OnGestureListener, OnDoubleTapListener, GLSurfaceView.Renderer{
	
	private final Context context;
	private GestureDetector gestureDetector;
	private int vertexShaderObject;
	private int fragmentShaderObject;
	private int shaderProgramObject;
	private int[] vao_sphere = new int[1];
    private int[] vbo_sphere_position = new int[1];
    private int[] vbo_sphere_normal = new int[1];
    private int[] vbo_sphere_element = new int[1];
	
	
	private int modelMatrixUniform;
	private int viewMatrixUniform;
	private int projectionMatrixUniform;
	private int laUniform;
	private int ldUniform;
	private int lsUniform;
	private int kaUniform;
	private int kdUniform;
	private int ksUniform;
	private int shininessUniform;
	private int lKeyPressedUniform;
	private int lightDirectionUniform;
	
	private float shininess = 128.0f;
	
	float materialAmbient[] = new float[] { 0.0f, 0.0f, 0.0f };
    float materialDiffuse[] = new float[] { 1.0f, 1.0f, 1.0f };
    float materialSpecular[] = new float[] { 1.0f, 1.0f, 1.0f };
	
	float lightDirection[] = new float[] { 100.0f, 100.0f, 100.0f, 1.0f };;
	float ambientLight[] = new float[] { 0.0f, 0.0f, 0.0f };
	float diffuseLight[] = new float[] { 1.0f, 1.0f, 1.0f };;
	float specularLight[] = new float[] { 0.7f, 0.7f, 0.7f };
	
	private static boolean gbLighting = false;
	private static boolean gbAnimate = false;
	private float yRotate = 0.0f;
	private float perspectiveProjectionMatrix[] = new float[16];
	
	private int numVertices;
	private int numElements;
	
	
	
	public GLESView(Context drawingContext)
	{
		super(drawingContext);
		
		context = drawingContext;
		this.setEGLContextClientVersion(3); //opengl ndk is server and opengl sdk is client so we are setting client version to 3	
		this.setRenderer(this);
		
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		gestureDetector = new GestureDetector(context, this, null, false);
		gestureDetector.setOnDoubleTapListener(this);
	}
	
	//override method of GLSurfaceView.Renderer 
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		//OpenGL-ES version check
		String glesVersion = gl.glGetString(GL10.GL_VERSION);
		//String glslVersion = gl.glGetString(GLES32.GL_SHADING_LANGUAGE);
		System.out.println("SSD: " + glesVersion);
		//System.out.println("SSD: " + glslVersion);
		initialize(gl);
	}
	
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		resize(width, height);
	}
	
	@Override
	public void onDrawFrame(GL10 unused)
	{
		display();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		//code
		int eventAction = event.getAction();
		if(!gestureDetector.onTouchEvent(event))
			super.onTouchEvent(event);
		
		return true;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		//code
		if(gbLighting == false)
			gbLighting = true;
		else
			gbLighting = false;
		return true;
	}
	
	@Override
	public boolean onDoubleTapEvent(MotionEvent e)
	{
		return true;
	}
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e)
	{
		if(gbAnimate == true)
			gbAnimate = false;
		else
			gbAnimate = true;
		return true;
	}
	
	@Override
	public boolean onDown(MotionEvent e)
	{
		return true;
	}
	
	@Override 
	public boolean onFling(MotionEvent e1, MotionEvent e2, float veclocityX, float velocityY)
	{
		return true;
	}
	
	@Override
	public void onLongPress(MotionEvent e)
	{
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		uninitialize();
		System.exit(0);
		return true;
	}
	
	@Override
	public void onShowPress(MotionEvent e)
	{
		
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		return true;
	}
	
	private void initialize(GL10 gl)
	{
		//vertex shader source
		final String vertexShaderSourceCode = String.format
		(
		 "#version 320 es" +
		"\n" +
		"in vec4 vPosition;" +
		"in vec3 vNormal;" +
		"uniform mat4 u_model_matrix;" +
		"uniform mat4 u_view_matrix;" +
		"uniform mat4 u_projection_matrix;" +
		"uniform mediump int l_key_pressed;" +
		"uniform mediump vec4 u_light_position;" +
		"out vec4 eye_cordinate;" +
		"out vec3 trasformed_normal;" +
		"out vec3 light_direction;" +
		"out vec3 view_vector;" +
		"void main(void)" +
		"{" +
		"if(l_key_pressed == 1)" +
		"{" +
		"eye_cordinate = u_view_matrix * u_model_matrix * vPosition;" +
		"trasformed_normal = mat3(u_view_matrix * u_model_matrix) * vNormal;" +
		"light_direction = vec3(u_light_position - eye_cordinate);" +
		"view_vector = vec3(-eye_cordinate);" +
		"}" +
		"gl_Position = u_projection_matrix * u_view_matrix * u_model_matrix * vPosition;" +
		"}"
		);
		
		vertexShaderObject = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
		GLES32.glShaderSource(vertexShaderObject, vertexShaderSourceCode);
		
		//compile source code
		GLES32.glCompileShader(vertexShaderObject);
		
		//compile staus error chec
		int[] iShaderCompileStatus = new int[1];
		int[] iInfoLogLength = new int[1];
		String szInfoLog = null;
		
		GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_COMPILE_STATUS, iShaderCompileStatus, 0);
		
		if(iShaderCompileStatus[0] == GLES32.GL_FALSE)
		{
			GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
			
			if(iInfoLogLength[0] > 0)
			{
				szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject);
				System.out.println("SSD: vertex shader compilation log = " + szInfoLog);
				uninitialize();
				System.exit(0);
			}
		}
		
		
		//fragment shader
		//create shader
		fragmentShaderObject = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
		
		//fragment shader source code
		final String fragmentShaderSourceCode = String.format
		(
		 "#version 320 es" +
		"\n" +
		"precision highp float;" +
		"vec3 fong_ads_lighting;" +
		"in vec4 eye_cordinate;" +
		"in vec3 trasformed_normal;" +
		"in vec3 light_direction;" +
		"in vec3 view_vector;" +
		"uniform vec3 u_la;" +
		"uniform vec3 u_ld;" +
		"uniform vec3 u_ls;" +
		"uniform vec3 u_ka;" +
		"uniform vec3 u_kd;" +
		"uniform vec3 u_ks;" +
		"uniform float u_shininess;" +
		"uniform mediump int l_key_pressed;" +
		"out vec4 fragColor;" +
		"void main(void)" +
		"{" +
		"if(l_key_pressed == 1)" +
		"{" +
		"vec3 normalised_transformed_normal = normalize(trasformed_normal);" +
		"vec3 normalised_light_direction = normalize(light_direction);" +
		"vec3 normalized_view_vector = normalize(view_vector);" +
		"vec3 refliection_vector = reflect(-normalised_light_direction, normalised_transformed_normal);" +
		"vec3 ambient = u_la * u_ka;" +
		"vec3 diffuse = u_ld * u_kd * max(dot(normalised_light_direction, normalised_transformed_normal), 0.0);" +
		"vec3 specular = u_ls * u_ks* pow(max(dot(refliection_vector, normalized_view_vector), 0.0), u_shininess);" +
		"fong_ads_lighting = ambient + diffuse + specular;" +
		"}" +
		"else" +
		"{" +
		"fong_ads_lighting = vec3(1.0, 1.0, 1.0);" +
		"}" +
		"fragColor = vec4(fong_ads_lighting, 1.0);" +
		"}"
		);
		 
		 //provide source code
		 GLES32.glShaderSource(fragmentShaderObject, fragmentShaderSourceCode);
		 
		 //compile fragment shader
		 GLES32.glCompileShader(fragmentShaderObject);
		 
		 //compile status
		 iShaderCompileStatus[0] = 0;
		 iInfoLogLength[0] = 0;
		 szInfoLog = null;
		 
		 GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_COMPILE_STATUS, iShaderCompileStatus, 0);
		
		if(iShaderCompileStatus[0] == GLES32.GL_FALSE)
		{
			GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
			
			if(iInfoLogLength[0] > 0)
			{
				szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject);
				System.out.println("SSD: fragment shader compilation log = " + szInfoLog);
				uninitialize();
				System.exit(0);
			}
		}
		
		
		//shader program object
		shaderProgramObject = GLES32.glCreateProgram();
		
		//attach vertex shader
		GLES32.glAttachShader(shaderProgramObject, vertexShaderObject);
		
		//attach fragment shader
		GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
		
		//pre linking post attaching attribute binding
		GLES32.glBindAttribLocation(shaderProgramObject, GLESMacros.SSD_ATTRIBUTE_VERTEX, "vPosition");
		GLES32.glBindAttribLocation(shaderProgramObject, GLESMacros.SSD_ATTRIBUTE_NORMAL, "vNormal");
		
		//linking progam
		GLES32.glLinkProgram(shaderProgramObject);
		
		iShaderCompileStatus[0] = 0;
		 iInfoLogLength[0] = 0;
		 szInfoLog = null;
		 
		 GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_LINK_STATUS, iShaderCompileStatus, 0);
		
		if(iShaderCompileStatus[0] == GLES32.GL_FALSE)
		{
			GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
			
			if(iInfoLogLength[0] > 0)
			{
				szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject);
				System.out.println("SSD: shader shader link log = " + szInfoLog);
				uninitialize();
				System.exit(0);
			}
		}
		
		
		//get MVP uniform location
		modelMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_model_matrix");
		viewMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_view_matrix");
		projectionMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_projection_matrix");
		laUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_la");
		ldUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_ld");
		kaUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_ka");
		ksUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_ks");
		lsUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_ls");
		kdUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_kd");
		lightDirectionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_light_position");
		shininessUniform = GLES32.glGetUniformLocation(shaderProgramObject, "u_shininess");
		lKeyPressedUniform = GLES32.glGetUniformLocation(shaderProgramObject, "l_key_pressed");
		
		
	
		Sphere sphere=new Sphere();
        float sphere_vertices[]=new float[1146];
        float sphere_normals[]=new float[1146];
        float sphere_textures[]=new float[764];
        short sphere_elements[]=new short[2280];
        sphere.getSphereVertexData(sphere_vertices, sphere_normals, sphere_textures, sphere_elements);
        
		numVertices = sphere.getNumberOfSphereVertices();
        numElements = sphere.getNumberOfSphereElements();
		
		
		
		
		
		// vao
        GLES32.glGenVertexArrays(1,vao_sphere,0);
        GLES32.glBindVertexArray(vao_sphere[0]);
        
        // position vbo
        GLES32.glGenBuffers(1,vbo_sphere_position,0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo_sphere_position[0]);
        
        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(sphere_vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer verticesBuffer=byteBuffer.asFloatBuffer();
        verticesBuffer.put(sphere_vertices);
        verticesBuffer.position(0);
        
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
                            sphere_vertices.length * 4,
                            verticesBuffer,
                            GLES32.GL_STATIC_DRAW);
        
        GLES32.glVertexAttribPointer(GLESMacros.SSD_ATTRIBUTE_VERTEX,
                                     3,
                                     GLES32.GL_FLOAT,
                                     false,0,0);
        
        GLES32.glEnableVertexAttribArray(GLESMacros.SSD_ATTRIBUTE_VERTEX);
        
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,0);
        
        // normal vbo
        GLES32.glGenBuffers(1,vbo_sphere_normal,0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo_sphere_normal[0]);
        
        byteBuffer=ByteBuffer.allocateDirect(sphere_normals.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        verticesBuffer=byteBuffer.asFloatBuffer();
        verticesBuffer.put(sphere_normals);
        verticesBuffer.position(0);
        
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
                            sphere_normals.length * 4,
                            verticesBuffer,
                            GLES32.GL_STATIC_DRAW);
        
        GLES32.glVertexAttribPointer(GLESMacros.SSD_ATTRIBUTE_NORMAL,
                                     3,
                                     GLES32.GL_FLOAT,
                                     false,0,0);
        
        GLES32.glEnableVertexAttribArray(GLESMacros.SSD_ATTRIBUTE_NORMAL);
        
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,0);
        
        // element vbo
        GLES32.glGenBuffers(1,vbo_sphere_element,0);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER,vbo_sphere_element[0]);
        
        byteBuffer=ByteBuffer.allocateDirect(sphere_elements.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer elementsBuffer=byteBuffer.asShortBuffer();
        elementsBuffer.put(sphere_elements);
        elementsBuffer.position(0);
        
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER,
                            sphere_elements.length * 2,
                            elementsBuffer,
                            GLES32.GL_STATIC_DRAW);
        
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER,0);

        GLES32.glBindVertexArray(0);

		
		//enable depth testing
		GLES32.glEnable(GLES32.GL_DEPTH_TEST);
		GLES32.glDepthFunc(GLES32.GL_LEQUAL);
		
		//GLES32.glEnable(GLES32.GL_CULL_FACE);
		
		//set background color
		GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		Matrix.setIdentityM(perspectiveProjectionMatrix, 0);
		
	}
	
	private void resize(int width, int height)
	{
		GLES32.glViewport(0, 0, width, height);
		
		Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, (float)width / (float) height, 1.0f, 100.0f);
	}
	
	public void display()
	{
		
		GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);
		
		GLES32.glUseProgram(shaderProgramObject); 
		
		ByteBuffer byteBufferDirection = ByteBuffer.allocateDirect(lightDirection.length * 4);
		byteBufferDirection.order(ByteOrder.nativeOrder());
		FloatBuffer lightDirectionBuffer = byteBufferDirection.asFloatBuffer();
		lightDirectionBuffer.put(lightDirection);
		lightDirectionBuffer.position(0);
		
		ByteBuffer byteBufferAmbient = ByteBuffer.allocateDirect(ambientLight.length * 4);
		byteBufferAmbient.order(ByteOrder.nativeOrder());
		FloatBuffer lightAmbientBuffer = byteBufferAmbient.asFloatBuffer();
		lightAmbientBuffer.put(ambientLight);
		lightAmbientBuffer.position(0);
		
		ByteBuffer byteBufferDiffuse = ByteBuffer.allocateDirect(diffuseLight.length * 4);
		byteBufferDiffuse.order(ByteOrder.nativeOrder());
		FloatBuffer lightDiffuseBuffer = byteBufferDiffuse.asFloatBuffer();
		lightDiffuseBuffer.put(diffuseLight);
		lightDiffuseBuffer.position(0);
		
		ByteBuffer byteBufferSpecular = ByteBuffer.allocateDirect(specularLight.length * 4);
		byteBufferSpecular.order(ByteOrder.nativeOrder());
		FloatBuffer lightSpecularBuffer = byteBufferSpecular.asFloatBuffer();
		lightSpecularBuffer.put(specularLight);
		lightSpecularBuffer.position(0);
		
		//material
		ByteBuffer byteMaterialAmbient = ByteBuffer.allocateDirect(materialAmbient.length * 4);
		byteMaterialAmbient.order(ByteOrder.nativeOrder());
		FloatBuffer lightMaterialABuffer = byteMaterialAmbient.asFloatBuffer();
		lightMaterialABuffer.put(materialAmbient);
		lightMaterialABuffer.position(0);
		
		ByteBuffer byteMaterialDiffuse = ByteBuffer.allocateDirect(materialDiffuse.length * 4);
		byteMaterialDiffuse.order(ByteOrder.nativeOrder());
		FloatBuffer lightMaterialDBuffer = byteMaterialDiffuse.asFloatBuffer();
		lightMaterialDBuffer.put(materialDiffuse);
		lightMaterialDBuffer.position(0);
		
		ByteBuffer byteMaterialSpecular = ByteBuffer.allocateDirect(materialSpecular.length * 4);
		byteMaterialSpecular.order(ByteOrder.nativeOrder());
		FloatBuffer lightMaterialSBuffer = byteMaterialSpecular.asFloatBuffer();
		lightMaterialSBuffer.put(materialSpecular);
		lightMaterialSBuffer.position(0);
		
		
		if(gbLighting == true)
		{
			GLES32.glUniform1i(lKeyPressedUniform, 1);
			GLES32.glUniform4fv(lightDirectionUniform, 1, lightDirectionBuffer);
			GLES32.glUniform3fv(ldUniform, 1, lightDiffuseBuffer);
			GLES32.glUniform3fv(laUniform, 1, lightAmbientBuffer);
			GLES32.glUniform3fv(lsUniform, 1, lightSpecularBuffer);
			GLES32.glUniform3fv(kaUniform, 1, lightMaterialABuffer);
			GLES32.glUniform3fv(kdUniform, 1, lightMaterialDBuffer);
			GLES32.glUniform3fv(ksUniform, 1, lightMaterialSBuffer);
			GLES32.glUniform1f(shininessUniform, shininess);
			
		}else
		{
			GLES32.glUniform1i(lKeyPressedUniform, 0);
		}
		
		float modelMatrix[] = new float[16];
		float viewMatrix[] = new float[16];
		float translateMatrix[] = new float[16];
		float rotateMatrix[] = new float[16];
		
		//set identity
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(viewMatrix, 0);
		Matrix.setIdentityM(translateMatrix, 0);
		Matrix.setIdentityM(rotateMatrix, 0);
		Matrix.translateM(translateMatrix, 0, 0.0f, 0.0f, -4.0f);
		Matrix.setRotateM(rotateMatrix, 0, yRotate, 0.0f, 1.0f, 0.0f);
		
		//matrix multiply
		Matrix.multiplyMM(modelMatrix, 0, translateMatrix, 0, rotateMatrix, 0);
		
		
		GLES32.glUniformMatrix4fv(modelMatrixUniform, 1, false, modelMatrix, 0);
		GLES32.glUniformMatrix4fv(viewMatrixUniform, 1, false, viewMatrix, 0);
		GLES32.glUniformMatrix4fv(projectionMatrixUniform, 1, false, perspectiveProjectionMatrix, 0);
		
		// bind vao
        GLES32.glBindVertexArray(vao_sphere[0]);
        
        // *** draw, either by glDrawTriangles() or glDrawArrays() or glDrawElements()
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_sphere_element[0]);
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, numElements, GLES32.GL_UNSIGNED_SHORT, 0);
        
        // unbind vao
        GLES32.glBindVertexArray(0);
		GLES32.glUseProgram(0);
		
		if(gbAnimate == true)
		{
			if(yRotate < 360.0f)
			yRotate += 0.5f;
		else
			yRotate = 0.0f;
		}
		
		requestRender();
	}
	
	private void uninitialize()
	{
		//code
		//destroy vao
		
		 if(vao_sphere[0] != 0)
        {
            GLES32.glDeleteVertexArrays(1, vao_sphere, 0);
            vao_sphere[0]=0;
        }
        
        // destroy position vbo
        if(vbo_sphere_position[0] != 0)
        {
            GLES32.glDeleteBuffers(1, vbo_sphere_position, 0);
            vbo_sphere_position[0]=0;
        }
        
        // destroy normal vbo
        if(vbo_sphere_normal[0] != 0)
        {
            GLES32.glDeleteBuffers(1, vbo_sphere_normal, 0);
            vbo_sphere_normal[0]=0;
        }
        
        // destroy element vbo
        if(vbo_sphere_element[0] != 0)
        {
            GLES32.glDeleteBuffers(1, vbo_sphere_element, 0);
            vbo_sphere_element[0]=0;
        }
		
		
		if(shaderProgramObject != 0)
		{
			if(vertexShaderObject != 0)
			{
				GLES32.glDetachShader(shaderProgramObject, vertexShaderObject);
				GLES32.glDeleteShader(vertexShaderObject);
				vertexShaderObject = 0;
			}
			
			if(fragmentShaderObject != 0)
			{
				GLES32.glDetachShader(shaderProgramObject, fragmentShaderObject);
				GLES32.glDeleteShader(fragmentShaderObject);
				fragmentShaderObject = 0;
			}
			
			GLES32.glDeleteProgram(shaderProgramObject);
		}
		
		
	}
	
}

