package com.moonslab.sharlet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Photo_view extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        ImageView image = findViewById(R.id.image);
        TextView image_name = findViewById(R.id.image_name);
        TextView back = findViewById(R.id.back_button);
        String image_path;
        Boolean delete_after = false;

        //Try reading from file
        image_path = read_from_file("Image_last.txt", this);

        //Read bundle
        Bundle b = this.getIntent().getExtras();
        String pre_name = null;

        if(null != b && null != b.getString("pre_name") && !b.getString("pre_name").isEmpty()){
            pre_name = b.getString("pre_name");
        }

        if(null != b && null != b.getString("image_path") && !b.getString("image_path").isEmpty()){
            image_path = b.getString("image_path");
        }

        if(null != b && b.getBoolean("delete_after")){
            delete_after = true;
        }

        //Main
        if(null != image_path){
            File image_file = new File(image_path);
            if(image_file.exists()){
                if(null == pre_name) {
                    image_name.setText(image_file.getName());
                }
                else {
                    image_name.setText(pre_name);
                }
                if(image_file.length() > 10000000){
                    Picasso.get().load(image_file).fit().centerInside().placeholder(R.drawable.ic_baseline_photo_24).into(image);
                    Toast.makeText(getApplicationContext(), "Resized to preview", Toast.LENGTH_SHORT).show();
                }
                else {
                    Bitmap bitmap = BitmapFactory.decodeFile(image_file.getAbsolutePath());
                    image.setImageBitmap(bitmap);
                }
                if(delete_after){
                    image_file.delete();
                }
            }
            else {
                if(!delete_after) {
                    Toast.makeText(getApplicationContext(), "Image corrupted or not found!", Toast.LENGTH_SHORT).show();
                }
                this.finish();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No file to show!", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        back.setOnClickListener(v -> finish());
    }
    //Helpers
    private String read_from_file(String file_name, Context context) {
        try {
            String location = context.getFilesDir().getAbsolutePath()+"/"+file_name;
            return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            return null;
        }
    }
}