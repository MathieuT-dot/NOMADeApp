package com.nomade.android.nomadeapp.helperClasses;

/**
 * CrossReferenceAnswer
 *
 * Used the match answers between a submission and the corresponding questionnaire.
 */
public class CrossReferenceAnswer {
    private int id;
    private int questionId;
    private int answerId;
    private String value;

    public CrossReferenceAnswer(int id, int questionId, int answerId, String value) {
        this.id = id;
        this.questionId = questionId;
        this.answerId = answerId;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getAnswerId() {
        return answerId;
    }

    public void setAnswerId(int answerId) {
        this.answerId = answerId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
