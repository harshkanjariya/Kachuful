package com.harsh.cards;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {
    TableLayout lastTable,headTable,rankTable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_result);
        lastTable=findViewById(R.id.last_table);
        headTable=findViewById(R.id.head_table);
        rankTable=findViewById(R.id.rank_table);
        Intent intent=getIntent();
        HashMap<String,String>data= (HashMap<String, String>) intent.getSerializableExtra("data");
        if (data==null)return;
        HashMap<String,String>names= (HashMap<String, String>) intent.getSerializableExtra("names");
        ArrayList<String>keys=new ArrayList<>(names.keySet());
        Collections.sort(keys);

        TableRow row=new TableRow(this);
        TextView t=new TextView(this);
        t.setText("Round");
        t.setBackground(getDrawable(R.drawable.table_head_cell));
        t.setPadding(20,10,20,10);
        t.setTextColor(Color.WHITE);
        row.addView(t);
        for (String s:keys){
            t=new TextView(this);
            t.setText(names.get(s));
            t.setBackground(getDrawable(R.drawable.table_head_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.WHITE);
            row.addView(t);
        }
        lastTable.addView(row);
        row=new TableRow(this);
        t=new TextView(this);
        t.setText("Round");
        t.setBackground(getDrawable(R.drawable.table_head_cell));
        t.setPadding(20,10,20,10);
        t.setTextColor(Color.WHITE);
        row.addView(t);
        for (String s:keys){
            t=new TextView(this);
            t.setText(names.get(s));
            t.setBackground(getDrawable(R.drawable.table_head_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.WHITE);
            row.addView(t);
        }
        headTable.addView(row);
        HashMap<String, ArrayList<String>>board=new HashMap<>();
        for(Map.Entry<String,String>e:data.entrySet())
            board.put(e.getKey(),new ArrayList<>(Arrays.asList(e.getValue().split(","))));
        final HashMap<String,Integer>sum=new HashMap<>();
        for (String k:keys)
            sum.put(k,0);
        for(int i=0;i<=board.get(keys.get(0)).size()-1;i++){
            row=new TableRow(this);
            t=new TextView(this);
            t.setText(""+(i+1));
            t.setTextColor(Color.BLACK);
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setGravity(Gravity.CENTER);
            row.addView(t);
            for(String s:keys){
                String n="";
                if (board.get(s)!=null && board.get(s).size()>i)
                    n=board.get(s).get(i);
                t=new TextView(this);
                if (n.indexOf("=")==0){
                    t.setText(" "+n.substring(1)+" ");
                    t.setPaintFlags(t.getPaintFlags()| Paint.STRIKE_THRU_TEXT_FLAG);
                }else{
                    t.setText(n);
                }
                try{
                    int sm=Integer.parseInt(n);
                    sum.put(s,sum.get(s)+sm);
                }catch (Exception e){}
                t.setBackground(getDrawable(R.drawable.table_cell));
                t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.BLACK);
                row.addView(t);
            }
            lastTable.addView(row);
        }
        row=new TableRow(this);
        t=new TextView(this);
        t.setText("Total");
        t.setBackground(getDrawable(R.drawable.table_head_cell));
        t.setPadding(20,10,20,10);
        t.setTextColor(Color.WHITE);
        row.addView(t);
        for(String s:keys){
            t=new TextView(this);
            t.setText(""+sum.get(s));
            t.setBackground(getDrawable(R.drawable.table_head_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.WHITE);
            row.addView(t);
        }
        lastTable.addView(row);
        headTable.bringToFront();

        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.compare(sum.get(o2),sum.get(o1));
            }
        });
        for (int i=0;i<keys.size();i++){
            row=new TableRow(this);

            t=new TextView(this);
            t.setText(""+(i+1));
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setGravity(Gravity.CENTER);
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.BLACK);
            row.addView(t);

            t=new TextView(this);
            t.setText(names.get(keys.get(i)));
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setGravity(Gravity.CENTER);
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.BLACK);
            row.addView(t);

            t=new TextView(this);
            t.setText(""+sum.get(keys.get(i)));
            t.setGravity(Gravity.CENTER);
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.BLACK);
            row.addView(t);

            rankTable.addView(row);
        }
    }
}