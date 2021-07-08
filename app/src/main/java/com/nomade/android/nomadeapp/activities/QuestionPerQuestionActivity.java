package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.BodyChartImageView;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.NewJsonLib;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.google.gson.Gson;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Answer;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Question;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireQuestion;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestion;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestionnaire;
import com.nomade.android.nomadeapp.setups.User;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.StandardFonts;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

/**
 * QuestionPerQuestionActivity
 *
 * Activity to display, fill in and submit questionnaires
 * Uses a question per question principle
 */
public class QuestionPerQuestionActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "QuestionPerQuestionActi";
    private final Context context = this;

    private ProgressDialog pDialog;

    private Questionnaire questionnaire;
    private SubmittingQuestionnaire submittingQuestionnaire;
    private SubmittedQuestionnaire submittedQuestionnaire;
    private String questionnaireId;
    private int currentQuestionIndex = -1;
    private int highestQuestionIndex = -1;
    private boolean endScreen = false;
    private boolean jsonParsedSuccessful = false;
    private boolean copy = false;
    private boolean alternateNumbering = false;

    private String stringJsonUserList = "";

    private TextView titleTextView;
    private TextView questionTextView;
    private ImageView questionTooltipImageView;
    private LinearLayout answersContainerLinearLayout;
    private ScrollView scrollView;
    private ProgressBar questionnaireProgressBar;
    private Button nextButton;
    private Button previousButton;
    private Button submitButton;

    // Dynamic answers
    private RadioButton[] radioButtons;
    private CheckBox[] checkBoxes;
    private EditText[] editTexts;
    private LinearLayout[] linearLayouts;
    private TextView[] textViews;
    private ImageView[] imageViews;
    private LinearLayout[] seekBarLayouts;
    private SeekBar[] seekBars;
    private TextView[] seekBarTextViews;
    private int[] seekBarValues;
    private boolean[] seekBarTouched;
    private Button[] dateButtons;
    private Button[] timeButtons;
    private String[] bodyChartAreas;

    private BodyChartImageView bodyChartImageView;

    // Questionnaire Overview Text Views
    private TextView[] questionsTextViews;
    private TextView[] answerTextViews;

    private Gson gson;
    private SharedPreferences questionnairesSharedPreferences;
    private SharedPreferences.Editor questionnairesEditor;
    private Boolean loadSavedAnswers;

    private static final String STATE_CURRENT_QUESTION = "dataCurrentQuestion";
    private String stringJsonResponse = "";

    private static final int CREATE_PDF_FILE = 1;

    private static final String SUBMIT_ERROR_DIALOG = "dialogTagSubmitError";
    private static final String QUESTION_OVERVIEW_DIALOG = "dialogTagQuestionOverview";
    private static final String OTHER_USER_DIALOG = "dialogTagOtherUser";
    private static final String CHOOSE_OTHER_USER_DIALOG = "dialogTagChooseOtherUser";

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private PDFWriter mPDFWriter;
    private int defaultLeftMargin = 50;
    private int questionLeftMargin = 70;
    private int answerLeftMargin = 90;
    private int currentTopPositionFromBottom = 772;
    private int defaultFontSize = 12;
    private int titleFontSize = 20;
    private int defaultInterlinearDistance = 10;
    private int smallInterlinearDistance = 5;
    private int bigInterlinearDistance = 25;
    private static final int A4_WIDTH = 595;
    private static final int A4_HEIGHT = 842;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_per_question);

        questionnaireId = getIntent().getStringExtra("QUESTIONNAIRE_ID");
        loadSavedAnswers = getIntent().getBooleanExtra("LOAD_SAVED_ANSWERS", false);
        boolean downloadJson = getIntent().getBooleanExtra("DOWNLOAD_JSON", true);
        copy = getIntent().getBooleanExtra("COPY", false);

        String stringSubmittedQuestionnaireJson = getIntent().getStringExtra("EDIT_SUBMITTED_QUESTIONNAIRE");

        gson = new Gson();
        questionnairesSharedPreferences = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE);
        questionnairesEditor = questionnairesSharedPreferences.edit();
        questionnairesEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        titleTextView = findViewById(R.id.name_text_view);
        questionTextView = findViewById(R.id.question_text_view);
        questionTooltipImageView = findViewById(R.id.question_tooltip_image_view);
        answersContainerLinearLayout = findViewById(R.id.answers_container_linear_layout);
        scrollView = findViewById(R.id.scroll_view);
        questionnaireProgressBar = findViewById(R.id.questionnaire_progress_bar);

        nextButton = findViewById(R.id.next_button);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(v -> loadNextQuestion());

        previousButton = findViewById(R.id.previous_button);
        previousButton.setEnabled(false);
        previousButton.setOnClickListener(v -> loadPreviousQuestion());

        submitButton = findViewById(R.id.submit_button);
        submitButton.setEnabled(false);
        submitButton.setOnClickListener(v -> {
            submitButton.setEnabled(false);
            saveQuestionnaire();
        });

        if (savedInstanceState != null){
            currentQuestionIndex = savedInstanceState.getInt(STATE_CURRENT_QUESTION);
            loadSavedAnswers = true;
            stringJsonResponse = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES_ + questionnaireId, "");
            parseJsonResponse(stringJsonResponse);
        }
        else {
            if (stringSubmittedQuestionnaireJson != null){

                try {
                    JSONObject jQuestionnaire = new JSONObject(stringSubmittedQuestionnaireJson).getJSONObject("data").getJSONObject("questionnaire");

                    Questionnaire extractedQuestionnaire = NewJsonLib.parseJsonObjectQuestionnaire(context, jQuestionnaire);

                    submittedQuestionnaire = NewJsonLib.parseJsonSubmittedQuestionnaire(context, stringSubmittedQuestionnaireJson, extractedQuestionnaire);
                }
                catch (JSONException e) {
                    MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
                    Utilities.displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
                }
            }

            if (downloadJson && Utilities.checkNetworkConnection(context)){
                getQuestionnaireById();
            }
            else {
                stringJsonResponse = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES_ + questionnaireId, "");
                if (!stringJsonResponse.equals("")){
                    parseJsonResponse(stringJsonResponse);
                    Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                }
                else {
                    titleTextView.setText(R.string.no_offline_data);
                    Utilities.displayToast(context, getString(R.string.no_internet_access));
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {

        if (jsonParsedSuccessful){
            if (currentQuestionIndex < questionnaire.getQuestionsList().size()){
                saveAnswer(currentQuestionIndex, false);
            }
        }

        savedInstanceState.putInt(STATE_CURRENT_QUESTION, currentQuestionIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Creates and executes a network request to get a questionnaire based on the ID.
     */
    private void getQuestionnaireById(){
        String tag_string_req = "req_questionnaire";

        pDialog.setMessage(getString(R.string.getting_selected_questionnaire_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/" + questionnaireId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/" + questionnaireId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG,"JSON Response: " + response, false);

                    questionnairesEditor.putString(Constants.API_QUESTIONNAIRES_ + questionnaireId, response).apply();
                    stringJsonResponse = response;
                    parseJsonResponse(stringJsonResponse);

                }, e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

                    stringJsonResponse = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES_ + questionnaireId, "");
                    if (stringJsonResponse != null && !stringJsonResponse.equals("")){
                        parseJsonResponse(stringJsonResponse);
                        hideDialog();
                        Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA);
                    }
                    else {
                        titleTextView.setText(R.string.no_offline_data);
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
     * Parses the JSON response containing the questionnaire and displaying the contents
     * question per question.
     *
     * @param response JSON response containing the questionnaire
     */
    private void parseJsonResponse(String response){

        try {
            questionnaire = NewJsonLib.parseJsonQuestionnaire(context, response);

            ArrayList<SubmittingQuestion> submittingQuestionList = new ArrayList<>();

            for (int i = 0; i < questionnaire.getQuestionsList().size(); i++){
                submittingQuestionList.add(new SubmittingQuestion(questionnaire.getQuestionsList().get(i).getId()));
            }

            if (submittedQuestionnaire != null){

                MyLog.d("EDITDEBUG", "submittedQuestionnaire != null");

                if (submittedQuestionnaire.getQnrId() == questionnaire.getId()){

                    MyLog.d("EDITDEBUG", "submittedQuestionnaire.getQnrId() == questionnaire.getId()");

                    if (copy){
                        MyLog.d("EDITDEBUG", "copy");
                        submittingQuestionnaire = new SubmittingQuestionnaire(questionnaire.getId(), questionnaire.getQuestionsList().size(), submittingQuestionList, questionnaire.getTitle(),getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1), submittedQuestionnaire.getUserId(), getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1));
                    }
                    else {
                        MyLog.d("EDITDEBUG", "!copy");
                        submittingQuestionnaire = new SubmittingQuestionnaire(questionnaire.getId(), questionnaire.getQuestionsList().size(), submittingQuestionList, questionnaire.getTitle(), submittedQuestionnaire.getId(), submittedQuestionnaire.getUserId(), getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1));
                    }

                    submittingQuestionnaire.setStartMillis(System.currentTimeMillis());

                    if (questionnairesSharedPreferences.contains("answers_json")){
                        questionnairesEditor.remove("answers_json").apply();
                    }

                    SubmittedQuestionnaireQuestion submittedQuestionnaireQuestion;
                    SubmittedQuestionnaireAnswer submittedQuestionnaireAnswer;

                    for (int questionIndexQuest = 0; questionIndexQuest < questionnaire.getQuestionsList().size(); questionIndexQuest++){

                        MyLog.d("EDITDEBUG", "questionnaire.getQuestionsList(): " + questionIndexQuest);

                        ArrayList<SubmittingAnswer> submittingAnswerList = new ArrayList<>();

                        for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++){

                            MyLog.d("EDITDEBUG", "submittedQuestionnaire.getQuestionsList(): " + i);

                            submittedQuestionnaireQuestion = submittedQuestionnaire.getQuestionsList().get(i);

                            if (questionnaire.getQuestionsList().get(questionIndexQuest).getId() == submittedQuestionnaireQuestion.getId()){

                                MyLog.d("EDITDEBUG", "questionnaire.getQuestionsList().get(questionIndexQuest).getId() == submittedQuestionnaireQuestion.getId()");

                                for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++){

                                    MyLog.d("EDITDEBUG", "submittedQuestionnaire.getQuestionsList(): " + i);

                                    submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);

                                    // Switch based on answer type
                                    switch (submittedQuestionnaireAnswer.getTypeId()){

                                        case Answer.FIXED: // Fixed
                                            submittingAnswerList.add(
                                                    new SubmittingAnswer(
                                                            submittedQuestionnaireAnswer.getId(),
                                                            submittedQuestionnaireAnswer.getTypeId()
                                                    )
                                            );
                                            break;

                                        case Answer.INTEGER: // Integer
                                        case Answer.SCALE: // Scale
                                        case Answer.YES_NO: // Yes/No Scale
                                            submittingAnswerList.add(
                                                    new SubmittingAnswer(
                                                            submittedQuestionnaireAnswer.getId(),
                                                            submittedQuestionnaireAnswer.getTypeId(),
                                                            Integer.parseInt(submittedQuestionnaireAnswer.getAnswer())
                                                    )
                                            );
                                            break;

                                        case Answer.DOUBLE: // Double
                                            submittingAnswerList.add(
                                                    new SubmittingAnswer(
                                                            submittedQuestionnaireAnswer.getId(),
                                                            submittedQuestionnaireAnswer.getTypeId(),
                                                            Double.parseDouble(submittedQuestionnaireAnswer.getAnswer())
                                                    )
                                            );
                                            break;

                                        case Answer.OPTIONAL_CUSTOM: // Optional custom
                                            if (submittedQuestionnaireAnswer.getAnswer() != null && !submittedQuestionnaireAnswer.getAnswer().equals("")){
                                                submittingAnswerList.add(
                                                        new SubmittingAnswer(
                                                                submittedQuestionnaireAnswer.getId(),
                                                                submittedQuestionnaireAnswer.getTypeId(),
                                                                submittedQuestionnaireAnswer.getAnswer()
                                                        )
                                                );
                                            }
                                            else {
                                                submittingAnswerList.add(
                                                        new SubmittingAnswer(
                                                                submittedQuestionnaireAnswer.getId(),
                                                                submittedQuestionnaireAnswer.getTypeId()
                                                        )
                                                );
                                            }
                                            break;

                                        case Answer.MANDATORY_CUSTOM: // Mandatory custom
                                            submittingAnswerList.add(
                                                    new SubmittingAnswer(
                                                            submittedQuestionnaireAnswer.getId(),
                                                            submittedQuestionnaireAnswer.getTypeId(),
                                                            submittedQuestionnaireAnswer.getAnswer()
                                                    )
                                            );
                                            break;
                                    }

                                }

                            }

                        }

                        submittingQuestionnaire.getSubmittingQuestionsList().get(questionIndexQuest).setSubmittingAnswerList(submittingAnswerList);

                    }

                }
                else {
                    finish();
                }

            }
            else {

                if (questionnairesSharedPreferences.contains("answers_json") && loadSavedAnswers){
                    MyLog.d(TAG, "Getting the saved answers from SharedPreferences");
                    String answers_json = questionnairesSharedPreferences.getString("answers_json", "null");
                    submittingQuestionnaire = gson.fromJson(answers_json, SubmittingQuestionnaire.class);
                }
                else {
                    submittingQuestionnaire = new SubmittingQuestionnaire(questionnaire.getId(), questionnaire.getQuestionsList().size(), submittingQuestionList, questionnaire.getTitle(), 0, getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1), getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1));
                    submittingQuestionnaire.setStartMillis(System.currentTimeMillis());
                    if (questionnairesSharedPreferences.contains("answers_json")){
                        questionnairesEditor.remove("answers_json").apply();
                    }
                }

            }

            initialLoad();

        } catch (Exception e) {
            hideDialog();
            MyLog.e(TAG, "Exception Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "Exception Error: " + e.toString() + ", " + e.getMessage());
            finish();
        }
    }

    /**
     * Initializes the title, progressbar, etc.
     */
    private void initialLoad(){

        jsonParsedSuccessful = true;

        titleTextView.setText(questionnaire.getTitle());

        questionnaireProgressBar.setMax(questionnaire.getQuestionsList().size());
        questionnaireProgressBar.setProgress(currentQuestionIndex);

        for (Question question : questionnaire.getQuestionsList()) {
            String stringQuestion = question.getQuestion();
            int hashCount = stringQuestion.length() - stringQuestion.replaceAll("#", "").length();

            if (hashCount == 2) {
                alternateNumbering = true;
                break;
            }
        }

        if (currentQuestionIndex == -1){
            loadStartScreen();
        }
        else if (currentQuestionIndex == questionnaire.getQuestionsList().size()){
            loadEndScreen();
        }
        else {
            loadQuestion(currentQuestionIndex);

            loadAnswer(currentQuestionIndex);

            nextButton.setEnabled(true);

            if (currentQuestionIndex >= 0) {
                previousButton.setEnabled(true);
            }
        }

        hideDialog();

        MyLog.d(TAG, "Current Question Index: " + currentQuestionIndex);

        // ask if the user wants to fill in the questionnaire for someone else
        if (!submittingQuestionnaire.isForUserIdChanged() && (!loadSavedAnswers && (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX_COMPANY, false)) && (submittedQuestionnaire == null || copy))) {
            SimpleDialog.build()
                    .title(R.string.yourself_or_other_user)
                    .msg(R.string.complete_questionnaire_yourself_other_user)
                    .pos(R.string.other_user)
                    .neg(R.string.myself)
                    .cancelable(false)
                    .show(QuestionPerQuestionActivity.this, OTHER_USER_DIALOG);
        }
        else {
            submittingQuestionnaire.setForUserIdChanged(true);
        }

        if (submittingQuestionnaire.isForUserIdChanged() && !submittingQuestionnaire.getForUserName().equals("")) {
            titleTextView.setText(String.format("%s%s%s", questionnaire.getTitle(), getString(R.string._for_), submittingQuestionnaire.getForUserName()));
        }
    }

    /**
     * Loads the next question.
     */
    private void loadNextQuestion(){

        if (saveAnswer(currentQuestionIndex, true)){

            // Check if no view has focus (hides the soft keyboard):
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            currentQuestionIndex++;

            questionnaireProgressBar.setProgress(currentQuestionIndex);

            if (currentQuestionIndex == questionnaire.getQuestionsList().size()){
                loadEndScreen();
            }
            else {

                while (skipQuestion(currentQuestionIndex)){

                        currentQuestionIndex++;

                        questionnaireProgressBar.setProgress(currentQuestionIndex);

                }

                if (highestQuestionIndex < currentQuestionIndex){
                    highestQuestionIndex = currentQuestionIndex;
                }

                if (currentQuestionIndex == questionnaire.getQuestionsList().size()){
                    loadEndScreen();
                }
                else {
                    loadQuestion(currentQuestionIndex);

                    loadAnswer(currentQuestionIndex);

                    if (!previousButton.isEnabled()){
                        previousButton.setEnabled(true);
                    }

                    MyLog.d(TAG, "Current Question Index: " + currentQuestionIndex);
                }
            }
        }
    }

    /**
     * Loads the previous question.
     */
    private void loadPreviousQuestion(){

        if (!endScreen){
            saveAnswer(currentQuestionIndex, false);

            // Check if no view has focus (hides the soft keyboard):
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        else {
            endScreen = false;
        }

        currentQuestionIndex--;

        if (currentQuestionIndex >= 0){
            questionnaireProgressBar.setProgress(currentQuestionIndex);

            while (skipQuestion(currentQuestionIndex)){

                currentQuestionIndex--;

                questionnaireProgressBar.setProgress(currentQuestionIndex);
            }

            loadQuestion(currentQuestionIndex);

            loadAnswer(currentQuestionIndex);

            if (!nextButton.isEnabled()){
                nextButton.setEnabled(true);
                nextButton.setVisibility(View.VISIBLE);

                submitButton.setEnabled(false);
                submitButton.setVisibility(View.GONE);
            }

            MyLog.d(TAG, "Current Question Index: " + currentQuestionIndex);
        }
        else {
            loadStartScreen();
            previousButton.setEnabled(false);
        }
    }

    /**
     * Loads the question chosen in the question overview dialog
     *
     * @param questionIndex index of question to load
     */
    private void loadQuestionFromOverview(int questionIndex) {

        if (!endScreen){
            saveAnswer(currentQuestionIndex, false);

            // Check if no view has focus (hides the soft keyboard):
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        else {
            endScreen = false;
        }

        currentQuestionIndex = questionIndex;

        if (currentQuestionIndex >= 0){
            questionnaireProgressBar.setProgress(currentQuestionIndex);

            loadQuestion(currentQuestionIndex);

            loadAnswer(currentQuestionIndex);

            if (!nextButton.isEnabled()){
                nextButton.setEnabled(true);
                nextButton.setVisibility(View.VISIBLE);

                submitButton.setEnabled(false);
                submitButton.setVisibility(View.GONE);
            }

            if (!previousButton.isEnabled()){
                previousButton.setEnabled(true);
            }

            MyLog.d(TAG, "Current Question Index: " + currentQuestionIndex);
        }
        else {
            loadStartScreen();
            previousButton.setEnabled(false);
        }
    }

    /**
     * Checks whether or not a question needs to be skipped.
     *
     * @param questionIndex current question index
     * @return true if the question has to be skipped
     *         false if the question hasn't to be skipped
     */
    private boolean skipQuestion(int questionIndex){

        if (questionIndex != questionnaire.getQuestionsList().size()){

            Question question = questionnaire.getQuestionsList().get(questionIndex);

            if (question.getConditional() == 1){

                SubmittingQuestion submittingQuestion;
                SubmittingAnswer submittingAnswer;

                for (int i = 0; i < submittingQuestionnaire.getSubmittingQuestionsList().size(); i++){

                    submittingQuestion = submittingQuestionnaire.getSubmittingQuestionsList().get(i);

                    if (submittingQuestion.getId() == question.getConditionQuestionId()){
                        boolean skip = true;
                        for (int j = 0; j < submittingQuestion.getSubmittingAnswerList().size(); j++){

                            submittingAnswer = submittingQuestion.getSubmittingAnswerList().get(j);

                            if (submittingAnswer.getId() == question.getConditionAnswerId()){
                                skip = false;
                            }
                        }
                        if (skip) {
                            ArrayList<SubmittingAnswer> submittingAnswerList = new ArrayList<>();
                            submittingQuestionnaire.getSubmittingQuestionsList().get(currentQuestionIndex).setSubmittingAnswerList(submittingAnswerList);
                            MyLog.d(TAG, "skipQuestion: skip = true");
                        }
                        return skip;
                    }
                }
                return false;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    /**
     * Loads a question determined by the current question index.
     *
     * @param questionIndex current question index
     */
    private void loadQuestion(final int questionIndex){

        Question question = questionnaire.getQuestionsList().get(questionIndex);

        answersContainerLinearLayout.removeAllViews();
        radioButtons = null;
        checkBoxes = null;
        editTexts = null;
        linearLayouts = null;
        textViews = null;
        imageViews = null;
        seekBarLayouts = null;
        seekBars = null;
        seekBarTextViews = null;
        seekBarValues = null;
        seekBarTouched = null;
        questionsTextViews = null;
        answerTextViews = null;
        dateButtons = null;
        timeButtons = null;
        bodyChartAreas = null;
        bodyChartImageView = null;

        boolean atLeastOneTooltip = false;

        int bulletType = question.getBulletType();

        questionTextView.setText(parseAlternateQuestionNumbering(questionIndex + 1, question.getQuestion()));

        if (!question.getTooltip().equals("null") && !question.getTooltip().equals("")) {
            questionTooltipImageView.setOnClickListener(v -> {
                // SimpleDialog without clickable links
//                SimpleDialog.build()
//                        .title(R.string.tooltip)
//                        .msg(question.getTooltip())
//                        .show(QuestionPerQuestionActivity.this);

                // Linkify the message and display AlertDialog with clickable link
                final SpannableString s = new SpannableString(question.getTooltip()); // msg should have url to enable clicking
                Linkify.addLinks(s, Linkify.ALL);

                final AlertDialog d = new AlertDialog.Builder(this)
                        .setTitle(R.string.tooltip)
                        .setPositiveButton(android.R.string.ok, null)
                        .setMessage( s )
                        .create();
                d.show();
                // Make the textview clickable. Must be called after show()
                ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            });
            questionTooltipImageView.setVisibility(View.VISIBLE);
            questionTooltipImageView.setEnabled(true);
        }
        else {
            questionTooltipImageView.setVisibility(View.GONE);
        }

        // Body chart
        if (bulletType == Question.BODY_CHART){
            loadBodyChart(question, questionIndex);
            scrollView.scrollTo(0,0);
            return;
        }

        final int answersCount = question.getAnswersList().size();

        linearLayouts = new LinearLayout[answersCount];
        imageViews = new ImageView[answersCount];
        textViews = new TextView[answersCount];
        radioButtons = new RadioButton[answersCount];
        checkBoxes = new CheckBox[answersCount];
        editTexts = new EditText[answersCount];
        seekBarLayouts = new LinearLayout[answersCount];
        seekBars = new SeekBar[answersCount];
        seekBarTextViews = new TextView[answersCount];
        seekBarValues = new int[answersCount];
        seekBarTouched = new boolean[answersCount];
        dateButtons = new Button[answersCount];
        timeButtons = new Button[answersCount];

        Answer answerI;

        for (int i = 0; i < answersCount; i++){

            answerI = question.getAnswersList().get(i);

            final int finalI = i;

            // Create the horizontal linearLayout for the current answer
            linearLayouts[i] = new LinearLayout(this);
            linearLayouts[i].setOrientation(LinearLayout.HORIZONTAL);
            linearLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayouts[i].setGravity(Gravity.CENTER_VERTICAL);

            answersContainerLinearLayout.addView(linearLayouts[i]);

            // Add the tooltip, VISIBLE if available, INVISIBLE otherwise
            imageViews[i] = new ImageView(this);
            imageViews[i].setImageResource(R.drawable.ic_info_dark);

            if (!answerI.getTooltip().equals("null") && !answerI.getTooltip().equals("")){
                imageViews[i].setOnClickListener(v ->
                    SimpleDialog.build()
                            .title(R.string.tooltip)
                            .msg(question.getAnswersList().get(finalI).getTooltip())
                            .show(QuestionPerQuestionActivity.this)
                );
                imageViews[i].setVisibility(View.VISIBLE);
                atLeastOneTooltip = true;
            }
            else {
                imageViews[i].setVisibility(View.INVISIBLE);
            }

            linearLayouts[i].addView(imageViews[i]);

            // Bullet types
            switch (bulletType){

                case Question.NO_BULLETS: // No bullets

                    // Add the prefix if available
                    if (!answerI.getPrefix().equals("null") && !answerI.getPrefix().equals("") && !answerI.getPrefix().equals(" ") && !answerI.getPrefix().trim().equals(":")){
                        textViews[i] = new TextView(this);
                        textViews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                        if (answerI.getTypeId() > 1){

                            switch (answerI.getPrefix().substring(answerI.getPrefix().length() - 1)){
                                case ":":
                                case "?":
                                case "!":
                                    textViews[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                                    break;

                                default:
                                    textViews[i].setText(String.format("%s: ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                            }

                            if (answerI.getTypeId() == 99){
                                textViews[i].setText(String.format("%s* ", textViews[i].getText()));
                            }

                        }
                        else {
                            textViews[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                        }

                        linearLayouts[i].addView(textViews[i]);
                    }

                    break;

                case Question.RADIO_BUTTONS: // Radio buttons

                    // Add the radio button
                    radioButtons[i] = new RadioButton(this);
                    radioButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    if (answerI.getTypeId() > 1){
                        if (answerI.getPrefix().contains(":")){
                            radioButtons[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                        }
                        else {
                            radioButtons[i].setText(String.format("%s: ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                        }
                    }
                    else {
                        radioButtons[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                    }
                    radioButtons[i].setOnClickListener(v -> {
                        if (question.getAnswersList().get(finalI).getTypeId() > 9 && question.getAnswersList().get(finalI).getTypeId() != 20){
                            editTexts[finalI].setEnabled(true);
                            editTexts[finalI].setHint(R.string.hint);
                        }

                        Answer answerJ;

                        for (int j = 0; j < answersCount; j++){

                            answerJ = question.getAnswersList().get(j);

                            if (finalI != j){
                                radioButtons[j].setChecked(false);

                                if (answerJ.getTypeId() > 9 && answerJ.getTypeId() != 20){
                                    editTexts[j].setText("");
                                    editTexts[j].setEnabled(false);
                                    editTexts[j].setHint("");
                                }
                            }
                        }
                    });

                    linearLayouts[i].addView(radioButtons[i]);

                    break;

                case Question.CHECKBOXES: // Checkboxes

                    // Add the checkbox
                    checkBoxes[i] = new CheckBox(this);
                    checkBoxes[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    if (answerI.getTypeId() > 1){
                        if (answerI.getPrefix().contains(":")){
                            checkBoxes[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                        }
                        else {
                            checkBoxes[i].setText(String.format("%s: ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                        }
                    }
                    else {
                        checkBoxes[i].setText(String.format("%s ", parseAlternateAnswerNumbering(answerI.getPrefix())));
                    }

                    checkBoxes[i].setOnClickListener(v -> {
                        if (question.getAnswersList().get(finalI).getTypeId() > 9 && question.getAnswersList().get(finalI).getTypeId() != 20){
                            if (checkBoxes[finalI].isChecked()){
                                editTexts[finalI].setEnabled(true);
                                editTexts[finalI].setHint(R.string.hint);
                            }
                            else {
                                editTexts[finalI].setText("");
                                editTexts[finalI].setEnabled(false);
                                editTexts[finalI].setHint("");
                            }
                        }
                    });

                    linearLayouts[i].addView(checkBoxes[i]);

                    break;

            }

            // Add the different types of answers
            switch (answerI.getTypeId()){

                case Answer.FIXED: // Fixed
                    // Type 1 answer not possible for no bullets or already added as setText for the radio button or the checkbox
                    break;


                case Answer.INTEGER: // Integer
                    editTexts[i] = new EditText(this);
                    editTexts[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    editTexts[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    editTexts[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_LENGTH_OF_EDIT_TEXT)});

                    if (answerI.isHasOptions() && answerI.getOptionsMinInt() >= 0){
                        editTexts[i].setInputType(InputType.TYPE_CLASS_NUMBER);
                    }
                    else {
                        editTexts[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    }

                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                        editTexts[i].setEnabled(false);
                    }
                    else {
                        editTexts[i].setHint(R.string.hint);
                    }

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(editTexts[i]);
                    }
                    else {
                        linearLayouts[i].addView(editTexts[i]);
                    }
                    break;


                case Answer.DOUBLE: // Double
                    editTexts[i] = new EditText(this);
                    editTexts[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    editTexts[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    editTexts[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editTexts[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_LENGTH_OF_EDIT_TEXT)});

                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                        editTexts[i].setEnabled(false);
                    }
                    else {
                        editTexts[i].setHint(R.string.hint);
                    }

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(editTexts[i]);
                    }
                    else {
                        linearLayouts[i].addView(editTexts[i]);
                    }
                    break;


                case Answer.SCALE: // Scale
                    seekBarLayouts[i] = new LinearLayout(this);
                    seekBarLayouts[i].setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    if (textViews[i] != null){
                        if (i > 0){
                            textViews[i].setPadding(0,getResources().getDimensionPixelOffset(R.dimen.padding_scale),0,0);
                        }
                        else {
                            textViews[i].setPadding(0,getResources().getDimensionPixelOffset(R.dimen.padding_scale) / 2,0,0);
                        }
                    }
                    else {
                        params1.setMargins(0,Math.round(LibUtilities.pxFromDp(context, 5)),0,Math.round(LibUtilities.pxFromDp(context, 25)));
                    }

                    seekBarLayouts[i].setLayoutParams(params1);

                    answersContainerLinearLayout.addView(seekBarLayouts[i]);

                    seekBarValues[i] = answerI.getOptionsMinInt();

                    seekBarTouched[i] = false;

                    seekBars[i] = new SeekBar(this);
                    seekBars[i].setMax(answerI.getOptionsMaxInt() - answerI.getOptionsMinInt());

                    seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            seekBarValues[finalI] = seekBar.getProgress() + question.getAnswersList().get(finalI).getOptionsMinInt();
                            seekBarTextViews[finalI].setText(String.format("%s / %s", seekBarValues[finalI], question.getAnswersList().get(finalI).getOptionsMaxInt()));

                            seekBarTouched[finalI] = true;
                            seekBars[finalI].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_touched_drawable));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            seekBarValues[finalI] = seekBar.getProgress() + question.getAnswersList().get(finalI).getOptionsMinInt();
                            seekBarTextViews[finalI].setText(String.format("%s / %s", seekBarValues[finalI], question.getAnswersList().get(finalI).getOptionsMaxInt()));

                            seekBarTouched[finalI] = true;
                            seekBars[finalI].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_touched_drawable));
                        }
                    });

                    seekBars[i].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_untouched_drawable));
                    seekBars[i].setPadding(getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar));

                    seekBarLayouts[i].addView(seekBars[i]);

                    seekBarTextViews[i] = new TextView(this);
                    seekBarTextViews[i].setText(String.format("%s / %s", seekBarValues[finalI], question.getAnswersList().get(finalI).getOptionsMaxInt()));
                    seekBarTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    seekBarTextViews[i].setGravity(Gravity.CENTER_HORIZONTAL);

                    seekBarLayouts[i].addView(seekBarTextViews[i]);
                    break;


                case Answer.YES_NO: // Yes/No Scale
                    seekBarLayouts[i] = new LinearLayout(this);
                    seekBarLayouts[i].setOrientation(LinearLayout.VERTICAL);
                    seekBarLayouts[i].setGravity(Gravity.CENTER_HORIZONTAL);
                    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.width_general), LinearLayout.LayoutParams.WRAP_CONTENT);

                    if (textViews[i] != null){
                        if (i > 0){
                            textViews[i].setPadding(0,getResources().getDimensionPixelOffset(R.dimen.padding_scale),0,0);
                        }
                        else {
                            textViews[i].setPadding(0,getResources().getDimensionPixelOffset(R.dimen.padding_scale) / 2,0,0);
                        }
                    }
                    else {
                        params2.setMargins(0,Math.round(LibUtilities.pxFromDp(context, 5)),0,Math.round(LibUtilities.pxFromDp(context, 25)));
                    }

                    seekBarLayouts[i].setLayoutParams(params2);

                    answersContainerLinearLayout.addView(seekBarLayouts[i]);

                    seekBarValues[i] = answerI.getOptionsMinInt();

                    seekBarTouched[i] = false;

                    seekBars[i] = new SeekBar(this);
                    seekBars[i].setMax(answerI.getOptionsMaxInt() - answerI.getOptionsMinInt());

                    seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            seekBarValues[finalI] = seekBar.getProgress() + question.getAnswersList().get(finalI).getOptionsMinInt();
                            switch (seekBarValues[finalI]){
                                case 0:
                                    seekBarTextViews[finalI].setText(R.string.no);
                                    break;

                                case 1:
                                    seekBarTextViews[finalI].setText(R.string.yes);
                                    break;

                                default:
                            }

                            seekBarTouched[finalI] = true;
                            seekBars[finalI].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_touched_drawable));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            seekBarValues[finalI] = seekBar.getProgress() + question.getAnswersList().get(finalI).getOptionsMinInt();
                            switch (seekBarValues[finalI]){
                                case 0:
                                    seekBarTextViews[finalI].setText(R.string.no);
                                    break;

                                case 1:
                                    seekBarTextViews[finalI].setText(R.string.yes);
                                    break;

                                default:
                            }

                            seekBarTouched[finalI] = true;
                            seekBars[finalI].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_touched_drawable));
                        }
                    });

                    seekBars[i].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_untouched_drawable));
                    seekBars[i].setPadding(getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar));

                    seekBarLayouts[i].addView(seekBars[i]);

                    seekBarTextViews[i] = new TextView(this);
                    seekBarTextViews[i].setText(R.string.no);
                    seekBarTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    seekBarTextViews[i].setGravity(Gravity.CENTER_HORIZONTAL);

                    seekBarLayouts[i].addView(seekBarTextViews[i]);
                    break;

                case Answer.DATE: // Date
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
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
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    break;

                case Answer.DATE_PAST: // Date (Past)
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            Calendar calendar = Calendar.getInstance();
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }

                        // Create the new DatePickerDialog instance.
                        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

                        // Set dialog icon and title.
                        datePickerDialog.setTitle(getString(R.string.please_select_a_date));

                        // Set available dates to past
                        datePickerDialog.setMaxDate(Calendar.getInstance());

                        // Popup the dialog.
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    break;

                case Answer.DATE_FUTURE: // Date (Future)
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
                            calendar.setTime(date);
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }

                        // Create the new DatePickerDialog instance.
                        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

                        // Set dialog icon and title.
                        datePickerDialog.setTitle(getString(R.string.please_select_a_date));

                        // Set available dates to future
                        datePickerDialog.setMinDate(Calendar.getInstance());

                        // Popup the dialog.
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    break;

                case Answer.DATETIME: // Datetime
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
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
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    timeButtons[i] = new Button(this);
                    timeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    timeButtons[i].setOnClickListener(v -> {
                        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                            timeButtons[finalI].setText(String.format("%02d:%02d", hourOfDay, minute));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int hour;
                        int minute;

                        if (timeButtons[finalI].getText().toString().equals("")){
                            hour = calendar.get(Calendar.HOUR_OF_DAY);
                            minute = calendar.get(Calendar.MINUTE);
                        }
                        else {
                            String[] splitTimeString = timeButtons[finalI].getText().toString().split(":");
                            hour = Integer.parseInt(splitTimeString[0]);
                            minute = Integer.parseInt(splitTimeString[1]);
                        }

                        // Create the new TimePickerDialog instance.
                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

                        // Set dialog title.
                        timePickerDialog.setTitle(getString(R.string.please_select_a_time));

                        // Popup the dialog.
                        timePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(timeButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(timeButtons[i]);
                    }

                    break;

                case Answer.DATETIME_PAST: // Datetime (Past)
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
                            calendar.setTime(date);
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }

                        // Create the new DatePickerDialog instance.
                        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

                        // Set dialog icon and title.
                        datePickerDialog.setTitle(getString(R.string.please_select_a_date));

                        // Set available dates to past
                        datePickerDialog.setMaxDate(Calendar.getInstance());

                        // Popup the dialog.
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    timeButtons[i] = new Button(this);
                    timeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    timeButtons[i].setOnClickListener(v -> {
                        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                            timeButtons[finalI].setText(String.format("%02d:%02d", hourOfDay, minute));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int hour;
                        int minute;

                        if (timeButtons[finalI].getText().toString().equals("")){
                            hour = calendar.get(Calendar.HOUR_OF_DAY);
                            minute = calendar.get(Calendar.MINUTE);
                        }
                        else {
                            String[] splitTimeString = timeButtons[finalI].getText().toString().split(":");
                            hour = Integer.parseInt(splitTimeString[0]);
                            minute = Integer.parseInt(splitTimeString[1]);
                        }

                        // Create the new TimePickerDialog instance.
                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

                        // Set dialog title.
                        timePickerDialog.setTitle(getString(R.string.please_select_a_time));

                        // Popup the dialog.
                        timePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(timeButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(timeButtons[i]);
                    }

                    break;

                case Answer.DATETIME_FUTURE: // Datetime (Future)
                    dateButtons[i] = new Button(this);
                    dateButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    dateButtons[i].setOnClickListener(v -> {

                        DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker, year, month, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(year, month, dayOfMonth);
                            dateButtons[finalI].setText(sdf.format(calendar.getTime()));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int year;
                        int month;
                        int day;

                        if (dateButtons[finalI].getText().toString().equals("")) {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                        else {
                            Date date = sdf.parse(dateButtons[finalI].getText().toString(), new ParsePosition(0));
                            calendar.setTime(date);
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }

                        // Create the new DatePickerDialog instance.
                        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(onDateSetListener, year, month, day);

                        // Set dialog icon and title.
                        datePickerDialog.setTitle(getString(R.string.please_select_a_date));

                        // Set available dates to future
                        datePickerDialog.setMinDate(Calendar.getInstance());

                        // Popup the dialog.
                        datePickerDialog.show(getSupportFragmentManager(), "DatePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(dateButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(dateButtons[i]);
                    }

                    timeButtons[i] = new Button(this);
                    timeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    timeButtons[i].setOnClickListener(v -> {
                        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                            timeButtons[finalI].setText(String.format("%02d:%02d", hourOfDay, minute));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int hour;
                        int minute;

                        if (timeButtons[finalI].getText().toString().equals("")){
                            hour = calendar.get(Calendar.HOUR_OF_DAY);
                            minute = calendar.get(Calendar.MINUTE);
                        }
                        else {
                            String[] splitTimeString = timeButtons[finalI].getText().toString().split(":");
                            hour = Integer.parseInt(splitTimeString[0]);
                            minute = Integer.parseInt(splitTimeString[1]);
                        }

                        // Create the new TimePickerDialog instance.
                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

                        // Set dialog title.
                        timePickerDialog.setTitle(getString(R.string.please_select_a_time));

                        // Popup the dialog.
                        timePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(timeButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(timeButtons[i]);
                    }
                    break;

                case Answer.TIME_HH_MM: // Time (hh:mm)
                    timeButtons[i] = new Button(this);
                    timeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    timeButtons[i].setOnClickListener(v -> {
                        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                            timeButtons[finalI].setText(String.format("%02d:%02d", hourOfDay, minute));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int hour;
                        int minute;

                        if (timeButtons[finalI].getText().toString().equals("")){
                            hour = calendar.get(Calendar.HOUR_OF_DAY);
                            minute = calendar.get(Calendar.MINUTE);
                        }
                        else {
                            String[] splitTimeString = timeButtons[finalI].getText().toString().split(":");
                            hour = Integer.parseInt(splitTimeString[0]);
                            minute = Integer.parseInt(splitTimeString[1]);
                        }

                        // Create the new TimePickerDialog instance.
                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, true);

                        // Set dialog title.
                        timePickerDialog.setTitle(getString(R.string.please_select_a_time));

                        // Popup the dialog.
                        timePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(timeButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(timeButtons[i]);
                    }
                    break;

                case Answer.TIME_HH_MM_SS: // Time (hh:mm:ss)
                    timeButtons[i] = new Button(this);
                    timeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    timeButtons[i].setOnClickListener(v -> {
                        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view, hourOfDay, minute, second) -> {
                            timeButtons[finalI].setText(String.format("%02d:%02d:%02d", hourOfDay, minute, second));
                        };

                        Calendar calendar = Calendar.getInstance();
                        int hour;
                        int minute;
                        int second;

                        if (timeButtons[finalI].getText().toString().equals("")){
                            hour = calendar.get(Calendar.HOUR_OF_DAY);
                            minute = calendar.get(Calendar.MINUTE);
                            second = calendar.get(Calendar.SECOND);
                        }
                        else {
                            String[] splitTimeString = timeButtons[finalI].getText().toString().split(":");
                            hour = Integer.parseInt(splitTimeString[0]);
                            minute = Integer.parseInt(splitTimeString[1]);
                            second = Integer.parseInt(splitTimeString[2]);
                        }

                        // Create the new TimePickerDialog instance.
                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(onTimeSetListener, hour, minute, second, true);

                        // Set dialog title.
                        timePickerDialog.setTitle(getString(R.string.please_select_a_time));

                        // Enable seconds
                        timePickerDialog.enableSeconds(true);

                        // Popup the dialog.
                        timePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
                    });

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(timeButtons[i]);
                    }
                    else {
                        linearLayouts[i].addView(timeButtons[i]);
                    }
                    break;

                case Answer.OPTIONAL_CUSTOM: // Optional custom
                case Answer.MANDATORY_CUSTOM: // Mandatory custom
                    editTexts[i] = new EditText(this);
                    editTexts[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    // "InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS" does not work if the user uses SwiftKey of the OS has SwiftKey built in, "InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD" is added as hack.
//                    editTexts[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    // TODO: "InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD" disables several symbols (€, £...) on the S8 phone, look for a fix while still disabling the predictive text.
                    editTexts[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    editTexts[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    editTexts[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_LENGTH_OF_EDIT_TEXT)});

                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                        editTexts[i].setEnabled(false);
                    }
                    else {
                        editTexts[i].setHint(R.string.hint);
                    }

                    if (answerI.getPrefix().length() > getResources().getInteger(R.integer.max_prefix_length)){
                        answersContainerLinearLayout.addView(editTexts[i]);
                    }
                    else {
                        linearLayouts[i].addView(editTexts[i]);
                    }
                    break;
            }

        }

        // If there is not at least one tooltip VISIBLE, remove all tooltips
        if (!atLeastOneTooltip){
            for (int i = 0; i < answersCount; i++){
                imageViews[i].setVisibility(View.GONE);
            }
        }

        scrollView.scrollTo(0,0);

    }

    /**
     * Loads a body chart based on the question.
     *
     * @param question to display the body chart
     */
    private void loadBodyChart(Question question, int questionIndex){

        LayoutInflater inflater = LayoutInflater.from(context);
        View bodyChartParent = inflater.inflate(R.layout.body_chart, answersContainerLinearLayout);

        Button[] removeButtons = new Button[question.getAnswersList().size()];

        bodyChartImageView = bodyChartParent.findViewById(R.id.bodyChartImageView);
        bodyChartImageView.setEnabled(true);
        bodyChartImageView.setMaximumAreas(question.getAnswersList().size());
        bodyChartImageView.setNewAreaDrawnListener(() -> {
            MyLog.d(TAG, "onNewAreaDrawn");

            bodyChartAreas = bodyChartImageView.getAreas();

            for (int i = 0; i < removeButtons.length; i++) {
                if (i <= bodyChartAreas.length - 1) {
                    removeButtons[i].setEnabled(true);
                }
                else {
                    removeButtons[i].setEnabled(false);
                }
            }
        });

        ImageButton painLevel1 = bodyChartParent.findViewById(R.id.pain_level_1);
        ImageButton painLevel2 = bodyChartParent.findViewById(R.id.pain_level_2);
        ImageButton painLevel3 = bodyChartParent.findViewById(R.id.pain_level_3);

        ImageButton painFilled = bodyChartParent.findViewById(R.id.pain_filled);
        ImageButton painPinsAndNeedles = bodyChartParent.findViewById(R.id.pain_pins);

        ImageButton painSmall = bodyChartParent.findViewById(R.id.pain_small);
        ImageButton painNormal = bodyChartParent.findViewById(R.id.pain_normal);
        ImageButton painBig = bodyChartParent.findViewById(R.id.pain_big);

        View.OnClickListener paintClickListener = v -> {
            switch (v.getTag().toString()){
                case "1":
                    bodyChartImageView.setErase(false);
                    // set color to yellow
                    bodyChartImageView.setPainLevel(1);
                    painLevel1.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    painLevel2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painLevel3.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    break;

                case "2":
                    bodyChartImageView.setErase(false);
                    // set color to orange
                    bodyChartImageView.setPainLevel(2);
                    painLevel1.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painLevel2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    painLevel3.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    break;

                case "3":
                    bodyChartImageView.setErase(false);
                    // set color to red
                    bodyChartImageView.setPainLevel(3);
                    painLevel1.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painLevel2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painLevel3.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    break;

                case "filled":
                    bodyChartImageView.setErase(false);
                    // set pattern to filled
                    bodyChartImageView.setPattern("filled");
                    painFilled.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    painPinsAndNeedles.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    break;

                case "pins_and_needles":
                    bodyChartImageView.setErase(false);
                    // set pattern to filled
                    bodyChartImageView.setPattern("pins_and_needles");
                    painFilled.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painPinsAndNeedles.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    break;

                case "small":
                    bodyChartImageView.setErase(false);
                    bodyChartImageView.setBrushSize(getResources().getDimensionPixelSize(R.dimen.brush_size_small));
                    painSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_small_pressed));
                    painNormal.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painBig.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_big));
                    break;

                case "normal":
                    bodyChartImageView.setErase(false);
                    bodyChartImageView.setBrushSize(getResources().getDimensionPixelSize(R.dimen.brush_size_normal));
                    painSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_small));
                    painNormal.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
                    painBig.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_big));
                    break;

                case "big":
                    bodyChartImageView.setErase(false);
                    bodyChartImageView.setBrushSize(getResources().getDimensionPixelSize(R.dimen.brush_size_big));
                    painSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_small));
                    painNormal.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
                    painBig.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_big_pressed));
                    break;
            }
        };

        painLevel1.setOnClickListener(paintClickListener);
        painLevel2.setOnClickListener(paintClickListener);
        painLevel3.setOnClickListener(paintClickListener);
        painFilled.setOnClickListener(paintClickListener);
        painPinsAndNeedles.setOnClickListener(paintClickListener);
        painSmall.setOnClickListener(paintClickListener);
        painNormal.setOnClickListener(paintClickListener);
        painBig.setOnClickListener(paintClickListener);

        bodyChartImageView.setErase(false);
        // set color to orange
        bodyChartImageView.setPainLevel(2);
        painLevel1.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));
        painLevel2.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
        painLevel3.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));

        bodyChartImageView.setErase(false);
        // set pattern to filled
        bodyChartImageView.setPattern("filled");
        painFilled.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
        painPinsAndNeedles.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal));

        bodyChartImageView.setErase(false);
        bodyChartImageView.setBrushSize(getResources().getDimensionPixelSize(R.dimen.brush_size_normal));
        painSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_small));
        painNormal.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_normal_pressed));
        painBig.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selector_big));

        LinearLayout removeButtonsLinearLayout = bodyChartParent.findViewById(R.id.remove_button_linear_layout);

        View.OnClickListener removeListener = v -> {
            int removeIndex = (int) v.getTag();
            bodyChartImageView.removeArea(removeIndex);
        };

        int savedAreaCount = submittingQuestionnaire.getSubmittingQuestionsList().get(questionIndex).getSubmittingAnswerList().size();

        for (int i = 0; i < removeButtons.length; i++) {
            removeButtons[i] = new Button(this);
            removeButtons[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            removeButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.font_size_small));
            removeButtons[i].setText(getString(R.string.remove_site_x, (i + 1)));
            removeButtons[i].setTag(i);
            removeButtons[i].setOnClickListener(removeListener);
            removeButtonsLinearLayout.addView(removeButtons[i]);

            if (i <= savedAreaCount - 1) {
                removeButtons[i].setEnabled(true);
            }
            else {
                removeButtons[i].setEnabled(false);
            }
        }
    }

    /**
     * Loads (a) previously saved answer(s) for the current question.
     *
     * @param questionIndex current question index
     */
    private void loadAnswer(int questionIndex){

        SubmittingQuestion submittingQuestion = submittingQuestionnaire.getSubmittingQuestionsList().get(questionIndex);

        if (submittingQuestion.getSubmittingAnswerList().size() > 0){

            Question question = questionnaire.getQuestionsList().get(questionIndex);

            int bulletType = question.getBulletType();

            final int answersCount = question.getAnswersList().size();

            Answer answer;

            if (bulletType == Question.BODY_CHART){
                bodyChartAreas = new String[submittingQuestion.getSubmittingAnswerList().size()];
            }

            // Iterate through all answers
            for (int i = 0; i < answersCount; i++){

                answer = question.getAnswersList().get(i);

                for (int j = 0; j < submittingQuestion.getSubmittingAnswerList().size(); j++){

                    SubmittingAnswer submittingAnswer = submittingQuestion.getSubmittingAnswerList().get(j);

                    if (submittingAnswer.getId() == answer.getId()){

                        if (bulletType == Question.NO_BULLETS || bulletType == Question.RADIO_BUTTONS ||bulletType == Question.CHECKBOXES){

                            if (bulletType == Question.RADIO_BUTTONS){
                                MyLog.d(TAG, "loadAnswer: radio button checked");
                                radioButtons[i].setChecked(true);
                            }

                            if (bulletType == Question.CHECKBOXES){
                                MyLog.d(TAG, "loadAnswer: checkbox checked");
                                checkBoxes[i].setChecked(true);
                            }

                            // Switch based on answer type
                            switch (answer.getTypeId()){

                                case Answer.FIXED: // Fixed
                                    // Type 1 answer not possible for no bullets or has no value for the radio button or the checkbox
                                    break;

                                case Answer.INTEGER: // Integer
                                    MyLog.d(TAG, "loadAnswer: integer answer loaded");
                                    editTexts[i].append(String.valueOf(submittingAnswer.getIntValue()));
                                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                                        editTexts[i].setEnabled(true);
                                    }
                                    break;

                                case Answer.DOUBLE: // Double
                                    MyLog.d(TAG, "loadAnswer: double answer loaded");
                                    editTexts[i].append(String.valueOf(submittingAnswer.getDoubleValue()));
                                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                                        editTexts[i].setEnabled(true);
                                    }
                                    break;

                                case Answer.SCALE: // Scale
                                case Answer.YES_NO: // Yes/No Scale
                                    MyLog.d(TAG, "loadAnswer: (yes/no) scale answer loaded");
                                    seekBars[i].setProgress(submittingAnswer.getIntValue() - answer.getOptionsMinInt());
                                    seekBarTouched[i] = true;
                                    seekBars[i].setThumb(ContextCompat.getDrawable(context, R.drawable.thumb_touched_drawable));
                                    break;

                                case Answer.DATE: // Date
                                case Answer.DATE_PAST: // Date (Past)
                                case Answer.DATE_FUTURE: // Date (Future)
                                    MyLog.d(TAG, "loadAnswer: date answer loaded");
                                    dateButtons[i].append(submittingAnswer.getStringValue());
                                    break;

                                case Answer.DATETIME: // Datetime
                                case Answer.DATETIME_PAST: // Datetime (Past)
                                case Answer.DATETIME_FUTURE: // Datetime (Future)
                                    MyLog.d(TAG, "loadAnswer: datetime answer loaded");
                                    String[] splitAnswer = submittingAnswer.getStringValue().split(" ");
                                    dateButtons[i].append(splitAnswer[0]);
                                    timeButtons[i].append(splitAnswer[1]);
                                    break;

                                case Answer.TIME_HH_MM: // Time (hh:mm)
                                case Answer.TIME_HH_MM_SS: // Time (hh:mm:ss)
                                    MyLog.d(TAG, "loadAnswer: time answer loaded");
                                    timeButtons[i].append(submittingAnswer.getStringValue());
                                    break;

                                case Answer.OPTIONAL_CUSTOM: // Optional custom
                                    MyLog.d(TAG, "loadAnswer: optional custom answer loaded");
                                    try {
                                        editTexts[i].append(submittingAnswer.getStringValue());
                                    }
                                    catch (NullPointerException e){
                                        MyLog.e(TAG, "Optional custom has no string value: %s", e);
                                    }
                                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                                        editTexts[i].setEnabled(true);
                                    }
                                    break;

                                case Answer.MANDATORY_CUSTOM: // Mandatory custom
                                    MyLog.d(TAG, "loadAnswer: mandatory custom answer loaded");
                                    editTexts[i].append(submittingAnswer.getStringValue());
                                    if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                                        editTexts[i].setEnabled(true);
                                    }
                                    break;
                            }

                        }
                        else if (bulletType == Question.BODY_CHART){
                            bodyChartAreas[j] = submittingAnswer.getStringValue();
                        }
                    }
                }
            }

            if (bulletType == Question.BODY_CHART && bodyChartAreas != null){
                bodyChartImageView.post(() -> {
                    bodyChartImageView.setAreas(bodyChartAreas);
                });
            }
        }
        else {
            MyLog.d(TAG, "No answer for QuestionIndex " + questionIndex);
        }

    }

    /**
     * Checks and saves the answers of the current question.
     * Errors can be shown if needed (for example when going to the next question).
     *
     * @param questionIndex current question index
     * @param showErrors determines whether or not errors are shown
     * @return true if the answers were saved
     *         false if there were errors and the answers are not saved
     */
    private boolean saveAnswer(int questionIndex, boolean showErrors){

        if (questionIndex < 0 || questionIndex == questionnaire.getQuestionsList().size()){
            return true;
        }

        Question question = questionnaire.getQuestionsList().get(questionIndex);

        int bulletType = question.getBulletType();

        final int answersCount = question.getAnswersList().size();

        boolean answerAccepted = true;
        boolean oneRadioChecked = false;
        boolean oneBoxChecked = false;

        String errorString = "";
        String errorStringTemp = "";

        ArrayList<SubmittingAnswer> submittingAnswerList = new ArrayList<>();

        Answer answer;

        // Iterate through all answers
        for (int i = 0; i < answersCount; i++){

            answer = question.getAnswersList().get(i);

            String answerString;
            if (!answer.getPrefix().equals("null") && !answer.getPrefix().equals("") && !answer.getPrefix().equals(" ") && !answer.getPrefix().trim().equals(":")) {
                answerString = "\"" + parseAlternateAnswerNumbering(answer.getPrefix()) + "\"";
            }
            else {
                answerString = "" + (i + 1);
            }

            if (bulletType == Question.NO_BULLETS || (bulletType == Question.RADIO_BUTTONS && radioButtons[i].isChecked()) || (bulletType == Question.CHECKBOXES && checkBoxes[i].isChecked())){

                if (bulletType == Question.RADIO_BUTTONS){
                    oneRadioChecked = true;
                }

                if (bulletType == Question.CHECKBOXES){
                    oneBoxChecked = true;
                }

                // Switch based on answer type
                switch (answer.getTypeId()){

                    case Answer.FIXED: // Fixed
                        if (bulletType == Question.RADIO_BUTTONS || bulletType == Question.CHECKBOXES){
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId()
                                    )
                            );
                        }
                        break;

                    case Answer.INTEGER: // Integer
                        if (editTexts[i].getText().toString().length() != 0){
                            if (answer.isHasOptions()){
                                try {
                                    int integerValue = Integer.parseInt(editTexts[i].getText().toString());

                                    if (integerValue >= answer.getOptionsMinInt() && integerValue <= answer.getOptionsMaxInt()){
                                        submittingAnswerList.add(
                                                new SubmittingAnswer(
                                                        answer.getId(),
                                                        answer.getTypeId(),
                                                        integerValue
                                                )
                                        );
                                    }
                                    else {
                                        errorStringTemp = getString(R.string.value_out_of_range_int, answerString, answer.getOptionsMinInt(), answer.getOptionsMaxInt());
                                        answerAccepted = false;
                                    }
                                }
                                catch (Exception e) {
                                    errorStringTemp = getString(R.string.value_out_of_range_int, answerString, answer.getOptionsMinInt(), answer.getOptionsMaxInt());
                                    answerAccepted = false;
                                }
                            }
                            else {
                                submittingAnswerList.add(
                                        new SubmittingAnswer(
                                                answer.getId(),
                                                answer.getTypeId(),
                                                Integer.parseInt(editTexts[i].getText().toString())
                                        )
                                );
                            }
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;

                    case Answer.DOUBLE: // Double
                        if (editTexts[i].getText().toString().length() != 0){
                            if (answer.isHasOptions()){
                                double doubleValue =  Double.parseDouble(editTexts[i].getText().toString());
                                if (doubleValue >= answer.getOptionsMinDouble() && doubleValue <= answer.getOptionsMaxDouble()){
                                    submittingAnswerList.add(
                                            new SubmittingAnswer(
                                                    answer.getId(),
                                                    answer.getTypeId(),
                                                    doubleValue
                                            )
                                    );
                                }
                                else {
                                    errorStringTemp = getString(R.string.value_out_of_range_double, answerString, answer.getOptionsMinDouble(), answer.getOptionsMaxDouble());
                                    answerAccepted = false;
                                }
                            }
                            else {
                                submittingAnswerList.add(
                                        new SubmittingAnswer(
                                                answer.getId(),
                                                answer.getTypeId(),
                                                Double.parseDouble(editTexts[i].getText().toString())
                                        )
                                );
                            }
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;

                    case Answer.SCALE: // Scale
                    case Answer.YES_NO: // Yes/No Scale
                        if (seekBarTouched[i]){
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            seekBarValues[i]
                                    )
                            );
                        }
                        else {
                            errorStringTemp = getString(R.string.touch_seekbar_once, answerString);
                            answerAccepted = false;
                        }
                        break;


                    case Answer.DATE: // Date
                    case Answer.DATE_PAST: // Date (Past)
                    case Answer.DATE_FUTURE: // Date (Future)
                        if (dateButtons[i].getText().toString().length() != 0) {
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            dateButtons[i].getText().toString()
                                    )
                            );
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;

                    case Answer.DATETIME:
                    case Answer.DATETIME_PAST:
                    case Answer.DATETIME_FUTURE:
                        if (dateButtons[i].getText().toString().length() != 0 && timeButtons[i].getText().toString().length() != 0) {
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            dateButtons[i].getText().toString() + " " + timeButtons[i].getText().toString()
                                    )
                            );
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;

                    case Answer.TIME_HH_MM:
                    case Answer.TIME_HH_MM_SS:
                        if (timeButtons[i].getText().toString().length() != 0) {
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            timeButtons[i].getText().toString()
                                    )
                            );
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;

                    case Answer.OPTIONAL_CUSTOM: // Optional custom
                        if (editTexts[i].getText().toString().length() != 0){
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            editTexts[i].getText().toString()
                                    )
                            );
                        }
                        else if ((bulletType == Question.RADIO_BUTTONS && radioButtons[i].isChecked()) || (bulletType == Question.CHECKBOXES && checkBoxes[i].isChecked())) {
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId()
                                    ));
                        }
                        break;

                    case Answer.MANDATORY_CUSTOM: // Mandatory custom
                        if (editTexts[i].getText().toString().length() != 0){
                            submittingAnswerList.add(
                                    new SubmittingAnswer(
                                            answer.getId(),
                                            answer.getTypeId(),
                                            editTexts[i].getText().toString()
                                    )
                            );
                        }
                        else {
                            errorStringTemp = getString(R.string.fill_in_the_field, answerString);
                            answerAccepted = false;
                        }
                        break;
                }
                if (errorString.equals("")){
                    errorString = errorStringTemp;
                }
            }
            else if(bulletType == Question.BODY_CHART && bodyChartAreas != null){
                if (i < bodyChartAreas.length){
                    submittingAnswerList.add(
                            new SubmittingAnswer(
                                    answer.getId(),
                                    answer.getTypeId(),
                                    bodyChartAreas[i]
                            )
                    );
                }
            }

        }

        if (!oneRadioChecked && bulletType == Question.RADIO_BUTTONS) {
            errorString = getString(R.string.check_a_button);
            answerAccepted = false;
        }

        if (!oneBoxChecked && bulletType == Question.CHECKBOXES) {
            errorString = getString(R.string.check_one_checkbox);
            answerAccepted = false;
        }

        submittingQuestionnaire.getSubmittingQuestionsList().get(questionIndex).setSubmittingAnswerList(submittingAnswerList);

        if (!errorString.equals("") && showErrors){
            Utilities.displayToast(context, errorString);
        }

        MyLog.d(TAG, "Save answers in SharedPreferences");
        String answers_json = gson.toJson(submittingQuestionnaire);
        LibUtilities.printGiantLog(TAG, "Saved answers: " + answers_json, true);
        questionnairesEditor.putString("answers_json", answers_json);
        questionnairesEditor.apply();

        return answerAccepted;
    }

    /**
     * Loads the start screen that contains the questionnaire title and description.
     * Also sets the "Next"- and "Previous"-buttons to VISIBLE and enables the "Next"-button.
     */
    private void loadStartScreen(){

        questionTooltipImageView.setVisibility(View.GONE);

        answersContainerLinearLayout.removeAllViews();
        radioButtons = null;
        checkBoxes = null;
        editTexts = null;
        linearLayouts = null;
        textViews = null;
        imageViews = null;
        seekBarLayouts = null;
        seekBars = null;
        seekBarTextViews = null;
        seekBarValues = null;
        questionsTextViews = null;
        answerTextViews = null;

        previousButton.setEnabled(false);
        previousButton.setVisibility(View.VISIBLE);

        nextButton.setEnabled(true);
        nextButton.setVisibility(View.VISIBLE);

        questionTextView.setText(questionnaire.getDescription());

    }

    /**
     * Loads the end screen that contains an overview of all questions and answers.
     * Also replaces the "Next"-button with a "Submit"-button.
     */
    private void loadEndScreen(){

        endScreen = true;

        questionTooltipImageView.setVisibility(View.GONE);

        answersContainerLinearLayout.removeAllViews();
        radioButtons = null;
        checkBoxes = null;
        editTexts = null;
        linearLayouts = null;
        textViews = null;
        imageViews = null;
        seekBarLayouts = null;
        seekBars = null;
        seekBarTextViews = null;
        seekBarValues = null;
        seekBarTouched = null;
        questionsTextViews = null;
        answerTextViews = null;
        dateButtons = null;
        timeButtons = null;
        bodyChartAreas = null;
        bodyChartImageView = null;

        nextButton.setEnabled(false);
        nextButton.setVisibility(View.GONE);

        submitButton.setEnabled(true);
        submitButton.setVisibility(View.VISIBLE);

        questionTextView.setText(getString(R.string.questionnaire_completed));

        textViews = new TextView[1];

        textViews[0] = new TextView(this);
        textViews[0].setText(getString(R.string.submit_of_return));
        textViews[0].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
        textViews[0].setTextColor(Color.RED);

        answersContainerLinearLayout.addView(textViews[0]);

        int totalAnswers = 0;

        for (int i = 0; i < questionnaire.getQuestionsList().size(); i++){
            for (int j = 0; j < submittingQuestionnaire.getSubmittingQuestionsList().get(i).getSubmittingAnswerList().size(); j++){
                totalAnswers++;
            }
        }

        questionsTextViews = new TextView[questionnaire.getQuestionsList().size()];
        answerTextViews = new TextView[totalAnswers];

        int currentAnswer = 0;

        Question question;
        Answer answer;
        SubmittingQuestion submittingQuestion;
        SubmittingAnswer submittingAnswer;

        for (int i = 0; i < questionnaire.getQuestionsList().size(); i++){

            question = questionnaire.getQuestionsList().get(i);
            submittingQuestion = submittingQuestionnaire.getSubmittingQuestionsList().get(i);

            if (submittingQuestion.getSubmittingAnswerList().size() != 0){

                questionsTextViews[i] = new TextView(this);
                LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                questionParams.setMargins(0,Math.round(LibUtilities.pxFromDp(context, 40)),0,0);
                questionsTextViews[i].setLayoutParams(questionParams);
                questionsTextViews[i].setText(parseAlternateQuestionNumbering(i + 1, question.getQuestion()));
                questionsTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                questionsTextViews[i].setTypeface(null, Typeface.BOLD);

                answersContainerLinearLayout.addView(questionsTextViews[i]);

                if (question.getBulletType() == Question.NO_BULLETS || question.getBulletType() == Question.RADIO_BUTTONS || question.getBulletType() == Question.CHECKBOXES){

                    for (int j = 0; j < submittingQuestion.getSubmittingAnswerList().size(); j++){

                        submittingAnswer = submittingQuestion.getSubmittingAnswerList().get(j);

                        answerTextViews[currentAnswer] = new TextView(this);
                        LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        answerParams.setMargins(0,Math.round(LibUtilities.pxFromDp(context, 8)),0,0);
                        answerTextViews[currentAnswer].setLayoutParams(answerParams);

                        for (int k = 0; k < question.getAnswersList().size(); k++){

                            answer = question.getAnswersList().get(k);

                            if (submittingAnswer.getId() == answer.getId()){

                                String answerValue = "";

                                switch (submittingAnswer.getAnswerType()){
                                    case 0:
                                        answerValue = "";
                                        break;

                                    case 1:
                                        answerValue = String.valueOf(submittingAnswer.getIntValue());
                                        break;

                                    case 2:
                                        answerValue = String.valueOf(submittingAnswer.getDoubleValue());
                                        break;

                                    case 3:
                                        answerValue = submittingAnswer.getStringValue();
                                        break;
                                }

                                // Special case for the "Yes/No Scale"
                                if (submittingAnswer.getTypeId() == Answer.YES_NO){
                                    if (answerValue.equals("1")){
                                        answerValue = getString(R.string.yes);
                                    }
                                    else {
                                        answerValue = getString(R.string.no);
                                    }
                                }

                                if (answer.getPrefix().equals("null") || answer.getPrefix().equals("") || answer.getPrefix().equals(" ") || answer.getPrefix().trim().equals(":")) {
                                    answerTextViews[currentAnswer].setText(String.format("%s %s", getString(R.string.bullet), answerValue));
                                }
                                else {
                                    if (!answerValue.equals("")){

                                        switch (answer.getPrefix().substring(answer.getPrefix().length() - 1)){
                                            case ":":
                                            case "?":
                                            case "!":
                                                answerTextViews[currentAnswer].setText(String.format("%s %s %s", getString(R.string.bullet), parseAlternateAnswerNumbering(answer.getPrefix()), answerValue));
                                                break;

                                            default:
                                                answerTextViews[currentAnswer].setText(String.format("%s %s: %s", getString(R.string.bullet), parseAlternateAnswerNumbering(answer.getPrefix()), answerValue));
                                        }

                                    }
                                    else {
                                        answerTextViews[currentAnswer].setText(String.format("%s %s", getString(R.string.bullet), parseAlternateAnswerNumbering(answer.getPrefix())));
                                    }

                                }
                                answerTextViews[currentAnswer].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));

                                answersContainerLinearLayout.addView(answerTextViews[currentAnswer]);

                            }

                        }

                        currentAnswer++;
                    }

                }
                else if (question.getBulletType() == Question.BODY_CHART){

                    LinearLayout horizontalLinearLayout = new LinearLayout(this);
                    horizontalLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    horizontalLinearLayout.setPadding(0, getResources().getDimensionPixelSize(R.dimen.padding_button),0,0);

                    bodyChartImageView = new BodyChartImageView(this, question.getAnswersList().size());
                    bodyChartImageView.setEnabled(false);
                    bodyChartImageView.setBackground(ContextCompat.getDrawable(context, R.drawable.body_chart));
                    bodyChartImageView.setLayoutParams(new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.body_chart_size), getResources().getDimensionPixelSize(R.dimen.body_chart_size)));
                    bodyChartImageView.setAdjustViewBounds(true);

                    answersContainerLinearLayout.addView(horizontalLinearLayout);
                    horizontalLinearLayout.addView(bodyChartImageView);

                    bodyChartAreas = new String[submittingQuestion.getSubmittingAnswerList().size()];

                    for (int j = 0; j < submittingQuestion.getSubmittingAnswerList().size(); j++){
                        submittingAnswer = submittingQuestion.getSubmittingAnswerList().get(j);
                        bodyChartAreas[j] = submittingAnswer.getStringValue();
                        currentAnswer++;
                    }

                    if (bodyChartAreas != null){
                        bodyChartImageView.post(() -> {
                            bodyChartImageView.setAreas(bodyChartAreas);
                        });
                    }
                }
            }
        }

        if (!previousButton.isEnabled()){
            previousButton.setEnabled(true);
        }
    }

    /**
     * Saves the answers for the current questionnaire, it also sets the completion time.
     * If there is an active internet connection, it will try to upload the answers.
     * If there is no active internet connection, it will save the answers for later upload.
     */
    private void saveQuestionnaire(){

        if (!questionnaire.isDraft() || PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_QNR_DRAFT_SUBMIT, false)){

            submittingQuestionnaire.setStopMillis(System.currentTimeMillis());

            String submitString = Utilities.serializeAnswers(context, submittingQuestionnaire);

            if (Utilities.checkNetworkConnection(context)){
                submitQuestionnaire(submitString);
            }
            else {
                int index = 0;
                do{
                    index++;
                }
                while (questionnairesSharedPreferences.contains("remaining_questionnaire_" + index));

                questionnairesEditor.putString("remaining_questionnaire_" + index, submitString).remove("answers_json").apply();

                Utilities.displayToast(context, getString(R.string.no_internet_access_questionnaire_saved));

                if (submittingQuestionnaire.getPreviousSubmissionId() > 0){
                    Intent intent = new Intent(context, SubmissionsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else {
                    finish();
                }
            }
        }
        else {

            questionnairesEditor.remove("answers_json").apply();

            Utilities.displayToast(context, getString(R.string.drafts_cant_be_submitted));

            if (submittingQuestionnaire.getPreviousSubmissionId() > 0){
                Intent intent = new Intent(context, SubmissionsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else {
                finish();
            }

        }
    }

    /**
     * Creates and executes a network request to submit the answers of a questionnaire.
     *
     * @param jsonString JSON containing the answers of a questionnaire.
     */
    private void submitQuestionnaire(final String jsonString){
        String tag_string_req = "sbm_set_questionnaire";

        pDialog.setMessage(getString(R.string.submitting_your_answers_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/");

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    Utilities.displayToast(context, getString(R.string.questionnaire_successfully_submitted));
                    questionnairesEditor.remove("answers_json").apply();
                    hideDialog();

                    if (submittingQuestionnaire.getPreviousSubmissionId() > 0){
                        Intent intent = new Intent(context, SubmissionsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    else {
                        finish();
                    }
                }, e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
                    hideDialog();

                    Bundle bundle = new Bundle();
                    bundle.putString("JSON_STRING", jsonString);

                    SimpleDialog.build()
                            .title(R.string.submitting_failed)
                            .msg(R.string.couldnt_submit_questionnaire)
                            .pos(R.string.yes)
                            .neg(R.string.no)
                            .extra(bundle)
                            .cancelable(false)
                            .show(QuestionPerQuestionActivity.this, SUBMIT_ERROR_DIALOG);
                }) {

            // replaces getParams with getBody
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            // replaces getParams with getBodyContentType
            @Override
            public byte[] getBody() {
                return jsonString == null ? null : jsonString.getBytes(StandardCharsets.UTF_8);
            }

            // Deprecated by getBodyContentType and getBody
//            @Override
//            protected Map<String, String> getParams() {
//                // Posting parameters to login url
//                Map<String, String> params = new HashMap<>();
//                params.put("json", jsonString);
//
//                return params;
//            }

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
     * Creates the intent to let the user choose the location to save the PDF file.
     */
    private void createPdfIntent() {
        String name = "questionnaire_" + questionnaireId + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, name);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_PDF_FILE);
    }

    /**
     * Receives the result from an activity.
     * In this case, receiving the PDF save location is the only option, if the result is OK,
     * the PDF is created.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_PDF_FILE) {
            if (resultCode == RESULT_OK) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createPdfFile(data);
                    }
                }, 10);
            } else {
                hideDialog();
                Utilities.displayToast(context, getString(R.string.pdf_not_saved));
            }
        }
    }

    /**
     * Creates and outputs the PDF file.
     *
     * @param data contains the save location
     */
    private void createPdfFile(Intent data) {
        Bitmap radioButtonBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.radio_button);
        Bitmap checkBoxBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.check_box);

        mPDFWriter = new PDFWriter();

        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
        addText(defaultLeftMargin, currentTopPositionFromBottom, titleFontSize, questionnaire.getTitle());
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nomade_logo_small);
        mPDFWriter.addImageKeepRatio(420, 735, 65, 65, bitmap);

        String descriptionString = questionnaire.getDescription();

        if (descriptionString.length() > 0) {
            String[] splitDescription = descriptionString.split(System.lineSeparator());
            for (String s : splitDescription) {
                currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
                addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, s);
            }
        }

        Question question;
        Answer answer;

        for (int i = 0; i < questionnaire.getQuestionsList().size(); i++) {

            question = questionnaire.getQuestionsList().get(i);

            String questionString = parseAlternateQuestionNumbering(i + 1, question.getQuestion());
            questionString = questionString.replaceAll("\\/","\\\\/");
            questionString = questionString.replaceAll("\\(","\\\\(");
            questionString = questionString.replaceAll("\\)","\\\\)");
            questionString = questionString.replaceAll("–","-");
            questionString = questionString.replaceAll("’","'");

            String[] splitQuestion = questionString.split(System.lineSeparator());

            for (int k = 0; k < splitQuestion.length; k++) {

                if (k == 0) {
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
                }
                else {
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, smallInterlinearDistance);
                }
                addText(questionLeftMargin, currentTopPositionFromBottom, defaultFontSize, splitQuestion[k]);
            }

            for (int k = 0; k < question.getAnswersList().size(); k++) {

                answer = question.getAnswersList().get(k);

                String prefix = answer.getPrefix();

                if (prefix.equals("null") || prefix.equals("") || prefix.equals(" ")) {
                    prefix = "..................................................";
                }
                else {
                    if (answer.getTypeId() > 1) {
                        switch (prefix.substring(prefix.length() - 1)){
                            case ":":
                            case "?":
                            case "!":
                                prefix = String.format("%s ", parseAlternateAnswerNumbering(prefix));
                                break;

                            default:
                                prefix = String.format("%s: ", parseAlternateAnswerNumbering(prefix));
                        }
                    }
                    else {
                        prefix = String.format("%s ", parseAlternateAnswerNumbering(prefix));
                    }

                    switch (answer.getTypeId()) {

                        case Answer.FIXED: // Fixed
                            // Type 1 answer not possible for no bullets or already added as setText for the radio button or the check_box
                            break;

                        case Answer.INTEGER: // Integer
                        case Answer.DOUBLE: // Double
                        case Answer.SCALE: // Scale
                            prefix = String.format("%s ...", prefix);
                            break;

                        case Answer.YES_NO: // Yes/No Scale
                            prefix = String.format("%s %s // %s", prefix, getString(R.string.yes), getString(R.string.no));
                            break;

                        case Answer.DATE: // Date
                        case Answer.DATE_PAST: // Date (Past)
                        case Answer.DATE_FUTURE: // Date (Future)
                            prefix = String.format("%s ../../....", prefix);
                            break;

                        case Answer.DATETIME: // Datetime
                        case Answer.DATETIME_PAST: // Datetime
                        case Answer.DATETIME_FUTURE: // Datetime
                            prefix = String.format("%s ../../.... ..:..", prefix);
                            break;

                        case Answer.TIME_HH_MM:
                            prefix = String.format("%s ..:..", prefix);
                            break;

                        case Answer.TIME_HH_MM_SS:
                            prefix = String.format("%s ..:..:..", prefix);
                            break;

                        case Answer.OPTIONAL_CUSTOM: // Optional custom
                        case Answer.MANDATORY_CUSTOM: // Mandatory custom
                            prefix = String.format("%s ..................................................", prefix);
                            break;
                    }
                }

                if (question.getBulletType() == Question.NO_BULLETS) {
                    // No bullets
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
                    addText(answerLeftMargin, currentTopPositionFromBottom, defaultFontSize, prefix);
                }
                else if (question.getBulletType() == Question.RADIO_BUTTONS) {
                    // Radio buttons
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
                    mPDFWriter.addImageKeepRatio(answerLeftMargin, currentTopPositionFromBottom - 1, 10, 10, radioButtonBitmap);
                    addText(answerLeftMargin + 15, currentTopPositionFromBottom, defaultFontSize, prefix);
                }
                else if (question.getBulletType() == Question.CHECKBOXES) {
                    // Checkboxes
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
                    mPDFWriter.addImageKeepRatio(answerLeftMargin, currentTopPositionFromBottom - 1, 10, 10, checkBoxBitmap);
                    addText(answerLeftMargin + 15, currentTopPositionFromBottom, defaultFontSize, prefix);
                }
            }
        }

        int pageCount = mPDFWriter.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            mPDFWriter.setCurrentPage(i);
            mPDFWriter.addText(550, 30, 10, (i + 1) + " / " + pageCount);
        }

        String pdfContent = mPDFWriter.asString();

        try {
            Uri uri = data.getData();

            OutputStream outputStream = getContentResolver().openOutputStream(uri);

            outputStream.write(pdfContent.getBytes(crl.android.pdfwriter.StandardCharsets.ISO_8859_1));

            outputStream.close();

            hideDialog();
            runOnUiThread(() -> Utilities.displayToast(context, getString(R.string.pdf_saved)));
        } catch (IOException e) {
            hideDialog();
            runOnUiThread(() -> Utilities.displayToast(context, getString(R.string.pdf_failed)));
        }
    }

    /**
     * Adds text to the PDF file.
     * If the text is too long, the text will be split and printed on the next line.
     */
    private void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text) {
        Paint paint = new TextPaint();
        paint.setTextSize(fontSize);
        paint.setTypeface(Typeface.create("Times New Roman", Typeface.NORMAL));
        float textLength = paint.measureText(text);

        if (leftPosition + textLength > A4_WIDTH - 40) {
            int lastSpace = text.lastIndexOf(' ');
            String truncatedText = text.substring(0, lastSpace);
            textLength = paint.measureText(truncatedText);

            while (leftPosition + textLength > A4_WIDTH - 40) {
                lastSpace = truncatedText.lastIndexOf(' ');
                if (lastSpace == -1) {
                    break;
                }
                truncatedText = truncatedText.substring(0, lastSpace);
                textLength = paint.measureText(truncatedText);
            }

            mPDFWriter.addText(leftPosition, topPositionFromBottom, fontSize, truncatedText);

            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, fontSize, smallInterlinearDistance);
            addText(leftPosition, currentTopPositionFromBottom, defaultFontSize, text.substring(lastSpace + 1));
        }
        else {
            mPDFWriter.addText(leftPosition, topPositionFromBottom, fontSize, text);
        }
    }

    /**
     * Moves the cursor for the PDF writer, creates new page if needed.
     * @return new cursor position
     */
    private int moveCursor(int currentTopPositionFromBottom, int previousFontSize, int interlinearDistance) {
        if (currentTopPositionFromBottom < 80) {
            mPDFWriter.newPage();
            return 772;
        }
        else {
            return currentTopPositionFromBottom - previousFontSize - interlinearDistance;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_question_per_question, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem downloadPdf = menu.findItem(R.id.action_create_pdf);
        downloadPdf.setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_create_pdf) {
            pDialog.setMessage(getString(R.string.creating_pdf_version_ellipsis));
            showDialog();
            createPdfIntent();
        } else if (itemId == R.id.action_question_overview) {
            showQuestionOverview();
        }
        return true;
    }

    /**
     * Shows a dialog containing an overview of the questions,
     */
    private void showQuestionOverview() {
        pDialog.setMessage(getString(R.string.loading_ellipsis));
        showDialog();

        saveAnswer(currentQuestionIndex, false);

        ArrayList<String> stringArrayList = new ArrayList<>();
        ArrayList<Long> longArrayList = new ArrayList<>();

        for (int k = 0; k < questionnaire.getQuestionsList().size(); k++) {

            Question currentQuestion = questionnaire.getQuestionsList().get(k);
            SubmittingQuestion currentSubmittingQuestion = submittingQuestionnaire.getSubmittingQuestionsList().get(k);

            boolean add = false;
            boolean breakLoop = false;

            if (currentQuestion.getConditional() == 1) {

                SubmittingQuestion submittingQuestion;
                SubmittingAnswer submittingAnswer;

                for (int l = 0; l < submittingQuestionnaire.getSubmittingQuestionsList().size(); l++){

                    submittingQuestion = submittingQuestionnaire.getSubmittingQuestionsList().get(l);

                    if (submittingQuestion.getId() == currentQuestion.getConditionQuestionId()){
                        boolean skip = true;
                        for (int m = 0; m < submittingQuestion.getSubmittingAnswerList().size(); m++){

                            submittingAnswer = submittingQuestion.getSubmittingAnswerList().get(m);

                            if (submittingAnswer.getId() == currentQuestion.getConditionAnswerId()){
                                skip = false;
                            }
                        }
                        if (!skip){
                            add = true;
                        }
                        break;
                    }
                }
            }
            else {
                add = true;
            }

            // if add is true, check if the loop should be broken and add the question
            if (add) {

                // if there are no bullets (=1), iterate through all answers
                if (currentQuestion.getBulletType() == Question.NO_BULLETS) {

                    for (Answer answer : currentQuestion.getAnswersList()) {

                        // if the answer is not fixed (=1) and not optional custom (=98), an answer is needed
                        if (answer.getTypeId() != 1 && answer.getTypeId() != 98) {

                            boolean matched = false;

                            for (SubmittingAnswer submittingAnswer : currentSubmittingQuestion.getSubmittingAnswerList()) {

                                if (answer.getId() == submittingAnswer.getId()){
                                    matched = true;
                                    break;
                                }

                            }

                            // if no match was found, breakLoop
                            if (!matched) {
                                breakLoop = true;
                                break;
                            }
                        }
                    }

                }
                else {
                    if (currentSubmittingQuestion.getSubmittingAnswerList().size() == 0 && currentQuestion.getBulletType() != 5) {
                        breakLoop = true;
                    }
                }

                stringArrayList.add(parseAlternateQuestionNumbering(k + 1, currentQuestion.getQuestion()));
                longArrayList.add((long) k);
            }

            // if breakLoop is true, break
            if (breakLoop) {
                break;
            }
        }

        String[] labels = new String[stringArrayList.size()];
        long[] ids = new long[longArrayList.size()];

        for (int k = 0; k < stringArrayList.size(); k ++) {
            labels[k] = stringArrayList.get(k);
            if (labels[k].length() > 50) {
                labels[k] = labels[k].substring(0, 47);
                labels[k] = labels[k].trim();
                labels[k] = labels[k] + getString(R.string.ellipsis);
            }
            ids[k] = longArrayList.get(k);
        }

        hideDialog();

        if (currentQuestionIndex >= 0 && currentQuestionIndex < questionnaire.getQuestionsList().size()){
            SimpleListDialog.build()
                    .title("Pick a question to navigate to")
                    .items(labels, ids)
                    .choiceIdPreset(currentQuestionIndex)
                    .choiceMode(CustomListDialog.SINGLE_CHOICE)
                    .show(QuestionPerQuestionActivity.this, QUESTION_OVERVIEW_DIALOG);
        }
        else {
            SimpleListDialog.build()
                    .title("Pick a question to navigate to")
                    .items(labels, ids)
                    .choiceMode(CustomListDialog.SINGLE_CHOICE)
                    .show(QuestionPerQuestionActivity.this, QUESTION_OVERVIEW_DIALOG);
        }
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
    public void onBackPressed() {

        if (jsonParsedSuccessful){
            if (currentQuestionIndex < questionnaire.getQuestionsList().size()){
                saveAnswer(currentQuestionIndex, false);
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        switch (dialogTag) {
            case SUBMIT_ERROR_DIALOG:
                String jsonString = extras.getString("JSON_STRING");
                switch (which){
                    case BUTTON_POSITIVE:
                        submitQuestionnaire(jsonString);
                        return true;
                    case BUTTON_NEGATIVE:
                        int index = 0;
                        do{
                            index++;
                        }
                        while (questionnairesSharedPreferences.contains("remaining_questionnaire_" + index));

                        questionnairesEditor.putString("remaining_questionnaire_" + index, jsonString).remove("answers_json").apply();

                        Utilities.displayToast(context, getString(R.string.questionnaire_saved));

                        if (submittingQuestionnaire.getPreviousSubmissionId() > 0){
                            Intent intent = new Intent(context, SubmissionsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("EDIT_WAS_MADE", true);
                            startActivity(intent);
                        }
                        else {
                            finish();
                        }
                        return true;
                }
                break;

            case QUESTION_OVERVIEW_DIALOG:
                if (which == BUTTON_POSITIVE) {
                    int selection = (int) extras.getLong(SimpleListDialog.SELECTED_SINGLE_ID);
                    if (selection != currentQuestionIndex) {
                        loadQuestionFromOverview(selection);
                        return true;
                    }
                }
                break;

            case OTHER_USER_DIALOG:
                if (which == BUTTON_POSITIVE) {
                    pDialog.setMessage(getString(R.string.getting_user_list_ellipsis));
                    showDialog();
                    getUserList();
                }
                break;

            case CHOOSE_OTHER_USER_DIALOG:
                if (which == BUTTON_POSITIVE) {
                    long otherUserId = extras.getLong(SimpleListDialog.SELECTED_SINGLE_ID);
                    submittingQuestionnaire.setForUserId((int) otherUserId);
                    submittingQuestionnaire.setForUserIdChanged(true);
                    submittingQuestionnaire.setForUserName(extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL));
                    titleTextView.setText(String.format("%s%s%s", questionnaire.getTitle(), getString(R.string._for_), submittingQuestionnaire.getForUserName()));
                }
                break;
        }

        return false;
    }

    /**
     * Creates and executes a request to get a list of the users (developers only).
     */
    private void getUserList() {
        String tag_string_req = "user_list";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE).edit().putString(Constants.API_USERS, response).apply();
                    stringJsonUserList = response;
                    parseUserList(stringJsonUserList);
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

    private void parseUserList(String userList) {

        try {

            int ownCompanyId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("company_id", -1);

            ArrayList<User> userArrayList = new ArrayList<>();

            if (!userList.equals("")){
                JSONObject jObjUserList = new JSONObject(userList);

                JSONArray jUsersArray = jObjUserList.getJSONArray("data");

                for (int i = 0; i < jUsersArray.length(); i++){

                    JSONObject currentUser = jUsersArray.getJSONObject(i);

                    int id = currentUser.getInt("id");
                    String name = currentUser.getString("username");
                    int companyId = currentUser.getInt("company_id");

                    if (companyId == ownCompanyId || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)) {
                        userArrayList.add(new User(id, name, companyId));
                    }
                }

                String[] labels = new String[userArrayList.size()];
                long[] ids = new long[userArrayList.size()];

                for (int i = 0; i < userArrayList.size(); i++) {
                    User currentUser = userArrayList.get(i);
                    labels[i] = currentUser.getName();
                    ids[i] = currentUser.getUserId();
                }

                SimpleListDialog.build()
                        .title(R.string.select_a_user_colon)
                        .items(labels, ids)
                        .choiceMode(CustomListDialog.SINGLE_CHOICE)
                        .choiceMin(1)
                        .pos(R.string.ok)
                        .neut(R.string.cancel)
                        .cancelable(false)
                        .show(QuestionPerQuestionActivity.this, CHOOSE_OTHER_USER_DIALOG);

                hideDialog();
            }
        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        hideDialog();
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
