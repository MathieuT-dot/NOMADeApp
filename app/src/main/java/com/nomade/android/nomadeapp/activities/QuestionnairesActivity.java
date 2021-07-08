package com.nomade.android.nomadeapp.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.adapters.QuestionnairesAdapter;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.google.gson.Gson;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestionnaire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

/**
 * QuestionnairesActivity
 *
 * This activity displays a list of available questionnaires.
 */
public class QuestionnairesActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "QuestionnairesActivity";
    private final Context context = this;

    private Gson gson;
    private SharedPreferences questionnairesSharedPreferences;
    private SharedPreferences.Editor questionnairesEditor;
    private ProgressDialog pDialog;

    private TextView continueQuestionnaireTextView;
    private View continueDividerView;
    private ListView questionnairesListView;
    private TextView backgroundTextView;

    ArrayList<Questionnaire> questionnairesArrayList;
    private SubmittingQuestionnaire savedAnswers;
    private Boolean savedAnswersPresent = false;

    private String jsonResponseString = "";

    private int layoutId = 1;
    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_LIST = 4;
    private static final int LAYOUT_LIST = 5;
    private static final int LAYOUT_NO_OFFLINE_DATA = 6;
    private static final int NO_QUESTIONNAIRES = 7;

    private int sortIndex = 0;
    private boolean reverseOrder = false;
    private static final String SORT_DIALOG = "dialogTagSort";
    private static final String ORDER_DIALOG = "dialogTagOrder";

    private static final String OPEN_NEW_QUESTIONNAIRE_DIALOG = "dialogTagOpenNewQuestionnaire";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaires);

        gson = new Gson();
        questionnairesSharedPreferences = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE);
        questionnairesEditor = questionnairesSharedPreferences.edit();
        questionnairesEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        continueQuestionnaireTextView = findViewById(R.id.continue_questionnaire_text_view);
        continueQuestionnaireTextView.setOnClickListener(v -> {
            continueQuestionnaireTextView.setEnabled(false);
            if (Utilities.checkNetworkConnection(context)) {
                Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
                intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(savedAnswers.getId()));
                intent.putExtra("LOAD_SAVED_ANSWERS", true);
                intent.putExtra("DOWNLOAD_JSON", true);
                startActivity(intent);
            } else {
                if (questionnairesSharedPreferences.contains(Constants.API_QUESTIONNAIRES_ + savedAnswers.getId())) {
                    Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
                    intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(savedAnswers.getId()));
                    intent.putExtra("LOAD_SAVED_ANSWERS", true);
                    intent.putExtra("DOWNLOAD_JSON", false);
                    startActivity(intent);
                } else {
                    continueQuestionnaireTextView.setEnabled(true);
                    handleLayoutChanges(TOAST_NO_INTERNET);
                }
            }
        });

        continueDividerView = findViewById(R.id.continue_divider_view);
        questionnairesListView = findViewById(R.id.questionnaires_list_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        if (savedInstanceState != null) {
            jsonResponseString = savedInstanceState.getString("STATE_JSON_RESPONSE", "");
            parseJsonResponse(jsonResponseString);
        } else {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getAvailableQuestionnaires();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                jsonResponseString = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES, "");
                if (jsonResponseString != null && !jsonResponseString.equals("")) {
                    parseJsonResponse(jsonResponseString);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        continueQuestionnaireTextView.setEnabled(true);
        questionnairesListView.setEnabled(true);

        if (questionnairesSharedPreferences.contains("answers_json")) {
            savedAnswersPresent = true;
            MyLog.d(TAG, "Getting the saved answers from SharedPreferences");
            String answers_json = questionnairesSharedPreferences.getString("answers_json", "null");
            savedAnswers = gson.fromJson(answers_json, SubmittingQuestionnaire.class);
        } else {
            savedAnswersPresent = false;
        }

        if (layoutId == LAYOUT_LIST) {
            if (savedAnswersPresent && savedQuestionnaireAvailable()) {
                if (savedAnswers.isForUserIdChanged() && !savedAnswers.getForUserName().equals("")) {
                    continueQuestionnaireTextView.setText(String.format("%s\r\n\"%s\"%s%s", getString(R.string.continue_questionnaire), savedAnswers.getTitle(), getString(R.string._for_), savedAnswers.getForUserName()));
                }
                else {
                    continueQuestionnaireTextView.setText(String.format("%s\r\n\"%s\"", getString(R.string.continue_questionnaire), savedAnswers.getTitle()));
                }
                continueQuestionnaireTextView.setVisibility(View.VISIBLE);
                continueDividerView.setVisibility(View.VISIBLE);
            } else {
                continueQuestionnaireTextView.setVisibility(View.GONE);
                continueDividerView.setVisibility(View.GONE);
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString("STATE_JSON_RESPONSE", jsonResponseString);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Handles the layout changes of the UI.
     *
     * @param layout specifies which layout needs to be activated
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                layoutId = layout;
                questionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                layoutId = layout;
                questionnairesListView.setVisibility(View.GONE);
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
                layoutId = layout;
                backgroundTextView.setVisibility(View.GONE);
                questionnairesListView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_OFFLINE_DATA:
                layoutId = layout;
                questionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_offline_data);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case NO_QUESTIONNAIRES:
                questionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_questionnaires);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Creates and executes a request to get the available questionnaires.
     */
    private void getAvailableQuestionnaires() {
        String tag_string_req = "req_questionnaire";

        pDialog.setMessage(getString(R.string.getting_available_questionnaires_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "questionnaires/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    questionnairesEditor.putString(Constants.API_QUESTIONNAIRES, response).apply();
                    jsonResponseString = response;
                    parseJsonResponse(jsonResponseString);
                },
                e -> {
                    MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

                    handleLayoutChanges(LAYOUT_LOADING);
                    jsonResponseString = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES, "");
                    if (jsonResponseString != null && !jsonResponseString.equals("")) {
                        parseJsonResponse(jsonResponseString);
                        hideDialog();
                        Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA);
                    } else {
                        handleLayoutChanges(LAYOUT_NO_OFFLINE_DATA);
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

            switch (sortIndex) {
                case 0:
                    Collections.sort(questionnairesArrayList, new Questionnaire.IdComparator());
                    break;

                case 1:
                    Collections.sort(questionnairesArrayList, new Questionnaire.TitleComparator());
                    break;
            }

            if (reverseOrder) {
                Collections.reverse(questionnairesArrayList);
            }

            QuestionnairesAdapter adapter = new QuestionnairesAdapter(
                    context,
                    questionnairesArrayList,
                    getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)
            );

            questionnairesListView.setAdapter(adapter);
            questionnairesListView.setOnItemClickListener(((parent, view, position, id) -> {
                questionnairesListView.setEnabled(false);
                openClickedQuestionnaire(position);
            }));

            if (questionnairesArrayList.size() == 0){
                handleLayoutChanges(NO_QUESTIONNAIRES);
            }
            else {
                handleLayoutChanges(LAYOUT_LIST);
            }

            if (savedAnswersPresent && savedQuestionnaireAvailable()) {
                if (savedAnswers.isForUserIdChanged() && !savedAnswers.getForUserName().equals("")) {
                    continueQuestionnaireTextView.setText(String.format("%s\r\n\"%s\"%s%s", getString(R.string.continue_questionnaire), savedAnswers.getTitle(), getString(R.string._for_), savedAnswers.getForUserName()));
                }
                else {
                    continueQuestionnaireTextView.setText(String.format("%s\r\n\"%s\"", getString(R.string.continue_questionnaire), savedAnswers.getTitle()));
                }
                continueQuestionnaireTextView.setVisibility(View.VISIBLE);
                continueDividerView.setVisibility(View.VISIBLE);
            } else {
                continueQuestionnaireTextView.setVisibility(View.GONE);
                continueDividerView.setVisibility(View.GONE);
            }
        }
        catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        hideDialog();
    }

    /**
     * Opens the selected questionnaire in a new activity.
     *
     * @param position index of the questionnaire in the list view
     */
    private void openClickedQuestionnaire(int position) {

        Questionnaire questionnaire = questionnairesArrayList.get(position);
        final Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
        intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(questionnaire.getId()));

        if (Utilities.checkNetworkConnection(context)) {
            if (savedAnswersPresent) {

                intent.putExtra("LOAD_SAVED_ANSWERS", false);
                intent.putExtra("DOWNLOAD_JSON", true);

                Bundle bundle = new Bundle();
                bundle.putParcelable("INTENT", intent);

                SimpleDialog.build()
                        .title(R.string.open_questionnaire)
                        .msg(getString(R.string.do_you_want_new_questionnaire) + "\r\n" + getString(R.string.answers_will_be_deleted))
                        .pos(R.string.yes)
                        .neg(R.string.no)
                        .extra(bundle)
                        .show(this, OPEN_NEW_QUESTIONNAIRE_DIALOG);
            } else {
                intent.putExtra("LOAD_SAVED_ANSWERS", false);
                intent.putExtra("DOWNLOAD_JSON", true);
                startActivity(intent);
            }

        } else {
            if (questionnairesSharedPreferences.contains(Constants.API_QUESTIONNAIRES_ + questionnaire.getId())) {
                if (savedAnswersPresent) {

                    intent.putExtra("LOAD_SAVED_ANSWERS", false);
                    intent.putExtra("DOWNLOAD_JSON", false);

                    Bundle bundle = new Bundle();
                    bundle.putParcelable("INTENT", intent);

                    SimpleDialog.build()
                            .title(R.string.open_questionnaire)
                            .msg(getString(R.string.do_you_want_new_questionnaire) + "\r\n" + getString(R.string.answers_will_be_deleted))
                            .pos(R.string.yes)
                            .neg(R.string.no)
                            .extra(bundle)
                            .show(this, OPEN_NEW_QUESTIONNAIRE_DIALOG);
                } else {
                    intent.putExtra("LOAD_SAVED_ANSWERS", false);
                    intent.putExtra("DOWNLOAD_JSON", false);
                    startActivity(intent);
                }

            } else {
                questionnairesListView.setEnabled(true);
                handleLayoutChanges(TOAST_NO_INTERNET);
            }
        }
    }

    /**
     * Check if there are saved answers for a questionnaire on the device.
     * @return true if there are saved answers for a questionnaire
     *         false if there are no saved answers for a questionnaire
     */
    private boolean savedQuestionnaireAvailable() {
        for (Questionnaire questionnaire : questionnairesArrayList){
            if (questionnaire.getId() == savedAnswers.getId()){
                MyLog.d(TAG, "Match found");
                return true;
            }
        }
        MyLog.d(TAG, "No match found");
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sort_and_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_list:
                if (Utilities.checkNetworkConnection(context)) {
                    handleLayoutChanges(LAYOUT_LOADING);
                    getAvailableQuestionnaires();
                } else {
                    handleLayoutChanges(LAYOUT_LOADING);
                    String savedJsonList = questionnairesSharedPreferences.getString(Constants.API_QUESTIONNAIRES, "null");
                    if (!savedJsonList.equals("null")) {
                        parseJsonResponse(savedJsonList);
                        handleLayoutChanges(TOAST_OFFLINE_LIST);
                    } else {
                        handleLayoutChanges(LAYOUT_NO_INTERNET);
                    }
                }
                break;

            case R.id.action_sort:
                SimpleListDialog.build()
                        .title(R.string.select_how_to_sort)
                        .items(context, new int[]{R.string.questionnaire_id, R.string.title})
                        .choiceMode(CustomListDialog.SINGLE_CHOICE)
                        .choiceMin(1)
                        .choicePreset(sortIndex)
                        .cancelable(false)
                        .show(this, SORT_DIALOG);
                break;
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

                        parseJsonResponse(jsonResponseString);

                        return true;

                    case BUTTON_NEGATIVE:
                    case BUTTON_NEUTRAL:
                    case CANCELED:
                        return true;
                }
                break;

            case OPEN_NEW_QUESTIONNAIRE_DIALOG:
                switch (which){
                    case BUTTON_POSITIVE:
                        Intent intent = extras.getParcelable("INTENT");
                        startActivity(intent);
                        return true;
                    case BUTTON_NEGATIVE:
                    case BUTTON_NEUTRAL:
                    case CANCELED:
                        questionnairesListView.setEnabled(true);
                        return true;
                }
                break;
        }
        return false;
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
