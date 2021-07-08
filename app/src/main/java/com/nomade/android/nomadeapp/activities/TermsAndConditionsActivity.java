package com.nomade.android.nomadeapp.activities;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nomade.android.nomadeapp.R;

/**
 * TermsAndConditionsActivity
 *
 * Activity to display the terms and conditions
 */
public class TermsAndConditionsActivity extends AppCompatActivity {

    private static final String TAG = "TermsAndConditionsActiv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        TextView termsAndConditionsTextView = findViewById(R.id.terms_and_conditions_text_view);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            termsAndConditionsTextView.setText(Html.fromHtml(getString(R.string.terms_text), Html.FROM_HTML_MODE_LEGACY));
        } else {
            termsAndConditionsTextView.setText(Html.fromHtml(getString(R.string.terms_text)));
        }
    }
}
