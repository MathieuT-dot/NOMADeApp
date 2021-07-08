package com.nomade.android.nomadeapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.MeasurementAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.KeyValuePairsDialog;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Measurement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

/**
 * MeasurementListActivity
 *
 * This activity displays a list of measurements.
 */
public class MeasurementListActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener, MeasurementAdapter.MeasurementAdapterCallback {

    public static final String TAG = MeasurementListActivity.class.getSimpleName();
    private final Context context = this;
    private ProgressDialog pDialog;

    private SharedPreferences setupSharedPreferences;
    private SharedPreferences.Editor setupEditor;

    private ListView listView;
    private TextView backgroundTextView;
    private ArrayList<Measurement> measurementArrayList;

    private static final String STATE_JSON_RESPONSE = "dataJsonResponse";
    private String jsonResponseString = "";

    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_DATA = 4;
    private static final int LAYOUT_LIST = 5;
    private static final int NO_ITEMS = 6;

    private static final String EDIT_NAME_DIALOG = "dialogTagEditName";
    private static final String NAME = "name";
    private Measurement selectedMeasurement;
    private static final String IGNORE_SAVED_INSTANCE_STATE = "ignoreSavedInstanceState";
    private boolean ignoreSavedInstanceState = false;

    private boolean pickMeasurement = false;

    private int sortIndex = 0;
    private boolean reverseOrder = true;
    private static final String SORT_DIALOG = "dialogTagSort";
    private static final String ORDER_DIALOG = "dialogTagOrder";

