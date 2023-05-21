package com.moonslab.sharlet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class file_receive_grid_adapter extends BaseAdapter {
    Context context;
    List<String> paths;
    LayoutInflater inflater;
    String type;
    ImageView image;
    int total_size = 0;
    String receive_path;

    public file_receive_grid_adapter(Context context, String type, List<String> paths) {
        this.context = context;
        List<String> new_list = new ArrayList<String>();
        this.total_size = paths.size();
        if(total_size > 4){
            int x = 0;
            for(String path:paths){
                if(x >= 8){
                    break;
                }
                new_list.add(path);
                x++;
            }
        }
        else {
            new_list = paths;
        }
        this.paths = new_list;
        this.type = type;
    }

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(inflater == null){
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if(convertView == null){
            if(type.equals("Files")){
                convertView = inflater.inflate(R.layout.unknown_grid, null);
            }
            else {
                convertView = inflater.inflate(R.layout.image_grid, null);
                image = convertView.findViewById(R.id.grid_image);
            }
        }
        String path = paths.get(position);
        TextView name = convertView.findViewById(R.id.file_name);
        String file_name = path.substring(path.lastIndexOf("/")+1);
        String File_extension = file_name.substring(file_name.lastIndexOf(".")+1).toLowerCase();
        name.setText(file_name);
        receive_path = Home.get_app_home_directory();
        int pid = 0;
        if(null != image) {
            if (type.equals("Videos")) {
                pid = R.drawable.ic_baseline_video_file_24;
                receive_path += "/Videos";
            } else if (type.equals("Audio")) {
                pid = R.drawable.ic_baseline_audio_file_24;
            } else if (type.equals("Photos")) {
                pid = R.drawable.ic_baseline_photo_24;
                receive_path += "/Photos";
            } else if (type.equals("Apps")) {
                pid = R.drawable.ic_baseline_android_24;
                receive_path += "/Apps";
            }
            try {
                //Play-ables
                if (type.equals("Videos") || type.equals("Photos") || type.equals("Audio")){
                    receive_path+= "/"+file_name;
                    File save_address = new File(receive_path);
                        if(type.equals("Photos")) {
                            Picasso.get().load(save_address).placeholder(pid).resize(250, 250).centerCrop().into(image);
                        }
                        else if(type.equals("Videos")) {
                            Glide.with(context)
                                    .load(save_address)
                                    .placeholder(pid)
                                    .into(image);
                        }
                        else {
                            //Audio
                            try {
                                android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                                mmr.setDataSource(save_address.getPath());
                                byte[] data = mmr.getEmbeddedPicture();
                                if (data != null) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    image.setImageBitmap(bitmap);
                                } else {
                                    image.setImageResource(pid);
                                }
                            }
                            catch (Exception e){
                                image.setImageResource(pid);
                            }
                        }
                }
                else {
                    image.setImageDrawable(ContextCompat.getDrawable(context, pid));
                }
            } catch (Exception e) {
                image.setImageDrawable(ContextCompat.getDrawable(context, pid));
            }
            String finalReceive_path = receive_path;
            convertView.setOnClickListener(v -> {
                File save_address1 = new File(finalReceive_path);
                if(save_address1.isFile()) {
                    if (type.equals("Photos")) {
                        Intent intent = new Intent(context, Photo_view.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //Save the file first
                        store_as_file("Image_last.txt", finalReceive_path, context);
                        context.startActivity(intent);
                    } else if (type.equals("Videos")) {
                        Intent intent = new Intent(context, Video_player.class);
                        store_as_file("Video_last.txt", finalReceive_path, context);
                        context.startActivity(intent);
                    } else if (type.equals("Audio")) {
                        Intent intent = new Intent(context, Music_player.class);
                        //Save the file first
                        //Unset loop
                        store_as_file("Audio_loop.txt", "", context);
                        store_as_file("Audio_last.txt", finalReceive_path, context);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                        Home.openFile(context, save_address1);
                    }
                }
                else {
                    if (type.equals("Audio")) {
                        Toast.makeText(context, "Audio files may not be played from here!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "Wait to receive!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            convertView.setOnLongClickListener(v -> {
                Toast.makeText(context, "Opening...", Toast.LENGTH_SHORT).show();
                Home.openFile(context, new File(finalReceive_path));
                return true;
            });
        }
        else {
            TextView ext_holder = convertView.findViewById(R.id.unknown_file_text);
            if(File_extension.length() > 3){
                File_extension = File_extension.substring(0, 3);
            }
            ext_holder.setText(File_extension);
        }
        //See all
        return convertView;
    }
    private void store_as_file(String file_name, String data , Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Toast.makeText(context, "Error loading file!", Toast.LENGTH_SHORT).show();
        }
    }
}