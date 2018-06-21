package nl.bigbrotter.androidclient.Controllers;

import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
import nl.bigbrotter.androidclient.Model.Key;
import nl.bigbrotter.androidclient.R;
import nl.bigbrotter.androidclient.View.ChatAdapter;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    //Pop-up
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    //Views
    private SrsPublisher publisher;
    private Button btnStream, btnSendMessage, btnGetUrl;
    private ImageView btnToggleVideo, btnToggleAudio;
    private EditText etMessage;
    private RecyclerView chatView;
    private RecyclerView.Adapter chatAdapter;

    //Stream info
    private String streamId;
    private String streamKey;
    private String streamerId;
    private String certificate;
    private String url;

    //Volley requests
    private RequestQueue queue;

    private Timer timer;
    private List<Chat> chatMessages;
    private long longTime;

    //
    private boolean bMicState = true, bCameraState = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(getApplicationContext());
        certificate = Key.getCertificate(getApplicationContext());
        longTime = System.currentTimeMillis() / 1000L;

        etMessage = findViewById(R.id.etChat);
        etMessage.setEnabled(false);

        btnStream = findViewById(R.id.btnStream);
        btnStream.setEnabled(false);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);
        btnToggleAudio = findViewById(R.id.btnToggleAudio);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnSendMessage.setEnabled(false);
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
        publisher.setPreviewResolution(640, 360);
        publisher.setOutputResolution(720, 1280);
        publisher.setVideoHDMode();
        publisher.startCamera();
        publisher.switchCameraFace(0);

        timer = new Timer();

        //Start and stop the stream
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
                    stopGetChat();
                    btnStream.setEnabled(false);
                    btnGetUrl.setEnabled(true);
                    etMessage.setEnabled(false);
                    btnSendMessage.setEnabled(false);
                }
            }
        });

        //Turn audio on and off
        btnToggleAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bMicState) {
                    publisher.setSendVideoOnly(true);
                    btnToggleAudio.setBackgroundResource(R.drawable.mic_off);
                    bMicState = false;
                } else {
                    publisher.setSendVideoOnly(false);
                    btnToggleAudio.setBackgroundResource(R.drawable.mic_on);
                    bMicState = true;
                }
            }
        });

        //Turn video on and off
        btnToggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bCameraState) {
                    publisher.setSendAudioOnly(true);
                    btnToggleVideo.setBackgroundResource(R.drawable.video_off);
                    bCameraState = false;
                } else {
                    publisher.setSendAudioOnly(false);
                    btnToggleVideo.setBackgroundResource(R.drawable.video_on);
                    bCameraState = true;
                }
            }
        });

        btnGetUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStreamInfo();
                getChat();
                btnStream.setEnabled(true);
                btnGetUrl.setEnabled(false);
                etMessage.setEnabled(true);
                btnSendMessage.setEnabled(true);
            }
        });

        //Send messages
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String message = etMessage.getText().toString();
                    new SendChatDetails().execute("https://bigbrotter.herokuapp.com/api/chat", streamerId, message, encryptRSA(hashSHA256(message)), certificate);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                etMessage.setText("");
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(streamKey != null) {
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Deleting stream info...").setCancelable(false);
            dialog = builder.create();
            dialog.show();
            deleteStreamKey();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        publisher.stopPublish();
        if (streamKey != null) {
            deleteStreamKey();
            stopGetChat();
            btnStream.setEnabled(false);
            btnGetUrl.setEnabled(true);
            etMessage.setEnabled(false);
            btnSendMessage.setEnabled(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        publisher.stopPublish();
        if(streamKey != null) {
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Deleting stream info...").setCancelable(false);
            dialog = builder.create();
            dialog.show();
            deleteStreamKey();
        }else {
            super.onDestroy();
        }
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

    public void getStreamInfo() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, "https://bigbrotter.herokuapp.com/api/streams", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.i("", response.toString());
                    JSONObject stream = response.getJSONObject("stream");
                    streamId = stream.getString("_id");
                    streamKey = stream.getString("key");
                    streamerId = stream.getString("user");
                    url = response.getString("stream_url");
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

                //remove local stream info if not done so already.
                streamKey = null;
                url = "";

                //End activity when back button is pressed after key is deleted.
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
        timer = new Timer();
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
                            //e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Log.i("", error.getMessage());
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("streamer", streamerId);
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

    public void stopGetChat() {
        timer.cancel();
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
            String privateKey = Key.getPrivateKey(getApplicationContext());

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(privateKey));
            byte[] encryptedBytes = cipher.doFinal(input.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("", e.getMessage());
            return null;
        }
    }

    public String decryptRSA(String input) {
        try {
            String publicKey = Key.getPublicKey(getApplicationContext());

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.DECRYPT_MODE, getPublicKey(publicKey));
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(input.getBytes("UTF-8"), Base64.DEFAULT));
            return new String(decryptedBytes);
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
                params.put("streamer", strings[1]);
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