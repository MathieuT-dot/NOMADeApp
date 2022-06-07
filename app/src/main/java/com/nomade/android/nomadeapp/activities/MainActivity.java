package com.nomade.android.nomadeapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.preference.PreferenceManager;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.ChannelIds;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.CustomRequest;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.services.UsbAndTcpService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;

/**
 * Interreg VA 2 Seas – Project NOMADe
 * Date 2021/08, Author Mathieu Troch
 * Version Number 0.13
 * NOMADe App
 *
 * MainActivity
 *
 * Home page of the app
 * In the current state used to display the buttons to open other activities
 */
public class MainActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "MainActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private TextView uploadQuestionnairesTextView;
    private TextView loginStatusTextView;
    private View uploadDividerView;

    private LinearLayout loginLinearLayout;
    private LinearLayout buttonsLinearLayout;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private Button controlPanelButton;
    private Button viewQuestionnairesButton;
    private Button viewSubmittedQuestionnairesButton;
    private Button setupMenuButton;
    private Button measurementMenuButton;
    private Button graphButton;
    private Button valuesButton;

    private int remainingQuestionnaires = 0;
    private int currentQuestionnaire = 0;
    private int successfulQuestionnaires = 0;

    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor defaultEditor;
    private SharedPreferences loginSharedPreferences;
    private SharedPreferences.Editor loginEditor;
    private SharedPreferences permissionsSharedPreferences;
    private SharedPreferences.Editor permissionsEditor;
    private SharedPreferences questionnairesSharedPreferences;
    private SharedPreferences.Editor questionnairesEditor;

    public static String username;
    public static char[] secret1;
    public static char[] secret2;
    public static boolean loggedIn = false;

    private String currentVersion = null;

    private boolean startup = true;

    private static final String LOGOUT_DIALOG = "dialogTagLogout";
    private static final String GOOGLE_PLAY_DIALOG = "dialogTagGooglePlay";
    private static final String SUBMIT_ERROR_DIALOG = "dialogTagSubmitError";
    private static final String TERMS_DIALOG = "dialogTagTerms";
    private static final String TERMS_ACCEPTED = "termsAccepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_main);

        if(getIntent().getBooleanExtra("KILL_APP", false)){
            finishAndRemoveTask();
        }

        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultEditor = defaultSharedPreferences.edit();
        defaultEditor.apply();

        loginSharedPreferences = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE);
        loginEditor = loginSharedPreferences.edit();
        loginEditor.apply();

        permissionsSharedPreferences = getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE);
        permissionsEditor = permissionsSharedPreferences.edit();
        permissionsEditor.apply();

        questionnairesSharedPreferences = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE);
        questionnairesEditor = questionnairesSharedPreferences.edit();
        questionnairesEditor.apply();

        if (!defaultSharedPreferences.contains(Constants.SETTING_SERVER_API_URL)) {
            defaultEditor.putString(Constants.SETTING_SERVER_API_URL, Constants.API_URL).apply();
        }

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        uploadDividerView = findViewById(R.id.upload_divider_view);
        loginLinearLayout = findViewById(R.id.login_linear_layout);
        buttonsLinearLayout = findViewById(R.id.buttons_linear_layout);
        usernameEditText = findViewById(R.id.username_edit_text);
        usernameEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginStatusTextView = findViewById(R.id.login_status_text_view);

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> logIn());

        uploadQuestionnairesTextView = findViewById(R.id.upload_questionnaires_text_view);
        uploadQuestionnairesTextView.setOnClickListener(v -> {
            if (Utilities.checkNetworkConnection(context)) {
                uploadQuestionnairesTextView.setVisibility(View.GONE);
                uploadDividerView.setVisibility(View.GONE);

                currentQuestionnaire = 0;
                successfulQuestionnaires = 0;

                pDialog.setMessage(getString(R.string.submitting_your_answers_ellipsis));
                showDialog();

                uploadRemainingQuestionnaires();
            } else {
                Utilities.displayToast(context, getString(R.string.no_internet_access));
            }
        });

        controlPanelButton = findViewById(R.id.control_panel_button);
        controlPanelButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, ControlPanelActivity.class);
            startActivity(intent);
        });

        viewQuestionnairesButton = findViewById(R.id.view_questionnaires_button);
        viewQuestionnairesButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, QuestionnairesActivity.class);
            startActivity(intent);
        });

        viewSubmittedQuestionnairesButton = findViewById(R.id.view_submitted_questionnaires_button);
        viewSubmittedQuestionnairesButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, SubmissionsActivity.class);
            startActivity(intent);
        });

        graphButton = findViewById(R.id.graph_button);
        graphButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, GraphActivity.class);
            startActivity(intent);
        });

        valuesButton = findViewById(R.id.values_button);
        valuesButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, ValuesActivity.class);
            startActivity(intent);
        });

        setupMenuButton = findViewById(R.id.setup_menu_button);
        setupMenuButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, SetupMenuActivity.class);
            startActivity(intent);
        });

        measurementMenuButton = findViewById(R.id.measurement_menu_button);
        measurementMenuButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, MeasurementMenuActivity.class);
            startActivity(intent);
        });

        TextView buttonTermsAndConditionsTextView = findViewById(R.id.button_terms_and_conditions_text_view);
        buttonTermsAndConditionsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TermsAndConditionsActivity.class);
            startActivity(intent);
        });

        if (savedInstanceState != null){
            startup = savedInstanceState.getBoolean("STARTUP");
        }
        else {
            String actionString = getIntent().getAction();
            if (actionString != null && actionString.contains("android.hardware.usb.action.USB_ACCESSORY_ATTACHED") && !UsbAndTcpService.isRunning()){
                if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false)) {
                    controlPanelButton.setVisibility(View.VISIBLE);
                }
                startUsbService();
            }
        }

        prepareNotificationChannels();

        showPrivacyPolicy();

        // memory debugging
        Runtime rt = Runtime.getRuntime();
        // currently available memory in bytes
        long maxMemory = rt.maxMemory();
        MyLog.v("onCreate", "maxMemory: " + Long.toString(maxMemory) + " bytes");
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        // available memory in megabytes without largeHeap
        int memoryClass = am.getMemoryClass();
        MyLog.v("onCreate", "memoryClass: " + Integer.toString(memoryClass) + " megabytes");
        // available memory in megabytes with largeHeap
        int largeMemoryClass = am.getLargeMemoryClass();
        MyLog.v("onCreate", "largeMemoryClass: " + Integer.toString(largeMemoryClass) + " megabytes");
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableButtons(true);
        checkLoginStatusOnDevice();

        if (UsbAndTcpService.isRunning()) {
            controlPanelButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("STARTUP", startup);
        super.onSaveInstanceState(outState);
    }

    /**
     * Enables or disables all buttons in the MainActivity
     *
     * @param b enables all buttons if true
     *          disables all buttons if false
     */
    private void enableButtons(boolean b){
        viewQuestionnairesButton.setEnabled(b);
        viewSubmittedQuestionnairesButton.setEnabled(b);
        setupMenuButton.setEnabled(b);
        measurementMenuButton.setEnabled(b);
        controlPanelButton.setEnabled(b);
        graphButton.setEnabled(b);
        valuesButton.setEnabled(b);
    }

    /**
     * Checks the login status on the device and changes the UI based on the login status.
     */
    private void checkLoginStatusOnDevice() {
        MyLog.d(TAG, "checkLoginStatusOnDevice");

        pDialog.setMessage(getString(R.string.checking_server_and_login_ellipsis));
        showDialog();

        invalidateOptionsMenu();

        if (loginSharedPreferences.getBoolean("logged_in", false) && loginSharedPreferences.contains("cred1") && loginSharedPreferences.contains("cred2") && loginSharedPreferences.contains("cred3")) {

            username = loginSharedPreferences.getString("cred1", "null");
            secret1 = loginSharedPreferences.getString("cred2", "null").toCharArray();
            secret2 = loginSharedPreferences.getString("cred3", "null").toCharArray();

            buttonsLinearLayout.setVisibility(View.VISIBLE);
            loginLinearLayout.setVisibility(View.GONE);

            loggedIn = true;

            loginStatusTextView.setText(String.format("%s: %s", getString(R.string.logged_in_as), username));

            showOrHideMenuButtons();

            if (checkForRemainingQuestionnaires()) {
                uploadQuestionnairesTextView.setVisibility(View.VISIBLE);
                uploadDividerView.setVisibility(View.VISIBLE);

                uploadQuestionnairesTextView.setText(getString(R.string.upload_remaining_questionnaires, remainingQuestionnaires));
            } else {
                uploadQuestionnairesTextView.setVisibility(View.GONE);
                uploadDividerView.setVisibility(View.GONE);
            }

            usernameEditText.setText("");
            passwordEditText.setText("");

            String instrumentTypesJson = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).getString(Constants.API_INSTRUMENT_TYPES, "");
            String instrumentParametersJson = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).getString(Constants.API_PARAMETERS, "");

            if (instrumentTypesJson != null && !instrumentTypesJson.equals("") && instrumentParametersJson != null && !instrumentParametersJson.equals("")) {
                MyLog.d(TAG, "Instrument types and parameters available in shared preferences");
            }
            else {
                if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_PARAMETER_INDEX, false) && permissionsSharedPreferences.getBoolean(Constants.PERMISSION_INSTRUMENT_TYPE_INDEX, false)) {
                    MyLog.d(TAG, "Instrument types and/or parameters missing from shared preferences, requesting both lists");
                    getInstrumentTypes();
                }
                else {
                    MyLog.d(TAG, "Instrument types and/or parameters missing from shared preferences and the user has no permissions, do nothing");
                }
            }

        } else {
            buttonsLinearLayout.setVisibility(View.GONE);
            loginLinearLayout.setVisibility(View.VISIBLE);

            loggedIn = false;

            loginStatusTextView.setText(getString(R.string.not_logged_in));
        }

        if (getIntent().getBooleanExtra("log_out", false)) {
            getIntent().removeExtra("log_out");
            MyLog.d(TAG, "UNAUTHORIZED FORCE LOG OUT");
            forceLogOut();
        } else {
            if (startup){
                startup = false;
                checkConnectionToServer();
            }
            else {
                hideDialog();
            }
        }
    }

    /**
     * Logs out the user.
     */
    private void forceLogOut() {
        MyLog.d(TAG, "forceLogOut");

        if (username != null && !username.equals("") && !username.equals("null")) {
            MyLog.d("SharedPreferences", "Saving user settings");

            defaultEditor.putBoolean(username + "_" + Constants.SETTING_QNR_DRAFT_VIEW, defaultSharedPreferences.getBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false)).apply();
            defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false).apply();

            defaultEditor.putBoolean(username + "_" + Constants.SETTING_QNR_DRAFT_SUBMIT, defaultSharedPreferences.getBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false)).apply();
            defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false).apply();
        }

        loginEditor.putBoolean("logged_in", false).apply();
        loginEditor.putString("last_username", username).apply();

        if (AppController.getInstance().resetRequestQueue()) {
            MyLog.d(TAG, "Request Queue reset");
        }

        checkLoginStatusOnDevice();
    }

    /**
     * Check the connection to the server to see if it's available.
     */
    private void checkConnectionToServer() {
        MyLog.d(TAG, "checkConnectionToServer");

        if (Utilities.checkNetworkConnection(context)) {

            String url = defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL);

            if (url != null && !url.endsWith("/")) {
                url += "/";
                defaultEditor.putString(Constants.SETTING_SERVER_API_URL, url).apply();
                MyLog.d(TAG, "/ appended to the URL");
            }

            MyLog.d("StringRequest", defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "conn/");

            StringRequest testRequest = new StringRequest(
                    Request.Method.GET,
                    defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "conn/",
                    response -> {
                        LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                        if (Utilities.extractConnectionCheck(response)) {
                            MyLog.d(TAG, getString(R.string.server_test_successful));

                            String localUrl = Utilities.extractUrl(response, "local");
                            MyLog.d(TAG, "localUrl: " + localUrl);
                            String updateUrl = Utilities.extractUrl(response, "update");
                            MyLog.d(TAG, "updateUrl: " + updateUrl);
                            String developmentUrl = Utilities.extractUrl(response, "development");
                            MyLog.d(TAG, "developmentUrl: " + developmentUrl);

                            try {
                                currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                            }
                            catch (PackageManager.NameNotFoundException e){
                                e.printStackTrace();
                            }

                            String minimumVersion = Utilities.extractVersion(response, "minimum", "com.nomade.android.nomadeapp");
                            MyLog.d(TAG, "minimumVersion: " + minimumVersion);
                            String latestVersion = Utilities.extractVersion(response, "latest", "com.nomade.android.nomadeapp");
                            MyLog.d(TAG, "latestVersion: " + latestVersion);

                            String notice = Utilities.extractNotice(response, "com.nomade.android.nomadeapp");
                            MyLog.d(TAG, "notice: " + notice);
                            String generalNotice = Utilities.extractGeneralNotice(response);
                            MyLog.d(TAG, "generalNotice: " + generalNotice);

                            if (!currentVersion.equals("") && !minimumVersion.equals("") && !latestVersion.equals("")){
                                checkForNewVersion(currentVersion, minimumVersion, latestVersion);
                            }

                            if (!notice.equals("") && !notice.equals(defaultSharedPreferences.getString("notice_string", "empty"))){
                                SimpleDialog.build()
                                        .title(R.string.notice)
                                        .msg(notice)
                                        .cancelable(false)
                                        .show(MainActivity.this);
                            }

                            defaultEditor.putString("notice_string", notice).apply();

                            if (!generalNotice.equals("") && !generalNotice.equals(defaultSharedPreferences.getString("general_notice_string", "empty"))){
                                SimpleDialog.build()
                                        .title(R.string.general_notice)
                                        .msg(generalNotice)
                                        .cancelable(false)
                                        .show(MainActivity.this);
                            }

                            defaultEditor.putString("general_notice_string", generalNotice).apply();

                            if (loginSharedPreferences.getBoolean("logged_in", false) && loginSharedPreferences.contains("cred1") && loginSharedPreferences.contains("cred2") && loginSharedPreferences.contains("cred3")) {
                                checkLoginStatusOnServer();
                            } else {
                                hideDialog();
                                loginLinearLayout.setVisibility(View.VISIBLE);
                            }

                        } else {
                            hideDialog();
                            Utilities.displayToast(context, getString(R.string.server_test_unsuccessful));
                            MyLog.d(TAG, getString(R.string.server_test_unsuccessful));
                        }
                    },
                    e -> {
                        MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
                        hideDialog();
                        Utilities.displayVolleyError(context, e);
                    }){

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                    return headers;
                }
            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(testRequest, "test_connection");

        } else {
            hideDialog();
            Utilities.displayToast(context, getString(R.string.no_internet_access));
        }
    }

    /**
     * Checks for a new version of the app, it will compare the current version with the minimum
     * and the latest version and it will show a dialog with the appropriate message for the user.
     *
     * @param current version of the app, currently installed
     * @param minimum version of the app, indicated by the server
     * @param latest version of the app, indicated by the server
     */
    private void checkForNewVersion(String current, String minimum, String latest){

        MyLog.d(TAG, "checkForNewVersion");

        switch (compareVersions(current, latest)) {
            case -1:
                MyLog.d(TAG, "checkForNewVersion: App version is lower than the latest version");
                switch (compareVersions(current, minimum)){
                    case -1:
                        MyLog.d(TAG, "checkForNewVersion: App version is lower than the minimum version");
                        SimpleDialog.build()
                                .title(R.string.update_required)
                                .msg(R.string.update_the_app_lower_minimum)
                                .pos(R.string.yes)
                                .neg(R.string.no)
                                .cancelable(false)
                                .show(this, GOOGLE_PLAY_DIALOG);
                        break;

                    case 0:
                    case 1:
                        MyLog.d(TAG, "checkForNewVersion: App version is higher than the minimum version");
                        SimpleDialog.build()
                                .title(R.string.update_available)
                                .msg(R.string.update_the_app)
                                .pos(R.string.yes)
                                .neg(R.string.no)
                                .cancelable(false)
                                .show(this, GOOGLE_PLAY_DIALOG);
                        break;
                }
                break;

            case 0:
                MyLog.d(TAG, "checkForNewVersion: App version is equal to the latest version");
                break;

            case 1:
                MyLog.d(TAG, "checkForNewVersion: App version is higher than the latest version");
                break;
        }
    }

    /**
     * Compares two versions codes.
     *
     * @param current version to compare
     * @param online version to compare
     * @return 0 if the versions are equal
     *         -1 if the current version is lower than the online version
     *         1 if the current version is higher than the online version
     */
    private int compareVersions(String current, String online) {
        MyLog.d(TAG, "compareVersions");

        String[] currentComponents = current.split("\\.");
        String[] onlineComponents = online.split("\\.");
        int length = Math.min(currentComponents.length, onlineComponents.length);
        for (int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.parseInt(currentComponents[i]), Integer.parseInt(onlineComponents[i]));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(currentComponents.length, onlineComponents.length);
    }

    /**
     * Checks the login status on the server to make sure the user is still signed in. If for
     * example the user logged in on another device, the users will be logged out automatically.
     */
    private void checkLoginStatusOnServer() {
        MyLog.d(TAG, "checkLoginStatusOnServer");

        String tag_string_req = "req_permissions";

        MyLog.d("StringRequest", defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/login/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/login/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    try {
                        JSONObject jObj = new JSONObject(response);
                        JSONObject jData = jObj.getJSONObject("data");

                        int userId = jData.getInt("id");
                        String name = jData.getString("username");

                        if (userId == loginSharedPreferences.getInt("user_id", -1) && name.equals(loginSharedPreferences.getString("cred1", "null"))){

//                            String email = jData.getString("email");
//                            String emailVerifiedAt = jData.getString("email_verified_at");
//                            String companyId = jData.getString("company_id");
//                            String createdAt = jData.getString("created_at");
//                            String updatedAt = jData.getString("updated_at");
//                            String deletedAt = jData.getString("deleted_at");

                            JSONArray jPermissionsArray = jData.getJSONArray("permissions");
                            for (int i = 0; i < jPermissionsArray.length(); i++) {
                                JSONObject currentObject = jPermissionsArray.getJSONObject(i);
                                permissionsEditor.putBoolean("permission." + currentObject.getString("uuid"), true);
                            }
                            permissionsEditor.apply();

                            // Uncomment for a list of all permissions with the corresponding values
//                            for (String tag : Constants.PERMISSIONS){
//                                MyLog.d(tag, String.valueOf(permissionsSharedPreferences.getBoolean(tag, false)));
//                            }

                            showOrHideMenuButtons();

                            hideDialog();
                        }
                        else {
                            forceLogOut();
                        }

                    } catch (JSONException e) {
                        MyLog.e(TAG, "JSON Exception: ", e);
                        forceLogOut();
                    }
                },
                e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
                    hideDialog();
                    Utilities.displayVolleyError(context, e);
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Shows a dialog asking if the user wants to log out.
     */
    private void logOut() {
        MyLog.d(TAG, "logOut");

        SimpleDialog.build()
                .title(R.string.log_out)
                .msg(R.string.do_you_want_to_log_out)
                .pos(R.string.yes)
                .neg(R.string.no)
                .show(this, LOGOUT_DIALOG);
    }

    /**
     * Does all the required work for a login attempt using the username and password provided
     * by the user.
     */
    private void logIn() {
        MyLog.d(TAG, "logIn");

        pDialog.setMessage(getString(R.string.logging_in_ellipsis));

        showDialog();

        getSharedPreferences(Constants.AUTH_CACHE, MODE_PRIVATE).edit().clear().apply();

        if (AppController.getInstance().resetRequestQueue()) {
            MyLog.d(TAG, "Request Queue reset");
        }

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        if (Utilities.checkNetworkConnection(context)) {

            if (!usernameEditText.getText().toString().equals("") && !passwordEditText.getText().toString().equals("")) {

                username = usernameEditText.getText().toString().trim();

                secret1 = passwordEditText.getText().toString().toCharArray();

//                MyLog.d(TAG, "Username: " + username + " | Password: " + new String(secret1));

                defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_VIEW, loginSharedPreferences.getBoolean(username + "_" + Constants.SETTING_QNR_DRAFT_VIEW, false)).apply();
                defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, loginSharedPreferences.getBoolean(username + "_" + Constants.SETTING_QNR_DRAFT_SUBMIT, false)).apply();
//                defaultEditor.putString(Constants.SETTING_SERVER_API_URL, loginSharedPreferences.getString(username + "_" + Constants.SETTING_SERVER_API_URL, Constants.API_URL)).apply();

                MyLog.d("StringRequest", defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/login/");

                CustomRequest loginRequest = new CustomRequest(
                        Request.Method.GET,
                        defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/login/",
                        response -> {

                            try {
                                MyLog.d(TAG, "Response: " + response.toString());

                                if (!loginSharedPreferences.getString("last_username", "null").equals(username)) {
                                    getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).edit().clear().apply();
                                    getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).edit().clear().apply();
                                    getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).edit().clear().apply();
                                    getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE).edit().clear().apply();
                                    getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).edit().clear().apply();
                                    getSharedPreferences(Constants.GRAPH_DATA, MODE_PRIVATE).edit().clear().apply();
                                }

                                if (checkLoginAndStorePermissions(response.get("string_data"))) {

                                    secret1 = Utilities.md5(new String(secret1)).toCharArray();

//                                    MyLog.d(TAG, "Password MD5 Hash: " + new String(secret1));

//                                    MyLog.d(TAG, "HA1 MD5 Hash: " + new String(secret2));

                                    loginEditor.putString("cred1", username).putString("cred2", new String(secret1)).putString("cred3", new String(secret2)).putBoolean("logged_in", true).apply();

                                    Utilities.displayToast(context, getString(R.string.login_successful));
                                } else {
                                    Utilities.displayToast(context, getString(R.string.login_failed));
                                    defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false).apply();
                                    defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false).apply();
