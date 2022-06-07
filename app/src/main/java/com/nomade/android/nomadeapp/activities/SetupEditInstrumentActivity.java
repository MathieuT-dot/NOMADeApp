package com.nomade.android.nomadeapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.HexadecimalInputFilter;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.ParameterOption;
import com.nomade.android.nomadeapp.setups.Setup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SetupEditInstrumentActivity
 *
 * Activity to edit an Instrument
 */
public class SetupEditInstrumentActivity extends AppCompatActivity {

    private static final String TAG = "SetupEditInstrumentActi";
    private Context context = this;

    private ProgressDialog pDialog;
    private SharedPreferences setupSharedPreferences;
    private SharedPreferences.Editor setupEditor;

    private AlertDialog.Builder builder;
    private LayoutInflater inflater;

    private Instrument instrument;
    private Setup setup;

    private ScrollView setParametersScrollView;
    private LinearLayout setParametersLinearLayout;

    private Button cancelButton;
    private Button acceptButton;

    private EditText instrumentNameEditText;
    private EditText instrumentDescriptionEditText;
    private TextView instrumentIdTextView;
    private TextView instrumentTypeIdTextView;
    private TextView instrumentTypeNameTextView;
    private TextView instrumentSetupIdTextView;
    private TextView parametersTextView;

    private EditText[] parameterValueEditTexts;
    private Spinner[] parameterValueSpinners;
    private CheckBox[] parameterValueCheckBoxes;

    private ArrayList<String> d4SpinnerArrayList;
    private ArrayList<Integer> d4IdArrayList;

    private String jsonParameterInfoList;
    private ArrayList<Parameter> parameterInfoArrayList;

