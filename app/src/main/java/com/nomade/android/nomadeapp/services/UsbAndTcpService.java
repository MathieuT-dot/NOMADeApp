package com.nomade.android.nomadeapp.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.activities.MainActivity;
import com.nomade.android.nomadeapp.communication.Connectivity;
import com.nomade.android.nomadeapp.communication.FT311UARTInterface;
import com.nomade.android.nomadeapp.communication.StreamData;
import com.nomade.android.nomadeapp.communication.TcpTelegram;
import com.nomade.android.nomadeapp.communication.UsbTelegram;
import com.nomade.android.nomadeapp.helperClasses.AppController;
import com.nomade.android.nomadeapp.helperClasses.ChannelIds;
import com.nomade.android.nomadeapp.helperClasses.Constants;
import com.nomade.android.nomadeapp.helperClasses.MessageCodes;
import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.StatusCodes;
import com.nomade.android.nomadeapp.helperClasses.Utilities;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Measurement;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.Setup;
import com.nomade.android.nomadeapp.setups.Variable;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * UsbAndTcpService
 *
 * Service that handles the USB connection to the DMU and the TCP connection to the server.
 */
public class UsbAndTcpService extends Service {

    private static final String TAG = "UsbAndTcpService";
    private final Context context = this;

    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor defaultEditor;
    private SharedPreferences setupDataSharedPreferences;
    private SharedPreferences.Editor setupDataEditor;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private static boolean isRunning = false;
    private static int statusCode = StatusCodes.UTS_NOT_INIT;

    private final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.
    private ArrayList<Messenger> mClients = new ArrayList<>(); // Keeps track of all current registered clients.

    /* declare a FT311 UART interface variable */
    private FT311UARTInterface uartInterface;

    private UsbReadThread usbReadThread;

    boolean watchDogRunnableActive = false;
    boolean bigDataRunnableActive = false;

    private List<UsbTelegram> usbTelegramList = Collections.synchronizedList(new ArrayList<>());

//    private boolean automaticFlow = false;

    private int showWatchDogMessageSent = 0;
    private int showWatchDogMessageReceived = 0;
    private boolean showLogWatchdogExceeded = true;

    private byte ownAddress = 0x00;
    private byte boardAddress = 0x01;

    private ArrayList<byte[]> bigDataList;
    private int bigDataPackets = 0;
    private int bigDataCurrentPacket = 0;
    private int bigDataPacketsWithoutAck = 0;
    private byte bigDataDsap = (byte) 0x00;
    private byte bigDataSsap = (byte) 0x00;
    private int bigDataPacketsToSend = 0;

    private List<StreamData> streamDataArrayList = Collections.synchronizedList(new ArrayList<>());
    private List<Float> dataOrder = Collections.synchronizedList(new ArrayList<>());
    private byte numberOfPackets = (byte) 0x00;
    private byte[] streamingLength = Utilities.shortToByteArray((short) 0);

    private long lastWatchdogMessage = 0;

    private Setup setup;
    private Setup streamSetup;

    private static boolean measurementRunning = false;
    private static boolean manualMeasurement = false;
    private int currentUserId = -1;
    // TODO get company ID from user (send measurement list and start measurement from list is not implemented, so this is not needed anymore)
    private int currentUserCompanyId = 0;
    private int currentSetupId = -1;
    private int currentSetupVersion = -1;
    private static int measurementId = -1;
    private String measurementName;
    private ArrayList<Measurement> measurementArrayList = new ArrayList<>();
    private int measurementIndex = 0;
    private long startTime = 946688400000L;
    private long endTime = 946688400000L;

    private List<TcpTelegram> tcpTelegramList = Collections.synchronizedList(new ArrayList<>());
    private final int  maxnumbytes = 65535;
    private byte [] readBuffer = new byte [maxnumbytes]; /*circular buffer*/
    private int totalBytes = 0;
    private int writeIndex = 0;
    private int readIndex = 0;

    private long previousCycleCounter = 0;
    private long currentCycleCounter = 1;
    private long lastSentCycleCounter = 0;
    private int missingCycleCounters = 0;
    private boolean tcpReadyForData = false;

    private static final int tcpBufferSize = 15000;
    private TcpTelegram[] tcpTelegramsOutput = new TcpTelegram[tcpBufferSize];
    private int writeTcpTelegramIndex = 0;
    private int readTcpTelegramIndex = 0;

    private static SimpleDateFormat timeFormatter;

    private IConnectionManager mManager;
    private boolean tcpConnectionAttemptInProgress = false;

    // reconnect delays                       0s 1s    5s    10s    20s    40s    80s    160s    320s    640s    1280s    2560s
    private int[] reconnectDelays = new int[]{0, 1000, 5000, 10000, 20000, 40000, 80000, 160000, 320000, 640000, 1280000, 2560000};
    private int delayIndex = 0;
    private Timer reconnectTimer;
    private boolean reconnectDelayInProgress = false;

    private int measurementMode = 0;
    private static final int START_MEASUREMENT_MODE = 1;
    private static final int SEND_MEASUREMENT_LIST_MODE = 2;
    private static final int START_MEASUREMENT_FROM_LIST_MODE = 3;

    private int actualSpeedValueIndex = -1;

    private static byte sensorsStatus = 0x00;

    private final Handler handler = new Handler();

    private SocketActionAdapter adapter = new SocketActionAdapter() {

        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            localLog(TAG, "Connecting Success");

            tcpConnectionAttemptInProgress = false;
            reconnectDelayInProgress = false;

            // check if the socket is opened successfully
            checkTcpSocket();
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                localLog(TAG, "Disconnected with exception:" + e.getMessage());

                tcpConnectionAttemptInProgress = false;

                if (!reconnectDelayInProgress) {
                    reconnectDelayInProgress = true;

                    delayIndex++;
                    if (delayIndex == reconnectDelays.length){
                        delayIndex--;
                    }

                    reconnectTimer = new Timer();
                    reconnectTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            localLog(TAG, "run: timer ran out, executing the tcpConnect now");
                            reconnectDelayInProgress = false;
                            tcpConnect("onSocketDisconnection");
                        }
                    }, reconnectDelays[delayIndex]);
                }

            } else {
                localLog(TAG, "Disconnect Manually");
            }
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            localLog(TAG, "Connecting Failed");

            tcpConnectionAttemptInProgress = false;

            if (!reconnectDelayInProgress) {
                reconnectDelayInProgress = true;

                delayIndex++;
                if (delayIndex == reconnectDelays.length){
                    delayIndex--;
                }

                reconnectTimer = new Timer();
                reconnectTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        localLog(TAG, "run: timer ran out, executing the tcpConnect now");
                        reconnectDelayInProgress = false;
                        tcpConnect("onSocketConnectionFailed");
                    }
                }, reconnectDelays[delayIndex]);
            }
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {

            byte[] bytes = new byte[data.getHeadBytes().length + data.getBodyBytes().length];
            System.arraycopy(data.getHeadBytes(), 0, bytes, 0, data.getHeadBytes().length);
            System.arraycopy(data.getBodyBytes(), 0, bytes, data.getHeadBytes().length, data.getBodyBytes().length);

            int readCount = bytes.length;
            if (readCount > 0){
                for (byte aByte : bytes) {
                    readBuffer[writeIndex] = aByte;
                    writeIndex++;
                    writeIndex %= maxnumbytes;
                }

                if (writeIndex >= readIndex){
                    totalBytes = writeIndex - readIndex;
                }
                else {
                    totalBytes = (maxnumbytes - readIndex) + writeIndex;
                }
            }

            readTcpTelegrams();

            while (tcpTelegramList.size() > 0){
                handleTcpTelegram(tcpTelegramList.remove(0));
            }

        }

        @Override
        public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {

        }

        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        MyLog.i(TAG, "Service Binded.");
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MyLog.i(TAG, "Service Created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){

            String action = intent.getAction();

            if (action != null) {
                switch (action){
                    case Constants.ACTION.ACTION_START_FOREGROUND_SERVICE:
                        startForegroundService();
                        Utilities.displayToast(getApplicationContext(), getString(R.string.usb_service_started));
                        break;
                    case Constants.ACTION.ACTION_STOP_FOREGROUND_SERVICE:
                        stopForegroundService();
                        Utilities.displayToast(getApplicationContext(), getString(R.string.usb_service_stopped));
                        break;
                }
            }
        }
        // TODO check if this has influence on the multiple firing of the "Connecting success" receiver
