package com.ebook.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * Created by Administrator on 2016/12/16.
 */

public class SaveHelper {
    public static final String THEME = "SettingPopup_Theme";
    public static final String FLIP_STYLE = "SettingPopup_FlipStyle";

    public static final String DRAW_INFO = "BookPageFactory_draw_info";
    public static final String PAINT_INFO = "BookPageFactory_paint_info";

    public static final String IS_PRE_PAGE_OVER = "FlipView_IsPrePageOver";


    public static void save(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(key, value)
                .apply();

    }

    public static void save(Context context, String key, float value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putFloat(key, value)
                .apply();

    }

    public static void save(Context context, String key, boolean is) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(key, is)
                .apply();

    }


    public static void saveObject(Context context, String key, Object object) {
        //序列化对象，编码成String
        String objectStr = serObject(object);

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, objectStr)
                .apply();

    }


    public static int getInt(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, 0);

    }

    public static float getFloat(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key, 0f);

    }

    public static boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);

    }



    public static <T> T getObject(Context context, String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.contains(key)){
            String objectStr =sp.getString(key, null);
            T t = deserObject(objectStr);
            return t;
        }
        return null;

    }

    //序列化对象
    public static String serObject(Object object) {
        String objectStr = "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(object);
            objectStr = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return objectStr;
    }

    //反序列化获得对象
    public static <T> T deserObject(String objectStr) {

        byte[] buffer = Base64.decode(objectStr, Base64.DEFAULT);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            T t = (T) ois.readObject();
            return t;

        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bais != null) {
                    bais.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
