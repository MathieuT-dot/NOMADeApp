package com.nomade.android.nomadeapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Setup;
import com.nomade.android.nomadeapp.setups.Variable;
import com.nomade.android.nomadeapp.helperClasses.MessageCodes;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.services.UsbAndTcpService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

/**
 * ValuesActivity
 *
 * Activity to display the live values from the main board
 */
public class ValuesActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener{

    private static final String TAG = "ValuesActivity";
    private final Context context = this;

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private boolean initComplete = false;
    private int showStreamData = 0;
    private int convertedFrequency = 1;

    private LinearLayout variablesContainer;

    private TextView[] valueTextViews;

    private static final String CHOICE_DIALOG = "dialogTagChoice";

    private static SimpleDateFormat timeFormatter;

    private Setup memorySetup;
    private Setup streamSetup;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_values);

        SharedPreferences setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        timeFormatter = new SimpleDateFormat("d MMM yyyy HH:mm:ss,SSS", Locale.getDefault());

        if (savedInstanceState != null){
            convertedFrequency = savedInstanceState.getInt("CONVERTED_FREQUENCY");
        }

        variablesContainer = findViewById(R.id.variables_container);

        init();

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("CONVERTED_FREQUENCY", convertedFrequency);

        super.onSaveInstanceState(outState);
    }

    /**
     * Initializes the activity by getting the memorySetup that is in the memory of the Android device.
     */
    private void init(){

        String jsonSetupInMemory = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE).getString("setup_in_memory", "");
        if (jsonSetupInMemory != null && !jsonSetupInMemory.equals("")){
            memorySetup = Utilities.parseJsonSetup(context, jsonSetupInMemory, jsonTypeInfoList, jsonParameterInfoList);
            if (memorySetup != null){
                streamSetup = Utilities.generateStreamSetup(memorySetup);
                displayVariables();
                initComplete = true;
            }
        }
    }

    /**
     * Inflates the needed views to display the measured values.
     */
    private void displayVariables(){

        int amountOfInstruments = 0;
        int instrumentIndex = 0;
        int amountOfVariables = 0;
        int variableIndex = 0;

        for (Instrument instrument : streamSetup.getInstrumentArrayList()){
            if (instrument.getVariableArrayList().size() > 0){
                amountOfInstruments++;
                amountOfVariables += instrument.getVariableArrayList().size();
            }
        }

        View[] graphInstrumentInflatedLayouts = new View[amountOfInstruments];
        TextView[] graphInstrumentNameTextViews = new TextView[amountOfInstruments];

        View[] variablesInflatedLayouts = new View[amountOfVariables];
        TextView[] variableTextViews = new TextView[amountOfVariables];
        valueTextViews = new TextView[amountOfVariables];

        for (Instrument instrument : streamSetup.getInstrumentArrayList()){
            if (instrument.getVariableArrayList().size() > 0){

                LayoutInflater layoutInflater1 = LayoutInflater.from(context);
                graphInstrumentInflatedLayouts[instrumentIndex] = layoutInflater1.inflate(R.layout.linear_layout_graph_instrument, null);

                graphInstrumentNameTextViews[instrumentIndex] = graphInstrumentInflatedLayouts[instrumentIndex].findViewById(R.id.graph_instrument_name_text_view);
                graphInstrumentNameTextViews[instrumentIndex].setText(String.format("%s (%s)", instrument.getName(), instrument.getId()));

                variablesContainer.addView(graphInstrumentInflatedLayouts[instrumentIndex]);

                for (Variable variable : instrument.getVariableArrayList()){

                    LayoutInflater layoutInflater2 = LayoutInflater.from(context);
                    variablesInflatedLayouts[variableIndex] = layoutInflater2.inflate(R.layout.linear_layout_variable_value, null);

                    variableTextViews[variableIndex] = variablesInflatedLayouts[variableIndex].findViewById(R.id.variable_text_view);
                    variableTextViews[variableIndex].setText(String.format("%s (%s): ", variable.getName(), variable.getUnit()));

                    valueTextViews[variableIndex] = variablesInflatedLayouts[variableIndex].findViewById(R.id.value_text_view);
                    valueTextViews[variableIndex].setText(getString(R.string.ellipsis));

                    variablesContainer.addView(variablesInflatedLayouts[variableIndex]);

                    variableIndex++;
                }

                instrumentIndex++;
            }
        }
    }

    /**
     * Updates the values displayed in the activity.
     *
     * @param bigDecimalArrayList array list containing the measured values
     */
    private void updateValues(ArrayList<BigDecimal> bigDecimalArrayList){

        int textViewIndex = 0;
        int bigDecimalIndex = bigDecimalArrayList.size() - 1;

        for (Instrument instrument : streamSetup.getInstrumentArrayList()){

            for (Variable variable : instrument.getVariableArrayList()){

                if (bigDecimalIndex >= 0){
                    BigDecimal bigDecimal = bigDecimalArrayList.get(bigDecimalIndex--);
                    if (bigDecimal != null){
                        if (variable.getType() == Variable.LONG){
                            valueTextViews[textViewIndex++].setText(timeFormatter.format(new Date(bigDecimal.longValue())));
                        }
                        else {
                            valueTextViews[textViewIndex++].setText(bigDecimal.stripTrailingZeros().toPlainString());
                        }
                    }
                    else {
                        textViewIndex++;
                    }
                }
                else {
                    valueTextViews[textViewIndex++].setText("Error: Index out of bounds");
                }
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
                    showStreamData++;
                    if (showStreamData % convertedFrequency == 0) {
                        updateValues(bigDecimalArrayList);
                    }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_values, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_update_frequency) {
            int position = 0;
            switch (convertedFrequency) {
                case 50:
                    position = 0;
                    break;
                case 25:
                    position = 1;
                    break;
                case 10:
                    position = 2;
                    break;
                case 5:
                    position = 3;
                    break;
                case 2:
                    position = 4;
                    break;
                case 1:
                    position = 5;
                    break;
            }
            SimpleListDialog.build()
                    .title(R.string.select_update_frequency)
                    .choiceMode(SimpleListDialog.SINGLE_CHOICE)
                    .items(new String[]{"1Hz", "2Hz", "5Hz", "10Hz", "25Hz", "50Hz"})
                    .choicePreset(position)
                    .show(this, CHOICE_DIALOG);
        }
        return true;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (CHOICE_DIALOG.equals(dialogTag)){
            if (which == BUTTON_POSITIVE){
                switch (extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL)){
                    case "1Hz":
                        convertedFrequency = 50;
                        break;
                    case "2Hz":
                        convertedFrequency = 25;
                        break;
                    case "5Hz":
                        convertedFrequency = 10;
                        break;
                    case "10Hz":
                        convertedFrequency = 5;
                        break;
                    case "25Hz":
                        convertedFrequency = 2;
                        break;
                    case "50Hz":
                        convertedFrequency = 1;
                        break;
                }
            }
            return true;
        }

        return false;
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
            Utilities.displayToast(context, R.string.connection_to_the_dmu_is_active);

            if (UsbAndTcpService.isMeasurementRunning()){
                setTitle(String.format("%s (meas. ID: %s)", getTitle(), UsbAndTcpService.getMeasurementId()));
            }
        }
        else {
            Utilities.displayToast(context, R.string.no_connection_to_the_dmu);
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
}
