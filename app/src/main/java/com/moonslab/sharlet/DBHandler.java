package com.moonslab.sharlet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.moonslab.sharlet.objects.fileOBJ;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "SharletDB";
    private static final int DB_VERSION = 6;

    //Default tables
    //Table -- history
    private static final String HISTORY_TABLE_NAME = "transfer_history";
    //Columns
    //id
    private static final String ID_COL = "id";
    //Portal_id
    private static final String ID_PORTAL = "portal_id";
    //File path
    private static final String PATH_COL = "path";
    //Size(Bytes)
    private static final String SIZE_COL = "size";
    //timestamp(millis)
    private static final String TIME_COL = "send_time";
    //Send method(Android/Http)
    private static final String METHOD_COL = "send_method";
    //Receiver data(Receiver name, ip address etc)
    private static final String INFO_COL = "receiver_info";
    //Table -- history -- ends

    //Default tables
    //User settings
    private final String SETTINGS_TABLE_NAME = "user_settings";
    private final String SETTINGS_ID_COL = "id";
    //SETTING NAME
    private final String SETTINGS_NAME_COL = "name";
    //SETTING VALUE
    private final String SETTINGS_VAL_COL = "value";

    //Default tables
    //User profile
    private final String PROFILE_TABLE_NAME = "user_profile";
    private final String PROFILE_ID_COL = "id";
    //Data name
    //Data value
    private final String PROFILE_DATA_NAME = "name";
    private final String PROFILE_DATA_VAL = "value";

    //Music library paths
    private final String MUSIC_TABLE_NAME = "music_list";
    private final String MUSIC_ID_COL = "id";
    private final String MUSIC_PATH = "path";
    private final String MUSIC_ADDED_TIME = "time";

    //Favourite music paths
    private final String FAV_MUSIC_TABLE_NAME = "favourite_music";
    private final String FAV_MUSIC_ID_COL = "id";
    private final String FAV_MUSIC_PATH = "path";
    private final String FAV_MUSIC_ADDED_TIME = "time";

    //Incoming file database
    private final String INC_NAME = "incoming";
    private final String INC_ID = "id";
    private final String INC_FILE = "file";
    private final String INC_LINK = "link";
    private final String INC_PASS = "pass";
    private final String INC_STATUS = "status"; //0 MEANS PENDING, 1 MEANS RECEIVED

    //Constructor
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase db) {
        //HISTORY TABLE
        String query = "CREATE TABLE " + HISTORY_TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PATH_COL + " TEXT,"
                + INFO_COL + " TEXT,"
                + ID_PORTAL+ " TEXT,"
                + METHOD_COL + " TEXT,"
                + SIZE_COL + " TEXT,"
                + TIME_COL + " TEXT)";
        db.execSQL(query);

        //USER SETTINGS TABLE
        String query2 = "CREATE TABLE " + SETTINGS_TABLE_NAME + " ("
                + SETTINGS_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SETTINGS_NAME_COL + " TEXT,"
                + SETTINGS_VAL_COL + " TEXT)";
        db.execSQL(query2);

        //PROFILE
        String query3 = "CREATE TABLE " + PROFILE_TABLE_NAME + " ("
                + PROFILE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PROFILE_DATA_NAME + " TEXT,"
                + PROFILE_DATA_VAL + " TEXT)";
        db.execSQL(query3);

        //FAV - MUSIC
        String query4 = "CREATE TABLE " + FAV_MUSIC_TABLE_NAME + " ("
                + FAV_MUSIC_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FAV_MUSIC_PATH + " TEXT,"
                + FAV_MUSIC_ADDED_TIME + " TEXT)";
        db.execSQL(query4);

        //LIBRARY - MUSIC
        String query5 = "CREATE TABLE " + MUSIC_TABLE_NAME + " ("
                + MUSIC_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MUSIC_PATH + " TEXT,"
                + MUSIC_ADDED_TIME + " TEXT)";
        db.execSQL(query5);

        //INCOMING FILES
        String query6 = "CREATE TABLE " + INC_NAME + " ("
                + INC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + INC_FILE + " TEXT,"
                + INC_LINK + " TEXT,"
                + INC_PASS + " TEXT,"
                + INC_STATUS + " INTEGER)";
        db.execSQL(query6);

        //KNOWN DEVICE IPS
        String query7 = "CREATE TABLE known_ip (id INTEGER PRIMARY KEY AUTOINCREMENT, ip TEXT)";
        db.execSQL(query7);
    }

    //DB METHODS
    public Boolean add_new_file_history(String portal_id, String path, String size, String timestamp, String info, String method) {
        try {
            //DB
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            //Add values
            values.put(ID_PORTAL, portal_id);
            values.put(PATH_COL, path);
            values.put(INFO_COL, info);
            values.put(METHOD_COL, method);
            values.put(SIZE_COL, size);
            values.put(TIME_COL, timestamp);

            //Pass
            db.insert(HISTORY_TABLE_NAME, null, values);
            db.close();
            return true;
        }
        catch (Exception e){
         return false;
        }
    }

    public Boolean add_new_fav_music(String path){
        try {
            //DB
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            //Add values
            values.put(FAV_MUSIC_PATH, path);
            values.put(FAV_MUSIC_ADDED_TIME, Long.toString(Home.get_timestamp()));

            //Pass
            if(!fav_exists(path)) {
                db.insert(FAV_MUSIC_TABLE_NAME, null, values);
                db.close();
            }

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public Boolean add_new_music(String path) {
        try {
            //DB
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            //Add values
            values.put(MUSIC_PATH, path);
            values.put(MUSIC_ADDED_TIME, Long.toString(Home.get_timestamp()));

            //Pass
            if(!music_exists(path)) {
                db.insert(MUSIC_TABLE_NAME, null, values);
                db.close();
            }
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public Boolean music_exists(String path){
        try {
            Boolean result = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_ID_COL + " FROM " + MUSIC_TABLE_NAME + " WHERE " + MUSIC_PATH + " = ?", new String[]{path});
            if (cursor.moveToFirst()) {
                do {
                    result = true;
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return false;
        }
    }

    public int get_music_id_by_path(String path){
        try {
            int result = 0;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_ID_COL + " FROM " + MUSIC_TABLE_NAME + " WHERE " + MUSIC_PATH + " = ?", new String[]{path});
            if (cursor.moveToFirst()) {
                do {
                    result = Integer.parseInt(cursor.getString(0));
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return 0;
        }
    }

    public String get_music_path_by_id(int id){
        try {
            String result = null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_PATH + " FROM " + MUSIC_TABLE_NAME + " WHERE " + MUSIC_ID_COL + " = ?", new String[]{Integer.toString(id)});
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(0);
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public int get_music_count(){
        int count = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_ID_COL + " FROM " + MUSIC_TABLE_NAME, null);
            if (cursor.moveToFirst()) {
                do {
                    count++;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return count;
        }
        catch (Exception e){
            return count;
        }
    }

    public void remove_history_file(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, ID_COL+" = "+id, null);
        db.close();
    }

    public void remove_history_file_batch(Integer[] set){
        for(Integer id: set) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(HISTORY_TABLE_NAME, ID_COL + " = " + id, null);
            db.close();
        }
    }

    public String get_music_path_random(String old_path){
        try {
            String result = null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_PATH + " FROM " + MUSIC_TABLE_NAME + " WHERE "+MUSIC_PATH+" != ? ORDER BY RANDOM() LIMIT 1", new String[]{old_path});
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(0);
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public String get_next_music_path(Integer current_id, Boolean get_previous){
        try {
            String result = null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor;
            if(get_previous){
                cursor = db.rawQuery("SELECT " + MUSIC_PATH + " FROM " + MUSIC_TABLE_NAME + " WHERE " + MUSIC_ID_COL + " < ? ORDER BY "+MUSIC_ID_COL+" DESC LIMIT 1", new String[]{Integer.toString(current_id)});
            }
            else {
                 cursor = db.rawQuery("SELECT " + MUSIC_PATH + " FROM " + MUSIC_TABLE_NAME + " WHERE " + MUSIC_ID_COL + " > ? LIMIT 1", new String[]{Integer.toString(current_id)});
            }
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(0);
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public Boolean fav_exists(String path){
        try {
            Boolean result = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + FAV_MUSIC_ID_COL + " FROM " + FAV_MUSIC_TABLE_NAME + " WHERE " + FAV_MUSIC_PATH + " = ?", new String[]{path});
            if (cursor.moveToFirst()) {
                do {
                    result = true;
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return false;
        }
    }

    public List<String> search_music_all(String query){
        try {
            List<String> result = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + MUSIC_PATH + " FROM " + MUSIC_TABLE_NAME + " WHERE "+MUSIC_PATH+" LIKE '%"+query+"%' order by " +MUSIC_ADDED_TIME+" DESC LIMIT 20", null);
            if (cursor.moveToFirst()) {
                do {
                    result.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }


    public List<String> get_fav_list(){
        try {
            List<String> result = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + FAV_MUSIC_PATH + " FROM " + FAV_MUSIC_TABLE_NAME + " order by " +FAV_MUSIC_ADDED_TIME+" DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    result.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public Boolean is_knownIP(String path){
        try {
            Boolean result = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT ip FROM known_ip WHERE ip = ?", new String[]{path});
            if (cursor.moveToFirst()) {
                do {
                    result = true;
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return false;
        }
    }

    public Boolean add_knownIp(String path){
        try {
            //DB
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            //Add values
            values.put("ip", path);

            //Pass
            if(!is_knownIP(path)) {
                db.insert("known_ip", null, values);
                db.close();
            }
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public List<String> get_knownIp(){
        try {
            List<String> result = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT ip FROM known_ip order by id DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    result.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public Boolean add_setting(String setting_name, String value){
       try {
           //DB
           SQLiteDatabase db = this.getWritableDatabase();
           ContentValues values = new ContentValues();

           //VALUES
           values.put(SETTINGS_NAME_COL, setting_name);
           values.put(SETTINGS_VAL_COL, value);

           //Pass
           if(get_settings(setting_name) != null ){
               db.update(SETTINGS_TABLE_NAME, values, SETTINGS_NAME_COL+" = ?", new String[]{setting_name});
           }
           else {
               db.insert(SETTINGS_TABLE_NAME, null, values);
           }
           return true;
       }
       catch (Exception e){
           return false;
       }
    }

    public void add_profile_data(String data_key, String value){
        try {
            //DB
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            //VALUES
            values.put(PROFILE_DATA_NAME, data_key);
            values.put(PROFILE_DATA_VAL, value);

            //Pass
            if(get_profile_data(data_key) == null) {
                //New
                db.insert(PROFILE_TABLE_NAME, null, values);
            }
            else {
                //Update
                db.update(PROFILE_TABLE_NAME, values, PROFILE_DATA_NAME+" = ?", new String[]{data_key});
            }

            db.close();
        }
        catch (Exception e){
        }
    }

    public String get_profile_data(String data_key){
        try {
            String result = null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + PROFILE_DATA_VAL + " FROM " + PROFILE_TABLE_NAME + " WHERE " + PROFILE_DATA_NAME + " = ?", new String[]{data_key});
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(0);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public void delete_profile_data(String data_key){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PROFILE_TABLE_NAME, PROFILE_DATA_NAME+" = ?", new String[]{data_key});
        db.close();
    }

    public void fav_music_remove(String path){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(FAV_MUSIC_TABLE_NAME, FAV_MUSIC_PATH+" = ?", new String[]{path});
        db.close();
    }

    public String get_settings(String setting_name){
        try {
            String result = null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + SETTINGS_VAL_COL + " FROM " + SETTINGS_TABLE_NAME + " WHERE " + SETTINGS_NAME_COL + " = ? ORDER BY id DESC LIMIT 1 OFFSET 0", new String[]{setting_name});
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(0);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return result;
        }
        catch (Exception e){
            return null;
        }
    }

    public List<String[]> get_files_history(){
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME + " ORDER BY send_time DESC", null);
            List<String[]> results = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    results.add(
                            //portal_id, path, size, time, method, info, colum id
                            new String[]{
                                    cursor.getString(3),
                                    cursor.getString(1),
                                    cursor.getString(5),
                                    cursor.getString(6),
                                    cursor.getString(4),
                                    cursor.getString(2),
                                    cursor.getString(0)
                            }
                    );
                } while (cursor.moveToNext());
            }
            cursor.close();
            return results;
        }
        catch (Exception e){
            return null;
        }
    }

    public List<String> get_files_history_by_portal_id(String portal_id){
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT path FROM " + HISTORY_TABLE_NAME + " WHERE portal_id = ? ORDER BY send_time DESC", new String[]{portal_id});
            List<String> results = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    results.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return results;
        }
        catch (Exception e){
            return null;
        }
    }

    public void incomingPut(fileOBJ[] fileOBJS){
        //Empty the table
        SQLiteDatabase db = this.getWritableDatabase();

        //ALL grater then 0
        db.delete(INC_NAME, INC_ID+" > ?", new String[]{"0"});
        db.close();

        //ADD THE LIST
        for(fileOBJ file: fileOBJS){
            //INSERT
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            //Add values
            values.put(INC_FILE, file.getFile());
            values.put(INC_LINK, file.getLink());
            values.put(INC_PASS, file.getPass());
            db.insert(INC_NAME, null, values);
            db.close();
        }
    }

    public List<fileOBJ> incomingGet(){
        SQLiteDatabase db = this.getReadableDatabase();
        List<fileOBJ> list = new ArrayList<>();
        //Select all
        Cursor cursor = db.rawQuery("SELECT * FROM " + INC_NAME + " WHERE "+INC_ID+" > 0", null);
        if (cursor.moveToFirst()) {
            do {
                fileOBJ fileOBJ = new fileOBJ();
                fileOBJ.setId(Integer.parseInt(cursor.getString(0)));
                fileOBJ.setFile(cursor.getString(1));
                fileOBJ.setLink(cursor.getString(2));
                fileOBJ.setPass(cursor.getString(3));
                int received = (null != cursor.getString(4))?Integer.parseInt(cursor.getString(4)):0;
                if(received == 0){
                    fileOBJ.setReceived(false);
                }
                fileOBJ.setReceived(true);
                list.add(fileOBJ);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PROFILE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + FAV_MUSIC_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MUSIC_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + INC_NAME);
        db.execSQL("DROP TABLE IF EXISTS known_ip");
        onCreate(db);
    }
}
