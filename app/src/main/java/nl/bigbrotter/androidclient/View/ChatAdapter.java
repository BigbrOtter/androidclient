package nl.bigbrotter.androidclient.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import nl.bigbrotter.androidclient.Model.Chat;
import nl.bigbrotter.androidclient.R;

/**
 * Created by Ricardo on 11-6-2018.
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<Chat> chatMessages;
    private LayoutInflater inflater;

    /**
     * Constructor
     * @param context context for the LayoutInflater
     * @param messages list of messages to fill the RecyclerView
     */
    public ChatAdapter(Context context, List<Chat> messages) {
        inflater = LayoutInflater.from(context);
        chatMessages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat message = chatMessages.get(position);
        holder.setMessage(message.getMessage());
    }

    /**
     * @return Returns the amount of messages in the array
     */
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    /**
     * ViewHolder used by the RecyclerView
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView message;
        ViewHolder(View view) {
            super(view);
            message = view.findViewById(R.id.message);
        }

        public void setMessage(String message) {
            this.message.setText(message);
        }
    }
}