package com.nomade.android.nomadeapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.setups.Setup;

import java.util.ArrayList;

/**
 * SetupListAdapter
 *
 * Adapter to populate the SetupListActivity
 */
public class SetupListAdapter extends ArrayAdapter<Setup> {

    private static final String TAG = "SetupListAdapter";

    private boolean showPopupMenuButton;
    private SetupListAdapterCallback callback;

    // View lookup cache
    static class ViewHolder {
        ImageView imageViewLock;
        TextView textViewName;
        TextView textViewId;
        TextView textViewObsolete;
        TextView textViewDetails;
        ImageButton imageButtonPopup;
    }

    public SetupListAdapter(Context context, ArrayList<Setup> data, boolean pickSetup) {
        super(context, R.layout.list_item_setup, data);
        this.showPopupMenuButton = !pickSetup;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Setup setup = getItem(position);

        ViewHolder viewHolder;

        if (convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_setup, parent, false);
            viewHolder.imageViewLock = convertView.findViewById(R.id.lock_image_view);
            viewHolder.textViewName = convertView.findViewById(R.id.setup_name_text_view);
            viewHolder.textViewId = convertView.findViewById(R.id.setup_id_text_view);
            viewHolder.textViewObsolete = convertView.findViewById(R.id.setup_obsolete_text_view);
            viewHolder.textViewDetails = convertView.findViewById(R.id.setup_details_text_view);
            viewHolder.imageButtonPopup = convertView.findViewById(R.id.popup_image_button);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (setup.isLocked()) {
            viewHolder.imageViewLock.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.imageViewLock.setVisibility(View.GONE);
        }

        if (setup.isObsolete()) {
            viewHolder.textViewObsolete.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.textViewObsolete.setVisibility(View.GONE);
        }

        viewHolder.textViewName.setText(setup.getName());
        viewHolder.textViewId.setText(String.format("ID: %s", setup.getId()));
        viewHolder.textViewDetails.setText(String.format("Hardware Identifier: %s\nVersion: %s", setup.getHardwareIdentifier(), setup.getVersion()));
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


        return convertView;
    }

    public void setCallback(SetupListAdapterCallback callback) {
        this.callback = callback;
    }

    public interface SetupListAdapterCallback {
        public void dotsPressed(int position, View v);
    }
}
