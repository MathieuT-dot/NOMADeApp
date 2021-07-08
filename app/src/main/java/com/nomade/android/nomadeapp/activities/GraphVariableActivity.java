package com.nomade.android.nomadeapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.setups.Setup;

import java.util.ArrayList;
import java.util.Random;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;

/**
 * GraphVariableActivity
 *
 * In this activity the user can add, remove or adjust variables that are displayed in the graphs
 * in {@link com.nomade.android.nomadeapp.activities.GraphActivity}.
 * An overview of the configured variables can be found in
 * {@link com.nomade.android.nomadeapp.activities.GraphConfigActivity}.
 */
public class GraphVariableActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener{

    private static final String TAG = GraphVariableActivity.class.getSimpleName();
    private final Context context = this;

    private Setup streamSetup;
    private int graphIndex = 0;
    private int graphInstrumentIndex = 0;
    private int variableIndex = 0;

    private ArrayList<String> instrumentSpinnerArrayList = new ArrayList<>();
    private ArrayList<String> variableSpinnerArrayList = new ArrayList<>();

    private EditText nameEditText;
    private View colorView;
    private Spinner instrumentSpinner;
    private Spinner variableSpinner;

    private String name = "";
    private int currentColor;
    private String COLOR_DIALOG = "dialogTagColor";

