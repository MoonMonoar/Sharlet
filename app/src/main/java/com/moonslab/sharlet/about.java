package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

public class about extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView back = findViewById(R.id.back_button);
        back.setOnClickListener(v-> finish());
        Button get_in = findViewById(R.id.get_in);
        get_in.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://moonmonoar.github.io/portfolio/"));
            startActivity(browserIntent);
        });
        ImageView image = findViewById(R.id.moon);
        InputStream imageStream = this.getResources().openRawResource(R.raw.creator_photo);
        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
        bitmap = Home.getCroppedBitmap(bitmap);
        image.setImageBitmap(bitmap);
    }
}