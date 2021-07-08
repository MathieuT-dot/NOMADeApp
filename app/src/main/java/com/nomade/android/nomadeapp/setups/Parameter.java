package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Parameter
 *
 * Class that describes a Parameter.
 */
public class Parameter implements Parcelable {

    private int id;
    private String uuid;
    private String name;
    private String description;
    private Integer level;
    private Integer dataType;
    private Boolean mainBoard;
    private Float min;
    private Float max;
    private Float defaultValue;
    private Float value;
    private String valueDescription;
    private ArrayList<ParameterOption> parameterOptionArrayList;


    public Parameter(int id, String uuid, String name, String description, Integer level, Integer dataType, Boolean mainBoard, Float min, Float max, Float defaultValue, ArrayList<ParameterOption> parameterOptionArrayList) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.level = level;
        this.dataType = dataType;
        this.mainBoard = mainBoard;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.parameterOptionArrayList = parameterOptionArrayList;
    }

    public Parameter(int id, String uuid, String name, String description, Integer level, Integer dataType, Boolean mainBoard, Float min, Float max, Float defaultValue, Float value, ArrayList<ParameterOption> parameterOptionArrayList) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.level = level;
        this.dataType = dataType;
        this.mainBoard = mainBoard;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = value;
        this.parameterOptionArrayList = parameterOptionArrayList;
    }

    public Parameter(int id, String uuid, String name, String description, Integer level, Integer dataType, Boolean mainBoard, Float min, Float max, Float defaultValue, Float value, String valueDescription, ArrayList<ParameterOption> parameterOptionArrayList) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.level = level;
        this.dataType = dataType;
        this.mainBoard = mainBoard;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = value;
        this.valueDescription = valueDescription;
        this.parameterOptionArrayList = parameterOptionArrayList;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getDataType() {
        return dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    public Boolean getMainBoard() {
        return mainBoard;
    }

    public void setMainBoard(Boolean mainBoard) {
        this.mainBoard = mainBoard;
    }

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }

    public Float getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Float defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public String getValueDescription() {
        return valueDescription;
    }

    public void setValueDescription(String valueDescription) {
        this.valueDescription = valueDescription;
    }

    public ArrayList<ParameterOption> getParameterOptionArrayList() {
        return parameterOptionArrayList;
    }

    public void setParameterOptionArrayList(ArrayList<ParameterOption> parameterOptionArrayList) {
        this.parameterOptionArrayList = parameterOptionArrayList;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    protected Parameter(Parcel in) {
        id = in.readInt();
        uuid = in.readString();
        name = in.readString();
        description = in.readString();
        level = in.readByte() == 0x00 ? null : in.readInt();
        dataType = in.readByte() == 0x00 ? null : in.readInt();
        byte mainBoardVal = in.readByte();
        mainBoard = mainBoardVal == 0x02 ? null : mainBoardVal != 0x00;
        min = in.readByte() == 0x00 ? null : in.readFloat();
        max = in.readByte() == 0x00 ? null : in.readFloat();
        defaultValue = in.readByte() == 0x00 ? null : in.readFloat();
        value = in.readByte() == 0x00 ? null : in.readFloat();
        valueDescription = in.readString();
        if (in.readByte() == 0x01) {
            parameterOptionArrayList = new ArrayList<ParameterOption>();
            in.readList(parameterOptionArrayList, ParameterOption.class.getClassLoader());
        } else {
            parameterOptionArrayList = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(uuid);
        dest.writeString(name);
        dest.writeString(description);
        if (level == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(level);
        }
        if (dataType == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(dataType);
        }
        if (mainBoard == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (mainBoard ? 0x01 : 0x00));
        }
        if (min == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(min);
        }
        if (max == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(max);
        }
        if (defaultValue == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(defaultValue);
        }
        if (value == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(value);
        }
        dest.writeString(valueDescription);
        if (parameterOptionArrayList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(parameterOptionArrayList);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Parameter> CREATOR = new Parcelable.Creator<Parameter>() {
        @Override
        public Parameter createFromParcel(Parcel in) {
            return new Parameter(in);
        }

        @Override
        public Parameter[] newArray(int size) {
            return new Parameter[size];
        }
    };
}
