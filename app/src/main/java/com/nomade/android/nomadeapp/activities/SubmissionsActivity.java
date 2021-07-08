package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.SubmissionsAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * SubmittedQuestionnairesActivity
 *
 * Activity to display submitted questionnaires in a ListView
 */
public class SubmissionsActivity extends AppCompatActivity {

    private static final String TAG = "SubmissionsActivity";
    private final Context context = this;

    private ProgressDialog pDialog;

    private ListView submittedQuestionnairesListView;
    private TextView backgroundTextView;

    private String stringJsonResponse = "";
    private String stringJsonUserList = "";

    private SharedPreferences submissionsSharedPreferences;
    private SharedPreferences.Editor submissionsEditor;

    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_LIST = 4;
    private static final int LAYOUT_LIST = 5;
    private static final int LAYOUT_NO_OFFLINE_DATA = 6;
    private static final int NO_SUBMISSIONS = 7;

    private Date filterStartDate;
    private boolean filterStartDateIsEnabled = false;
    private Date filterEndDate;
    private boolean filterEndDateIsEnabled = false;
    private int filterQuestionnaireId = -1;
    private boolean filterQuestionnaireIdIsEnabled = false;
    private String filterUserName = "";
    private boolean filterUsernameIsEnabled = false;

    private SimpleDateFormat sdfDateAndTime;
    private SimpleDateFormat sdfDateAndTimeLaravel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitted_questionnaires);

        submissionsSharedPreferences = getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE);
        submissionsEditor = submissionsSharedPreferences.edit();
        submissionsEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

        submittedQuestionnairesListView = findViewById(R.id.submitted_questionnaires_list_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        if (getIntent().getBooleanExtra("FILTER", false))
        {
            filterStartDate = (Date) getIntent().getSerializableExtra("START_DATE");
            filterStartDateIsEnabled = getIntent().getBooleanExtra("START_DATE_IS_ENABLED", false);
            filterEndDate = (Date) getIntent().getSerializableExtra("END_DATE");
            filterEndDateIsEnabled = getIntent().getBooleanExtra("END_DATE_IS_ENABLED", false);
            filterQuestionnaireId = getIntent().getIntExtra("QUESTIONNAIRE_ID", -1);
            filterQuestionnaireIdIsEnabled = getIntent().getBooleanExtra("QUESTIONNAIRE_IS_ENABLED", false);
            filterUserName = getIntent().getStringExtra("USERNAME");
            filterUsernameIsEnabled = getIntent().getBooleanExtra("USERNAME_IS_ENABLED", false);
        }

        if (savedInstanceState != null)
        {
            if (submissionsSharedPreferences.contains(Constants.API_SUBMISSIONS)){
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
            }
            if (submissionsSharedPreferences.contains(Constants.API_USERS)){
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS, "");
            }

            parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
        }
        else {
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
                if (stringJsonResponse != null && !stringJsonResponse.equals("") && stringJsonUserList != null){
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        submittedQuestionnairesListView.setEnabled(true);

        if (getIntent().getBooleanExtra("EDIT_WAS_MADE",false)){
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "null");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
                if (stringJsonResponse != null && !stringJsonResponse.equals("null") && stringJsonUserList != null){
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    /**
     * Handles the layout changes depending on the provided layout ID.
     *
     * @param layout id determining the layout
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_internet_access);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case TOAST_NO_INTERNET:
                Utilities.displayToast(context, getString(R.string.no_internet_access));
                break;

            case TOAST_OFFLINE_LIST:
                Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                break;

            case LAYOUT_LIST:
                backgroundTextView.setVisibility(View.GONE);
                submittedQuestionnairesListView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_OFFLINE_DATA:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_offline_data);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case NO_SUBMISSIONS:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_submissions);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Creates and executes a request to get a list of the submitted questionnaires.
     */
    private void getSubmittedQuestionnaires()
    {
        String tag_string_req = "sbm_list_questionnaire";

        pDialog.setMessage(getString(R.string.getting_submitted_questionnaires_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString(Constants.API_SUBMISSIONS, response).apply();
                    stringJsonResponse = response;

                    if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false)){
                        getUserList();
                    }
                    else {
                        parseAndDisplayJsonResponse(stringJsonResponse, "");
                    }
                }, e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

                    handleLayoutChanges(LAYOUT_LOADING);
                    stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
                    stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
                    if (stringJsonResponse != null && !stringJsonResponse.equals("")){
                        parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                        hideDialog();
                        Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA);
                    }
                    else {
                        handleLayoutChanges(LAYOUT_NO_INTERNET);
                        hideDialog();
                        Utilities.displayVolleyError(context, e);
                    }

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
     * Creates and executes a request to get a list of the users (developers only).
     */
    private void getUserList()
    {
        String tag_string_req = "user_list";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString(Constants.API_USERS, response).apply();
                    stringJsonUserList = response;
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
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
     * Parses the JSON containing the list of submitted questionnaires and if not null, also the
     * list of users.
     * Also displays the list of submitted questionnaires after a successful parse.
     *
     * @param response JSON containing the list of submitted questionnaires
     * @param userList JSON containing the list of users
     */
    private void parseAndDisplayJsonResponse(String response, String userList)
    {
        try {
            int ownUserId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1);
            int ownCompanyId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("company_id", -1);

            final SparseArray<String> usersSparseArray = new SparseArray<>();

            if (!userList.equals("")){
                JSONObject jObjUserList = new JSONObject(userList);

                JSONArray jUsersArray = jObjUserList.getJSONArray("data");

                for (int i = 0; i < jUsersArray.length(); i++){

                    JSONObject currentUser = jUsersArray.getJSONObject(i);

                    int id = currentUser.getInt("id");
                    String name = currentUser.getString("username");
                    int companyId = currentUser.getInt("company_id");

                    if (companyId == ownCompanyId || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false)) {
                        usersSparseArray.append(id, name);
                    }
                }
            }

            JSONObject jObjSubmissions = new JSONObject(response);

            JSONArray jSubmissionsArray = jObjSubmissions.getJSONArray("data");

            final ArrayList<SubmittedQuestionnaire> submissionsArrayList = new ArrayList<>();

            for (int i = 0; i < jSubmissionsArray.length(); i++){

                JSONObject currentSubmission = jSubmissionsArray.getJSONObject(i);

                int id = currentSubmission.getInt("id");
                int questionnaireId = currentSubmission.getInt("questionnaire_id");
                int userId = currentSubmission.getInt("user_id");

                int previousSubmissionId = 0;
                if (!currentSubmission.isNull("prev_submission_id")){
                    previousSubmissionId = currentSubmission.getInt("prev_submission_id");
                }

                int nextSubmissionId = 0;
                if (!currentSubmission.isNull("next_submission_id")){
                    nextSubmissionId = currentSubmission.getInt("next_submission_id");
                }

                String createdAt = currentSubmission.getString("created_at");
                Long createdAtMillis = null;
                if (!createdAt.equals("null")) {
                    try {
                        createdAtMillis = sdfDateAndTimeLaravel.parse(createdAt).getTime();
                        createdAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(createdAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String updatedAt = currentSubmission.getString("updated_at");
                if (!updatedAt.equals("null")) {
                    try {
                        updatedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(updatedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String startedAt = currentSubmission.getString("started_at");
                if (!startedAt.equals("null")) {
                    try {
                        startedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(startedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String finishedAt = currentSubmission.getString("finished_at");
                if (!finishedAt.equals("null")) {
                    try {
                        finishedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(finishedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String deletedAt = currentSubmission.getString("deleted_at");
                String createdBy = currentSubmission.getString("created_by");
                String updatedBy = currentSubmission.getString("updated_by");
                String deletedBy = currentSubmission.getString("deleted_by");

                JSONObject jQuestionnaire = currentSubmission.getJSONObject("questionnaire");

                int questionnaireGroupId = jQuestionnaire.getInt("questionnaire_group_id");
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

                String userName = "";
                if (usersSparseArray.size() > 0){
                    userName = usersSparseArray.get(userId, "");
                }

                if (ownUserId == userId || ((getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false)) && !userName.equals("")))
                {
                    boolean filterStartDateBool = true;
                    boolean filterEndDateBool = true;
                    boolean filterQuestionnaireIdBool = true;
                    boolean filterUsernameBool = true;

                    if (getIntent().getBooleanExtra("FILTER", false))
                    {
                        if (filterStartDateIsEnabled && createdAtMillis != null) {
                            filterStartDateBool = filterStartDate.getTime() < createdAtMillis;
                        }

                        if (filterEndDateIsEnabled && createdAtMillis != null) {
                            filterEndDateBool = createdAtMillis < (filterEndDate.getTime() + 86400000);
                        }

                        if (filterQuestionnaireIdIsEnabled) {
                            filterQuestionnaireIdBool = filterQuestionnaireId == questionnaireId;
                        }

                        if (filterUsernameIsEnabled) {
                            filterUsernameBool = filterUserName.equals(usersSparseArray.get(userId));
                        }
                    }

                    if (filterStartDateBool && filterEndDateBool && filterQuestionnaireIdBool && filterUsernameBool && nextSubmissionId == 0)
                    {
                        submissionsArrayList.add(new SubmittedQuestionnaire(id, questionnaireId, questionnaireGroupId, version, title, description, userId, userName, createdAt, updatedAt, previousSubmissionId, nextSubmissionId));
                    }
                }
            }

            Collections.sort(submissionsArrayList, (s1, s2) -> {

                try {
                    Date mDate1 = sdfDateAndTime.parse(s1.getDate());
                    Date mDate2 = sdfDateAndTime.parse(s2.getDate());
                    return mDate2.compareTo(mDate1);
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                return 0;
            });

            SubmissionsAdapter adapter = new SubmissionsAdapter(
                    context,
                    submissionsArrayList,
                    (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false))
            );

            submittedQuestionnairesListView.setAdapter(adapter);
            submittedQuestionnairesListView.setOnItemClickListener((parent, view, position, id) -> {

                submittedQuestionnairesListView.setEnabled(false);

                SubmittedQuestionnaire submittedQuestionnaire = submissionsArrayList.get(position);

                if (Utilities.checkNetworkConnection(context)){
                    Intent intent = new Intent(context, SubmissionViewerActivity.class);
                    intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getId()));
                    intent.putExtra("DOWNLOAD_JSON", true);

                    if (submittedQuestionnaire.getUserName() != null && !submittedQuestionnaire.getUserName().equals("")  && !submittedQuestionnaire.getUserName().equals("null")) {
                        intent.putExtra("USERNAME", submittedQuestionnaire.getUserName());
                    }

                    startActivity(intent);
                }
                else {
                    if (submissionsSharedPreferences.contains(Constants.API_SUBMISSIONS_ + submittedQuestionnaire.getId())){
                        Intent intent = new Intent(context, SubmissionViewerActivity.class);
                        intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getId()));
                        intent.putExtra("DOWNLOAD_JSON", false);

                        if (submittedQuestionnaire.getUserName() != null && !submittedQuestionnaire.getUserName().equals("")  && !submittedQuestionnaire.getUserName().equals("null")) {
                            intent.putExtra("USERNAME", submittedQuestionnaire.getUserName());
                        }

                        startActivity(intent);
                    }
                    else {
                        submittedQuestionnairesListView.setEnabled(true);
                        handleLayoutChanges(TOAST_NO_INTERNET);
                    }
                }
            });

            if (submissionsArrayList.size() == 0){
                handleLayoutChanges(NO_SUBMISSIONS);
            }
            else {
                handleLayoutChanges(LAYOUT_LIST);
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        hideDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_submitted_questionnaires, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_list) {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS, "");
                if (stringJsonResponse != null && !stringJsonResponse.equals("") && stringJsonUserList != null) {
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        } else if (itemId == R.id.action_filter_list) {
            Intent intent = new Intent(this, SubmissionsFilterActivity.class);
            if (filterStartDate != null) {
                intent.putExtra("START_DATE", filterStartDate);
            }
            intent.putExtra("START_DATE_IS_ENABLED", filterStartDateIsEnabled);
            if (filterEndDate != null) {
                intent.putExtra("END_DATE", filterEndDate);
            }
            intent.putExtra("END_DATE_IS_ENABLED", filterEndDateIsEnabled);

            intent.putExtra("QUESTIONNAIRE_ID", filterQuestionnaireId);
            intent.putExtra("QUESTIONNAIRE_IS_ENABLED", filterQuestionnaireIdIsEnabled);

            intent.putExtra("USERNAME", filterUserName);
            intent.putExtra("USERNAME_IS_ENABLED", filterUsernameIsEnabled);
            startActivityForResult(intent, 0);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0){

            if (resultCode == RESULT_OK)
            {
                getIntent().putExtra("START_DATE", data.getSerializableExtra("START_DATE"));
                getIntent().putExtra("START_DATE_IS_ENABLED", data.getBooleanExtra("START_DATE_IS_ENABLED", false));
                getIntent().putExtra("END_DATE", data.getSerializableExtra("END_DATE"));
                getIntent().putExtra("END_DATE_IS_ENABLED", data.getBooleanExtra("END_DATE_IS_ENABLED", false));
                getIntent().putExtra("QUESTIONNAIRE_ID", data.getIntExtra("QUESTIONNAIRE_ID", -1));
                getIntent().putExtra("QUESTIONNAIRE_IS_ENABLED", data.getBooleanExtra("QUESTIONNAIRE_IS_ENABLED", false));
                getIntent().putExtra("USERNAME", data.getStringExtra("USERNAME"));
                getIntent().putExtra("USERNAME_IS_ENABLED", data.getBooleanExtra("USERNAME_IS_ENABLED", false));
                getIntent().putExtra("FILTER", true);
                recreate();
            }

            if (resultCode == RESULT_CANCELED)
            {
                getIntent().putExtra("FILTER", false);
                recreate();
            }
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
