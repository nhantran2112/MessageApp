package com.project.meetingapp.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alan.alansdk.AlanCallback;
import com.alan.alansdk.AlanConfig;
import com.alan.alansdk.button.AlanButton;
import com.alan.alansdk.events.EventCommand;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.project.meetingapp.R;
import com.project.meetingapp.listener.UsersListener;
import com.project.meetingapp.models.User;
import com.project.meetingapp.ui.chat.ChatRoomActivity;
import com.project.meetingapp.ui.home.adapters.UsersAdapter;
import com.project.meetingapp.ui.invitation.OutgoingInvitationActivity;
import com.project.meetingapp.ui.signin.SignInActivity;
import com.project.meetingapp.utilities.Constants;
import com.project.meetingapp.utilities.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements UsersListener {

    private PreferenceManager preferenceManager;
    private List<User> users;
    private UsersAdapter usersAdapter;
    private TextView textErrorMessage;
    private RecyclerView usersRecyclerview;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView imageConference;
    private MaterialToolbar tbMain;

    private AlanButton alanButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });

        mapping();
        initListener();
        getUsers();
    }

    private void mapping() {
        preferenceManager = new PreferenceManager(getApplicationContext());

        tbMain = findViewById(R.id.tbMain);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        usersRecyclerview = findViewById(R.id.recyclerViewUsers);
        textErrorMessage = findViewById(R.id.textErrorMessage);
        imageConference = findViewById(R.id.imageConference);

        TextView textView = findViewById(R.id.textTitle);
        textView.setText(String.format(
                "%s %s",
                preferenceManager.getString(Constants.KEY_FIRST_NAME),
                preferenceManager.getString(Constants.KEY_LAST_NAME)
        ));
        TextView textFirstChar = findViewById(R.id.textFirstChar);
        textFirstChar.setText(preferenceManager.getString(Constants.KEY_FIRST_NAME).substring(0, 1));

        users = new ArrayList<>();

        usersAdapter = new UsersAdapter(users, this);
        usersRecyclerview.setAdapter(usersAdapter);

        /// Define the project key
        AlanConfig config = AlanConfig.builder().setProjectId(Constants.KEY_ALAN).build();
        alanButton = findViewById(R.id.alan_button);
        alanButton.initWithConfig(config);
    }

    private void initListener() {
        tbMain.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.itemLogout) {
                signOut();
            } else {
                item.getItemId();
            }
            return true;
        });

        swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        //swipe item
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                //swipe left to call
                if (direction == ItemTouchHelper.LEFT) {
                    usersAdapter.notifyItemChanged(pos);
                    initiateAudioMeeting(users.get(pos));
                }
                //swipe right to video call
                else if (direction == ItemTouchHelper.RIGHT) {
                    usersAdapter.notifyItemChanged(pos);
                    initiateVideoMeeting(users.get(pos));
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    //draw layout
                    if (dX < 0) {
                        Paint p = new Paint();
                        p.setColor(Color.parseColor("#FFD300"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background, p);
                        Bitmap icon = drawableToBitmap(getDrawable(R.drawable.ic_audio));
                        float margin = (dX / 5 - width) / 2;
                        RectF iconDest = new RectF((float) itemView.getRight() + margin, (float) itemView.getTop() + width, (float) itemView.getRight() + (margin + width), (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, iconDest, p);
                    }
                    if (dX > 0) {
                        Paint p = new Paint();
                        p.setColor(Color.parseColor("#428BE0"));
                        RectF background = new RectF((float) itemView.getLeft() + dX, (float) itemView.getTop(), (float) itemView.getLeft(), (float) itemView.getBottom());
                        c.drawRect(background, p);
                        Bitmap icon = drawableToBitmap(getDrawable(R.drawable.ic_video));
                        float margin = (dX / 5 - width) / 2;
                        RectF iconDest = new RectF(margin, (float) itemView.getTop() + width, margin + width, (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, iconDest, p);
                    }
                } else {
                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(usersRecyclerview);


        AlanCallback alanCallback = new AlanCallback() {
            /// Handle commands from Alan Studio
            @Override
            public void onCommand(final EventCommand eventCommand) {
                try {
                    JSONObject command = eventCommand.getData().getJSONObject("data");
                    String commandName = command.getString("command");
                    String email = command.getString("email");
                    User userCommand = null;
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).email.equals(email)) {
                            userCommand = users.get(i);
                        }
                    }
                    if (userCommand != null) {
                        switch (commandName) {
                            case "call":
                                initiateAudioMeeting(userCommand);
                                break;
                            case "video":
                                initiateVideoMeeting(userCommand);
                                break;
                            case "mess":
                                chatWithUser(userCommand, null);
                                break;
                            case "send mess":
                                chatWithUser(userCommand, command.getString("message"));
                                break;
                        }
                    }
                } catch (JSONException e) {
                    Timber.tag("DTAG").e(e);
                }
            }

        };

        // Register callbacks
        alanButton.registerCallback(alanCallback);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getUsers() {
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    String myUsersId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        users.clear();
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (myUsersId.equals(documentSnapshot.getId())) {
                                continue;
                            }

                            User user = new User();
                            user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            users.add(user);
                        }

                        if (users.size() > 0) {
                            usersAdapter.notifyDataSetChanged();
                        } else {
                            textErrorMessage.setText(String.format("%s", "No users available"));
                            textErrorMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        textErrorMessage.setText(String.format("%s", "No users available"));
                        textErrorMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void signOut() {
        Toast.makeText(this, "Signing Out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to sign out", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void initiateVideoMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for meeting", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "audio");
            startActivity(intent);
        }
    }

    @Override
    public void chatWithUser(User user, String mess) {
        Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
        intent.putExtra("user", user);
        if (mess != null) {
            intent.putExtra("message", mess);
        }
        startActivity(intent);
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            imageConference.setVisibility(View.VISIBLE);
            imageConference.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                intent.putExtra("selectedUsers", new Gson().toJson(usersAdapter.getSelectedUsers()));
                intent.putExtra("type", "video");
                intent.putExtra("isMultiple", true);
                startActivity(intent);
            });
        } else {
            imageConference.setVisibility(View.GONE);
        }
    }
}