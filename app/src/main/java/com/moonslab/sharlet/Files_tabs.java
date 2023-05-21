package com.moonslab.sharlet;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.moonslab.sharlet.fileselector.app_selection;
import com.moonslab.sharlet.fileselector.file_selection_all;
import com.moonslab.sharlet.localfiles.media;

public class Files_tabs extends FragmentStateAdapter {
    Context context;
    int height, width;
    public void set_Context(Context context_target){
        context = context_target;
    }
    public Files_tabs(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    public void set_dimen(int screen_height, int screen_width){
        height = screen_height;
        width = screen_width;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Home.loading_state.setVisibility(View.INVISIBLE);
        switch (position){
            case 0:
                file_selection_all x = new file_selection_all();
                x.set_Context(context);
                x.is_from_home(true);
                return x;
            case 1:
                app_selection d = new app_selection();
                d.set_Context(context);
                d.set_home(true);
                return d;
            case 2:
                media media_tab = new media();
                media_tab.set_dimen(height, width);
                media_tab.set_Context(context);
                return media_tab;
            case 3:
                media media_tab1 = new media();
                media_tab1.set_Context(context);
                media_tab1.set_dimen(height, width);
                media_tab1.set_media_mood("Video");
                return media_tab1;
            case 4:
                media media_tab2 = new media();
                media_tab2.set_Context(context);
                media_tab2.set_dimen(height, width);
                media_tab2.set_media_mood("Audio");
                return media_tab2;
            case 5:
                media media_tab3 = new media();
                media_tab3.set_Context(context);
                media_tab3.set_dimen(height, width);
                media_tab3.set_media_mood("Docs");
                return media_tab3;
            default:
                file_selection_all x2 = new file_selection_all();
                x2.set_Context(context);
                return x2;
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }
}
