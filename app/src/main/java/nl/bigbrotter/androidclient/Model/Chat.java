package nl.bigbrotter.androidclient.Model;

/**
 * Created by Ricardo on 11-6-2018.
 */

//Chat messages
public class Chat {
    private String message;

    public Chat(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