    private SimpleDateFormat sdfDateAndTime;
    private SimpleDateFormat sdfDateAndTimeLaravel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_list);

        if (getCallingActivity() != null){
            pickMeasurement = true;
        }

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        listView = findViewById(R.id.list_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        if (savedInstanceState != null && savedInstanceState.getBoolean(IGNORE_SAVED_INSTANCE_STATE)){
            savedInstanceState = null;
        }

        if (savedInstanceState != null){
            jsonResponseString = savedInstanceState.getString(STATE_JSON_RESPONSE);
            parseMeasurementList(jsonResponseString);
        }
        else {
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getMeasurementList();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                jsonResponseString = setupSharedPreferences.getString("measurement_list_json", "");
                if (jsonResponseString != null && !jsonResponseString.equals("")){
                    parseMeasurementList(jsonResponseString);
                    handleLayoutChanges(TOAST_OFFLINE_DATA);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_JSON_RESPONSE, jsonResponseString);
        savedInstanceState.putBoolean(IGNORE_SAVED_INSTANCE_STATE, ignoreSavedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void getMeasurementList(){
        String tag_string_req = "get_measurement_list";

        pDialog.setMessage("Getting measurements...");
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    setupEditor.putString("measurement_list_json", response).apply();
                    jsonResponseString = response;
                    parseMeasurementList(jsonResponseString);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            hideDialog();
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

    private void parseMeasurementList(String jsonMeasurementList){

        try {
            JSONObject jObj = new JSONObject(jsonMeasurementList);

            JSONArray jMeasurementsArray = jObj.getJSONArray("data");

            measurementArrayList = new ArrayList<>();

            JSONObject currentMeasurement;

            for (int i = 0; i < jMeasurementsArray.length(); i++){

                currentMeasurement = jMeasurementsArray.getJSONObject(i);

                int measurementId = currentMeasurement.getInt("id");
                int measurementCategoryId = currentMeasurement.getInt("measurement_category_id");
                int setupId = currentMeasurement.getInt("setup_id");
                int userId = currentMeasurement.getInt("user_id");
                String name = currentMeasurement.getString("name_en");
                String description = currentMeasurement.getString("description_en");

                Integer max;
                try {
                    max = currentMeasurement.getInt("max");
                }
                catch (JSONException e){
                    max = null;
                }

                Integer count;
                try {
                    count = currentMeasurement.getInt("count");
                }
                catch (JSONException e){
                    count = null;
                }

                String stringStartTime = currentMeasurement.getString("started_at");
                String stringEndTime = currentMeasurement.getString("stopped_at");

                Long startTime = 0L;
                try {
                    startTime = sdfDateAndTimeLaravel.parse(stringStartTime).getTime();
                } catch (ParseException e) {
//                    e.printStackTrace();
                }

                Long endTime = 0L;
                try {
                    endTime = sdfDateAndTimeLaravel.parse(stringEndTime).getTime();
                } catch (ParseException e) {
//                    e.printStackTrace();
                }

                try {
                    stringStartTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringStartTime));
                } catch (ParseException e) {
//                    e.printStackTrace();
                }

                try {
                    stringEndTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringEndTime));
                } catch (ParseException e) {
//                    e.printStackTrace();
                }

                measurementArrayList.add(new Measurement(measurementId, measurementCategoryId, setupId, userId, name, description, max, count, startTime, endTime, stringStartTime, stringEndTime));
            }

            switch (sortIndex) {
                case 0:
                    Collections.sort(measurementArrayList, new Measurement.IdComparator());
                    break;

                case 1:
                    Collections.sort(measurementArrayList, new Measurement.StartTimeComparator());
                    break;

                case 2:
                    Collections.sort(measurementArrayList, new Measurement.NameComparator());
                    break;
            }

            if (reverseOrder) {
                Collections.reverse(measurementArrayList);
            }

            MeasurementAdapter adapter = new MeasurementAdapter(context, measurementArrayList, pickMeasurement);
            adapter.setCallback(this);

            listView.setAdapter(adapter);
            listView.setOnItemClickListener(((parent, view, position, id) -> {

                if (pickMeasurement){

                    Measurement measurement = measurementArrayList.get(position);

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("MEASUREMENT_ID", measurement.getMeasurementId());
                    setResult(AppCompatActivity.RESULT_OK, returnIntent);
                    finish();
                }
                else {
                    pDialog.setMessage("Getting measurement details...");
                    showDialog();

                    getMeasurementById(measurementArrayList.get(position).getMeasurementId());
                }
            }));

            listView.setOnItemLongClickListener((parent, view, position, id) -> {

                selectedMeasurement = measurementArrayList.get(position);

                SimpleFormDialog.build()
                        .title(R.string.measurement)
                        .msg(R.string.edit_measurement_name)
                        .fields(
                                Input.plain(NAME).hint(R.string.name).required().inputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).text(selectedMeasurement.getName())
                        )
                        .pos(R.string.save_changes)
                        .neg(R.string.cancel)
                        .show(this, EDIT_NAME_DIALOG);

                return true;
            });

            if (measurementArrayList.size() == 0){
                handleLayoutChanges(NO_ITEMS);
            }
            else {
                handleLayoutChanges(LAYOUT_LIST);
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        hideDialog();
    }

    /**
     * Opens a popup menu when the thee dots in the list are pressed.
     * @param position of the selected measurement
     * @param v view to which the popup menu needs to be anchored
     */
    @Override
    public void dotsPressed(int position, View v) {
        selectedMeasurement = measurementArrayList.get(position);

        PopupMenu popupMenu = new PopupMenu(context, v);
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.menu_measurement_popup, popupMenu.getMenu());
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_show_measurement_info) {
                if (pickMeasurement) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("MEASUREMENT_ID", selectedMeasurement.getMeasurementId());
                    setResult(AppCompatActivity.RESULT_OK, returnIntent);
                    finish();
                } else {
                    pDialog.setMessage("Getting measurement details...");
                    showDialog();
                    getMeasurementById(selectedMeasurement.getMeasurementId());
                }
            } else if (itemId == R.id.action_edit_measurement_name) {
                SimpleFormDialog.build()
                        .title(R.string.measurement)
                        .msg(R.string.edit_measurement_name)
                        .fields(
                                Input.plain(NAME).hint(R.string.name).required().inputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).text(selectedMeasurement.getName())
                        )
                        .pos(R.string.save_changes)
                        .neg(R.string.cancel)
                        .show(this, EDIT_NAME_DIALOG);
            }
            return true;
        });
    }

    /**
     * Handles the layout changes depending on the provided layout ID.
     *
     * @param layout id determining the layout
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                listView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                listView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_internet_access);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case TOAST_NO_INTERNET:
                Utilities.displayToast(context, getString(R.string.no_internet_access));
                break;

            case TOAST_OFFLINE_DATA:
                Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                break;

            case LAYOUT_LIST:
                backgroundTextView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                break;

            case NO_ITEMS:
                listView.setVisibility(View.GONE);
                backgroundTextView.setText(getString(R.string.no_measurements));
                backgroundTextView.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (EDIT_NAME_DIALOG.equals(dialogTag)){

            switch (which){
                case BUTTON_POSITIVE:

                    pDialog.setMessage(getString(R.string.saving_changes_ellipsis));
                    showDialog();

                    String newName = extras.getString(NAME);

                    prepareMeasurement(newName);

                    return true;

                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }

        if (SORT_DIALOG.equals(dialogTag)){

            switch (which){
                case BUTTON_POSITIVE:

                    sortIndex = extras.getInt(SimpleListDialog.SELECTED_SINGLE_POSITION);

                    SimpleListDialog.build()
                            .title(R.string.select_the_sorting_order)
                            .items(context, new int[]{R.string.ascending, R.string.descending})
                            .choiceMode(CustomListDialog.SINGLE_CHOICE)
                            .choiceMin(1)
                            .choicePreset(reverseOrder ? 1 : 0)
                            .cancelable(false)
                            .show(this, ORDER_DIALOG);

                    return true;

                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }

        if (ORDER_DIALOG.equals(dialogTag)){

            switch (which){
                case BUTTON_POSITIVE:

                    reverseOrder = extras.getInt(SimpleListDialog.SELECTED_SINGLE_POSITION) == 1;

                    parseMeasurementList(jsonResponseString);

                    return true;

                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }

        return false;
    }

    /**
     * Prepares a JSON string to be submitted to edit the measurement name.
     */
    private void prepareMeasurement(String newName){

        if (selectedMeasurement != null && newName != null && !newName.equals("")){

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            try {
                JSONObject jObj = new JSONObject();

                JSONObject jData = new JSONObject();

                jData.put("id", selectedMeasurement.getMeasurementId());

                // TODO change later (1 Uncategorized / 2 Development / 3 Production)
                jData.put("measurement_category_id", 2);

                jData.put("setup_id", selectedMeasurement.getSetupId());
                jData.put("user_id", selectedMeasurement.getUserId());
                jData.put("name_en", newName);
                jData.put("started_at", sdf.format(selectedMeasurement.getStartTime()));
                jData.put("stopped_at", sdf.format(selectedMeasurement.getEndTime()));

                jObj.put("data", jData);

                String jsonMeasurement = jObj.toString();

                updateMeasurement(jsonMeasurement);
            }
            catch (JSONException e){
                MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            }
        }
    }

    /**
     * Creates and executes a request to update a measurement based on a JSON string.
     *
     * @param jsonMeasurement JSON string containing the data to update a measurement
     */
    private void updateMeasurement(String jsonMeasurement){
        String tag_string_req = "create_new_measurement";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + selectedMeasurement.getMeasurementId() + "/");
        MyLog.d("StringRequest", "Post body: " + jsonMeasurement);

        StringRequest strReq = new StringRequest(
                Request.Method.PUT,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + selectedMeasurement.getMeasurementId() + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    ignoreSavedInstanceState = true;
                    hideDialog();

                    if (Utilities.checkNetworkConnection(context)) {
                        handleLayoutChanges(LAYOUT_LOADING);
                        getMeasurementList();
                    }
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
     * Creates and executes a request to get a measurement.
     */
    private void getMeasurementById(int measurementId){
        String tag_string_req = "get_measurement";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + measurementId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + measurementId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    parseMeasurement(response);
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
     * Parses the JSON string containing a measurement, it then extracts the setup ID and user ID and adds
     * that data to the existing list of Measurements.
     *
     * @param jsonMeasurement JSON string containing a measurement
     */
    private void parseMeasurement(String jsonMeasurement){

        try {
            JSONObject jObj = new JSONObject(jsonMeasurement);

            JSONObject jMeasurement = jObj.getJSONObject("data");

            int measurementId = jMeasurement.getInt("id");
            int measurementCategoryId = jMeasurement.getInt("measurement_category_id");
            int setupId = jMeasurement.getInt("setup_id");
            int userId = jMeasurement.getInt("user_id");
            String name = jMeasurement.getString("name_en");
            String description = jMeasurement.getString("description_en");

            Integer max;
            try {
                max = jMeasurement.getInt("max");
            }
            catch (JSONException e){
                max = null;
            }

            Integer count;
            try {
                count = jMeasurement.getInt("count");
            }
            catch (JSONException e){
                count = null;
            }

            String stringStartTime = jMeasurement.getString("started_at");
            String stringEndTime = jMeasurement.getString("stopped_at");

            Long startTime = 0L;
            try {
                startTime = sdfDateAndTimeLaravel.parse(stringStartTime).getTime();
            } catch (ParseException e) {
//                e.printStackTrace();
            }

            Long endTime = 0L;
            try {
                endTime = sdfDateAndTimeLaravel.parse(stringEndTime).getTime();
            } catch (ParseException e) {
//                e.printStackTrace();
            }

            try {
                stringStartTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringStartTime));
            } catch (ParseException e) {
//                e.printStackTrace();
            }

            try {
                stringEndTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringEndTime));
            } catch (ParseException e) {
//                e.printStackTrace();
            }

            Measurement measurement = new Measurement(measurementId, measurementCategoryId, setupId, userId, name, description, max, count, startTime, endTime, stringStartTime, stringEndTime);

            measurement.setUserId(userId);
            measurement.setSetupId(setupId);

            displayMeasurementDetails(measurement);
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    private void displayMeasurementDetails(Measurement measurement){

        // TODO fix layout
        KeyValuePairsDialog.build()
                .keys("ID: ", "Category ID: ", "Setup ID: ", "User ID: ", "Name: ", "Description: ", "Max: ", "Count: ", "Start millis: ", "End millis: ", "Start time: ", "End time: ")
                .values(measurement.getMeasurementId(), measurement.getMeasurementCategoryId(), measurement.getSetupId(), measurement.getUserId(), measurement.getName(), measurement.getDescription(), measurement.getMax(), measurement.getCount(), measurement.getStartTime(), measurement.getEndTime(), measurement.getStringStartTime(), measurement.getStringEndTime())
                .show(this);

        hideDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sort_and_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_list) {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getMeasurementList();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                String savedJson = setupSharedPreferences.getString("instrument_list_json", "null");
                if (savedJson != null && !savedJson.equals("null")) {
                    parseMeasurementList(savedJson);
                    handleLayoutChanges(TOAST_OFFLINE_DATA);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        } else if (itemId == R.id.action_sort) {
            SimpleListDialog.build()
                    .title(R.string.select_how_to_sort)
                    .items(context, new int[]{R.string.measurement_id, R.string.start_time, R.string.name})
                    .choiceMode(CustomListDialog.SINGLE_CHOICE)
                    .choiceMin(1)
                    .choicePreset(sortIndex)
                    .cancelable(false)
                    .show(this, SORT_DIALOG);
        }

        return true;
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