package com.nomade.android.nomadeapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;

import java.util.ArrayList;

/**
 * QuestionnairesAdapter
 *
 * Adapter to populate the QuestionnairesActivity
 */
public class QuestionnairesAdapter extends ArrayAdapter<Questionnaire> {

    private static final String TAG = "QuestionnairesAdapter";
    private Context context;
    private boolean developer;

    // View lookup cache
    private static class ViewHolder {
        TextView textViewTitle;
        TextView textViewDescription;
        TextView textViewDetails;
    }

    public QuestionnairesAdapter(Context context, ArrayList<Questionnaire> data, boolean developer) {
        super(context, R.layout.list_item_questionnaire, data);
        this.context = context;
        this.developer = developer;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Questionnaire questionnaire = getItem(position);

        ViewHolder viewHolder;

        if (convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_questionnaire, parent, false);
            viewHolder.textViewTitle = convertView.findViewById(R.id.name_text_view);
            viewHolder.textViewDescription = convertView.findViewById(R.id.description_text_view);
            viewHolder.textViewDetails = convertView.findViewById(R.id.details_text_view);

            if (!developer){
                convertView.findViewById(R.id.details_linear_layout).setVisibility(View.GONE);
            }

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (questionnaire.isDraft()){
            viewHolder.textViewTitle.setText(context.getString(R.string.draft, questionnaire.getTitle()));
        }
        else {
            viewHolder.textViewTitle.setText(questionnaire.getTitle());
        }

        String description = questionnaire.getDescription();
        if (description.length() > 105){
            description = description.substring(0, 101);
            description = description + "...";
        }
        viewHolder.textViewDescription.setText(description);

        viewHolder.textViewDetails.setText(context.getString(R.string.list_child_details_colon, questionnaire.getId(), questionnaire.getVersion()));

        return convertView;
    }
}