//                                        defaultEditor.putString(Constants.SETTING_SERVER_API_URL, Constants.API_URL).apply();
                                }

                                checkLoginStatusOnDevice();
                            } catch (NoSuchAlgorithmException e) {
                                MyLog.e(TAG, "NoSuchAlgorithmException Error: %s", e);
                                Utilities.displayToast(context, "NoSuchAlgorithmException Error: " + e);

                                defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false).apply();
                                defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false).apply();
//                                    defaultEditor.putString(Constants.SETTING_SERVER_API_URL, Constants.API_URL).apply();
                            }

                        },
                        e -> {
                            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
                            hideDialog();
//                            Utilities.displayVolleyError(context, e, Constants.ERRORS.WRONG_LOGIN_CREDENTIALS);
                            Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.LOGIN_ATTEMPT);

                            defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false).apply();
                            defaultEditor.putBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false).apply();
//                                defaultEditor.putString(Constants.SETTING_SERVER_API_URL, Constants.API_URL).apply();
                        }){

                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Content-Type", "application/json");
                        headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                        return headers;
                    }
                };

//                loginRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                AppController.getInstance().addToRequestQueue(loginRequest);

            } else {
                hideDialog();
                Utilities.displayToast(context, getString(R.string.username_password_fields_empty));
            }

        } else {
            hideDialog();
            Utilities.displayToast(context, getString(R.string.no_internet_access));
        }
    }

    /**
     * Checks if the login was successful and if so, stores the permissions.
     *
     * @param json answer from the server regarding the login attempt, also contains permissions
     * @return true if the login was successful
     *         false if the login was unsuccessful
     */
    private boolean checkLoginAndStorePermissions(String json) {

        MyLog.d(TAG, "checkLoginAndStorePermissions");

        try {
            JSONObject jsonObject = new JSONObject(json);

            JSONObject jData = jsonObject.getJSONObject("data");

            String username = jData.getString("username");

            if (username.equals(MainActivity.username)) {
                loginEditor.putString("username", username);

                int userId = jData.getInt("id");
                loginEditor.putInt("user_id", userId);

                int companyId = jData.getInt("company_id");
                loginEditor.putInt("company_id", companyId);

                loginEditor.apply();

                JSONArray jPermissionsArray = jData.getJSONArray("permissions");
                for (int i = 0; i < jPermissionsArray.length(); i++) {
                    JSONObject currentObject = jPermissionsArray.getJSONObject(i);
                    permissionsEditor.putBoolean("permission." + currentObject.getString("uuid"), true);
                }
                permissionsEditor.apply();

                // Uncomment for a list of all permissions with the corresponding values
                for (String tag : Constants.PERMISSIONS){
                    MyLog.d(tag, String.valueOf(permissionsSharedPreferences.getBoolean(tag, false)));
                }

                return true;
            } else {
                return false;
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSON Exception: %s", e);
            return false;
        }
    }

    /**
     * Checks whether there are answers for questionnaires that still need to be submitted.
     *
     * @return true if there are remaining questionnaires
     *         false if there are no remaining questionnaires
     */
    private boolean checkForRemainingQuestionnaires() {

        MyLog.d(TAG, "checkForRemainingQuestionnaires");

        remainingQuestionnaires = 0;

        while (questionnairesSharedPreferences.contains("remaining_questionnaire_" + (remainingQuestionnaires + 1))) {
            remainingQuestionnaires++;
        }

        return remainingQuestionnaires > 0;
    }

    /**
     * Starts the process of uploading one or more remaining answers for questionnaires.
     */
    private void uploadRemainingQuestionnaires() {

        MyLog.d(TAG, "uploadRemainingQuestionnaires");

        currentQuestionnaire++;

        if (currentQuestionnaire <= remainingQuestionnaires) {
            String json = questionnairesSharedPreferences.getString("remaining_questionnaire_" + currentQuestionnaire, "null");

            if (json != null && !json.equals("null")) {
                submitQuestionnaire(json);
            }
        } else {
            hideDialog();

            Utilities.displayToast(context, getString(R.string.questionnaires_submitted, successfulQuestionnaires, remainingQuestionnaires - successfulQuestionnaires));

            currentQuestionnaire = 0;
            remainingQuestionnaires = 0;

            if (checkForRemainingQuestionnaires()) {
                uploadQuestionnairesTextView.setVisibility(View.VISIBLE);
                uploadDividerView.setVisibility(View.VISIBLE);

                uploadQuestionnairesTextView.setText(getString(R.string.upload_remaining_questionnaires, remainingQuestionnaires));
            }
        }
    }

    /**
     * Shows or hides menu buttons based on the permissions of the user
     */
    private void showOrHideMenuButtons() {

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_QUESTIONNAIRE_INDEX, false)) {
            viewQuestionnairesButton.setVisibility(View.VISIBLE);
        } else {
            viewQuestionnairesButton.setVisibility(View.GONE);
        }

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_USER, false) || permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false) || permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false)) {
            viewSubmittedQuestionnairesButton.setVisibility(View.VISIBLE);
        } else {
            viewSubmittedQuestionnairesButton.setVisibility(View.GONE);
        }

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false)) {
            setupMenuButton.setVisibility(View.VISIBLE);
            graphButton.setVisibility(View.VISIBLE);
            valuesButton.setVisibility(View.VISIBLE);
        } else {
            setupMenuButton.setVisibility(View.GONE);
            graphButton.setVisibility(View.GONE);
            valuesButton.setVisibility(View.GONE);
        }

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_MEASUREMENT_INDEX, false)) {
            measurementMenuButton.setVisibility(View.VISIBLE);
        } else {
            measurementMenuButton.setVisibility(View.GONE);
        }
    }

    /**
     * Submits answers of a questionnaire to the server.
     *
     * @param jsonString answers for a questionnaire
     */
    private void submitQuestionnaire(final String jsonString) {

        MyLog.d(TAG, "submitQuestionnaire");

        String tag_string_req = "sbm_set_questionnaire";

        MyLog.d("StringRequest", defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/");

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                defaultSharedPreferences.getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    questionnairesEditor.remove("remaining_questionnaire_" + currentQuestionnaire).apply();
                    successfulQuestionnaires++;
                    uploadRemainingQuestionnaires();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());

            hideDialog();

            Bundle bundle = new Bundle();
            bundle.putString("JSON_STRING", jsonString);

            SimpleDialog.build()
                    .title(R.string.submitting_failed)
                    .msg(R.string.couldnt_submit_questionnaire)
                    .pos(R.string.yes)
                    .neg(R.string.no)
                    .extra(bundle)
                    .cancelable(false)
                    .show(MainActivity.this, SUBMIT_ERROR_DIALOG);
        }) {

            // replaces getParams with getBody
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            // replaces getParams with getBodyContentType
            @Override
            public byte[] getBody() {
                return jsonString == null ? null : jsonString.getBytes(StandardCharsets.UTF_8);
            }

            // Deprecated by getBodyContentType and getBody
//            @Override
//            protected Map<String, String> getParams() {
//                // Posting parameters to login url
//                Map<String, String> params = new HashMap<>();
//                params.put("json", jsonString);
//
//                return params;
//            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Starts the USB Service.
     */
    private void startUsbService(){
        Intent startIntent = new Intent(MainActivity.this, UsbAndTcpService.class);
        startIntent.setAction(Constants.ACTION.ACTION_START_FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent);
        } else {
            context.startService(startIntent);
        }
    }

    /**
     * Creates the different Notification Channels for the app.
     * Also removes the "App"-channel that was used for testing.
     */
    private void prepareNotificationChannels(){
        // notification channels are available and mandatory starting from Android Oreo (SDK version 26)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // create the channel for general notifications
            if (mNotificationManager.getNotificationChannel(ChannelIds.GENERAL) == null) {
                CharSequence name = getString(R.string.channel_id__name_general);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(ChannelIds.GENERAL, name, importance);
                channel.enableVibration(false);
                mNotificationManager.createNotificationChannel(channel);
            }

            // create the channel for notifications about the usb service
            if (mNotificationManager.getNotificationChannel(ChannelIds.USB_SERVICE) == null) {
                CharSequence name = getString(R.string.channel_id__name_usb_service);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(ChannelIds.USB_SERVICE, name, importance);
                channel.enableVibration(false);
                mNotificationManager.createNotificationChannel(channel);
            }

            // create the channel for notifications about the tcp service
            if (mNotificationManager.getNotificationChannel(ChannelIds.TCP_SERVICE) == null) {
                CharSequence name = getString(R.string.channel_id__name_tcp_service);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(ChannelIds.TCP_SERVICE, name, importance);
                channel.enableVibration(false);
                mNotificationManager.createNotificationChannel(channel);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logout = menu.findItem(R.id.action_log_out);
        logout.setVisible(loginSharedPreferences.getBoolean("logged_in", false));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_log_out) {
            logOut();
        } else if (itemId == R.id.action_home_about) {
            Intent aboutIntent = new Intent(context, AboutActivity.class);
            aboutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(aboutIntent);
        } else if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(context, SettingsActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(settingsIntent);
        }
        return true;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (LOGOUT_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE) {
                forceLogOut();
                return true;
            }
        }

        if (GOOGLE_PLAY_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.nomade.android.nomadeapp"));
                startActivity(intent);
                return true;
            }
        }

        if (SUBMIT_ERROR_DIALOG.equals(dialogTag)){
            String jsonString = extras.getString("JSON_STRING");
            switch (which){
                case BUTTON_POSITIVE:
                    showDialog();
                    submitQuestionnaire(jsonString);
                    return true;
                case BUTTON_NEGATIVE:
                    int index = 0;
                    do {
                        index++;
                    }
                    while (questionnairesSharedPreferences.contains("remaining_questionnaire_" + index));
                    questionnairesEditor.putString("remaining_questionnaire_" + index, jsonString).remove("remaining_questionnaire_" + currentQuestionnaire).apply();
                    showDialog();
                    uploadRemainingQuestionnaires();
                    return true;
            }
        }

        if (TERMS_DIALOG.equals(dialogTag)){ /* {@link MainActivity#showHtml} **/
            if (which == BUTTON_POSITIVE){ // terms accepted
                MyLog.d(TAG, "Privacy policy accepted");
                defaultEditor.putBoolean(TERMS_ACCEPTED, true).apply();
            } else { // terms declined, exit
                MyLog.d(TAG, "Privacy policy declined, app exit");
                System.exit(0);
            }
            return true;
        }

        return false;
    }

    /**
     * Shows the privacy policy on screen and asks the user to accept the privacy policy.
     * When accepted the window will close and won't be shown again unless the app data is removed.
     * When declined, the app wil automatically close.
     */
    private void showPrivacyPolicy(){
        if (!defaultSharedPreferences.getBoolean(TERMS_ACCEPTED, false)) {
            SimpleDialog.build()
                    .title(R.string.terms_title)
                    .msgHtml(R.string.terms_text)
                    .cancelable(false)
                    .pos(R.string.accept)
                    .neg(R.string.decline)
                    .show(this, TERMS_DIALOG);
        }
    }

    /**
     * Gets a list with instrument types from the server and saves it in the shared preferences for
     * future usage. This is only done once to make sure the list is available to parse setups.
     */
    private void getInstrumentTypes() {
        String tag_string_req = "get_types";

        pDialog.setMessage(getString(R.string.updating_type_list_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instrumentTypes/");

        final StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instrumentTypes/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).edit().putString(Constants.API_INSTRUMENT_TYPES, response).apply();
                    getInstrumentParameters();
                },
                error -> {
                    MyLog.e(TAG, "Volley Error: " + error.toString() + ", " + error.getMessage() + ", " + error.getLocalizedMessage());
                    hideDialog();
                    Utilities.displayVolleyError(context, error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
    }

    /**
     * Gets a list with instrument parameters from the server and saves it in the shared preferences
     * for future usage. This is only done once to make sure the list is available to parse setups.
     */
    private void getInstrumentParameters() {
        String tag_string_req = "get_instrument_parameters";

        pDialog.setMessage(getString(R.string.updating_parameter_list_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "parameters/");

        final StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "parameters/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);
                    getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).edit().putString(Constants.API_PARAMETERS, response).apply();
                    hideDialog();
                },
                error -> {
                    MyLog.e(TAG, "Volley Error: " + error.toString() + ", " + error.getMessage() + ", " + error.getLocalizedMessage());
                    hideDialog();
                    Utilities.displayVolleyError(context, error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
    }

    /**
     * Shows the dialog when it's not already showing.
     */
    private void showDialog() {
        if (!isFinishing() && pDialog != null && !pDialog.isShowing())
            pDialog.show();
    }

    /**
     * Hides the dialog when it's showing.
     */
    private void hideDialog() {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        hideDialog();
        super.onDestroy();
    }
}
