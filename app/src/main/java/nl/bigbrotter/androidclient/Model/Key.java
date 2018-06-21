package nl.bigbrotter.androidclient.Model;

/**
 * Created by Martijn on 11-6-2018.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Key {

    //Get keys from Json, and put them in the local storage
    public static void saveKeys(String json, Context context) throws JSONException {
        JSONObject mainObject = new JSONObject(json);

        String privateString = mainObject.getString("userPrivateKey");
        String publicString = mainObject.getString("serverPublicKey");
        String certificateString = mainObject.getString("userCertificate");

        SharedPreferences.Editor editor = context.getSharedPreferences("Keys", context.MODE_PRIVATE).edit();

        editor.putString("userPrivateKey", privateString);
        editor.putString("serverPublicKey", publicString);
        editor.putString("userCertificate", certificateString);

        editor.apply();
        editor.commit();
    }

    // Get public key from local storage
    public static String getPublicKey(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Keys", context.MODE_PRIVATE);
        return prefs.getString("serverPublicKey", "errorPublic");
    }

    //Get private key from local storage
    public static String getPrivateKey(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Keys", context.MODE_PRIVATE);
        return prefs.getString("userPrivateKey", "errorPrivate");
    }

    //Get certificate from local storage
    public static String getCertificate(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Keys", context.MODE_PRIVATE);
        return prefs.getString("userCertificate", "errorPublic");
    }

}
