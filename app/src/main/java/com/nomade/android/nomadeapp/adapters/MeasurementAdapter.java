package com.nomade.android.nomadeapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.setups.Measurement;

import java.util.ArrayList;

/**
 * MeasurementAdapter
 *
 * Adapter to populate the MeasurementListActivity
 */
public class MeasurementAdapter extends ArrayAdapter<Measurement> {

    private static final String TAG = "MeasurementAdapter";

    private boolean showPopupMenuButton;
    private MeasurementAdapterCallback callback;

    // View lookup cache
    private static class ViewHolder {
        TextView textViewName;
        TextView textViewDescription;
        TextView textViewDetails;
        ImageButton imageButtonPopup;
    }

    public MeasurementAdapter(Context context, ArrayList<Measurement> data, boolean pickMeasurement){
        super(context, R.layout.list_item_measurement, data);
        this.showPopupMenuButton = !pickMeasurement;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //get the Measurement item from the position "position" from array list to put it on the TextView
        Measurement measurement = getItem(position);

        ViewHolder viewHolder;

        //check to see if the reused view is null or not, if is not null then reuse it
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_measurement, parent, false);
            viewHolder.textViewName = convertView.findViewById(R.id.name_text_view);
            viewHolder.textViewDescription = convertView.findViewById(R.id.description_text_view);
            viewHolder.textViewDetails = convertView.findViewById(R.id.details_text_view);
            viewHolder.imageButtonPopup = convertView.findViewById(R.id.popup_image_button);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textViewName.setText(measurement.getName());
        viewHolder.textViewDescription.setText(String.format("Measurement ID: %s\nSetup ID: %s", measurement.getMeasurementId(), measurement.getSetupId()));
        viewHolder.textViewDetails.setText(String.format("Start:\n%s\nEnd:\n%s", measurement.getStringStartTime(), measurement.getStringEndTime()));

        if (showPopupMenuButton) {
            viewHolder.imageButtonPopup.setOnClickListener(v -> {
                if (callback != null) {
                    callback.dotsPressed(position, v);
                }
            });
        }
        else {
            viewHolder.imageButtonPopup.setVisibility(View.GONE);
        }

        //this method must return the view corresponding to the data at the specified position.
        return convertView;
    }

    public void setCallback(MeasurementAdapterCallback callback) {
        this.callback = callback;
    }

    public interface MeasurementAdapterCallback {
        public void dotsPressed(int position, View v);
    }
}
