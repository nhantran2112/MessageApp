package com.project.meetingapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.project.meetingapp.R;
import com.project.meetingapp.models.User;

public class ChatRoomActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private Button btnSend;
    private EditText edtInput;
    private RecyclerView rvMess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        mapping();
        initListener();

        User user = (User) getIntent().getSerializableExtra("user");


        toolbar.setTitle(user.lastName + user.firstName);
    }

    private void mapping(){
        toolbar = findViewById(R.id.tbChatRoom);
        btnSend = findViewById(R.id.btnSend);
        edtInput = findViewById(R.id.edtInput);
        rvMess = findViewById(R.id.rvMess);
    }

    private void initListener(){
        toolbar.setNavigationOnClickListener(view -> {
            finish();
        });

        btnSend.setOnClickListener(view -> {
            //todo
        });
    }
}