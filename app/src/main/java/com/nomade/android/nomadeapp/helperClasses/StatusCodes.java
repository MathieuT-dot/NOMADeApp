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

    // Watchdog messages active, no setup
    public static final int UTS_NO_SETUP = 204;

    // Watchdog messages active, setup available, not locked
    public static final int UTS_UNLOCKED_SETUP = 205;

    // Watchdog messages active, setup available, locked
    public static final int UTS_LOCKED_SETUP = 206;

    // Data stream active
    public static final int UTS_STREAM_BUSY = 207;

    // Measurement active
    public static final int UTS_MEASUREMENT_BUSY = 208;

    // Watchdog messages active, new setup sent, reboot necessary
    public static final int UTS_NEW_SETUP = 209;


    // Unknown
    public static final int UNKNOWN = 0;

    // Pending
    public static final int PENDING = 1;

    // Active
    public static final int ACTIVE = 2;

    // Inactive
    public static final int INACTIVE = 3;
    
}
