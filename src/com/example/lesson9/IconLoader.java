package com.example.lesson9;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: asus
 * Date: 19.11.13
 * Time: 18:38
 * To change this template use File | Settings | File Templates.
 */
public class IconLoader extends IntentService {
    final static private String TAG = "Reloader service (icon)";
    final static String LOADING_ERROR = "Error loading (icon)";

    public IconLoader(){
        super("IconLoader");
    }

    public void onCreate(){
        super.onCreate();

    }

    static String a;
    @Override
    protected void onHandleIntent(Intent intent) {

        String url = intent.getStringExtra("url");

        Intent intentResponse = new Intent();
        intentResponse.setAction(MainActivity.ACTION_response);
        intentResponse.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            //Bitmap pic = grabImageFromUrl(url);


        } catch (Exception ex){
            Log.w(TAG, ex.getMessage());
            intentResponse.putExtra(LOADING_ERROR, true);
        }
        intent.putExtra("type", MainActivity.ICON_LOAD);
        //sendBroadcast(intentResponse);
        a = "";

    }

    private Bitmap grabImageFromUrl(String url) throws Exception {
        boolean error = false;
        InputStream in = null;
        Bitmap bt = null;
        try{
            in = (InputStream) new URL(url).getContent();
            bt = BitmapFactory.decodeStream(in);
        } catch (Exception ex){
            error = true;
        } finally {
            try {
                in.close();
            } catch (Throwable ex){
            }
        }

        if (error){
            throw new Exception("Icon loading failed");
        }
        return bt;
    }
}

