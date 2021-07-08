package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Type
 *
 * Class that describes a Type.
 */
public class Type implements Parcelable {

    private int id;
    private String name;
    private String category;
    private String description;
    private ArrayList<Parameter> parameterArrayList = new ArrayList<>();


    public Type(int id, String name, String category, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Parameter> getParameterArrayList() {
        return parameterArrayList;
    }

    public void setParameterArrayList(ArrayList<Parameter> parameterArrayList) {
        this.parameterArrayList = parameterArrayList;
    }


    protected Type(Parcel in) {
        id = in.readInt();
        name = in.readString();
        category = in.readString();
        description = in.readString();
        if (in.readByte() == 0x01) {
            parameterArrayList = new ArrayList<Parameter>();
            in.readList(parameterArrayList, Parameter.class.getClassLoader());
        } else {
            parameterArrayList = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(description);
        if (parameterArrayList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(parameterArrayList);
        }
    }

    @SuppressWarnings("unused")
    public static final Creator<Type> CREATOR = new Creator<Type>() {
        @Override
        public Type createFromParcel(Parcel in) {
            return new Type(in);
        }

        @Override
        public Type[] newArray(int size) {
            return new Type[size];
        }
    };
}