package com.nomade.android.nomadeapp.activities;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.Constants;

import java.util.Locale;

/**
 * AboutActivity
 *
 * Activity to display information about app such as: App name, version, NOMADe website and changelog
 */
public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";

    private String versionName;
    private int versionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView appNameTextView = findViewById(R.id.about_app_name_text_view);
        appNameTextView.setText(getString(R.string.app_name));


        TextView urlKeyTextView = findViewById(R.id.about_link_key_text_view);
        urlKeyTextView.setText(getString(R.string.about_website_key));

        TextView urlValueTextView = findViewById(R.id.about_link_value_text_view);
        urlValueTextView.setText(Constants.NOMADE_URL);


        TextView versionKeyTextView = findViewById(R.id.about_version_key_text_view);
        versionKeyTextView.setText(getString(R.string.about_version_key));

        TextView versionValueTextView = findViewById(R.id.about_version_value_text_view);
        versionValueTextView.setText(String.format("%s (%s)", versionName, versionCode));


        TextView changelogKeyTextView = findViewById(R.id.changelog_key_text_view);
        changelogKeyTextView.setText(getString(R.string.about_changelog_key));

        TextView changelogValueTextView = findViewById(R.id.changelog_value_text_view);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            changelogValueTextView.setText(Html.fromHtml(getString(R.string.about_changelog_value), Html.FROM_HTML_MODE_LEGACY));
        } else {
            changelogValueTextView.setText(Html.fromHtml(getString(R.string.about_changelog_value)));
        }
    }
}
