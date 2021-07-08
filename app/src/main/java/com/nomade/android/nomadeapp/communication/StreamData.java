package com.nomade.android.nomadeapp.communication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * StreamData
 *
 * Class to define the structure of the StreamData
 */
public class StreamData implements Parcelable {

    private long cycleCounter;
    private byte[] data;
    private byte receivedPackets;
    private byte watchdog;
    private boolean expectingData;
    private boolean acknowledged;

    public StreamData(long cycleCounter, byte[] data, byte receivedPackets, byte watchdog, boolean expectingData, boolean acknowledged) {
        this.cycleCounter = cycleCounter;
        this.data = data;
        this.receivedPackets = receivedPackets;
        this.watchdog = watchdog;
        this.expectingData = expectingData;
        this.acknowledged = acknowledged;
    }

    public long getCycleCounter() {
        return cycleCounter;
    }

    public void setCycleCounter(long cycleCounter) {
        this.cycleCounter = cycleCounter;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte getReceivedPackets() {
        return receivedPackets;
    }

    public void setReceivedPackets(byte receivedPackets) {
        this.receivedPackets = receivedPackets;
    }

    public byte getWatchdog() {
        return watchdog;
    }

    public void setWatchdog(byte watchdog) {
        this.watchdog = watchdog;
    }

    public boolean isExpectingData() {
        return expectingData;
    }

    public void setExpectingData(boolean expectingData) {
        this.expectingData = expectingData;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }



    protected StreamData(Parcel in) {
        cycleCounter = in.readLong();
        data = in.createByteArray();
        receivedPackets = in.readByte();
        watchdog = in.readByte();
        expectingData = in.readByte() != 0x00;
        acknowledged = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(cycleCounter);
        dest.writeByteArray(data);
        dest.writeByte(receivedPackets);
        dest.writeByte(watchdog);
        dest.writeByte((byte) (expectingData ? 0x01 : 0x00));
        dest.writeByte((byte) (acknowledged ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<StreamData> CREATOR = new Parcelable.Creator<StreamData>() {
        @Override
        public StreamData createFromParcel(Parcel in) {
            return new StreamData(in);
        }

        @Override
        public StreamData[] newArray(int size) {
            return new StreamData[size];
        }
    };
}
