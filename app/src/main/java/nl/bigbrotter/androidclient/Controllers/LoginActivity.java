package nl.bigbrotter.androidclient.Controllers;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import nl.bigbrotter.androidclient.Helpers.AlertHelper;
import nl.bigbrotter.androidclient.Model.Key;
import nl.bigbrotter.androidclient.R;

public class LoginActivity extends AppCompatActivity {

    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Connect layout
        btn = findViewById(R.id.login_button);

        if(Key.getPrivateKey(LoginActivity.this).equals("errorPrivate")) {

            //Permission for Storage, Camera en Audio
            requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO},
                    105);

            //Onclick open file picker
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialFilePicker()
                            .withActivity(LoginActivity.this)
                            .withRequestCode(1)
                            .withFilter(Pattern.compile(".*\\.circle$")) // Filtering on Circle file
                            .withHiddenFiles(true) // Show hidden files and folders
                            .start();
                }
            });
        }else{
            // If user is authenticated go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Get selected file from device
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            File file = new File(filePath);

            //Get json from file
            String json = null;
            try {
                InputStream is = new FileInputStream(file);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, "UTF-8");

                //Save keys to device
                Key.saveKeys(json, LoginActivity.this);

                //Check if keys are correct
                if(Key.getPrivateKey(LoginActivity.this).equals("errorPrivate")){
                    AlertHelper.error(LoginActivity.this, "Er is iets mis gegaan!");
                }else {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            } catch (IOException | JSONException ex) {
                ex.printStackTrace();
            }
        }
    }
}
