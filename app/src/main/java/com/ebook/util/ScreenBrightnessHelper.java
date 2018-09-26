package com.ebook.util;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Created by Administrator on 2017/1/13.
 */

public class ScreenBrightnessHelper {

    //设置系统屏幕亮度值,在设置之前先关闭亮度自动调节，设为手动模式
    public static void setBrightness(Context context, int value) {
        if (isAutoBrightness(context)){
            closeAutoBrightness(context);
        }
        ContentResolver contentResolver = context.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, value);
    }


    //获取当前系统屏幕亮度，获取失败返回-1
    public static int getBrightness(Context context)
    {
        int brightnessValue = -1;
        try
        {
            brightnessValue = Settings.System.
                    getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return brightnessValue;
    }


    //判断系统是否打开了自动调节亮度
    public static boolean isAutoBrightness(Context context)
    {
        boolean autoBrightness = false;
        try
        {
            autoBrightness = Settings.System.getInt(context.getContentResolver() , Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return autoBrightness;
    }


    //关闭自动调节亮度,设为手动模式
    public static void closeAutoBrightness(Context context)
    {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    //打开自动调节亮度
    public static void openAutoBrightness(Context context)
    {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }




}
