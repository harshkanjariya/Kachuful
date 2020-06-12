package com.harsh.cards;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;

import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Game{
    public boolean started;
    public boolean decks;
    public int sir;
    public int initial;
    public String initId;
    public String lastId;
    public String task;
    public int round;
    public HashMap<String,Integer>CurrentHands;
    public HashMap<String,String> players;
    public ArrayList<Integer>z;
    public ArrayList<String>sequence;
    public HashMap<String,Boolean>off;
    public HashMap<String,String>leaderboard;
    public HashMap<String,String>CurrentCards;
    public HashMap<String,Integer>showing;

    static String encrypt(String data, String key){
        SecretKeySpec keySpec=new SecretKeySpec(key.getBytes(),"AES");
        try {
            Cipher ec=Cipher.getInstance("AES/ECB/PKCS5Padding");
            ec.init(Cipher.ENCRYPT_MODE,keySpec);
            return Base64.encodeToString(ec.doFinal(data.getBytes()),Base64.DEFAULT);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String decrypt(String data, String key){
        SecretKeySpec keySpec=new SecretKeySpec(key.getBytes(),"AES");
        try {
            Cipher ec=Cipher.getInstance("AES/ECB/PKCS5Padding");
            ec.init(Cipher.DECRYPT_MODE,keySpec);
            return new String(ec.doFinal(Base64.decode(data, Base64.DEFAULT)));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);
        return output;
    }
}
