package nl.bigbrotter.androidclient.Controllers;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import nl.bigbrotter.androidclient.Helpers.AlertHelper;
import nl.bigbrotter.androidclient.R;

public class MainActivity extends AppCompatActivity {

    Button bAlertTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bAlertTest = findViewById(R.id.activity_main_btn_alert_test);

        bAlertTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertHelper.error(MainActivity.this, "Stream werkt voor geen meter maatie!");
            }
        });

    }
}
