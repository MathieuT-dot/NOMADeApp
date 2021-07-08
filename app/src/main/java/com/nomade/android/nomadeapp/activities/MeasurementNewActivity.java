package com.nomade.android.nomadeapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Setup;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * MeasurementNewActivity
 *
 * This activity is used to create new measurements.
 */
public class MeasurementNewActivity extends AppCompatActivity {

    private static final String TAG = MeasurementNewActivity.class.getSimpleName();
    private final Context context = this;
    private ProgressDialog pDialog;

    private Spinner userSpinner;
    private Spinner setupSpinner;
    private EditText nameEditText;
    private Button startDateButton;
    private Button endDateButton;
    private Button startTimeButton;
    private Button endTimeButton;
    private Button cancelButton;
    private Button createButton;

    private String[] userNameArray;
    private int[] userIdArray;
    private String[] setupNameArray;
    private int[] setupIdArray;

    private int selectedUserId;
    private int selectedSetupId;

    private String name;

    private String jsonMeasurement = "";

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private int currentUserId = -1;
    private int currentUserIndex = -1;
    private int currentSetupId = -1;
    private int currentSetupIndex = -1;

    private SharedPreferences setupSharedPreferences;
    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_new);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        userSpinner = findViewById(R.id.user_spinner);
        setupSpinner = findViewById(R.id.setup_spinner);
        nameEditText = findViewById(R.id.name_edit_text);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        startDateButton = findViewById(R.id.start_date_button);
        startDateButton.setOnClickListener(v -> {
            // Create a new OnDateSetListener instance. This listener will be invoked when user click ok button in DatePickerDialog.
            DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                startDateButton.setText(sdf.format(calendar.getTime()));
            };

            Calendar calendar = Calendar.getInstance();
            int year;
            int month;
            int day;

            if (startDateButton.getText().toString().equals("")) {
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
            }
            else {
                Date date = sdf.parse(startDateButton.getText().toString(), new ParsePosition(0));
                calendar.setTime(date);
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
            }

            // Create the new DatePickerDialog instance.
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

            // Set dialog icon and title.
            datePickerDialog.setTitle(getString(R.string.please_select_a_date));

            // Popup the dialog.
            datePickerDialog.show(getSupportFragmentManager(), "StartDatePickerDialog");
        });

        endDateButton = findViewById(R.id.end_date_button);
        endDateButton.setOnClickListener(v -> {
            // Create a new OnDateSetListener instance. This listener will be invoked when user click ok button in DatePickerDialog.
            DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                endDateButton.setText(sdf.format(calendar.getTime()));
            };

            Calendar calendar = Calendar.getInstance();
            int year;
            int month;
            int day;

            if (endDateButton.getText().toString().equals("")) {
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
            }
            else {
                Date date = sdf.parse(endDateButton.getText().toString(), new ParsePosition(0));
                calendar.setTime(date);
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
            }

            // Create the new DatePickerDialog instance.
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

            // Set dialog title.
            datePickerDialog.setTitle(getString(R.string.please_select_a_date));

            // Popup the dialog.
            datePickerDialog.show(getSupportFragmentManager(), "EndDatePickerDialog");
        });

        startTimeButton = findViewById(R.id.start_time_button);
        startTimeButton.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                startTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
            };

            Calendar calendar = Calendar.getInstance();
            int hour;
            int minute;

            if (startTimeButton.getText().toString().equals("")){
                hour = calendar.get(Calendar.HOUR_OF_DAY);
                minute = calendar.get(Calendar.MINUTE);
            }
            else {
                String[] splitTimeString = startTimeButton.getText().toString().split(":");
                hour = Integer.parseInt(splitTimeString[0]);
                minute = Integer.parseInt(splitTimeString[1]);
            }

            // Create the new TimePickerDialog instance.
            TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

            // Set dialog title.
            timePickerDialog.setTitle(getString(R.string.please_select_a_time));

            // Popup the dialog.
            timePickerDialog.show(getSupportFragmentManager(), "StartTimePickerDialog");
        });

        endTimeButton = findViewById(R.id.end_time_button);
        endTimeButton.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                endTimeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
            };

            Calendar calendar = Calendar.getInstance();
            int hour;
            int minute;

            if (endTimeButton.getText().toString().equals("")){
                hour = calendar.get(Calendar.HOUR_OF_DAY);
                minute = calendar.get(Calendar.MINUTE);
            }
            else {
                String[] splitTimeString = endTimeButton.getText().toString().split(":");
                hour = Integer.parseInt(splitTimeString[0]);
                minute = Integer.parseInt(splitTimeString[1]);
            }

            // Create the new TimePickerDialog instance.
            TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

            // Set dialog title.
            timePickerDialog.setTitle(getString(R.string.please_select_a_time));

            // Popup the dialog.
            timePickerDialog.show(getSupportFragmentManager(), "EndTimePickerDialog");
        });

        cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v ->
                finish()
        );

        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(v -> {

            selectedUserId = userIdArray[userSpinner.getSelectedItemPosition()];
            selectedSetupId = setupIdArray[setupSpinner.getSelectedItemPosition()];

            name = nameEditText.getText().toString().trim();

            if (name.length() == 0){
                Utilities.displayToast(context, getString(R.string.please_provide_a_name));
                return;
            }

            name = name + " [UNKNOWN]";

            loadSelectedSetupInMemory();
        });

        getUserList();
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

            prepare();
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    private void prepare(){

        userSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_item_padded, userNameArray));
        if (currentUserIndex != -1){
            userSpinner.setSelection(currentUserIndex);
        }
        setupSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_item_padded, setupNameArray));
        if (currentSetupIndex != -1){
            setupSpinner.setSelection(currentSetupIndex);
        }

        hideDialog();
    }

    private void loadSelectedSetupInMemory(){
        if (selectedSetupId != currentSetupId){
            getSetupById(selectedSetupId);
        }
        else {
            prepareMeasurement();
        }
    }

    /**
     * Creates and executes a request to get a setup with a given ID.
     */
    private void getSetupById(int setupId) {

        String tag_string_req = "get_setup";

        pDialog.setMessage(getString(R.string.getting_selected_setup_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/");

        final StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    Setup setup = Utilities.parseJsonSetup(context, response, jsonTypeInfoList, jsonParameterInfoList);

                    if (setup != null && setup.getId() == selectedSetupId){
                        setupSharedPreferences.edit().putString("setup_in_memory", response).apply();
                    }

                    prepareMeasurement();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
            prepareMeasurement();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void prepareMeasurement(){

        if (startDateButton.getText().toString().equals("")){
            Utilities.displayToast(context, getString(R.string.please_select_a_start_date));
            hideDialog();
            return;
        }
        if (startTimeButton.getText().toString().equals("")){
            Utilities.displayToast(context, getString(R.string.please_select_a_start_time));
            hideDialog();
            return;
        }
        if (endDateButton.getText().toString().equals("")){
            Utilities.displayToast(context, getString(R.string.please_select_an_end_date));
            hideDialog();
            return;
        }
        if (endTimeButton.getText().toString().equals("")){
            Utilities.displayToast(context, getString(R.string.please_select_an_end_time));
            hideDialog();
            return;
        }

        Date startDate = sdf.parse(startDateButton.getText().toString(), new ParsePosition(0));
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        String[] splitStartTimeString = startTimeButton.getText().toString().split(":");
        int startHour = Integer.parseInt(splitStartTimeString[0]);
        int startMinute = Integer.parseInt(splitStartTimeString[1]);
        startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        startCalendar.set(Calendar.MINUTE, startMinute);
        long startTime = startCalendar.getTimeInMillis();

        Date endDate = sdf.parse(endDateButton.getText().toString(), new ParsePosition(0));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        String[] splitTimeString = endTimeButton.getText().toString().split(":");
        int endHour = Integer.parseInt(splitTimeString[0]);
        int endMinute = Integer.parseInt(splitTimeString[1]);
        endCalendar.set(Calendar.HOUR_OF_DAY, endHour);
        endCalendar.set(Calendar.MINUTE, endMinute);
        long endTime = endCalendar.getTimeInMillis();

        if (endTime < startTime){
            Utilities.displayToast(context, getString(R.string.start_time_after_end_time));
            hideDialog();
            return;
        }

        if (endTime == startTime){
            Utilities.displayToast(context, getString(R.string.start_time_same_as_end_time));
            hideDialog();
            return;
        }

        MyLog.d(TAG, "Start: " + startTime + " | End: " + endTime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            JSONObject jObj = new JSONObject();

            JSONObject jData = new JSONObject();

            // TODO change later (1 Uncategorized / 2 Development / 3 Production)
            jData.put("measurement_category_id", 2);

            jData.put("setup_id", selectedSetupId);
            jData.put("user_id", selectedUserId);
            jData.put("name_en", name);
            jData.put("started_at", sdf.format(startTime));
            jData.put("stopped_at", sdf.format(endTime));

            jObj.put("data", jData);

            jsonMeasurement = jObj.toString();

            createNewMeasurement();
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    private void createNewMeasurement(){

        String tag_string_req = "create_new_measurement";

        pDialog.setMessage(getString(R.string.creating_a_new_measurement_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/");

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    hideDialog();

                    finish();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return jsonMeasurement.getBytes(StandardCharsets.UTF_8);
            }

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