package com.nomade.android.nomadeapp.helperClasses;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MyLog
 *
 * Custom Log class to be able to save log files locally.
 */
public class MyLog {

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH'h'mm'm'ss.SSS's'", Locale.getDefault());

    public static int v(String tag, String msg) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " V " + tag + ": " + msg);
        return Log.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " V " + tag + ": " + msg);
        return Log.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " D " + tag + ": " + msg);
        return Log.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " D " + tag + ": " + msg);
        return Log.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " I " + tag + ": " + msg);
        return Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " I " + tag + ": " + msg);
        return Log.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " W " + tag + ": " + msg);
        return Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " W " + tag + ": " + msg);
        return Log.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " W " + tag);
        return Log.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " E " + tag + ": " + msg);
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        AppController.getInstance().appendToSharedPreferenceLog(sdf.format(new Date(System.currentTimeMillis())) + " E " + tag + ": " + msg);
        return Log.e(tag, msg, tr);
    }
}

