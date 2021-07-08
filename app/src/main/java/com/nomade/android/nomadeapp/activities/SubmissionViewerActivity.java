package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Answer;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Question;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.BodyChartImageView;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.NewJsonLib;
import com.nomade.android.nomadeapp.helperClasses.Utilities;

import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireQuestion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.StandardCharsets;
import crl.android.pdfwriter.StandardFonts;
import eltos.simpledialogfragment.SimpleDialog;

/**
 * SubmittedQuestionnaireViewerActivity
 *
 * Activity to display a submitted questionnaire, has a button in the action bar to download a PDF version
 */
public class SubmissionViewerActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "SubmissionViewerActivit";
    private final Context context = this;

    private ProgressDialog pDialog;

    private String submittedQuestionnaireId;
    private String username = "";
    private SubmittedQuestionnaire submittedQuestionnaire;
    private Questionnaire questionnaire;

    private SharedPreferences submissionsSharedPreferences;
    private SharedPreferences.Editor submissionsEditor;

    private TextView viewOriginalSubmissionTextView;
    private View dividerView;
    private TextView notTheLatestVersionTextView;
    private TextView titleTextView;
    private TextView dateTextView;
    private TextView descriptionTextView;
    private LinearLayout submittedQuestionnaireContainerLinearLayout;

    private String jsonResponseString = "";

    private static final int CREATE_PDF_FILE = 1;

    private static final String ALREADY_EDITED_DIALOG = "dialogTagAlreadyEdited";
    private static final String EDIT_DIALOG = "dialogTagEdit";
    private static final String COPY_DIALOG = "dialogTagCopy";

    private boolean alternateNumbering = false;

    TextView[] questionsTextViews;
    TextView[] answerTextViews;
    BodyChartImageView bodyChartImageView;

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
        setContentView(R.layout.activity_submitted_questionnaire_viewer);

        submissionsSharedPreferences = getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE);
        submissionsEditor = submissionsSharedPreferences.edit();
        submissionsEditor.apply();

        viewOriginalSubmissionTextView = findViewById(R.id.view_original_submission_text_view);
        dividerView = findViewById(R.id.divider_view);
        notTheLatestVersionTextView = findViewById(R.id.not_the_latest_version_text_view);
        titleTextView = findViewById(R.id.name_text_view);
        dateTextView = findViewById(R.id.date_text_view);
        descriptionTextView = findViewById(R.id.description_text_view);
        submittedQuestionnaireContainerLinearLayout = findViewById(R.id.submitted_questionnaire_container_linear_layout);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        submittedQuestionnaireId = getIntent().getStringExtra("QUESTIONNAIRE_ID");
        boolean downloadJson = getIntent().getBooleanExtra("DOWNLOAD_JSON", true);

        if (getIntent().hasExtra("USERNAME")) {
            username = getIntent().getStringExtra("USERNAME");
        }

        if (savedInstanceState != null) {
            jsonResponseString = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS_ + submittedQuestionnaireId, "");
            parseJsonResponse(jsonResponseString);
        } else {
            if (downloadJson && Utilities.checkNetworkConnection(context)) {
                getSubmittedQuestionnaire();
            } else {
                jsonResponseString = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS_ + submittedQuestionnaireId, "");
                if (jsonResponseString != null && !jsonResponseString.equals("")) {
                    parseJsonResponse(jsonResponseString);
                    Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                } else {
                    titleTextView.setText(R.string.no_offline_data);
                    Utilities.displayToast(context, getString(R.string.no_internet_access));
                }
            }
        }
    }

    /**
     * Creates and executes a request to get a submitted questionnaire.
     */
    private void getSubmittedQuestionnaire() {
        String tag_string_req = "sbm_get_questionnaire";

        pDialog.setMessage(getString(R.string.getting_selected_questionnaire_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString(Constants.API_SUBMISSIONS_ + submittedQuestionnaireId, response).apply();
                    jsonResponseString = response;
                    parseJsonResponse(jsonResponseString);
                }, e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

                    jsonResponseString = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS_ + submittedQuestionnaireId, "");
                    if (jsonResponseString != null && !jsonResponseString.equals("")) {
                        parseJsonResponse(jsonResponseString);
                        hideDialog();
                        Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA);
                    } else {
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
     * Parses the JSON containing the submitted questionnaire.
     *
     * @param response JSON containing the submitted questionnaire
     */
    private void parseJsonResponse(String response) {

        try{
            JSONObject jQuestionnaire = new JSONObject(response).getJSONObject("data").getJSONObject("questionnaire");

            questionnaire = NewJsonLib.parseJsonObjectQuestionnaire(context, jQuestionnaire);

            submittedQuestionnaire = NewJsonLib.parseJsonSubmittedQuestionnaire(context, response, questionnaire);

            if (submittedQuestionnaire != null && username != null && !username.equals("")) {
                submittedQuestionnaire.setUserName(username);
            }

            if (submittedQuestionnaire != null) {
                displaySubmittedQuestionnaire();
            } else {
                hideDialog();
                finish();
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
            Utilities.displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
        }
    }

    /**
     * Displays the submitted questionnaire.
     */
    private void displaySubmittedQuestionnaire() {

        for (Question question : questionnaire.getQuestionsList()) {
            String stringQuestion = question.getQuestion();
            int hashCount = stringQuestion.length() - stringQuestion.replaceAll("#", "").length();

            if (hashCount == 2) {
                alternateNumbering = true;
                break;
            }
        }

        int totalAnswers = 0;

        titleTextView.setText(submittedQuestionnaire.getTitle());

//        dateTextView.setText(submittedQuestionnaire.getDate());
        if (submittedQuestionnaire.getEditDate().equals("null") || submittedQuestionnaire.getDate().equals(submittedQuestionnaire.getEditDate())) {
            dateTextView.setText(String.format("%s%s\n%s%s\n%s%s", getString(R.string.list_submission_date_colon), submittedQuestionnaire.getDate(), getString(R.string.started_at_colon), submittedQuestionnaire.getStartedAt(), getString(R.string.finished_at_colon), submittedQuestionnaire.getFinishedAt()));
        } else {
            dateTextView.setText(String.format("%s%s\n%s%s\n%s%s\n%s%s", getString(R.string.list_submission_date_colon), submittedQuestionnaire.getDate(), getString(R.string.list_last_edit_colon), submittedQuestionnaire.getEditDate(), getString(R.string.started_at_colon), submittedQuestionnaire.getStartedAt(), getString(R.string.finished_at_colon), submittedQuestionnaire.getFinishedAt()));
        }

        if (submittedQuestionnaire.getDescription().length() > 0) {
            descriptionTextView.setText(submittedQuestionnaire.getDescription());
            descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            descriptionTextView.setVisibility(View.GONE);
        }

        for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++) {
            for (int j = 0; j < submittedQuestionnaire.getQuestionsList().get(i).getAnswersList().size(); j++) {
                totalAnswers++;
            }
        }

        questionsTextViews = new TextView[submittedQuestionnaire.getQuestionsList().size()];
        answerTextViews = new TextView[totalAnswers];

        int currentAnswer = 0;

        SubmittedQuestionnaireQuestion submittedQuestionnaireQuestion;
        SubmittedQuestionnaireAnswer submittedQuestionnaireAnswer;

        for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++) {

            submittedQuestionnaireQuestion = submittedQuestionnaire.getQuestionsList().get(i);

            questionsTextViews[i] = new TextView(this);
            LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            questionParams.setMargins(0, Math.round(LibUtilities.pxFromDp(context, 40)), 0, 0);
            questionsTextViews[i].setLayoutParams(questionParams);
            questionsTextViews[i].setText(parseAlternateQuestionNumbering(i + 1, submittedQuestionnaireQuestion.getQuestion()));
            questionsTextViews[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
            questionsTextViews[i].setTypeface(null, Typeface.BOLD);

            submittedQuestionnaireContainerLinearLayout.addView(questionsTextViews[i]);

            if (submittedQuestionnaireQuestion.getBulletType() == Question.NO_BULLETS || submittedQuestionnaireQuestion.getBulletType() == Question.RADIO_BUTTONS || submittedQuestionnaireQuestion.getBulletType() == Question.CHECKBOXES) {

                for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++) {

                    submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);

                    answerTextViews[currentAnswer] = new TextView(this);
                    LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    answerParams.setMargins(0, Math.round(LibUtilities.pxFromDp(context, 8)), 0, 0);
                    answerTextViews[currentAnswer].setLayoutParams(answerParams);

                    String answerValue = submittedQuestionnaireAnswer.getAnswer();

                    // Special case for the "Yes/No Scale"
                    if (submittedQuestionnaireAnswer.getTypeId() == Answer.YES_NO) {
                        if (answerValue.equals("1")) {
                            answerValue = getString(R.string.yes);
                        } else {
                            answerValue = getString(R.string.no);
                        }
                    }

                    if (submittedQuestionnaireAnswer.getPrefix().equals("null") || submittedQuestionnaireAnswer.getPrefix().equals("") || submittedQuestionnaireAnswer.getPrefix().equals(" ") || submittedQuestionnaireAnswer.getPrefix().trim().equals(":")) {
                        answerTextViews[currentAnswer].setText(String.format("%s %s", getString(R.string.bullet), answerValue));
                    } else {
                        if (!answerValue.equals("") && !answerValue.equals("null")) {

                            switch (submittedQuestionnaireAnswer.getPrefix().substring(submittedQuestionnaireAnswer.getPrefix().length() - 1)) {
                                case ":":
                                case "?":
                                case "!":
                                    answerTextViews[currentAnswer].setText(String.format("%s %s %s", getString(R.string.bullet), parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue));
                                    break;

                                default:
                                    answerTextViews[currentAnswer].setText(String.format("%s %s: %s", getString(R.string.bullet), parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue));
                            }

                        } else {
                            answerTextViews[currentAnswer].setText(String.format("%s %s", getString(R.string.bullet), parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix())));
                        }

                    }
                    answerTextViews[currentAnswer].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));

                    submittedQuestionnaireContainerLinearLayout.addView(answerTextViews[currentAnswer]);

                    currentAnswer++;
                }

            }
            else if (submittedQuestionnaireQuestion.getBulletType() == Question.BODY_CHART){

                LinearLayout horizontalLinearLayout = new LinearLayout(this);
                horizontalLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                horizontalLinearLayout.setPadding(0, getResources().getDimensionPixelSize(R.dimen.padding_button),0,0);

                bodyChartImageView = new BodyChartImageView(this, submittedQuestionnaireQuestion.getAnswersList().size());
                bodyChartImageView.setEnabled(false);
                bodyChartImageView.setBackground(ContextCompat.getDrawable(context, R.drawable.body_chart));
                bodyChartImageView.setLayoutParams(new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.body_chart_size), getResources().getDimensionPixelSize(R.dimen.body_chart_size)));
                bodyChartImageView.setAdjustViewBounds(true);

                submittedQuestionnaireContainerLinearLayout.addView(horizontalLinearLayout);
                horizontalLinearLayout.addView(bodyChartImageView);

                String[] bodyChartAreas = new String[submittedQuestionnaireQuestion.getAnswersList().size()];

                for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++){
                    submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);
                    bodyChartAreas[j] = submittedQuestionnaireAnswer.getAnswer();
                    currentAnswer++;
                }

                if (bodyChartAreas != null){
                    bodyChartImageView.post(() -> {
                        bodyChartImageView.setAreas(bodyChartAreas);
                    });
                }

            }

        }

        invalidateOptionsMenu();

        if (submittedQuestionnaire.getPreviousSubmissionId() > 0){
            viewOriginalSubmissionTextView.setOnClickListener(v -> {
                if (Utilities.checkNetworkConnection(context)){
                    Intent intent = new Intent(context, SubmissionViewerActivity.class);
                    intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getPreviousSubmissionId()));
                    intent.putExtra("DOWNLOAD_JSON", true);
                    startActivity(intent);
                }
                else {
                    if (submissionsSharedPreferences.contains(Constants.API_SUBMISSIONS_ + submittedQuestionnaire.getPreviousSubmissionId())){
                        Intent intent = new Intent(context, SubmissionViewerActivity.class);
                        intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getPreviousSubmissionId()));
                        intent.putExtra("DOWNLOAD_JSON", false);
                        startActivity(intent);
                    }
                    else {
                        Utilities.displayToast(context, R.string.no_internet_access);
                    }
                }
            });

            viewOriginalSubmissionTextView.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.VISIBLE);
        }

        if (submittedQuestionnaire.getNextSubmissionId() > 0){
            notTheLatestVersionTextView.setVisibility(View.VISIBLE);
        }

        hideDialog();
    }

    /**
     * Creates the intent to let the user choose the location to save the PDF file.
     */
    private void createPdfIntent() {
        String name = "submitted_questionnaire_" + submittedQuestionnaire.getUserId() + "_" + submittedQuestionnaireId + ".pdf";

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
        Bitmap bulletBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet);

        mPDFWriter = new PDFWriter();

        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
        addText(defaultLeftMargin, currentTopPositionFromBottom, titleFontSize, submittedQuestionnaire.getTitle());
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);

        currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, titleFontSize, defaultInterlinearDistance);
        if (username != null && !username.equals("")) {
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_user_colon, username));
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
        }
        addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.user_id_colon) + submittedQuestionnaire.getUserId());

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nomade_logo_small);
        mPDFWriter.addImageKeepRatio(420, 735, 65, 65, bitmap);

        if (submittedQuestionnaire.getEditDate().equals("null") || submittedQuestionnaire.getDate().equals(submittedQuestionnaire.getEditDate())) {
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
        } else {
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_last_edit_colon) + submittedQuestionnaire.getEditDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
        }

        String descriptionString = submittedQuestionnaire.getDescription();

        if (descriptionString.length() > 0) {
            String[] splitDescription = descriptionString.split(System.lineSeparator());
            for (String s : splitDescription) {
                currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
                addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, s);
            }
        }

        SubmittedQuestionnaireQuestion submittedQuestionnaireQuestion;
        SubmittedQuestionnaireAnswer submittedQuestionnaireAnswer;

        for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++) {

            submittedQuestionnaireQuestion = submittedQuestionnaire.getQuestionsList().get(i);

            String questionString = parseAlternateQuestionNumbering(i + 1, submittedQuestionnaireQuestion.getQuestion());
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

                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
                    mPDFWriter.addImageKeepRatio(answerLeftMargin, currentTopPositionFromBottom + 1, 5, 5, bulletBitmap);
                    addText(answerLeftMargin + 10, currentTopPositionFromBottom, defaultFontSize, answerValue);
                }

            }
            else if (submittedQuestionnaireQuestion.getBulletType() == Question.BODY_CHART) {
                if (bodyChartImageView != null) {
                    Bitmap bodyChartBitmap = getBitmapFromView(bodyChartImageView);
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, 200);
                    if (currentTopPositionFromBottom == 772) {
                        currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, 200);
                    }
                    mPDFWriter.addImageKeepRatio(answerLeftMargin, currentTopPositionFromBottom, 200, 200, bodyChartBitmap);
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
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

            outputStream.write(pdfContent.getBytes(StandardCharsets.ISO_8859_1));

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_submitted_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (submittedQuestionnaire != null) {
            MenuItem menuDownloadPdf = menu.findItem(R.id.action_create_pdf);
            menuDownloadPdf.setVisible(true);

            if (questionnaire != null) {

                if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_EDIT, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_EDIT_USER, false) && getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1) == submittedQuestionnaire.getUserId()) {
                    MenuItem menuItemEdit = menu.findItem(R.id.action_edit);
                    menuItemEdit.setVisible(questionnaire.getEditable() > 0 && submittedQuestionnaire.getNextSubmissionId() == 0);
                }

                MenuItem menuItemCopy = menu.findItem(R.id.action_copy);
                menuItemCopy.setVisible(questionnaire.getEditable() > 1);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_create_pdf) {
            pDialog.setMessage(getString(R.string.creating_pdf_version_ellipsis));
            showDialog();
            createPdfIntent();
        } else if (itemId == R.id.action_edit) {
            pDialog.setMessage(getString(R.string.checking_saved_edits));
            showDialog();

            int remainingQuestionnaires = 0;
            int remainingId = 0;
            boolean editInProgress = false;

            while (getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).contains("remaining_questionnaire_" + (remainingQuestionnaires + 1))) {

                String stringRemainingQuestionnaire = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).getString("remaining_questionnaire_" + (remainingQuestionnaires + 1), "");
                if (stringRemainingQuestionnaire != null && !stringRemainingQuestionnaire.equals("")) {
                    try {
                        JSONObject jObj = new JSONObject(stringRemainingQuestionnaire);
                        JSONObject jQuestionnaire = jObj.getJSONObject("questionnaire");
                        if (jQuestionnaire.has("sbm_edit_id")) {
                            int editId = jQuestionnaire.getInt("sbm_edit_id");
                            MyLog.d("DEBUGGING", "editId: " + editId);
                            MyLog.d("DEBUGGING", "sbmId: " + submittedQuestionnaire.getId());
                            if (editId == submittedQuestionnaire.getId()) {
                                editInProgress = true;
                                remainingId = remainingQuestionnaires + 1;
                            }
                        }
                    } catch (JSONException e) {
                        MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                        Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
                    }
                }

                remainingQuestionnaires++;
            }

            final int finalRemainingId = remainingId;

            hideDialog();

            if (editInProgress) {

                Bundle bundle = new Bundle();
                bundle.putInt("FINAL_REMAINING_ID", finalRemainingId);

                SimpleDialog.build()
                        .title(R.string.saved_edit)
                        .msg(R.string.already_edited)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .extra(bundle)
                        .show(this, ALREADY_EDITED_DIALOG);
            } else {
                SimpleDialog.build()
                        .title(R.string.edit)
                        .msg(R.string.do_you_want_edit)
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .show(this, EDIT_DIALOG);
            }
        } else if (itemId == R.id.action_copy) {
            SimpleDialog.build()
                    .title(R.string.copy_)
                    .msg(R.string.do_you_want_copy)
                    .pos(R.string.yes)
                    .neg(R.string.no)
                    .show(this, COPY_DIALOG);
        }
        return true;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (ALREADY_EDITED_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE) {
                pDialog.setMessage(getString(R.string.removing_saved_edit));
                showDialog();

                int finalRemainingId = extras.getInt("FINAL_REMAINING_ID");

                getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).edit().remove("remaining_questionnaire_" + finalRemainingId).apply();

                int index = finalRemainingId;

                while (getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).contains("remaining_questionnaire_" + (index + 1))) {
                    String string = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).getString("remaining_questionnaire_" + (index + 1), "");
                    getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE).edit().putString("remaining_questionnaire_" + index, string).remove("remaining_questionnaire_" + (index + 1)).apply();
                    index++;
                }

                hideDialog();

                Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
                intent.putExtra("EDIT_SUBMITTED_QUESTIONNAIRE", jsonResponseString);
                intent.putExtra("QUESTIONNAIRE_ID", String.valueOf(submittedQuestionnaire.getQnrId()));
                intent.putExtra("DOWNLOAD_JSON", Utilities.checkNetworkConnection(context));

                startActivity(intent);
                return true;
            }
        }

        if (EDIT_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE) {
                Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
                intent.putExtra("EDIT_SUBMITTED_QUESTIONNAIRE", jsonResponseString);
                intent.putExtra("QUESTIONNAIRE_ID", String.valueOf(submittedQuestionnaire.getQnrId()));
                intent.putExtra("DOWNLOAD_JSON", Utilities.checkNetworkConnection(context));
                startActivity(intent);
                return true;
            }
        }

        if (COPY_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE) {
                Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
                intent.putExtra("EDIT_SUBMITTED_QUESTIONNAIRE", jsonResponseString);
                intent.putExtra("QUESTIONNAIRE_ID", String.valueOf(submittedQuestionnaire.getQnrId()));
                intent.putExtra("COPY", true);
                intent.putExtra("DOWNLOAD_JSON", Utilities.checkNetworkConnection(context));
                startActivity(intent);
                return true;
            }
        }

        return false;
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
