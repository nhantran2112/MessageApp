package com.project.meetingapp.models;

public class DetailMessage {
    public String user_send;
    public String text;
    public String time;

    public DetailMessage() {

    }

    public DetailMessage(String user_send, String text, String time) {
        this.user_send = user_send;
        this.text = text;
        this.time = time;
    }

}
