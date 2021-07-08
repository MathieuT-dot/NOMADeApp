package com.nomade.android.nomadeapp.helperClasses;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * AppController
 *
 * Inflate Volley objects to handle network requests
 */
public class AppController extends Application {

    private static final String TAG = "AppController";

    private static FirebaseAnalytics mFirebaseAnalytics;
    private static FirebaseCrashlytics mFirebaseCrashlytics;

    private RequestQueue mRequestQueue;

    private static AppController mInstance;

    private SharedPreferences logSharedPreferences;
    private SharedPreferences.Editor logEditor;
    private boolean logEnabled = false;
    private String logString = "";
    private String logName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);

        // Obtain the FirebaseCrashlytics instance.
        mFirebaseCrashlytics = FirebaseCrashlytics.getInstance();
        mFirebaseCrashlytics.setCrashlyticsCollectionEnabled(true);

        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.SETTING_ENABLE_LOGGING, false)) {
            startSharedPreferenceLog();
        }
    }

    /**
     * Gets the application instance.
     *
     * @return application instance
     */
    public static synchronized AppController getInstance() {
        return mInstance;
    }

    private void startSharedPreferenceLog() {
        logSharedPreferences = getSharedPreferences(Constants.LOG_DATA, MODE_PRIVATE);
        logEditor = logSharedPreferences.edit();
        logEditor.apply();
        logEnabled = true;
    }

    public void appendToSharedPreferenceLog(String s) {
        if (logEnabled) {
            if (logName.equals("")) {
                logName = s.substring(0, 24);
                logString = s;
            }
            else {
                String newLogName = s.substring(0, 24);
                if (!newLogName.substring(0, 13).equals(logName.substring(0,13))) {
                    logName = newLogName;
                    logString = s;
                }
            }
            logString = logString.concat(s).concat("\n");
            logEditor.putString(logName, logString).apply();
        }
    }

    /**
     * Gets the Volley request queue.
     *
     * @return Volley request queue
     */
    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HttpDigestStack());
        }

        return mRequestQueue;
    }

    /**
     * Resets the Volley request queue.
     *
     * @return true if the reset was successful
     *         false if the request queue was null
     */
    public boolean resetRequestQueue(){
        if (mRequestQueue != null){
            mRequestQueue = null;
            mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HttpDigestStack());
            return true;
        }
        return false;
    }

    /**
     * Adds a request to the queue with a tag.
     *
     * @param req request
     * @param tag tag describing the request
     */
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    /**
     * Adds a request to the queue.
     *
     * @param req request
     */
    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    /**
     * Cancels the pending requests.
     */
    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public static FirebaseAnalytics getFirebaseAnalytics() {
        return mFirebaseAnalytics;
    }

    public static FirebaseCrashlytics getFirebaseCrashlytics() {
        return mFirebaseCrashlytics;
    }
}
