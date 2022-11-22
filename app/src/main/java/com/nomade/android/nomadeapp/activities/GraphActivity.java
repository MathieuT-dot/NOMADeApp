package com.nomade.android.nomadeapp.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.graphs.CoupleChartGestureListener;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MessageCodes;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.services.UsbAndTcpService;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Setup;
import com.nomade.android.nomadeapp.setups.Variable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GraphActivity
 *
 * This activity displays live graphs of the values received from the DMU.
 * Up to four graphs can be displayed, each with multiple variables.
 * The graphs can be configured in
 * {@link com.nomade.android.nomadeapp.activities.GraphConfigActivity}.
 */
public class GraphActivity extends AppCompatActivity {

    private static final String TAG = "GraphActivity";
    private final Context context = this;

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private boolean initComplete = false;
    private boolean graphIsRunning = false;

    private LineChart[] lineCharts;
    private boolean[] enableGraphs;
    private ArrayList<ArrayList<BigDecimal>> collectionOfData = new ArrayList<>();
    private String[][] units = new String[4][2];

    private LinearLayout[] linearLayouts = new LinearLayout[4];
    private View[] dividerViews = new View[3];

    private ArrayList<Long> timeStampArrayList;
    private ArrayList<Variable> graphVariableArrayList;

    // TODO fix orientation change with custom legends
//    List<List<LegendEntry>> customLegends = new ArrayList<List<LegendEntry>>(4);
//    boolean customLegendsAdded = false;

    private TextView placeholderTextView;

    private boolean graphsActive = false;

    private boolean showPauseResume = false;
    private boolean showClearConfiguration = false;
    private boolean showConfigure = false;

    private Menu menu;

    private Gson gson;

    private SharedPreferences setupSharedPreferences;
    private SharedPreferences graphSharedPreferences;
    private SharedPreferences.Editor graphEditor;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private Setup memorySetup;
    private Setup streamSetup;

    private int realTimeClockValueIndex = -1;

    private static final int CONFIGURE_GRAPHS = 1;

