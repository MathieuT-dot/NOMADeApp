package com.nomade.android.nomadeapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.activities.MainActivity;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.Utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.CustomListDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

import static android.content.Context.MODE_PRIVATE;

/**
 * SettingsFragment
 *
 * Fragment to display the settings of the app
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SimpleDialog.OnDialogResultListener {

    private static final String EXPORT_LOG_DIALOG = "dialogTagExportLog";
    private static final String CLEAR_LOG_DIALOG = "dialogTagClearLog";
    private static final int CREATE_LOG = 1;
    private String selectedLogLabel = "";
    private SharedPreferences logSharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preference_screen");

        Preference preferenceAccessibilitySettings = findPreference("accessibility_settings");

        Preference preferenceExportLogFiles = findPreference("export_log_files");
        Preference preferenceClearLogFiles = findPreference("clear_log_files");

        if (preferenceScreen != null && preferenceAccessibilitySettings != null && preferenceExportLogFiles != null && preferenceClearLogFiles != null) {

            preferenceAccessibilitySettings.setOnPreferenceClickListener(v ->
            {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                return true;
            });

            CheckBoxPreference preferenceEnableLogging = (CheckBoxPreference) findPreference(Constants.SETTING_ENABLE_LOGGING);
            CheckBoxPreference preferenceQnrDraftView = (CheckBoxPreference) findPreference(Constants.SETTING_QNR_DRAFT_VIEW);
            CheckBoxPreference preferenceQnrDraftSubmit = (CheckBoxPreference) findPreference(Constants.SETTING_QNR_DRAFT_SUBMIT);
            CheckBoxPreference preferenceAutomaticUsbComm = (CheckBoxPreference) findPreference(Constants.SETTING_AUTOMATIC_USB_COMMUNICATION);
            CheckBoxPreference preferenceOnlyEnableRelevantButtons = (CheckBoxPreference) findPreference(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS);
            CheckBoxPreference preferenceHideUnchangeableParameters = (CheckBoxPreference) findPreference(Constants.SETTING_HIDE_UNCHANGEABLE_PARAMETERS);

            SharedPreferences permissionsSharedPreferences;

            Activity activity = getActivity();

            if (activity != null) {

                preferenceExportLogFiles.setOnPreferenceClickListener(v -> {
                    logSharedPreferences = activity.getSharedPreferences(Constants.LOG_DATA, MODE_PRIVATE);
                    Map<String, ?> allEntries = logSharedPreferences.getAll();
                    ArrayList<String> keysArrayList = new ArrayList<>();
                    long totalSize = 0;
                    long currentSize;
                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        currentSize = entry.getValue().toString().length();
                        totalSize += currentSize;
                        keysArrayList.add(entry.getKey() + " (" + humanReadableByteCount(currentSize) + ")");
                    }

                    Collections.sort(keysArrayList, String::compareToIgnoreCase);
                    Collections.reverse(keysArrayList);

                    String[] keys = new String[allEntries.size()];
                    for (int i = 0; i < keysArrayList.size(); i++) {
                        keys[i] = keysArrayList.get(i);
                    }

                    SimpleListDialog.build()
                            .title(getString(R.string.select_file_export, humanReadableByteCount(totalSize)))
                            .items(keys)
                            .choiceMode(CustomListDialog.SINGLE_CHOICE)
                            .choiceMin(1)
                            .pos(R.string.ok)
                            .neut(R.string.cancel)
                            .cancelable(false)
                            .show(this, EXPORT_LOG_DIALOG);

                    return true;
                });

                preferenceClearLogFiles.setOnPreferenceClickListener(v -> {
                    SimpleDialog.build()
                            .title(R.string.clear_log_files_title)
                            .msg(R.string.clear_log_files_question)
                            .pos(R.string.yes)
                            .neg(R.string.no)
                            .show(this, CLEAR_LOG_DIALOG);
                    return true;
                });

                permissionsSharedPreferences = getActivity().getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE);

                if (preferenceEnableLogging != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceEnableLogging);
                }

                if ((!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceExportLogFiles);
                    preferenceScreen.removePreference(preferenceClearLogFiles);
                }

                if (preferenceQnrDraftView != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_QUESTIONNAIRE_DRAFT_SHOW, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceQnrDraftView);
                }

                if (preferenceQnrDraftSubmit != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_QUESTIONNAIRE_DRAFT_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceQnrDraftSubmit);
                }

                if (preferenceAutomaticUsbComm != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceAutomaticUsbComm);
                }

                if (preferenceOnlyEnableRelevantButtons != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceOnlyEnableRelevantButtons);
                }

                if (preferenceHideUnchangeableParameters != null && (!permissionsSharedPreferences.getBoolean(Constants.PERMISSION_SETUP_CREATE, false) || !MainActivity.loggedIn)) {
                    preferenceScreen.removePreference(preferenceHideUnchangeableParameters);
                }
            }
        }
    }

    /**
     * Creates the intent to let the user choose the location to save the PDF file.
     */
    private void createLogIntent() {
        String name = "log_file_" + selectedLogLabel + ".txt";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, name);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_LOG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_LOG) {
            if (resultCode == Activity.RESULT_OK) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Uri uri = data.getData();

                            OutputStream outputStream = getActivity().getContentResolver().openOutputStream(uri);

                            outputStream.write(logSharedPreferences.getString(selectedLogLabel, "").getBytes());

                            outputStream.close();

                            getActivity().runOnUiThread(() -> Utilities.displayToast(getActivity(), R.string.log_file_exported));
                        } catch (IOException e) {
                            getActivity().runOnUiThread(() -> Utilities.displayToast(getActivity(), R.string.error_log_file_export));
                        }
                    }
                }, 10);
            }
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (EXPORT_LOG_DIALOG.equals(dialogTag)) {
            if (which == BUTTON_POSITIVE) {
                selectedLogLabel = extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL).split(" ")[0].trim();
                createLogIntent();
                return true;
            }
        }

        if (CLEAR_LOG_DIALOG.equals(dialogTag)) {
            if (which == BUTTON_POSITIVE) {
                getActivity().getSharedPreferences(Constants.LOG_DATA, MODE_PRIVATE).edit().clear().commit();
                return true;
            }
        }

        return false;
    }

    private static String humanReadableByteCount(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }
}
