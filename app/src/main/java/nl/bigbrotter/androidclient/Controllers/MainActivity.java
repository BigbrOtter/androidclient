package nl.bigbrotter.androidclient.Controllers;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;

import nl.bigbrotter.androidclient.R;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private SrsPublisher publisher;
    private Button btnStream, btnToggleVideo, btnToggleAudio;
    private String url = "rtmp://145.49.60.154:1935/live/stream";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 50);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 50);
        }

        //SharedPreferences to save url for next use
        sharedPreferences = getSharedPreferences("Otter", MODE_PRIVATE);
        url = sharedPreferences.getString("url", url);

        final EditText etUrl = findViewById(R.id.etUrl);
        etUrl.setHint("insert URL...");
        etUrl.setText(url);

        btnStream = findViewById(R.id.btnStream);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);
        btnToggleAudio = findViewById(R.id.btnToggleAudio);

        //Initialize stream
        publisher = new SrsPublisher((SrsCameraView) findViewById(R.id.camera_view));
        publisher.setEncodeHandler(new SrsEncodeHandler(this));
        publisher.setRtmpHandler(new RtmpHandler(this));
        publisher.setRecordHandler(new SrsRecordHandler(this));
        publisher.setPreviewResolution(720, 1280);
        publisher.setOutputResolution(720, 1280);
        publisher.setVideoHDMode();
        publisher.startCamera();
        publisher.switchCameraFace(0);

        btnStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStream.getText().toString().contentEquals(getResources().getString(R.string.start_stream))) {
                    url = etUrl.getText().toString();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("url", url);
                    editor.apply();

                    publisher.startPublish(url);
                    publisher.startCamera();
                    btnStream.setText(getResources().getString(R.string.stop_stream));
                } else {
                    publisher.stopPublish();
                    btnStream.setText(getResources().getString(R.string.start_stream));
                }
            }
        });

        //Turn audio on and off
        btnToggleAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnToggleAudio.getText().toString().contentEquals(getResources().getString(R.string.disable_mic))) {
                    publisher.setSendVideoOnly(true);
                    btnToggleAudio.setText(getResources().getString(R.string.enable_mic));
                } else {
                    publisher.setSendVideoOnly(false);
                    btnToggleAudio.setText(getResources().getString(R.string.disable_mic));
                }
            }
        });

        //Turn video on and off
        btnToggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnToggleVideo.getText().toString().contentEquals(getResources().getString(R.string.disable_camera))) {
                    publisher.setSendAudioOnly(true);
                    btnToggleVideo.setText(getResources().getString(R.string.enable_camera));
                } else {
                    publisher.setSendAudioOnly(false);
                    btnToggleVideo.setText(getResources().getString(R.string.disable_camera));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        publisher.stopPublish();
    }

    //region RtmpHandler overrides
    @Override
    public void onRtmpConnecting(String msg) {

    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {

    }

    @Override
    public void onRtmpAudioStreaming() {

    }

    @Override
    public void onRtmpStopped() {

    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i("FPS", String.format("Output FPS: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        Log.i("VBIT", String.format("Video bitrate %f bps", bitrate));
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        Log.i("ABIT", String.format("Video bitrate %f bps", bitrate));
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }
    //endregion

    //region EncodeHandler overrides
    @Override
    public void onNetworkWeak() {

    }

    @Override
    public void onNetworkResume() {

    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }
    //endregion

    //region RecordHandler overrides
    @Override
    public void onRecordPause() {

    }

    @Override
    public void onRecordResume() {

    }

    @Override
    public void onRecordStarted(String msg) {

    }

    @Override
    public void onRecordFinished(String msg) {

    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }
    //endregion

    public void handleException(Exception e) {
        Toast.makeText(getApplicationContext(), "An error has occurred.", Toast.LENGTH_SHORT).show();
        Log.e("ERROR", e.getMessage());
        publisher.stopPublish();
        btnStream.setText(getResources().getString(R.string.start_stream));
    }
}