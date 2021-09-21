package com.nomade.android.nomadeapp.helperClasses;

/**
 * MessageCodes
 *
 * List of message codes used in the communication between activities and the service.
 */
public class MessageCodes {

    // USB
    public static final int USB_MSG_DATA_PLACEHOLDER = 100;
    public static final int USB_MSG_REGISTER_CLIENT = 101;
    public static final int USB_MSG_UNREGISTER_CLIENT = 102;
    public static final int USB_MSG_CONFIG = 103;
    public static final int USB_MSG_AUTO = 104;
    public static final int USB_MSG_ASK_ADDRESS = 105;
    public static final int USB_MSG_ACK_MISS_CONFIG = 106;
    public static final int USB_MSG_ACK_EXIS_CONFIG = 107;
    public static final int USB_MSG_SETUP_JSON = 108;
    public static final int USB_MSG_SETUP_RAW = 109;
    public static final int USB_MSG_START_STREAM = 110;
    public static final int USB_MSG_STOP_STREAM = 111;
    public static final int USB_MSG_SEND_STRING = 112;
    public static final int USB_MSG_CURRENT_SETUP = 114;
    public static final int USB_MSG_SEND_STREAM_DATA = 116;
    public static final int USB_MSG_SEND_DATA = 117;
    public static final int USB_MSG_START_MEASUREMENT = 118;
    public static final int USB_MSG_STOP_MEASUREMENT = 119;
    public static final int USB_MSG_STORE_OR_DELETE = 121;
    public static final int USB_MSG_REQUEST_JSON_SETUP = 122;
    public static final int USB_MSG_MANUAL_MEASUREMENT_STARTED = 123;
    public static final int USB_MSG_MANUAL_MEASUREMENT_STOPPED = 124;
    public static final int USB_MSG_INIT = 125;
    public static final int USB_MSG_STATUS_UPDATE = 126;
}
