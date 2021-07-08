package com.nomade.android.nomadeapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;

import java.util.ArrayList;

/**
 * SubmittedQuestionnairesAdapter
 *
 * Adapter to populate the SubmittedQuestionnairesActivity
 */
public class SubmissionsAdapter extends ArrayAdapter<SubmittedQuestionnaire> {

    private static final String TAG = "SubmissionsAdapter";
    private Context context;

    private boolean developer;

    // View lookup cache
    private static class ViewHolder {
        TextView textViewTitle;
        TextView textViewUsername;
        TextView textViewDetails;
    }

    public SubmissionsAdapter(Context context, ArrayList<SubmittedQuestionnaire> data, boolean developer) {
        super(context, R.layout.list_item_submission, data);
        this.context = context;
        this.developer = developer;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SubmittedQuestionnaire submittedQuestionnaire = getItem(position);

        ViewHolder viewHolder;

        if (convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_submission, parent, false);
            viewHolder.textViewTitle = convertView.findViewById(R.id.submission_title_text_view);
            viewHolder.textViewUsername = convertView.findViewById(R.id.submission_username_text_view);
            viewHolder.textViewDetails = convertView.findViewById(R.id.submission_details_text_view);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textViewTitle.setText(submittedQuestionnaire.getTitle());

        if (developer){
            viewHolder.textViewUsername.setText(context.getString(R.string.list_user_colon, submittedQuestionnaire.getUserName()) + " (" + submittedQuestionnaire.getUserId() + ")");
        }
        else {
            viewHolder.textViewUsername.setVisibility(View.GONE);
//            viewHolder.textViewUsername.setText(context.getString(R.string.list_user_colon, submittedQuestionnaire.getUserName()));
        }

//        viewHolder.textViewDetails.setText(context.getString(R.string.list_submission_details_colon, submittedQuestionnaire.getQnrId(), submittedQuestionnaire.getVersion(), submittedQuestionnaire.getDate()));

        if (submittedQuestionnaire.getEditDate().equals("null") || submittedQuestionnaire.getEditDate().equals(submittedQuestionnaire.getDate())){
            viewHolder.textViewDetails.setText(context.getString(R.string.list_submission_date_colon) + "\n" + submittedQuestionnaire.getDate());
        }
        else {
            viewHolder.textViewDetails.setText(context.getString(R.string.list_submission_date_colon) + "\n" + submittedQuestionnaire.getDate() + "\n" + context.getString(R.string.list_last_edit_colon) + "\n" + submittedQuestionnaire.getEditDate());
        }

        return convertView;
    }
}