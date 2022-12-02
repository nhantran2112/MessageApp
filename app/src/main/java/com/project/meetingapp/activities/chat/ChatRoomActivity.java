package com.project.meetingapp.activities.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.meetingapp.R;
import com.project.meetingapp.activities.OutgoingInvitationActivity;
import com.project.meetingapp.models.ChatRoom;
import com.project.meetingapp.models.User;
import com.project.meetingapp.utilities.Constants;
import com.project.meetingapp.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ChatRoomActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private Button btnSend;
    private EditText edtInput;
    private RecyclerView rvMess;
    private TextView tvNotifi;

    private MessRVAdapter messRVAdapter;
    private List<DetailMessage> messageList;

    private String text;
    private User user;
    private int id_room = 0;

    private DatabaseReference mDatabase;
    private PreferenceManager preferenceManager;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        mapping();
        user = (User) getIntent().getSerializableExtra("user");
        email = preferenceManager.getString(Constants.KEY_EMAIL);
        CheckNewOrOldChat();

        initListener();


        messRVAdapter = new MessRVAdapter(messageList, this, R.layout.item_message);
        rvMess.setAdapter(messRVAdapter);

        toolbar.setTitle(user.lastName + user.firstName);
    }

    private void mapping() {
        toolbar = findViewById(R.id.tbChatRoom);
        btnSend = findViewById(R.id.btnSend);
        edtInput = findViewById(R.id.edtInput);
        rvMess = findViewById(R.id.rvMess);
        tvNotifi = findViewById(R.id.tvNotifi);
        messageList = new ArrayList<>();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        preferenceManager = new PreferenceManager(getApplicationContext());

    }

    private void initListener() {
        toolbar.setNavigationOnClickListener(view -> finish());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.itemCall) {
                if (user.token == null || user.token.trim().isEmpty()) {
                    Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("user", user);
                    intent.putExtra("type", "video");
                    startActivity(intent);
                }
            } else if (item.getItemId() == R.id.itemVideo) {
                if (user.token == null || user.token.trim().isEmpty()) {
                    Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("user", user);
                    intent.putExtra("type", "audio");
                    startActivity(intent);
                }
            }
            return false;
        });


        btnSend.setOnClickListener(v -> {
            text = edtInput.getText().toString();
            if (text.length() > 0) {
                Long date = new Date().getTime();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat ft1 = new SimpleDateFormat(Constants.DD_MM_YYYY_HH_MM_SS);
                DetailMessage message = new DetailMessage(email, text, ft1.format(date));
                if (id_room == 0) {
                    Random random = new Random();
                    id_room = random.nextInt(9999);
                    ChatRoom chatRoom = new ChatRoom(id_room, email, user.email);
                    //up message to firebase
                    mDatabase.child(Constants.CHAT_ROOM + "/" + id_room).setValue(chatRoom);
                }
                mDatabase.child(Constants.CHAT_ROOM + "/" + id_room + "/" + Constants.MESSAGE + "/" + date).setValue(message).addOnSuccessListener(unused -> {
                    edtInput.setText("");
                    text = null;
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void CheckNewOrOldChat() {
        mDatabase.child(Constants.CHAT_ROOM).orderByChild(Constants.USER1).equalTo(email).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null && user.email.equals(chatRoom.user2)) {
                    id_room = chatRoom.id_room;
                    getOldMess();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mDatabase.child(Constants.CHAT_ROOM).orderByChild(Constants.USER2).equalTo(email).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null && user.email.equals(chatRoom.user1)) {
                    id_room = chatRoom.id_room;
                    getOldMess();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getOldMess() {
        mDatabase.child(Constants.CHAT_ROOM + "/" + id_room + "/" + Constants.MESSAGE).orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                messageList.add(snapshot.getValue(DetailMessage.class));
                tvNotifi.setVisibility(View.GONE);
                messRVAdapter.notifyItemInserted(messageList.size() - 1);
                rvMess.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }
}