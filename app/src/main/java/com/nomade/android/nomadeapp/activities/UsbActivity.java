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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.StringListAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.setups.Setup;
import com.nomade.android.nomadeapp.helperClasses.MessageCodes;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.services.UsbAndTcpService;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.Spinner;

/**
 * UsbActivity
 *
 * Activity to manage the USB connection to the main board
 */
public class UsbActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "UsbActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private Button initButton, setupRawButton, startStreamButton, stopStreamButton, startMeasurementButton, stopMeasurementButton;

    private static SimpleDateFormat timeFormatter;

    ListView listView;
    private ArrayList<String> arrayList;
    private ArrayList<String> pauseArrayList;
    private StringListAdapter adapter;

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private int sendWhat = 0;
    private static final int SEND_JSON = 1;
    private static final int SEND_RAW = 2;

    public static final int PICK_SETUP_REQUEST = 1;
    public static final int PICK_MEASUREMENT_REQUEST = 2;

    private static final String NEW_MEASUREMENT_DIALOG = "dialogTagNewMeasurement";
    private static final String STORE_OR_DELETE_DIALOG = "dialogTagStoreOrDelete";

    private SharedPreferences setupSharedPreferences;
    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private static final String
            USER_ID = "userId",
            SETUP_ID = "setupId",
            DESCRIPTION = "description";

    private String[] userNameArray;
    private int[] userIdArray;
    private String[] setupNameArray;
    private int[] setupIdArray;

    private Menu menu;
    private boolean dataViewIsRunning = true;
    private boolean showDetailedData = false;

    private int currentUserId = -1;
    private int currentUserIndex = -1;
    private int currentSetupId = -1;
    private int currentSetupIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        timeFormatter = new SimpleDateFormat("hh:mm:ss,SSS", Locale.getDefault());

        if (savedInstanceState != null){
            arrayList = savedInstanceState.getStringArrayList("LOG_ARRAY");
        }
        else {
            arrayList = new ArrayList<>();
        }

        pauseArrayList = new ArrayList<>();

        //relate the listView from java to the one created in xml
        listView = findViewById(R.id.list);
        adapter = new StringListAdapter(this, arrayList);
        listView.setAdapter(adapter);

        initButton = findViewById(R.id.init_button);
        initButton.setOnClickListener(v -> {
            // Sends the message to initialise communication (config and ask address).
            sendMessageToService(MessageCodes.USB_MSG_INIT);
        });

        Button configButton = findViewById(R.id.config_button);
        configButton.setOnClickListener(v -> {
            // Sends the message to configure the USB chip.
            sendMessageToService(MessageCodes.USB_MSG_CONFIG);
        });

        Button autoButton = findViewById(R.id.auto_button);
        autoButton.setOnClickListener(v -> {
            // Sends the message to automatically go through all communication needed for the streaming.
            sendMessageToService(MessageCodes.USB_MSG_AUTO);
        });

        Button askAddressButton = findViewById(R.id.ask_address_button);
        askAddressButton.setOnClickListener(v -> {
            // Sends the message to ask an address from the main board.
            sendMessageToService(MessageCodes.USB_MSG_ASK_ADDRESS);
        });

        Button ackMissConfigButton = findViewById(R.id.ack_miss_config_button);
        ackMissConfigButton.setOnClickListener(v -> {
            // Sends the message to acknowledge a missing configuration.
            sendMessageToService(MessageCodes.USB_MSG_ACK_MISS_CONFIG);
        });

        Button ackExisConfigButton = findViewById(R.id.ack_exis_config_button);
        ackExisConfigButton.setOnClickListener(v -> {
            // Sends the message to acknowledge an existing configuration.
            sendMessageToService(MessageCodes.USB_MSG_ACK_EXIS_CONFIG);
        });

        Button setupJsonButton = findViewById(R.id.setup_json_button);
        setupJsonButton.setOnClickListener(v -> {
            sendWhat = SEND_JSON;
            pickSetup();
        });

        setupRawButton = findViewById(R.id.setup_raw_button);
        setupRawButton.setOnClickListener(v -> {
            sendWhat = SEND_RAW;
            pickSetup();
        });

        startStreamButton = findViewById(R.id.start_stream_button);
        startStreamButton.setOnClickListener(v -> {
            // Sends the message to start the streaming of the data.
            sendMessageToService(MessageCodes.USB_MSG_START_STREAM);
        });

        stopStreamButton = findViewById(R.id.stop_stream_button);
        stopStreamButton.setOnClickListener(v -> {
            // Sends the message to stop the streaming of the data.
            sendMessageToService(MessageCodes.USB_MSG_STOP_STREAM);
        });

        Button requestJsonButton = findViewById(R.id.request_json_button);
        requestJsonButton.setOnClickListener(v -> {
            // Sends the message to request the JSON setup from the DMU
            sendMessageToService(MessageCodes.USB_MSG_REQUEST_JSON_SETUP);
        });

        startMeasurementButton = findViewById(R.id.start_measurement_button);
        startMeasurementButton.setOnClickListener(v ->{
            getUserList();
        });

        stopMeasurementButton = findViewById(R.id.stop_current_measurement_button);
        stopMeasurementButton.setOnClickListener(v -> {
            // Sends the message to stop the measurement.
            sendMessageToService(MessageCodes.USB_MSG_STOP_MEASUREMENT);
        });

        Button sendMeasurementListButton = findViewById(R.id.send_measurement_list_button);
        sendMeasurementListButton.setOnClickListener(v -> {
            // Sends the list of the measurements.
            sendMessageToService(MessageCodes.USB_MSG_SEND_MEASUREMENT_LIST);
        });

        updateUi();

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putStringArrayList("LOG_ARRAY", arrayList);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        showDetailedData = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false);
    }

    /**
     * Handler to handle the incoming data from the USB service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCodes.USB_MSG_SEND_STRING:
                    String str1 = msg.getData().getString("str1");
                    addData(str1);
                    break;
                case MessageCodes.USB_MSG_CURRENT_SETUP:
                    showCurrentSetup();
                    break;
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STARTED:
//                    startStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(true);
                    startStreamButton.setEnabled(true);
                    startMeasurementButton.setEnabled(true);
                    stopMeasurementButton.setEnabled(true);
                    break;
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STOPPED:
//                    startMeasurementButton.setEnabled(true);
//                    stopMeasurementButton.setEnabled(false);
                    startMeasurementButton.setEnabled(true);
                    stopMeasurementButton.setEnabled(true);
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
            switch (UsbAndTcpService.getStatusCode()) {
//                case StatusCodes.UTS_NOT_INIT:
//                    initButton.setEnabled(true);
//                    setupRawButton.setEnabled(false);
//                    startStreamButton.setEnabled(false);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_INIT:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(false);
//                    startStreamButton.setEnabled(false);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_ADDRESS:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(true);
//                    startStreamButton.setEnabled(true);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(true);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_WATCHDOG:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(true);
//                    startStreamButton.setEnabled(true);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(true);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_NO_SETUP:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(true);
//                    startStreamButton.setEnabled(false);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_STREAM_BUSY:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(false);
//                    startStreamButton.setEnabled(false);
//                    stopStreamButton.setEnabled(true);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(false);
//                    break;
//                case StatusCodes.UTS_MEASUREMENT_BUSY:
//                    initButton.setEnabled(false);
//                    setupRawButton.setEnabled(false);
//                    startStreamButton.setEnabled(false);
//                    stopStreamButton.setEnabled(false);
//                    startMeasurementButton.setEnabled(false);
//                    stopMeasurementButton.setEnabled(true);
//                    break;
                default:
                    initButton.setEnabled(true);
                    setupRawButton.setEnabled(true);
                    startStreamButton.setEnabled(true);
                    stopStreamButton.setEnabled(true);
                    startMeasurementButton.setEnabled(true);
                    stopMeasurementButton.setEnabled(true);
                    break;
            }
        }
        else {
            initButton.setEnabled(false);
            setupRawButton.setEnabled(false);
            startStreamButton.setEnabled(false);
            stopStreamButton.setEnabled(false);
            startMeasurementButton.setEnabled(false);
            stopMeasurementButton.setEnabled(false);
        }
    }

    /**
     * Creates a new connection to the USB service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            addDataWithMillis("Attached.");
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
            addDataWithMillis("Disconnected.");
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
            Utilities.displayToast(context, R.string.connection_to_the_dmu_is_active);
        }
        else {
            addDataWithMillis("UsbAndTcpService is not running");
            Utilities.displayToast(context, R.string.no_connection_to_the_dmu);
        }
    }

    /**
     * Binds the activity to the USB service.
     */
    private void doBindService() {
        bindService(new Intent(this, UsbAndTcpService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        addDataWithMillis("Binding.");
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
            addDataWithMillis("Unbinding.");
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

    /**
     * Adds the current time to the data and adds it to the log view
     *
     * @param s data to add
     */
    private void addDataWithMillis(String s){
        addData(timeFormatter.format(new Date(System.currentTimeMillis())) + " | " +  s);
    }

    /**
     * Adds data to the log view.
     *
     * @param s data to add
     */
    private void addData(String s) {
        pauseArrayList.add(s);

        if (dataViewIsRunning){

            while (pauseArrayList.size() > 0){
                arrayList.add(pauseArrayList.remove(0));
            }

            while (arrayList.size() > 100){
                arrayList.remove(0);
            }

            adapter.notifyDataSetChanged();
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
     * Display a dialog of the setup in the main board and in the memory of the Android device.
     */
    private void showCurrentSetup(){

        String jsonSetupInMemory = setupSharedPreferences.getString("setup_in_memory", "");
        int currentSetupId = setupSharedPreferences.getInt("current_setup_id", 0);
        int currentSetupVersion = setupSharedPreferences.getInt("current_setup_version", 0);

        String messageMemory = "";
        String messageCurrent = "";

        if (jsonSetupInMemory != null && !jsonSetupInMemory.equals("")){
            Setup setup = Utilities.parseJsonSetup(context, jsonSetupInMemory, jsonTypeInfoList, jsonParameterInfoList);
            if (setup != null){
                messageMemory = getString(R.string.setup_in_memory_has_id_and_version, setup.getId(), setup.getVersion());
            }
        }
        if (messageMemory.equals("")){
            messageMemory = getString(R.string.no_setup_in_memory);
        }

        if (currentSetupId > 0 && currentSetupVersion > 0){
            messageCurrent = getString(R.string.setup_in_main_board_has_id_and_version, currentSetupId, currentSetupVersion);
        }
        if (messageCurrent.equals("")){
            messageCurrent = getString(R.string.no_setup_in_main_board);
        }

        SimpleDialog.build()
                .title(R.string.setup)
                .msg(messageMemory + " " + messageCurrent)
                .show(this);
    }

    /**
     * Creates and starts an intent to select a setup.
     */
    private void pickSetup() {
        Intent pickSetupIntent = new Intent(context, SetupListActivity.class);
        startActivityForResult(pickSetupIntent, PICK_SETUP_REQUEST);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){

            if (requestCode == PICK_SETUP_REQUEST){
                setupSharedPreferences.edit().putString("setup_in_memory", data.getStringExtra("json_setup")).apply();

                switch (sendWhat){
                    case SEND_JSON:
                        sendWhat = 0;

                        // Sends the message to send the json of the setup.
                        MyLog.d(TAG, "sendJson");
                        sendMessageToService(MessageCodes.USB_MSG_SETUP_JSON);
                        break;

                    case SEND_RAW:
                        sendWhat = 0;

                        // Sends the message to send the raw of the setup.
                        MyLog.d(TAG, "sendRaw");
                        sendMessageToService(MessageCodes.USB_MSG_SETUP_RAW);
                        break;
                }
            }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_usb_communication, menu);
        this.menu = menu;

        this.menu.findItem(R.id.action_show_detailed_data).setChecked(showDetailedData);

        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_pause_resume) {
            if (dataViewIsRunning) {
                dataViewIsRunning = false;
                setOptionTitle(R.id.action_pause_resume, getString(R.string.resume));
                setOptionIcon(R.id.action_pause_resume, R.drawable.ic_action_play_arrow);
            } else {
                dataViewIsRunning = true;
                setOptionTitle(R.id.action_pause_resume, getString(R.string.pause));
                setOptionIcon(R.id.action_pause_resume, R.drawable.ic_action_pause);
                adapter.notifyDataSetChanged();
            }
        } else if (itemId == R.id.action_show_detailed_data) {
            if (showDetailedData) {
                showDetailedData = false;
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false).apply();
                item.setChecked(false);
            } else {
                showDetailedData = true;
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_SHOW_DETAILED_DATA, true).apply();
                item.setChecked(true);
            }
        }
        return true;
    }

    /**
     * Dynamically change the title of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the title
     * @param title new title for the option
     */
    private void setOptionTitle(int id, String title) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setTitle(title);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
        }
    }

    /**
     * Dynamically change the icon of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the icon
     * @param iconRes resource id of the new icon
     */
    private void setOptionIcon(int id, int iconRes) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setIcon(iconRes);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
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
