package com.moonslab.sharlet.musiclibrary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.Music_player;
import com.moonslab.sharlet.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class library extends Fragment {
    private Context context;
    private View main_view;
    private DBHandler dbHandler;
    private int height, width;
    RelativeLayout last_player_tag = null;
    public void set_Context(Context context_target){context = context_target;}
    public void set_dimen(int height_screen, int width_screen){
        height = height_screen;
        width = width_screen;
    }
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        main_view = inflater.inflate(R.layout.fragment_library, container, false);
        dbHandler = new DBHandler(context);
        TableLayout table = main_view.findViewById(R.id.table);
        LinearLayout empty = main_view.findViewById(R.id.empty);
        ViewGroup.LayoutParams params = empty.getLayoutParams();
        params.height = height - 600;
        params.width = width;
        empty.setLayoutParams(params);
        //Load music files - concurrent
        new Thread(()->{
            List<File> files = All_files(context);
            if(null == files || files.size() == 0){
               //Empty
               main_view.post(()->{
                   TextView icon = empty.findViewById(R.id.icon),
                            text = empty.findViewById(R.id.text);
                   icon.setText("\\\uf65d");
                   text.setText("No audio files!");
               });
               return;
            }
            boolean check = false;
            for(File target_file:files){
             if(target_file.exists()){
                 dbHandler.add_new_music(target_file.getPath());
                 View child = inflater.inflate(R.layout.audio_library_child, null);
                 TextView name = child.findViewById(R.id.file_name);
                 TextView info = child.findViewById(R.id.file_info);
                 ImageView image = child.findViewById(R.id.file_image);
                 RelativeLayout is_playing_tag = child.findViewById(R.id.playing_tag),
                                fav_tag = child.findViewById(R.id.fav_tag);
                 //Check last playing path
                 String last_path = dbHandler.get_settings("last_music_path");
                 if(last_path != null && last_path.equals(target_file.getPath())){
                  is_playing_tag.setVisibility(View.VISIBLE);
                  last_player_tag = is_playing_tag;
                 }
                 Boolean fav_check = dbHandler.fav_exists(target_file.getPath());
                 if(fav_check){
                     fav_tag.setVisibility(View.VISIBLE);
                 }
                 //Try getting the cover
                 try {
                     android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                     mmr.setDataSource(target_file.getPath());
                     byte[] data = mmr.getEmbeddedPicture();
                     if (data != null) {
                         Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                         Drawable m = new BitmapDrawable(getResources(), bitmap);
                         image.setImageDrawable(m);
                         image.setBackground(null);
                     }
                 }
                 catch (Exception e){
                     //Do nothing
                 }
                 name.setText(target_file.getName());
                 info.setText(folderFromPath(target_file.getPath(), target_file.getName())+" - "+ Home.convertTime(target_file.lastModified()));
                 if(!check){
                     main_view.post(() ->table.removeAllViews());
                     check = true;
                 }
                 //Events
                 child.setOnClickListener(v -> {
                     dbHandler.add_setting("last_music_path", target_file.getPath());
                     main_view.post(() -> {
                             if(last_player_tag != null) {
                                 last_player_tag.setVisibility(View.GONE);
                             }
                             is_playing_tag.setVisibility(View.VISIBLE);
                             last_player_tag = is_playing_tag;
                         });
                         Intent i = new Intent(context, Music_player.class);
                         Bundle b = new Bundle();
                         b.putString("start_new", "1");
                         i.putExtras(b);
                         startActivity(i);
                    });

                 main_view.post(() -> table.addView(child));
             }
            }
        }).start();
        return main_view;
    }
    //Passive codes
    public List<File> All_files(Context context) {
        List<File> files = new ArrayList<>();
        List<String> file_paths = new ArrayList<>();
        try {
            final String[] columns = {
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED
            };
            //Sort
            String sort_audio = MediaStore.Audio.Media.DATE_ADDED + " DESC";

            //Query sets
            MergeCursor cursor;
            cursor = new MergeCursor(new Cursor[]{
                    context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, null, null, sort_audio)
            });

            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                int lastPoint = path.lastIndexOf(".");
                path = path.substring(0, lastPoint) + path.substring(lastPoint).toLowerCase();
                files.add(new File(path));
                file_paths.add(path);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            //ERROR GETTING FILES
            //Handle
            return null;
        }
        return files;
    }
    public static String folderFromPath(String path, String name){
        String r_str = "", r_n_str = "";
        char ch;
        path = path.substring(0, path.indexOf(name)-1);
        for (int i=0; i< path.length(); i++)
        {
            ch = path.charAt(i);
            r_str = ch+r_str;
        }
        r_str = r_str.substring(0, r_str.indexOf("/"));
        int i = r_str.length()-1;
        while(i >= 0){
            ch = r_str.charAt(i);
            r_n_str = r_n_str+ch;
            i--;
        }
        return r_n_str;
    }
}