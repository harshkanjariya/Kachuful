package com.harsh.cards;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        final FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(getApplicationContext(),user==null?LoginActivity.class:MainActivity.class);
                if (user==null){
                    Pair<View,String>pair1=new Pair<>(findViewById(R.id.icon),"image");
                    Pair<View,String>pair2=new Pair<>(findViewById(R.id.splash_title),"title");
                    ActivityOptionsCompat options=ActivityOptionsCompat.makeSceneTransitionAnimation(SplashActivity.this,pair1,pair2);
                    startActivity(intent,options.toBundle());
                }else
                    startActivity(intent);
                finish();
            }
        },3000);
    }
}
