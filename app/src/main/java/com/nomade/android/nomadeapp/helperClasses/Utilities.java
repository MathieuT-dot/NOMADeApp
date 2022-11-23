package com.nomade.android.nomadeapp.helperClasses;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.appcompat.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.ClientError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.nomade.android.nomadeapp.R;
import com.nomade.android.nomadeapp.activities.MainActivity;
import com.nomade.android.nomadeapp.setups.Instrument;
import com.nomade.android.nomadeapp.setups.Parameter;
import com.nomade.android.nomadeapp.setups.ParameterOption;
import com.nomade.android.nomadeapp.setups.Setup;
import com.nomade.android.nomadeapp.setups.Type;
import com.nomade.android.nomadeapp.setups.Variable;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestion;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestionnaire;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;

/**
 * Utilities
 *
 * Various utilities used to parse JSON, convert value, displays toast with adjustable text size
 */
public class Utilities {

    private static final String TAG = "Utilities";

    /**
     * Displays information about a Volley Error.
     *
     * @param context needed for toasts and shared preferences
     * @param volleyError of which the information needs to be displayed
     * @param extra optional extra info
     */
    public static void displayVolleyError(Context context, VolleyError volleyError, int extra){

        String developerString = "";
        String errorString = "";
        String extraString = "";
        boolean showExtra = true;

        if (volleyError instanceof TimeoutError){
            developerString = context.getString(R.string.timeout_error);
            errorString = context.getString(R.string.no_response_from_server);
        }
        else if (volleyError instanceof NoConnectionError){
            developerString = context.getString(R.string.no_connection_error);

            ConnectivityManager cm = (ConnectivityManager)context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = null;
            if (cm != null) {
                activeNetwork = cm.getActiveNetworkInfo();
            }
            if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()){
                errorString  = context.getString(R.string.server_unreachable);
            } else {
                errorString = context.getString(R.string.no_internet_access);
            }
        }
        else if (volleyError instanceof AuthFailureError){
            developerString = context.getString(R.string.auth_failure_error);
        }
        else if (volleyError instanceof ClientError){
            developerString = context.getString(R.string.client_error);
            if (extra == Constants.VOLLEY_ERRORS.LOGIN_ATTEMPT){
                errorString = context.getString(R.string.wrong_login_credentials);
                showExtra = false;
            }
            else {
                errorString = context.getString(R.string.logged_in_other_device);
                showExtra = false;
            }
        }
        else if (volleyError instanceof ServerError){
            developerString = context.getString(R.string.server_error);
        }
        else if (volleyError instanceof NetworkError){
            developerString = context.getString(R.string.network_error);
        }
        else if (volleyError instanceof ParseError){
            developerString = context.getString(R.string.parse_error);
        }
        else {
            developerString = context.getString(R.string.volley_error);
        }

        if (extra == Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA){
            extraString = context.getString(R.string.displaying_offline_data);
        }


