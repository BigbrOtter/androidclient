package nl.bigbrotter.androidclient.Controllers;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.bigbrotter.androidclient.Helpers.DataHelper;
import nl.bigbrotter.androidclient.R;
import nl.bigbrotter.androidclient.View.FileChooser;

public class LoginActivity extends AppCompatActivity {

    FileChooser filechooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        filechooser = new FileChooser(LoginActivity.this);
        filechooser.showDialog();
        filechooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {

                String json = null;
                try {
                    //Get and read file
                    InputStream is = new FileInputStream(file);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    json = new String(buffer, "UTF-8");
                    DataHelper.saveKeys(json, LoginActivity.this);

                    //Testing
                    Log.e("TESTTING", "Json: " + json);
                    Log.e("TESTTING", "Public: " + DataHelper.getPublicKey(LoginActivity.this));
                    Log.e("TESTTING", "Private: " + DataHelper.getPrivateKey(LoginActivity.this));
                } catch (IOException | JSONException ex) {
                    ex.printStackTrace();
                }

            }
        });
    }
}