    boolean oasSoftwareInstrument = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_edit_instrument);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        builder = new AlertDialog.Builder(this);
        inflater = getLayoutInflater();

        setParametersScrollView = findViewById(R.id.set_parameters_scroll_view);
        setParametersLinearLayout = findViewById(R.id.set_parameters_linear_layout);

        instrumentNameEditText = findViewById(R.id.instrument_name_edit_text);
        instrumentDescriptionEditText = findViewById(R.id.instrument_description_edit_text);
        instrumentIdTextView = findViewById(R.id.instrument_id_text_view);
        instrumentTypeIdTextView = findViewById(R.id.instrument_type_id_text_view);
        instrumentTypeNameTextView = findViewById(R.id.instrument_type_name_text_view);
        instrumentSetupIdTextView = findViewById(R.id.instrument_setup_id_text_view);
        parametersTextView = findViewById(R.id.parameters_text_view);

        cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        });

        acceptButton = findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(v -> {
            if (checkUserInput()){
                Intent intent = new Intent();
                if (instrument != null){
                    intent.putExtra("EDITED_INSTRUMENT", instrument);
                }
                else {
                    MyLog.d(TAG, "Edited instrument is null");
                }

                setResult(RESULT_OK, intent);
                finish();
            }
        });

        if (savedInstanceState != null){
            instrument = savedInstanceState.getParcelable("STATE_INSTRUMENT");
            setup = savedInstanceState.getParcelable("STATE_SETUP");
        }
        else {
            instrument = getIntent().getParcelableExtra("INSTRUMENT");
            setup = getIntent().getParcelableExtra("SETUP");
        }

        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");
        parameterInfoArrayList = Utilities.parseJsonParameterList(context, jsonParameterInfoList);

        if (parameterInfoArrayList != null){
            requestType(instrument.getInstrumentTypeId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (instrument != null){
            if (instrument.isLocked() || setup.isLocked()){
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setIcon(R.drawable.ic_lock_light);

                instrumentNameEditText.setEnabled(false);
                instrumentDescriptionEditText.setEnabled(false);

                cancelButton.setText(R.string.close);
                acceptButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        MyLog.d(TAG, "onSaveInstanceState");

        instrument.setName(instrumentNameEditText.getText().toString());
        instrument.setDescription(instrumentDescriptionEditText.getText().toString());

        for (int i = 0; i < instrument.getParameterArrayList().size(); i++)
        {
            Parameter parameter = instrument.getParameterArrayList().get(i);

            switch (parameter.getDataType()){

                case 0: // Strict
                    if (parameterValueSpinners[i].isEnabled()){
                        parameter.setValue(parameter.getParameterOptionArrayList().get(parameterValueSpinners[i].getSelectedItemPosition()).getValue());
                        parameter.setValueDescription(parameter.getParameterOptionArrayList().get(parameterValueSpinners[i].getSelectedItemPosition()).getName());
                    }
                    break;

                case 1: // Integer
                    if (parameterValueEditTexts[i].isEnabled()){
                        try {
                            Float value = Float.parseFloat(parameterValueEditTexts[i].getText().toString());
                            parameter.setValue(value);
                        }
                        catch (NumberFormatException e){
                            parameter.setValue(null);
                        }
                    }
                    break;

                case 2: // Real
                    if (parameterValueEditTexts[i].isEnabled()){
                        try {
                            Float value = Float.parseFloat(parameterValueEditTexts[i].getText().toString());
                            parameter.setValue(value);
                        }
                        catch (NumberFormatException e){
                            parameter.setValue(null);
                        }
                    }
                    break;

                case 3: // Boolean
                    if (parameterValueCheckBoxes[i].isEnabled()){
                        if (parameterValueCheckBoxes[i].isChecked()){
                            parameter.setValue(1f);
                        }
                        else {
                            parameter.setValue(0f);
                        }
                    }
                    break;

                case 4: // List of all id's in the setup
                    if (parameterValueSpinners[i].isEnabled()){
                        parameter.setValue((float) d4IdArrayList.get(parameterValueSpinners[i].getSelectedItemPosition()));
                    }
                    break;

                case 5: // Half of a MAC address, represented in the database by a float equivalent
                    if (parameterValueEditTexts[i].isEnabled()){
                        String s = parameterValueEditTexts[i].getText().toString();
                        if (s.length() == 6) {
                            parameter.setValue(Utilities.macToFloat(s));
                        }
                        else {
                            parameter.setValue(null);
                        }
                    }
                    break;
            }
        }

        outState.putParcelable("STATE_INSTRUMENT", instrument);

        outState.putParcelable("STATE_SETUP", setup);

        super.onSaveInstanceState(outState);
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

            JSONArray jParameterArray = jInstrumentType.getJSONArray("parameters");
            for (int i = 0; i < jParameterArray.length(); i++){
                JSONObject jCurrentParameter = jParameterArray.getJSONObject(i);

                int parameterId = jCurrentParameter.getInt("parameter_id");

                Float parameterMin = null;
                Float parameterMax = null;

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

                for (Parameter parameter : instrument.getParameterArrayList()) {
                    if (parameter.getId() == parameterId){
                        parameter.setMin(parameterMin);
                        parameter.setMax(parameterMax);

                        for (int j = parameter.getParameterOptionArrayList().size() - 1; j >= 0; j--){
                            ParameterOption parameterOption = parameter.getParameterOptionArrayList().get(j);
                            if ((parameterMin != null && parameterOption.getValue().intValue() < parameterMin.intValue()) || (parameterMax != null && parameterMax.intValue() < parameterOption.getValue().intValue())) {
                                parameter.getParameterOptionArrayList().remove(j);
                            }
                        }
                        break;
                    }
                }
            }

            displayParameters();
        }
        catch (JSONException e){
            e.printStackTrace();
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    /**
     * Displays all the parameters for the specific type of the selected instrument.
     */
    private void displayParameters() {
        MyLog.d(TAG, "displayParameters");

        instrumentNameEditText.setText(String.format("%s", instrument.getName()));
        instrumentDescriptionEditText.setText(String.format("%s", instrument.getDescription()));

        if (instrument.getId() == 0){
            instrumentIdTextView.setText("/");
        }
        else {
            instrumentIdTextView.setText(String.format("%s", instrument.getId()));
        }

        instrumentTypeIdTextView.setText(String.format("%s", instrument.getInstrumentTypeId()));
        instrumentTypeNameTextView.setText(String.format("%s", instrument.getType().getName()));
        instrumentSetupIdTextView.setText(String.format("%s", instrument.getSetupId()));

        oasSoftwareInstrument = (instrument.getInstrumentTypeId() == 15 || instrument.getInstrumentTypeId() == 24);

        int parameterCount = instrument.getParameterArrayList().size();

        if (parameterCount > 0){
            parametersTextView.setText(getString(R.string.parameters_colon));
        }
        else {
            parametersTextView.setText(getString(R.string.no_parameters));
        }

        View[] parameterInflatedLayouts = new View[parameterCount];
        ImageView[] parameterInfoImageViews = new ImageView[parameterCount];
        TextView[] parameterNameTextViews = new TextView[parameterCount];
        parameterValueEditTexts = new EditText[parameterCount];
        parameterValueSpinners = new Spinner[parameterCount];
        parameterValueCheckBoxes = new CheckBox[parameterCount];

        for (int i = 0; i < parameterCount; i++)
        {
            Parameter parameter = instrument.getParameterArrayList().get(i);

            LayoutInflater layoutInflater = LayoutInflater.from(context);
            parameterInflatedLayouts[i] = layoutInflater.inflate(R.layout.linear_layout_parameter, null);

            parameterInfoImageViews[i] = parameterInflatedLayouts[i].findViewById(R.id.info_image_view);
            parameterInfoImageViews[i].setOnClickListener(v -> {
                View parameterInfoView = inflater.inflate(R.layout.linear_layout_parameter_details, null);

                ((TextView) parameterInfoView.findViewById(R.id.parameter_id_text_view)).setText(String.format("%s", parameter.getId()));
                ((TextView) parameterInfoView.findViewById(R.id.parameter_name_text_view)).setText(String.format("%s", parameter.getName()));
                ((TextView) parameterInfoView.findViewById(R.id.parameter_description_text_view)).setText(String.format("%s", parameter.getDescription()));
                ((TextView) parameterInfoView.findViewById(R.id.parameter_level_text_view)).setText(String.format("%s", parameter.getLevel()));
                ((TextView) parameterInfoView.findViewById(R.id.parameter_data_type_text_view)).setText(String.format("%s", parameter.getDataType()));
                ((TextView) parameterInfoView.findViewById(R.id.parameter_main_board_text_view)).setText(String.format("%s", parameter.getMainBoard()));

                TextView parameterMinimumTextView = parameterInfoView.findViewById(R.id.parameter_min_text_view);
                if (parameter.getDataType() == 1 && parameter.getMin() != null){
                    parameterMinimumTextView.setText(String.format("%s",parameter.getMin().intValue()));
                }
                else {
                    parameterMinimumTextView.setText(String.format("%s", parameter.getMin()));
                }

                TextView parameterMaximumTextView = parameterInfoView.findViewById(R.id.parameter_max_text_view);
                if (parameter.getDataType() == 1 && parameter.getMax() != null){
                    parameterMaximumTextView.setText(String.format("%s", parameter.getMax().intValue()));
                }
                else {
                    parameterMaximumTextView.setText(String.format("%s",parameter.getMax()));
                }

                TextView parameterDefaultValueTextView = parameterInfoView.findViewById(R.id.parameter_default_value_text_view);
                if (parameter.getDataType() == 1 && parameter.getDefaultValue() != null){
                    parameterDefaultValueTextView.setText(String.format("%s", parameter.getDefaultValue().intValue()));
                }
                else {
                    parameterDefaultValueTextView.setText(String.format("%s", parameter.getDefaultValue()));
                }

                TextView parameterValueTextView = parameterInfoView.findViewById(R.id.parameter_value_text_view);
                if (parameter.getDataType() == 1 && parameter.getDefaultValue() != null){
                    parameterValueTextView.setText(String.format("%s", parameter.getValue().intValue()));
                }
                else {
                    parameterValueTextView.setText(String.format("%s", parameter.getValue()));
                }

                TextView parameterValueDescriptionTextView = parameterInfoView.findViewById(R.id.parameter_value_description_text_view);
                if (parameter.getValueDescription() != null && !parameter.getValueDescription().equals("")) {
                    parameterValueDescriptionTextView.setText(String.format("%s", parameter.getValueDescription()));
                }
                else {
                    parameterValueDescriptionTextView.setText("/");
                }

                builder.setView(parameterInfoView)
                        .setCancelable(true)
                        .create()
                        .show();
            });

            parameterNameTextViews[i] = parameterInflatedLayouts[i].findViewById(R.id.parameter_name_text_view);
            parameterNameTextViews[i].setText(String.format("%s", parameter.getName()));

            parameterValueEditTexts[i] = parameterInflatedLayouts[i].findViewById(R.id.parameter_value_edit_text);
            parameterValueSpinners[i] = parameterInflatedLayouts[i].findViewById(R.id.option_spinner);
            parameterValueCheckBoxes[i] = parameterInflatedLayouts[i].findViewById(R.id.check_box);

            switch (parameter.getDataType())
            {
                case 0: // Strict
                    if (parameter.getParameterOptionArrayList().size() > 0){
                        ArrayList<String> spinnerArray = new ArrayList<>();
                        int selectionIndex = -1;
                        for (ParameterOption parameterOption : parameter.getParameterOptionArrayList()){
                            spinnerArray.add(parameterOption.getValue().intValue() + ": " + parameterOption.getName());
                            if (parameterOption.getValue().equals(parameter.getValue())){
                                selectionIndex = parameter.getParameterOptionArrayList().indexOf(parameterOption);
                            }
                        }
                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_padded, spinnerArray);
                        parameterValueSpinners[i].setAdapter(spinnerArrayAdapter);
                        parameterValueSpinners[i].setSelection(selectionIndex);
                    }

                    parameterValueEditTexts[i].setVisibility(View.GONE);
                    parameterValueSpinners[i].setVisibility(View.VISIBLE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueSpinners[i].setEnabled(false);
                    }
                    break;

                case 1: // Integer
                    parameterValueEditTexts[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    if (parameter.getValue() != null){
                        parameterValueEditTexts[i].setText(String.format("%s", parameter.getValue().intValue()));
                    }
                    else {
                        parameterValueEditTexts[i].setText(String.format("%s", parameter.getValue()));
                    }

                    parameterValueEditTexts[i].setVisibility(View.VISIBLE);
                    parameterValueSpinners[i].setVisibility(View.GONE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueEditTexts[i].setEnabled(false);
                    }
                    break;

                case 2: // Real
                    parameterValueEditTexts[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    parameterValueEditTexts[i].setText(String.format("%s", parameter.getValue()));

                    parameterValueEditTexts[i].setVisibility(View.VISIBLE);
                    parameterValueSpinners[i].setVisibility(View.GONE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueEditTexts[i].setEnabled(false);
                    }
                    break;

                case 3: // Boolean
                    parameterValueCheckBoxes[i].setChecked(parameter.getValue().equals(1f));

                    parameterValueEditTexts[i].setVisibility(View.GONE);
                    parameterValueSpinners[i].setVisibility(View.GONE);
                    parameterValueCheckBoxes[i].setVisibility(View.VISIBLE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueCheckBoxes[i].setEnabled(false);
                    }
                    break;

                case 4: // List of all id's in the setup
                    if (d4IdArrayList == null | d4SpinnerArrayList == null){
                        initD4ArrayLists();
                    }

                    int selectionIndex;
                    if (parameter.getValue() != null) {
                        selectionIndex = d4IdArrayList.indexOf(parameter.getValue().intValue());
                        if (selectionIndex == -1){
                            selectionIndex = 0;
                            parameter.setValue(0f);
                        }
                    }
                    else {
                        selectionIndex = 0;
                        parameter.setValue(0f);
                    }

                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_padded, d4SpinnerArrayList);
                    parameterValueSpinners[i].setAdapter(spinnerArrayAdapter);
                    parameterValueSpinners[i].setSelection(selectionIndex);

                    parameterValueEditTexts[i].setVisibility(View.GONE);
                    parameterValueSpinners[i].setVisibility(View.VISIBLE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueSpinners[i].setEnabled(false);
                    }
                    break;

                case 5: // Half of a MAC address, represented in the database by a float equivalent
                    InputFilter hexadecimalInputFilter = new HexadecimalInputFilter(true);
                    InputFilter lengthFilter = new InputFilter.LengthFilter(6);

                    parameterValueEditTexts[i].setFilters(new InputFilter[] {hexadecimalInputFilter, lengthFilter});
                    parameterValueEditTexts[i].setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                    if (parameter.getValue() != null) {
                        parameterValueEditTexts[i].setText(String.format("%s", Utilities.floatToMac(parameter.getValue())));
                    }
                    else {
                        MyLog.d(TAG, "displayParameters: MAC address is null");
                    }

                    parameterValueEditTexts[i].setVisibility(View.VISIBLE);
                    parameterValueSpinners[i].setVisibility(View.GONE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);

                    if (parameter.getLevel() == 0 || instrument.isLocked() || setup.isLocked()){
                        parameterValueEditTexts[i].setEnabled(false);
                    }
                    break;

                default:
                    parameterNameTextViews[i].append("\r\n" + getResources().getString(R.string.parameter_not_supported));

                    parameterValueEditTexts[i].setVisibility(View.GONE);
                    parameterValueSpinners[i].setVisibility(View.GONE);
                    parameterValueCheckBoxes[i].setVisibility(View.GONE);
            }

            setParametersLinearLayout.addView(parameterInflatedLayouts[i]);

            if (parameter.getLevel() == 0 && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_HIDE_UNCHANGEABLE_PARAMETERS, true)) {
                parameterInflatedLayouts[i].setVisibility(View.GONE);
            }
        }

        setParametersScrollView.fullScroll(ScrollView.FOCUS_UP);

        hideDialog();
    }

    /**
     * Puts all instruments of the setup in lists for data type 4 usage (software instrument).
     */
    private void initD4ArrayLists() {
        d4SpinnerArrayList = new ArrayList<>();
        d4IdArrayList = new ArrayList<>();

        d4SpinnerArrayList.add("0: " + getResources().getString(R.string.none));
        d4IdArrayList.add(0);

        for (Instrument currentInstrument : setup.getInstrumentArrayList()){
            if (currentInstrument.getId() != instrument.getId()){
                if (oasSoftwareInstrument){
                    if (
                            currentInstrument.getInstrumentTypeId() == 13 ||
                                    currentInstrument.getInstrumentTypeId() == 14 ||
                                    currentInstrument.getInstrumentTypeId() == 16 ||
                                    currentInstrument.getInstrumentTypeId() == 17 ||
                                    currentInstrument.getInstrumentTypeId() == 18 ||
                                    currentInstrument.getInstrumentTypeId() == 21
                    ){
                        d4SpinnerArrayList.add(currentInstrument.getId() + ": " + currentInstrument.getName());
                        d4IdArrayList.add(currentInstrument.getId());
                    }
                }
                else {
                    d4SpinnerArrayList.add(currentInstrument.getId() + ": " + currentInstrument.getName());
                    d4IdArrayList.add(currentInstrument.getId());
                }
            }
        }
    }

    /**
     * Checks the user input for the parameters to see if they are valid.
     *
     * @return true if all parameters are set to a valid value
     *         false if there are parameters with an invalid value
     */
    private boolean checkUserInput() {
        MyLog.d(TAG, "displayParametersForType");

        instrument.setName(instrumentNameEditText.getText().toString());
        instrument.setDescription(instrumentDescriptionEditText.getText().toString());

        Float slopeStart = null, slopeEnd = null;

        for (int i = 0; i < instrument.getParameterArrayList().size(); i++)
        {
            Parameter parameter = instrument.getParameterArrayList().get(i);

            String parameterDescription = parameter.getDescription();
            if (parameterDescription == null || parameterDescription.equals("null")) {
                parameterDescription = parameter.getName();
            }

            switch (parameter.getDataType())
            {
                case 0: // Strict
                    if (parameterValueSpinners[i].isEnabled()){
                        parameter.setValue(parameter.getParameterOptionArrayList().get(parameterValueSpinners[i].getSelectedItemPosition()).getValue());
                        parameter.setValueDescription(parameter.getParameterOptionArrayList().get(parameterValueSpinners[i].getSelectedItemPosition()).getName());
                    }
                    break;

                case 1: // Integer
                    if (parameterValueEditTexts[i].isEnabled()){
                        try {
                            Float value = Float.parseFloat(parameterValueEditTexts[i].getText().toString());

                            if (parameter.getMin() == null || value >= parameter.getMin()){
                                parameter.setValue(value);
                            }
                            else {
                                Utilities.displayToast(context, getString(R.string.parameter_value_greater, parameterDescription, Float.toString(parameter.getMin())));
                                return false;
                            }

                            if (parameter.getMax() == null || value <= parameter.getMax()){
                                parameter.setValue(value);
                            }
                            else {
                                Utilities.displayToast(context, getString(R.string.parameter_value_less, parameterDescription, Float.toString(parameter.getMax())));
                                return false;
                            }
                        }
                        catch (NumberFormatException e){
//                            Utilities.displayToast(context, getString(R.string.parameter_value_decimal, i+1), Toast.LENGTH_LONG);
                            parameter.setValue(null);
                        }
                    }
                    break;

                case 2: // Real
                    if (parameterValueEditTexts[i].isEnabled()){
                        try {
                            Float value = Float.parseFloat(parameterValueEditTexts[i].getText().toString());

                            if (parameter.getMin() == null || value >= parameter.getMin()){
                                parameter.setValue(value);
                            }
                            else {
                                Utilities.displayToast(context, getString(R.string.parameter_value_greater, parameterDescription, Float.toString(parameter.getMin())));
                                return false;
                            }

                            if (parameter.getMax() == null || value <= parameter.getMax()){
                                parameter.setValue(value);
                            }
                            else {
                                Utilities.displayToast(context, getString(R.string.parameter_value_less, parameterDescription, Float.toString(parameter.getMax())));
                                return false;
                            }
                        }
                        catch (NumberFormatException e){
//                            Utilities.displayToast(context, getString(R.string.parameter_value_decimal, i+1), Toast.LENGTH_LONG);
                            parameter.setValue(null);
                        }
                    }
                    break;

                case 3: // Boolean
                    if (parameterValueCheckBoxes[i].isChecked()){
                        parameter.setValue(1f);
                    }
                    else {
                        parameter.setValue(0f);
                    }
                    break;

                case 4: // List of all id's in the setup
                    if (parameterValueSpinners[i].isEnabled()){
                        parameter.setValue((float) d4IdArrayList.get(parameterValueSpinners[i].getSelectedItemPosition()));
                    }
                    break;

                case 5: // Half of a MAC address, represented in the database by a float equivalent
                    if (parameterValueEditTexts[i].isEnabled()) {
                        String s = parameterValueEditTexts[i].getText().toString();
                        if (s.length() != 6) {
                            // TODO add text resource
                            Utilities.displayToast(context, "Parameter " + parameterDescription + " should contain 6 characters");
                            return false;
                        }
                        else {
                            parameter.setValue(Utilities.macToFloat(s));
                        }
                    }
                    break;
            }

            if (parameter.getId() == Constants.SETUP_PRM_SAMPLERATE) {
                if (parameter.getValue() != null) {
                    if (parameter.getValue() != 10 && parameter.getValue() != 25 && parameter.getValue() != 50 && parameter.getValue() != 100) {
                        // TODO add text resource
                        Utilities.displayToast(context, "The samplerate must be 10 Hz, 25 Hz, 50 Hz or 100 Hz");
                        return false;
                    }
                }
                else {
                    // TODO add text resource
                    Utilities.displayToast(context, "The samplerate is required");
                    return false;
                }
            }

            if (parameter.getId() == Constants.SETUP_PRM_OAS_SLOPE_START) {
                slopeStart = parameter.getValue();
            }

            if (parameter.getId() == Constants.SETUP_PRM_OAS_SLOPE_END) {
                slopeEnd = parameter.getValue();
            }

            if (slopeStart != null && slopeEnd != null && slopeStart > slopeEnd) {
                Utilities.displayToast(context, getString(R.string.parameter_slope_start_greater_than_slope_end));
                return false;
            }
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

    @Override
    protected void onDestroy() {
        hideDialog();
        super.onDestroy();
    }
}
