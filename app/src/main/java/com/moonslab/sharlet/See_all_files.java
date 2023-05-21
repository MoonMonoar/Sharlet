package com.moonslab.sharlet;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class See_all_files extends AppCompatActivity {
    TableLayout files_table;
    ArrayList<String> files_paths_to_pass;
    List<File> file_list;
    Context context;
    boolean no_selection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all_files);;
        context = this;
        TextView folder_name = (TextView) findViewById(R.id.folder_name);
        TextView back_button = (TextView) findViewById(R.id.back_button);
        files_table = findViewById(R.id.files_table);
        //The incoming data will be folder path
        Bundle b = this.getIntent().getExtras();
        files_paths_to_pass = b.getStringArrayList("file_paths");
        String target_folder_name = b.getString("folder_name");
        no_selection = b.getBoolean("no_selection");
        if (null != target_folder_name && null != files_paths_to_pass) {
            folder_name.setText("📁 " + target_folder_name);
            file_list = new ArrayList<>();
            for(String path : files_paths_to_pass){
                file_list.add(new File(path));
            }

            new Thread(() -> {
                for (int i = 0; i < file_list.size(); i++) {
                    File file = file_list.get(i);
                    if (null == file || !file.exists()) {
                        continue;
                    }
                    LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                    String File_name = file.getName();
                    String File_extension = FilenameUtils.getExtension(File_name).toLowerCase(Locale.ROOT);
                    View view = inflater.inflate(R.layout.all_files_child, null);
                    if (null == File_extension) {
                        view = inflater.inflate(R.layout.all_files_child_unknown, null);
                    }
                    ImageView image;

                    image = view.findViewById(R.id.grid_image);
                    TextView name = view.findViewById(R.id.file_name);
                    File target_file = file;
                    name.setText(target_file.getName());
                    if (File_extension.equals("png")
                            || File_extension.equals("jpg")
                            || File_extension.equals("gif")
                            || File_extension.equals("jpeg")
                            || File_extension.equals("heic")
                            || File_extension.equals("webp")
                            || File_extension.equals("tiff")
                            || File_extension.equals("raw")) {
                        TextView info = view.findViewById(R.id.file_info);
                        info.setText("🖼️ Image file");
                        if (!no_selection) {
                            view.setOnLongClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Photo_view.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                //Save the file first
                                store_as_file("Image_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                                return true;
                            });
                        } else {
                            view.setOnClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Photo_view.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                //Save the file first
                                store_as_file("Image_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                            });
                        }
                        try {
                            runOnUiThread(() -> Picasso.get().load(target_file).placeholder(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(image));
                        } catch (Exception e) {
                            runOnUiThread(() -> Picasso.get().load(R.drawable.ic_baseline_photo_24).resize(250, 250).centerCrop().into(image));
                        }
                    } else if (File_extension.equals("mp3")
                            || File_extension.equals("wav")
                            || File_extension.equals("ogg")
                            || File_extension.equals("m4a")
                            || File_extension.equals("aac")
                            || File_extension.equals("alac")
                            || File_extension.equals("aiff")) {
                        TextView info = view.findViewById(R.id.file_info);
                        info.setText("🎵 Audio file");
                        if (!no_selection) {
                            view.setOnLongClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Music_player.class);
                                //Save the file first
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                store_as_file("Audio_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                                return true;
                            });
                        } else {
                            view.setOnClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Music_player.class);
                                //Save the file first
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                store_as_file("Audio_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                            });
                        }
                        runOnUiThread(() -> Picasso.get().load(R.drawable.ic_baseline_queue_music_24).placeholder(R.drawable.ic_baseline_queue_music_24).resize(200, 200).centerCrop().into(image));
                    } else if (File_extension.equals("mp4")
                            || File_extension.equals("mkv")
                            || File_extension.equals("flv")
                            || File_extension.equals("avi")
                            || File_extension.equals("webm")
                            || File_extension.equals("mov")) {

                        TextView info = view.findViewById(R.id.file_info);
                        info.setText("📽️ Video file");
                        if (!no_selection) {
                            view.setOnLongClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Video_player.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                store_as_file("Video_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                                return true;
                            });
                        } else {
                            view.setOnClickListener(v -> {
                                Intent intent = new Intent(getApplicationContext(), Video_player.class);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                store_as_file("Video_last.txt", target_file.getPath(), getApplicationContext());
                                getApplicationContext().startActivity(intent);
                            });
                        }
                        try {
                            runOnUiThread(() -> Glide.with(getApplicationContext())
                                    .load(target_file)
                                    .placeholder(R.drawable.ic_baseline_video_file_24)
                                    .into(image));
                        } catch (Exception e) {
                            runOnUiThread(() -> Picasso.get().load(R.drawable.ic_baseline_video_file_24).resize(250, 250).centerCrop().into(image));
                        }
                    } else {

                        View view2 = inflater.inflate(R.layout.all_files_child_unknown, null);

                        TextView info = view.findViewById(R.id.file_info);
                        info.setText("❔ " + File_extension.toUpperCase(Locale.ROOT) + " file");

                        TextView ext_holder = (TextView) view2.findViewById(R.id.unknown_file_text);
                        if (File_extension.length() > 3) {
                            File_extension = File_extension.substring(0, 3);
                        }
                        ext_holder.setText(File_extension);
                        TextView name_x = view.findViewById(R.id.file_name);
                        name_x.setText(File_name);
                        if (!no_selection) {
                            view2.setOnLongClickListener(v -> {
                                Toast.makeText(getApplicationContext(), "Opening...", Toast.LENGTH_SHORT).show();
                                Home.openFile(context, file);
                                return true;
                            });
                        } else {
                            view2.setOnClickListener(v -> {
                                Toast.makeText(getApplicationContext(), "Opening...", Toast.LENGTH_SHORT).show();
                                Home.openFile(context, file);
                            });
                        }
                        view = view2;
                    }

                    if (no_selection) {
                        view.setOnLongClickListener(v -> {
                            Toast.makeText(getApplicationContext(), "Opening...", Toast.LENGTH_SHORT).show();
                            Home.openFile(context, file);
                            return false;
                        });
                    }

                    if (!no_selection) {
                        View finalConvertView = view;
                        view.setOnClickListener(v -> {
                            //Read the bucket
                            List<String> bucket_list = new ArrayList<>();
                            String location = Home.get_app_home_bundle_data_store() + "/Selection_bucket.txt";
                            File main_file = new File(location);
                            if (!main_file.exists()) {
                                try {
                                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                                    outputStreamWriter.write("");
                                    outputStreamWriter.close();
                                } catch (Exception e) {
                                    Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            try {
                                BufferedReader reader = new BufferedReader(new FileReader(main_file));
                                String line = reader.readLine();
                                while (line != null) {
                                    bucket_list.add(line);
                                    // read next line
                                    line = reader.readLine();
                                }
                                reader.close();
                            } catch (IOException e) {
                                bucket_list = null;
                            }

                            Boolean selected_already = false;
                            //Not null, so look for the file
                            //if exists, its selected
                            for (String path : bucket_list) {
                                String path0 = target_file.getPath();
                                String home = "/storage";
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    home = Environment.getStorageDirectory().getPath();
                                }
                                path0 = path0.substring(home.length());
                                if (path.equals(path0)) {
                                    selected_already = true;
                                    break;
                                }
                            }

                            LinearLayout main_layout = finalConvertView.findViewById(R.id.main_layout);

                            if (!selected_already) {
                                main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                                //Add to the list convert to text and save
                                String path0 = target_file.getPath();
                                String home = "/storage";
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    home = Environment.getStorageDirectory().getPath();
                                }
                                path0 = path0.substring(home.length());
                                bucket_list.add(path0);
                                //New bucket
                                String new_data = null;
                                for (String path : bucket_list) {
                                    if (path.equals("empty")) {
                                        continue;
                                    }
                                    if (null == new_data) {
                                        new_data = path;
                                    } else {
                                        new_data += System.lineSeparator() + path;
                                    }
                                }
                                if (null != new_data) {
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    } catch (Exception e) {
                                        Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                main_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                                if (bucket_list.size() == 1) {
                                    //Empty and return
                                    main_file.delete();
                                    File_selection.selection_update();
                                    return;
                                }
                                //New bucket
                                String new_data = null;
                                for (String path : bucket_list) {
                                    String path0 = target_file.getPath();
                                    String home = "/storage";
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        home = Environment.getStorageDirectory().getPath();
                                    }
                                    path0 = path0.substring(home.length());
                                    if (path.equals("empty") || path.equals(path0)) {
                                        continue;
                                    }
                                    if (null == new_data) {
                                        new_data = path;
                                    } else {
                                        new_data += System.lineSeparator() + path;
                                    }
                                }
                                if (null != new_data) {
                                    //Save the string
                                    try {
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                                        outputStreamWriter.write(new_data);
                                        outputStreamWriter.close();
                                    } catch (Exception e) {
                                        Toast.makeText(context, "Can't unselect!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            File_selection.selection_update();
                        });
                    }

                    //Pre check
                    List<String> bucket_list = new ArrayList<>();
                    String location = Home.get_app_home_bundle_data_store() + "/Selection_bucket.txt";
                    File main_file = new File(location);
                    if (!main_file.exists()) {
                        try {
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(main_file));
                            outputStreamWriter.write("");
                            outputStreamWriter.close();
                        } catch (Exception e) {
                            Toast.makeText(context, "Can't select!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(main_file));
                        String line = reader.readLine();
                        while (line != null) {
                            bucket_list.add(line);
                            // read next line
                            line = reader.readLine();
                        }
                        reader.close();
                    } catch (IOException e) {
                        bucket_list = null;
                    }

                    Boolean selected_already = false;
                    //Not null, so look for the file
                    //if exists, its selected
                    for (String path : bucket_list) {
                        String path0 = target_file.getPath();
                        String home = "/storage";
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            home = Environment.getStorageDirectory().getPath();
                        }
                        path0 = path0.substring(home.length());
                        if (path.equals(path0)) {
                            selected_already = true;
                            break;
                        }
                    }
                    if (selected_already) {
                        view.setBackgroundColor(ContextCompat.getColor(context, R.color.selection));
                    }
                    View finalView = view;
                    runOnUiThread(() -> files_table.addView(finalView));
                }
            }).start();

        } else {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        back_button.setOnClickListener(v -> finish());
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