package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Instrument
 *
 * Class that describes an Instrument.
 */
public class Instrument implements Parcelable {

    private int id;
    private int instrumentTypeId;
    private String name;
    private String description;
    private boolean locked;
    private int setupId;
    private Type type;
    private ArrayList<Parameter> parameterArrayList = new ArrayList<>();

    private int dragViewId;
    private Float locationX;
    private Float locationY;
    private Float rotation;
    private boolean locatable;
    private boolean placed;

    private int outputDataType;
    private ArrayList<Variable> variableArrayList = new ArrayList<>();
    private int indexOffset;


    public Instrument(int id, int instrumentTypeId, String name) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
    }

    public Instrument(int id, int instrumentTypeId, String name, String description, boolean locked) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
        this.description = description;
        this.locked = locked;
    }

    public Instrument(int id, int instrumentTypeId, String name, boolean locked, int setupId, Type type, ArrayList<Parameter> parameterArrayList) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
        this.locked = locked;
        this.setupId = setupId;
        this.type = type;
        this.parameterArrayList = parameterArrayList;
    }

    public Instrument(int id, int instrumentTypeId, String name, String description, boolean locked, int setupId, Type type, ArrayList<Parameter> parameterArrayList) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
        if (description != null && !description.equals("null")){
            this.description = description;
        }
        else {
            this.description = "";
        }
        this.locked = locked;
        this.setupId = setupId;
        this.type = type;
        this.parameterArrayList = parameterArrayList;
    }

    public Instrument(int id, int instrumentTypeId, String name, boolean locked, Type type, ArrayList<Parameter> parameterArrayList) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
        this.locked = locked;
        this.type = type;
        this.parameterArrayList = parameterArrayList;
    }

    public Instrument(int id, int instrumentTypeId, String name, String description, boolean locked, Type type, ArrayList<Parameter> parameterArrayList) {
        this.id = id;
        this.instrumentTypeId = instrumentTypeId;
        this.name = name;
        if (description != null && !description.equals("null")){
            this.description = description;
        }
        else {
            this.description = "";
        }
        this.locked = locked;
        this.type = type;
        this.parameterArrayList = parameterArrayList;
    }

    public Instrument(int id, int outputDataType, String name, ArrayList<Variable> variableArrayList, int indexOffset) {
        this.id = id;
        this.outputDataType = outputDataType;
        this.name = name;
        this.variableArrayList = variableArrayList;
        this.indexOffset = indexOffset;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInstrumentTypeId() {
        return instrumentTypeId;
    }

    public void setInstrumentTypeId(int instrumentTypeId) {
        this.instrumentTypeId = instrumentTypeId;
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

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getSetupId() {
        return setupId;
    }

    public void setSetupId(int setupId) {
        this.setupId = setupId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ArrayList<Parameter> getParameterArrayList() {
        return parameterArrayList;
    }

    public void setParameterArrayList(ArrayList<Parameter> parameterArrayList) {
        this.parameterArrayList = parameterArrayList;
    }

    public int getDragViewId() {
        return dragViewId;
    }

    public void setDragViewId(int dragViewId) {
        this.dragViewId = dragViewId;
    }

    public Float getLocationX() {
        return locationX;
    }

    public void setLocationX(Float locationX) {
        this.locationX = locationX;
    }

    public Float getLocationY() {
        return locationY;
    }

    public void setLocationY(Float locationY) {
        this.locationY = locationY;
    }

    public Float getRotation() {
        return rotation;
    }

    public void setRotation(Float rotation) {
        this.rotation = rotation;
    }

    public boolean isLocatable() {
        return locatable;
    }

    public void setLocatable(boolean locatable) {
        this.locatable = locatable;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    public int getOutputDataType() {
        return outputDataType;
    }

    public void setOutputDataType(int outputDataType) {
        this.outputDataType = outputDataType;
    }

    public ArrayList<Variable> getVariableArrayList() {
        return variableArrayList;
    }

    public void setVariableArrayList(ArrayList<Variable> variableArrayList) {
        this.variableArrayList = variableArrayList;
    }

    public int getIndexOffset() {
        return indexOffset;
    }

    public void setIndexOffset(int indexOffset) {
        this.indexOffset = indexOffset;
    }


    protected Instrument(Parcel in) {
        id = in.readInt();
        instrumentTypeId = in.readInt();
        name = in.readString();
        description = in.readString();
        locked = in.readByte() != 0x00;
        setupId = in.readInt();
        type = (Type) in.readValue(Type.class.getClassLoader());
        if (in.readByte() == 0x01) {
            parameterArrayList = new ArrayList<Parameter>();
            in.readList(parameterArrayList, Parameter.class.getClassLoader());
        } else {
            parameterArrayList = null;
        }
        dragViewId = in.readInt();
        locationX = in.readByte() == 0x00 ? null : in.readFloat();
        locationY = in.readByte() == 0x00 ? null : in.readFloat();
        rotation = in.readByte() == 0x00 ? null : in.readFloat();
        locatable = in.readByte() != 0x00;
        placed = in.readByte() != 0x00;
        outputDataType = in.readInt();
        if (in.readByte() == 0x01) {
            variableArrayList = new ArrayList<Variable>();
            in.readList(variableArrayList, Variable.class.getClassLoader());
        } else {
            variableArrayList = null;
        }
        indexOffset = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(instrumentTypeId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeByte((byte) (locked ? 0x01 : 0x00));
        dest.writeInt(setupId);
        dest.writeValue(type);
        if (parameterArrayList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(parameterArrayList);
        }
        dest.writeInt(dragViewId);
        if (locationX == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(locationX);
        }
        if (locationY == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(locationY);
        }
        if (rotation == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeFloat(rotation);
        }
        dest.writeByte((byte) (locatable ? 0x01 : 0x00));
        dest.writeByte((byte) (placed ? 0x01 : 0x00));
        dest.writeInt(outputDataType);
        if (variableArrayList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(variableArrayList);
        }
        dest.writeInt(indexOffset);
    }

    @SuppressWarnings("unused")
    public static final Creator<Instrument> CREATOR = new Creator<Instrument>() {
        @Override
        public Instrument createFromParcel(Parcel in) {
            return new Instrument(in);
        }

        @Override
        public Instrument[] newArray(int size) {
            return new Instrument[size];
        }
    };
}
