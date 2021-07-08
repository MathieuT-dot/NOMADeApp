package com.nomade.android.nomadeapp.communication;

import android.os.Parcel;
import android.os.Parcelable;

import com.xuhao.didi.core.iocore.interfaces.ISendable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * TcpTelegram
 *
 * Class to define the structure of a TcpTelegram
 */
public class TcpTelegram implements Parcelable, ISendable {

    // Start delimiters
    public static final byte SD1 = (byte) 0x10;
    public static final byte SD2 = (byte) 0x68;
    public static final byte SD3 = (byte) 0xA2;
    public static final byte SD4 = (byte) 0xDC;

    // End delimiter
    public static final byte ED = (byte) 0x16;

    // Short Confirmation
    public static final byte SC = (byte) 0xE5;

    // Variables
    private byte sd;
    private int le;
    private byte fc;
    private int fcs;
    private byte ed;
    private byte[] du;

    private long cycleCounter;

    /**
     * Constructs a new telegram based on sd, da, sa, fc and du
     *
     * @param sd Start Delimiter
     * @param fc Function Code
     * @param du Data Unit (payload data)
     */
    public TcpTelegram(byte sd, byte fc, byte[] du){

        this.le = 0;
        this.fcs = 0;

        switch (sd){

            case SD1:
                this.sd = sd;
                this.fc = fc;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();

                break;

            case SD2:
                this.sd = sd;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();
                for (byte b : du) {
                    this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, b}).getInt();
                }

                this.le += (short) (1 + du.length);

                break;

            case SD3:
                this.sd = sd;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();
                for (byte b : du) {
                    this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, b}).getInt();
                }

                break;

            case SD4:
                this.sd = sd;
                break;

            case SC:
                this.sd = sd;
                break;
        }

    }

    public TcpTelegram(byte sd, byte fc, byte[] du, long cycleCounter) {

        this.le = 0;
        this.fcs = 0;

        switch (sd){

            case SD1:
                this.sd = sd;
                this.fc = fc;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();

                break;

            case SD2:
                this.sd = sd;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();
                for (byte b : du) {
                    this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, b}).getInt();
                }

                this.le += (short) (1 + du.length);

                break;

            case SD3:
                this.sd = sd;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, fc}).getInt();
                for (byte b : du) {
                    this.fcs += ByteBuffer.wrap(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, b}).getInt();
                }

                break;

            case SD4:
                this.sd = sd;
                break;

            case SC:
                this.sd = sd;
                break;
        }

        this.cycleCounter = cycleCounter;
    }

    /**
     * Converts a telegram to a byte[].
     *
     * @return byte[] value of the telegram
     */
    public byte[] ConvertToByteArray(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            switch (sd) {
                case SD1:
                    outputStream.write(sd);
                    outputStream.write(fc);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[2]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[3]);
                    outputStream.write(ed);
                    break;

                case SD2:
                    outputStream.write(sd);
                    outputStream.write(ByteBuffer.allocate(4).putInt(le).array()[2]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(le).array()[3]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(le).array()[2]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(le).array()[3]);
                    outputStream.write(sd);
                    outputStream.write(fc);
                    outputStream.write(du);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[2]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[3]);
                    outputStream.write(ed);
                    break;

                case SD3:
                    outputStream.write(sd);
                    outputStream.write(fc);
                    outputStream.write(du);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[2]);
                    outputStream.write(ByteBuffer.allocate(4).putInt(fcs).array()[3]);
                    outputStream.write(ed);
                    break;

                case SD4:
                    outputStream.write(sd);
                    break;

                case SC:
                    outputStream.write(sd);
                    break;
            }

        } catch (IOException e){

        }
        return outputStream.toByteArray( );
    }

    @Override
    public byte[] parse() {
        return ConvertToByteArray();
    }

    public byte getSd() {
        return sd;
    }

    public void setSd(byte sd) {
        this.sd = sd;
    }

    public int getLe() {
        return le;
    }

    public void setLe(int le) {
        this.le = le;
    }

    public byte getFc() {
        return fc;
    }

    public void setFc(byte fc) {
        this.fc = fc;
    }

    public int getFcs() {
        return fcs;
    }

    public void setFcs(int fcs) {
        this.fcs = fcs;
    }

    public byte getEd() {
        return ed;
    }

    public void setEd(byte ed) {
        this.ed = ed;
    }

    public byte[] getDu() {
        return du;
    }

    public void setDu(byte[] du) {
        this.du = du;
    }

    public long getCycleCounter() {
        return cycleCounter;
    }

    protected TcpTelegram(Parcel in) {
        sd = in.readByte();
        le = in.readInt();
        fc = in.readByte();
        fcs = in.readInt();
        ed = in.readByte();
        du = in.createByteArray();
        cycleCounter = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(sd);
        dest.writeInt(le);
        dest.writeByte(fc);
        dest.writeInt(fcs);
        dest.writeByte(ed);
        dest.writeByteArray(du);
        dest.writeLong(cycleCounter);
    }

    @SuppressWarnings("unused")
    public static final Creator<TcpTelegram> CREATOR = new Creator<TcpTelegram>() {
        @Override
        public TcpTelegram createFromParcel(Parcel in) {
            return new TcpTelegram(in);
        }

        @Override
        public TcpTelegram[] newArray(int size) {
            return new TcpTelegram[size];
        }
    };
}

