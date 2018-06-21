package nl.bigbrotter.androidclient.Model;

/**
 * Created by Ricardo on 11-6-2018.
 */

public class Chat {

    private String username;
    private String message;
    private String timestamp;
    private int streamer;

    public Chat() {

    }

    public Chat(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStreamer() {
        return streamer;
    }

    public void setStreamer(int streamer) {
        this.streamer = streamer;
    }
}
