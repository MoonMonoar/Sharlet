package com.moonslab.sharlet.musiclibrary;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.moonslab.sharlet.fileselector.app_selection;
import com.moonslab.sharlet.fileselector.file_selection_all;
import com.moonslab.sharlet.fileselector.file_selection_audio;
import com.moonslab.sharlet.fileselector.file_selection_photo;
import com.moonslab.sharlet.fileselector.file_selection_video;
import com.moonslab.sharlet.localfiles.media;

public class adapter extends FragmentStateAdapter {
    Context context;
    int height, width;
    public void set_Context(Context context_target){
        context = context_target;
    }
    public adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    public void set_dimen(int screen_height, int screen_width){
        height = screen_height;
        width = screen_width;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                library music_library = new library();
                music_library.set_Context(context);
                music_library.set_dimen(height, width);
                return music_library;
            case 1:
                favourite favourite_list = new favourite();
                favourite_list.set_Context(context);
                favourite_list.set_dimen(height, width);
                return favourite_list;
            default:
                library music_library_duplicate = new library();
                music_library_duplicate.set_Context(context);
                music_library_duplicate.set_dimen(height, width);
                return music_library_duplicate;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
