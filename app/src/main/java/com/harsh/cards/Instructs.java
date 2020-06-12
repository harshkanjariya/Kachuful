package com.harsh.cards;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Instructs extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_instructs);
        TextView t=findViewById(R.id.infotxt);
        t.setText("");
        t.setMovementMethod(new ScrollingMovementMethod());
        try{
            BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("info.txt")));
            String s;
            while((s=reader.readLine())!=null)
                t.append(s+"\n");
        }catch (Exception e){e.printStackTrace();}
    }
}
