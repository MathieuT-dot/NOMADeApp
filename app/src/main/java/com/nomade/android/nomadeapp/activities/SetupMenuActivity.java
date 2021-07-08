package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.Setup;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

/**
 * SetupMenuActivity
 *
 * Activity with the menu for the Setups
 */
public class SetupMenuActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "SetupMenuActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private Button setupListButton;
    private Button newSetupButton;
    private Button newSetupFromTemplateButton;

    private String name = "";
    private int hardwareId = -1;
    private int version = -1;
    private String what = "";
    private int newSetupId = -1;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;
    private String jsonTemplateSetup;

    public static final int PICK_SETUP_REQUEST = 1;
    private ArrayList<TemplateInstrument> templateInstrumentArrayList = new ArrayList<>();
    private int templateInstrumentIndex = 0;
    private boolean postOrPutError = false;

    private static final String NAME = "name";
    private static final String HARDWARE_ID = "hardwareID";
    private static final String VERSION = "version";

    private static final String NEW_SETUP_DIALOG = "dialogTagNewSetup";
    private static final String NEW_SETUP_FROM_TEMPLATE_DIALOG = "dialogTagNewSetupFromTemplate";
    private static final String COULD_NOT_POST_DIALOG = "dialogTagCouldNotPost";
    private static final String COULD_NOT_PUT_DIALOG = "dialogTagCouldNotPut";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_menu);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupListButton = findViewById(R.id.view_setup_list_button);
        setupListButton.setOnClickListener(v -> {
            enableButtons(false);
            Intent intent = new Intent(context, SetupListActivity.class);
            startActivity(intent);
        });

        newSetupButton = findViewById(R.id.new_setup_button);
        newSetupButton.setOnClickListener(v -> {
            enableButtons(false);

            what = NEW_SETUP_DIALOG;

            SimpleFormDialog.build()
                    .title(R.string.create_new_setup)
                    .msg(R.string.please_provide_details)
                    .fields(
                            Input.plain(NAME).hint(R.string.name).required().inputType(InputType.TYPE_CLASS_TEXT).min(5),
                            Input.plain(HARDWARE_ID).hint(R.string.hardware_identifier).required().inputType(InputType.TYPE_CLASS_NUMBER),
                            Input.plain(VERSION).hint(R.string.version).required().inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .pos(R.string.create)
                    .neg(R.string.cancel)
                    .show(this, NEW_SETUP_DIALOG);
        });

        newSetupFromTemplateButton = findViewById(R.id.new_setup_from_template_button);
        newSetupFromTemplateButton.setOnClickListener(v -> {
            enableButtons(false);

            what = NEW_SETUP_FROM_TEMPLATE_DIALOG;

            SimpleFormDialog.build()
                    .title(R.string.create_new_setup_template)
                    .msg(R.string.please_provide_details_template)
                    .fields(
                            Input.plain(NAME).hint(R.string.name).required().inputType(InputType.TYPE_CLASS_TEXT).min(5),
                            Input.plain(HARDWARE_ID).hint(R.string.hardware_identifier).required().inputType(InputType.TYPE_CLASS_NUMBER),
                            Input.plain(VERSION).hint(R.string.version).required().inputType(InputType.TYPE_CLASS_NUMBER)
                    )
                    .pos(R.string.pick_setup_template)
                    .neg(R.string.cancel)
                    .show(this, NEW_SETUP_FROM_TEMPLATE_DIALOG);
        });

        showOrHideMenuButtons();

        getInstrumentTypes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableButtons(true);
    }

    /**
     * Shows or hides menu buttons based on the permissions of the user
     */
    private void showOrHideMenuButtons() {
        SharedPreferences permissionsSharedPreferences = getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE);

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_INDEX, false)) {
            setupListButton.setVisibility(View.VISIBLE);
        } else {
            setupListButton.setVisibility(View.GONE);
        }

        if (permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false)) {
            newSetupButton.setVisibility(View.VISIBLE);
            newSetupFromTemplateButton.setVisibility(View.VISIBLE);
        } else {
            newSetupButton.setVisibility(View.GONE);
            newSetupFromTemplateButton.setVisibility(View.GONE);
        }
    }

    /**
     * Gets a list with instrument types from the server and updates it in the shared preferences
     * for future usage.
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
                    jsonTypeInfoList = response;
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
     * Gets a list with instrument parameters from the server and updates it in the shared
     * preferences for future usage.
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
                    jsonParameterInfoList = response;
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
     * Enables or disables all buttons in the SetupMenuActivity
     *
     * @param b enables all buttons if true
     *          disables all buttons if false
     */
    private void enableButtons(boolean b) {
        setupListButton.setEnabled(b);
        newSetupButton.setEnabled(b);
        newSetupFromTemplateButton.setEnabled(b);
    }

    /**
     * Prepares a new setup by putting the needed values in a JSON object.
     *
     * @param name of the new setup
     * @param hardwareId of the new setup
     * @param version of the new setup
     */
    private void prepareNewSetup(String name, Integer hardwareId, Integer version) {
        try {
            JSONObject jData = new JSONObject();
            jData.put("name_en", name);
            jData.put("hw_identifier", hardwareId);
            jData.put("version", version);
            jData.put("locked", false);

            // TODO change later
            jData.put("setup_group_id", 1);

            // TODO change later (1 Uncategorized / 2 Development / 3 Production)
            jData.put("setup_category_id", 2);

            JSONObject jObj = new JSONObject();
            jObj.put("data", jData);

            postNewSetup(jObj.toString());
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    /**
     * Creates and executes a request to post a new setup to the server.
     *
     * @param submitString JSON containing the needed info to create a new setup
     */
    private void postNewSetup(String submitString) {
        String tag_string_req = "create_setup";

        pDialog.setMessage(getString(R.string.creating_new_setup_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/");
        MyLog.d("StringRequest", "Post body: " + submitString);

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);

                    try {
                        JSONObject jObj = new JSONObject(response);

                        JSONObject jSetup = jObj.getJSONObject("data");

                        newSetupId = jSetup.getInt("id");

                        if (NEW_SETUP_DIALOG.equals(what)) {
                            hideDialog();
                            Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                            intent.putExtra("SETUP_ID", newSetupId);
                            startActivity(intent);
                        }
                        if (NEW_SETUP_FROM_TEMPLATE_DIALOG.equals(what)) {
                            createListOfInstrumentsToPost();
                        }
                    }
                    catch (JSONException e){
                        MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                        Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                    }
                }, e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
                    Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
                    hideDialog();
                }) {

            // replaces getParams with getBody
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            // replaces getParams with getBodyContentType
            @Override
            public byte[] getBody() {
                return submitString == null ? null : submitString.getBytes(StandardCharsets.UTF_8);
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

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (NEW_SETUP_DIALOG.equals(dialogTag)) {

            enableButtons(true);

            switch (which) {
                case BUTTON_POSITIVE:
                    name = extras.getString(NAME);
                    hardwareId = Integer.parseInt(extras.getString(HARDWARE_ID));
                    version = Integer.parseInt(extras.getString(VERSION));

                    MyLog.d(TAG, "New setup | Name: " + name + ", Hardware ID: " + hardwareId + ", Version: " + version);

                    showDialog();
                    prepareNewSetup(name, hardwareId, version);

                    return true;
                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }
        if (NEW_SETUP_FROM_TEMPLATE_DIALOG.equals(dialogTag)) {

            enableButtons(true);

            switch (which) {
                case BUTTON_POSITIVE:
                    name = extras.getString(NAME);
                    hardwareId = Integer.parseInt(extras.getString(HARDWARE_ID));
                    version = Integer.parseInt(extras.getString(VERSION));

                    MyLog.d(TAG, "New setup from template | Name: " + name + ", Hardware ID: " + hardwareId + ", Version: " + version);

                    pickSetup();

                    return true;
                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }
        if (COULD_NOT_POST_DIALOG.equals(dialogTag)) {
            switch (which){
                case BUTTON_POSITIVE:
                    templateInstrumentIndex = 0;
                    postOrPutError = false;
                    postInstrumentsFromTemplate();
                    return true;
                case BUTTON_NEGATIVE:
                    createListOfInstrumentsToPut();
                    return true;
            }
        }
        if (COULD_NOT_PUT_DIALOG.equals(dialogTag)) {
            switch (which){
                case BUTTON_POSITIVE:
                    templateInstrumentIndex = 0;
                    postOrPutError = false;
                    putInstrumentsFromTemplate();
                    return true;
                case BUTTON_NEGATIVE:
                    Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                    intent.putExtra("SETUP_ID", newSetupId);
                    startActivity(intent);
                    return true;
            }
        }

        return false;
    }

    /**
     * Creates and starts an intent to select a setup.
     */
    private void pickSetup() {
        Intent pickSetupIntent = new Intent(context, SetupListActivity.class);
        startActivityForResult(pickSetupIntent, PICK_SETUP_REQUEST);
    }

    /**
     * Creates a list of TemplateInstrument containing the needed info to add the instruments
     * from the template setup to the new setup.
     */
    private void createListOfInstrumentsToPost() {
        Setup templateSetup = Utilities.parseJsonSetup(context, jsonTemplateSetup, jsonTypeInfoList, jsonParameterInfoList);

        templateInstrumentIndex = 0;
        postOrPutError = false;
        templateInstrumentArrayList.clear();

        if (templateSetup != null) {
            for (Instrument instrument : templateSetup.getInstrumentArrayList()) {
                templateInstrumentArrayList.add(new TemplateInstrument(instrument));
            }
        }

        if (templateInstrumentArrayList.size() > 0) {
            postInstrumentsFromTemplate();
        }
    }

    /**
     * Posts the new instruments based on the template to the server, this method calls itself to
     * iterate through the list of instruments to add.
     */
    private void postInstrumentsFromTemplate() {

        while (templateInstrumentIndex < templateInstrumentArrayList.size() && templateInstrumentArrayList.get(templateInstrumentIndex).isPosted()) {
            templateInstrumentIndex++;
        }

        if (templateInstrumentIndex == templateInstrumentArrayList.size()) {

            hideDialog();

            if (!postOrPutError) {
                createListOfInstrumentsToPut();
            }
            else {
                SimpleDialog.build()
                        .title(R.string.adding_failed)
                        .msg(R.string.couldnt_add_all_instruments)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .show(this, COULD_NOT_POST_DIALOG);
            }

        }
        else {
            String tag_string_req = "post_instrument_from_template";

            pDialog.setMessage(getString(R.string.adding_instruments_template));
            showDialog();

            MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/");
            MyLog.d("StringRequest", "Post body: " + (templateInstrumentArrayList.get(templateInstrumentIndex) == null ? null : templateInstrumentArrayList.get(templateInstrumentIndex).getJsonInstrument().getBytes(StandardCharsets.UTF_8)));

            StringRequest strReq = new StringRequest(
                    Request.Method.POST,
                    PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/",
                    response -> {
                        LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                        templateInstrumentArrayList.get(templateInstrumentIndex).setPosted(true);
                        templateInstrumentArrayList.get(templateInstrumentIndex).setNewId(extractNewId(response));
                        templateInstrumentIndex++;
                        postInstrumentsFromTemplate();
                    }, e -> {
                MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
                Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
                hideDialog();

                postOrPutError = true;

                templateInstrumentIndex++;
                postInstrumentsFromTemplate();
            }) {

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return templateInstrumentArrayList.get(templateInstrumentIndex) == null ? null : templateInstrumentArrayList.get(templateInstrumentIndex).getJsonInstrument().getBytes(StandardCharsets.UTF_8);
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
    }

    /**
     * Creates a list of TemplateInstrument containing the needed info to update the instruments
     * from the template setup in the new setup.
     */
    private void createListOfInstrumentsToPut() {

        templateInstrumentIndex = 0;
        postOrPutError = false;
        boolean updateNeeded = false;

        for (TemplateInstrument templateInstrument : templateInstrumentArrayList) {

            updateNeeded = false;

            for (Parameter parameter : templateInstrument.getInstrument().getParameterArrayList()) {

                if (parameter.getDataType() == 4) {

                    for (TemplateInstrument templateInstrumentIds : templateInstrumentArrayList) {

                        if (parameter.getValue() == (float) templateInstrumentIds.getOldId()) {

                            parameter.setValue((float) templateInstrumentIds.getNewId());
                            updateNeeded = true;
                            break;
                        }

                    }

                }

            }

            if (updateNeeded) {
                templateInstrument.updateJson();
            }
            else {
                templateInstrument.setPutted(true);
            }

        }

        putInstrumentsFromTemplate();
    }

    /**
     * Puts the updated instruments based on the template to the server, this method calls itself to
     * iterate through the list of instruments to update.
     */
    private void putInstrumentsFromTemplate() {

        while (templateInstrumentIndex < templateInstrumentArrayList.size() && templateInstrumentArrayList.get(templateInstrumentIndex).isPutted()) {
            templateInstrumentIndex++;
        }

        if (templateInstrumentIndex == templateInstrumentArrayList.size()) {

            hideDialog();

            if (!postOrPutError) {
                Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                intent.putExtra("SETUP_ID", newSetupId);
                startActivity(intent);
            }
            else {
                SimpleDialog.build()
                        .title(R.string.updating_failed)
                        .msg(R.string.couldnt_update_all_instruments)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .show(this, COULD_NOT_PUT_DIALOG);
            }

        }
        else {
            String tag_string_req = "upload_para_value";

            pDialog.setMessage(getString(R.string.saving_changes_ellipsis));
            showDialog();

            MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/" + templateInstrumentArrayList.get(templateInstrumentIndex).getNewId() + "/");
            MyLog.d("StringRequest", "Put body: " + (templateInstrumentArrayList.get(templateInstrumentIndex) == null ? null : templateInstrumentArrayList.get(templateInstrumentIndex).getJsonInstrument().getBytes(StandardCharsets.UTF_8)));

            StringRequest strReq = new StringRequest(
                    Request.Method.PUT,
                    PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "instruments/" + templateInstrumentArrayList.get(templateInstrumentIndex).getNewId() + "/",
                    response -> {
                        LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);
                        templateInstrumentArrayList.get(templateInstrumentIndex).setPutted(true);
                        templateInstrumentIndex++;
                        putInstrumentsFromTemplate();
                    }, e -> {
                MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
                Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
                hideDialog();

                postOrPutError = true;

                templateInstrumentIndex++;
                putInstrumentsFromTemplate();
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return templateInstrumentArrayList.get(templateInstrumentIndex) == null ? null : templateInstrumentArrayList.get(templateInstrumentIndex).getJsonInstrument().getBytes(StandardCharsets.UTF_8);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){

            if (requestCode == PICK_SETUP_REQUEST){
                jsonTemplateSetup = data.getStringExtra("json_setup");

                showDialog();
                prepareNewSetup(name, hardwareId, version);
            }
        }
    }

    /**
     * Exracts the instrument ID from an instrument JSON.
     * @param json to extract ID from
     * @return extracted instrument ID
     */
    public static int extractNewId(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            return jData.getInt("id");
        }
        catch (JSONException e){
            MyLog.w(TAG, "JSON Exception: " + e);
            return -1;
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

    @Override
    protected void onDestroy() {
        hideDialog();
        super.onDestroy();
    }

    /**
     * Class used to create new setups from a template.
     */
    private class TemplateInstrument {

        private int oldId = -1;
        private int newId = -1;
        private Instrument instrument;
        private String jsonInstrument;
        private boolean posted = false;
        private boolean putted = false;

        public TemplateInstrument(Instrument instrument) {
            oldId = instrument.getId();
            this.instrument = instrument;
            this.instrument.setId(0);
            this.instrument.setSetupId(newSetupId);
            jsonInstrument = Utilities.serializeInstrument(context, this.instrument);
        }

        public void updateJson() {
            jsonInstrument = Utilities.serializeInstrument(context, this.instrument);
        }

        public int getOldId() {
            return oldId;
        }

        public void setOldId(int oldId) {
            this.oldId = oldId;
        }

        public int getNewId() {
            return newId;
        }

        public void setNewId(int newId) {
            this.newId = newId;
        }

        public Instrument getInstrument() {
            return instrument;
        }

        public void setInstrument(Instrument instrument) {
            this.instrument = instrument;
        }

        public String getJsonInstrument() {
            return jsonInstrument;
        }

        public void setJsonInstrument(String jsonInstrument) {
            this.jsonInstrument = jsonInstrument;
        }

        public boolean isPosted() {
            return posted;
        }

        public void setPosted(boolean posted) {
            this.posted = posted;
        }

        public boolean isPutted() {
            return putted;
        }

        public void setPutted(boolean putted) {
            this.putted = putted;
        }
    }
}
