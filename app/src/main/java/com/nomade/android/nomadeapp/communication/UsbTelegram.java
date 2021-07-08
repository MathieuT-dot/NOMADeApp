package com.nomade.android.nomadeapp.communication;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * UsbTelegram
 *
 * Class to define the structure of a UsbTelegram
 */
public class UsbTelegram implements Parcelable {

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
    private byte le;
    private byte da;
    private byte sa;
    private byte fc;
    private byte dsap;
    private byte ssap;
    private byte fcs;
    private byte ed;
    private byte[] du;

    /**
     * Constructs a new telegram based on sd, da, sa, fc and du
     *
     * @param sd Start Delimiter
     * @param da Destination Address
     * @param sa Source Address
     * @param fc Function Code
     * @param du Data Unit (payload data)
     */
    public UsbTelegram(byte sd, byte da, byte sa, byte fc, byte[] du){

        this.le = (byte) 0;
        this.fcs = (byte) 0;

        switch (sd){

            case SD1:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);

                break;

            case SD2:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);
                for (byte b : du) {
                    this.fcs += b;
                }

                this.le += (byte) (3 + du.length);

                break;

            case SD3:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);
                for (byte b : du) {
                    this.fcs += b;
                }

                break;

            case SD4:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                break;

            case SC:
                this.sd = sd;
                break;
        }

    }

    /**
     * Constructs a new telegram based on sd, da, sa, fc and du
     *
     * @param sd Start Delimiter
     * @param da Destination Address
     * @param sa Source Address
     * @param fc Function Code
     * @param dsap Destination Service Access Point
     * @param ssap Source Service Access Point
     * @param du Data Unit (payload data)
     */
    public UsbTelegram(byte sd, byte da, byte sa, byte fc, byte dsap, byte ssap, byte[] du){

        this.le = (byte) 0;
        this.fcs = (byte) 0;

        switch (sd){

            case SD1:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);

                break;

            case SD2:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);
                for (byte b : du) {
                    this.fcs += b;
                }

                this.le += (byte) (3 + du.length);

                if (!(dsap == 0x00 && ssap == 0x00)){
                    this.da = (byte) (da | 0x80);
                    this.sa = (byte) (sa | 0x80);

                    this.dsap = dsap;
                    this.ssap = ssap;
                    this.fcs += (byte) (dsap + ssap);
                    this.le += (byte) 2;
                }

                break;

            case SD3:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                this.fc = fc;
                this.du = du;
                this.ed = ED;

                this.fcs += (byte) (da + sa + fc);
                for (byte b : du) {
                    this.fcs += b;
                }

                break;

            case SD4:
                this.sd = sd;
                this.da = da;
                this.sa = sa;
                break;

            case SC:
                this.sd = sd;
                break;
        }

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
                    outputStream.write(da);
                    outputStream.write(sa);
                    outputStream.write(fc);
                    outputStream.write(fcs);
                    outputStream.write(ed);
                    break;

                case SD2:
                    outputStream.write(sd);
                    outputStream.write(le);
                    outputStream.write(le);
                    outputStream.write(sd);
                    outputStream.write(da);
                    outputStream.write(sa);
                    outputStream.write(fc);

                    if ((da & 0x80) == 0x80 && (sa & 0x80) == 0x80) {
                        outputStream.write(dsap);
                        outputStream.write(ssap);
                    }

                    outputStream.write(du);
                    outputStream.write(fcs);
                    outputStream.write(ed);
                    break;

                case SD3:
                    outputStream.write(sd);
                    outputStream.write(da);
                    outputStream.write(sa);
                    outputStream.write(fc);
                    outputStream.write(du);
                    outputStream.write(fcs);
                    outputStream.write(ed);
                    break;

                case SD4:
                    outputStream.write(sd);
                    outputStream.write(da);
                    outputStream.write(sa);
                    break;

                case SC:
                    outputStream.write(sd);
                    break;
            }

        } catch (IOException e){

        }
        return outputStream.toByteArray( );
    }


    public byte getSd() {
        return sd;
    }

    public void setSd(byte sd) {
        this.sd = sd;
    }

    public byte getLe() {
        return le;
    }

    public void setLe(byte le) {
        this.le = le;
    }

    public byte getDa() {
        return da;
    }

    public void setDa(byte da) {
        this.da = da;
    }

    public byte getSa() {
        return sa;
    }

    public void setSa(byte sa) {
        this.sa = sa;
    }

    public byte getFc() {
        return fc;
    }

    public void setFc(byte fc) {
        this.fc = fc;
    }

    public byte getDsap() {
        return dsap;
    }

    public void setDsap(byte dsap) {
        this.dsap = dsap;
    }

    public byte getSsap() {
        return ssap;
    }

    public void setSsap(byte ssap) {
        this.ssap = ssap;
    }

    public byte getFcs() {
        return fcs;
    }

    public void setFcs(byte fcs) {
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


    protected UsbTelegram(Parcel in) {
        sd = in.readByte();
        le = in.readByte();
        da = in.readByte();
        sa = in.readByte();
        fc = in.readByte();
        dsap = in.readByte();
        ssap = in.readByte();
        fcs = in.readByte();
        ed = in.readByte();
        du = in.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(sd);
        dest.writeByte(le);
        dest.writeByte(da);
        dest.writeByte(sa);
        dest.writeByte(fc);
        dest.writeByte(dsap);
        dest.writeByte(ssap);
        dest.writeByte(fcs);
        dest.writeByte(ed);
        dest.writeByteArray(du);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<UsbTelegram> CREATOR = new Parcelable.Creator<UsbTelegram>() {
        @Override
        public UsbTelegram createFromParcel(Parcel in) {
            return new UsbTelegram(in);
        }

        @Override
        public UsbTelegram[] newArray(int size) {
            return new UsbTelegram[size];
        }
    };
}