        if (context.getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)){
            if (showExtra){
                displayToast(context, developerString + ": " + errorString + extraString);
            }
            else {
                displayToast(context, developerString + ": " + errorString);
            }
        }
        else {
            if (!errorString.equals("")){
                if (showExtra){
                    displayToast(context, errorString + extraString);
                }
                else {
                    displayToast(context, errorString);
                }
            }
            else {
                displayToast(context, developerString);
            }
        }
    }

    /**
     * Displays information about a Volley Error.
     *
     * @param context needed for toasts and shared preferences
     * @param volleyError of which the information needs to be displayed
     */
    public static void displayVolleyError(Context context, VolleyError volleyError){
        displayVolleyError(context, volleyError, -1);
    }

    /**
     * Displays a toast with the given parameters
     *
     * @param context context to display the toast
     * @param text a string containing the text that needs to be displayed
     * @param duration an integer determining the duration of the toast
     */
    public static void displayToast(Context context, String text, int duration) {
        try {
            Toast toast = Toast.makeText(context, text, duration);
            ViewGroup group = (ViewGroup) toast.getView();
            TextView toastView = (TextView) group.getChildAt(0);
            toastView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_normal));
            toast.show();
        }
        catch (RuntimeException e) {
            MyLog.e(TAG, "RuntimeException while making toast: " + text);
            Toast.makeText(context, text, duration).show();
        }
    }

    /**
     * Displays a toast with the given parameters, uses the default long duration.
     *
     * @param context context to display the toast
     * @param text a string containing the text that needs to be displayed
     */
    public static void displayToast(Context context, String text) {
        displayToast(context, text, Toast.LENGTH_LONG);
    }

    /**
     * Displays a toast with the given parameters, uses the default long duration.
     *
     * @param context context to display the toast
     * @param resource id of the string that needs to be displayed
     */
    public static void displayToast(Context context, int resource) {
        displayToast(context, context.getString(resource), Toast.LENGTH_LONG);
    }

    /**
     * Converts the answers of a SubmittingQuestionnaire to a string that can be submitted.
     *
     * @param context needed for the shared preferences
     * @param submittingQuestionnaire contains the answers to a questionnaire
     * @return a string containing the answers to a questionnaire, suitable for submitting
     * @see SubmittingQuestionnaire
     */
    public static String serializeAnswers(Context context, SubmittingQuestionnaire submittingQuestionnaire) {

        SubmittingQuestion submittingQuestion;
        SubmittingAnswer submittingAnswer;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {

            JSONObject jObj = new JSONObject();

            JSONObject jData = new JSONObject();

            jData.put("questionnaire_id", submittingQuestionnaire.getId());
            jData.put("user_id", submittingQuestionnaire.getForUserId());

            if (submittingQuestionnaire.getPreviousSubmissionId() != 0){
                jData.put("prev_submission_id", submittingQuestionnaire.getPreviousSubmissionId());
            }

            jData.put("started_at", sdf.format(submittingQuestionnaire.getStartMillis()));
            jData.put("finished_at", sdf.format(submittingQuestionnaire.getStopMillis()));
            jData.put("created_by", submittingQuestionnaire.getByUserId());

            JSONArray jAnswerArray = new JSONArray();

            ArrayList<SubmittingQuestion> submittingQuestionArrayList = submittingQuestionnaire.getSubmittingQuestionsList();

            for (int i = 0; i < submittingQuestionArrayList.size(); i++){

                submittingQuestion = submittingQuestionArrayList.get(i);

                ArrayList<SubmittingAnswer> submittingAnswerArrayList = submittingQuestion.getSubmittingAnswerList();

                for (int j = 0; j < submittingAnswerArrayList.size(); j++){

                    submittingAnswer = submittingAnswerArrayList.get(j);

                    JSONObject jAnswer = new JSONObject();

                    jAnswer.put("question_id", submittingQuestion.getId());
                    jAnswer.put("answer_id", submittingAnswer.getId());

                    switch (submittingAnswer.getAnswerType()){
                        case 0: // Fixed
                            break;

                        case 1: // Integer
                            jAnswer.put("value", submittingAnswer.getIntValue());
                            break;

                        case 2: // Double
                            jAnswer.put("value", submittingAnswer.getDoubleValue());
                            break;

                        case 3: // String
                            jAnswer.put("value", submittingAnswer.getStringValue());
                            break;

                        default:
                    }

                    jAnswerArray.put(jAnswer);
                }
            }

            jData.put("answers", jAnswerArray);

            jObj.put("data", jData);

            MyLog.d(TAG, "" + jObj.toString());

            return jObj.toString();

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        return null;
    }

    /**
     * Creates an MD5 hash based on the strings provided
     *
     * @param s one or multiple strings to calculate the MD5 hash from
     * @return a string with the MD5 hash
     * @throws NoSuchAlgorithmException an exception thrown when the MD5 hash is unavailable
     */
    public static String md5(String... s) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.reset();
        boolean nfirst = false;
        StringBuilder res = new StringBuilder();
        for (String str : s) {
            if (str != null) {
                if (nfirst)
                    res.append(":");
                else
                    nfirst = true;

                res.append(str);
            }
        }
//        MyLog.d(TAG, res.toString());
        res = new StringBuilder(new BigInteger(1, digest.digest(res.toString().getBytes())).toString(16));
//        MyLog.d(TAG, res.toString());
        return res.toString();
    }

    /**
     * Converts headers in a string to a Map.
     *
     * @param header a string containing the headers
     * @return a map containg the headers from the string
     */
    static Map<String,String> parseHeaderToMap(String header) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        header = header.substring(7);
        String[] elements = header.split("\",");
        for (String element : elements){
            String[] parts = element.split("=");
            if (parts[1].substring(parts[1].length() - 1).equals("\"")){
                parts[1] = parts[1].substring(0, parts[1].length() - 1);
            }
            if (parts[1].substring(0, 1).equals("\"")){
                parts[1] = parts[1].substring(1);
            }
            map.put(parts[0].trim(), parts[1].trim());
        }

        MyLog.d(TAG, "parseHeaderToMap: " + map);

        return map;
    }

    /**
     * Converts an Instrument to a string that can be submitted.
     *
     * @param context needed to display the toast
     * @param instrument the Instrument that needs to be serialized
     * @return a string containing the serialized Instrument
     * @see Instrument
     */
    public static String serializeInstrument(Context context, Instrument instrument) {

        try {
            JSONObject jObj = new JSONObject();

            JSONObject jInstrument = new JSONObject();

            JSONArray jParametersArray = new JSONArray();

            for (int i = 0; i < instrument.getParameterArrayList().size(); i++){

                JSONObject jParameter = new JSONObject();

                jParameter.put("id", instrument.getParameterArrayList().get(i).getId());
                if (instrument.getParameterArrayList().get(i).getValue() != null){
                    jParameter.put("value", instrument.getParameterArrayList().get(i).getValue());
                }
                else {
                    jParameter.put("value", JSONObject.NULL);
                }

                jParametersArray.put(jParameter);
            }

            jInstrument.put("instrument_type_id", instrument.getInstrumentTypeId());
            jInstrument.put("setup_id", instrument.getSetupId());
            jInstrument.put("name_en", instrument.getName());
            jInstrument.put("description_en", instrument.getDescription());
            jInstrument.put("parameters", jParametersArray);

            jObj.put("data", jInstrument);

            return jObj.toString();

        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        return "";
    }

    /**
     * Checks whether there is an active network connection or not.
     *
     * @param context needed to get an instance from ConnectivityManager
     * @return a boolean indicating whether there is an active network connection or not
     */
    public static boolean checkNetworkConnection(Context context) {

        ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * An extra check to see if the server is reachable.
     *
     * @return a boolean indicating whether the server is reachable or not
     */
    public static boolean isOnline() {
        try {
            Process p1 = Runtime.getRuntime().exec("ping -c 1 www.nomadeproject.eu");
            int returnVal = p1.waitFor();
            return (returnVal==0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Character array used to convert bytes to hexadecimal representation.
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Converts bytes to the hexadecimal representation with spaces.
     *
     * @param bytes byte array to be converted
     * @return string with the hexadecimal representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        hexChars[hexChars.length - 1] = ' ';
        return new String(hexChars);
    }

    /**
     * Converts a byte to the hexadecimal representation.
     *
     * @param b byte to be converted
     * @return string with the hexadecimal representation of the byte
     */
    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        int v = b & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    /**
     * Converts a half of MAC address in a String to a float equivalent.
     *
     * @param mac String containing half of the MAC address
     * @return float equivalent of the half of the MAC address
     */
    public static float macToFloat(String mac) {
        MyLog.d(TAG, "macToFloat: MAC = " + mac);
        mac = "00" + mac;
        byte[] bytes = bytesFromHexString(mac);
        float f = floatFromByteArray(bytes);
        MyLog.d(TAG, "macToFloat: float = " + f);
        return f;
    }

    /**
     * Converts a float to the half of a MAC address.
     *
     * @param f float to be converted
     * @return String containing the half of a MAC address
     */
    public static String floatToMac (float f) {
        MyLog.d(TAG, "floatToMac: float = " + f);
        byte[] bytes = floatToByteArray(f);
        String s = bytesToHexString(bytes).substring(2);
        MyLog.d(TAG, "floatToMac: MAC = " + s);
        return s;
    }

    /**
     * Converts bytes to the hexadecimal representation without spaces.
     *
     * @param bytes byte array to be converted
     * @return string with the hexadecimal representation of the byte array
     */
    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts a hexadecimal String to a byte array
     *
     * @param s hexadecimal String to convert
     * @return byte array representing the hexadecimal string
     */
    public static byte[] bytesFromHexString(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    /**
     * Converts a short value to a byte array.
     *
     * @param value short to be converted
     * @return byte array that represents the short value
     */
    public static byte[] shortToByteArray(short value) {
        return  ByteBuffer.allocate(2).putShort(value).array();
    }

    /**
     * Converts a byte array to a short value.
     *
     * @param bytes byte array to be converted
     * @return the short value that was represented by the byte array
     */
    public static short shortFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }

    /**
     * Converts an integer value to a byte array.
     *
     * @param value integer to be converted
     * @return byte array that represents the integer value
     */
    public static byte[] intToByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    /**
     * Converts a byte array to an integer value.
     *
     * @param bytes byte array to be converted
     * @return the integer value that was represented by the byte array
     */
    public static int intFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Converts a long value to a byte array.
     *
     * @param value long to be converted
     * @return byte array that represents the long value
     */
    public static byte[] longToByteArray(long value) {
        return  ByteBuffer.allocate(8).putLong(value).array();
    }

    /**
     * Converts a byte array to an long value.
     *
     * @param bytes byte array to be converted
     * @return the long value that was represented by the byte array
     */
    public static long longFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    /**
     * Converts a float value to a byte array.
     *
     * @param value float to be converted
     * @return byte array that represents the float value
     */
    public static byte[] floatToByteArray(float value) {
        return  ByteBuffer.allocate(4).putFloat(value).array();
    }

    /**
     * Converts a byte array to a float value.
     *
     * @param bytes byte array to be converted
     * @return the float value that was represented by the byte array
     */
    public static float floatFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }

    /**
     * Converts a double value to a byte array.
     *
     * @param value double to be converted
     * @return byte array that represents the double value
     */
    public static byte[] doubleToByteArray(double value) {
        return  ByteBuffer.allocate(8).putDouble(value).array();
    }

    /**
     * Converts a byte array to a double value.
     *
     * @param bytes byte array to be converted
     * @return the double value that was represented by the byte array
     */
    public static float doubleFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }

    /**
     * Converts characters in a string to their corresponding ASCII representation in a byte array.
     *
     * @param str string to be converted
     * @return byte array containing the ASCII representation of the string
     */
    public static byte[] stringToBytesASCII(String str) {
        byte[] b = new byte[str.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) str.charAt(i);
        }
        return b;
    }

    /**
     * Restarts the app.
     *
     * @param context needed for the restart
     */
    public static void triggerRebirth(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra(KEY_RESTART_INTENT, nextIntent);
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }

        Runtime.getRuntime().exit(0);
    }

    /**
     * Creates a linear layout containing all the details of a specific component of a setup with
     * the setup as top level component.
     *
     * @param context needed to get the resources
     * @param inflater to inflate layouts
     * @param what options: SETUP, INSTRUMENT, TYPE, PARAMETER
     * @param setup of which the details of a component needs to be displayed
     * @param instrumentIndex index of the instrument, pass -1 if not needed
     * @param parameterIndex index of the parameter, pass -1 if not needed
     * @return a linear layout containing the details of a specific component of a setup
     */
    public static LinearLayout createSetupDetailsLinearLayout(Context context, LayoutInflater inflater, int what, Setup setup, int instrumentIndex, int parameterIndex){

        LinearLayout detailsLinearLayout = new LinearLayout(context);
        detailsLinearLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        detailsLinearLayout.setPadding(context.getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), context.getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), context.getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar), context.getResources().getDimensionPixelOffset(R.dimen.padding_seek_bar));

        ArrayList<Integer> keysArrayList;
        ArrayList<String> valuesArrayList;

        switch (what){
            case Constants.SETUP:
                keysArrayList = Utilities.createSetupKeysArrayList();
                valuesArrayList = Utilities.createSetupValuesArrayList(setup);
                break;

            case Constants.INSTRUMENT:
                keysArrayList = Utilities.createInstrumentKeysArrayList();
                valuesArrayList = Utilities.createInstrumentValuesArrayList(setup.getInstrumentArrayList().get(instrumentIndex));
                break;

            case Constants.TYPE:
                keysArrayList = Utilities.createTypeKeysArrayList();
                valuesArrayList = Utilities.createTypeValuesArrayList(setup.getInstrumentArrayList().get(instrumentIndex).getType());
                break;

            case Constants.PARAMETER:
                keysArrayList = Utilities.createParameterKeysArrayList(true);
                valuesArrayList = Utilities.createParameterValuesArrayList(setup.getInstrumentArrayList().get(instrumentIndex).getParameterArrayList().get(parameterIndex), true);
                break;

            default:
                keysArrayList = new ArrayList<>();
                valuesArrayList = new ArrayList<>();
        }

        for (int i = 0; i < keysArrayList.size(); i++){

            View keyValuePairView = inflater.inflate(R.layout.linear_layout_key_value_pair, null);

            ((TextView) keyValuePairView.findViewById(R.id.key_text_view)).setText(keysArrayList.get(i));
            ((TextView) keyValuePairView.findViewById(R.id.value_text_view)).setText(valuesArrayList.get(i));

            detailsLinearLayout.addView(keyValuePairView);
        }

        return detailsLinearLayout;
    }

    /**
     * Creates an array list of integers containing the keys for the details for a setup.
     *
     * @return array list of integers containing the keys for the details for a setup
     */
    private static ArrayList<Integer> createSetupKeysArrayList(){
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(R.string.setup);
        arrayList.add(R.string.id_colon);
        arrayList.add(R.string.name_colon);
        arrayList.add(R.string.hardware_identifier_colon);
        arrayList.add(R.string.version_colon);
        arrayList.add(R.string.locked_colon);
        return arrayList;
    }

    /**
     * Creates an array list of strings containing the values for the details for a setup.
     *
     * @param setup of which the details needs to be extracted
     * @return array list of strings containing the values for the details for a setup
     */
    private static ArrayList<String> createSetupValuesArrayList(Setup setup){
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("");
        arrayList.add(String.format("%s", setup.getId()));
        arrayList.add(String.format("%s", setup.getName()));
        arrayList.add(String.format("%s", setup.getHardwareIdentifier()));
        arrayList.add(String.format("%s", setup.getVersion()));
        arrayList.add(String.format("%s", setup.isLocked()));
        return arrayList;
    }

    /**
     * Creates an array list of integers containing the keys for the details for an instrument.
     *
     * @return array list of integers containing the keys for the details for an instrument
     */
    private static ArrayList<Integer> createInstrumentKeysArrayList(){
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(R.string.instrument);
        arrayList.add(R.string.id_colon);
        arrayList.add(R.string.name_colon);
        arrayList.add(R.string.description_colon);
        arrayList.add(R.string.locked_colon);
        arrayList.add(R.string.type_id_colon);
        arrayList.add(R.string.setup_id_colon);
        return arrayList;
    }

    /**
     * Creates an array list of strings containing the values for the details for an instrument.
     *
     * @param instrument of which the details needs to be extracted
     * @return array list of strings containing the values for the details for an instrument
     */
    private static ArrayList<String> createInstrumentValuesArrayList(Instrument instrument){
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("");
        arrayList.add(String.format("%s", instrument.getId()));
        arrayList.add(String.format("%s", instrument.getName()));
        arrayList.add(String.format("%s", instrument.getDescription()));
        arrayList.add(String.format("%s", instrument.isLocked()));
        arrayList.add(String.format("%s", instrument.getInstrumentTypeId()));
        arrayList.add(String.format("%s", instrument.getSetupId()));
        return arrayList;
    }

    /**
     * Creates an array list of integers containing the keys for the details for a type.
     *
     * @return array list of integers containing the keys for the details for a type
     */
    private static ArrayList<Integer> createTypeKeysArrayList(){
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(R.string.type);
        arrayList.add(R.string.id_colon);
        arrayList.add(R.string.name_colon);
        arrayList.add(R.string.category_colon);
        arrayList.add(R.string.description_colon);
        return arrayList;
    }

    /**
     * Creates an array list of strings containing the values for the details for a type.
     *
     * @param type of which the details needs to be extracted
     * @return array list of strings containing the values for the details for a type
     */
    private static ArrayList<String> createTypeValuesArrayList(Type type){
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("");
        arrayList.add(String.format("%s", type.getId()));
        arrayList.add(String.format("%s", type.getName()));
        arrayList.add(String.format("%s", type.getCategory()));
        arrayList.add(String.format("%s", type.getDescription()));
        return arrayList;
    }

    /**
     * Creates an array list of integers containing the keys for the details for a parameter.
     *
     * @return array list of integers containing the keys for the details for a parameter
     */
    private static ArrayList<Integer> createParameterKeysArrayList(boolean showValueDescription){
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(R.string.parameter);
        arrayList.add(R.string.id_colon);
        arrayList.add(R.string.name_colon);
        arrayList.add(R.string.description_colon);
        arrayList.add(R.string.value_colon);
        if (showValueDescription){
            arrayList.add(R.string.value_description_colon);
        }
        arrayList.add(R.string.level_colon);
        arrayList.add(R.string.data_type_colon);
        arrayList.add(R.string.main_board_colon);
        arrayList.add(R.string.minimum_colon);
        arrayList.add(R.string.maximum_colon);
        arrayList.add(R.string.default_value_colon);
        return arrayList;
    }

    /**
     * Creates an array list of strings containing the values for the details for a parameter.
     *
     * @param parameter of which the details needs to be extracted
     * @return array list of strings containing the values for the details for a parameter
     */
    private static ArrayList<String> createParameterValuesArrayList(Parameter parameter, boolean showValueDescription){
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("");
        arrayList.add(String.format("%s", parameter.getId()));
        arrayList.add(String.format("%s", parameter.getName()));
        arrayList.add(String.format("%s", parameter.getDescription()));
        arrayList.add(String.format("%s", parameter.getValue()));
        if (showValueDescription){
            arrayList.add(String.format("%s", parameter.getValueDescription()));
        }
        arrayList.add(String.format("%s", parameter.getLevel()));
        arrayList.add(String.format("%s", parameter.getDataType()));
        arrayList.add(String.format("%s", parameter.getMainBoard()));
        arrayList.add(String.format("%s", parameter.getMin()));
        arrayList.add(String.format("%s", parameter.getMax()));
        arrayList.add(String.format("%s", parameter.getDefaultValue()));
        return arrayList;
    }

    /**
     * Concatenates two byte arrays into one.
     *
     * @param src1 first byte array
     * @param src2 second byte array
     * @return concatenated byte array
     */
    public static byte[] concatenateArrays(byte[] src1, byte[] src2){
        if (src1 == null){
            throw new IllegalArgumentException("src1 is required");
        }
        if (src2 == null){
            throw new IllegalArgumentException("src2 is required");
        }

        byte[] result = new byte[src1.length + src2.length];

        System.arraycopy(src1, 0, result, 0, src1.length);
        System.arraycopy(src2, 0, result, src1.length, src2.length);

        return result;
    }

    /**
     * Creates a deep copy of the given Setup.
     *
     * @param setup of which the deep copy needs to be made
     * @return deepcopy of the setup
     */
    public static Setup deepCopy(Setup setup){
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(setup), Setup.class);
    }

    /**
     * Parses a JSON containing a setup.
     *
     * @param context to display the toast
     * @param jsonSetup JSON containing a setup
     * @return the parsed setup
     */
    public static Setup parseJsonSetup(Context context, String jsonSetup, String jsonTypeInfoList, String jsonParameterInfoList) {
        if (jsonSetup == null || jsonSetup.equals("")){
            MyLog.e(TAG, "parseJsonSetup: jsonSetup is null or empty");
            return null;
        }

        if (jsonTypeInfoList == null || jsonTypeInfoList.equals("")){
            MyLog.e(TAG, "parseJsonSetup: jsonTypeInfoList is null or empty");
            return null;
        }

        if (jsonParameterInfoList == null || jsonParameterInfoList.equals("")){
            MyLog.e(TAG, "parseJsonSetup: jsonParameterInfoList is null or empty");
            return null;
        }

        ArrayList<Type> typeInfoArrayList = parseJsonTypeList(context, jsonTypeInfoList);
        if (typeInfoArrayList == null){
            MyLog.e(TAG, "parseJsonSetup: typeInfoArrayList is null");
            return null;
        }

        ArrayList<Parameter> parameterInfoArrayList = parseJsonParameterList(context, jsonParameterInfoList);
        if (parameterInfoArrayList == null){
            MyLog.e(TAG, "parseJsonSetup: parameterInfoArrayList is null");
            return null;
        }

        try {
            JSONObject jObj = new JSONObject(jsonSetup);

            JSONObject jSetup = jObj.getJSONObject("data");

            int setupId = jSetup.getInt("id");
            int setupGroupId = jSetup.getInt("setup_group_id");

            String setupNameEn = jSetup.getString("name_en");
            String setupNameNl = jSetup.getString("name_nl");
            String setupNameFr = jSetup.getString("name_fr");
            String setupDescriptionEn = jSetup.getString("description_en");
            String setupDescriptionNl = jSetup.getString("description_nl");
            String setupDescriptionFr = jSetup.getString("description_fr");
            String setupName;
            String setupDescription;
            switch (Locale.getDefault().getLanguage()){
                case "nl":
                    if (!setupNameNl.equals("null")) {
                        setupName = setupNameNl;
                        setupDescription = setupDescriptionNl;
                    }
                    else {
                        setupName = setupNameEn;
                        setupDescription = setupDescriptionEn;
                    }
                    break;
                case "fr":
                    if (!setupNameNl.equals("null")) {
                        setupName = setupNameFr;
                        setupDescription = setupDescriptionFr;
                    }
                    else {
                        setupName = setupNameEn;
                        setupDescription = setupDescriptionEn;
                    }
                    break;
                default:
                    setupName = setupNameEn;
                    setupDescription = setupDescriptionEn;
            }

            int setupHardwareIdentifier = jSetup.getInt("hw_identifier");
            int setupVersion = jSetup.getInt("version");
            boolean setupLocked = jSetup.getInt("locked") == 1;

            JSONArray jInstrumentArray = jSetup.getJSONArray("instruments");

            final ArrayList<Instrument> instrumentArrayList = new ArrayList<>();

            for (int i = 0; i < jInstrumentArray.length(); i++) {

                JSONObject jCurrentInstrument = jInstrumentArray.getJSONObject(i);

                int instrumentId = jCurrentInstrument.getInt("id");
                int instrumentTypeId = jCurrentInstrument.getInt("instrument_type_id");
                String instrumentName = jCurrentInstrument.getString("name_en");
                String instrumentDescription = jCurrentInstrument.getString("description_en");

                int typeId = -1;
                String typeName = "";
                String typeCategory = "";
                String typeDescription = "";

                for (Type type : typeInfoArrayList){
                    if (type.getId() == instrumentTypeId){
                        typeId = type.getId();
                        typeName = type.getName();
                        typeCategory = type.getCategory();
                        typeDescription = type.getDescription();

                        break;
                    }
                }

                JSONArray jParameterArray = jCurrentInstrument.getJSONArray("parameters");

                final ArrayList<Parameter> parameterArrayList = new ArrayList<>();

                for (int j = 0; j < jParameterArray.length(); j++) {

                    JSONObject jCurrentParameter = jParameterArray.getJSONObject(j);

                    int parameterId = jCurrentParameter.getInt("id");

                    Float parameterValue;
                    try {
                        parameterValue = BigDecimal.valueOf(jCurrentParameter.getDouble("value")).floatValue();
                    }
                    catch (JSONException e){
                        parameterValue = null;
                    }

                    Float parameterMin;
                    try {
                        parameterMin = BigDecimal.valueOf(jCurrentParameter.getDouble("min")).floatValue();
                    }
                    catch (JSONException e){
                        parameterMin = null;
                    }

                    Float parameterMax;
                    try {
                        parameterMax = BigDecimal.valueOf(jCurrentParameter.getDouble("max")).floatValue();
                    }
                    catch (JSONException e){
                        parameterMax = null;
                    }

                    String parameterUuid = "";
                    String parameterName = "";
                    String parameterDescription = "";
                    Integer parameterLevel = null;
                    Integer parameterDataType = null;
                    Boolean parameterMainBoard = null;
                    Float parameterDefaultValue = null;
                    ArrayList<ParameterOption> fullParameterOptionArrayList = new ArrayList<>();

                    for (Parameter parameterInfo : parameterInfoArrayList)
                    {
                        if (parameterInfo.getId() == parameterId)
                        {
                            parameterUuid = parameterInfo.getUuid();
                            parameterName = parameterInfo.getName();
                            parameterDescription = parameterInfo.getDescription();
                            parameterLevel = parameterInfo.getLevel();
                            parameterDataType = parameterInfo.getDataType();
                            parameterMainBoard = parameterInfo.getMainBoard();
                            parameterDefaultValue = parameterInfo.getDefaultValue();
                            fullParameterOptionArrayList.addAll(parameterInfo.getParameterOptionArrayList());

                            break;
                        }
                    }

                    String parameterValueDescription = "";

                    ArrayList<ParameterOption> parameterOptionArrayList = new ArrayList<>();

                    for (ParameterOption parameterOption : fullParameterOptionArrayList){
                        if ((parameterMin == null || parameterOption.getValue() >= parameterMin) && (parameterMax == null || parameterOption.getValue() <= parameterMax)){
                            parameterOptionArrayList.add(parameterOption);
                        }

                        if (parameterOption.getValue().equals(parameterValue)){
                            parameterValueDescription = parameterOption.getName();
                        }
                    }

                    // add the new parameter to the parameter array list
                    parameterArrayList.add(new Parameter(parameterId, parameterUuid, parameterName, parameterDescription, parameterLevel, parameterDataType, parameterMainBoard, parameterMin, parameterMax, parameterDefaultValue, parameterValue, parameterValueDescription, parameterOptionArrayList));
                }

                Type type = new Type(typeId, typeName, typeCategory, typeDescription);

                instrumentArrayList.add(new Instrument(instrumentId, instrumentTypeId, instrumentName, instrumentDescription, setupLocked, setupId, type, parameterArrayList));

            }

            // sets the obsolete boolean to true if the setup contains an obsolete output datatype
            boolean obsolete = setupId == 1 || setupId == 2 || setupId == 5 || setupId == 8 ||
                    setupId == 10 || setupId == 11 || setupId == 12 || setupId == 14 ||
                    setupId == 15 || setupId == 16 || setupId == 17 || setupId == 18 ||
                    setupId == 19 || setupId == 20 || setupId == 21 || setupId == 22 ||
                    setupId == 23 || setupId == 24 || setupId == 25 || setupId == 35 ||
                    setupId == 55 || setupId == 56 || setupId == 57 || setupId == 58 ||
                    setupId == 59 || setupId == 60 || setupId == 61 || setupId == 62;

            return new Setup(setupId, setupGroupId, setupName, setupHardwareIdentifier, setupVersion, setupLocked, instrumentArrayList, obsolete);

        } catch (JSONException e) {
            e.printStackTrace();
            MyLog.e(TAG, "JSONException Error: " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.getMessage());
            return null;
        }

    }

    public static ArrayList<Type> parseJsonTypeList(Context context, String jsonTypeList) {
        if (jsonTypeList == null || jsonTypeList.equals("")){
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonTypeList);

            JSONArray jTypeArray = jsonObject.getJSONArray("data");

            ArrayList<Type> typeArrayList = new ArrayList<>();

            for (int i = 0; i < jTypeArray.length(); i++) {

                JSONObject jCurrentType = jTypeArray.getJSONObject(i);

                int id = jCurrentType.getInt("id");
                String name = jCurrentType.getString("name_en");
                String category = jCurrentType.getString("description_en");
                String description = jCurrentType.getString("description_en");

                typeArrayList.add(new Type(id, name, category, description));
            }

            return typeArrayList;

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<Parameter> parseJsonParameterList(Context context, String jsonParameterList) {
        if (jsonParameterList == null || jsonParameterList.equals("")){
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonParameterList);

            JSONArray jParameterArray = jsonObject.getJSONArray("data");

            ArrayList<Parameter> parameterArrayList = new ArrayList<>();

            for (int i = 0; i < jParameterArray.length(); i++) {

                JSONObject jCurrentParameter = jParameterArray.getJSONObject(i);

                int id = jCurrentParameter.getInt("id");
                String uuid = jCurrentParameter.getString("uuid");
                String name = jCurrentParameter.getString("name_en");
                String description = jCurrentParameter.getString("description_en");

                Integer level;
                try {
                    level = jCurrentParameter.getInt("level");
                }
                catch (JSONException e){
                    level = null;
                }

                Integer datatype;
                try {
                    datatype = jCurrentParameter.getInt("datatype");
                }
                catch (JSONException e){
                    datatype = null;
                }

                Boolean mainboard = jCurrentParameter.getInt("mainboard") == 1;

                Float min;
                try {
                    min = BigDecimal.valueOf(jCurrentParameter.getDouble("min")).floatValue();
                }
                catch (JSONException e){
                    min = null;
                }

                Float max;
                try {
                    max = BigDecimal.valueOf(jCurrentParameter.getDouble("max")).floatValue();
                }
                catch (JSONException e){
                    max = null;
                }

                Float defaultValue;
                try {
                    defaultValue = BigDecimal.valueOf(jCurrentParameter.getDouble("default")).floatValue();
                }
                catch (JSONException e){
                    defaultValue = null;
                }

                ArrayList<ParameterOption> parameterOptionArrayList = new ArrayList<>();

                if (jCurrentParameter.has("values")){

                    JSONArray jParameterOptionArray = jCurrentParameter.getJSONArray("values");

                    for (int j = 0; j < jParameterOptionArray.length(); j++) {

                        JSONObject jCurrentParameterOption = jParameterOptionArray.getJSONObject(j);

                        Float parameterOptionValue;
                        try {
                            parameterOptionValue = BigDecimal.valueOf(jCurrentParameterOption.getDouble("value")).floatValue();
                        }
                        catch (JSONException e){
                            parameterOptionValue = null;
                        }

                        String parameterOptionName = jCurrentParameterOption.getString("name_en");

                        parameterOptionArrayList.add(new ParameterOption(parameterOptionValue, parameterOptionName));
                    }

                }

//                MyLog.d(TAG, "parseJsonParameterList: parameter id = " + id + " | parameterOptionArrayList size = " + parameterOptionArrayList.size());

                parameterArrayList.add(new Parameter(id, uuid, name, description, level, datatype, mainboard, min, max, defaultValue, parameterOptionArrayList));
            }

            return parameterArrayList;

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Converts the argument to an {@code int} by an unsigned
     * conversion.  In an unsigned conversion to an {@code int}, the
     * high-order 24 bits of the {@code int} are zero and the
     * low-order 8 bits are equal to the bits of the {@code byte} argument.
     *
     * Consequently, zero and positive {@code byte} values are mapped
     * to a numerically equal {@code int} value and negative {@code
     * byte} values are mapped to an {@code int} value equal to the
     * input plus 2<sup>8</sup>.
     *
     * @param  x the value to convert to an unsigned {@code int}
     * @return the argument converted to {@code int} by an unsigned
     *         conversion
     * @since 1.8
     */
    public static int byteToUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    /**
     * Converts the argument to an {@code int} by an unsigned
     * conversion.  In an unsigned conversion to an {@code int}, the
     * high-order 16 bits of the {@code int} are zero and the
     * low-order 16 bits are equal to the bits of the {@code short} argument.
     *
     * Consequently, zero and positive {@code short} values are mapped
     * to a numerically equal {@code int} value and negative {@code
     * short} values are mapped to an {@code int} value equal to the
     * input plus 2<sup>16</sup>.
     *
     * @param  x the value to convert to an unsigned {@code int}
     * @return the argument converted to {@code int} by an unsigned
     *         conversion
     * @since 1.8
     */
    public static int shortToUnsignedInt(short x) {
        return ((int) x) & 0xffff;
    }

    public static Setup generateStreamSetup(Setup setup){

        Instrument instrument;
        Parameter parameter;

        int valueIndex = 1;

        for (int i = 0; i < setup.getInstrumentArrayList().size(); i++){

            instrument = setup.getInstrumentArrayList().get(i);

            int outputDataType = 0;
            ArrayList<Variable> variableArrayList = new ArrayList<>();
            int indexOffset = 0;

            Float locationX = null;
            Float locationY = null;
            Float rotation = null;

            for (int j = 0; j < instrument.getParameterArrayList().size(); j++){

                parameter = instrument.getParameterArrayList().get(j);

                switch (parameter.getId()){

                    case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE:

                        if (parameter.getValue() != null) {

                            switch (Float.floatToIntBits(parameter.getValue())){

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1;
                                    variableArrayList.add(new Variable("Actual speed", "mm/s", 1f, Variable.SHORT, 4, valueIndex++));
                                    variableArrayList.add(new Variable("Turn", "?", 1f, Variable.BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("Speed", "?", 1f, Variable.BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("Profile", "?", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 6;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2;
                                    variableArrayList.add(new Variable("Actual speed", "mm/s", 1f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("Turn", "?", 1f, Variable.BYTE, 4, valueIndex++));
                                    variableArrayList.add(new Variable("Speed", "?", 1f, Variable.BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("Profile", "?", 1f, Variable.BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("Mode", "?", 1f, Variable.BYTE, 1, valueIndex++));
                                    indexOffset = 7;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3;
                                    variableArrayList.add(new Variable("Actual speed", "mm/s", 1f, Variable.SHORT, 4, valueIndex++));
                                    variableArrayList.add(new Variable("Turn", "?", 1f, Variable.BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("Speed", "?", 1f, Variable.BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("Profile", "?", 1f, Variable.BYTE, 1, valueIndex++));
                                    indexOffset = 6;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1;
                                    variableArrayList.add(new Variable("ax", "cm/s²", 1f, Variable.SHORT, 25, valueIndex++));
                                    variableArrayList.add(new Variable("ay", "cm/s²", 1f, Variable.SHORT, 23, valueIndex++));
                                    variableArrayList.add(new Variable("az", "cm/s²", 1f, Variable.SHORT, 21, valueIndex++));
                                    variableArrayList.add(new Variable("gx", "0,01 degrees/s", 1f, Variable.SHORT, 19, valueIndex++));
                                    variableArrayList.add(new Variable("gy", "0,01 degrees/s", 1f, Variable.SHORT, 17, valueIndex++));
                                    variableArrayList.add(new Variable("gz", "0,01 degrees/s", 1f, Variable.SHORT, 15, valueIndex++));
                                    variableArrayList.add(new Variable("mx", "0,01 µT", 1f, Variable.SHORT, 13, valueIndex++));
                                    variableArrayList.add(new Variable("my", "0,01 µT", 1f, Variable.SHORT, 11, valueIndex++));
                                    variableArrayList.add(new Variable("mz", "0,01 µT", 1f, Variable.SHORT, 9, valueIndex++));
                                    variableArrayList.add(new Variable("real", "0,001", 1f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("i", "0,001", 1f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("j", "0,001", 1f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("k", "0,001", 1f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 27;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2;
                                    variableArrayList.add(new Variable("ax", "cm/s²", 1f, Variable.SHORT, 11, valueIndex++));
                                    variableArrayList.add(new Variable("az", "cm/s²", 1f, Variable.SHORT, 9, valueIndex++));
                                    variableArrayList.add(new Variable("ay", "cm/s²", 1f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("gx", "0,01 degrees/s", 1f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("gy", "0,01 degrees/s", 1f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("gz", "0,01 degrees/s", 1f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 13;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1;
                                    variableArrayList.add(new Variable("longitude", "degrees", 1f, Variable.FLOAT, 14, valueIndex++));
                                    variableArrayList.add(new Variable("latitude", "degrees", 1f, Variable.FLOAT, 10, valueIndex++));
                                    variableArrayList.add(new Variable("hMSL", "m", 1f, Variable.FLOAT, 6, valueIndex++));
                                    variableArrayList.add(new Variable("speed", "m/s", 1f, Variable.FLOAT, 2, valueIndex++));
                                    variableArrayList.add(new Variable("gnss", "", 1f, Variable.BYTE, 1, valueIndex++));
                                    indexOffset = 18;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2;

                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3;

                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US;
                                    variableArrayList.add(new Variable("US", "cm", 1f, Variable.UNSIGNED_SHORT, 1, valueIndex++));
                                    indexOffset = 3;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR;
                                    variableArrayList.add(new Variable("IR", "cm", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 2;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR;
                                    variableArrayList.add(new Variable("Calculated", "cm", 1f, Variable.UNSIGNED_SHORT, 4, valueIndex++));
                                    variableArrayList.add(new Variable("US", "cm", 1f, Variable.UNSIGNED_SHORT, 2, valueIndex++));
                                    variableArrayList.add(new Variable("IR", "cm", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 6;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR;
                                    variableArrayList.add(new Variable("Calculated", "cm", 1f, Variable.UNSIGNED_SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("US", "cm", 1f, Variable.UNSIGNED_SHORT,3, valueIndex++));
                                    variableArrayList.add(new Variable("IR 1", "cm", 1f, Variable.UNSIGNED_BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("IR 2", "cm", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 7;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR;
                                    variableArrayList.add(new Variable("Calculated", "cm", 1f, Variable.UNSIGNED_SHORT, 6, valueIndex++));
                                    variableArrayList.add(new Variable("US", "cm", 1f, Variable.UNSIGNED_SHORT, 4, valueIndex++));
                                    variableArrayList.add(new Variable("IR 1", "cm", 1f, Variable.UNSIGNED_BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("IR 2", "cm", 1f, Variable.UNSIGNED_BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("IR 3", "cm", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 8;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR;
                                    variableArrayList.add(new Variable("Calculated", "cm", 1f, Variable.UNSIGNED_SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("IR 1", "cm", 1f, Variable.UNSIGNED_BYTE, 4, valueIndex++));
                                    variableArrayList.add(new Variable("IR 2", "cm", 1f, Variable.UNSIGNED_BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("IR 3", "cm", 1f, Variable.UNSIGNED_BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("IR 4", "cm", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 7;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1;
                                    variableArrayList.add(new Variable("Unix_epoch_ms", "ms", 1f, Variable.LONG, 1, valueIndex++));
                                    indexOffset = 9;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1;
                                    variableArrayList.add(new Variable("OAS Calculated Value", "/", 1f, Variable.UNSIGNED_BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("OAS Booleans", "/", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 3;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2;
                                    variableArrayList.add(new Variable("OAS Calculated Value", "/", 1f, Variable.UNSIGNED_BYTE, 3, valueIndex++));
                                    variableArrayList.add(new Variable("OAS Booleans", "/", 1f, Variable.UNSIGNED_BYTE, 2, valueIndex++));
                                    variableArrayList.add(new Variable("Sensor Activate Bits", "/", 1f, Variable.UNSIGNED_BYTE, 1, valueIndex++));
                                    indexOffset = 4;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_QUAT_ONLY__0XB4:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_QUAT_ONLY__0XB4;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 9;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_0XB5:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_0XB5;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 19, valueIndex++));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 17, valueIndex++));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 15, valueIndex++));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 13, valueIndex++));
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 11, valueIndex++));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 9, valueIndex++));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 21;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_100Hz_0XB6:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_100Hz_0XB6;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 39, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 37, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 35, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 33, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 31, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 29, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 27, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 25, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 23, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 21, valueIndex++, true, 10));
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 19, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 17, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 15, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 13, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 11, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 9, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 7, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 5, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 3, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 1, valueIndex++, true, -1));
                                    indexOffset = 41;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT100HZ_0XB7:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT100HZ_0XB7;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 15, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 13, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 11, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 9, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 7, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 5, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 3, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 1, valueIndex++, true, -1));
                                    indexOffset = 17;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF_0XB8:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF_0XB8;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 9;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF100HZ_0XB9:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF100HZ_0XB9;
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 15, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 13, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 11, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 9, valueIndex++, true, 4));
                                    variableArrayList.add(new Variable("a", "real", 10000f, Variable.SHORT, 7, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("b", "i", 10000f, Variable.SHORT, 5, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("c", "j", 10000f, Variable.SHORT, 3, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("d", "k", 10000f, Variable.SHORT, 1, valueIndex++, true, -1));
                                    indexOffset = 17;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG_0XBA:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG_0XBA;
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 17, valueIndex++));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 15, valueIndex++));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 13, valueIndex++));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 11, valueIndex++));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 9, valueIndex++));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 7, valueIndex++));
                                    variableArrayList.add(new Variable("MAG X", "µT", 1f, Variable.SHORT, 5, valueIndex++));
                                    variableArrayList.add(new Variable("MAG Y", "µT", 1f, Variable.SHORT, 3, valueIndex++));
                                    variableArrayList.add(new Variable("MAG Z", "µT", 1f, Variable.SHORT, 1, valueIndex++));
                                    indexOffset = 19;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG100HZ_0XBB:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG100HZ_0XBB;
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 35, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 33, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 31, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 29, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 27, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 25, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("MAG X", "µT", 1f, Variable.SHORT, 23, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("MAG Y", "µT", 1f, Variable.SHORT, 21, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("MAG Z", "µT", 1f, Variable.SHORT, 19, valueIndex++, true, 9));
                                    variableArrayList.add(new Variable("GYRO X", "dps", 10f, Variable.SHORT, 17, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("GYRO Y", "dps", 10f, Variable.SHORT, 15, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("GYRO Z", "dps", 10f, Variable.SHORT, 13, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC X", "m/s²", 1000f, Variable.SHORT, 11, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC Y", "m/s²", 1000f, Variable.SHORT, 9, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("ACC Z", "m/s²", 1000f, Variable.SHORT, 7, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("MAG X", "µT", 1f, Variable.SHORT, 5, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("MAG Y", "µT", 1f, Variable.SHORT, 3, valueIndex++, true, -1));
                                    variableArrayList.add(new Variable("MAG Z", "µT", 1f, Variable.SHORT, 1, valueIndex++, true, -1));
                                    indexOffset = 37;
                                    break;

                                case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE:
                                default:
                                    outputDataType = (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE;
                                    break;
                            }

                        }

                        break;

                    case (int) Constants.SETUP_PRM_X:
                        locationX = parameter.getValue();
                        break;

                    case (int) Constants.SETUP_PRM_Y:
                        locationY = parameter.getValue();
                        break;

                    case (int) Constants.SETUP_PRM_R:
                        rotation = parameter.getValue();
                        break;

                }

            }

            instrument.setOutputDataType(outputDataType);
            instrument.setVariableArrayList(variableArrayList);
            instrument.setIndexOffset(indexOffset);


            instrument.setLocationX(locationX);
            instrument.setLocationY(locationY);
            instrument.setRotation(rotation);
        }

        ArrayList<Instrument> streamInstrumentArrayList = new ArrayList<>();

        for (int i = 0; i < setup.getInstrumentArrayList().size(); i++){

            instrument = setup.getInstrumentArrayList().get(i);

            if (instrument.getVariableArrayList().size() > 0){

                streamInstrumentArrayList.add(instrument);

            }

        }

        return new Setup(
                setup.getId(),
                setup.getGroupId(),
                setup.getName(),
                setup.getHardwareIdentifier(),
                setup.getVersion(),
                setup.isLocked(),
                streamInstrumentArrayList
        );
    }

    /**
     * Extracts the connection check from a JSON response from the server.
     *
     * @param json response from the server
     * @return true if the connection is ok
     *         false otherwise
     */
    public static boolean extractConnectionCheck(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            String connectionString = jData.getString("connection");
            return connectionString.equals("OK");
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSON Exception: " + e);
            return false;
        }
    }

    /**
     * Extracts an url from a JSON response from the server.
     *
     * @param json response from the server
     * @param urlToExtract determines which url to extract
     *                     ("current" or "update" or "development")
     * @return extracted url
     *         empty string if an error occurred
     */
    public static String extractUrl(String json, String urlToExtract) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            JSONObject jApi = jData.getJSONObject("api");
            JSONObject jUrl = jApi.getJSONObject("url");
            return jUrl.getString(urlToExtract);
        }
        catch (JSONException e){
            MyLog.w(TAG, "JSON Exception: " + e);
            return "";
        }
    }

    /**
     * Extracts a version from a JSON response from the server.
     *
     * @param json response from the server
     * @param versionToExtract determines which version to extract
     *                         ("minimum" or "latest")
     * @param appPackageName determines which package name to extract
     *                       ("com.nomade.android.nomadeapp")
     * @return extracted version
     *         empty string if an error occurred
     */
    public static String extractVersion(String json, String versionToExtract, String appPackageName) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            JSONObject jAppName = jData.getJSONObject(appPackageName);
            JSONObject jVersion = jAppName.getJSONObject("version");
            return jVersion.getString(versionToExtract);
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSON Exception: " + e);
            return "";
        }
    }

    /**
     * Extracts a notice from a JSON response from the server.
     *
     * @param json response from the server
     * @param appPackageName determines which package name to extract
     *                       ("com.nomade.android.nomadeapp")
     * @return extracted notice
     *         empty string if an error occurred
     */
    public static String extractNotice(String json, String appPackageName) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            JSONObject jAppName = jData.getJSONObject(appPackageName);
            return jAppName.getString("notice");
        }
        catch (JSONException e){
            MyLog.w(TAG, "JSON Exception: " + e);
            return "";
        }
    }

    /**
     * Extracts a general notice from a JSON response from the server.
     *
     * @param json response from the server
     * @return extracted general notice
     *         empty string if an error occurred
     */
    public static String extractGeneralNotice(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            JSONObject jData = jObj.getJSONObject("data");
            return jData.getString("notice");
        }
        catch (JSONException e){
            MyLog.w(TAG, "JSON Exception: " + e);
            return "";
        }
    }

    public static Object evaluateArgb(float fraction, Object startValue, Object endValue){
        int startInt = (Integer) startValue;
        float startA = ((startInt >> 24) & 0xff) / 255.0f;
        float startR = ((startInt >> 16) & 0xff) / 255.0f;
        float startG = ((startInt >> 8) & 0xff) / 255.0f;
        float startB = ((startInt) & 0xff) / 255.0f;

        int endInt = (Integer) endValue;
        float endA = ((endInt >> 24) & 0xff) / 255.0f;
        float endR = ((endInt >> 16) & 0xff) / 255.0f;
        float endG = ((endInt >> 8) & 0xff) / 255.0f;
        float endB = ((endInt) & 0xff) / 255.0f;

        // convert from sRGB to linear
        startR = (float) Math.pow(startR, 2.2);
        startG = (float) Math.pow(startG, 2.2);
        startB = (float) Math.pow(startB, 2.2);

        endR = (float) Math.pow(endR, 2.2);
        endG = (float) Math.pow(endG, 2.2);
        endB = (float) Math.pow(endB, 2.2);

        // compute the interpolated color in linear space
        float a = startA + fraction * (endA - startA);
        float r = startR + fraction * (endR - startR);
        float g = startG + fraction * (endG - startG);
        float b = startB + fraction * (endB - startB);

        // convert back to sRGB in the [0..255] range
        a = a * 255.0f;
        r = (float) Math.pow(r, 1.0 / 2.2) * 255.0f;
        g = (float) Math.pow(g, 1.0 / 2.2) * 255.0f;
        b = (float) Math.pow(b, 1.0 / 2.2) * 255.0f;

        return Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }

    /**
     * Converts a setup to raw format so it can be interpreted by the main board.
     *
     * @param setup to be converted
     * @return byte array containing the setup in raw format
     */
    public static byte[] convertSetupToRawData(Context context, Setup setup){

        MyLog.d(TAG, "convertSetupToRawData start");

        ArrayList<Instrument> instrumentArrayList = new ArrayList<>();

        // Calculating the needed size for the array

        for (Instrument instrument : setup.getInstrumentArrayList()){

            boolean appOnlyParameter = false;

            for (Parameter parameter : instrument.getParameterArrayList()){
                if (parameter.getId() == 268){

                    appOnlyParameter = true;

                    if (!parameter.getValue().equals(1f)){
                        instrumentArrayList.add(instrument);
                    }
                }
            }

            if (!appOnlyParameter){
                instrumentArrayList.add(instrument);
            }
        }

        int byteArraySize = 0;
        int[] parameterCounts = new int[instrumentArrayList.size()];
        byteArraySize += 10;
        int i = 0;
        for (Instrument instrument : instrumentArrayList){
            byteArraySize += 4;
            parameterCounts[i] = 0;
            for (Parameter parameter : instrument.getParameterArrayList()){
                if (parameter.getMainBoard()){
                    byteArraySize += 6;
                    parameterCounts[i]++;
                }
            }
            i++;
        }
        byteArraySize += 2;


        int index = 0;

        byte[] bytes = new byte[byteArraySize];

        // Start of heading (SOH)
        bytes[index++] = 0x01;

        // Setup ID
        bytes[index++] = (byte) ((setup.getId() >> 8) & 0xff);
        bytes[index++] = (byte) (setup.getId() & 0xff);

        // Version
        bytes[index++] = (byte) ((setup.getVersion() >> 8) & 0xff);
        bytes[index++] = (byte) (setup.getVersion() & 0xff);

        // TODO Company ID is removed!
        bytes[index++] = (byte) 0;
        bytes[index++] = (byte) 0;

        // Start of text (STX)
        bytes[index++] = 0x02;

        // Number of instruments
        bytes[index++] = (byte) ((instrumentArrayList.size() >> 8) & 0xff);
        bytes[index++] = (byte) (instrumentArrayList.size() & 0xff);

        i = 0;

        int nullValueCount = 0;

        // For every instrument in the setup
        for (Instrument instrument : instrumentArrayList){

//            MyLog.d(TAG, "Instrument: " + instrument.getName());

            // Instrument ID
            bytes[index++] = (byte) ((instrument.getId() >> 8) & 0xff);
            bytes[index++] = (byte) (instrument.getId() & 0xff);

            // Number of instruments
            bytes[index++] = (byte) ((parameterCounts[i] >> 8) & 0xff);
            bytes[index++] = (byte) (parameterCounts[i] & 0xff);

            // For every parameter in this instrument
            for (Parameter parameter : instrument.getParameterArrayList()){

//                MyLog.d(TAG, "Parameter: " + parameter.getName());

                // Checks if the parameter needs to be added to the raw data
                if (parameter.getMainBoard()){

                    // Parameter ID
                    bytes[index++] = (byte) ((parameter.getId() >> 8) & 0xff);
                    bytes[index++] = (byte) (parameter.getId() & 0xff);

                    // Parameter value
                    Float floatValue = parameter.getValue();
                    if (floatValue != null){
                        byte[] floatBytes = ByteBuffer.allocate(4).putFloat(floatValue).array();
                        bytes[index++] = floatBytes[0];
                        bytes[index++] = floatBytes[1];
                        bytes[index++] = floatBytes[2];
                        bytes[index++] = floatBytes[3];
                    }
                    else {
                        byte[] floatBytes = ByteBuffer.allocate(4).putFloat(0f).array();
                        bytes[index++] = floatBytes[0];
                        bytes[index++] = floatBytes[1];
                        bytes[index++] = floatBytes[2];
                        bytes[index++] = floatBytes[3];

                        nullValueCount++;
                    }
                }
            }
            i++;
        }

        if (nullValueCount > 0) {
            if (nullValueCount == 1) {
                Utilities.displayToast(context, "Warning: one parameter has a \"null\" value assigned, which is replaced with a zero value in the RAW setup");
            }
            else {
                Utilities.displayToast(context, "Warning: " + nullValueCount + " parameters have a \"null\" value assigned, which is replaced with a zero value in the RAW setup");
            }
        }

        // End of text (ETX)
        bytes[index++] = 0x03;

        // End of transmission (EOT)
        bytes[index] = 0x04;

        MyLog.d(TAG, "RAW: " + Utilities.bytesToHex(bytes));

        return bytes;
    }
}

