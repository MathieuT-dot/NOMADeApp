package com.nomade.android.nomadeapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.annotation.Nullable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.setups.Setup;

/**
 * GraphConfigActivity
 *
 * This activity shows an overview of the configuration of the graphs displayed in
 * {@link com.nomade.android.nomadeapp.activities.GraphActivity}.
 * This only gives an overview of the already configured variables per graph.
 * The adding, removing or adjusting of each variable is done in
 * {@link com.nomade.android.nomadeapp.activities.GraphVariableActivity}.
 */
public class GraphConfigActivity extends AppCompatActivity {

    private static final String TAG = GraphConfigActivity.class.getSimpleName();
    private final Context context = this;

    private LinearLayout[] linearLayouts = new LinearLayout[4];

    private Setup streamSetup;

    private static final int ADD_OR_EDIT_VARIABLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_config);

        if (savedInstanceState != null && savedInstanceState.containsKey("STREAM_SETUP")){
            streamSetup = savedInstanceState.getParcelable("STREAM_SETUP");
        }
        else {
            if (getIntent().hasExtra("STREAM_SETUP")){
                streamSetup = getIntent().getParcelableExtra("STREAM_SETUP");
            }
        }

        init();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putParcelable("STREAM_SETUP", streamSetup);

        super.onSaveInstanceState(outState);
    }

    /**
     * Initializes the needed buttons and linear layouts.
     */
    private void init(){
        findViewById(R.id.graph_1_button).setOnClickListener(onAddClickListener);
        findViewById(R.id.graph_2_button).setOnClickListener(onAddClickListener);
        findViewById(R.id.graph_3_button).setOnClickListener(onAddClickListener);
        findViewById(R.id.graph_4_button).setOnClickListener(onAddClickListener);

        linearLayouts[0] = findViewById(R.id.graph_1_container);
        linearLayouts[1] = findViewById(R.id.graph_2_container);
        linearLayouts[2] = findViewById(R.id.graph_3_container);
        linearLayouts[3] = findViewById(R.id.graph_4_container);

        findViewById(R.id.save_button).setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("STREAM_SETUP", streamSetup);
            setResult(AppCompatActivity.RESULT_OK,returnIntent);
            finish();
        });

        updateVariables();
    }

    /**
     * The OnClickListener for the buttons.
     */
    private View.OnClickListener onAddClickListener = v -> {
        int id = v.getId();
        if (id == R.id.graph_1_button) {
            addVariable(1);
        } else if (id == R.id.graph_2_button) {
            addVariable(2);
        } else if (id == R.id.graph_3_button) {
            addVariable(3);
        } else if (id == R.id.graph_4_button) {
            addVariable(4);
        }
    };

    /**
     * Opens a new activity where the user can add a new variable to the graphs.
     *
     * @param graphIndex index of the graph where the new variable needs to be added
     */
    private void addVariable(int graphIndex){
        Intent intent = new Intent(this, GraphVariableActivity.class);
        intent.putExtra("GRAPH_INDEX", graphIndex);
        intent.putExtra("STREAM_SETUP", streamSetup);
        startActivityForResult(intent, ADD_OR_EDIT_VARIABLE);
    }

    /**
     * Opens a new activity where the user can edit an existing variable.
     *
     * @param graphIndex index of the graph where the existing variable was added to
     * @param graphInstrumentIndex index of the instrument where the existing variable is part of
     * @param variableIndex index of the existing variable
     */
    private void editVariable(int graphIndex, int graphInstrumentIndex, int variableIndex){
        Intent intent = new Intent(this, GraphVariableActivity.class);
        intent.putExtra("GRAPH_INDEX", graphIndex);
        intent.putExtra("GRAPH_INSTRUMENT_INDEX", graphInstrumentIndex);
        intent.putExtra("VARIABLE_INDEX", variableIndex);
        intent.putExtra("STREAM_SETUP", streamSetup);
        startActivityForResult(intent, ADD_OR_EDIT_VARIABLE);
    }

    /**
     * Updates all the variables to reflect changes.
     */
    private void updateVariables(){

        for (LinearLayout linearLayout : linearLayouts) {
            linearLayout.removeAllViews();
        }

        String[][] units = new String[4][2];

        // iterate through all graph instruments
        for (int i = 0; i < streamSetup.getInstrumentArrayList().size(); i++){

            // iterate through all variables for a graph instrument
            for (int j = 0; j < streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().size(); j++){

                // check if the current variable is assigned to a chart
                if (streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() > 0){

                    boolean axisSet = false;

                    // check if the left axis already has a unit
                    if (units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][0] != null && !units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][0].equals("")){
                        // check if the unit of the current variable is the same as the unit of the left axis
                        if (units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][0].equals(streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getUnit())){
                            // set the left axis for the variable
                            streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).setAxisIndex(1);
                            axisSet = true;
                        }
                        else {
                            // check if the right axis already has a unit
                            if (units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][1] != null && !units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][1].equals("")){
                                // check if the unit of the current variable is the same as the unit of the right axis
                                if (units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][1].equals(streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getUnit())){
                                    // set the right axis for the variable
                                    streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).setAxisIndex(2);
                                    axisSet = true;
                                }
                                else {
                                    MyLog.e(TAG, "More than two different units for chart " + streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() + ", " + streamSetup.getInstrumentArrayList().get(i).getName() + ": " + streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getName() + " is not visualised");
                                }
                            }
                            else {
                                // set the unit of the current variable for the right axis
                                streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).setAxisIndex(2);
                                axisSet = true;
                                units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][1] = streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getUnit();
                            }
                        }
                    }
                    else {
                        // set the unit of the current variable for the left axis
                        streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).setAxisIndex(1);
                        axisSet = true;
                        units[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1][0] = streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getUnit();
                    }

                    final int graphInstrumentIndex = i;
                    final int variableIndex = j;

                    Button button = new Button(this);
                    button.setAllCaps(false);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_normal));
                    button.setText(streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartName() + " (" + streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getUnit() + ")");
                    button.setOnClickListener(v -> editVariable(streamSetup.getInstrumentArrayList().get(graphInstrumentIndex).getVariableArrayList().get(variableIndex).getChartIndex(), graphInstrumentIndex, variableIndex));
                    button.getBackground().setColorFilter(streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartColor(), PorterDuff.Mode.SRC);

                    if (!axisSet){
                        button.setTextColor(Color.RED);
                        button.setText(button.getText() + " !");
                    }

                    linearLayouts[streamSetup.getInstrumentArrayList().get(i).getVariableArrayList().get(j).getChartIndex() - 1].addView(button);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null){
            if (requestCode == ADD_OR_EDIT_VARIABLE) {
                if(resultCode == AppCompatActivity.RESULT_OK){
                    // update the streamSetup
                    streamSetup = null;
                    streamSetup = data.getParcelableExtra("STREAM_SETUP");
                    updateVariables();
                }
                if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                    // add or edit canceled, do nothing
                }
            }
        }
    }
}
