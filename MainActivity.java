package com.astromedicomp.Android18;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.view.View;

import android.content.pm.ActivityInfo;

public class MainActivity extends AppCompatActivity {

		private GLESView gLESView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
	  
	  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
       
	   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	   
	   gLESView = new GLESView(this);
	   
	   setContentView(gLESView);
    }
	
}

