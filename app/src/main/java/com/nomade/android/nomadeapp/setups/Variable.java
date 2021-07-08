package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Variable
 *
 * Class that describes a Variable.
 */
public class Variable implements Parcelable {

    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INTEGER = 3;
    public static final int LONG = 4;
    public static final int FLOAT = 5;
    public static final int DOUBLE = 6;

    public static final int UNSIGNED_BYTE = 10;
    public static final int UNSIGNED_SHORT = 11;

    private String name;
    private String unit;
    private float factor;
    private int type;
    private int indexOffset;
    private String chartName;
    private int chartColor;
    private int chartIndex;
    private int axisIndex;
    private int valueIndex;
    private boolean hundredHertz;
    private int oversampledOffset;


    public Variable(String name, String unit, float factor, int type, int indexOffset) {
        this.name = name;
        this.unit = unit;
        this.factor = factor;
        this.type = type;
        this.indexOffset = indexOffset;
        this.chartName = "";
        this.chartColor = 0;
        this.chartIndex = 0;
        this.axisIndex = 0;
        this.valueIndex = 0;
        this.hundredHertz = false;
    }

    public Variable(String name, String unit, float factor, int type, int indexOffset, int valueIndex) {
        this.name = name;
        this.unit = unit;
        this.factor = factor;
        this.type = type;
        this.indexOffset = indexOffset;
        this.chartName = "";
        this.chartColor = 0;
        this.chartIndex = 0;
        this.axisIndex = 0;
        this.valueIndex = valueIndex;
        this.hundredHertz = false;
    }

    public Variable(String name, String unit, float factor, int type, int indexOffset, int valueIndex, boolean hundredHertz, int oversampledOffset) {
        this.name = name;
        this.unit = unit;
        this.factor = factor;
        this.type = type;
        this.indexOffset = indexOffset;
        this.chartName = "";
        this.chartColor = 0;
        this.chartIndex = 0;
        this.axisIndex = 0;
        this.valueIndex = valueIndex;
        this.hundredHertz = hundredHertz;
        this.oversampledOffset = oversampledOffset;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getIndexOffset() {
        return indexOffset;
    }

    public void setIndexOffset(int indexOffset) {
        this.indexOffset = indexOffset;
    }

    public String getChartName() {
        return chartName;
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
    }

    public int getChartColor() {
        return chartColor;
    }

    public void setChartColor(int chartColor) {
        this.chartColor = chartColor;
    }

    public int getChartIndex() {
        return chartIndex;
    }

    public void setChartIndex(int chartIndex) {
        this.chartIndex = chartIndex;
    }

    public int getAxisIndex() {
        return axisIndex;
    }

    public void setAxisIndex(int axisIndex) {
        this.axisIndex = axisIndex;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
    }

    public boolean isHundredHertz() {
        return hundredHertz;
    }

    public void setHundredHertz(boolean hundredHertz) {
        this.hundredHertz = hundredHertz;
    }

    public int getOversampledOffset() {
        return oversampledOffset;
    }

    public void setOversampledOffset(int oversampledOffset) {
        this.oversampledOffset = oversampledOffset;
    }

    protected Variable(Parcel in) {
        name = in.readString();
        unit = in.readString();
        factor = in.readFloat();
        type = in.readInt();
        indexOffset = in.readInt();
        chartName = in.readString();
        chartColor = in.readInt();
        chartIndex = in.readInt();
        axisIndex = in.readInt();
        valueIndex = in.readInt();
        hundredHertz = in.readInt() == 1;
        oversampledOffset = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(unit);
        dest.writeFloat(factor);
        dest.writeInt(type);
        dest.writeInt(indexOffset);
        dest.writeString(chartName);
        dest.writeInt(chartColor);
        dest.writeInt(chartIndex);
        dest.writeInt(axisIndex);
        dest.writeInt(valueIndex);
        dest.writeInt(hundredHertz ? 1 : 0);
        dest.writeInt(oversampledOffset);
    }

    @SuppressWarnings("unused")
    public static final Creator<Variable> CREATOR = new Creator<Variable>() {
        @Override
        public Variable createFromParcel(Parcel in) {
            return new Variable(in);
        }

        @Override
        public Variable[] newArray(int size) {
            return new Variable[size];
        }
    };
}