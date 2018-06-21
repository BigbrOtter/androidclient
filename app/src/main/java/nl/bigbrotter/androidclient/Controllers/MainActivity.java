package nl.bigbrotter.androidclient.Controllers;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.faucamp.simplertmp.RtmpHandler;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;

import nl.bigbrotter.androidclient.Model.Chat;
import nl.bigbrotter.androidclient.R;
import nl.bigbrotter.androidclient.View.ChatAdapter;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private SrsPublisher publisher;
    private Button btnStream, btnToggleVideo, btnToggleAudio, btnSendMessage, btnGetUrl;
    private EditText etMessage, etUrl;
    private RecyclerView chatView;
    private RecyclerView.Adapter chatAdapter;
    private String streamId;
    private String streamKey;
    private String certificate;
    private String url = "rtmp://145.49.34.214:1935/live/";
    private List<Chat> chatMessages;
    private long longTime;

    //Volley requests
    private RequestQueue queue;

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

        queue = Volley.newRequestQueue(getApplicationContext());
        //TODO: make certificate dynamic
        certificate = "KKsOCYy0uQgcKIPU8OgI8PtAXQEecQzqVBL6QTcnY0RzHRSc+0BXdyEpCROeTpjY3F6hWcEcZoVY5P7mehH33+310457oFqZRy8j350eWDfp9lKuShVGR0Doq212q6mOtMxxkv8JM2+WjDmsKdxmvUJJnJJ8Z7AQmAd3z+2ch1ZijajM/ai1itOegvmuA8XE6iu9AAXdvUNkaGC9b06kU3OtxWHbnqHFQ4ei66zP8hwiyhjsyZVRGSeDSuTjfqFkdKmMgvDuBEiQsYrdhm3myqKpUVlarM/Cr33vpwwoP9XZY8xEFxx9jzIqBVHEb3b5ca1owIrl/4ZDZUw58cTk9oCRY09TMt3TUx0Ed7o64NlQ9j85/FwuzIRYZOsuwnyho2+AG14lWM7iPCvbOlVcvV+A98fqHxXxLc4UnUFi5O7CalbEp6e7InhVsugprO4ZlvNLhtiVSzlXhk29ysBFoQWMxNtNvRbp8Hw6sLxKAYpdqTZmUd33/KLPeEJFNvM4ENT5desuP7KVAm6LZozOwMPXf4IkL9jvsbXCCFvNYwgvqfDGrvGit3whgg3krJTmJVKCtrsQ0Y3aevUbhDsRcBViFr+5dHDzMLTzkJ2oMcDz0ncA74nL7G/wIa385Fi8QYoMEnITt/GGqtmOGkNYDtYtN0G1Pf4LaSOeXBHJ8pg=";
        longTime = System.currentTimeMillis() / 1000L;

        etUrl = findViewById(R.id.etUrl);
        etUrl.setHint("insert URL...");
        etUrl.setText(url);

        etMessage = findViewById(R.id.etChat);

        btnStream = findViewById(R.id.btnStream);
        btnStream.setEnabled(false);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);
        btnToggleAudio = findViewById(R.id.btnToggleAudio);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnGetUrl = findViewById(R.id.btnGetUrl);

        chatMessages = new ArrayList<>();
        chatView = findViewById(R.id.chatView);
        chatView.setHasFixedSize(true);
        RecyclerView.LayoutManager chatLayoutManager = new LinearLayoutManager(this);
        chatView.setLayoutManager(chatLayoutManager);
        chatAdapter = new ChatAdapter(getApplicationContext(), chatMessages);
        chatView.setAdapter(chatAdapter);

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

        //Start and stop the stream
        //TODO: reset url when person stops streaming
        btnStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStream.getText().toString().contentEquals(getResources().getString(R.string.start_stream))) {

                    publisher.startPublish(url);
                    publisher.startCamera();
                    btnStream.setText(getResources().getString(R.string.stop_stream));
                } else {
                    publisher.stopPublish();
                    btnStream.setText(getResources().getString(R.string.start_stream));
                    deleteStreamKey();
                    btnStream.setEnabled(false);
                    btnGetUrl.setEnabled(true);
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

        btnGetUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStreamInfo();
                btnStream.setEnabled(true);
                btnGetUrl.setEnabled(false);
            }
        });

        //Send messages
        //TODO: replace hardcoded with real values
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String message = etMessage.getText().toString();
                    new SendChatDetails().execute("https://bigbrotter.herokuapp.com/api/chat", "1", message, encryptRSA(hashSHA256(message)), certificate);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                etMessage.setText("");
            }
        });
        getChat();
    }

    @Override
    public void onBackPressed() {
        if(streamKey != null) {
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Deleting stream info...").setCancelable(false);
            dialog = builder.create();
            dialog.show();
            deleteStreamKey();
        }
    }

    @Override
    protected void onDestroy() {
        publisher.stopPublish();
        super.onDestroy();
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


    public long getLongTime() {
        return longTime;
    }

    public void setLongTime(long longTime) {
        this.longTime = longTime;
    }

    //TODO: get the other stream info as well. Make url dynamic
    public void getStreamInfo() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, "https://bigbrotter.herokuapp.com/api/streams", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.i("", response.toString());
                    JSONObject stream = response.getJSONObject("stream");
                    streamId = stream.getString("_id");
                    streamKey = stream.getString("key");

                    url += streamKey;
                    etUrl.setText(url);

                } catch (JSONException jE) {
                    Log.i("", jE.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("", error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("cert", certificate);
                return headers;
            }
        };
        queue.add(jsonRequest);
    }

    public void deleteStreamKey() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.DELETE, "https://bigbrotter.herokuapp.com/api/streams/" + streamId, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("", response.toString());

                //remove local stream info in not done so already.
                streamKey = null;
                if (dialog != null) {
                    finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("", error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("cert", certificate);
                return headers;
            }
        };
        queue.add(jsonRequest);
    }

    public void getChat() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, "https://bigbrotter.herokuapp.com/api/chat", null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("chats");
                            String signature = response.getString("signature");
                            String decryptedSignature = decryptRSA(signature);
                            String arrayString = array.toString();
                            arrayString = arrayString.replaceAll("\\\\", "");
                            String hashedArray = hashSHA256(arrayString);

                            if (hashedArray.equals(decryptedSignature)) {
                                String message;
                                for (int i = 0; i < array.length(); i++) {
                                    message = "";
                                    JSONObject object = array.getJSONObject(i);
                                    message += object.getString("name") + ": " + object.getString("message");
                                    chatMessages.add(new Chat(message));
                                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                                    chatView.scrollToPosition(chatAdapter.getItemCount() - 1);
                                }
                                JSONObject getTime = array.getJSONObject(array.length() - 1);
                                setLongTime(getTime.getLong("timestamp"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("", error.getMessage());
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("streamer", "1");
                        //headers.put("timestamp", "0");
                        headers.put("timestamp", String.valueOf(getLongTime()));
                        headers.put("cert", certificate);
                        return headers;
                    }
                };
                queue.add(jsonRequest);
            }
        };
        timer.schedule(task, 0, 1000);
    }

    public void handleException(Exception e) {
        Toast.makeText(getApplicationContext(), "An error has occurred.", Toast.LENGTH_SHORT).show();
        Log.e("ERROR", e.getMessage());
        publisher.stopPublish();
        btnStream.setText(getResources().getString(R.string.start_stream));
    }

    public String hashSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(input.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b: digest) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }

    public String encryptRSA(String input) {
        try {
            //TODO: replace hardcoded key with actual key
            String privkey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIIEpQIBAAKCAQEAoXVmDDzqkBD9wMpxBPxLNFqTg5Ru4llWPT0ofOroaEAQMxEL\n" +
                    "I/PUxz5ToCT7xqoraPClsy1Sfrtof+dyuHexFCiQxHPnFe4uSmRgCEWyOmgsj7JD\n" +
                    "A268BEEmlJ8WDoht2r4JW2Qsnjll10xBDv11Lnhcc3ubWi6DvP3LoI/KJcZJBwJd\n" +
                    "aoo5Lq4s5TGC81XYXTEDmIDDEO+O8WbBq2CuTUT6nsBoLoIjMjJgk6qrNkSMw9ya\n" +
                    "ZJsUqfZsJ3zaGFJe0DkTuX7jUer4akbfuRC+Qb8HUf5aGyLb7is6ZhbFDPULwVPH\n" +
                    "3xQz9FiNQbJBxiTgyMXLhrmyhSWJHBqFKNMa5QIDAQABAoIBACLvyU4anFLiKlZe\n" +
                    "N8hxY0CH3OWa58d4t012/1zQY8uzGQ5DwNpdt4wJc4Tym7xoNA54DBLSWshrevg6\n" +
                    "N7uswpdvE6w+vCElscSNJa6EjkVPJ11MoG2Mt4hgJJ4CMn6gjMzJVDL/YRw3pU7K\n" +
                    "BEXfGE0e5Dpk47/G0uDBNh+fHYnAnyy1uQQ0xNRPk9Pdt2ctP+wAns812nZtoC64\n" +
                    "h8ItPHQ2EEAIM0zr6nN3043H3JlSEK/5bYe/DeZ6atdPTCWChupF+7ftceuEbEx5\n" +
                    "41I2zGtlmasVyozVmCyR9jJywJxPIi1exwGceRhpxWOOulQuSBl1GFUAYGQXFg79\n" +
                    "/xOSWAECgYEAzfRCBRU79kzMuTFkoh4B9HHBSHzJIU5sCgP5rvcQiRt1ak7LIhci\n" +
                    "Wr3nyeB4n2IWOPSXD2DpbnzQ6fMcHNHMcV2IdZLc4hQkEjlN6JqjmMyD8yyHHAO4\n" +
                    "eNy71nM9UEMeSmTWKp6Dy1ElH3ubqSDgiIjCKvCV/GBcoj1tNj5iDgECgYEAyLE3\n" +
                    "bFGEwrWb6XMb6A5hGlMnwnoHW1nFxz7URQDfgWXUCElOhLsM+GTxHvdMpgKr+K5J\n" +
                    "uNicTLMNjCNn/Pexzhg/5jMIJcxGmsnfUQ2+bKu6+m1unsAYCDnQ0rE2TFmhWQcf\n" +
                    "P3s+WSHy2/WQUzxtFXFwG4VkZ2i5oHt/HHMElOUCgYEAiEyaVJrU8A+rfPQ/UTri\n" +
                    "uE+ARuSuhyhLP+WZnD1N6C8P6abzsD/3MG51s5imu3RCmLbmMftFASYBbJLDjB8c\n" +
                    "Wfo4kPb8z3Hc3WKnOMT+d+UBfjF9yQB9WR9cAHSLo06IAVvykIoPVsMA+nDnd2qW\n" +
                    "rkUzmw9Vc4yiQYy9diSa6AECgYEAuEdJXdOodOUvSXfhyv3RGcv7OS61rKLM4TwG\n" +
                    "y2mW0QlAXW96gpQCv95oLQfkwJa5c/oNRYbYVfEfYmtsY7LI+DX6DpUTSSm+Nwlg\n" +
                    "Xduh28UARkzPg0NdjcgQwDXqZsbySX4pqi+vO0bZ6jEcmeFlRIhJ6WtdmzplID/l\n" +
                    "oqjWLyUCgYEAvc3rstwN4eQQ2hP6/SD5ZaClBSAkvv0nGVqyOyhFMQXLn8Oef7x8\n" +
                    "xBkCEFy6lBZXXrqV+bvsJTYO1aQG8XqqMs+g+MHImTCkkVFR6dqpFixahnuw2vLu\n" +
                    "ABVbpeZfHHd7rVljZFOoHlAHED3iZ5Hk3BCsa92VTVPaWJJ7P2Z74vM=\n" +
                    "-----END RSA PRIVATE KEY-----";

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(privkey));
            byte[] encryptedBytes = cipher.doFinal(input.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("", e.getMessage());
            return "";
        }
    }

    public String decryptRSA(String input) {
        try {
            //TODO: replace hardcoded key with actual key
            String pubKey =
                    "-----BEGIN PUBLIC KEY-----\n" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAugGy5Ao9fJJon1IPXs/+\n" +
                    "jmgSelj7oz+ORcfc2IavZyoGMQbZpmkheoItOmVx62YFse3y1zie1nHd9Rx/jgu0\n" +
                    "GTY7VZHLWQ8j2mDvfus7/XZwEBgPzgawF4xsoivPQNLSdJ6t5kzI1CynX3M1JsEi\n" +
                    "/Xv+TacqgmaeAaX1KWOWIMrtL3YxFL+pLUTeQ5mNr+9ooHB8Cik34dXqJjYGk1J+\n" +
                    "HY27wWRCxoXH7m143ZVW0VAHyQ6/3De3AbgQtsP6Kkg2qc5us2uC333r06YPwW1c\n" +
                    "Q7yLMHHmu61CEH1B9xBA6dy78nI2zVQZeGXJR4OhUR0ZpIEnpo8NwvQ5qN+xIkV9\n" +
                    "CwIDAQAB\n" +
                    "-----END PUBLIC KEY-----";

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.DECRYPT_MODE, getPublicKey(pubKey));
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(input.getBytes("UTF-8"), Base64.DEFAULT));
            //byte[] decryptedBytes = Base64.decode(input.getBytes(), Base64.DEFAULT);
            return new String(decryptedBytes);
            //return Base64.encodeToString(decryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private PrivateKey getPrivateKey(String keyString) {
        try {
            StringBuilder pkcs8Lines = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(keyString));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!(line.startsWith("-----") || line.endsWith("-----")))
                pkcs8Lines.append(line);
            }

            String pkcs8Pem = pkcs8Lines.toString();
            byte[] pkcs8EncodedBytes = Base64.decode(pkcs8Pem, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private PublicKey getPublicKey(String keyString) {
        try {
            StringBuilder x509Lines = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(keyString));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!(line.startsWith("-----") || line.endsWith("-----")))
                x509Lines.append(line);
            }

            String x509Pem = x509Lines.toString();
            byte[] x509EncodedBytes = Base64.decode(x509Pem, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509EncodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class SendChatDetails extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {

            try {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("streamer", Integer.parseInt(strings[1]));
                params.put("message", strings[2]);
                params.put("signature", strings[3]);
                params.put("cert", strings[4]);

                URL api_url = new URL(strings[0]);
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (postData.length() > 0) postData.append("&");
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append("=");
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }

                byte[] postDataBytes = postData.toString().getBytes("UTF-8");
                HttpURLConnection conn = (HttpURLConnection) api_url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                conn.disconnect();
                return stringBuilder.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}