package com.nomade.android.nomadeapp.helperClasses;

import android.content.Context;
import android.widget.Toast;

import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Answer;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Question;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireQuestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

/**
 * NewJsonLib
 *
 * Small library the contains commonly used methods that differ from
 * {@link com.kuleuven.android.kuleuvenlibrary.JsonLib}.
 */
public class NewJsonLib {

    private static final String TAG = "NewJsonLib";

    /**
     * Displays a toast.
     *
     * @param context to show the toast
     * @param text to be shown in the toast
     */
    private static void displayToast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Parses a JSON containing a questionnaire.
     *
     * @param context to display the toast
     * @param response JSON containing a questionnaire
     * @return the parsed questionnaire
     */
    public static Questionnaire parseJsonQuestionnaire(Context context, String response) {

        try{
            JSONObject jObj = new JSONObject(response);

            JSONObject jQuestionnaire = jObj.getJSONObject("data");

            return parseJsonObjectQuestionnaire(context, jQuestionnaire);

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
            displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
        }

        return null;
    }

    public static Questionnaire parseJsonObjectQuestionnaire(Context context, JSONObject jQuestionnaire){

        try{
            int questionnaireId = jQuestionnaire.getInt("id");
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

//            String questionnaireCreatedAt = jQuestionnaire.getString("created_at");
//            String questionnaireUpdatedAt = jQuestionnaire.getString("updated_at");
//            String questionnaireDeletedAt = jQuestionnaire.getString("deleted_at");
//            String questionnaireCreatedBy = jQuestionnaire.getString("created_by");
//            String questionnaireUpdatedBy = jQuestionnaire.getString("updated_by");
//            String questionnaireDeletedBy = jQuestionnaire.getString("deleted_by");

            JSONArray jQuestionsArray= jQuestionnaire.getJSONArray("questions");

            ArrayList<Question> questionList = new ArrayList<>();

            for (int i = 0; i < jQuestionsArray.length(); i++){

//                MyLog.d(TAG, "parseJsonObjectQuestionnaire: Question " + (i+1));

                JSONObject jCurrentQuestion = jQuestionsArray.getJSONObject(i);

                int questionId = jCurrentQuestion.getInt("id");
//                int questionSequence = jCurrentQuestion.getInt("sequence");

                String questionEn = jCurrentQuestion.getString("name_en");
                String questionNl = jCurrentQuestion.getString("name_nl");
                String questionFr = jCurrentQuestion.getString("name_fr");
                String questionTooltipEn = jCurrentQuestion.getString("description_en");
                String questionTooltipNl = jCurrentQuestion.getString("description_nl");
                String questionTooltipFr = jCurrentQuestion.getString("description_fr");

                String question;
                String questionTooltip;

                switch (Locale.getDefault().getLanguage()){
                    case "nl":
                        question = questionNl;
                        questionTooltip = questionTooltipNl;
                        break;

                    case "fr":
                        question = questionFr;
                        questionTooltip = questionTooltipFr;
                        break;

                    default:
                        question = questionEn;
                        questionTooltip = questionTooltipEn;
                }

                int questionTypeId = jCurrentQuestion.getInt("question_type_id");             // Was bullet type

//                String questionCreatedAt = jCurrentQuestion.getString("created_at");
//                String questionUpdatedAt = jCurrentQuestion.getString("updated_at");
//                String questionDeletedAt = jCurrentQuestion.getString("deleted_at");
//                String questionCreatedBy = jCurrentQuestion.getString("created_by");
//                String questionUpdatedBy = jCurrentQuestion.getString("updated_by");
//                String questionDeletedBy = jCurrentQuestion.getString("deleted_by");

                JSONArray jAnswersArray = jCurrentQuestion.getJSONArray("answers");

                ArrayList<Answer> answerList = new ArrayList<>();

                for (int j = 0; j < jAnswersArray.length(); j++){

//                    MyLog.d(TAG, "parseJsonObjectQuestionnaire: Answer " + (j+1));

                    JSONObject jCurrentAnswer = jAnswersArray.getJSONObject(j);

                    int answerId = jCurrentAnswer.getInt("id");
//                    int answerSequence = jCurrentAnswer.getInt("sequence");
                    int answerTypeId = jCurrentAnswer.getInt("answer_type_id");

                    String answerEn = jCurrentAnswer.getString("name_en");
                    String answerNl = jCurrentAnswer.getString("name_nl");
                    String answerFr = jCurrentAnswer.getString("name_fr");
                    String answerTooltipEn = jCurrentAnswer.getString("description_en");
                    String answerTooltipNl = jCurrentAnswer.getString("description_nl");
                    String answerTooltipFr = jCurrentAnswer.getString("description_fr");

                    String answer;
                    String answerTooltip;

                    switch (Locale.getDefault().getLanguage()){
                        case "nl":
                            answer = answerNl;
                            answerTooltip = answerTooltipNl;
                            break;

                        case "fr":
                            answer = answerFr;
                            answerTooltip = answerTooltipFr;
                            break;

                        default:
                            answer = answerEn;
                            answerTooltip = answerTooltipEn;
                    }

                    if (answerTypeId == 10 || answerTypeId == 11 || answerTypeId == 20 || answerTypeId == 25) {
                        JSONObject jOption;
                        try {
                            jOption = jCurrentAnswer.getJSONObject("option");
                        }
                        catch (Exception e){
                            jOption = null;
                        }

                        switch (answerTypeId){
                            case 10:
                                int optionsMinInt;
                                try {
                                    assert jOption != null;
                                    optionsMinInt = jOption.getInt("min");
                                }
                                catch (Exception | Error e) {
                                    optionsMinInt = Integer.MIN_VALUE;
                                }

                                int optionsMaxInt;
                                try {
                                    assert jOption != null;
                                    optionsMaxInt= jOption.getInt("max");
                                }
                                catch (Exception | Error e){
                                    optionsMaxInt = Integer.MAX_VALUE;
                                }

                                answerList.add(new Answer(answerId, answerTypeId, answer, answerTooltip, optionsMinInt, optionsMaxInt));
                                break;

                            case 11:
                                double optionsMinDouble;
                                try {
                                    assert jOption != null;
                                    optionsMinDouble= jOption.getDouble("min");
                                }
                                catch (Exception | Error e){
                                    optionsMinDouble = Double.NEGATIVE_INFINITY;
                                }

                                double optionsMaxDouble;
                                try {
                                    assert jOption != null;
                                    optionsMaxDouble= jOption.getDouble("max");
                                }
                                catch (Exception | Error e){
                                    optionsMaxDouble = Double.POSITIVE_INFINITY;
                                }

                                answerList.add(new Answer(answerId, answerTypeId, answer, answerTooltip, optionsMinDouble, optionsMaxDouble));
                                break;

                            case 20:
                                try {
                                    assert jOption != null;
                                    optionsMinInt = jOption.getInt("min");
                                    optionsMaxInt= jOption.getInt("max");
                                }
                                catch (Exception | Error e){
                                    optionsMinInt = 0;
                                    optionsMaxInt= 10;
                                }

                                answerList.add(new Answer(answerId, answerTypeId, answer, answerTooltip, optionsMinInt, optionsMaxInt));
                                break;

                            case 25:
                                try {
                                    assert jOption != null;
                                    optionsMinInt = jOption.getInt("min");
                                    optionsMaxInt= jOption.getInt("max");
                                }
                                catch (Exception | Error e){
                                    optionsMinInt = 0;
                                    optionsMaxInt= 1;
                                }

                                answerList.add(new Answer(answerId, answerTypeId, answer, answerTooltip, optionsMinInt, optionsMaxInt));
                                break;
                        }
                    }
                    else {
                        answerList.add(new Answer(answerId, answerTypeId, answer, answerTooltip));
                    }
                }

//                MyLog.d(TAG, "parseJsonObjectQuestionnaire: " + answerList.size() + " answers added");

                /*
                  If there is a condition, get the condition and use constructor with condition
                  Otherwise just use constructor without condition
                 */
                if (jCurrentQuestion.has("conditions")){
//                    MyLog.d(TAG, "parseJsonObjectQuestionnaire: question has conditions");
                    JSONArray jConditionArray = jCurrentQuestion.getJSONArray("conditions");
                    if (jConditionArray.length() > 0) {
//                        MyLog.d(TAG, "parseJsonObjectQuestionnaire: conditional is 1 and array is bigger than 1");
                        JSONObject jCondition = jConditionArray.getJSONObject(0);
                        int conditionQuestionId = jCondition.getInt("condition_question_id");
                        int conditionAnswerId = jCondition.getInt("condition_answer_id");

                        questionList.add(new Question(questionId, question, questionTooltip, questionTypeId, 1, answerList, conditionQuestionId, conditionAnswerId));
                    }
                    else {
//                        MyLog.d(TAG, "parseJsonObjectQuestionnaire: conditional is 0 or array is 0");
                        questionList.add(new Question(questionId, question,  questionTooltip, questionTypeId, 0, answerList));
                    }
                }
                else {
//                    MyLog.d(TAG, "parseJsonObjectQuestionnaire: questions has no conditions");
                    questionList.add(new Question(questionId, question,  questionTooltip, questionTypeId, 0, answerList));
                }
            }

//            MyLog.d(TAG, "parseJsonObjectQuestionnaire: " + questionList.size() + " questions added");

            return new Questionnaire(questionnaireId, groupId, version, title, description, draft, editable, questionList);

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
            displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
        }

        return null;
    }

