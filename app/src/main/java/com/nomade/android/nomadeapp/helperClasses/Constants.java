package com.nomade.android.nomadeapp.helperClasses;

/**
 * Constants
 *
 * Various constant values used in the app
 */
public class Constants {

    // URL's
    public static final String NOMADE_URL = "nomadeproject.eu";
    public static final String API_URL = "https://nomade.clouddb.myriade.be/api/";

    // added "_laravel" to make sure everyone is using the right url, independent from the shared preferences
    // change this to insert a new url
    // change setting name in preferences.xml
    public static final String SETTING_SERVER_API_URL = "setting_server_api_url_laravel_https";
    public static final String SERVER_API_IP = "server_api_ip";

    public static final String TIME_SERVER = "pool.ntp.org";

    // Permissions
    public static final String[] PERMISSIONS = new String[] {
            "permission.api.index",
            "permission.api.log.index",
            "permission.api.log.show",
            "permission.company.index",
            "permission.company.index.company",
            "permission.company.index.user",
            "permission.company.show",
            "permission.company.show.company",
            "permission.company.show.user",
            "permission.company.create",
            "permission.company.edit",
            "permission.debug.console",
            "permission.debug.log.show",
            "permission.debug.server.edit",
            "permission.debug.server.show",
            "permission.permission.create",
            "permission.permission.destroy",
            "permission.permission.index",
            "permission.permission.index.user",
            "permission.questionnaire.create",
            "permission.questionnaire.destroy",
            "permission.questionnaire.draft.arrange",
            "permission.questionnaire.draft.create",
            "permission.questionnaire.draft.delete",
            "permission.questionnaire.draft.duplicate",
            "permission.questionnaire.draft.edit",
            "permission.questionnaire.draft.show",
            "permission.questionnaire.edit",
            "permission.questionnaire.index",
            "permission.questionnaire.pdf.show",
            "permission.questionnaire.show",
            "permission.parameter.create",
            "permission.parameter.index",
            "permission.parameter.show",
            "permission.parameter.edit",
            "permission.questionnaireCategory.create",
            "permission.questionnaireCategory.index",
            "permission.questionnaireCategory.show",
            "permission.questionnaireCategory.edit",
            "permission.questionnaireGroup.create",
            "permission.questionnaireGroup.index",
            "permission.questionnaireGroup.show",
            "permission.questionnaireGroup.edit",
            "permission.measurement.create",
            "permission.measurement.index",
            "permission.measurement.show",
            "permission.measurement.edit",
            "permission.setup.create",
            "permission.setup.index",
            "permission.setup.index.company",
            "permission.setup.index.user",
            "permission.setup.show",
            "permission.setup.show.company",
            "permission.setup.show.user",
            "permission.setup.stream.index",
            "permission.setup.stream.index.company",
            "permission.setup.edit",
            "permission.instrument.create",
            "permission.instrument.index",
            "permission.instrument.show",
            "permission.instrument.edit",
            "permission.instrumentType.create",
            "permission.instrumentType.index",
            "permission.instrumentType.show",
            "permission.instrumentType.edit",
            "permission.submission.create",
            "permission.submission.edit",
            "permission.submission.edit.company",
            "permission.submission.edit.user",
            "permission.submission.index",
            "permission.submission.index.company",
            "permission.submission.index.user",
            "permission.submission.pdf.show",
            "permission.submission.pdf.show.company",
            "permission.submission.pdf.show.user",
            "permission.submission.show",
            "permission.submission.show.company",
            "permission.submission.show.user",
            "permission.user.create",
            "permission.user.create.company",
            "permission.user.destroy",
            "permission.user.destroy.company",
            "permission.user.edit",
            "permission.user.edit.company",
            "permission.user.index",
            "permission.user.index.company",
            "permission.user.permission.create",
            "permission.user.permission.edit",
            "permission.user.permission.index",
            "permission.user.permission.index.company",
            "permission.user.permission.index.user",
            "permission.user.permission.show",
            "permission.user.permission.show.company",
            "permission.user.permission.show.user",
            "permission.user.show",
            "permission.user.show.company",
            "permission.user.show.user",
            "permission.measurement.delete"
    };