//        return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        MyLog.d(TAG, "onDestroy");

        if (uartInterface != null){
            MyLog.d(TAG, "stopForegroundService: Destroy Accessory");
            uartInterface.DestroyAccessory(true);
        }

        if (uartInterface != null){
            MyLog.d(TAG, "stopForegroundService: Close Accessory");
            uartInterface.CloseAccessory();
            uartInterface = null;
        }

        if (usbReadThread != null){
            MyLog.d(TAG, "stopForegroundService: Interrupt usbReadThread");
            usbReadThread.interrupt();
            usbReadThread = null;
        }

        try {
            if (mManager != null) {
                if (mManager.isConnect()){
                    MyLog.d(TAG, "stopForegroundService: Disconnect mManager");
                    mManager.disconnect();
                }
                MyLog.d(TAG, "stopForegroundService: Unregister mManager receiver");
                mManager.unRegisterReceiver(adapter);
                mManager = null;
            }
        }
        catch (IllegalMonitorStateException e){
            e.printStackTrace();
        }

        super.onDestroy();
    }

    private void startForegroundService(){

        MyLog.d(TAG, "Start foreground service.");

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Create notification builder.
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(this, ChannelIds.USB_SERVICE);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(this);
        }

        builder.setContentTitle("USB Service is running");
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        // Build the notification.
        Notification notification = builder.build();

        // Start foreground service.
        startForeground(Constants.NOTIFICATION_IDS.USB_SERVICE, notification);

        isRunning = true;
        statusCode = StatusCodes.UTS_NOT_INIT;
        manualMeasurement = false;
        measurementRunning = false;

        timeFormatter = new SimpleDateFormat("hh:mm:ss,SSS", Locale.getDefault());

        uartInterface = new FT311UARTInterface(this);
        /* thread to read the data */
        usbReadThread = new UsbReadThread();
        usbReadThread.start();
        uartInterface.ResumeAccessory();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultEditor = defaultSharedPreferences.edit();
        defaultEditor.apply();

        setupDataSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupDataEditor = setupDataSharedPreferences.edit();
        setupDataEditor.apply();

        jsonTypeInfoList = setupDataSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupDataSharedPreferences.getString(Constants.API_PARAMETERS, "");

        if (
                MainActivity.loggedIn && (
                        !getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SETUP_CREATE, false) ||
                                (
                                        getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SETUP_CREATE, false)
                                                &&
                                                defaultSharedPreferences.getBoolean(Constants.SETTING_AUTOMATIC_USB_COMMUNICATION, false)
                                )
                )
        ){
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    auto();
                }
            }, 500);
        }
    }

    private void stopForegroundService(){

        MyLog.d(TAG, "Stop foreground service.");

        if (isRunning) {
            MyLog.d(TAG, "stopForegroundService: isRunning = " + isRunning);
            isRunning = false;
            MyLog.d(TAG, "stopForegroundService: isRunning = " + isRunning);

            // Stop foreground service and remove the notification.
            MyLog.d(TAG, "stopForegroundService: stopForeground");
            stopForeground(true);

            // Stop the foreground service.
            MyLog.d(TAG, "stopForegroundService: stopSelf");
            stopSelf();
        }
    }

    /**
     * Checks the state of the service.
     *
     * @return true if the service is running
     *         false if the service isn't running
     */
    public static boolean isRunning()
    {
        return isRunning;
    }

    public static int getStatusCode() {
        return statusCode;
    }

    public static int getMeasurementId() {
        return measurementId;
    }

    public static boolean isMeasurementRunning() {
        return measurementRunning;
    }

    public static boolean isManualMeasurement() {
        return manualMeasurement;
    }

    public static byte getSensorsStatus() {
        return sensorsStatus;
    }

    public static void setSensorsStatus(byte sensorsStatus) {
        UsbAndTcpService.sensorsStatus = sensorsStatus;
    }

    /**
     * Handler to handle all incoming messages.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MessageCodes.USB_MSG_DATA_PLACEHOLDER:
                    break;
                case MessageCodes.USB_MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MessageCodes.USB_MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MessageCodes.USB_MSG_INIT:
                    if (uartInterface != null){
                        uartInterface.SetDefaultConfig();
                        localLog(TAG, "S: SetDefaultConfig");
                        statusCode = StatusCodes.UTS_INIT;
                        sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);
                        askAddress();
                    }
                    else {
                        localLog(TAG, "UART Interface is null!");
                    }
                    break;
                case MessageCodes.USB_MSG_CONFIG:
                    if (uartInterface != null){
                        uartInterface.SetDefaultConfig();
                        localLog(TAG, "S: SetDefaultConfig");
                    }
                    else {
                        localLog(TAG, "UART Interface is null!");
                    }
                    break;
                case MessageCodes.USB_MSG_AUTO:
                    auto();
                    break;
                case MessageCodes.USB_MSG_ASK_ADDRESS:
                    askAddress();
                    break;
                case MessageCodes.USB_MSG_ACK_MISS_CONFIG:
                    ackMissingConfig();
                    break;
                case MessageCodes.USB_MSG_ACK_EXIS_CONFIG:
                    ackExistingConfig();
                    break;
                case MessageCodes.USB_MSG_SETUP_JSON:
                    sendSetupJson();
                    break;
                case MessageCodes.USB_MSG_SETUP_RAW:
                    sendSetupRaw();
                    break;
                case MessageCodes.USB_MSG_START_STREAM:
                    streamDataArrayList.clear();
                    startStream();
                    break;
                case MessageCodes.USB_MSG_STOP_STREAM:
                    stopStream();
                    break;
                case MessageCodes.USB_MSG_REQUEST_JSON_SETUP:
                    requestSetupJson();
                    break;
                case MessageCodes.USB_MSG_START_MEASUREMENT:
                    measurementId = -1;
                    previousCycleCounter = 0;
                    currentCycleCounter = 1;
                    lastSentCycleCounter = 0;
                    missingCycleCounters = 0;
                    tcpReadyForData = false;
                    writeTcpTelegramIndex = 0;
                    readTcpTelegramIndex = 0;
                    streamDataArrayList.clear();

                    manualMeasurement = true;

                    measurementMode = START_MEASUREMENT_MODE;
                    Bundle data = msg.getData();
                    currentUserId = data.getInt("user_id");
                    currentSetupId = data.getInt("setup_id");
                    measurementName = data.getString("description");
                    measurementName = measurementName + " [UNKNOWN]";
                    startMeasurement(0);
                    break;
                case MessageCodes.USB_MSG_STOP_MEASUREMENT:
                    stopStream();
                    stopMeasurement();
                    if (manualMeasurement) {
                        manualMeasurement = false;
                        sendMessageToUI(MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STOPPED);
                    }
                    break;
                case MessageCodes.USB_MSG_SEND_MEASUREMENT_LIST:
                    measurementMode = SEND_MEASUREMENT_LIST_MODE;
                    currentUserId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1);
                    if (currentUserId < 0){
                        MyLog.e(TAG, "Current user ID is not valid (" + currentUserId + ")");
                        Utilities.displayToast(context, "Current user ID is not valid (" + currentUserId + ")");
                        break;
                    }
                    if (currentSetupId < 0){
                        MyLog.e(TAG, "Current setup ID is not valid (" + currentSetupId + ")");
                        Utilities.displayToast(context, "Current setup ID is not valid (" + currentSetupId + ")");
                        break;
                    }
                    localLog(TAG, "Preparing the measurement list");
                    getMeasurementList();
                    break;
                case MessageCodes.USB_MSG_STORE_OR_DELETE:
                    if (msg.arg1 == 1 && measurementName != null) {
                        measurementName = measurementName.replace(" [UNKNOWN]", "");
                    }
                    if (msg.arg1 == -1 && measurementName != null) {
                        measurementName = measurementName.replace(" [UNKNOWN]", " [DELETE]");
                    }
                    prepareMeasurement(false);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Sends a message to the UI.
     *
     * @param message integer that determines the message type
     */
    private void sendMessageToUI(int message){
        // Loop through all registered clients from back to front
        for (int i = mClients.size() - 1; i >= 0; i--){
            try {
                // Prepare data to be sent
                Message msg = Message.obtain(null, message);

                // Send the data to the client.
                try {
                    mClients.get(i).send(msg);
                }
                catch (IndexOutOfBoundsException e) {

                }
            }
            catch (RemoteException e){
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Sends a string to the UI.
     *
     * @param message to be sent to the UI
     */
    private void sendStringToUI(String message){
        // Prepare bundle to be sent
        Bundle b = new Bundle();
        b.putString("str1", message);

        // Loop through all registered clients from back to front
        for (int i = mClients.size() - 1; i >= 0; i--){
            try {
                // Prepare data to be sent
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_SEND_STRING);
                msg.setData(b);

                // Send the data to the client.
                try {
                    mClients.get(i).send(msg);
                }
                catch (IndexOutOfBoundsException e) {

                }
            }
            catch (RemoteException e){
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Sends the measured values to the UI for visualization.
     *
     * @param bigDecimalArrayList array list of BigDecimal containing the measured values
     */
    private void sendDataToUI(ArrayList<BigDecimal> bigDecimalArrayList){
        // Prepare bundle to be sent
        Bundle b = new Bundle();
        b.putSerializable("stream_data", bigDecimalArrayList);

        // Loop through all registered clients from back to front
        for (int i = mClients.size() - 1; i >= 0; i--){
            try {
                // Prepare data to be sent
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_SEND_DATA);
                msg.setData(b);

                // Send the data to the client.
                try {
                    mClients.get(i).send(msg);
                }
                catch (IndexOutOfBoundsException e) {

                }
            }
            catch (RemoteException e){
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Thread to read telegrams from the list.
     */
    private class UsbReadThread extends Thread {
        /* constructor */
        UsbReadThread(){
        }

        public void run() {

            while (true) {

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // thread is interrupted
                    return;
                }

                if (uartInterface != null){
                    byte status = uartInterface.readUsbTelegrams(usbTelegramList);

                    if (status == (byte) 0xFF){
                        MyLog.d(TAG, "uartInterface.readUsbTelegrams(usbTelegramList) status = 0xFF");
                        stopForegroundService();
                    }
                }

                while (usbTelegramList.size() > 0){

                    UsbTelegram usbTelegram = usbTelegramList.remove(0);

                    handleUsbTelegram(usbTelegram);
                }
            }
        }
    }

    /**
     * Analyzes the incoming telegrams and takes appropriate actions.
     *
     * @param usbTelegram to be analyzed
     */
    private void handleUsbTelegram(UsbTelegram usbTelegram){

        if (usbTelegram != null){

            byte sd = usbTelegram.getSd();
            byte da = usbTelegram.getDa();
            byte sa = usbTelegram.getSa();
            byte fc = usbTelegram.getFc();
            byte dsap = usbTelegram.getDsap();
            byte ssap = usbTelegram.getSsap();
            byte[] du = usbTelegram.getDu();
            byte[] usbTelegramBytes = usbTelegram.ConvertToByteArray();

//            MyLog.d(TAG, "R (HEX): " + Utilities.bytesToHex(usbTelegramBytes));

            switch (sd){

                case UsbTelegram.SD1:
                    MyLog.d(TAG, "R: SD1 received" + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                    break;

                case UsbTelegram.SD2:

                    if (ownAddress == (byte) 0x00 || ownAddress == da || (ownAddress | (byte) 0x80) == da){

                        if ((da & (byte) 0x80) == (byte) 0x80 && (sa & (byte) 0x80) == (byte) 0x80){
                            // SD2 with DSAP and SSAP

                            //
                            if (fc == (byte) 0x00){

                                // Watchdog message received
                                if (dsap == (byte) 0x00 && ssap == (byte) 0x80){
                                    lastWatchdogMessage = System.currentTimeMillis();

                                    showWatchDogMessageReceived++;
                                    if (showWatchDogMessageReceived % 40 == 0){
                                        localLog(TAG, "R: Watchdog message #" + showWatchDogMessageReceived + " received: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }

                                    if (!showLogWatchdogExceeded) {
                                        showLogWatchdogExceeded = true;
                                    }
                                }

                                // Address received (FE 81 00 80 01 XX)
                                // DU contains address (1 byte)
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x01){
                                    ownAddress = du[0];
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Address received: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Address received: " + Utilities.bytesToHex(du));
                                    }

                                    statusCode = StatusCodes.UTS_ADDRESS;
                                    sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);

                                    try {
                                        setRtcTimeFromNetwork();
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                        setRtcTimeFromDevice();
                                    }

                                    if (!watchDogRunnableActive){
                                        watchDogRunnableActive = true;
                                        new Thread(new WatchdogRunnable()).start();

                                        statusCode = StatusCodes.UTS_WATCHDOG;
                                        sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);
                                    }
                                }

                                // Mainboard already has a configuration (XX 81 00 02 80 XXXX XXXX)
                                // DU contains setup ID (2 bytes) and setup version (2 bytes)
                                if (dsap == (byte) 0x02 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Main board already has a configuration: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Main board already has a configuration: " + Utilities.bytesToHex(du));
                                    }

                                    int setupId = Utilities.shortFromByteArray(new byte[]{du[0], du[1]});
                                    int setupVersion = Utilities.shortFromByteArray(new byte[]{du[2], du[3]});

                                    setupDataEditor.putInt("current_setup_id", setupId).apply();
                                    setupDataEditor.putInt("current_setup_version", setupVersion).apply();

                                    currentSetupId = setupId;
                                    currentSetupVersion = setupVersion;

                                    if (Utilities.checkNetworkConnection(context)){
                                        if (setupId > 0){
                                            getSetupById(setupId);
                                        }
                                        else {
                                            Utilities.displayToast(context, "Error: Setup ID in DMU = " + setupId);
                                        }
                                    }
                                    else {
                                        boolean currentSetupInMemory = false;
                                        if (setupDataSharedPreferences.contains("setup_" + setupId)){

                                            String jsonSetup = setupDataSharedPreferences.getString("setup_" + setupId, "");
                                            if (jsonSetup != null && !jsonSetup.equals("")){
                                                setupDataEditor.putString("setup_in_memory", jsonSetup).apply();

                                                setup = Utilities.parseJsonSetup(context, jsonSetup, jsonTypeInfoList, jsonParameterInfoList);
                                                if (setup != null){
                                                    currentSetupInMemory = true;
                                                    calculateParametersForDataStream(setup);
                                                }
                                            }
                                        }

                                        if (currentSetupInMemory){
                                            sendMessageToUI(MessageCodes.USB_MSG_CURRENT_SETUP);
                                            streamSetup = Utilities.generateStreamSetup(setup);

//                                            if (automaticFlow) {
//                                                ackExistingConfig();
//                                            }

//                                        if (automaticFlow) {
//                                            startStream();
//                                        }
                                        } else {
                                            if (setupId > 0){
                                                getSetupById(setupId);
                                            }
                                            else {
                                                Utilities.displayToast(context, "Error: Setup ID in DMU = " + setupId);
                                            }
                                        }
                                    }
                                }

                                // Mainboard was never configured (XX 81 00 02 81 0000)
                                if (dsap == (byte) 0x02 && ssap == (byte) 0x81){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Mainboard was never configured: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Mainboard was never configured: " + Utilities.bytesToHex(du));
                                    }

                                    statusCode = StatusCodes.UTS_NO_SETUP;
                                    sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);

                                    setupDataEditor.putInt("current_setup_id", 0).apply();
                                    setupDataEditor.putInt("current_setup_version", 0).apply();
                                    setupDataEditor.putString("setup_in_memory", "").apply();
                                }

                                if (dsap == (byte) 0x80 && ssap == (byte) 0x10){

                                    if (du.length > 0){
                                        // Start streaming acknowledged (XX 81 00 80 10 80)
                                        if (du[0] == (byte) 0x80){
                                            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                localLog(TAG, "R: Start streaming acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                            }
                                            else {
                                                localLog(TAG, "R: Start streaming acknowledged");
                                            }

                                            if (watchDogRunnableActive){
                                                watchDogRunnableActive = false;
                                            }

                                            if (statusCode != StatusCodes.UTS_MEASUREMENT_BUSY) {
                                                statusCode = StatusCodes.UTS_STREAM_BUSY;
                                                sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);
                                            }
                                        }

                                        // Start streaming not acknowledged (XX 81 00 80 10 FE)
                                        if (du[0] == (byte) 0xFE){
                                            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                localLog(TAG, "R: Start streaming not acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                            }
                                            else {
                                                localLog(TAG, "R: Start streaming not acknowledged");
                                            }
                                        }
                                    }
                                }

                                // Data Stream (XX 81 00 11 80 XXXXXXXX XX XXXXXXXXXXXXX...)
                                // DU contains cycle counter (4 bytes), packetnumber (1 byte) and data
                                if (dsap == (byte) 0x11 && ssap == (byte) 0x80){

                                    boolean newCycleCounterAdded = false;

                                    // extract the cycle counter and packet number
                                    long cycleCounter = Utilities.intFromByteArray(new byte[]{du[0], du[1], du[2], du[3]});
//                                    MyLog.d(TAG, "cycleCounter: " + cycleCounter + " | bytes: " + Utilities.bytesToHex(usbTelegramBytes));
//                                    MyLog.d("CycleCounterDebug", "Cycle counter rec: " + cycleCounter);
                                    byte packetNumber = du[4];

//                                    if (cycleCounter % 100 == 0){
//                                        MyLog.d(TAG, "Telegram %s: %s", cycleCounter, Utilities.bytesToHex(usbTelegramBytes));
////                                        sendStringToUI(String.format("Raw message: %s", Utilities.bytesToHex(du)));
//                                    }

                                    // check if the cycle counter is already present in the stream data list, if so present=true and add new stream data to cycle counter
                                    boolean present = false;
                                    for (StreamData streamData : streamDataArrayList){
                                        if (streamData.getCycleCounter() == cycleCounter){
                                            present = true;

                                            // check if the previous packet is already received
                                            if (packetNumber == streamData.getReceivedPackets() + (byte) 0x01 && (du[du.length - 1] == (byte) 0x80 | du[du.length - 1] == (byte) 0x7f)){
                                                streamData.setReceivedPackets(packetNumber);

                                                byte[] newBytes = new byte[du.length - 6];
                                                System.arraycopy(du, 5, newBytes, 0, newBytes.length);

                                                streamData.setData(Utilities.concatenateArrays(streamData.getData(), newBytes));
                                                if (du[du.length - 1] == (byte) 0x7f){
                                                    streamData.setExpectingData(true);
                                                }
                                                else {
                                                    streamData.setExpectingData(false);
                                                }
                                            }
                                        }
                                    }

                                    // if the cycle counter is not yet present
                                    if (!present){
                                        // check to see if the stream data list is empty
                                        if (streamDataArrayList.size() > 0){
                                            // if the stream data list is not empty, check the last cycle counter in the list, if this is not the current one - 1, add the missing cycle counters first
                                            long lastCycleCounter = streamDataArrayList.get(streamDataArrayList.size() - 1).getCycleCounter();
                                            lastCycleCounter++;
                                            while (lastCycleCounter < cycleCounter){
                                                streamDataArrayList.add(
                                                        new StreamData(
                                                                lastCycleCounter,
                                                                new byte[0],
                                                                (byte) 0x00,
                                                                (byte) 0x00,
                                                                true,
                                                                false
                                                        )
                                                );
                                                lastCycleCounter++;
                                            }
                                        }

                                        if (packetNumber == (byte) 0x01){

                                            byte[] bytes = new byte[du.length - 6];
                                            System.arraycopy(du, 5, bytes, 0, bytes.length);

                                            // if the packet number is 0x01, the packet is the first for this cycle counter and it can be added
                                            streamDataArrayList.add(
                                                    new StreamData(
                                                            cycleCounter,
                                                            bytes,
                                                            packetNumber,
                                                            (byte) 0x00,
                                                            du[du.length - 1] == (byte) 0x7f,
                                                            false));
                                            newCycleCounterAdded = true;
                                        }
                                        // else the packet is not the first and we missed the first one for this cycle counter, the cycle counter is added with 0 received packets and the watchdog set to 0x03
                                        else {
                                            streamDataArrayList.add(
                                                    new StreamData(
                                                            cycleCounter,
                                                            new byte[0],
                                                            (byte) 0x00,
                                                            (byte) 0x03,
                                                            true,
                                                            false
                                                    )
                                            );
                                            newCycleCounterAdded = true;
                                        }
                                    }

                                    for (StreamData streamData : streamDataArrayList){
                                        // iterate through the stream data list to acknowledge completed cycle counters
                                        if (!streamData.isAcknowledged() && !streamData.isExpectingData()){

//                                            MyLog.d("CycleCounterDebug", "Cycle counter ack: " + streamData.getCycleCounter());

                                            if (cycleCounter % 100 == 0){
                                                if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                    localLog(TAG, "R: Streaming data | Cycle counter: " + cycleCounter + " | Data length (excl. 0x80): " + streamData.getData().length + " | Data: " + Utilities.bytesToHex(streamData.getData()));
                                                }
                                                else {
                                                    localLog(TAG, "R: Streaming data | Cycle counter: " + cycleCounter + " | Data length (excl. 0x80): " + streamData.getData().length);
                                                }
                                            }

                                            ackStream(streamData.getCycleCounter(), true);

                                            streamData.setAcknowledged(true);

                                            ArrayList<BigDecimal> bigDecimalArrayList = extractDataFromStream(streamData);
                                            sendDataToUI(bigDecimalArrayList);

                                            if (measurementRunning){
                                                currentCycleCounter = streamData.getCycleCounter();
                                                if (currentCycleCounter > previousCycleCounter){
                                                    if (currentCycleCounter != previousCycleCounter + 1){
                                                        missingCycleCounters += currentCycleCounter - previousCycleCounter - 1;
                                                    }
                                                    previousCycleCounter = currentCycleCounter;
                                                }

                                                byte[] data = streamData.getData();
                                                byte[] tcpDu = new byte[4 + data.length + 1];
                                                System.arraycopy(data, 0, tcpDu, 4, data.length);
                                                System.arraycopy(Utilities.longToByteArray(streamData.getCycleCounter()), 4, tcpDu, 0, 4);
                                                tcpDu[tcpDu.length - 1] = (byte) 0x80;
                                                tcpTelegramsOutput[writeTcpTelegramIndex++] = new TcpTelegram(TcpTelegram.SD2, (byte) 0x80, tcpDu, currentCycleCounter);
                                                writeTcpTelegramIndex %= tcpTelegramsOutput.length;

                                                TcpTelegram tcpTelegram;

                                                for (int i = 0; i < 5; i++){

                                                    if (tcpReadyForData && writeTcpTelegramIndex != readTcpTelegramIndex && mManager != null && mManager.isConnect()){
                                                        tcpTelegram = tcpTelegramsOutput[readTcpTelegramIndex++];
                                                        readTcpTelegramIndex %= tcpTelegramsOutput.length;

                                                        if (tcpTelegram != null) {
                                                            mManager.send(tcpTelegram);

                                                            lastSentCycleCounter = tcpTelegram.getCycleCounter();

                                                            if (lastSentCycleCounter % 100 == 0){
                                                                if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                                    localLog(TAG, "S: Stream data sent to database | Cycle counter: " + lastSentCycleCounter + " | Missing cycle counters: " + missingCycleCounters + " | Data: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                                                                }
                                                                else {
                                                                    localLog(TAG, "S: Stream data sent to database | Cycle counter: " + lastSentCycleCounter + " | Missing cycle counters: " + missingCycleCounters);
                                                                }
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        break;
                                                    }
                                                }

                                                if (tcpReadyForData && (mManager == null || !mManager.isConnect()) && !tcpConnectionAttemptInProgress && !reconnectDelayInProgress){
                                                    tcpConnect("handleUsbTelegram");
                                                }
                                            }
                                        }

                                        // iterate through the stream data list to increment the watchdog counter
                                        if (newCycleCounterAdded && !streamData.isAcknowledged() && cycleCounter != streamData.getCycleCounter()){
                                            streamData.setWatchdog((byte) (streamData.getWatchdog() + 0x01));
                                            if (streamData.getWatchdog() > (byte) 0x03){
                                                ackStream(streamData.getCycleCounter(), false);

                                                // not acknowledged cycle counters should be removed
                                                streamData.setAcknowledged(true);
                                            }
                                        }
                                    }

                                    // remove acknowledged cycle counters at the front of the stream data list, while keeping at least one cycle counter in the list
                                    while (streamDataArrayList.size() > 1 && streamDataArrayList.get(0).isAcknowledged()){
                                        streamDataArrayList.remove(0);
                                    }
                                }

                                // Stop streaming acknowledged (XX 81 00 80 12)
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x12){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Stop streaming acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Stop streaming acknowledged");
                                    }

                                    tcpReadyForData = false;

                                    if (!watchDogRunnableActive){
                                        watchDogRunnableActive = true;
                                        new Thread(new WatchdogRunnable()).start();
                                    }

                                    statusCode = StatusCodes.UTS_WATCHDOG;
                                    sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);
                                }

                                // Live Stream (XX 81 00 19 80 XXXXXXXX XX XXXXXXXXXXXXX...)
                                // DU contains cycle counter (4 bytes), packet number (1 byte) and data
                                if (dsap == (byte) 0x19 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Live Stream not implemented! | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Live Stream not implemented!");
                                    }
                                }

                                // Measurement started acknowledged (XX 81 00 80 20 XXXXXXXXXXXXXXXX)
                                // DU contains start timestamp in milliseconds (8 bytes)
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x20){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Measurement started acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Measurement started acknowledged");
                                    }

                                    startTime = Utilities.longFromByteArray(du);

                                    measurementRunning = true;

                                    if (manualMeasurement) {
                                        sendMessageToUI(MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STARTED);
                                    }

                                    statusCode = StatusCodes.UTS_MEASUREMENT_BUSY;
                                    sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);

                                    startStream();

                                    prepareMeasurement(true);
                                }

                                // Measurement start asked (XX 81 00 20 80 XXXXXXXX)
                                // DU contains measurement ID (4 bytes)
                                if (dsap == (byte) 0x20 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Measurement start asked | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Measurement start asked");
                                    }

                                    measurementId = Utilities.intFromByteArray(du);

                                    if (measurementId > 0){

                                        UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x20, new byte[]{});
                                        byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                        if (uartInterface != null){
                                            uartInterface.SendData(bytes.length, bytes);

                                            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                localLog(TAG, "S: Start measurement acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                            }
                                            else {
                                                localLog(TAG, "S: Start measurement acknowledged");
                                            }
                                        }
                                        else {
                                            localLog(TAG, "UART Interface is null!");
                                        }

                                        previousCycleCounter = 0;
                                        currentCycleCounter = 1;
                                        lastSentCycleCounter = 0;
                                        missingCycleCounters = 0;
                                        tcpReadyForData = false;
                                        writeTcpTelegramIndex = 0;
                                        readTcpTelegramIndex = 0;

                                        measurementMode = START_MEASUREMENT_FROM_LIST_MODE;

                                        getMeasurementList();
                                    }
                                    else if (measurementId == 0){
                                        localLog(TAG, "Measurement ID is 0, exception is not yet implemented!");
                                    }
                                    else {
                                        localLog(TAG, "Measurement ID is smaller than 0, exception is not yet implemented!");
                                    }
                                }

                                // Measurement stopped acknowledged (XX 81 00 80 21 XXXXXXXXXXXXXXXX)
                                // DU contains stop timestamp in milliseconds (8 bytes)
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x21){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Measurement stopped acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Measurement stopped acknowledged");
                                    }

                                    endTime = Utilities.longFromByteArray(du);
                                    measurementRunning = false;

                                    if (mManager != null && mManager.isConnect()){
                                        mManager.disconnect();
                                    }

                                    if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)) {
                                        prepareMeasurement(false);
                                        if (measurementName != null) {
                                            sendMessageToUI(MessageCodes.USB_MSG_STORE_OR_DELETE);
                                        }
                                    } else {
                                        if (measurementName != null) {
                                            measurementName = measurementName.replace(" [UNKNOWN]", "");
                                        }
                                        prepareMeasurement(false);
                                    }
                                }

                                // Measurement stop asked (XX 81 00 21 80)
                                if (dsap == (byte) 0x21 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Measurement stop asked | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Measurement stop asked");
                                    }

                                    UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x21, new byte[]{});
                                    byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                    if (uartInterface != null){
                                        uartInterface.SendData(bytes.length, bytes);

                                        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                            localLog(TAG, "S: Stop measurement acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                        }
                                        else {
                                            localLog(TAG, "S: Stop measurement acknowledged");
                                        }
                                    }
                                    else {
                                        localLog(TAG, "UART Interface is null!");
                                    }
                                }

                                // Measurement list acknowledged (XX 81 00 80 22)
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x22){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Measurement list acknowledged | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Measurement list acknowledged");
                                    }
                                }

                                // Acyclic Buffered Data Stream (XX 81 00 31 80 XXXXXXXX XX XXXXXXXXXXXXX...)
                                // DU contains cycle counter (4 bytes), packet number (1 byte) and data
                                if (dsap == (byte) 0x31 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Acyclic Buffered Data Stream not implemented! | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Acyclic Buffered Data Stream not implemented!");
                                    }
                                }

                                // Start transmission of tablet instrument data (XX 81 00 91 80)
                                if (dsap == (byte) 0x91 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Start transmission of tablet instrument requested | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Start transmission of tablet instrument requested");
                                    }

                                    // Acknowledge start transmission of tablet instrument (81 XX 00 80 91 80)
                                    UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x91, new byte[]{(byte) 0x80});
                                    byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "S: Start transmission of tablet instrument acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                    }
                                    else {
                                        localLog(TAG, "S: Start transmission of tablet instrument acknowledged");
                                    }

                                    uartInterface.SendData(bytes.length, bytes);
                                }

                                // Stop transmission of tablet instrument data
                                if (dsap == (byte) 0x93 && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG,"R: Stop transmission of tablet instrument requested | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG,"R: Stop transmission of tablet instrument requested");
                                    }

                                    // Acknowledge stop transmission of tablet instrument (81 XX 00 80 93 80)
                                    UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x93, new byte[]{(byte) 0x80});
                                    byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "S: Stop transmission of tablet instrument acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                    }
                                    else {
                                        localLog(TAG, "S: Stop transmission of tablet instrument acknowledged");
                                    }

                                    uartInterface.SendData(bytes.length, bytes);
                                }

                                // USB connection closing
                                if (dsap == (byte) 0xFE && ssap == (byte) 0x80){
                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: USB connection closing, shutting down the application | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: USB connection closing, shutting down the application");
                                    }

                                    stopForegroundService();

                                    Intent intent = new Intent(AppController.getInstance(), MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("KILL_APP", true);
                                    AppController.getInstance().startActivity(intent);
                                }
                            }

                            // Flow init
                            if (fc == (byte) 0x02){

                                // JSON Setup
                                if (dsap == (byte) 0x80 && ssap == (byte) 0x05){

                                    bigDataList.clear();
                                    bigDataCurrentPacket = 0;

                                    bigDataPackets = Utilities.intFromByteArray(new byte[]{du[0], du[1], du[2], du[3]});
                                    MyLog.d(TAG, "Total packets to receive: " + bigDataPackets);
                                    bigDataPacketsWithoutAck = du[4];
                                    MyLog.d(TAG, "Packets without ack: " + bigDataPacketsWithoutAck);

                                    UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x06, (byte) 0x05, (byte) 0x80, new byte[]{(byte) 0x00, du[0], du[1], du[2], du[3]});
                                    byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "S: Flow init acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                    }
                                    else {
                                        localLog(TAG, "S: Flow init acknowledged");
                                    }

                                    uartInterface.SendData(bytes.length, bytes);
                                }

                            }

                            // Flow Ack
                            if (fc == (byte) 0x06){

                                // Flow Ack (Init) Check DSAP and SSAP
                                if (dsap == bigDataSsap && ssap == bigDataDsap){

                                    // Flow Ack (Init)
                                    if (du[0] == (byte) 0x00){

                                        // Flow Ack (Init) check number of packets
                                        if (bigDataPackets == ByteBuffer.wrap(new byte[]{du[1], du[2], du[3], du[4]}).getInt()){

                                            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                localLog(TAG, "R: Flow ack init: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                            }
                                            else {
                                                localLog(TAG, "R: Flow ack init: " + Utilities.bytesToHex(du));
                                            }

                                            MyLog.d(TAG, "bigDataPacketsWithoutAck: " + bigDataPacketsWithoutAck);

                                            sendBigData(bigDataPacketsWithoutAck);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            // SD2 without DSAP and SSAP

                            // Flow Data
                            if (fc == (byte) 0x04){
                                int packetNumber = Utilities.intFromByteArray(new byte[]{du[0], du[1], du[2], du[3]});
                                byte[] data = new byte[du.length - 4];
                                System.arraycopy(du, 4, data, 0, data.length);

                                if (packetNumber == bigDataCurrentPacket + 1){
                                    bigDataCurrentPacket = packetNumber;
                                    bigDataList.add(data);

                                    if (bigDataCurrentPacket % bigDataPacketsWithoutAck == 0){
                                        byte[] currentPacket = Utilities.intToByteArray(bigDataCurrentPacket);
                                        UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x06, (byte) 0x00, (byte) 0x00, new byte[]{(byte) 0x80, currentPacket[0], currentPacket[1], currentPacket[2], currentPacket[3]});
                                        byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                            localLog(TAG, "S: Flow ok acknowledged (packet: " + bigDataCurrentPacket + ") | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                        }
                                        else {
                                            localLog(TAG, "S: Flow ok acknowledged (packet: " + bigDataCurrentPacket + ")");
                                        }

                                        uartInterface.SendData(bytes.length, bytes);

                                        if (bigDataCurrentPacket == bigDataPackets){

                                            localLog(TAG, "R: Flow control for JSON from DMU completed");

                                            int dataSize = 0;

                                            for (int i = 0; i < bigDataList.size(); i++){
                                                dataSize += bigDataList.get(i).length;
                                            }

                                            byte[] completeData = new byte[dataSize];
                                            int index = 0;

                                            for (int i = 0; i < bigDataList.size(); i++){
                                                byte[] partOfData = bigDataList.get(i);
                                                System.arraycopy(partOfData, 0, completeData, index, partOfData.length);
                                                index += partOfData.length;
                                            }

                                            String jsonInDmu = new String(completeData, StandardCharsets.UTF_8);
                                            MyLog.d(TAG, "JSON in DMU: " + jsonInDmu);
                                        }
                                    }
                                }
                                else {
                                    byte[] nextPacket = Utilities.intToByteArray(bigDataCurrentPacket + 1);
                                    UsbTelegram newUsbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x06, (byte) 0x00, (byte) 0x00, new byte[]{(byte) 0xFE, nextPacket[0], nextPacket[1], nextPacket[2], nextPacket[3]});
                                    byte[] bytes = newUsbTelegram.ConvertToByteArray();

                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "S: Flow error acknowledged (packet: " + (bigDataCurrentPacket + 1) + ") | UsbTelegram: " + Utilities.bytesToHex(bytes));
                                    }
                                    else {
                                        localLog(TAG, "S: Flow error acknowledged (packet: " + (bigDataCurrentPacket + 1) + ")");
                                    }

                                    uartInterface.SendData(bytes.length, bytes);
                                }
                            }

                            // Flow Ack
                            if (fc == (byte) 0x06){

                                // Flow Ack (Ok)
                                if (du[0] == (byte) 0x80){

                                    // Flow Ack (Init) check the current packet
                                    if ((bigDataCurrentPacket - 1) == Utilities.intFromByteArray(new byte[]{du[1], du[2], du[3], du[4]})){

                                        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                            localLog(TAG, "R: Flow ack Ok: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                        }
                                        else {
                                            localLog(TAG, "R: Flow ack Ok: " + Utilities.bytesToHex(du));
                                        }

                                        if (Utilities.intFromByteArray(new byte[]{du[1], du[2], du[3], du[4]}) == bigDataPackets){
                                            bigDataList.clear();
                                            bigDataPackets = 0;
                                            bigDataCurrentPacket = 0;
                                            bigDataPacketsWithoutAck = 0;

                                            // Flow control for JSON completed
                                            if (bigDataDsap == (byte) 0x03 && bigDataSsap == (byte) 0x80){
                                                bigDataDsap = (byte) 0x00;
                                                bigDataSsap = (byte) 0x00;

                                                if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                    localLog(TAG, "R: Flow control for JSON to DMU completed: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                                }
                                                else {
                                                    localLog(TAG, "R: Flow control for JSON to DMU completed: " + Utilities.bytesToHex(du));
                                                }

//                                                if (automaticFlow){
//                                                    sendSetupRaw();
//                                                    return;
//                                                }
                                            }

                                            // Flow control for RAW completed
                                            if (bigDataDsap == (byte) 0x04 && bigDataSsap == (byte) 0x80){
                                                bigDataDsap = (byte) 0x00;
                                                bigDataSsap = (byte) 0x00;

                                                if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                    localLog(TAG, "R: Flow control for RAW to DMU completed: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                                }
                                                else {
                                                    localLog(TAG, "R: Flow control for RAW to DMU completed: " + Utilities.bytesToHex(du));
                                                }

//                                                if (automaticFlow){
//                                                    localLog(TAG, "R: Automatic mode completed (RAW completed)");
//
////                                                    startStream();
//                                                    return;
//                                                }
                                            }

                                            // Flow control for measurement list completed
                                            if (bigDataDsap == (byte) 0x22 && bigDataSsap == (byte) 0x80){
                                                bigDataDsap = (byte) 0x00;
                                                bigDataSsap = (byte) 0x00;

                                                if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                                    localLog(TAG, "R: Flow control for measurement list to DMU completed: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                                }
                                                else {
                                                    localLog(TAG, "R: Flow control for measurement list to DMU completed: " + Utilities.bytesToHex(du));
                                                }
                                            }
                                        }
                                        else {
                                            MyLog.d(TAG, "Continue flow control after flow ack ok with packet " + bigDataCurrentPacket + ", packets to send: " + bigDataPacketsWithoutAck);
                                            sendBigData(bigDataPacketsWithoutAck);
                                        }
                                    }
                                }

                                // Flow Ack (Err)
                                if (du[0] == (byte) 0xFE){

                                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                                        localLog(TAG, "R: Flow ack error: " + Utilities.bytesToHex(du) + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                                    }
                                    else {
                                        localLog(TAG, "R: Flow ack error: " + Utilities.bytesToHex(du));
                                    }

                                    if (bigDataList.size() > 0 && bigDataPacketsWithoutAck > 0){
                                        // Get the corrupted packet number
                                        bigDataCurrentPacket = Utilities.intFromByteArray(new byte[]{du[1], du[2], du[3], du[4]});
                                        if (bigDataCurrentPacket % bigDataPacketsWithoutAck == 0){
                                            MyLog.d(TAG, "Continue flow control after flow ack error with packet " + bigDataCurrentPacket + ", packets to send: " + 1);
                                            sendBigData(1);
                                        }
                                        else {
                                            MyLog.d(TAG, "Continue flow control after flow ack error with packet " + bigDataCurrentPacket + ", packets to send: " + (bigDataPacketsWithoutAck + 1 - bigDataCurrentPacket % bigDataPacketsWithoutAck));
                                            sendBigData(bigDataPacketsWithoutAck + 1 - bigDataCurrentPacket % bigDataPacketsWithoutAck);
                                        }
                                    }
                                    else {
                                        localLog(TAG, "Flow control was already completed! Ignoring the flow ack error...");
                                    }
                                }
                            }
                        }

                    }
                    else {
                        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                            localLog(TAG, "R: Destination is not this device | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                        }
                        else {
                            localLog(TAG, "R: Destination is not this device");
                        }

                    }

                    break;

                case UsbTelegram.SD3:
                    MyLog.d(TAG, "R: SD3 received" + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                    break;

                case UsbTelegram.SD4:
                    MyLog.d(TAG, "R: SD4 received" + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                    break;

                case UsbTelegram.SC:
                    MyLog.d(TAG, "R: SC received" + " | UsbTelegram: " + Utilities.bytesToHex(usbTelegramBytes));
                    break;

            }

        }

    }

    /**
     * Activates the automatic communication.
     */
    private void auto(){
//        automaticFlow = true;
        localLog(TAG, "S: Automatic started");

        if (uartInterface != null){
            localLog(TAG, "S: SetDefaultConfig");
            uartInterface.SetDefaultConfig();
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }

        askAddress();
    }

    /**
     * Sends a telegram over USB to ask an address.
     */
    private void askAddress(){
        ownAddress = 0x00;
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x01, (byte) 0x80, new byte[0]);
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Address asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Address asked");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    private void setRtcTimeFromNetwork() throws IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress inetAddress = InetAddress.getByName(Constants.TIME_SERVER);
        TimeInfo timeInfo = timeClient.getTime(inetAddress);
        long networkTime = timeInfo.getMessage().getReceiveTimeStamp().getTime();
        byte[] timeBytes = Utilities.longToByteArray(networkTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH'h'mm'm'ss.SSS's'", Locale.getDefault());

        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x01, (byte) 0x81, timeBytes);
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Set RTC time from network (" + sdf.format(new Date(networkTime)) + ") | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Set RTC time from network (" + sdf.format(new Date(networkTime)) + ")");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    private void setRtcTimeFromDevice() {
        long deviceTime = System.currentTimeMillis();
        byte[] timeBytes = Utilities.longToByteArray(deviceTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH'h'mm'm'ss.SSS's'", Locale.getDefault());

        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x01, (byte) 0x81, timeBytes);
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Set RTC time from Android device (" + sdf.format(new Date(deviceTime)) + ") | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Set RTC time from Android device (" + sdf.format(new Date(deviceTime)) + ")");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a telegram over USB to acknowledge a missing configuration.
     */
    private void ackMissingConfig(){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x81, (byte) 0x02, new byte[]{(byte) 0x00, (byte) 0x00});
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Missing config message acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Missing config message acknowledged");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a telegram over USB to acknowledge an existing configuration.
     */
    private void ackExistingConfig(){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x02, new byte[]{(byte) ((currentSetupId >> 8) & 0xFF), (byte) (currentSetupId & 0xFF), (byte) ((currentSetupVersion >> 8) & 0xFF), (byte) (currentSetupVersion & 0xFF)});
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Existing config message acknowledged | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Existing config message acknowledged");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a telegram over USB with a setup in JSON format.
     */
    private void sendSetupJson(){
        String setupInMemory = setupDataSharedPreferences.getString("setup_in_memory", "");
        if (setupInMemory != null && !setupInMemory.equals("")){
            byte[] setupJson = Utilities.stringToBytesASCII(setupInMemory);
            byte[] transmissionSetupJson = new byte[setupJson.length + 3];
            transmissionSetupJson[0] = (byte) 0x02;
            transmissionSetupJson[transmissionSetupJson.length - 2] = (byte) 0x03;
            transmissionSetupJson[transmissionSetupJson.length - 1] = (byte) 0x04;
            System.arraycopy(setupJson, 0, transmissionSetupJson, 1, setupJson.length);
            localLog(TAG, "S: Setup JSON is being sent | Total length: " + transmissionSetupJson.length);

            prepareBigData256(transmissionSetupJson, (byte) 0x03, (byte) 0x80, 5);
        }
        else {
            localLog(TAG, "E: No setup in memory!");
        }
    }

    /**
     * Sends a telegram over USB with a setup in raw format.
     */
    private void sendSetupRaw(){
        String setupInMemory = setupDataSharedPreferences.getString("setup_in_memory", "");
        if (setupInMemory != null && !setupInMemory.equals("")){
            Setup setup = Utilities.parseJsonSetup(context, setupInMemory, jsonTypeInfoList, jsonParameterInfoList);

            if (setup != null) {
                byte[] bytes = Utilities.convertSetupToRawData(context, setup);

                calculateParametersForDataStream(setup);

                localLog(TAG, "S: Setup RAW is being sent | Total length: " + bytes.length);

                prepareBigData256(bytes, (byte) 0x04, (byte) 0x80, 2);
            }
            else {
                localLog(TAG, "E: Setup couldn't be parsed!");
            }
        }
        else {
            localLog(TAG, "E: No setup in memory!");
        }
    }

    /**
     * Requests the setup in the main board
     */
    private void requestSetupJson(){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x05, (byte) 0x80, new byte[0]);
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: JSON Setup asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: JSON Setup asked");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a telegram over USB to start the stream.
     */
    private void startStream(){
        if (numberOfPackets != (byte) 0x00 && streamingLength != Utilities.shortToByteArray((short) 0)){
            byte[] dataUnit = new byte[]{numberOfPackets, streamingLength[0], streamingLength[1]};
            UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x10, (byte) 0x80, dataUnit);
            byte[] bytes = usbTelegram.ConvertToByteArray();
            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                localLog(TAG, "S: Start stream asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
            }
            else {
                localLog(TAG, "S: Start stream asked");
            }

            if (uartInterface != null){
                uartInterface.SendData(bytes.length, bytes);
            }
            else {
                localLog(TAG, "UART Interface is null!");
            }
        }
    }

    /**
     * Sends a telegram over USB to acknowledge a stream packet.
     *
     * @param cycleCounter contains the cycle count
     * @param ack sends an ack if true
     *            sends a nack if false
     */
    private void ackStream(long cycleCounter, boolean ack){
        byte[] cycleCounterBytes = Utilities.longToByteArray(cycleCounter);
        byte[] dataUnit;
        if (ack){
            dataUnit = new byte[]{cycleCounterBytes[4], cycleCounterBytes[5], cycleCounterBytes[6], cycleCounterBytes[7],  (byte) 0x80};
//            MyLog.d(TAG, "ACK " + shortFromByteArray(cycleCounter));
        }
        else {
            dataUnit = new byte[]{cycleCounterBytes[4], cycleCounterBytes[5], cycleCounterBytes[6], cycleCounterBytes[7],  (byte) 0xFE};
//            MyLog.e(TAG, "NACK " + shortFromByteArray(cycleCounter));
        }
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x80, (byte) 0x11, dataUnit);
        byte[] bytes = usbTelegram.ConvertToByteArray();

        if (cycleCounter % 100 == 0){
            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                if (ack) {
                    localLog(TAG, "S: Streaming data acknowledged " + cycleCounter + " | UsbTelegram: " + Utilities.bytesToHex(bytes));
                }
                else {
                    localLog(TAG, "S: Streaming data not acknowledged " + cycleCounter + " | UsbTelegram: " + Utilities.bytesToHex(bytes));
                }
            }
            else {
                if (ack) {
                    localLog(TAG, "S: Streaming data acknowledged " + cycleCounter);
                }
                else {
                    localLog(TAG, "S: Streaming data not acknowledged " + cycleCounter);
                }
            }
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a telegram over USB to stop the stream
     */
    private void stopStream(){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x12, (byte) 0x80, new byte[]{});
        byte[] bytes = usbTelegram.ConvertToByteArray();
        if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
            localLog(TAG, "S: Stop stream asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
        }
        else {
            localLog(TAG, "S: Stop stream asked");
        }

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a message to start a new measurement.
     *
     * @param measurementId integer representing the measurement ID.
     */
    private void startMeasurement(int measurementId){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x20, (byte) 0x80, ByteBuffer.allocate(4).putInt(measurementId).array());
        byte[] bytes = usbTelegram.ConvertToByteArray();

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);

            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                localLog(TAG, "S: Start measurement asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
            }
            else {
                localLog(TAG, "S: Start measurement asked");
            }
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends a message to stop the current measurement.
     */
    private void stopMeasurement(){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x21, (byte) 0x80, new byte[]{});
        byte[] bytes = usbTelegram.ConvertToByteArray();

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);

            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                localLog(TAG, "S: Stop measurement asked | UsbTelegram: " + Utilities.bytesToHex(bytes));
            }
            else {
                localLog(TAG, "S: Stop measurement asked");
            }
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Sends the list of future measurements to the DMU.
     *
     * @param du byte array containing the list of measurements
     */
    private void sendMeasurementList(byte[] du){
        localLog(TAG, "S: Send measurement list | Total length: " + du.length);
        prepareBigData256(du, (byte) 0x22, (byte) 0x80, 2);
    }

    /**
     * Sets the measurement ID for the currently running measurement.
     *
     * @param du byte array containing the measurement ID
     */
    private void setMeasurementId(byte[] du){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x23, (byte) 0x80, du);
        byte[] bytes = usbTelegram.ConvertToByteArray();

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);

            if (defaultSharedPreferences.getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false)){
                localLog(TAG, "S: Set measurement ID | UsbTelegram: " + Utilities.bytesToHex(bytes));
            }
            else {
                localLog(TAG, "S: Set measurement ID");
            }
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Extracts the measurement data from the stream.
     *
     * @param streamData stream data to extract the values from
     * @return an array list of BigDecimal containing the measured values
     */
    private ArrayList<BigDecimal> extractDataFromStream(StreamData streamData){
        return extractDataFromStream(streamData.getData());
    }

    /**
     * Extracts the measurement data from the stream.
     *
     * @param data data to extract the values from
     * @return an array list of BigDecimal containing the measured values
     */
    private ArrayList<BigDecimal> extractDataFromStream(byte[] data){

        int index = data.length - 1;
        byte status;

        ArrayList<BigDecimal> bigDecimalArrayList = new ArrayList<>();

        Instrument instrument;
        Variable variable;

        for (int i = streamSetup.getInstrumentArrayList().size() - 1; i >= 0; i--){

            instrument = streamSetup.getInstrumentArrayList().get(i);

            if (instrument.getVariableArrayList().size() > 0){

                status = data[index];

                if (status == (byte) 0x80){

                    for (int j = instrument.getVariableArrayList().size() - 1; j >= 0; j--){

                        variable = instrument.getVariableArrayList().get(j);

                        BigDecimal bigDecimal = null;

                        switch (variable.getType()){

                            case Variable.BYTE:
                                bigDecimal = new BigDecimal(Byte.toString((byte) (data[index - variable.getIndexOffset()] & 0xFF)));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.UNSIGNED_BYTE:
                                bigDecimal = new BigDecimal(Integer.toString(Utilities.byteToUnsignedInt((byte) (data[index - variable.getIndexOffset()] & 0xFF))));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.SHORT:
                                bigDecimal = new BigDecimal(Short.toString(Utilities.shortFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]})));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.UNSIGNED_SHORT:
                                bigDecimal = new BigDecimal(Integer.toString(Utilities.shortToUnsignedInt(Utilities.shortFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]}))));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.INTEGER:
                                bigDecimal = new BigDecimal(Integer.toString(Utilities.intFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 3], data[index - variable.getIndexOffset() - 2], data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]})));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.LONG:
                                bigDecimal = new BigDecimal(Long.toString(Utilities.longFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 7], data[index - variable.getIndexOffset() - 6],data[index - variable.getIndexOffset() - 5], data[index - variable.getIndexOffset() - 4],data[index - variable.getIndexOffset() - 3], data[index - variable.getIndexOffset() - 2], data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]})));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.FLOAT:
                                bigDecimal = new BigDecimal(Float.toString(Utilities.floatFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 3], data[index - variable.getIndexOffset() - 2], data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]})));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;

                            case Variable.DOUBLE:
                                bigDecimal = new BigDecimal(Double.toString(Utilities.doubleFromByteArray(new byte[]{data[index - variable.getIndexOffset() - 7], data[index - variable.getIndexOffset() - 6],data[index - variable.getIndexOffset() - 5], data[index - variable.getIndexOffset() - 4],data[index - variable.getIndexOffset() - 3], data[index - variable.getIndexOffset() - 2], data[index - variable.getIndexOffset() - 1], data[index - variable.getIndexOffset()]})));
