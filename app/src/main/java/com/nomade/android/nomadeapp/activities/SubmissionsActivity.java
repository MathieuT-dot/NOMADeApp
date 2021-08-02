package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.util.SparseArray;
import android.util.TypedValue;
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
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Answer;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Question;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireQuestion;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.SubmissionsAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.BodyChartImageView;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.NewJsonLib;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

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

    private ArrayList<SubmittedQuestionnaire> submissionsArrayList;
    private int downloadPdfIndex = 0;
    private int requestErrors = 0;
    private List<Integer> requestErrorIds = new ArrayList<>();
    private int pdfErrors = 0;
    private List<Integer> pdfErrorIds = new ArrayList<>();
    private int submittedQuestionnaireId;
    private String username = "";
    private SubmittedQuestionnaire submittedQuestionnaire;
    private Questionnaire questionnaire;
    private boolean alternateNumbering = false;

    private PDDocument document;
    private PDPage page;
    private PDPageContentStream contentStream;
    private float width;
    private float startY;
    private float endY;
    private float heightCounter;
    private float currentXPosition;
    private float wrapOffsetY;

    private ArrayList<Area> areas;
    private Paint textPaint;
    private int maximumAreas = 3;
    private int areaIndex = 0;

    //drawing path
    private Path drawPath;
    //drawing and canvas selector_normal
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    //brush sizes
    private float brushSize;
    //pain level
    private int painLevel;
    //pattern
    private String pattern;
    //erase flag
    private boolean erase=false;

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
    private void parseAndDisplayJsonResponse(String response, String userList) {
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

            submissionsArrayList = new ArrayList<>();

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
                    (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false)),
                    getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)
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

    private void downloadAllPdfFiles() {
        alternateNumbering = false;
        if (downloadPdfIndex < submissionsArrayList.size()) {
            pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + submissionsArrayList.size() + ")");
            submittedQuestionnaireId = submissionsArrayList.get(downloadPdfIndex).getId();
            if (submittedQuestionnaireId > 0) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getSubmittedQuestionnaire(submittedQuestionnaireId);
                    }
                }, 1000);

            }
            else {
                downloadPdfIndex++;
                downloadAllPdfFiles();
            }
        }
        else {
            hideDialog();

            MyLog.d(TAG, "requestErrors: " + requestErrors);
            for (int i = 0; i < requestErrorIds.size(); i++) {
                MyLog.d(TAG, "requestError ID " + (i+1) + ": " + requestErrorIds.get(i));
            }
            MyLog.d(TAG, "pdfErrors: " + pdfErrors);
            for (int i = 0; i < pdfErrorIds.size(); i++) {
                MyLog.d(TAG, "pdfError ID " + (i+1) + ": " + pdfErrorIds.get(i));
            }

            runOnUiThread(() -> Utilities.displayToast(context, "Downloading PDF files completed (Request errors: " + requestErrors + " | PDF errors: " + pdfErrors + ")"));
        }
    }

    /**
     * Creates and executes a request to get a submitted questionnaire.
     */
    private void getSubmittedQuestionnaire(int submittedQuestionnaireId) {
        String tag_string_req = "sbm_get_questionnaire";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonResponse(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

            requestErrors++;
            requestErrorIds.add(submittedQuestionnaireId);

            downloadPdfIndex++;
            downloadAllPdfFiles();
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
     * Parses the JSON containing the submitted questionnaire.
     *
     * @param response JSON containing the submitted questionnaire
     */
    private void parseJsonResponse(String response) {

        try{
            JSONObject jQuestionnaire = new JSONObject(response).getJSONObject("data").getJSONObject("questionnaire");

            questionnaire = NewJsonLib.parseJsonObjectQuestionnaire(context, jQuestionnaire);

            submittedQuestionnaire = NewJsonLib.parseJsonSubmittedQuestionnaire(context, response, questionnaire);

            username = submissionsArrayList.get(downloadPdfIndex).getUserName();

            if (submittedQuestionnaire != null && username != null && !username.equals("")) {
                submittedQuestionnaire.setUserName(username);
            }

            if (submittedQuestionnaire != null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createPdfFile();
                    }
                }, 10);
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
            Utilities.displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());

            downloadPdfIndex++;
            downloadAllPdfFiles();
        }
    }

    /**
     * Creates and outputs the PDF file.
     */
    private void createPdfFile() {
        // Enable Android asset loading
        PDFBoxResourceLoader.init(getApplicationContext());

        document = new PDDocument();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDRectangle mediaBox = page.getMediaBox();
        float marginY = 80;
        float marginX = 60;
        width = mediaBox.getWidth() - 2 * marginX;
        float startX = mediaBox.getLowerLeftX() + marginX;
        float endX = mediaBox.getUpperRightX() - marginX;
        startY = mediaBox.getUpperRightY() - marginY;
        endY = mediaBox.getLowerLeftY() + marginY;
        heightCounter = startY;
        currentXPosition = 0;
        float answerPositionX = startX + 10;

        float smallOffsetY = 8;
        float normalOffsetY = 20;
        wrapOffsetY = 2;

        Paint titleTextPaint = new TextPaint();
        float titleFontSize = 20;
        titleTextPaint.setTextSize(titleFontSize);
        titleTextPaint.setTypeface(Typeface.create("Helvetica", Typeface.BOLD));
        Paint.FontMetrics titleFontMetrics = titleTextPaint.getFontMetrics();
        float titleFontHeight = titleFontMetrics.descent - titleFontMetrics.ascent;

        Paint defaultTextPaint = new TextPaint();
        float defaultFontSize = 12;
        defaultTextPaint.setTextSize(defaultFontSize);
        defaultTextPaint.setTypeface(Typeface.create("Helvetica", Typeface.NORMAL));
        Paint.FontMetrics defaultFontMetrics = defaultTextPaint.getFontMetrics();
        float defaultFontHeight = defaultFontMetrics.descent - defaultFontMetrics.ascent;

        // Create font objects
        PDFont titleFont = PDType1Font.HELVETICA_BOLD;
        PDFont defaultFont = PDType1Font.HELVETICA;

        try {
            // Define a content stream for adding to the PDF
            contentStream = new PDPageContentStream(document, page);

            // Load in logo
            InputStream in = getAssets().open("nomade_logo_small.png");

            // Draw the logo
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            PDImageXObject xImage = LosslessFactory.createFromImage(document, bitmap);
            contentStream.drawImage(xImage, endX - 148, startY - 63, 148, 63);

            // Write title
            contentStream.beginText();

            addParagraph(startX, 0, true, titleFont, titleFontSize, titleFontHeight, submittedQuestionnaire.getTitle(), width - 148);

            if (username != null && !username.equals("")) {
                // Write username
                addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.list_user_colon, username));
                // Write user ID
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.user_id_colon) + submittedQuestionnaire.getUserId());
            }
            else {
                // Write user ID
                addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.user_id_colon) + submittedQuestionnaire.getUserId());
            }

            if (submittedQuestionnaire.getEditDate().equals("null") || submittedQuestionnaire.getDate().equals(submittedQuestionnaire.getEditDate())) {
                addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
            } else {
                addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.list_last_edit_colon) + submittedQuestionnaire.getEditDate());
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
                addParagraph(startX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
            }

            String descriptionString = submittedQuestionnaire.getDescription();

            if (descriptionString.length() > 0) {
                String[] splitDescription = descriptionString.split(System.lineSeparator());
                for (int k = 0; k < splitDescription.length; k++) {
                    if (k == 0) {
                        addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, splitDescription[k]);
                    }
                    else {
                        addParagraph(startX, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, splitDescription[k]);
                    }
                }
            }

            SubmittedQuestionnaireQuestion submittedQuestionnaireQuestion;
            SubmittedQuestionnaireAnswer submittedQuestionnaireAnswer;

            for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++) {

                submittedQuestionnaireQuestion = submittedQuestionnaire.getQuestionsList().get(i);

                String questionString = parseAlternateQuestionNumbering(i + 1, submittedQuestionnaireQuestion.getQuestion());
                String[] splitQuestion = questionString.split(System.lineSeparator());
                for (int k = 0; k < splitQuestion.length; k++) {
                    if (k == 0) {
                        addParagraph(startX, normalOffsetY, defaultFont, defaultFontSize, defaultFontHeight, splitQuestion[k]);
                    }
                    else {
                        addParagraph(startX, wrapOffsetY, defaultFont, defaultFontSize, defaultFontHeight, splitQuestion[k]);
                    }
                }

                if (submittedQuestionnaireQuestion.getBulletType() == Question.NO_BULLETS || submittedQuestionnaireQuestion.getBulletType() == Question.RADIO_BUTTONS || submittedQuestionnaireQuestion.getBulletType() == Question.CHECKBOXES) {

                    for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++) {

                        submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);

                        String answerValue = submittedQuestionnaireAnswer.getAnswer();

                        // Special case for the "Yes/No Scale"
                        if (submittedQuestionnaireAnswer.getTypeId() == Answer.YES_NO) {
                            if (answerValue.equals("1")) {
                                answerValue = getString(R.string.yes);
                            } else {
                                answerValue = getString(R.string.no);
                            }
                        }

                        if (!submittedQuestionnaireAnswer.getPrefix().equals("null") && !submittedQuestionnaireAnswer.getPrefix().equals("") && !submittedQuestionnaireAnswer.getPrefix().equals(" ") && !submittedQuestionnaireAnswer.getPrefix().trim().equals(":")) {

                            if (!answerValue.equals("") && !answerValue.equals("null")) {
                                switch (submittedQuestionnaireAnswer.getPrefix().substring(submittedQuestionnaireAnswer.getPrefix().length() - 1)) {
                                    case ":":
                                    case "?":
                                    case "!":
                                        answerValue = String.format("%s %s", parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue);
                                        break;

                                    default:
                                        answerValue = String.format("%s: %s", parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue);
                                }

                            } else {
                                answerValue = parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix());
                            }

                        }

                        addParagraph(answerPositionX, smallOffsetY, defaultFont, defaultFontSize, defaultFontHeight, "•");
                        addParagraph(answerPositionX + 15, -defaultFontHeight, defaultFont, defaultFontSize, defaultFontHeight, answerValue);
                    }

                }
                else if (submittedQuestionnaireQuestion.getBulletType() == Question.BODY_CHART) {

                    // TODO clean up code for body chart

                    addParagraph(startX, normalOffsetY + 200 - titleFontHeight, titleFont, titleFontSize, titleFontHeight, " ");
                    contentStream.endText();

                    Bitmap bodyChartBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.body_chart);
                    PDImageXObject xImageBodyChart = LosslessFactory.createFromImage(document, bodyChartBitmap);
                    contentStream.drawImage(xImageBodyChart, answerPositionX, heightCounter, 200, 200);

                    canvasBitmap = Bitmap.createBitmap(bodyChartBitmap.getWidth(), bodyChartBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    drawCanvas = new Canvas(canvasBitmap);

                    //prepare for drawing and setup selector_normal stroke properties
                    brushSize = 2;
                    drawPath = new Path();
                    drawPaint = new Paint();
                    drawPaint.setColor(paintColor);
                    drawPaint.setAntiAlias(true);
                    drawPaint.setStrokeWidth(bodyChartBitmap.getWidth() * 0.02f * brushSize);
                    drawPaint.setStyle(Paint.Style.STROKE);
                    drawPaint.setStrokeJoin(Paint.Join.ROUND);
                    drawPaint.setStrokeCap(Paint.Cap.ROUND);
                    drawPaint.setAlpha(200);
                    canvasPaint = new Paint(Paint.DITHER_FLAG);
                    areas = new ArrayList<>();
                    textPaint = new Paint();
                    textPaint.setTextSize(bodyChartBitmap.getWidth() * 0.075f);
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    textPaint.setColor(getResources().getColor(R.color.colorPrimary));
                    textPaint.setFakeBoldText(true);
                    textPaint.setShadowLayer(10, 0, 0, Color.GRAY);


                    String[] bodyChartAreas = new String[submittedQuestionnaireQuestion.getAnswersList().size()];

                    for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++){
                        submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);
                        bodyChartAreas[j] = submittedQuestionnaireAnswer.getAnswer();
                    }

                    for (String stringArea : bodyChartAreas) {
                        try {
                            JSONObject jsonObject = new JSONObject(stringArea);
                            float brushSize = (float) jsonObject.getDouble("brush_size");
                            int painLevel = jsonObject.getInt("pain_level");
                            String pattern = jsonObject.getString("pattern");
                            ArrayList<Marker> markerArrayList = new ArrayList<>();
                            JSONArray jsonArray = jsonObject.getJSONArray("markers");
                            for (int j = 0; j < jsonArray.length(); j++) {
                                JSONObject jsonMarker = jsonArray.getJSONObject(j);
                                float xScaled = (float) jsonMarker.getDouble("x");
                                float yScaled = (float) jsonMarker.getDouble("y");
                                float x = xScaled / 100f * (float) bodyChartBitmap.getWidth();
                                float y = yScaled / 100f * (float) bodyChartBitmap.getHeight();
                                markerArrayList.add(new Marker(x, y));
                            }
                            areas.add(new Area(brushSize, painLevel, pattern, markerArrayList));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    areaIndex = areas.size();

                    drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    drawPath = new Path();
                    for (Area area : areas) {
                        brushSize = area.brushSize;
                        if (brushSize < 4) {
                            // new width based implementation
                            drawPaint.setStrokeWidth(bodyChartBitmap.getWidth() * 0.02f * brushSize);
                        }
                        else {
                            // legacy implementation based on values from resources
                            drawPaint.setStrokeWidth(brushSize);
                        }
                        setPainLevel(area.painLevel);
                        setPattern(area.pattern);

                        drawPath.moveTo(area.markerArrayList.get(0).x, area.markerArrayList.get(0).y);

                        for (int j = 1; j < area.markerArrayList.size(); j++) {
                            drawPath.lineTo(area.markerArrayList.get(j).x, area.markerArrayList.get(j).y);
                        }

                        drawCanvas.drawPath(drawPath, drawPaint);
                        drawPath.reset();
                    }

                    for (int j = 0; j < areas.size(); j++) {
                        float averageX = 0;
                        float averageY = 0;
                        for (Marker marker : areas.get(j).markerArrayList) {
                            averageX += marker.x;
                            averageY += marker.y;
                        }
                        averageX /= areas.get(j).markerArrayList.size();
                        averageY /= areas.get(j).markerArrayList.size();

                        drawCanvas.drawCircle(averageX, averageY, bodyChartBitmap.getWidth() * 0.01f, textPaint);
                        float verticalCorrection = 0f;
                        float yScaled = averageY / (float) bodyChartBitmap.getHeight() * 100f;
                        if (yScaled <= 25) {
                            verticalCorrection = textPaint.getTextSize() * 0.7f;
                        }
                        else if (yScaled > 25 && yScaled < 75) {
                            verticalCorrection = textPaint.getTextSize() * 0.35f;
                        }
                        else if (yScaled >= 75) {
                            verticalCorrection = 0f;
                        }
                        textPaint.setTextSize(bodyChartBitmap.getWidth() * 0.075f);
                        drawCanvas.drawText("" + (j + 1), averageX + bodyChartBitmap.getWidth() * 0.02f, averageY + verticalCorrection, textPaint);
                    }

                    PDImageXObject xImageCanvas = LosslessFactory.createFromImage(document, canvasBitmap);
                    contentStream.drawImage(xImageCanvas, answerPositionX, heightCounter, 200, 200);

                    contentStream.beginText();
                    addParagraph(startX, -defaultFontHeight, true, titleFont, titleFontSize, titleFontHeight, " ");
                }
            }

            contentStream.endText();

            contentStream.close();

            // Adding page numbers to the whole document
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                String pageNumberString = (i + 1) + " / " + pageCount;
                float size = defaultFontSize * defaultFont.getStringWidth(pageNumberString) / 1000;
                page = document.getPage(i);
                contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                contentStream.beginText();
                contentStream.setFont(defaultFont, defaultFontSize);
                contentStream.newLineAtOffset(endX + marginX - endY + defaultFontHeight - size, endY - defaultFontHeight);
                contentStream.showText(pageNumberString);
                contentStream.endText();
                contentStream.close();
            }

            // Make sure that the content stream is closed:
            contentStream.close();

            OutputStream outputStream;

            String name = submittedQuestionnaireId + "_" + submittedQuestionnaire.getDate().substring(0, 10) + "_" + submittedQuestionnaire.getUserName() + "_" + submittedQuestionnaire.getTitle() + ".pdf";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MyLog.d(TAG, "MediaStore used");
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        document.save(outputStream);
                        outputStream.close();
                        document.close();
                    }
                }
            }

            downloadPdfIndex++;
            downloadAllPdfFiles();
        } catch (IOException e) {
            e.printStackTrace();

            pdfErrors++;
            pdfErrorIds.add(submittedQuestionnaireId);

            downloadPdfIndex++;
            downloadAllPdfFiles();
        }
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     */
    private void addParagraph(float positionX, float offsetY, PDFont font, float fontSize, float fontHeight, String text) throws IOException {
        addParagraph(positionX, offsetY, false, font, fontSize, fontHeight, text, width);
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param setYToHeightCounter set Y location to height counter
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     */
    private void addParagraph(float positionX, float offsetY, boolean setYToHeightCounter, PDFont font, float fontSize, float fontHeight, String text) throws IOException {
        addParagraph(positionX, offsetY, setYToHeightCounter, font, fontSize, fontHeight, text, width);
    }

    /**
     * Adds one or multiple lines of text to the PDF
     *
     * @param positionX X position to write the text
     * @param offsetY Y offset to write the text
     * @param setYToHeightCounter set Y location to height counter
     * @param font to display the text
     * @param fontSize to display the text
     * @param fontHeight to determine the extra Y offset
     * @param text string to write
     * @param width available width
     */
    private void addParagraph(float positionX, float offsetY, boolean setYToHeightCounter, PDFont font, float fontSize, float fontHeight, String text, float width) throws IOException {
        List<String> lines = parseLines(text.replaceAll("\\p{Cntrl}", ""), width, font, fontSize);
        contentStream.setFont(font, fontSize);

        float neededHeight = lines.size() * (wrapOffsetY + fontHeight) + offsetY - wrapOffsetY;

        if (heightCounter - neededHeight < endY) {
            // Create new page
            contentStream.endText();
            contentStream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);

            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX, startY - fontHeight);
                    heightCounter = startY - fontHeight;
                    currentXPosition = positionX;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
        else if (setYToHeightCounter) {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX, heightCounter - offsetY - fontHeight);
                    heightCounter -= offsetY + fontHeight;
                    currentXPosition = positionX;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
        else {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    contentStream.newLineAtOffset(positionX - currentXPosition, - offsetY - fontHeight);
                    heightCounter -= offsetY + fontHeight;
                    currentXPosition += positionX - currentXPosition;
                }
                else {
                    contentStream.newLineAtOffset(0, - wrapOffsetY - fontHeight);
                    heightCounter -= wrapOffsetY + fontHeight;
                }
                contentStream.showText(lines.get(i));
            }
        }
    }

    /**
     * Splits up the text depending on the available width, the font and the font size
     *
     * @param text string to split
     * @param width available width
     * @param font font for the text
     * @param fontSize font size for the text
     * @return a list of split strings
     */
    private static List<String> parseLines(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<String>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = fontSize * font.getStringWidth(subString) / 1000;
            if (size > width) {
                if (lastSpace < 0){
                    lastSpace = spaceIndex;
                }
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    /**
     * Creates a question string with the alternate numbering, if applicable, otherwise the
     * standard numbering is used
     *
     * @param questionNumber standard number of question
     * @param stringQuestion the question itself
     * @return question string with numbering
     */
    private String parseAlternateQuestionNumbering(int questionNumber, String stringQuestion) {

        int hashCount = stringQuestion.length() - stringQuestion.replaceAll("#", "").length();

        switch (hashCount){
            case 0:
                if (alternateNumbering) {
                    return stringQuestion;
                }
                else {
                    return String.format("%s. %s", questionNumber, stringQuestion);
                }

            case 2:
                stringQuestion = stringQuestion.substring(stringQuestion.indexOf("#") + 1);
                String alternateNumber = stringQuestion.substring(0, stringQuestion.indexOf("#"));
                stringQuestion = stringQuestion.substring(stringQuestion.indexOf("#") + 1).trim();
                if (alternateNumber.endsWith(".")) {
                    return String.format("%s %s", alternateNumber, stringQuestion);
                }
                else {
                    return String.format("%s. %s", alternateNumber, stringQuestion);
                }

            default:
                return "Error in hash count";
        }
    }

    /**
     * Creates an answer string with the alternate numbering, if applicable
     *
     * @param stringAnswer the answer itself
     * @return answer string with the alternate numbering, if applicable
     */
    private String parseAlternateAnswerNumbering(String stringAnswer) {

        int hashCount = stringAnswer.length() - stringAnswer.replaceAll("#", "").length();

        switch (hashCount){
            case 0:
                return stringAnswer;

            case 2:
                stringAnswer = stringAnswer.substring(stringAnswer.indexOf("#") + 1);
                String alternateNumber = stringAnswer.substring(0, stringAnswer.indexOf("#"));
                stringAnswer = stringAnswer.substring(stringAnswer.indexOf("#") + 1).trim();
                if (alternateNumber.endsWith(".")) {
                    return String.format("%s %s", alternateNumber, stringAnswer);
                }
                else {
                    return String.format("%s. %s", alternateNumber, stringAnswer);
                }

            default:
                return "Error in hash count";
        }
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
        else if (itemId == R.id.action_download_all_pdf_files) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadPdfIndex = 0;
                pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + submissionsArrayList.size() + ")");
                showDialog();
                downloadAllPdfFiles();
            }
            else {
                Utilities.displayToast(context, "Android 10 is required to use this function");
            }
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

    private static class Area {
        //brush sizes
        float brushSize;
        //pain level
        int painLevel;
        //pattern
        String pattern;
        //path coordinates
        ArrayList<Marker> markerArrayList;

        Area(float brushSize, int painLevel, String pattern, ArrayList<Marker> markerArrayList) {
            this.brushSize = brushSize;
            this.painLevel = painLevel;
            this.pattern = pattern;
            this.markerArrayList = markerArrayList;
        }
    }

    private static class Marker {
        float x;
        float y;

        Marker(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    //update color
    public void setPainLevel(int painLevel) {
//        invalidate();
        this.painLevel = painLevel;
        switch (painLevel) {
            case 1:
                // yellow
                paintColor = Color.parseColor("#FFFFF200");
                break;

            case 2:
                // orange
                paintColor = Color.parseColor("#FFFF7F27");
                break;

            case 3:
                // red
                paintColor = Color.parseColor("#FFED1C24");
                break;
        }
        ColorFilter filter = new PorterDuffColorFilter(paintColor, PorterDuff.Mode.SRC_IN);
        drawPaint.setColorFilter(filter);
        drawPaint.setAlpha(200);
    }

    // update pattern
    public void setPattern(String pattern) {
//        invalidate();
        this.pattern = pattern;
        //check whether filled or pattern name
        if(pattern.equals("filled")){
            drawPaint.setColor(paintColor);
            drawPaint.setShader(null);
            drawPaint.setAlpha(200);
        }
        else if (pattern.equals("pins_and_needles")) {
            //pattern
            int patternID = getResources().getIdentifier(
                    pattern, "drawable", "com.nomade.android.nomadeapp");
            //decode
            Bitmap patternBMP = BitmapFactory.decodeResource(getResources(), patternID);
            //create shader
            BitmapShader patternBMPshader = new BitmapShader(patternBMP,
                    Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            //color and shader
            drawPaint.setColor(paintColor);
            drawPaint.setShader(patternBMPshader);
            drawPaint.setAlpha(200);
        }
    }

    //set brush size
    public void setBrushSize(float newSize) {
//        invalidate();
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize=pixelAmount;
        drawPaint.setStrokeWidth(brushSize);
    }
}
