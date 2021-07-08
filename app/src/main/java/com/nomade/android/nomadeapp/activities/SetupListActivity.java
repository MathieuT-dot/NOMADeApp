package com.nomade.android.nomadeapp.activities;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.SetupListAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Setup;

import com.kuleuven.android.kuleuvenlibrary.LibUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

/**
 * SetupListActivity
 *
 * Activity to display a list of Setups
 */
public class SetupListActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener, SetupListAdapter.SetupListAdapterCallback {

    private static final String TAG = "SetupListActivity";
    private final Context context = this;

    private ProgressDialog pDialog;

    private ListView setupListView;
    private TextView backgroundTextView;

    private static final String STATE_JSON_RESPONSE = "dataJsonResponse";
    private String jsonResponseString = "";

    private SharedPreferences setupSharedPreferences;
    private SharedPreferences.Editor setupEditor;

    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_DATA = 4;
    private static final int LAYOUT_LIST = 5;
    private static final int NO_SETUPS = 6;

    private boolean pickSetup = false;

    private int sortIndex = 0;
    private boolean reverseOrder = true;
    private static final String SORT_DIALOG = "dialogTagSort";
    private static final String ORDER_DIALOG = "dialogTagOrder";

    private static final String NAME = "name";
    private static final String EDIT_SETUP_NAME_DIALOG = "dialogTagEditSetupName";
    private static final String IGNORE_SAVED_INSTANCE_STATE = "ignoreSavedInstanceState";
    private boolean ignoreSavedInstanceState = false;

