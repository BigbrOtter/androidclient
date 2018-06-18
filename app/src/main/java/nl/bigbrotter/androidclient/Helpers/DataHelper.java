package nl.bigbrotter.androidclient.Helpers;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class DataHelper {

    public static void saveKeys(String json, Context context) throws JSONException {
        JSONObject mainObject = new JSONObject(json);
        String publicString = mainObject.getString("private");
        String privateString = mainObject.getString("public");

        SharedPreferences.Editor editor = context.getSharedPreferences("Keys", context.MODE_PRIVATE).edit();
        editor.putString("privateKey", privateString);
        editor.putString("publicKey", publicString);

        editor.apply();
        editor.commit();
    }

    public static String getPublicKey(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Keys", context.MODE_PRIVATE);
        return prefs.getString("publicKey", "errorPublic");
    }

    public static String getPrivateKey(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Keys", context.MODE_PRIVATE);
        return prefs.getString("privateKey", "errorPrivate");
    }
}
