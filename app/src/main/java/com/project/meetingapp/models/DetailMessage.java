package com.project.meetingapp.models;

public class DetailMessage {
    public String user_send;
    public String text;
    public String time;
    public String img;

    public DetailMessage() {

    }

    public DetailMessage(String user_send, String text, String time) {
        this.user_send = user_send;
        this.text = text;
        this.time = time;
    }

    public DetailMessage(String user_send, String text, String time, String img) {
        this.user_send = user_send;
        this.text = text;
        this.time = time;
        this.img = img;
    }

}
