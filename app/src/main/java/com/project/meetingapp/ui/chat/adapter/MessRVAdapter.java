package com.project.meetingapp.ui.chat.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.meetingapp.R;
import com.project.meetingapp.models.DetailMessage;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MessRVAdapter extends RecyclerView.Adapter<MessRVAdapter.ViewHolder> {
    private final List<DetailMessage> detailMessages;
    private final int layout;
    private final String email;

    public MessRVAdapter(List<DetailMessage> detailMessages, int layout, String email) {
        this.detailMessages = detailMessages;
        this.layout = layout;
        this.email = email;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetailMessage message = detailMessages.get(position);
        holder.setIsRecyclable(false);
        holder.idTVTextMess.setText(message.text);
        if (!message.user_send.equals(email)) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParams.addRule(RelativeLayout.BELOW, R.id.idTVTime);

            holder.idTVTextMess.setLayoutParams(layoutParams);
            holder.idTVTextMess.setTextColor(Color.WHITE);
            holder.idTVTextMess.setBackgroundResource(R.drawable.bg_mess_received);

            RelativeLayout.LayoutParams layoutParamsImg = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsImg.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParamsImg.addRule(RelativeLayout.BELOW, R.id.idTVTextMess);
            layoutParamsImg.topMargin = 20;

            holder.imgImage.setLayoutParams(layoutParamsImg);
        }
        holder.idTVTime.setText(message.time);

        if (message.img != null) {
            Picasso.get().load(message.img).into(holder.imgImage);
        } else {
            holder.imgImage.setVisibility(View.GONE);
        }

        holder.idTVTextMess.setOnClickListener(v -> {
            if (holder.idTVTime.getVisibility() == View.VISIBLE)
                holder.idTVTime.setVisibility(View.GONE);
            else
                holder.idTVTime.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public int getItemCount() {
        return detailMessages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView idTVTime;
        private final TextView idTVTextMess;
        private final ImageView imgImage;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            idTVTime = itemView.findViewById(R.id.idTVTime);
            idTVTextMess = itemView.findViewById(R.id.idTVTextMess);
            imgImage = itemView.findViewById(R.id.imgImage);
        }
    }
}
