package com.project.meetingapp.ui.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.meetingapp.R;
import com.project.meetingapp.models.ChatRoom;
import com.project.meetingapp.models.DetailMessage;
import com.project.meetingapp.models.User;
import com.project.meetingapp.ui.chat.adapter.MessRVAdapter;
import com.project.meetingapp.ui.invitation.OutgoingInvitationActivity;
import com.project.meetingapp.utilities.Constants;
import com.project.meetingapp.utilities.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ChatRoomActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private Button btnSend, btnPickImg, btnClose;
    private EditText edtInput;
    private RecyclerView rvMess;
    private TextView tvNotifi;
    private ImageView imgPick;

    private MessRVAdapter messRVAdapter;
    private List<DetailMessage> messageList;

    private String text;
    private User user;
    private String imgUrl;
    private SimpleDateFormat format;
    private int id_room = 0;

    private DatabaseReference mDatabase;
    private StorageReference storageRef;

    private PreferenceManager preferenceManager;
    private String email;
    private String autoMess;

    private Uri filePath;

    private final int PICK_IMAGE_REQUEST = 9999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        mapping();
        initData();
        CheckNewOrOldChat();
        initListener();
    }

    @SuppressLint("SimpleDateFormat")
    private void mapping() {
        toolbar = findViewById(R.id.tbChatRoom);
        btnSend = findViewById(R.id.btnSend);
        btnPickImg = findViewById(R.id.btnPickImg);
        btnClose = findViewById(R.id.btnClose);
        imgPick = findViewById(R.id.imgPick);
        edtInput = findViewById(R.id.edtInput);
        rvMess = findViewById(R.id.rvMess);
        tvNotifi = findViewById(R.id.tvNotifi);
        messageList = new ArrayList<>();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        preferenceManager = new PreferenceManager(getApplicationContext());

        format = new SimpleDateFormat(Constants.DD_MM_YYYY_HH_MM_SS);
    }

    private void initData() {
        user = (User) getIntent().getSerializableExtra("user");
        autoMess = getIntent().getStringExtra("message");

        email = preferenceManager.getString(Constants.KEY_EMAIL);

        messRVAdapter = new MessRVAdapter(messageList, R.layout.item_message, email);
        rvMess.setAdapter(messRVAdapter);

        toolbar.setTitle(user.firstName + " " + user.lastName);
    }

    private void initListener() {
        if (autoMess != null) {
            edtInput.setText(autoMess);
        }

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
                if (id_room == 0) {
                    Random random = new Random();
                    id_room = random.nextInt(9999);
                    ChatRoom chatRoom = new ChatRoom(id_room, email, user.email);
                    //up message to firebase
                    mDatabase.child(Constants.CHAT_ROOM + "/" + id_room).setValue(chatRoom);
                }
                //upload image
                //Insert avatar storage
                long date = new Date().getTime();
                if (filePath != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                        // aiming for ~500kb max. assumes typical device image size is around 2megs
                        int scaleDivider = 5;

                        int widthImg = bitmap.getWidth();
                        if (widthImg < 1000) {
                            scaleDivider = 1;
                        } else if (widthImg < 2000) {
                            scaleDivider = 2;
                        } else if (widthImg < 4000) {
                            scaleDivider = 4;
                        }
                        // 2. Get the downsized image content as a byte[]
                        int scaleWidth = bitmap.getWidth() / scaleDivider;
                        int scaleHeight = bitmap.getHeight() / scaleDivider;
                        byte[] downsizedImageBytes =
                                getDownsizedImageBytes(bitmap, scaleWidth, scaleHeight);
                        UploadTask uploadTask = storageRef.child("chatroom/image/" + id_room + "/" + date).putBytes(downsizedImageBytes);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            storageRef.child("chatroom/image/" + id_room + "/" + date).getDownloadUrl().addOnSuccessListener(uri -> {
                                imgUrl = uri.toString();
                                DetailMessage message;
                                message = new DetailMessage(email, text, format.format(date), imgUrl);
                                uploadMess(message, date);

                            }).addOnFailureListener(e -> {
                                //todo
                            });

                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    DetailMessage message = new DetailMessage(email, text, format.format(date));
                    uploadMess(message, date);
                }
            }
        });

        btnPickImg.setOnClickListener(v -> {
            chooseImage();
        });

        btnClose.setOnClickListener(v -> {
            imgPick.setImageBitmap(null);
            imgPick.setVisibility(View.GONE);
            filePath = null;
        });
    }

    private void uploadMess(DetailMessage message, long date) {
        mDatabase.child(Constants.CHAT_ROOM + "/" + id_room + "/" + Constants.MESSAGE + "/" + date).setValue(message).addOnSuccessListener(unused -> {
            edtInput.setText("");
            text = null;
            imgPick.setImageBitmap(null);
            imgPick.setVisibility(View.GONE);
            filePath = null;
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imgPick.setImageBitmap(bitmap);
                imgPick.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] getDownsizedImageBytes(Bitmap fullBitmap, int scaleWidth, int scaleHeight) throws IOException {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, scaleWidth, scaleHeight, true);
        // 2. Instantiate the downsized image content as a byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
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