package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.ParameterOption;
import com.nomade.android.nomadeapp.setups.Type;

import com.kuleuven.android.kuleuvenlibrary.LibUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SetupAddInstrumentActivity
 *
 * Activity to add an Instrument to a Setup
 */
public class SetupAddInstrumentActivity extends AppCompatActivity {

    private static final String TAG = "SetupAddInstrumentActiv";
    private Context context = this;

    private ProgressDialog pDialog;
    private SharedPreferences setupSharedPreferences;
    private SharedPreferences.Editor setupEditor;

    private LinearLayout chooseTypeLinearLayout;
    private Spinner typesSpinner;
    private View typeDetailsInflatedLayout;

    private ArrayList<Type> typeInfoArrayList;
    private ArrayList<Parameter> parameterInfoArrayList;
    private int selectedTypeId = -1;
    private Type selectedType;
    private int setupId;

    private Instrument instrument;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_add_instrument);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        chooseTypeLinearLayout = findViewById(R.id.choose_type_linear_layout);
        typesSpinner = findViewById(R.id.types_spinner);

        Button addInstrumentButton = findViewById(R.id.add_instrument_button);
        addInstrumentButton.setOnClickListener(v -> {
            String jsonPostInstrument = Utilities.serializeInstrument(context, instrument);
            if (!jsonPostInstrument.equals(""))
            {
                postInstrument(jsonPostInstrument);
            }
        });

        setupId = getIntent().getIntExtra("SETUP_ID", -1);

        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        typeInfoArrayList = Utilities.parseJsonTypeList(context, jsonTypeInfoList);
        parameterInfoArrayList = Utilities.parseJsonParameterList(context, jsonParameterInfoList);

        if (typeInfoArrayList != null && parameterInfoArrayList != null){
            populateSpinnerWithTypes();
        }
    }

    /**
     * Populates the spinner with the types from the list.
     */
    private void populateSpinnerWithTypes() {
        MyLog.d(TAG, "populateSpinnerWithTypes");

        // Removes all irrelevant instrument types
        // ---------------------------------------
        // 10: Joystick (Penny & Giles)
        // 11: IMU
        // 12: GPS
        // 13: CAN Node (1US & 1IR)
        // 14: CAN Node (1US & 3IR)
        // 15: OAS (4 instruments)
        // 16: CAN Node (1US & 2IR)
        // 17: CAN Node (1IR)
        // 18: CAN Node (1US)
        // 19: Joystick (Dynamic Control DX2)
        // 20: Real Time Clock
        // 21: CAN Node (4IR)
        // 22: Joystick (Dynamic Control LYNX)
        // 23: Android Device
        // 24: OAS (8 instruments)
        // 25: Wheelchair
        // 26: Joystick profile
        // 27: AAMS
        // 28: Wheelchair P&G
        // 29: Wheelchair DX
        // 30: Body (Front)
        // 31: Body (Back)
        // 32: Body (Complete)
        // 33: Wireless IMU
        for (int i = typeInfoArrayList.size() - 1; i >= 0; i--) {
            int id = typeInfoArrayList.get(i).getId();
            if (id != 20 && id != 33) {
                typeInfoArrayList.remove(i);
            }
        }

        ArrayList<String> spinnerArray = new ArrayList<>();

        for (Type type : typeInfoArrayList){
            spinnerArray.add(type.getName());
        }

        if (spinnerArray.size() == 0){
            spinnerArray.add("/");
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_padded, spinnerArray);
        typesSpinner.setAdapter(spinnerArrayAdapter);

        typesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (typeInfoArrayList.get(position).getId() != selectedTypeId){
                    requestType(typeInfoArrayList.get(position).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        requestType(typeInfoArrayList.get(0).getId());
    }

    /**
     * Creates and executes a request to get more information about the selected type.
     *
     * @param typeId ID of the type of which more information is needed
     */
    private void requestType(final int typeId) {
        String tag_string_req = "get_type_" + typeId;

        pDialog.setMessage(getString(R.string.requesting_selected_type_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instrumentTypes/" + typeId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instrumentTypes/" + typeId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    setupEditor.putString(Constants.API_INSTRUMENT_TYPES_ + typeId, response).apply();
                    parseType(response);
                }, e -> {
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
     * Parses the JSON containing the info about one type.
     *
     * @param response JSON containing the info about one type
     */
    private void parseType(String response) {
        MyLog.d(TAG, "parseType");

        try {
            JSONObject jObj = new JSONObject(response);

            JSONObject jInstrumentType = jObj.getJSONObject("data");

            int id = jInstrumentType.getInt("id");

            ArrayList<Parameter> parameterArrayList = new ArrayList<>();

            JSONArray jParameterArray = jInstrumentType.getJSONArray("parameters");
            for (int i = 0; i < jParameterArray.length(); i++){
                JSONObject jCurrentParameter = jParameterArray.getJSONObject(i);

                int parameterId = jCurrentParameter.getInt("parameter_id");

                String parameterUuid = "";
                String parameterName = "";
                String parameterDescription = "";
                Integer parameterLevel = null;
                Integer parameterDataType = null;
                boolean parameterMainBoard = false;
                Float parameterMin = null;
                Float parameterMax = null;
                Float parameterDefaultValue = null;
                ArrayList<ParameterOption> parameterOptionArrayList = new ArrayList<>();

                for (Parameter parameter : parameterInfoArrayList) {
                    if (parameter.getId() == parameterId){
                        parameterUuid = parameter.getUuid();
                        parameterName = parameter.getName();
                        parameterDescription = parameter.getDescription();
                        parameterLevel = parameter.getLevel();
                        parameterDataType = parameter.getDataType();
                        parameterMainBoard = parameter.getMainBoard();
                        parameterMin = parameter.getMin();
                        parameterMax = parameter.getMax();
                        parameterDefaultValue = parameter.getDefaultValue();
                        parameterOptionArrayList.addAll(parameter.getParameterOptionArrayList());
                        break;
                    }
                }

                try {
                    parameterMin = BigDecimal.valueOf(jCurrentParameter.getDouble("min")).floatValue();
                }
                catch (JSONException e) {
//                    e.printStackTrace();
                }

                try {
                    parameterMax = BigDecimal.valueOf(jCurrentParameter.getDouble("max")).floatValue();
                }
                catch (JSONException e) {
//                    e.printStackTrace();
                }

                Float parameterValue;
                try {
                    parameterValue = BigDecimal.valueOf(jCurrentParameter.getDouble("value")).floatValue();
                }
                catch (JSONException e){
//                    e.printStackTrace();
                    parameterValue = parameterDefaultValue;
                }

                for (int j = parameterOptionArrayList.size() - 1; j >= 0; j--){
                    ParameterOption parameterOption = parameterOptionArrayList.get(j);
                    if ((parameterMin != null && parameterOption.getValue().intValue() < parameterMin.intValue()) || (parameterMax != null && parameterMax.intValue() < parameterOption.getValue().intValue())) {
                        parameterOptionArrayList.remove(j);
                    }
                }

                parameterArrayList.add(new Parameter(parameterId, parameterUuid, parameterName, parameterDescription, parameterLevel, parameterDataType, parameterMainBoard, parameterMin, parameterMax, parameterDefaultValue, parameterValue, parameterOptionArrayList));
            }

            for (Type type : typeInfoArrayList){
                if (id == type.getId()){
                    type.setParameterArrayList(parameterArrayList);
                    break;
                }
            }

            displaySelectedTypeDetails();
        }
        catch (JSONException e){
            e.printStackTrace();
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    /**
     * Displays the details about the selected type.
     */
    private void displaySelectedTypeDetails() {
        MyLog.d(TAG, "displaySelectedTypeDetails");

        if (typeDetailsInflatedLayout != null){
            chooseTypeLinearLayout.removeView(typeDetailsInflatedLayout);
        }

        int selectedTypePosition = typesSpinner.getSelectedItemPosition();
        selectedType = typeInfoArrayList.get(selectedTypePosition);
        selectedTypeId = selectedType.getId();

        instrument = new Instrument(0, selectedTypeId, selectedType.getName(), selectedType.getDescription(), false, setupId, selectedType, selectedType.getParameterArrayList());

        boolean boolX = false;
        boolean boolY = false;

        for (Parameter parameter : instrument.getParameterArrayList()){
            switch (parameter.getId()){
                case 1:
                    instrument.setLocationX(parameter.getValue());
                    boolX = true;
                    break;

                case 2:
                    instrument.setLocationY(parameter.getValue());
                    boolY = true;
                    break;

                case 3:
                    instrument.setRotation(parameter.getValue());
                    break;
            }
        }

        instrument.setLocatable(boolX && boolY);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        typeDetailsInflatedLayout = layoutInflater.inflate(R.layout.linear_layout_type_details, null);

        TextView typeIdTextView = typeDetailsInflatedLayout.findViewById(R.id.type_id_text_view);
        typeIdTextView.setText(String.format("%s", instrument.getType().getId()));
        TextView typeNameTextView = typeDetailsInflatedLayout.findViewById(R.id.type_name_text_view);
        typeNameTextView.setText(String.format("%s", instrument.getType().getName()));
        TextView typeCategoryTextView = typeDetailsInflatedLayout.findViewById(R.id.type_category_text_view);
        typeCategoryTextView.setText(String.format("%s", instrument.getType().getCategory()));
        TextView typeDescriptionTextView = typeDetailsInflatedLayout.findViewById(R.id.type_description_text_view);
        typeDescriptionTextView.setText(String.format("%s", instrument.getType().getDescription()));

        chooseTypeLinearLayout.addView(typeDetailsInflatedLayout);

        hideDialog();
    }

    /**
     * Creates and executes a request to post a new instrument to the server.
     *
     * @param jsonString JSON containing the info to create a new instrument
     */
    private void postInstrument(final String jsonString) {
        String tag_string_req = "post_instrument";

        pDialog.setMessage(getString(R.string.adding_new_instrument_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/");
        MyLog.d("StringRequest", "Post body: " + jsonString);

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    try {
                        JSONObject jObj = new JSONObject(response);
                        JSONObject jInstrument = jObj.getJSONObject("data");

                        int newInstrumentId = jInstrument.getInt("id");
                        instrument.setId(newInstrumentId);

                        Intent intent = new Intent();
                        intent.putExtra("NEW_INSTRUMENT", instrument);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                    catch (JSONException e){
                        MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                        Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                    }

                    hideDialog();
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
                return jsonString == null ? null : jsonString.getBytes(StandardCharsets.UTF_8);
            }

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
