package com.moonslab.sharlet;

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

public class File_selection_adapter extends FragmentStateAdapter {
    Context context;
    int height, width;
    public void set_Context(Context context_target){
        context = context_target;
    }
    public File_selection_adapter(@NonNull FragmentActivity fragmentActivity) {
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
                file_selection_all x = new file_selection_all();
                x.set_Context(context);
                return x;
            case 1:
                app_selection d = new app_selection();
                d.set_Context(context);
                return d;
            case 2:
                file_selection_audio a = new file_selection_audio();
                a.set_Context(context);
                return a;
            case 3:
                file_selection_photo y = new file_selection_photo();
                y.set_Context(context);
                return y;
            case 4:
                file_selection_video z = new file_selection_video();
                z.set_Context(context);
                return z;
            default:
                file_selection_all b = new file_selection_all();
                b.set_Context(context);
                return b;
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