    private boolean initComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_variable);

        if (savedInstanceState != null && savedInstanceState.containsKey("COLOR_INT")){
            currentColor = savedInstanceState.getInt("COLOR_INT");
        }
        else {
            currentColor = Constants.GRAPH_COLORS[new Random().nextInt(Constants.GRAPH_COLORS.length)];
        }

        instrumentSpinner = findViewById(R.id.instrument_spinner);
        variableSpinner = findViewById(R.id.variable_spinner);

        extractDataFromIntent();

        nameEditText = findViewById(R.id.name_edit_text);
        nameEditText.setText(name);

        colorView = findViewById(R.id.color_view);
        colorView.setOnClickListener(onColorViewClickListener);
        colorView.setBackgroundColor(currentColor);

        findViewById(R.id.cancel_button).setOnClickListener(onButtonClickListener);
        findViewById(R.id.remove_button).setOnClickListener(onButtonClickListener);
        findViewById(R.id.add_button).setOnClickListener(onButtonClickListener);
        findViewById(R.id.save_button).setOnClickListener(onButtonClickListener);


        if (streamSetup.getInstrumentArrayList().size() > 0){
            populateInstrumentSpinner();
        }
        else {
            MyLog.e(TAG, "graphInstrumentArrayList is empty!");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("COLOR_INT", currentColor);

        super.onSaveInstanceState(outState);
    }

    /**
     * Extracts the required data from the intent.
     */
    private void extractDataFromIntent(){
        if (getIntent().hasExtra("GRAPH_INDEX")){
            graphIndex = getIntent().getIntExtra("GRAPH_INDEX", 0);
        }

        if (getIntent().hasExtra("STREAM_SETUP")){
            streamSetup = getIntent().getParcelableExtra("STREAM_SETUP");
        }

        if (getIntent().hasExtra("GRAPH_INSTRUMENT_INDEX") && getIntent().hasExtra("VARIABLE_INDEX")){
            graphInstrumentIndex = getIntent().getIntExtra("GRAPH_INSTRUMENT_INDEX", 0);
            instrumentSpinner.setEnabled(false);
            variableIndex = getIntent().getIntExtra("VARIABLE_INDEX", 0);
            variableSpinner.setEnabled(false);

            name = streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).getChartName();
            currentColor = streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).getChartColor();

            findViewById(R.id.add_button).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.remove_button).setVisibility(View.GONE);
            findViewById(R.id.save_button).setVisibility(View.GONE);
        }

        MyLog.d(TAG, "graphIndex: " + graphIndex);
        MyLog.d(TAG, "graphInstrumentIndex: " + graphInstrumentIndex);
        MyLog.d(TAG, "variableIndex: " + variableIndex);
    }

    /**
     * Populates the spinner with the instruments.
     */
    private void populateInstrumentSpinner(){
        instrumentSpinnerArrayList.clear();
        for (int i = 0; i < streamSetup.getInstrumentArrayList().size(); i++){
            instrumentSpinnerArrayList.add(streamSetup.getInstrumentArrayList().get(i).getName() + " (" + streamSetup.getInstrumentArrayList().get(i).getId() + ")");
        }
        instrumentSpinner.setAdapter(new ArrayAdapter<String>(context, R.layout.spinner_item_padded, instrumentSpinnerArrayList){
            // Disable item if there are no variables for this instrument
            @Override
            public boolean isEnabled(int position) {
                return streamSetup.getInstrumentArrayList().get(position).getVariableArrayList().size() > 0;
            }

            // Change color of the items based on whether they are enabled
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (streamSetup.getInstrumentArrayList().get(position).getVariableArrayList().size() > 0){
                    textView.setTextColor(getResources().getColor(R.color.default_text));
                }
                else {
                    textView.setTextColor(getResources().getColor(R.color.disabled_text));
                }
                return view;
            }
        });
        instrumentSpinner.setSelection(graphInstrumentIndex);
        instrumentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                graphInstrumentIndex = position;
                if (initComplete){
                    variableIndex = 0;
                }
                else {
                    initComplete = true;
                }
                populateVariableSpinner(graphInstrumentIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        populateVariableSpinner(graphInstrumentIndex);
    }

    /**
     * Populates the spinner with the variables
     * @param index determines the currently selected option
     */
    private void populateVariableSpinner(int index){
        variableSpinnerArrayList.clear();
        for (int j = 0; j < streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().size(); j++){
            if (streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().get(j).getOversampledOffset() != -1) {
                variableSpinnerArrayList.add(streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().get(j).getName() + " (" + streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().get(j).getUnit() + ")");
            }
        }
        variableSpinner.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_item_padded, variableSpinnerArrayList));
        variableSpinner.setSelection(variableIndex);
        variableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                variableIndex = position;
                nameEditText.setText(String.format("%s (%s): %s", streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getName(), streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getId(), streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().get(variableIndex).getName()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        nameEditText.setText(String.format("%s (%s): %s", streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getName(), streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getId(), streamSetup.getInstrumentArrayList().get(index).getVariableArrayList().get(variableIndex).getName()));
    }

    /**
     * Color click listener.
     */
    private View.OnClickListener onColorViewClickListener = v -> SimpleColorDialog.build()
            .title("Pick a color")
            .colorPreset(currentColor)
            .allowCustom(true)
            .show(GraphVariableActivity.this, COLOR_DIALOG);

    /**
     * Button click listeners.
     */
    private View.OnClickListener onButtonClickListener = v -> {

        Intent returnIntent = new Intent();

        int id = v.getId();
        if (id == R.id.cancel_button) {
            setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
            finish();
        } else if (id == R.id.remove_button) {
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartIndex(0);
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartName("");
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartColor(0);
            returnIntent.putExtra("STREAM_SETUP", streamSetup);
            setResult(AppCompatActivity.RESULT_OK, returnIntent);
            finish();
        } else if (id == R.id.add_button) {
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartIndex(graphIndex);
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartName(nameEditText.getText().toString());
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartColor(currentColor);
            returnIntent.putExtra("STREAM_SETUP", streamSetup);
            setResult(AppCompatActivity.RESULT_OK, returnIntent);
            finish();
        } else if (id == R.id.save_button) {
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartIndex(graphIndex);
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartName(nameEditText.getText().toString());
            streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).setChartColor(currentColor);
            returnIntent.putExtra("STREAM_SETUP", streamSetup);
            setResult(AppCompatActivity.RESULT_OK, returnIntent);
            finish();
        }
    };

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_POSITIVE && COLOR_DIALOG.equals(dialogTag)){
            // Set currentColor
            currentColor = extras.getInt(SimpleColorDialog.COLOR);
            colorView.setBackgroundColor(currentColor);
            return true;
        }
        return false;
    }
}
