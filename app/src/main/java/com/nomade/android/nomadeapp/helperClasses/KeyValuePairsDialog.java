package com.nomade.android.nomadeapp.helperClasses;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;

import java.util.ArrayList;

import eltos.simpledialogfragment.CustomViewDialog;

/**
 * KeyValuePairsDialog
 *
 * Custom dialog to easily display a list of key-value pairs.
 */
public class KeyValuePairsDialog extends CustomViewDialog<KeyValuePairsDialog> {

    public static final String TAG = "KeyValuePairsDialog";

    public static KeyValuePairsDialog build(){
        return new KeyValuePairsDialog();
    }

    public KeyValuePairsDialog keys(Object... args){
        for(Object o : args){
            mKeysArrayList.add(String.valueOf(o));
        }
        return this;
    }

    public KeyValuePairsDialog values(Object... args){
        for(Object o : args){
            mValuesArrayList.add(String.valueOf(o));
        }
        return this;
    }

    private ArrayList<String> mKeysArrayList = new ArrayList<>();
    private ArrayList<String> mValuesArrayList = new ArrayList<>();

    @Override
    protected View onCreateContentView(Bundle savedInstanceState) {

        View view = inflate(R.layout.linear_layout_key_value_pairs);
        TextView textView = view.findViewById(R.id.text_view);

        StringBuilder stringBuilder = new StringBuilder();

        int length = Math.min(mKeysArrayList.size(), mValuesArrayList.size());

        for (int i = 0; i < length; i++){
            stringBuilder.append(mKeysArrayList.get(i));
            stringBuilder.append("\n\t\t\t");
            stringBuilder.append(mValuesArrayList.get(i));
            stringBuilder.append("\n");
        }

        textView.setText(stringBuilder);

        return view;
    }


}

