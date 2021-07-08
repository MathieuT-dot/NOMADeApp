package com.nomade.android.nomadeapp.helperClasses;

/**
 * StatusCodes
 *
 * List of status codes used to keep track of the status of the service and communication with
 * the DMU.
 */
public class StatusCodes {

    // Communication not initialised.
    public static final int UTS_NOT_INIT = 200;

    // Communication initialised.
    public static final int UTS_INIT = 201;

    // Address received
    public static final int UTS_ADDRESS = 202;

    // Watchdog messages active
    public static final int UTS_WATCHDOG = 203;

    // Watchdog messages active
    public static final int UTS_NO_SETUP = 204;

    // Data stream active
    public static final int UTS_STREAM_BUSY = 205;

    // Measurement active
    public static final int UTS_MEASUREMENT_BUSY = 206;
}