    public static final String
            PERMISSION_API_INDEX =                      "permission.api.index",                     //
            PERMISSION_API_LOG_INDEX =                  "permission.api.log.index",                 //
            PERMISSION_API_LOG_SHOW =                   "permission.api.log.show",                  //
            PERMISSION_COMPANY_INDEX =                  "permission.company.index",                 //
            PERMISSION_COMPANY_INDEX_COMPANY =          "permission.company.index.company",         //
            PERMISSION_COMPANY_INDEX_USER =             "permission.company.index.user",            //
            PERMISSION_COMPANY_SHOW =                   "permission.company.show",                  //
            PERMISSION_COMPANY_SHOW_COMPANY =           "permission.company.show.company",          //
            PERMISSION_COMPANY_SHOW_USER =              "permission.company.show.user",             //
            PERMISSION_COMPANY_CREATE =                 "permission.company.create",                //
            PERMISSION_COMPANY_EDIT =                   "permission.company.edit",                  //
            PERMISSION_DEBUG_CONSOLE =                  "permission.debug.console",                 // Development Console
            PERMISSION_DEBUG_LOG_SHOW =                 "permission.debug.log.show",                //
            PERMISSION_DEBUG_SERVER_EDIT =              "permission.debug.server.edit",             //
            PERMISSION_DEBUG_SERVER_SHOW =              "permission.debug.server.show",             //
            PERMISSION_PERMISSION_CREATE =              "permission.permission.create",             // Give a user a permission
            PERMISSION_PERMISSION_DESTROY =             "permission.permission.destroy",            // Remove a permission from a user
            PERMISSION_PERMISSION_INDEX =               "permission.permission.index",              // View users own permissions
            PERMISSION_PERMISSION_INDEX_USER =          "permission.permission.index.user",         // View users own allowed permissions
            PERMISSION_QUESTIONNAIRE_CREATE =           "permission.questionnaire.create",          // Add Questionnaire
            PERMISSION_QUESTIONNAIRE_DESTROY =          "permission.questionnaire.destroy",         // Delete Questionnaire
            PERMISSION_QUESTIONNAIRE_DRAFT_ARRANGE =    "permission.questionnaire.draft.arrange",   //
            PERMISSION_QUESTIONNAIRE_DRAFT_CREATE =     "permission.questionnaire.draft.create",    // Submit a draft questionnaire
            PERMISSION_QUESTIONNAIRE_DRAFT_DELETE =     "permission.questionnaire.draft.delete",    //
            PERMISSION_QUESTIONNAIRE_DRAFT_DUPLICATE =  "permission.questionnaire.draft.duplicate", //
            PERMISSION_QUESTIONNAIRE_DRAFT_EDIT =       "permission.questionnaire.draft.edit",      //
            PERMISSION_QUESTIONNAIRE_DRAFT_SHOW =       "permission.questionnaire.draft.show",      // View the draft questionnaires
            PERMISSION_QUESTIONNAIRE_EDIT =             "permission.questionnaire.edit",            // Edit Questionnaire
            PERMISSION_QUESTIONNAIRE_INDEX =            "permission.questionnaire.index",           // View list of all questionnaires
            PERMISSION_QUESTIONNAIRE_PDF_SHOW =         "permission.questionnaire.pdf.show",        // Get pdf of questionnaires
            PERMISSION_QUESTIONNAIRE_SHOW =             "permission.questionnaire.show",            //
            PERMISSION_PARAMETER_CREATE =               "permission.parameter.create",              //
            PERMISSION_PARAMETER_INDEX =                "permission.parameter.index",               //
            PERMISSION_PARAMETER_SHOW =                 "permission.parameter.show",                //
            PERMISSION_PARAMETER_EDIT =                 "permission.parameter.edit",                //
            PERMISSION_QUESTIONNAIRE_CATEGORY_CREATE =  "permission.questionnaireCategory.create",  //
            PERMISSION_QUESTIONNAIRE_CATEGORY_INDEX =   "permission.questionnaireCategory.index",   //
            PERMISSION_QUESTIONNAIRE_CATEGORY_SHOW =    "permission.questionnaireCategory.show",    //
            PERMISSION_QUESTIONNAIRE_CATEGORY_EDIT =    "permission.questionnaireCategory.edit",    //
            PERMISSION_QUESTIONNAIRE_GROUP_CREATE =     "permission.questionnaireGroup.create",     //
            PERMISSION_QUESTIONNAIRE_GROUP_INDEX =      "permission.questionnaireGroup.index",      //
            PERMISSION_QUESTIONNAIRE_GROUP_SHOW =       "permission.questionnaireGroup.show",       //
            PERMISSION_QUESTIONNAIRE_GROUP_EDIT =       "permission.questionnaireGroup.edit",       //
            PERMISSION_MEASUREMENT_CREATE =             "permission.measurement.create",            //
            PERMISSION_MEASUREMENT_INDEX =              "permission.measurement.index",             //
            PERMISSION_MEASUREMENT_SHOW =               "permission.measurement.show",              //
            PERMISSION_MEASUREMENT_EDIT =               "permission.measurement.edit",              //
            PERMISSION_SETUP_CREATE =                   "permission.setup.create",                  //
            PERMISSION_SETUP_INDEX =                    "permission.setup.index",                   //
            PERMISSION_SETUP_INDEX_COMPANY =            "permission.setup.index.company",           //
            PERMISSION_SETUP_INDEX_USER =               "permission.setup.index.user",              //
            PERMISSION_SETUP_SHOW =                     "permission.setup.show",                    //
            PERMISSION_SETUP_SHOW_COMPANY =             "permission.setup.show.company",            //
            PERMISSION_SETUP_SHOW_USER =                "permission.setup.show.user",               //
            PERMISSION_SETUP_STREAM_INDEX =             "permission.setup.stream.index",            // List all current streams
            PERMISSION_SETUP_STREAM_INDEX_COMPANY =     "permission.setup.stream.index.company",    // List all streams by own company
            PERMISSION_SETUP_EDIT =                     "permission.setup.edit",                    //
            PERMISSION_INSTRUMENT_CREATE =              "permission.instrument.create",             //
            PERMISSION_INSTRUMENT_INDEX =               "permission.instrument.index",              //
            PERMISSION_INSTRUMENT_SHOW =                "permission.instrument.show",               //
            PERMISSION_INSTRUMENT_EDIT =                "permission.instrument.edit",               //
            PERMISSION_INSTRUMENT_TYPE_CREATE =         "permission.instrumentType.create",         //
            PERMISSION_INSTRUMENT_TYPE_INDEX =          "permission.instrumentType.index",          //
            PERMISSION_INSTRUMENT_TYPE_SHOW =           "permission.instrumentType.show",           //
            PERMISSION_INSTRUMENT_TYPE_EDIT =           "permission.instrumentType.edit",           //
            PERMISSION_SUBMISSION_CREATE =              "permission.submission.create",             //
            PERMISSION_SUBMISSION_EDIT =                "permission.submission.edit",               //
            PERMISSION_SUBMISSION_EDIT_COMPANY =        "permission.submission.edit.company",       //
            PERMISSION_SUBMISSION_EDIT_USER =           "permission.submission.edit.user",          //
            PERMISSION_SUBMISSION_INDEX =               "permission.submission.index",              // View the list of all submitted questionnaires
            PERMISSION_SUBMISSION_INDEX_COMPANY =       "permission.submission.index.company",      //
            PERMISSION_SUBMISSION_INDEX_USER =          "permission.submission.index.user",         // View the list of all own submitted questionnaires
            PERMISSION_SUBMISSION_PDF_SHOW =            "permission.submission.pdf.show",           //
            PERMISSION_SUBMISSION_PDF_SHOW_COMPANY =    "permission.submission.pdf.show.company",   //
            PERMISSION_SUBMISSION_PDF_SHOW_USER =       "permission.submission.pdf.show.user",      //
            PERMISSION_SUBMISSION_SHOW =                "permission.submission.show",               //
            PERMISSION_SUBMISSION_SHOW_COMPANY =        "permission.submission.show.company",       //
            PERMISSION_SUBMISSION_SHOW_USER =           "permission.submission.show.user",          //
            PERMISSION_USER_CREATE =                    "permission.user.create",                   // Add User
            PERMISSION_USER_CREATE_COMPANY =            "permission.user.create.company",           // Add users to own company
            PERMISSION_USER_DESTROY =                   "permission.user.destroy",                  // Delete User
            PERMISSION_USER_DESTROY_COMPANY =           "permission.user.destroy.company",          //
            PERMISSION_USER_EDIT =                      "permission.user.edit",                     // Edit User
            PERMISSION_USER_EDIT_COMPANY =              "permission.user.edit.company",             // Edit User from own company
            PERMISSION_USER_INDEX =                     "permission.user.index",                    // List all users
            PERMISSION_USER_INDEX_COMPANY =             "permission.user.index.company",            // List all users from own company
            PERMISSION_USER_PERMISSION_CREATE =         "permission.user.permission.create",        //
            PERMISSION_USER_PERMISSION_EDIT =           "permission.user.permission.edit",          //
            PERMISSION_USER_PERMISSION_INDEX =          "permission.user.permission.index",         //
            PERMISSION_USER_PERMISSION_INDEX_COMPANY =  "permission.user.permission.index.company", //
            PERMISSION_USER_PERMISSION_INDEX_USER =     "permission.user.permission.index.user",    //
            PERMISSION_USER_PERMISSION_SHOW =           "permission.user.permission.show",          //
            PERMISSION_USER_PERMISSION_SHOW_COMPANY =   "permission.user.permission.show.company",  //
            PERMISSION_USER_PERMISSION_SHOW_USER =      "permission.user.permission.show.user",     //
            PERMISSION_USER_SHOW =                      "permission.user.show",                     //
            PERMISSION_USER_SHOW_COMPANY =              "permission.user.show.company",             //
            PERMISSION_USER_SHOW_USER =                 "permission.user.show.user",                //
            PERMISSION_MEASUREMENT_DELETE =             "permission.measurement.delete";            //

