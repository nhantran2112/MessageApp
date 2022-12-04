package com.project.meetingapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.project.meetingapp.R;
import com.project.meetingapp.ui.signin.SignInActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgSplash = findViewById(R.id.imgSplash);
        Glide.with(this)
                .asGif()
                .load(R.raw.splash)
                .error(R.drawable.ic_meet_app)
                .into(imgSplash);

        new Handler().postDelayed(() -> {
            // This method will be executed once the timer is over
            startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            finish();
        }, 3000);
    }
}