    /**
     * Parses a JSON containing a submitted questionnaire.
     *
     * @param context to display the toast
     * @param response JSON containing a submitted questionnaire
     * @return the parsed submitted questionnaire
     */
    public static SubmittedQuestionnaire parseJsonSubmittedQuestionnaire(Context context, String response, Questionnaire questionnaire) {

        try {

            JSONObject jObj = new JSONObject(response);

            JSONObject jSubmission = jObj.getJSONObject("data");

            // Submission header

            int id = jSubmission.getInt("id");
            int questionnaireId = jSubmission.getInt("questionnaire_id");
            int userId = jSubmission.getInt("user_id");

            int previousSubmissionId = 0;
            if (!jSubmission.isNull("prev_submission_id")){
                previousSubmissionId = jSubmission.getInt("prev_submission_id");
            }

            int nextSubmissionId = 0;
            if (!jSubmission.isNull("next_submission_id")){
                nextSubmissionId = jSubmission.getInt("next_submission_id");
            }

            SimpleDateFormat sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

            String createdAt = jSubmission.getString("created_at");
            try {
                createdAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(createdAt));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String updatedAt = jSubmission.getString("updated_at");
            try {
                updatedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(updatedAt));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String startedAt = jSubmission.getString("started_at");
            try {
                startedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(startedAt));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String finishedAt = jSubmission.getString("finished_at");
            try {
                finishedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(finishedAt));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String deletedAt = jSubmission.getString("deleted_at");
            String createdBy = jSubmission.getString("created_by");
            String updatedBy = jSubmission.getString("updated_by");
            String deletedBy = jSubmission.getString("deleted_by");

            // Submission answers

            JSONArray jSubmissionAnswersArray = jSubmission.getJSONArray("answers");

            ArrayList<CrossReferenceAnswer> crossReferenceAnswerArrayList = new ArrayList<>();

            for (int i = 0; i < jSubmissionAnswersArray.length(); i++){

                JSONObject jSubmissionAnswer = jSubmissionAnswersArray.getJSONObject(i);

                int crossReferenceId = jSubmissionAnswer.getInt("id");
                int crossReferenceQuestionId = jSubmissionAnswer.getInt("question_id");
                int crossReferenceAnswerId = jSubmissionAnswer.getInt("answer_id");
                String crossReferenceValue = jSubmissionAnswer.getString("value");

                crossReferenceAnswerArrayList.add(new CrossReferenceAnswer(crossReferenceId, crossReferenceQuestionId, crossReferenceAnswerId, crossReferenceValue));
            }

            // Combining the questionnaire and the submission answers

            ArrayList<SubmittedQuestionnaireQuestion> submittedQuestionnaireQuestionArrayList = new ArrayList<>();

            ArrayList<Question> questionArrayList = questionnaire.getQuestionsList();

            for (int i = 0; i < questionArrayList.size(); i++){

                Question currentQuestion = questionArrayList.get(i);

                ArrayList<SubmittedQuestionnaireAnswer> submittedQuestionnaireAnswerArrayList = new ArrayList<>();

                ArrayList<Answer> answerArrayList = currentQuestion.getAnswersList();

                for (int j = 0; j < answerArrayList.size(); j++){

                    Answer currentAnswer = answerArrayList.get(j);

                    for (int k = 0; k < crossReferenceAnswerArrayList.size(); k++){

                        CrossReferenceAnswer currentCrossReferenceAnswer = crossReferenceAnswerArrayList.get(k);

                        if (currentQuestion.getId() == currentCrossReferenceAnswer.getQuestionId() && currentAnswer.getId() == currentCrossReferenceAnswer.getAnswerId()){
                            submittedQuestionnaireAnswerArrayList.add(new SubmittedQuestionnaireAnswer(currentAnswer.getId(), currentAnswer.getTypeId(), currentCrossReferenceAnswer.getValue(), currentAnswer.getPrefix()));
                            break;
                        }
                    }
                }

                if (submittedQuestionnaireAnswerArrayList.size() > 0){
                    submittedQuestionnaireQuestionArrayList.add(new SubmittedQuestionnaireQuestion(currentQuestion.getId(), currentQuestion.getQuestion(), currentQuestion.getBulletType(), currentQuestion.getConditional(), submittedQuestionnaireAnswerArrayList));
                }
            }

            return new SubmittedQuestionnaire(id, questionnaireId, questionnaire.getGroupId(), questionnaire.getVersion(), questionnaire.getTitle(), questionnaire.getDescription(), userId, createdAt, updatedAt, startedAt, finishedAt, previousSubmissionId, nextSubmissionId, submittedQuestionnaireQuestionArrayList);

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + " | " + e.getMessage());
            displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.toString() + " | " + e.getMessage());
        }

        return null;
    }
}