    public static final String SETTING_ENABLE_LOGGING = "setting_enable_logging";
    public static final String SETTING_QNR_DRAFT_VIEW = "setting_qnr_draft_view";
    public static final String SETTING_QNR_DRAFT_SUBMIT = "setting_qnr_draft_submit";

    public static final String SETTING_OAS_BUZZER = "setting_oas_buzzer";
    public static final String SETTING_OAS_HAPTIC = "setting_oas_haptic";
    public static final String SETTING_OAS_VISUAL = "setting_oas_visual";
    public static final String SETTING_OAS_VOICE = "setting_oas_voice";

    public static final String SETTING_OAS_SENSOR_ = "setting_oas_sensor_";

    public static final String SETTING_SHOW_DETAILED_DATA = "setting_show_detailed_data";

    public static final String SETTING_AUTOMATIC_USB_COMMUNICATION = "setting_automatic_usb_communication";
    public static final String SETTING_ONLY_ENABLE_RELEVANT_BUTTONS = "setting_only_enable_relevant_buttons";

    public static final String SETTING_HIDE_UNCHANGEABLE_PARAMETERS = "setting_hide_unchangeable_parameters";

    public static final int MAX_LENGTH_OF_EDIT_TEXT = 50000;

    public static final String[] COUNTRIES = new String[]{
            "be",
            "fr",
            "nl",
            "uk"
    };