//                                bigDecimal = bigDecimal.multiply(new BigDecimal(Float.toString(variable.getFactor())));
                                break;
                        }

                        bigDecimalArrayList.add(bigDecimal);
                    }

                    index -= instrument.getIndexOffset();
                }
                else {
                    bigDecimalArrayList.add(null);
                    index--;
                }
            }
        }

        return bigDecimalArrayList;
    }

    /**
     * Sends the data from the Android device as instrument.
     *
     * @param dataBytes byte array containing the data from the Android device as instrument
     */
    private void sendAndroidInstrumentData(byte[] dataBytes){
        UsbTelegram usbTelegram = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x92, (byte) 0x80, dataBytes);
        byte[] bytes = usbTelegram.ConvertToByteArray();

        if (uartInterface != null){
            uartInterface.SendData(bytes.length, bytes);
        }
        else {
            localLog(TAG, "UART Interface is null!");
        }
    }

    /**
     * Prepares data bigger than 243 bytes to be sent in separate packets so the maximum length of
     * each packet doesn't exceed 256 bytes.
     *
     * @param bigDataBytes array of bytes to be sent
     * @param dsap destination service access point
     * @param ssap source service access point
     * @param packetsWithoutAck amount of packets to be sent without an acknowledgement in between
     */
    private void prepareBigData256(byte[] bigDataBytes, byte dsap, byte ssap, int packetsWithoutAck){

        MyLog.d(TAG, "prepareBigData256");

        int packetCount = bigDataBytes.length / 243;
        if (bigDataBytes.length % 243 != 0){
            packetCount++;
        }

        packetsWithoutAck = Math.min(packetCount, packetsWithoutAck);

        bigDataList = new ArrayList<>();

        byte[] flowInit = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x02, dsap, ssap, new byte[]{(byte) (packetCount >> 24), (byte) (packetCount >> 16), (byte) (packetCount >> 8), (byte) (packetCount), (byte) packetsWithoutAck}).ConvertToByteArray();

        MyLog.d(TAG, "flowInit: " + Utilities.bytesToHex(flowInit));

        bigDataList.add(flowInit);

        byte[] packetId;
        byte[] data;
        byte[] dataUnit;
        byte[] bytes;

        for (int i = 0; i < packetCount; i++){

            packetId = new byte[4];
            packetId[0] = (byte) ((i + 1) >> 24);
            packetId[1] = (byte) ((i + 1)>> 16);
            packetId[2] = (byte) ((i + 1) >> 8);
            packetId[3] = (byte) ((i + 1) /*>> 0*/);

            if ((i + 1) * 243 > bigDataBytes.length){
                data = Arrays.copyOfRange(bigDataBytes, i * 243, bigDataBytes.length);
            }
            else {
                data = Arrays.copyOfRange(bigDataBytes, i * 243, (i + 1) * 243);
            }

            dataUnit = new byte[packetId.length + data.length];
            System.arraycopy(packetId, 0, dataUnit, 0, packetId.length);
            System.arraycopy(data, 0, dataUnit, packetId.length, data.length);

            bytes = new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x04, dataUnit).ConvertToByteArray();

            bigDataList.add(bytes);
        }

        bigDataPackets = packetCount;
        bigDataCurrentPacket = 1;
        bigDataPacketsWithoutAck = packetsWithoutAck;
        bigDataDsap = dsap;
        bigDataSsap = ssap;

