package com.moonslab.sharlet.musiclibrary;

import static com.moonslab.sharlet.musiclibrary.library.folderFromPath;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.moonslab.sharlet.DBHandler;
import com.moonslab.sharlet.Home;
import com.moonslab.sharlet.Music_player;
import com.moonslab.sharlet.R;

import java.io.File;
import java.util.List;

public class favourite extends Fragment {
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
        main_view = inflater.inflate(R.layout.fragment_favourite, container, false);
        dbHandler = new DBHandler(context);
        TableLayout table = main_view.findViewById(R.id.table);
        LinearLayout empty = main_view.findViewById(R.id.empty);
        ViewGroup.LayoutParams params = empty.getLayoutParams();
        params.height = height - 700;
        params.width = width;
        empty.setLayoutParams(params);
        //Load music files - concurrent
        new Thread(()->{
            List<String> files = dbHandler.get_fav_list();
            if(null == files || files.size() == 0){
                //Empty
                main_view.post(()->{
                    TextView icon = empty.findViewById(R.id.icon),
                            text = empty.findViewById(R.id.text);
                    icon.setText("\\\uf004");
                    text.setText("Tap heart to add");
                });
                return;
            }
            boolean check = false;
            for(String target_file_path: files){
                File target_file = new File(target_file_path);
                if(target_file.exists()){
                    View child = inflater.inflate(R.layout.audio_library_child, null);
                    TextView name = child.findViewById(R.id.file_name);
                    TextView info = child.findViewById(R.id.file_info);
                    ImageView image = child.findViewById(R.id.file_image);
                    RelativeLayout is_playing_tag = child.findViewById(R.id.playing_tag),
                                    fav_tag = child.findViewById(R.id.fav_tag);;
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
                    android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(target_file.getPath());
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Drawable m = new BitmapDrawable(getResources(), bitmap);
                        image.setImageDrawable(m);
                        image.setBackground(null);
                    }
                    name.setText(target_file.getName());
                    info.setText(folderFromPath(target_file.getPath(), target_file.getName())+" - "+ Home.convertTime(target_file.lastModified()));
                    if(!check){
                        main_view.post(table::removeAllViews);
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
}