    // Setup parameters for instruments
    // http://nomade.clouddb.myriade.be/parameters/define/java
    // PARAMETER DEFINES
    public static final short SETUP_PRM_X = 0x0001; // x coordinate for view
    public static final short SETUP_PRM_Y = 0x0002; // y coordinate for view
    public static final short SETUP_PRM_R = 0x0003; // rotation for view
    public static final short SETUP_PRM_COMM_METHOD = 0x0004; // Communication Interface: Number of the interface
    public static final float SETUP_PRM_COMM_METHOD_option_UART_UNUSED_FOR_NOW = 0x3F800000; // UART (Unused for now)
    public static final float SETUP_PRM_COMM_METHOD_option_CAN_UNUSED_FOR_NOW = 0x40000000; // CAN (Unused for now)
    public static final float SETUP_PRM_COMM_METHOD_option_SPI_UNUSED_FOR_NOW = 0x40400000; // SPI (Unused for now)
    public static final float SETUP_PRM_COMM_METHOD_option_JOYSTICK_DYNAMIC_CONTROL = 0x41800000; // Joystick Dynamic Control
    public static final float SETUP_PRM_COMM_METHOD_option_JOYSTICK_PENNY__GILES = 0x41880000; // Joystick Penny &amp; Giles
    public static final float SETUP_PRM_COMM_METHOD_option_JOYSTICK_LINX = 0x41900000; // Joystick LINX
    public static final float SETUP_PRM_COMM_METHOD_option_IMU = 0x42000000; // IMU
    public static final float SETUP_PRM_COMM_METHOD_option_GPS = 0x42800000; // GPS
    public static final float SETUP_PRM_COMM_METHOD_option_USB_FOR_ADDRESSING_ANDROID_DEVICE_AS_INSTRUMENT = 0x42A00000; // USB (for addressing Android Device as Instrument)
    public static final float SETUP_PRM_COMM_METHOD_option_BLUETOOTH_FOR_ADDRESSING_ANDROID_DEVICE_AS_INSTRUMENT = 0x42B00000; // Bluetooth (for addressing Android Device as Instrument)
    public static final float SETUP_PRM_COMM_METHOD_option_CAN_DISTANCE_SENSOR = 0x42C00000; // CAN Distance Sensor
    public static final float SETUP_PRM_COMM_METHOD_option_REAL_TIME_CLOCK_RTC = 0x43000000; // Real Time Clock (RTC)
    public static final float SETUP_PRM_COMM_METHOD_option_BLUETOOTH = 0x40800000; // Bluetooth
    public static final short SETUP_PRM_COMM_METHOD_VERSION = 0x0005; // Version of the specific communication method
    public static final short SETUP_PRM_COMM_ADDR = 0x0006; // Communication Address within the Interface
    public static final short SETUP_PRM_COMM_FAIL_CONSEQUENCE = 0x0007; // What needs to happen if this instrument fails
    public static final float SETUP_PRM_COMM_FAIL_CONSEQUENCE_option_DO_NOTHING = 0x00000000; // Do nothing
    public static final float SETUP_PRM_COMM_FAIL_CONSEQUENCE_option_STOP_SOFTWARE_INSTRUMENTS = 0x3F800000; // Stop software instruments
    public static final float SETUP_PRM_COMM_FAIL_CONSEQUENCE_option_STOP_VISUALISATION = 0x40000000; // Stop visualisation
    public static final float SETUP_PRM_COMM_FAIL_CONSEQUENCE_option_STOP_MEASUREMENTS = 0x40400000; // Stop measurements
    public static final float SETUP_PRM_COMM_FAIL_CONSEQUENCE_option_POWER_CUT_OFF_IF_ALLOWED_BY_ETHICAL_COMMISION = 0x40800000; // Power cut off (if allowed by Ethical Commision)
    public static final short SETUP_PRM_SAMPLERATE = 0x000A; // Samplerate (1/s)
    public static final short SETUP_PRM_DATA_INPUT_BYTES = 0x0010; // Number of Input Bytes sent to the instrument
    public static final short SETUP_PRM_DATA_INPUT_DATATYPE = 0x0011; // Datatype of the input bytes
    public static final float SETUP_PRM_DATA_INPUT_DATATYPE_option_NONE = 0x00000000; // None
    public static final short SETUP_PRM_DATA_OUTPUT_BYTES = 0x0020; // Number of Outbut Bytes received from the instrument (No datatype)
    public static final short SETUP_PRM_DATA_OUTPUT_DATATYPE = 0x0021; // Datatype of the output bytes
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE = 0x00000000; // None
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1 = 0x43210000; // JOYSTICK_DX2_OUTPUT (0xA1)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2 = 0x43220000; // JOYSTICK_PG_OUTPUT (0xA2)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3 = 0x43230000; // JOYSTICK_LINX_OUTPUT (0xA3)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1 = 0x43310000; // IMU_9AXIS_ROT_VEC (0xB1)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2 = 0x43320000; // IMU_6AXIS (0xB2)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1 = 0x43410000; // GPS_MIN_DATA (0xC1)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2 = 0x43420000; // GPS_STATUS (0xC2)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3 = 0x43430000; // GPS_DATA_STATUS (0xC3)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US = 0x43510000; // CAN_DISTANCE_NODES D1 (0xD1) (US)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR = 0x43520000; // CAN_DISTANCE_NODES D2 (0xD2) (IR)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR = 0x43530000; // CAN_DISTANCE_NODES D3 (0xD3) (US+IR)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR = 0x43540000; // CAN_DISTANCE_NODES D4 (0xD4) (US+2IR)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR = 0x43550000; // CAN_DISTANCE_NODES D5 (0xD5) (US+3IR)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR = 0x43560000; // CAN_DISTANCE_NODES D6 (0xD6) (4IR)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D7_0XD7__4IR_ONLY_CALCULATED_VALUE = 0x43570000; // CAN_DISTANCE_NODES D7 (0xD7) (4IR) Only Calculated Value
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1 = 0x43610000; // Real Time Clock (RTC) (0xE1)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1 = 0x43710000; // USB AD as Instrument (0xF1)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2 = 0x43720000; // USB AD as Instrument + Sensor activate bits (0xF2)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_AAMS_DATATYPE = 0x44000000; // AAMS Datatype
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_BATT_QUAT__BATT_ONLY__0XB3 = 0x43330000; // IMU_QUAT_BATT (Quat + Batt only) (0xB3)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_QUAT_ONLY__0XB4 = 0x43340000; // IMU_QUAT (Quat only) (0xB4)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_0XB5 = 0x43350000; // IMU_QUAT_GYRO_ACC (0xB5)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_QUAT_GYRO_ACC_100Hz_0XB6 = 0x43360000; // IMU_QUAT_GYRO_ACC_100Hz (0xB6)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT100HZ_0XB7 = 0x43370000; // IMU_QUAT_100Hz (Quat only) (0xB7)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF_0XB8 = 0x43380000; // IMU_QUAT_9DOF (Quat only) (0xB8)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUQUAT9DOF100HZ_0XB9 = 0x43390000; // IMU_QUAT_9DOF_100Hz (Quat only) (0xB9)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG_0XBA = 0x433A0000; // IMU_GYRO_ACC_MAG (0xBA)
    public static final float SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMUGYROACCMAG100HZ_0XBB = 0x433B0000; // IMU_GYRO_ACC_MAG_100Hz (0xBB)
    public static final short SETUP_PRM_SOFTWARE_FUNCTION = 0x0100; // Software function to be executed in case of software instrument (No datatype)
    public static final float SETUP_PRM_SOFTWARE_FUNCTION_option_NO_FUNCTION = 0x00000000; // No function
    public static final float SETUP_PRM_SOFTWARE_FUNCTION_option_SIMPLE_FUNCTION_THAT_CALCULATES_MIN_VALUE = 0x3F800000; // Simple function that calculates MIN value
    public static final float SETUP_PRM_SOFTWARE_FUNCTION_option_SIMPLE_FUNCTION_THAT_CALCULATES_MAX_VALUE = 0x40000000; // Simple function that calculates MAX value
    public static final float SETUP_PRM_SOFTWARE_FUNCTION_option_OAS_WITH_4_SENSORS = 0x40400000; // OAS with 4 Sensors
    public static final short SETUP_PRM_INSTRUMENT_1_ID = 0x0101; // Sensor ID for input 1 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_2_ID = 0x0102; // Sensor ID for input 2 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_3_ID = 0x0103; // Sensor ID for input 3 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_4_ID = 0x0104; // Sensor ID for input 4 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_5_ID = 0x0105; // Sensor ID for input 5 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_6_ID = 0x0106; // Sensor ID for input 6 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_7_ID = 0x0107; // Sensor ID for input 7 of software instrument
    public static final short SETUP_PRM_INSTRUMENT_8_ID = 0x0108; // Sensor ID for input 8 of software instrument
    public static final short SETUP_PRM_APP_INSTRUMENT_ICON = 0x010B; // Icon reference for visualisation in the app
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_NONE = 0x00000000; // None
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_IMU_ICON = 0x3F800000; // IMU Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_GPS_ICON = 0x40000000; // GPS Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_DISTANCE_SENSOR_ICON = 0x40400000; // Distance Sensor Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_HEART_RATE_ICON = 0x40800000; // Heart Rate Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_ICON = 0x40A00000; // Joystick Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_OAS_ICON = 0x40C00000; // OAS Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_RTC_ICON = 0x40E00000; // RTC Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_ANDROID_DEVICE_ICON = 0x41000000; // Android Device Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_JOYSTICK_PROFILE_ICON = 0x41100000; // Joystick Profile Icon
    public static final float SETUP_PRM_APP_INSTRUMENT_ICON_option_AAMS_ICON = 0x41200000; // AAMS Icon
    public static final short SETUP_PRM_APP_INSTRUMENT_APP_ONLY = 0x010C; // If this is set to False, the instrument has no functional link with the Mainboard. Only the Android Device uses this instrument.
    public static final short SETUP_PRM_VIEW_ANGLE = 0x010D; // View Angle  (Name probably needs changing)
    public static final short SETUP_PRM_INTER_SENSOR_DISTANCE = 0x010E; // The distance between multiple individual IR or US components within one single instrument.  (Name probably needs changing) in m
    public static final short SETUP_PRM_INSTRUMENT_MIN_ALLOWED_DATA_OUTPUT_DATATYPE = 0x010F; // Minimum allowed Data.Output.Datatype for Instrument
    public static final short SETUP_PRM_INSTRUMENT_MAX_ALLOWED_DATA_OUTPUT_DATATYPE = 0x0110; // Maximum allowed Data.Output.Datatype for Instrument
    public static final short SETUP_PRM_POLL_RANK = 0x0111; // Poll Rank
    public static final short SETUP_PRM_JOYSTICK_ID = 0x0200; // The Joystick ID to which this profile belongs
    public static final short SETUP_PRM_PROFILE_NUMBER = 0x0201; // Profile number for the specific joystick
    public static final short SETUP_PRM_SHORT_THROW_TRAVEL_AS_PERCENTAGE = 0x0202; // Short Throw Travel (as percentage)
    public static final short SETUP_PRM_FORWARD_SPEED_AS_PERCENTAGE = 0x0203; // Forward Speed (as percentage)
    public static final short SETUP_PRM_P_G_PARAMETER_PLACEHOLDER = 0x0300; // Placeholder parameter for a P&amp;G Parameter
    public static final short SETUP_PRM_DX_PARAMETER_PLACEHOLDER = 0x0301; // Placeholder parameter for a DX Parameter
    public static final short SETUP_PRM_HEIGHT_CM = 0x0302; //
    public static final short SETUP_PRM_WEIGHT_KG = 0x0303; //
    public static final short SETUP_PRM_TEMPLATE = 0x0400; // Template Boolean
    public static final short SETUP_PRM_TEMPLATE_TYPE = 0x0401; // Template Type allowing the app to visualize this template
    public static final float SETUP_PRM_TEMPLATE_TYPE_option_WHEELCHAIR = 0x3F800000; // Wheelchair
    public static final float SETUP_PRM_TEMPLATE_TYPE_option_BODY_COMPLETE = 0x40000000; // Body (Complete)
    public static final float SETUP_PRM_TEMPLATE_TYPE_option_BODY_FRONT = 0x40400000; // Body (Front)
    public static final float SETUP_PRM_TEMPLATE_TYPE_option_BODY_BACK = 0x40800000; // Body (Back)
    public static final short SETUP_PRM_MAXIMUM_SPEED = 0x0402; // Maximum Wheelchair Speed (km/h)
    public static final short SETUP_PRM_OAS_SLOPE_START = 0x0403; // Distance at the start of the slope
    public static final short SETUP_PRM_OAS_SLOPE_PERCENTAGE = 0x0404; // Percentage of max speed at the start of the slope
    public static final short SETUP_PRM_OAS_SLOPE_END = 0x0405; // Distance at the end of the slope at 100 % of max speed
    public static final short SETUP_PRM_DISTANCE_CALIBRATION = 0x0406; // PWC boundary calibration (cm)
    public static final short SETUP_PRM_BT_MAC_HIGH = 0x0407; //
    public static final short SETUP_PRM_BT_MAC_LOW = 0x0408; //