//        for (int i = 0; i < bigDataList.size(); i++){
//
//            printGiantLog("T" + i + ": ", bytesToHex(bigDataList.get(i)));
//
//        }

        uartInterface.SendData(flowInit.length, flowInit);

//        sendBigData(1);
    }

    /**
     * Sends the big data that was prepared.
     *
     * @param packetsToSend amount of packets to send from the queue
     */
    private void sendBigData(int packetsToSend){

        MyLog.d(TAG, "sendBigData (packetsToSend: " + packetsToSend + ")");

        bigDataPacketsToSend = packetsToSend;

        if (!bigDataRunnableActive){

            MyLog.d(TAG, "bigDataRunnableActive wasn't running, create and start new one");

            bigDataRunnableActive = true;
            new Thread(new BigDataRunnable()).start();
        }

//        byte[] bigDataBytes;
//
//        MyLog.d(TAG, "sendBigData: %s", packetsToSend);
//
//        if (uartInterface != null){
//            for (int i = 0; i < packetsToSend; i++){
//
//                MyLog.d(TAG, "i: %s | bigDataPacketsToSend: %s", i, packetsToSend);
//
//
//                bigDataBytes = bigDataList.get(bigDataCurrentPacket);
//
//                MyLog.d(TAG, "Big data %s sent", bigDataCurrentPacket);
////                    MyLog.d(TAG, "Big data %s sent | Packet: %s", bigDataCurrentPacket, Utilities.bytesToHex(bigDataBytes));
//
//                uartInterface.SendData(bigDataBytes.length, bigDataBytes);
//                bigDataCurrentPacket++;
//
//                if (bigDataCurrentPacket == bigDataList.size()){
//                    break;
//                }
//            }
//        }
//        else {
//            sendStringToUI("UART Interface is null!");
//        }
    }

    /**
     * Calculates the expected data length for the data stream.
     *
     * @param setup to be analyzed
     */
    private void calculateParametersForDataStream(Setup setup){

        int dataLength = 0;

        for (Instrument instrument : setup.getInstrumentArrayList()){

            for (Parameter parameter : instrument.getParameterArrayList()){

                if (parameter.getId() == Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE){

                    switch (Float.floatToIntBits(parameter.getValue())){

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1);
                            dataLength += 26;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2);
                            dataLength += 12;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1);
                            dataLength += 17;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR);
                            dataLength += 1;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR);
                            dataLength += 7;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D7_0XD7__4IR_ONLY_CALCULATED_VALUE:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D7_0XD7__4IR_ONLY_CALCULATED_VALUE);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1);
                            dataLength += 8;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2);
                            dataLength += 3;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_QUAT_ONLY__0XB4:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_QUAT_ONLY__0XB4);
                            dataLength += 8;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_0XB5:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_0XB5);
                            dataLength += 20;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_100Hz_0XB6:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_100Hz_0XB6);
                            dataLength += 40;
                            dataLength++;
                            break;

                        default:
                    }
                }
            }
        }

        dataLength++;

        streamingLength = Utilities.shortToByteArray((short) dataLength);
        numberOfPackets = (byte) (dataLength / 243);
        if (dataLength % 243 != 0){
            numberOfPackets++;
        }

        MyLog.d("CalculateLength", "Streaming length: " + Utilities.shortFromByteArray(streamingLength) + ", Number of packets: " + numberOfPackets + ", Data order length: " + dataOrder.size());
    }

    /**
     * Makes a log message appear in the logcat and in the UI.
     *
     * @param tag for the log
     * @param string message for the log
     */
    private void localLog(String tag, String string){
        sendStringToUI(timeFormatter.format(new Date(System.currentTimeMillis())) + " | " + string);
        MyLog.d(tag, string);
    }

    /**
     * Creates and executes a request to get a setup with the given ID.
     *
     * @param setupId ID of setup to get
     */
    private void getSetupById(int setupId) {

        String tag_string_req = "get_setup";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/");

        final StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    if (response != null && !response.equals("")){
                        setup = Utilities.parseJsonSetup(context, response, jsonTypeInfoList, jsonParameterInfoList);
                        if (setup != null){
                            setupDataEditor.putString("setup_in_memory", response).apply();
                            setupDataEditor.putString("setup_" + setup.getId(), response).apply();
                            calculateParametersForDataStream(setup);
                            sendMessageToUI(MessageCodes.USB_MSG_CURRENT_SETUP);

                            streamSetup = Utilities.generateStreamSetup(setup);

//                            if (automaticFlow) {
//                                ackExistingConfig();
//                            }

//                                if (automaticFlow){
//                                    startStream();
//                                }
                        }
                    }
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
        }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Creates and executes a request to get the list of measurements.
     */
    private void getMeasurementList(){
        String tag_string_req = "get_measurement_list";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    setupDataEditor.putString("measurement_list_json", response).apply();
                    parseMeasurementList(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Parses the JSON string containing the list of measurements.
     *
     * @param jsonMeasurementList JSON string containing the list of measurements
     */
    private void parseMeasurementList(String jsonMeasurementList){

        measurementArrayList.clear();

        SimpleDateFormat sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            JSONObject jObj = new JSONObject(jsonMeasurementList);

            JSONArray jMeasurementsArray = jObj.getJSONArray("data");

            measurementArrayList = new ArrayList<>();
            measurementIndex = 0;

            JSONObject currentMeasurement;

            for (int i = 0; i < jMeasurementsArray.length(); i++){

                currentMeasurement = jMeasurementsArray.getJSONObject(i);

                int mMeasurementId = currentMeasurement.getInt("id");
                int measurementCategoryId = currentMeasurement.getInt("measurement_category_id");
                int setupId = currentMeasurement.getInt("setup_id");
                int userId = currentMeasurement.getInt("user_id");
                String name = currentMeasurement.getString("name_en");
                String description = currentMeasurement.getString("description_en");

                Integer max;
                try {
                    max = currentMeasurement.getInt("max");
                }
                catch (JSONException e){
                    max = null;
                }

                Integer count;
                try {
                    count = currentMeasurement.getInt("count");
                }
                catch (JSONException e){
                    count = null;
                }

                String stringStartTime = currentMeasurement.getString("started_at");
                String stringEndTime = currentMeasurement.getString("stopped_at");

                Long startTime = 0L;
                try {
                    startTime = sdfDateAndTimeLaravel.parse(stringStartTime).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Long endTime = 0L;
                try {
                    endTime = sdfDateAndTimeLaravel.parse(stringEndTime).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                try {
                    stringStartTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringStartTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                try {
                    stringEndTime = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(stringEndTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (measurementMode == SEND_MEASUREMENT_LIST_MODE && currentUserId == userId && currentSetupId == setupId){
                    measurementArrayList.add(new Measurement(measurementId, measurementCategoryId, setupId, userId, name, description, max, count, startTime, endTime, stringStartTime, stringEndTime));
                }
                else if (measurementMode == START_MEASUREMENT_FROM_LIST_MODE && mMeasurementId == measurementId){
                    measurementArrayList.add(new Measurement(measurementId, measurementCategoryId, setupId, userId, name, description, max, count, startTime, endTime, stringStartTime, stringEndTime));
                    measurementIndex = measurementArrayList.size() - 1;
                    break;
                }
            }

            if (measurementArrayList.size() > 0){

                if (measurementMode == SEND_MEASUREMENT_LIST_MODE){
                    selectEligibleMeasurements();
                }
                else if (measurementMode == START_MEASUREMENT_FROM_LIST_MODE) {
                    Measurement measurement = measurementArrayList.get(measurementIndex);

                    currentUserId = measurement.getUserId();
                    currentSetupId = measurement.getSetupId();
                    measurementName = measurement.getName();
                    startTime = measurement.getStartTime();
                    endTime = measurement.getEndTime();

                    measurementRunning = true;

                    if (manualMeasurement) {
                        sendMessageToUI(MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STARTED);
                    }

                    statusCode = StatusCodes.UTS_MEASUREMENT_BUSY;
                    sendMessageToUI(MessageCodes.USB_MSG_STATUS_UPDATE);

                    tcpConnect("parseMeasurementList");
                }

            }
            else {
                localLog(TAG, "parseMeasurementList: The measurement list on the database is empty!");
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    /**
     * Selects eligible Measurements to be transferred based on the currently logged in user and
     * the setup that is in the DMU.
     */
    private void selectEligibleMeasurements(){

        long currentTimeMillis = System.currentTimeMillis();

        for (int i = measurementArrayList.size() - 1; i >= 0; i--){

            Measurement measurement = measurementArrayList.get(i);

            // remove ineligible measurements from the list (user ID's don't correspond, setup ID's don't correspond, the end time is before the current time)
            if (measurement.getUserId() != currentUserId || measurement.getSetupId() != currentSetupId || measurement.getEndTime() < currentTimeMillis){
                measurementArrayList.remove(i);
            }
        }

        if (measurementArrayList.size() > 0){

            byte asciiSoh = (byte) 0x01;
            byte asciiEtx = (byte) 0x03;
            byte asciiEot = (byte) 0x04;

            int amountOfMeasurements = measurementArrayList.size();

            int duLength = 11 + measurementArrayList.size() * 20 + 2;

            byte[] du = new byte[duLength];
            int index = 0;

            du[index] = asciiSoh;
            index += 1;
            System.arraycopy(ByteBuffer.allocate(4).putInt(currentSetupId).array(), 2, du, index, 2);
            index += 2;
            System.arraycopy(ByteBuffer.allocate(4).putInt(currentUserId).array(), 2, du, index, 2);
            index += 2;
            System.arraycopy(ByteBuffer.allocate(4).putInt(currentSetupVersion).array(), 2, du, index, 2);
            index += 2;
            System.arraycopy(ByteBuffer.allocate(4).putInt(currentUserCompanyId).array(), 2, du, index, 2);
            index += 2;
            System.arraycopy(ByteBuffer.allocate(4).putInt(amountOfMeasurements).array(), 2, du, index, 2);
            index += 2;

            for (int i = 0; i < measurementArrayList.size(); i++){
                Measurement measurement = measurementArrayList.get(i);
                System.arraycopy(ByteBuffer.allocate(4).putInt(measurement.getMeasurementId()).array(), 0, du, index, 4);
                index += 4;
                System.arraycopy(ByteBuffer.allocate(8).putLong(measurement.getStartTime()).array(), 0, du, index, 8);
                index += 8;
                System.arraycopy(ByteBuffer.allocate(8).putLong(measurement.getEndTime()).array(), 0, du, index, 8);
                index += 8;
            }

            du[index] = asciiEtx;
            index += 1;
            du[index] = asciiEot;
            index += 1;

            if (index != duLength){
                MyLog.w(TAG, "selectEligibleMeasurements: Final index is not equal to the DU length");
            }

            MyLog.d(TAG, "selectEligibleMeasurements: Measurement list: " + Utilities.bytesToHex(du));

            sendMeasurementList(du);
        }
        else {
            MyLog.e(TAG, "No eligible measurements for this user and setup");
            Utilities.displayToast(context, "No eligible measurements for this user and setup");
        }
    }

    /**
     * Prepares a JSON string to be submitted to create a new measurement.
     */
    private void prepareMeasurement(boolean createNew){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            JSONObject jObj = new JSONObject();

            JSONObject jData = new JSONObject();

            if (!createNew){
                jData.put("id", measurementId);
            }

            // TODO change later (1 Uncategorized / 2 Development / 3 Production)
            jData.put("measurement_category_id", 2);

            jData.put("setup_id", currentSetupId);
            jData.put("user_id", currentUserId);
            jData.put("name_en", measurementName);
            jData.put("started_at", sdf.format(startTime));
            jData.put("stopped_at", sdf.format(endTime));

            jObj.put("data", jData);

            String jsonMeasurement = jObj.toString();

            if (createNew){
                createNewMeasurement(jsonMeasurement);
            }
            else {
                updateMeasurement(jsonMeasurement);
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    /**
     * Creates and executes a request to create a new measurement based on a JSON string.
     *
     * @param jsonMeasurement JSON string containing the data to create a new measurement
     */
    private void createNewMeasurement(String jsonMeasurement){

        String tag_string_req = "create_new_measurement";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/");

        StringRequest strReq = new StringRequest(
                Request.Method.POST,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    extractMeasurementId(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return jsonMeasurement.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Extracts the measurement ID from the JSON string
     *
     * @param jsonResponse JSON string
     */
    private void extractMeasurementId(String jsonResponse){
        try {
            JSONObject jObj = new JSONObject(jsonResponse);

            JSONObject jData = jObj.getJSONObject("data");

            measurementId = jData.getInt("id");

            localLog(TAG, "Measurement ID received: " + measurementId);

            if (measurementId > 0){
                setMeasurementId(Utilities.intToByteArray(measurementId));
                tcpConnect("extractMeasurementId");
            }
            else {
                localLog(TAG, "ERROR: measurement id = -1");
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }
    }

    /**
     * Creates and executes a request to update a measurement based on a JSON string.
     *
     * @param jsonMeasurement JSON string containing the data to update a measurement
     */
    private void updateMeasurement(String jsonMeasurement){
        String tag_string_req = "create_new_measurement";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + measurementId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.PUT,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "measurements/" + measurementId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " +e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "Volley Error: " + e.toString() + ", " + e.getMessage());
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return jsonMeasurement.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void initManager(){
        String serverIp = "91.198.203.234";
        int serverPort = 20002;
        ConnectionInfo mInfo = new ConnectionInfo(serverIp, serverPort);
        OkSocketOptions mOkOptions = new OkSocketOptions.Builder()
                .setReconnectionManager(new NoneReconnect())
                .setConnectTimeoutSecond(10)
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        handler.post(runnable);
                    }
                })
                .setReaderProtocol(new IReaderProtocol() {
                    @Override
                    public int getHeaderLength() {
                        return 6;
                    }

                    @Override
                    public int getBodyLength(byte[] header, ByteOrder byteOrder) {
                        return Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, header[1], header[2]}) + 3;
                    }
                })
                .build();
        if (mManager != null) {
            mManager.unRegisterReceiver(adapter);
            mManager = null;
        }
        mManager = OkSocket.open(mInfo).option(mOkOptions);
        mManager.registerReceiver(adapter);
    }

    /**
     * Initializes a TCP connection.
     */
    private void tcpConnect(String source){

        localLog(TAG, "tcpConnect() called from " + source);
        localLog(TAG, "tcpConnect: measurementRunning = " + measurementRunning);
        localLog(TAG, "tcpConnect: Connectivity.isConnected(context) = " + Connectivity.isConnected(context));
        localLog(TAG, "tcpConnect: tcpConnectionAttemptInProgress = " + tcpConnectionAttemptInProgress);

        if (!Connectivity.isConnected(context)) {

            if (!reconnectDelayInProgress) {
                reconnectDelayInProgress = true;

                delayIndex++;
                if (delayIndex == reconnectDelays.length){
                    delayIndex--;
                }

                reconnectTimer = new Timer();
                reconnectTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        localLog(TAG, "run: timer ran out, executing the tcpConnect now");
                        reconnectDelayInProgress = false;
                        tcpConnect(source + " (repeated)");
                    }
                }, reconnectDelays[delayIndex]);
            }
        }
        else if (measurementRunning && Connectivity.isConnected(context) && !tcpConnectionAttemptInProgress){
            tcpReadyForData = false;
            tcpConnectionAttemptInProgress = true;
            initManager();
            mManager.connect();
        }
    }

    private void checkTcpSocket() {
        if ((mManager == null || !mManager.isConnect()) && !tcpConnectionAttemptInProgress && !reconnectDelayInProgress){
            tcpConnect("checkTcpSocket");
        } else if (mManager != null && mManager.isConnect()){
            TcpTelegram tcpTelegram = new TcpTelegram(TcpTelegram.SD2, (byte) 0x10, new byte[]{});
            localLog(TAG, "S: FC 0x10 check if the socket is opened successfully");
            mManager.send(tcpTelegram);
        }
    }

    /**
     * Ask permission to start streaming with measurement ID packet
     */
    private void sendTcpMeasurementId() {
        if ((mManager == null || !mManager.isConnect()) && !tcpConnectionAttemptInProgress && !reconnectDelayInProgress){
            tcpConnect("sendTcpMeasurementId");
        } else if (mManager != null && mManager.isConnect()){
            if (measurementId > 0){
                TcpTelegram tcpTelegram = new TcpTelegram(TcpTelegram.SD2, (byte) 0x11, Utilities.intToByteArray(measurementId));
                localLog(TAG, "S: FC 0x11 sends the measurement ID " + measurementId + ": " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                mManager.send(tcpTelegram);
            }
            else {
                localLog(TAG, "ERROR: measurement ID is invalid: " + measurementId);
                Utilities.displayToast(context, "ERROR: measurement ID is invalid: " + measurementId);
            }
        }
    }

    private void askTcpLatestCycleCounter() {
        if (mManager != null && mManager.isConnect()){
            TcpTelegram tcpTelegramToSend = new TcpTelegram(TcpTelegram.SD2, (byte) 0x12, Arrays.copyOfRange(Utilities.longToByteArray(lastSentCycleCounter), 4 ,8));
            localLog(TAG, "S: FC 0x12 ask the latest cycle counter and sent the last sent cycle counter: " + Utilities.bytesToHex(tcpTelegramToSend.ConvertToByteArray()));
            mManager.send(tcpTelegramToSend);
        }
        else {
            tcpConnect("askTcpLatestCycleCounter");
        }
    }

    /**
     * Converts the incoming binary data into TCP telegrams.
     */
    private void readTcpTelegrams(){

        boolean dataAvailable = true;

        byte sd;
        byte fc;
        byte[] du;
        int fcsRead;
        int fcsCalc;
        byte ed;

//        MyLog.d(TAG, "First 30 received bytes: " + Utilities.bytesToHex(Arrays.copyOfRange(readBuffer, readIndex, readIndex + 30)));

        boolean firstWrongSd = true;
        boolean firstArrayIndexOutOfBoundsException = true;

        while (dataAvailable){

            if (totalBytes == 0){
                return;
            }

            byte[] data = new byte[4096];
            int telegramIndex = readIndex;

            /*copy to the user buffer*/
            for(int count = 0; count < 4096; count++)
            {
                data[count] = readBuffer[telegramIndex];
                telegramIndex++;
                /*shouldnt read more than what is there in the buffer,
                 * 	so no need to check the overflow
                 */
                telegramIndex %= maxnumbytes;
            }

            try {
                sd = 0x00;
                fc = 0x00;
                du = null;
                fcsRead = 0;
                fcsCalc = 0;
                ed = 0x00;

                sd = data[0];

                switch (sd){

                    case TcpTelegram.SD1:
                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;
                        MyLog.e(TAG, "SD1 Telegram is not yet used or implemented");
                        break;

                    case TcpTelegram.SD2:

                        if (totalBytes >= 10){

                            int le = Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, data[1], data[2]});
                            int ler = Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, data[3], data[4]});

                            if (le == ler){

                                if (totalBytes >= le + 9){

                                    byte sd2 = data[5];

                                    if (sd2 == TcpTelegram.SD2){

                                        ed = data[le + 8];

                                        if (ed == TcpTelegram.ED){

                                            for (int i = 0; i < le; i++){
                                                fcsCalc += Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, data[i + 6]});
                                            }
                                            fcsCalc = Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, ByteBuffer.allocate(4).putInt(fcsCalc).array()[2], ByteBuffer.allocate(4).putInt(fcsCalc).array()[3]});

                                            fcsRead = Utilities.intFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, data[le + 6], data[le + 7]});

                                            if (fcsCalc == fcsRead){

                                                fc = data[6];

                                                if (le > 0){
                                                    du = Arrays.copyOfRange(data, 7, le + 6);
                                                }

//                                                MyLog.d(TAG, "SD2 Telegram correct");

                                                tcpTelegramList.add(new TcpTelegram(sd, fc, du));

                                                readIndex += le + 9;
                                                readIndex %= maxnumbytes;
                                                totalBytes -= le + 9;
                                            }
                                            else {
                                                MyLog.e(TAG, "Frame check sequence wrong (read: " + fcsRead + ", calculated: " + fcsCalc + ")");

                                                readIndex++;
                                                readIndex %= maxnumbytes;
                                                totalBytes--;
                                            }

                                        }
                                        else {
                                            MyLog.e(TAG, "Missing or wrong end delimiter (ed: " + ed + ")");

                                            readIndex++;
                                            readIndex %= maxnumbytes;
                                            totalBytes--;
                                        }

                                    }
                                    else {
                                        MyLog.e(TAG, "Missing start delimiter 2 (sd: " + sd2 + ")");

                                        readIndex++;
                                        readIndex %= maxnumbytes;
                                        totalBytes--;
                                    }

                                }
                                else {
//                                    MyLog.e(TAG, "Total bytes in buffer is shorter than the SD2 telegram length (Total bytes: " + totalBytes + " | SD2 length: " + le + "+9)");
                                    dataAvailable = false;
                                }

                            }
                            else {
                                MyLog.e(TAG, "Read length and length repeated doesn't match (le: " + (le & 0xFF) + ", ler: " + (ler & 0xFF) + ")");

                                readIndex++;
                                readIndex %= maxnumbytes;
                                totalBytes--;
                            }

                        }
                        else {
//                            MyLog.e(TAG, "Total bytes in buffer is shorter than the minimum SD2 telegram length (Total bytes: " + totalBytes + " | SD2 minimum length: 10)");
                            dataAvailable = false;
                        }

                        break;

                    case TcpTelegram.SD3:
                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;
                        MyLog.e(TAG, "SD3 Telegram is not yet used or implemented");
                        break;

                    case TcpTelegram.SD4:
                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;
                        MyLog.e(TAG, "SD4 Telegram is not yet used or implemented");
                        break;

                    case TcpTelegram.SC:
                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;
                        MyLog.e(TAG, "SC Telegram is not yet used or implemented");
                        break;

                    default:
                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;

                        if (firstWrongSd){
                            firstWrongSd = false;
                            MyLog.e(TAG, "FIRST unknown start delimiter (sd: " + Utilities.byteToHex(sd) + ")");
                        }
                }
            }
            catch (ArrayIndexOutOfBoundsException e){
                readIndex++;
                readIndex %= maxnumbytes;
                totalBytes--;

                if (firstArrayIndexOutOfBoundsException){
                    firstArrayIndexOutOfBoundsException = false;
                    MyLog.e(TAG, "FIRST ArrayIndexOutOfBoundsException: " + e);
                }
            }
        }
    }

    /**
     * Handles an incoming TCP telegram.
     *
     * @param tcpTelegram to be handled
     */
    private void handleTcpTelegram(TcpTelegram tcpTelegram){

        if (tcpTelegram != null){

//            MyLog.d(TAG, "%s", Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));

            byte sd = tcpTelegram.getSd();
            byte fc = tcpTelegram.getFc();
            byte[] du = tcpTelegram.getDu();

            switch (sd){

                case TcpTelegram.SD1:
                    MyLog.e(TAG, "SD1 Telegram is not yet used or implemented");
                    break;

                case TcpTelegram.SD2:

                    switch (fc){
                        // New protocol (04/02/2020)
                        case (byte) 0xFE:
                            localLog(TAG, "R: FC 0xFE socket not opened correctly: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                            // the socket was not opened successfully (too many streams, ...)
                            break;

                        case (byte) 0x90:
                            localLog(TAG, "R: FC 0x90 socket opened correctly: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                            // the socket was opened successfully
                            // proceed by sending the measurement id (FC 0x11)
                            sendTcpMeasurementId();
                            delayIndex = 0;
                            break;

                        case (byte) 0x91:
                            localLog(TAG, "R: FC 0x91 measurement id received correctly: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                            // measurement id received successfully by the server
                            // proceed by asking for the last cycle counter (FC 0x12)
                            askTcpLatestCycleCounter();
                            break;

                        case (byte) 0x92:
                            localLog(TAG, "R: FC 0x92 contains last cycle counter: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                            // message contains the last cycle counter, use it to check the difference between this and the current cycle counter
                            // proceed by sending the data (0x80)

                            long lastCycleCounterInDatabase = Utilities.longFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, du[0], du[1], du[2], du[3]});

                            MyLog.d(TAG, "handleTcpTelegram FC 0x92: lastCycleCounterInDatabase = " + lastCycleCounterInDatabase);

                            int difference = (int) (lastSentCycleCounter - lastCycleCounterInDatabase);
                            MyLog.d(TAG, "handleTcpTelegram FC 0x92: difference = " + difference);
                            if (difference > tcpBufferSize - 1000){
                                difference = tcpBufferSize - 1000;
                                localLog(TAG, "Difference is greater than " + (tcpBufferSize - 1000));
                            }

                            int mReadTcpTelegramIndex = readTcpTelegramIndex;
                            MyLog.d(TAG, "handleTcpTelegram FC 0x92: mReadTcpTelegramIndex = readTcpTelegramIndex = " + mReadTcpTelegramIndex);

                            if (mReadTcpTelegramIndex - difference < 0){
                                mReadTcpTelegramIndex += tcpBufferSize - difference;
                            }
                            else {
                                mReadTcpTelegramIndex -= difference;
                            }
                            MyLog.d(TAG, "handleTcpTelegram FC 0x92: mReadTcpTelegramIndex modified = " + mReadTcpTelegramIndex);

                            if (0 <= mReadTcpTelegramIndex && mReadTcpTelegramIndex < tcpBufferSize){
                                readTcpTelegramIndex = mReadTcpTelegramIndex;
                            }
                            MyLog.d(TAG, "handleTcpTelegram FC 0x92: readTcpTelegramIndex modified = " + readTcpTelegramIndex);

                            tcpReadyForData = mManager != null && mManager.isConnect();

                            break;

                        case (byte) 0xD2:
                            localLog(TAG, "R: FC 0xD2 contains last cycle counter: " + Utilities.bytesToHex(tcpTelegram.ConvertToByteArray()));
                            // message contains the last cycle counter for logging purposes (every 10000th cycle counter)

                            long logLastCycleCounterInDatabase = Utilities.longFromByteArray(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, du[0], du[1], du[2], du[3]});
                            localLog(TAG, "Android processed cycle = " + lastSentCycleCounter + " | Server processed cycle = " + logLastCycleCounterInDatabase);

                            break;
                    }

                    break;

                case TcpTelegram.SD3:
                    MyLog.e(TAG, "SD3 Telegram is not yet used or implemented");
                    break;

                case TcpTelegram.SD4:
                    MyLog.e(TAG, "SD4 Telegram is not yet used or implemented");
                    break;

                case TcpTelegram.SC:
                    MyLog.e(TAG, "SC Telegram is not yet used or implemented");
                    break;

                default:
                    MyLog.e(TAG, "SD not recognized: " + sd);
                    break;
            }

        }

    }

    /**
     * Thread to send watchdog messages over USB to keep the connection alive.
     */
    private class WatchdogRunnable implements Runnable {

        public void run() {

            byte[] bytes = (new UsbTelegram(UsbTelegram.SD2, boardAddress, ownAddress, (byte) 0x00, (byte) 0x00, (byte) 0x80, new byte[0])).ConvertToByteArray();

            lastWatchdogMessage = System.currentTimeMillis();

            while (true) {

                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e){
                    // thread is interrupted
                    return;
                }

                if (watchDogRunnableActive){
                    if (uartInterface != null){
                        uartInterface.SendData(bytes.length, bytes);

                        showWatchDogMessageSent++;
                        if (showWatchDogMessageSent % 40 == 0){
                            localLog(TAG, "S: Watchdog message #" + showWatchDogMessageSent + " sent: " + Utilities.bytesToHex(bytes));
                        }

                        if (showLogWatchdogExceeded && System.currentTimeMillis() - lastWatchdogMessage > 500){
                            MyLog.e(TAG, "Watchdog time exceeded!");
                            showLogWatchdogExceeded = false;
                        }
                    }
                }
                else {
                    break;
                }
            }
        }
    }

    private class BigDataRunnable implements Runnable {

        public void run(){

            byte[] bigDataBytes;

            if (uartInterface != null){
                for (int i = 0; i < bigDataPacketsToSend; i++){

                    MyLog.d(TAG, "i: " + i + " | bigDataPacketsToSend: " + bigDataPacketsToSend);

                    bigDataBytes = bigDataList.get(bigDataCurrentPacket);

                    MyLog.d(TAG, "Big data " + bigDataCurrentPacket + " sent");
//                    MyLog.d(TAG, "Big data %s sent | Packet: %s", bigDataCurrentPacket, Utilities.bytesToHex(bigDataBytes));

                    uartInterface.SendData(bigDataBytes.length, bigDataBytes);
                    bigDataCurrentPacket++;

                    if (bigDataCurrentPacket == bigDataList.size()){
                        break;
                    }

                    if (i == bigDataPacketsToSend - 1){
                        break;
                    }

                    try {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException e){
                        // thread is interrupted
                        return;
                    }
                }
            }
            else {
                localLog(TAG, "UART Interface is null!");
            }

            bigDataRunnableActive = false;
        }
    }
}

