package com.nomade.android.nomadeapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.InputType;
import android.view.View;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MessageCodes;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.StatusCodes;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.services.UsbAndTcpService;
import com.nomade.android.nomadeapp.setups.Setup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.Spinner;

/**
 * MeasurementMenuActivity
 *
 * This activity shows the measurement menu.
 */
public class MeasurementMenuActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "MeasurementMenuActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private SharedPreferences defaultSharedPreferences;

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private static final String NEW_MEASUREMENT_DIALOG = "dialogTagNewMeasurement";
    private static final String STORE_OR_DELETE_DIALOG = "dialogTagStoreOrDelete";

    private static final String
            USER_ID = "userId",
            SETUP_ID = "setupId",
            DESCRIPTION = "description";

    private String[] userNameArray;
    private int[] userIdArray;
    private String[] setupNameArray;
    private int[] setupIdArray;

    private int currentUserId = -1;
    private int currentUserIndex = -1;
    private int currentSetupId = -1;
    private int currentSetupIndex = -1;

    private Button measurementListButton, planNewMeasurementButton, startNewMeasurementButton, stopCurrentMeasurementButton;

    private SharedPreferences setupSharedPreferences;
    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_menu);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        measurementListButton = findViewById(R.id.view_measurement_list_button);
        measurementListButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, MeasurementListActivity.class);
            startActivity(intent);
        });

        planNewMeasurementButton = findViewById(R.id.plan_new_measurement_button);
        planNewMeasurementButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, MeasurementNewActivity.class);
            startActivity(intent);
        });

        startNewMeasurementButton = findViewById(R.id.start_new_measurement_button);
        startNewMeasurementButton.setOnClickListener(v -> {
            startNewMeasurementButton.setEnabled(false);
            getUserList();
        });

        stopCurrentMeasurementButton = findViewById(R.id.stop_current_measurement_button);
        stopCurrentMeasurementButton.setOnClickListener(v -> {
            // Sends the message to stop the measurement.
            sendMessageToService(MessageCodes.USB_MSG_STOP_MEASUREMENT);
        });

        showOrHideMenuButtons();

        updateUi();

        CheckIfServiceIsRunning();
    }

    /**
     * Shows or hides menu buttons based on the permissions of the user
     */
    private void showOrHideMenuButtons() {
        SharedPreferences permissionsSharedPreferences = getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE);

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_MEASUREMENT_INDEX, false)) {
            measurementListButton.setVisibility(View.VISIBLE);
        } else {
            measurementListButton.setVisibility(View.GONE);
        }

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_MEASUREMENT_CREATE, false)) {
            planNewMeasurementButton.setVisibility(View.GONE);
            startNewMeasurementButton.setVisibility(View.VISIBLE);
            stopCurrentMeasurementButton.setVisibility(View.VISIBLE);
        } else {
            planNewMeasurementButton.setVisibility(View.GONE);
            startNewMeasurementButton.setVisibility(View.GONE);
            stopCurrentMeasurementButton.setVisibility(View.GONE);
        }
    }

    /**
     * Creates and executes a request to get a list of the users (developers only).
     */
    private void getUserList(){
        String tag_string_req = "user_list";

        pDialog.setMessage(getString(R.string.getting_user_list_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonUserList(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
            hideDialog();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");
                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void parseJsonUserList(String jsonUserList){

        currentUserId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1);

        try {
            if (!jsonUserList.equals("")){
                JSONObject jObjUserList = new JSONObject(jsonUserList);

                JSONArray jUsersArray = jObjUserList.getJSONArray("data");

                userNameArray = new String[jUsersArray.length()];
                userIdArray = new int[jUsersArray.length()];

                for (int i = 0; i < jUsersArray.length(); i++){

                    JSONObject currentUser = jUsersArray.getJSONObject(i);

                    userIdArray[i] = currentUser.getInt("id");
                    userNameArray[i] = currentUser.getString("username");

                    if (currentUserId != -1 && currentUserId == userIdArray[i]){
                        currentUserIndex = i;
                    }
                }

                getSetupList();
            }
            else {
                hideDialog();
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    /**
     * Creates and executes a request to get the list of setups.
     */
    private void getSetupList(){
        String tag_string_req = "get_setup_list";

        pDialog.setMessage(getString(R.string.getting_setups_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonSetupList(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
            hideDialog();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Parses the JSON containing the list of setups.
     *
     * @param jsonSetupList JSON containing the list of setups
     */
    private void parseJsonSetupList(String jsonSetupList){

        Setup setupInMemory = Utilities.parseJsonSetup(context, setupSharedPreferences.getString("setup_in_memory", ""), jsonTypeInfoList, jsonParameterInfoList);
        if (setupInMemory != null) {
            currentSetupId = setupInMemory.getId();
        }

        try {
            JSONObject jObj = new JSONObject(jsonSetupList);

            JSONArray jSetupArray = jObj.getJSONArray("data");

            ArrayList<Setup> setupArrayList = new ArrayList<>();

            for (int i = 0; i < jSetupArray.length(); i++){

                JSONObject jCurrentSetup = jSetupArray.getJSONObject(i);

                int setupId = jCurrentSetup.getInt("id");
                int setupGroupId = jCurrentSetup.getInt("setup_group_id");
                String setupName = jCurrentSetup.getString("name_en");
                int setupHardwareIdentifier = jCurrentSetup.getInt("hw_identifier");
                int setupVersion = jCurrentSetup.getInt("version");
                boolean setupLocked = jCurrentSetup.getInt("locked") == 1;

                if (setupLocked){
                    setupArrayList.add(new Setup(setupId, setupGroupId, setupName, setupHardwareIdentifier, setupVersion, setupLocked));
                }
            }

            setupNameArray = new String[setupArrayList.size()];
            setupIdArray = new int[setupArrayList.size()];

            for (int i = 0; i < setupArrayList.size(); i++){
                Setup currentSetup = setupArrayList.get(i);
                setupNameArray[i] = currentSetup.getName();
                setupIdArray[i] = currentSetup.getId();

                if (currentSetupId != -1 && currentSetupId == setupIdArray[i]){
                    currentSetupIndex = i;
                }
            }

            hideDialog();

            showForm();
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    private void showForm(){
        SimpleFormDialog.build()
                .title(getString(R.string.start_measurement))
                .msg(getString(R.string.create_a_new_measuremet_before_starting_it))
                .fields(
                        Spinner.plain(USER_ID).label(getString(R.string.select_a_user_colon)).items(userNameArray).placeholder(getString(R.string.select_ellipsis)).preset(currentUserIndex).required(),
                        Spinner.plain(SETUP_ID).label(getString(R.string.select_a_setup_colon)).items(setupNameArray).placeholder(getString(R.string.select_ellipsis)).preset(currentSetupIndex).required(),
                        Input.plain(DESCRIPTION).hint(getString(R.string.description)).inputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).required()
                )
                .autofocus(false)
                .cancelable(false)
                .pos(R.string.ok)
                .neut(R.string.cancel)
                .show(this, NEW_MEASUREMENT_DIALOG);
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (NEW_MEASUREMENT_DIALOG.equals(dialogTag)){
            switch (which){
                case BUTTON_POSITIVE:
                    int userIndex = extras.getInt(USER_ID);
                    int setupIndex = extras.getInt(SETUP_ID);
                    String description = extras.getString(DESCRIPTION);

                    int selectedUserId = userIdArray[userIndex];
                    int selectedSetupId = setupIdArray[setupIndex];

//                    Utilities.displayToast(context, String.format("User ID: %s, Setup ID: %s, Description: %s", selectedUserId, selectedSetupId, description));

                    Bundle data = new Bundle();
                    data.putInt("user_id", selectedUserId);
                    data.putInt("setup_id", selectedSetupId);
                    data.putString("description", description);

                    sendMessageWithBundleToService(MessageCodes.USB_MSG_START_MEASUREMENT, data);
                    return true;

                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    startNewMeasurementButton.setEnabled(true);
                    return true;
            }
        }
        if (STORE_OR_DELETE_DIALOG.equals(dialogTag)){
            switch (which){
                case BUTTON_POSITIVE:
                    sendMessageWithIntegerToService(MessageCodes.USB_MSG_STORE_OR_DELETE, 1);
                    return true;

                case BUTTON_NEGATIVE:
                    sendMessageWithIntegerToService(MessageCodes.USB_MSG_STORE_OR_DELETE, -1);
                    return true;

                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }
        return false;
    }

    /**
     * Handler to handle the incoming data from the USB service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STARTED:
                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(true);
                    }
                    else {
                        startNewMeasurementButton.setEnabled(true);
                        stopCurrentMeasurementButton.setEnabled(true);
                    }
                    break;
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STOPPED:
                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                        startNewMeasurementButton.setEnabled(true);
                        stopCurrentMeasurementButton.setEnabled(false);
                    }
                    else {
                        startNewMeasurementButton.setEnabled(true);
                        stopCurrentMeasurementButton.setEnabled(true);
                    }
                    break;
                case MessageCodes.USB_MSG_STORE_OR_DELETE:
                    showStoreOrDelete();
                    break;
                case MessageCodes.USB_MSG_STATUS_UPDATE:
                    updateUi();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void updateUi() {
        if (UsbAndTcpService.isRunning()) {
            if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                switch (UsbAndTcpService.getStatusCode()) {
                    case StatusCodes.UTS_NOT_INIT:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_INIT:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_ADDRESS:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_WATCHDOG:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_NO_SETUP:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_UNLOCKED_SETUP:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_LOCKED_SETUP:
                        startNewMeasurementButton.setEnabled(true);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_STREAM_BUSY:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_MEASUREMENT_BUSY:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(true);
                        break;
                    case StatusCodes.UTS_NEW_SETUP:
                        startNewMeasurementButton.setEnabled(false);
                        stopCurrentMeasurementButton.setEnabled(false);
                        break;
                    default:
                        startNewMeasurementButton.setEnabled(true);
                        stopCurrentMeasurementButton.setEnabled(true);
                        break;
                }
            }
            else {
                startNewMeasurementButton.setEnabled(true);
                stopCurrentMeasurementButton.setEnabled(true);
            }
        }
        else {
            startNewMeasurementButton.setEnabled(false);
            stopCurrentMeasurementButton.setEnabled(false);
        }
    }

    /**
     * Show a dialog with the choice to mark the measurement for deletion
     */
    private void showStoreOrDelete(){
        SimpleDialog.build()
                .title(getString(R.string.measurement_stopped))
                .msg(getString(R.string.do_you_want_to_keep_the_measurement))
                .cancelable(false)
                .pos(R.string.yes)
                .neg(R.string.no)
                .show(this, STORE_OR_DELETE_DIALOG);
    }

    /**
     * Creates a new connection to the USB service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            MyLog.d(TAG, "onServiceConnected: Attached");
            try {
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                updateUi();
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            MyLog.d(TAG, "onServiceDisconnected: Disconnected");
        }
    };

    /**
     * Checks if the USB Service is running and if so,
     * it automatically binds to it.
     */
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (UsbAndTcpService.isRunning()) {
            doBindService();
            Utilities.displayToast(context, R.string.connection_to_the_dcu_is_active);
        }
        else {
            MyLog.d(TAG, "CheckIfServiceIsRunning: UsbAndTcpService is not running");
        }
    }

    /**
     * Binds the activity to the USB service.
     */
    private void doBindService() {
        bindService(new Intent(this, UsbAndTcpService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        MyLog.d(TAG, "doBindService: Binding");
    }

    /**
     * Unbinds the activity to the USB service.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MessageCodes.USB_MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            MyLog.d(TAG, "doUnbindService: Unbinding");
        }
    }

    /**
     * Sends a message to the USB service.
     *
     * @param messageId ID of the message to send
     */
    private void sendMessageToService(int messageId) {
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    /**
     * Sends a message to the USB service containing an integer.
     *
     * @param messageId ID of the message to send
     * @param arg1 integer to send
     */
    private void sendMessageWithIntegerToService(int messageId, int arg1){
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.arg1 = arg1;
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    /**
     * Sends a message to the USB service containing a bundle.
     *
     * @param messageId ID of the message to send
     * @param data bundle to send
     */
    private void sendMessageWithBundleToService(int messageId, Bundle data){
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.setData(data);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            MyLog.e(TAG, "Failed to unbind from the service", t);
        }
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
}