    private int entryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);

        graphSharedPreferences = getSharedPreferences(Constants.GRAPH_DATA, MODE_PRIVATE);
        graphEditor = graphSharedPreferences.edit();
        graphEditor.apply();

        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        placeholderTextView = findViewById(R.id.placeholder_text_view);

        gson = new Gson();

        String jsonMemorySetup = setupSharedPreferences.getString("setup_in_memory", "");
        if (jsonMemorySetup != null && !jsonMemorySetup.equals("")){
            memorySetup = Utilities.parseJsonSetup(context, jsonMemorySetup, jsonTypeInfoList, jsonParameterInfoList);
        }

        String jsonLastSetup = setupSharedPreferences.getString("last_setup_in_memory", "");

        String jsonStreamSetup = setupSharedPreferences.getString("stream_setup", "");
        if (jsonStreamSetup != null && !jsonStreamSetup.equals("")){
            try {
                streamSetup = gson.fromJson(jsonStreamSetup, Setup.class);
            }
            catch (JsonSyntaxException e) {
                jsonStreamSetup = null;
            }
        }

        if (graphVariableArrayList == null){
            graphVariableArrayList = new ArrayList<>();
        }

        if (jsonMemorySetup != null && !jsonMemorySetup.equals("") && jsonMemorySetup.equals(jsonLastSetup)){

            if (streamSetup != null){
                init();

                showPauseResume = true;
                showClearConfiguration = true;
                showConfigure = true;
                invalidateOptionsMenu();
            }
            else {
                if (memorySetup != null){
                    streamSetup = Utilities.generateStreamSetup(memorySetup);

                    if (streamSetup.getInstrumentArrayList() == null) {
                        showPauseResume = false;
                        showClearConfiguration = false;
                        showConfigure = false;
                        invalidateOptionsMenu();

                        placeholderTextView.setText(R.string.graphs_error);
                        placeholderTextView.setVisibility(View.VISIBLE);
                    }
                    else {
                        init();
                        showPauseResume = true;
                        showClearConfiguration = true;
                        showConfigure = true;
                        invalidateOptionsMenu();
                    }
                }
                else {
                    showPauseResume = false;
                    showClearConfiguration = false;
                    showConfigure = false;
                    invalidateOptionsMenu();

                    placeholderTextView.setText(getString(R.string.no_setup_in_memory));
                    placeholderTextView.setVisibility(View.VISIBLE);
                }
            }
        }
        else {
            // setup changed, remove streamSetup
            setupSharedPreferences.edit().remove("stream_setup").apply();
            streamSetup = null;

            if (memorySetup != null){
                setupSharedPreferences.edit().putString("last_setup_in_memory", jsonMemorySetup).apply();
                streamSetup = Utilities.generateStreamSetup(memorySetup);

                if (streamSetup.getInstrumentArrayList() == null) {
                    showPauseResume = false;
                    showClearConfiguration = false;
                    showConfigure = false;
                    invalidateOptionsMenu();

                    placeholderTextView.setText(R.string.graphs_error);
                    placeholderTextView.setVisibility(View.VISIBLE);
                }
                else {
                    init();
                    showPauseResume = true;
                    showClearConfiguration = true;
                    showConfigure = true;
                    invalidateOptionsMenu();
                }
            }
            else {
                showPauseResume = false;
                showClearConfiguration = false;
                showConfigure = false;
                invalidateOptionsMenu();

                placeholderTextView.setText(getString(R.string.no_setup_in_memory));
                placeholderTextView.setVisibility(View.VISIBLE);
            }
        }

        if (savedInstanceState != null){

            String jsonTimestamps = graphSharedPreferences.getString("TIMESTAMPS", "");
            if (jsonTimestamps != null && !jsonTimestamps.equals("")){
//                graphEditor.remove("TIMESTAMPS").apply();
                timeStampArrayList = gson.fromJson(jsonTimestamps, new TypeToken<ArrayList<Long>>(){}.getType());
            }

            entryCount = savedInstanceState.getInt("ENTRIES_COUNT");

            restoreEntries();
        }

        if (timeStampArrayList == null){
            timeStampArrayList = new ArrayList<>();
        }

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (lineCharts != null && lineCharts.length > 0){
            for (int i = 0; i < lineCharts.length; i++){
                if (lineCharts[i] != null){
                    int setIndex = 0;
                    while (lineCharts[i].getData().getDataSetByIndex(setIndex) != null){
                        graphEditor.putString(i + "." + setIndex, gson.toJson(((LineDataSet) lineCharts[i].getData().getDataSetByIndex(setIndex)).getValues(), new TypeToken<ArrayList<Entry>>(){}.getType())).apply();
                        setIndex++;
                    }
                }
            }

            graphEditor.putString("TIMESTAMPS", gson.toJson(timeStampArrayList, new TypeToken<ArrayList<Long>>(){}.getType())).apply();

            outState.putInt("ENTRIES_COUNT", entryCount);
        }

        super.onSaveInstanceState(outState);
    }

    /**
     * Initializes the views required for displaying the graphs.
     */
    private void init(){

        linearLayouts[0] = findViewById(R.id.chart_container0);
        linearLayouts[1] = findViewById(R.id.chart_container1);
        linearLayouts[2] = findViewById(R.id.chart_container2);
        linearLayouts[3] = findViewById(R.id.chart_container3);

        dividerViews[0] = findViewById(R.id.divider1);
        dividerViews[1] = findViewById(R.id.divider2);
        dividerViews[2] = findViewById(R.id.divider3);

        initGraphs();
    }

    /**
     * Initializes the graphs.
     */
    private void initGraphs(){

        graphsActive = false;
        graphVariableArrayList.clear();

        for (LinearLayout linearLayout : linearLayouts) {
            linearLayout.removeAllViews();
        }

        enableGraphs = new boolean[]{false, false, false, false};

        Instrument instrument;
        Variable variable;

        for (int i = 0; i < streamSetup.getInstrumentArrayList().size(); i++){

            instrument = streamSetup.getInstrumentArrayList().get(i);

            if (instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1) {
                realTimeClockValueIndex = instrument.getVariableArrayList().get(0).getValueIndex();
            }

            for (int j = 0; j < instrument.getVariableArrayList().size(); j++){

                variable = instrument.getVariableArrayList().get(j);

                if (variable.getChartIndex() > 0){

                    if (!enableGraphs[variable.getChartIndex() - 1]){
                        enableGraphs[variable.getChartIndex() - 1] = true;
                    }

                    // special case for the quaternions, they have no units, but real, i, j and k are used instead, they are assigned the same unit so they use one vertical axis
                    String currentUnit = variable.getUnit();
                    if (currentUnit.equals("real") || currentUnit.equals("i") || currentUnit.equals("j") || currentUnit.equals("k")) {
                        currentUnit = "real, i, j, k";
                    }

                    // check if the left axis already has a unit
                    if (units[variable.getChartIndex() - 1][0] != null && !units[variable.getChartIndex() - 1][0].equals("")){
                        // check if the unit of the current variable is the same as the unit of the left axis
                        if (units[variable.getChartIndex() - 1][0].equals(currentUnit)){
                            // set the left axis for the variable
                            variable.setAxisIndex(1);
                            graphVariableArrayList.add(variable);
                        }
                        else {
                            // check if the right axis already has a unit
                            if (units[variable.getChartIndex() - 1][1] != null && !units[variable.getChartIndex() - 1][1].equals("")){
                                // check if the unit of the current variable is the same as the unit of the right axis
                                if (units[variable.getChartIndex() - 1][1].equals(currentUnit)){
                                    // set the right axis for the variable
                                    variable.setAxisIndex(2);
                                    graphVariableArrayList.add(variable);
                                }
                                else {
                                    MyLog.e(TAG, "More than two different units for chart " + variable.getChartIndex() + ", " + instrument.getName() + ": " + variable.getName() + " is not visualised");
                                }
                            }
                            else {
                                // set the unit of the current variable for the right axis
                                variable.setAxisIndex(2);
                                units[variable.getChartIndex() - 1][1] = currentUnit;
                                graphVariableArrayList.add(variable);
                            }
                        }
                    }
                    else {
                        // set the unit of the current variable for the left axis
                        variable.setAxisIndex(1);
                        units[variable.getChartIndex() - 1][0] = currentUnit;
                        graphVariableArrayList.add(variable);
                    }
                }
            }
        }

        lineCharts = new LineChart[4];

        for (int i = 0; i < lineCharts.length; i++){

            if (enableGraphs[i] && (units[i][0] != null && !units[i][0].equals("") || units[i][1] != null && !units[i][1].equals(""))){

                linearLayouts[i].setVisibility(View.VISIBLE);
                if (i > 0){
                    dividerViews[i-1].setVisibility(View.VISIBLE);
                }

                lineCharts[i] = new LineChart(context);
                lineCharts[i].getDescription().setEnabled(false);
                lineCharts[i].setTouchEnabled(false);
                lineCharts[i].setDragEnabled(false);
                lineCharts[i].setScaleEnabled(false);
                lineCharts[i].setDrawGridBackground(false);
                lineCharts[i].setPinchZoom(false);

                LineData data = new LineData();
                data.setValueTextColor(Color.BLACK);

                lineCharts[i].setData(data);

                Legend l = lineCharts[i].getLegend();
                l.setForm(Legend.LegendForm.LINE);
                l.setTextColor(Color.BLACK);

                XAxis xl = lineCharts[i].getXAxis();
                xl.setTextColor(Color.BLACK);
                xl.setAvoidFirstLastClipping(true);
                xl.setEnabled(true);
                xl.setPosition(XAxis.XAxisPosition.BOTTOM);
                xl.setAvoidFirstLastClipping(false);
//                xl.setLabelRotationAngle(20);
                xl.setValueFormatter(new ValueFormatter() {

                    private final SimpleDateFormat mFormat = new SimpleDateFormat("mm'm':ss,SSS's'", Locale.getDefault());

                    @Override
                    public String getFormattedValue(float value) {
                        try {
                            return mFormat.format(new Date(timeStampArrayList.get((int)value)));
                        }
                        catch (IndexOutOfBoundsException e){
                            return "Error";
                        }
                    }
                });

                YAxis leftAxis = lineCharts[i].getAxisLeft();
                leftAxis.setTextColor(Color.BLACK);
                leftAxis.setDrawGridLines(true);
                leftAxis.setMinWidth(45f);
//                if (units[i][0] != null && !units[i][0].equals("")){
//                    leftAxis.setTextColor(Color.BLACK);
//                    leftAxis.setDrawGridLines(true);
//                    leftAxis.setMinWidth(45f);
//                }
//                else {
//                    leftAxis.setEnabled(false);
//                }

                YAxis rightAxis = lineCharts[i].getAxisRight();
                rightAxis.setTextColor(Color.BLACK);
                rightAxis.setDrawGridLines(true);
                rightAxis.setMinWidth(45f);
//                if (units[i][1] != null && !units[i][1].equals("")){
//                    rightAxis.setTextColor(Color.BLACK);
//                    rightAxis.setDrawGridLines(true);
//                    rightAxis.setMinWidth(45f);
//                }
//                else {
//                    rightAxis.setEnabled(false);
//                }

                lineCharts[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                linearLayouts[i].addView(lineCharts[i]);
            }
            else {
                linearLayouts[i].setVisibility(View.GONE);
                if (i > 0){
                    dividerViews[i-1].setVisibility(View.GONE);
                }
            }

//            customLegends.add(
//                    new ArrayList<LegendEntry>()
//            );
        }

        for (boolean b : enableGraphs){
            if (b) {
                graphsActive = true;
                break;
            }
        }

        if (graphsActive){
            showPauseResume = true;
            showClearConfiguration = true;
            invalidateOptionsMenu();

            placeholderTextView.setVisibility(View.GONE);
        }
        else {
            showPauseResume = false;
            showClearConfiguration = false;
            invalidateOptionsMenu();

            placeholderTextView.setText(getString(R.string.no_graphs_configured));
            placeholderTextView.setVisibility(View.VISIBLE);
        }

        initComplete = true;
        graphIsRunning = true;

        for (int i = 0; i < lineCharts.length; i++){
            if (lineCharts[i] != null){

                List<Chart> chartList = new ArrayList<>();

                for (int j = 0; j < lineCharts.length; j++){
                    if (i != j && lineCharts[j] != null){
                        chartList.add(lineCharts[j]);
                    }
                }

                lineCharts[i].setOnChartGestureListener(new CoupleChartGestureListener(lineCharts[i], chartList.toArray(new Chart[0])));
            }
        }
    }

    /**
     * Prepares the new entries by adding a timestamp to the timestamp array list and adds the
     * data to a queue waiting to be added to the graphs.
     *
     * @param bigDecimalArrayList array list of BigDecimal containing the measured values
     */
    private void prepareEntries(ArrayList<BigDecimal> bigDecimalArrayList){

        if (realTimeClockValueIndex == -1){
            if (timeStampArrayList.size() > 0) {
                timeStampArrayList.add((long) (10 * timeStampArrayList.size()));
                timeStampArrayList.add((long) (10 * timeStampArrayList.size()));
            }
            else {
                timeStampArrayList.add((long) 0);
            }
        }
        else {
            // TODO check special codes implementation (big decimal = null)
            BigDecimal bigDecimal;
            try {
                bigDecimal = bigDecimalArrayList.get(bigDecimalArrayList.size() - realTimeClockValueIndex);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                bigDecimal = null;
            }
            if (bigDecimal != null){
                if (timeStampArrayList.size() > 0) {
                    timeStampArrayList.add((timeStampArrayList.get(timeStampArrayList.size()-1) + bigDecimal.longValue()) / 2);
                    timeStampArrayList.add(bigDecimal.longValue());
                }
                else {
                    timeStampArrayList.add(bigDecimal.longValue());
                }
            }
            else {
                if (timeStampArrayList.size() > 0) {
                    timeStampArrayList.add(timeStampArrayList.get(timeStampArrayList.size()-1) + 10L);
                    timeStampArrayList.add(timeStampArrayList.get(timeStampArrayList.size()-1) + 10L);
                }
                else {
                    timeStampArrayList.add(0L);
                }
            }
        }

        collectionOfData.add(bigDecimalArrayList);

        if (collectionOfData.size() >= 1){

            if (graphIsRunning){
                addEntries();
            }
        }
    }

    /**
     * Adds the data waiting in the ArrayList to the graphs.
     */
    private void addEntries(){

        Variable variable;
        BigDecimal bigDecimal;

        for (ArrayList<BigDecimal> bigDecimalArrayList : collectionOfData){

            int[] setIndexes = new int[]{0,0,0,0};
            int bigDecimalArrayListSize = bigDecimalArrayList.size();

            for (int i = 0; i < graphVariableArrayList.size(); i++){

                variable = graphVariableArrayList.get(i);

                LineData data = lineCharts[variable.getChartIndex()-1].getData();

                LineDataSet set = (LineDataSet) data.getDataSetByIndex(setIndexes[variable.getChartIndex()-1]);

                if (set == null){
                    set = createSet(
                            variable.getChartName() + " (" + variable.getUnit() + ")",
                            null,
                            variable.getAxisIndex(),
                            variable.getChartColor()
                    );
                    data.addDataSet(set);
                }

                // TODO check special codes implementation (big decimal = null)
                if (!variable.isHundredHertz()) {
                    bigDecimal = bigDecimalArrayList.get(bigDecimalArrayListSize - variable.getValueIndex());
                    if (bigDecimal != null){
                        data.addEntry(new Entry(entryCount, bigDecimal.divide(BigDecimal.valueOf(variable.getFactor()), MathContext.DECIMAL128).floatValue()), setIndexes[variable.getChartIndex()-1]);
                    }
                    else {
                        data.addEntry(new Entry(entryCount, 0f), setIndexes[variable.getChartIndex()-1]);
                    }
                }
                else {
                    if (entryCount > 0) {
                        bigDecimal = bigDecimalArrayList.get(bigDecimalArrayListSize - variable.getValueIndex());
                        if (bigDecimal != null){
                            data.addEntry(new Entry(entryCount - 1, bigDecimal.divide(BigDecimal.valueOf(variable.getFactor()), MathContext.DECIMAL128).floatValue()), setIndexes[variable.getChartIndex()-1]);
                        }
                        else {
                            data.addEntry(new Entry(entryCount - 1, 0f), setIndexes[variable.getChartIndex()-1]);
                        }
                    }
                    bigDecimal = bigDecimalArrayList.get(bigDecimalArrayListSize - variable.getValueIndex() - variable.getOversampledOffset());
                    if (bigDecimal != null){
                        data.addEntry(new Entry(entryCount, bigDecimal.divide(BigDecimal.valueOf(variable.getFactor()), MathContext.DECIMAL128).floatValue()), setIndexes[variable.getChartIndex()-1]);
                    }
                    else {
                        data.addEntry(new Entry(entryCount, 0f), setIndexes[variable.getChartIndex()-1]);
                    }
                }

                setIndexes[variable.getChartIndex()-1]++;
            }

            entryCount++;
            entryCount++;
        }

        for (int i = 0; i < lineCharts.length; i++){
            if (enableGraphs[i]){
                lineCharts[i].notifyDataSetChanged();
                lineCharts[i].setVisibleXRangeMaximum(2000);
                lineCharts[i].moveViewToX(entryCount - 2);
            }
        }

//        if (!customLegendsAdded){
//            for (int i = 0; i < lineCharts.length; i++){
//
//                lineCharts[i].getData().notifyDataChanged();
//
//                if (enableGraphs[i]){
//                    for (LegendEntry legendEntry : customLegends.get(i)){
//                        MyLog.d(TAG, "LegendEntry label: " + legendEntry.label);
//                    }
//                    lineCharts[i].getLegend().setCustom(customLegends.get(i));
//                }
//            }
//            customLegendsAdded = true;
//        }

        collectionOfData.clear();
    }

    /**
     * Creates a new set using the arguments.
     *
     * @param setName name for the set
     * @param yVals list of entries
     * @param axisIndex index of the axis where the set needs to be added
     * @param color display color for the set in the graph
     * @return the created line data set
     */
    private LineDataSet createSet(String setName, List<Entry> yVals, int axisIndex, int color) {

        LineDataSet set = new LineDataSet(yVals, setName);

        if (axisIndex == 1){
            set.setAxisDependency(YAxis.AxisDependency.LEFT);

//            customLegends.get(chartIndex).add(
//                    0,
//                    new LegendEntry(
//                            setName,
//                            Legend.LegendForm.LINE,
//                            Float.NaN,
//                            Float.NaN,
//                            null,
//                            color
//                    )
//            );
        }
        else if (axisIndex == 2){
            set.setAxisDependency(YAxis.AxisDependency.RIGHT);

//            int nullIndex = -1;
//
//            for (int k = customLegends.get(chartIndex).size()-1; k >= 0; k--){
//                if (customLegends.get(chartIndex).get(k).label == null){
//                    nullIndex = k;
//                    break;
//                }
//            }
//
//            if (nullIndex == -1){
//                customLegends.get(chartIndex).add(new LegendEntry(null, Legend.LegendForm.EMPTY, Float.NaN, Float.NaN, null, Color.BLACK));
//                customLegends.get(chartIndex).add(new LegendEntry(null, Legend.LegendForm.EMPTY, Float.NaN, Float.NaN, null, Color.BLACK));
//                customLegends.get(chartIndex).add(new LegendEntry(null, Legend.LegendForm.EMPTY, Float.NaN, Float.NaN, null, Color.BLACK));
//
//                customLegends.get(chartIndex).add(
//                        new LegendEntry(
//                                setName,
//                                Legend.LegendForm.LINE,
//                                Float.NaN,
//                                Float.NaN,
//                                null,
//                                color
//                        )
//                );
//            }
//            else {
//                customLegends.get(chartIndex).add(
//                        nullIndex+1,
//                        new LegendEntry(
//                                setName,
//                                Legend.LegendForm.LINE,
//                                Float.NaN,
//                                Float.NaN,
//                                null,
//                                color
//                        )
//                );
//            }
        }
        else {
            MyLog.e(TAG, "Axis index is not 1 or 2 (" + axisIndex + ")");
        }

        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        return set;
    }

    /**
     * Restores the graph entries after an orientation change.
     */
    private void restoreEntries(){

        if (streamSetup == null) {
            return;
        }

        float entryCount = -1;
        int[] setIndexes = new int[]{0,0,0,0};

        Instrument instrument;
        Variable variable;

        for (int i = 0; i < streamSetup.getInstrumentArrayList().size(); i++){

            instrument = streamSetup.getInstrumentArrayList().get(i);

            for (int j = 0; j < instrument.getVariableArrayList().size(); j++){

                variable = instrument.getVariableArrayList().get(j);

                if (variable.getChartIndex() > 0 && variable.getAxisIndex() > 0){

                    if (lineCharts[variable.getChartIndex()-1] != null){
                        LineData data = lineCharts[variable.getChartIndex()-1].getData();

                        LineDataSet set = (LineDataSet) data.getDataSetByIndex(setIndexes[variable.getChartIndex()-1]);

                        List<Entry> entries = null;

                        String jsonEntries = graphSharedPreferences.getString((variable.getChartIndex()-1) + "." + setIndexes[variable.getChartIndex()-1], "");
                        if (jsonEntries != null && !jsonEntries.equals("")){
//                            graphEditor.remove((variable.getChartIndex()-1) + "." + setIndexes[variable.getChartIndex()-1]).apply();
                            entries = gson.fromJson(jsonEntries, new TypeToken<ArrayList<Entry>>(){}.getType());
                        }

                        if (set == null){
                            set = createSet(
                                    variable.getChartName() + " (" + variable.getUnit() + ")",
                                    entries,
                                    variable.getAxisIndex(),
                                    variable.getChartColor()
                            );
                            data.addDataSet(set);
                        }

                        entryCount = set.getEntryCount();

                        data.notifyDataChanged();

                        setIndexes[variable.getChartIndex()-1]++;
                    }
                }
            }
        }

        for (LineChart lineChart : lineCharts) {
            if (lineChart != null) {
                lineChart.notifyDataSetChanged();
                lineChart.setVisibleXRangeMaximum(1000);
                lineChart.moveViewToX(entryCount);
            }
        }
    }

    /**
     * Handler to handle the incoming data from the USB service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MessageCodes.USB_MSG_SEND_DATA) {
                ArrayList<BigDecimal> bigDecimalArrayList = (ArrayList<BigDecimal>) msg.getData().getSerializable("stream_data");
                if (bigDecimalArrayList != null && bigDecimalArrayList.size() > 0 && initComplete){
                    prepareEntries(bigDecimalArrayList);
                }
            } else {
                super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            doUnbindService();
        }
        catch (Throwable t) {
            MyLog.e(TAG, "Failed to unbind from the service", t);
        }
    }

    /**
     * Creates a new connection to the USB service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
//            displayToast(context, "USB service attached");
            try {
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
//            displayToast(context, "USB service disconnected");
        }
    };

    /**
     * Checks if the USB Service is running and if so,
     * it automatically binds to it.
     */
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (UsbAndTcpService.isRunning()) {
            doBindService();
            Utilities.displayToast(context, R.string.connection_to_the_dcu_is_active);

            if (UsbAndTcpService.isMeasurementRunning()){
                setTitle(String.format("%s (meas. ID: %s)", getTitle(), UsbAndTcpService.getMeasurementId()));
            }
        }
        else {
            Utilities.displayToast(context, R.string.no_connection_to_the_dcu);
        }
    }

    /**
     * Binds the activity to the USB service.
     */
    private void doBindService() {
        bindService(new Intent(this, UsbAndTcpService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
//        displayToast(context, "USB service binding");
    }

    /**
     * Unbinds the activity to the USB service.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MessageCodes.USB_MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
//            displayToast(context, "USB service unbinding");
        }
    }

    /**
     * Opens the activity to configure the graphs.
     */
    private void configureGraphs(){
        Intent intent = new Intent(this, GraphConfigActivity.class);
        intent.putExtra("STREAM_SETUP", streamSetup);
        startActivityForResult(intent, CONFIGURE_GRAPHS);
    }

    /**
     * Clears the current configuration of the graphs.
     */
    private void clearGraphConfiguration(){

        graphIsRunning = false;

        setupSharedPreferences.edit().remove("stream_setup").apply();

        if (memorySetup != null){
            streamSetup = null;
            streamSetup = Utilities.generateStreamSetup(memorySetup);
            setupSharedPreferences.edit().putString("stream_setup", gson.toJson(streamSetup)).apply();

            initGraphs();
        }
        else {
            showPauseResume = false;
            showClearConfiguration = false;
            showConfigure = false;
            invalidateOptionsMenu();

            placeholderTextView.setText(getString(R.string.no_setup_in_memory));
            placeholderTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null){
            if (requestCode == CONFIGURE_GRAPHS) {
                if(resultCode == AppCompatActivity.RESULT_OK){
                    // update the streamSetup
                    streamSetup = null;
                    streamSetup = data.getParcelableExtra("STREAM_SETUP");
                    timeStampArrayList.clear();
                    entryCount = 0;

                    setupSharedPreferences.edit().putString("stream_setup", gson.toJson(streamSetup)).apply();

                    initGraphs();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        this.menu = menu;

//        MyLog.d(TAG, "showPauseResume: %s", showPauseResume);
        this.menu.findItem(R.id.action_pause_resume).setVisible(showPauseResume && graphsActive);

//        MyLog.d(TAG, "showClearConfiguration: %s", showClearConfiguration);
        this.menu.findItem(R.id.action_clear_configuration).setVisible(showClearConfiguration && graphsActive);

//        MyLog.d(TAG, "showConfigure: %s", showConfigure);
        this.menu.findItem(R.id.action_configure).setVisible(showConfigure);

        invalidateOptionsMenu();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_configure) {
            configureGraphs();
        } else if (itemId == R.id.action_clear_configuration) {
            clearGraphConfiguration();
        } else if (itemId == R.id.action_pause_resume) {
            if (graphIsRunning) {
                pauseGraph();
            } else {
                resumeGraph();
            }
        }
        return true;
    }

    /**
     * Dynamically change the title of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the title
     * @param title new title for the option
     */
    private void setOptionTitle(int id, String title) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setTitle(title);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
        }
    }

    /**
     * Dynamically change the icon of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the icon
     * @param iconRes resource id of the new icon
     */
    private void setOptionIcon(int id, int iconRes) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setIcon(iconRes);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
        }
    }

    /**
     * Pauses the graphs, enabling the user to scroll and zoom on the graphs.
     */
    private void pauseGraph(){
        if (graphIsRunning){
            graphIsRunning = false;
            setOptionTitle(R.id.action_pause_resume, "Resume");
            setOptionIcon(R.id.action_pause_resume, R.drawable.ic_action_play_arrow);

            for (LineChart lineChart : lineCharts) {
                if (lineChart != null) {
                    lineChart.setTouchEnabled(true);
                    lineChart.setDragEnabled(true);
                    lineChart.setScaleEnabled(true);
                }
            }
        }
    }

    /**
     * Resumes the graphs while also resetting the scroll position and zoom.
     */
    private void resumeGraph(){
        if (!graphIsRunning){
            graphIsRunning = true;
            setOptionTitle(R.id.action_pause_resume, "Pause");
            setOptionIcon(R.id.action_pause_resume, R.drawable.ic_action_pause);

            for (LineChart lineChart : lineCharts) {
                if (lineChart != null) {
                    lineChart.setTouchEnabled(false);
                    lineChart.setDragEnabled(false);
                    lineChart.setScaleEnabled(false);
                    lineChart.fitScreen();
                }
            }
        }
    }
}