    public static final int[] GRAPH_COLORS = {
            0xfff44336, // red
            0xffe91e36, // pink
            0xff9c27b0, // purple
            0xff673ab7, // deep purple
            0xff3f51b5, // indigo
            0xff2196f3, // blue
            0xff03a9f4, // light blue
            0xff00bcd4, // cyan
            0xff009688, // teal
            0xff4caf50, // green
            0xff8bc34a, // light green
            0xffcddc39, // lime
            0xffffeb3b, // yellow
            0xffffc107, // amber
            0xffff9800, // orange
            0xffff5722, // deep orange
            0xff795548, // brown
            0xff9e9e9e, // grey
            0xff607d8b // blue grey
    };

    public static class ACTION {
        public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
        public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    }

    public static final class NOTIFICATION_IDS {
        public static final int USB_SERVICE = 1;
        public static final int TCP_SERVICE = 2;
    }

    public static final int SETUP = 1;
    public static final int INSTRUMENT = 2;
    public static final int TYPE = 3;
    public static final int PARAMETER = 4;

    public static class VOLLEY_ERRORS {
        public static final int LOGIN_ATTEMPT = 1;
        public static final int SHOWING_OFFLINE_DATA = 2;
    }

    // Shared preferences categories
    public static final String AUTH_CACHE = "auth_cache";
    public static final String LOGIN_CACHE = "login_cache";
    public static final String PERMISSIONS_CACHE = "permissions_cache";
    public static final String QUESTIONNAIRES_DATA = "questionnaires_data";
    public static final String SUBMISSIONS_DATA = "submissions_data";
    public static final String SETUP_DATA = "setup_data";
    public static final String GRAPH_DATA = "graph_data";
    public static final String LOG_DATA = "log_data";

    // Shared preferences keys
    public static final String API_QUESTIONNAIRES = "api_questionnaires";
    public static final String API_QUESTIONNAIRES_ = "api_questionnaires_";
    public static final String API_SUBMISSIONS = "api_submissions";
    public static final String API_SUBMISSIONS_ = "api_submissions_";
    public static final String API_SETUPS = "api_setups";
    public static final String API_SETUPS_ = "api_setups_";
    public static final String API_SETUPS_INSTRUMENTS = "api_setups_instruments";
    public static final String API_SETUPS_INSTRUMENTS_ = "api_setups_instruments_";
    public static final String API_INSTRUMENT_TYPES = "api_instrument_types";
    public static final String API_INSTRUMENT_TYPES_ = "api_instrument_types_";
    public static final String API_PARAMETERS = "api_parameters";
    public static final String API_PARAMETERS_ = "api_parameters_";
    public static final String API_USERS = "api_users";
    public static final String API_USERS_ = "api_users_";
}
