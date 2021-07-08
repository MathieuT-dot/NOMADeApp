package com.nomade.android.nomadeapp.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SubmittedQuestionnairesFilterActivity
 *
 * Activity to filter the submitted questionnaires in the SubmittedQuestionnairesActivity
 */
public class SubmissionsFilterActivity extends AppCompatActivity {

    private static final String TAG = "SubmissionsFilterActivi";
    private final Context context = this;

    private SwitchMaterial startDateSwitch;
    private Button startDateButton;

    private SwitchMaterial endDateSwitch;
    private Button endDateButton;

    private SwitchMaterial questionnaireSwitch;
    private Spinner questionnaireSpinner;

    private LinearLayout usernameLinearLayout;
    private SwitchMaterial usernameSwitch;
    private EditText usernameEditText;

    private Date startDate;
    private Date endDate;
    private int questionnaireId;
    private String username;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private String jsonResponseString = "";
    ArrayList<Questionnaire> questionnairesArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitted_questionnaires_filter);

        if (savedInstanceState != null){
            startDate = (Date) savedInstanceState.getSerializable("STATE_START_DATE");
            endDate = (Date) savedInstanceState.getSerializable("STATE_END_DATE");
        }

        questionnaireSwitch = findViewById(R.id.questionnaire_switch);
        questionnaireSpinner = findViewById(R.id.questionnaire_spinner);

        setTitle(getString(R.string.filter));

        showStartDatePickerDialog();

        showEndDatePickerDialog();

        initializeUsernameEditText();

        initialize();

        getAvailableQuestionnaires();

        finishActivity();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("STATE_START_DATE", startDate);
        outState.putSerializable("STATE_END_DATE", endDate);

        super.onSaveInstanceState(outState);
    }

    /**
     * Creates and shows the start date picker dialog.
     */
    private void showStartDatePickerDialog() {
        startDateButton = findViewById(R.id.start_date_button);
        startDateButton.setOnClickListener(v -> {
            // Create a new OnDateSetListener instance. This listener will be invoked when user click ok button in DatePickerDialog.
            DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                startDate = calendar.getTime();

                startDateButton.setText(sdf.format(startDate));
            };

            // Get current startDate (year, month and day).
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create the new DatePickerDialog instance.
            DatePickerDialog datePickerDialog = new DatePickerDialog(SubmissionsFilterActivity.this, onDateSetListener, year, month, day);

            // Set dialog icon and title.
            datePickerDialog.setTitle("Please select date.");

            // Popup the dialog.
            datePickerDialog.show();
        });

        startDateSwitch = findViewById(R.id.start_date_switch);
        startDateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> startDateButton.setEnabled(isChecked));
    }

    /**
     * Creates and shows the end date picker dialog.
     */
    private void showEndDatePickerDialog() {
        endDateButton = findViewById(R.id.end_date_button);
        endDateButton.setOnClickListener(v -> {
            // Create a new OnDateSetListener instance. This listener will be invoked when user click ok button in DatePickerDialog.
            DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                endDate = calendar.getTime();

                endDateButton.setText(sdf.format(endDate));
            };

            // Get current endDate (year, month and day).
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create the new DatePickerDialog instance.
            DatePickerDialog datePickerDialog = new DatePickerDialog(SubmissionsFilterActivity.this, onDateSetListener, year, month, day);

            // Set dialog icon and title.
            datePickerDialog.setTitle("Please select date.");

            // Popup the dialog.
            datePickerDialog.show();
        });

        endDateSwitch = findViewById(R.id.end_date_switch);
        endDateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> endDateButton.setEnabled(isChecked));
    }

    /**
     * Initializes the username edit text.
     */
    private void initializeUsernameEditText() {
        usernameLinearLayout = findViewById(R.id.username_linear_layout);
        if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)){
            usernameLinearLayout.setVisibility(View.VISIBLE);
        }
        else {
            usernameLinearLayout.setVisibility(View.GONE);
        }

        usernameEditText = findViewById(R.id.user_name_edit_text);

        usernameSwitch = findViewById(R.id.username_switch);
        usernameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> usernameEditText.setEnabled(isChecked));
    }

    /**
     * Initializes the different parts of the activity, if the filter was used before the previous
     * values are set, otherwise all filter options are disabled and set to default.
     */
    private void initialize() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);

        Calendar calendar = Calendar.getInstance();

        if (endDate == null){
            if (getIntent().hasExtra("END_DATE")){
                endDate = (Date) getIntent().getSerializableExtra("END_DATE");
            }
            else {
                calendar.set(year, month, dayOfMonth);
                endDate = calendar.getTime();
            }
        }

        endDateButton.setText(sdf.format(endDate));

        if (startDate == null){
            if (getIntent().hasExtra("START_DATE")){
                startDate = (Date) getIntent().getSerializableExtra("START_DATE");
            }
            else {
                calendar.add(Calendar.MONTH, -1);
                startDate = calendar.getTime();
            }
        }

        startDateButton.setText(sdf.format(startDate));

        if (getIntent().hasExtra("QUESTIONNAIRE_ID")){
            questionnaireId = getIntent().getIntExtra("QUESTIONNAIRE_ID", -1);
        }

        if (getIntent().hasExtra("USERNAME")){
            usernameEditText.setText(getIntent().getStringExtra("USERNAME"));
        }

        startDateSwitch.setChecked(getIntent().getBooleanExtra("START_DATE_IS_ENABLED", false));
        endDateSwitch.setChecked(getIntent().getBooleanExtra("END_DATE_IS_ENABLED", false));
        questionnaireSwitch.setChecked(getIntent().getBooleanExtra("QUESTIONNAIRE_IS_ENABLED", false));
        usernameSwitch.setChecked(getIntent().getBooleanExtra("USERNAME_IS_ENABLED", false));
    }

    /**
     * Creates and executes a request to get the available questionnaires.
     */
    private void getAvailableQuestionnaires() {
        String tag_string_req = "req_questionnaire";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).edit().putString(Constants.API_QUESTIONNAIRES, response).apply();
                    jsonResponseString = response;
                    parseJsonResponse(jsonResponseString);
                },
                e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Parses the JSON with available questionnaires and displays them in a expandable list.
     *
     * @param response JSON with available questionnaires
     */
    private void parseJsonResponse(String response){

        questionnairesArrayList = new ArrayList<>();

        try{

            JSONObject jObj = new JSONObject(response);

            JSONArray jDataArray = jObj.getJSONArray("data");

            for (int i = jDataArray.length() - 1; i >= 0; i--){

                JSONObject jQuestionnaire = jDataArray.getJSONObject(i);

                int id = jQuestionnaire.getInt("id");
                int groupId = jQuestionnaire.getInt("questionnaire_group_id");
                int version = jQuestionnaire.getInt("version");

                String titleEn = jQuestionnaire.getString("name_en");
                String titleNl = jQuestionnaire.getString("name_nl");
                String titleFr = jQuestionnaire.getString("name_fr");
                String descriptionEn = jQuestionnaire.getString("description_en");
                String descriptionNl = jQuestionnaire.getString("description_nl");
                String descriptionFr = jQuestionnaire.getString("description_fr");

                String title;
                String description;

                switch (Locale.getDefault().getLanguage()){
                    case "nl":
                        title = titleNl;
                        description = descriptionNl;
                        break;

                    case "fr":
                        title = titleFr;
                        description = descriptionFr;
                        break;

                    default:
                        title = titleEn;
                        description = descriptionEn;
                }

                boolean draft = jQuestionnaire.getInt("draft") == 1;
                int editable = jQuestionnaire.getInt("editable");

//                String createdAt = jQuestionnaire.getString("created_at");
//                String updatedAt = jQuestionnaire.getString("updated_at");
//                String deletedAt = jQuestionnaire.getString("deleted_at");
//                String createdBy = jQuestionnaire.getString("created_by");
//                String updatedBy = jQuestionnaire.getString("updated_by");
//                String deletedBy = jQuestionnaire.getString("deleted_by");

                if ((getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_QUESTIONNAIRE_DRAFT_SHOW, false) && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_QNR_DRAFT_VIEW, false)) || !draft){
                    questionnairesArrayList.add(new Questionnaire(id, groupId, version, title, description, draft, editable));
                }
            }

            if (questionnairesArrayList.size() == 0) {
                questionnaireSwitch.setEnabled(false);
                questionnaireSpinner.setEnabled(false);
                return;
            }

            Collections.reverse(questionnairesArrayList);

            ArrayList<String> spinnerArray = new ArrayList<>();

            Questionnaire questionnaire;
            int selectedQuestionnaireIndex = -1;

            for (int i = 0; i < questionnairesArrayList.size(); i++){
                questionnaire = questionnairesArrayList.get(i);
                spinnerArray.add(questionnaire.getTitle());

                if (questionnaireId > 0 && questionnaireId == questionnaire.getId()){
                    selectedQuestionnaireIndex = i;
                }
            }

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_padded, spinnerArray);
            questionnaireSpinner.setAdapter(spinnerArrayAdapter);

            questionnaireSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    questionnaireId = questionnairesArrayList.get(questionnaireSpinner.getSelectedItemPosition()).getId();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if (selectedQuestionnaireIndex > 0){
                questionnaireSpinner.setSelection(selectedQuestionnaireIndex);
            }
            questionnaireId = questionnairesArrayList.get(questionnaireSpinner.getSelectedItemPosition()).getId();

            questionnaireSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> questionnaireSpinner.setEnabled(isChecked));
        }
        catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    /**
     * Initializes and sets click listeners for the apply and clear filter buttons.
     * The result is added to the intent and returned to the previous activity.
     */
    private void finishActivity() {
        Button applyFilterButton = findViewById(R.id.apply_filter_button);
        applyFilterButton.setOnClickListener(v -> {
            if (startDateSwitch.isChecked() && endDateSwitch.isChecked() && startDate.getTime() > endDate.getTime()){
                Utilities.displayToast(context, getString(R.string.dates_in_right_order));
            }
            else {
                username = usernameEditText.getText().toString();

                Intent intent = new Intent();
                intent.putExtra("START_DATE", startDate);
                intent.putExtra("START_DATE_IS_ENABLED", startDateSwitch.isChecked());
                intent.putExtra("END_DATE", endDate);
                intent.putExtra("END_DATE_IS_ENABLED", endDateSwitch.isChecked());
                intent.putExtra("QUESTIONNAIRE_ID", questionnaireId);
                intent.putExtra("QUESTIONNAIRE_IS_ENABLED", questionnaireSwitch.isChecked());
                intent.putExtra("USERNAME", username);
                intent.putExtra("USERNAME_IS_ENABLED", usernameSwitch.isChecked());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button clearFilterButton = findViewById(R.id.clear_filter_button);
        clearFilterButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        });
    }
}