    private Setup selectedSetup;
    private ArrayList<Setup> setupArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_list);

        if (getCallingActivity() != null){
            pickSetup = true;
        }

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupListView = findViewById(R.id.setup_list_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        if (savedInstanceState != null && savedInstanceState.getBoolean(IGNORE_SAVED_INSTANCE_STATE)){
            savedInstanceState = null;
        }

        if (savedInstanceState != null){
            jsonResponseString = savedInstanceState.getString(STATE_JSON_RESPONSE);
            parseJsonSetupList(jsonResponseString);
        }
        else {
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSetupList();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                jsonResponseString = setupSharedPreferences.getString(Constants.API_SETUPS_, "");
                if (jsonResponseString != null && !jsonResponseString.equals("")){
                    parseJsonSetupList(jsonResponseString);
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

                    setupEditor.putString(Constants.API_SETUPS, response).apply();
                    jsonResponseString = response;
                    parseJsonSetupList(jsonResponseString);
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
     * Parses the JSON containing the list of setups.
     *
     * @param jsonSetupList JSON containing the list of setups
     */
    private void parseJsonSetupList(String jsonSetupList){

        try {
            JSONObject jObj = new JSONObject(jsonSetupList);

            JSONArray jSetupArray = jObj.getJSONArray("data");

            setupArrayList = new ArrayList<>();

            for (int i = 0; i < jSetupArray.length(); i++){

                JSONObject jCurrentSetup = jSetupArray.getJSONObject(i);

                int setupId = jCurrentSetup.getInt("id");
                int setupGroupId = jCurrentSetup.getInt("setup_group_id");

                String setupName = jCurrentSetup.getString("name_en");

//                String setupNameEn = jCurrentSetup.getString("name_en");
//                String setupNameNl = jCurrentSetup.getString("name_nl");
//                String setupNameFr = jCurrentSetup.getString("name_fr");
//                String setupDescriptionEn = jCurrentSetup.getString("description_en");
//                String setupDescriptionNl = jCurrentSetup.getString("description_nl");
//                String setupDescriptionFr = jCurrentSetup.getString("description_fr");
//
//                String setupName;
//                String setupDescription;
//
//                switch (Locale.getDefault().getLanguage()){
//                    case "nl":
//                        setupName = setupNameNl;
//                        setupDescription = setupDescriptionNl;
//                        break;
//
//                    case "fr":
//                        setupName = setupNameFr;
//                        setupDescription = setupDescriptionFr;
//                        break;
//
//                    default:
//                        setupName = setupNameEn;
//                        setupDescription = setupDescriptionEn;
//                }

                int setupHardwareIdentifier = jCurrentSetup.getInt("hw_identifier");
                int setupVersion = jCurrentSetup.getInt("version");
                boolean setupLocked = jCurrentSetup.getInt("locked") == 1;

//                String createdAt = jCurrentSetup.getString("created_at");
//                String updatedAt = jCurrentSetup.getString("updated_at");
//                String deletedAt = jCurrentSetup.getString("deleted_at");

                setupArrayList.add(new Setup(setupId, setupGroupId, setupName, setupHardwareIdentifier, setupVersion, setupLocked));
            }

            switch (sortIndex) {
                case 0:
                    Collections.sort(setupArrayList, new Setup.IdComparator());
                    break;

                case 1:
                    Collections.sort(setupArrayList, new Setup.NameComparator());
                    break;
            }

            if (reverseOrder) {
                Collections.reverse(setupArrayList);
            }

            SetupListAdapter adapter = new SetupListAdapter(context, setupArrayList, pickSetup);
            adapter.setCallback(this);

            setupListView.setAdapter(adapter);
            setupListView.setOnItemClickListener((parent, view, position, id) -> {
                Setup setup = setupArrayList.get(position);

                if (pickSetup){

                    if (Utilities.checkNetworkConnection(context)){
                        getSetupById(setup.getId());
                    }
                    else {
                        if (setupSharedPreferences.contains(Constants.API_SETUPS_ + setup.getId())){
                            String response = setupSharedPreferences.getString(Constants.API_SETUPS_ + setup.getId(), "");

                            if (response != null && !response.equals("")){
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("json_setup", response);
                                setResult(AppCompatActivity.RESULT_OK, returnIntent);
                                finish();
                            }
                            else {
                                handleLayoutChanges(TOAST_NO_INTERNET);
                            }
                        }
                        else {
                            handleLayoutChanges(TOAST_NO_INTERNET);
                        }
                    }
                }
                else {
                    if (Utilities.checkNetworkConnection(context)){
                        Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                        intent.putExtra("SETUP_ID", setup.getId());
                        startActivity(intent);
                    }
                    else {
                        if (setupSharedPreferences.contains(Constants.API_SETUPS_ + setup.getId())){
                            Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                            intent.putExtra("SETUP_ID", setup.getId());
                            startActivity(intent);
                        }
                        else {
                            handleLayoutChanges(TOAST_NO_INTERNET);
                        }
                    }
                }
            });

            setupListView.setOnItemLongClickListener((parent, view, position, id) -> {
                Setup setup = setupArrayList.get(position);

                if (Utilities.checkNetworkConnection(context)){
                    Intent intent = new Intent(context, SetupInfoActivity.class);
                    intent.putExtra("SETUP_ID", setup.getId());
                    startActivity(intent);
                    return true;
                }
                else {
                    if (setupSharedPreferences.contains(Constants.API_SETUPS_ + setup.getId())){
                        Intent intent = new Intent(context, SetupInfoActivity.class);
                        intent.putExtra("SETUP_ID", setup.getId());
                        startActivity(intent);
                        return true;
                    }
                    else {
                        handleLayoutChanges(TOAST_NO_INTERNET);
                    }
                }

                return true;
            });

            if (setupArrayList.size() == 0){
                handleLayoutChanges(NO_SETUPS);
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
        selectedSetup = setupArrayList.get(position);

        PopupMenu popupMenu = new PopupMenu(context, v);
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.menu_setup_popup, popupMenu.getMenu());
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_show_setup_info) {
                if (Utilities.checkNetworkConnection(context)) {
                    Intent intent = new Intent(context, SetupInfoActivity.class);
                    intent.putExtra("SETUP_ID", selectedSetup.getId());
                    context.startActivity(intent);
                    return true;
                } else {
                    if (setupSharedPreferences.contains(Constants.API_SETUPS_ + selectedSetup.getId())) {
                        Intent intent = new Intent(context, SetupInfoActivity.class);
                        intent.putExtra("SETUP_ID", selectedSetup.getId());
                        context.startActivity(intent);
                        return true;
                    } else {
                        Utilities.displayToast(context, context.getString(R.string.no_internet_access));
                    }
                }
            } else if (itemId == R.id.action_edit_setup) {
                if (Utilities.checkNetworkConnection(context)) {
                    Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                    intent.putExtra("SETUP_ID", selectedSetup.getId());
                    context.startActivity(intent);
                } else {
                    if (setupSharedPreferences.contains(Constants.API_SETUPS_ + selectedSetup.getId())) {
                        Intent intent = new Intent(context, BodyDragAndDropActivity.class);
                        intent.putExtra("SETUP_ID", selectedSetup.getId());
                        context.startActivity(intent);
                    } else {
                        Utilities.displayToast(context, context.getString(R.string.no_internet_access));
                    }
                }
            } else if (itemId == R.id.action_edit_setup_name) {
                SimpleFormDialog.build()
                        .title(R.string.setup)
                        .msg(R.string.edit_setup_name)
                        .fields(
                                Input.plain(NAME).hint(R.string.name).required().inputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).text(selectedSetup.getName())
                        )
                        .pos(R.string.save_changes)
                        .neg(R.string.cancel)
                        .show(this, EDIT_SETUP_NAME_DIALOG);
            }
            return true;
        });
    }

    /**
     * Creates and executes a request to get a setup with the given ID.     *
     * @param setupId ID of setup to get
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

                    setupEditor.putString(Constants.API_SETUPS_ + setupId, response).apply();
                    hideDialog();

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("json_setup", response);
                    setResult(AppCompatActivity.RESULT_OK, returnIntent);
                    finish();
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            hideDialog();
            Utilities.displayVolleyError(context, e);

            Intent returnIntent = new Intent();
            setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
            finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sort_and_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_list) {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getSetupList();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                String savedJson = setupSharedPreferences.getString("setup_list_json", "null");
                if (!savedJson.equals("null")) {
                    parseJsonSetupList(savedJson);
                    handleLayoutChanges(TOAST_OFFLINE_DATA);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        } else if (itemId == R.id.action_sort) {
            SimpleListDialog.build()
                    .title(R.string.select_how_to_sort)
                    .items(context, new int[]{R.string.setup_id, R.string.name})
                    .choiceMode(CustomListDialog.SINGLE_CHOICE)
                    .choiceMin(1)
                    .choicePreset(sortIndex)
                    .cancelable(false)
                    .show(this, SORT_DIALOG);
        }

        return true;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        switch (dialogTag) {

            case SORT_DIALOG:
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
                break;

            case ORDER_DIALOG:
                switch (which){
                    case BUTTON_POSITIVE:

                        reverseOrder = extras.getInt(SimpleListDialog.SELECTED_SINGLE_POSITION) == 1;

                        parseJsonSetupList(jsonResponseString);

                        return true;

                    case BUTTON_NEGATIVE:
                    case BUTTON_NEUTRAL:
                    case CANCELED:
                        return true;
                }
                break;

            case EDIT_SETUP_NAME_DIALOG:
                switch (which){
                    case BUTTON_POSITIVE:

                        pDialog.setMessage(getString(R.string.saving_changes_ellipsis));
                        showDialog();

                        String newName = extras.getString(NAME);

                        prepareSetup(newName);

                        return true;

                    case BUTTON_NEGATIVE:
                    case BUTTON_NEUTRAL:
                    case CANCELED:
                        return true;
                }
                break;
        }
        return false;
    }

    /**
     * Prepares an edit of a setup by putting the needed values in a JSON object.
     *
     * @param newName of the edited setup
     */
    private void prepareSetup(String newName) {
        try {
            JSONObject jData = new JSONObject();
            jData.put("id", selectedSetup.getId());
            jData.put("name_en", newName);
            jData.put("hw_identifier", selectedSetup.getHardwareIdentifier());
            jData.put("version", selectedSetup.getVersion());
            jData.put("locked", selectedSetup.isLocked());

            // TODO change later
            jData.put("setup_group_id", 1);

            // TODO change later (1 Uncategorized / 2 Development / 3 Production)
            jData.put("setup_category_id", 2);

            JSONObject jObj = new JSONObject();
            jObj.put("data", jData);

            updateSetup(jObj.toString());
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
    private void updateSetup(String submitString) {
        String tag_string_req = "update_setup";

        pDialog.setMessage(getString(R.string.creating_new_setup_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + selectedSetup.getId() + "/");
        MyLog.d("StringRequest", "Post body: " + submitString);

        StringRequest strReq = new StringRequest(
                Request.Method.PUT,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + selectedSetup.getId() + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, true);

                    ignoreSavedInstanceState = true;
                    hideDialog();
                    recreate();
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

    /**
     * Handles the layout changes depending on the provided layout ID.
     *
     * @param layout id determining the layout
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                setupListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                setupListView.setVisibility(View.GONE);
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
                setupListView.setVisibility(View.VISIBLE);
                break;

            case NO_SETUPS:
                setupListView.setVisibility(View.GONE);
                backgroundTextView.setText(getString(R.string.no_setups_available));
                backgroundTextView.setVisibility(View.VISIBLE);
                break;
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
}
