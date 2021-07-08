package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * ParameterOption
 *
 * Class that describes a ParameterOption.
 */
public class ParameterOption implements Parcelable {

    private Float value;
    private String name;


    public ParameterOption(Float value, String name){
        this.value = value;
        this.name = name;
    }


    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    protected ParameterOption(Parcel in) {
        value = in.readByte() == 0x00 ? null : in.readFloat();
        name = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (value == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(value);
        }
        dest.writeString(name);
    }

    @SuppressWarnings("unused")
    public static final Creator<ParameterOption> CREATOR = new Creator<ParameterOption>() {
        @Override
        public ParameterOption createFromParcel(Parcel in) {
            return new ParameterOption(in);
        }

        @Override
        public ParameterOption[] newArray(int size) {
            return new ParameterOption[size];
        }
    };
}