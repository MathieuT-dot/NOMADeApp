package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Setup
 *
 * Class that describes a Setup.
 */
public class Setup implements Parcelable {

    private int id;
    private int groupId;
    private String name;
    private int hardwareIdentifier;
    private int version;
    private boolean locked;
    private ArrayList<Instrument> instrumentArrayList = new ArrayList<>();


    public Setup(int id, int groupId, String name, int hardwareIdentifier, int version, boolean locked) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.hardwareIdentifier = hardwareIdentifier;
        this.version = version;
        this.locked = locked;
    }

    public Setup(int id, int groupId, String name, int hardwareIdentifier, int version, boolean locked, ArrayList<Instrument> instrumentArrayList) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.hardwareIdentifier = hardwareIdentifier;
        this.version = version;
        this.locked = locked;
        this.instrumentArrayList = instrumentArrayList;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHardwareIdentifier() {
        return hardwareIdentifier;
    }

    public void setHardwareIdentifier(int hardwareIdentifier) {
        this.hardwareIdentifier = hardwareIdentifier;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public ArrayList<Instrument> getInstrumentArrayList() {
        return instrumentArrayList;
    }

    public void setInstrumentArrayList(ArrayList<Instrument> instrumentArrayList) {
        this.instrumentArrayList = instrumentArrayList;
    }


    public static class IdComparator implements Comparator<Setup> {
        @Override
        public int compare(Setup o1, Setup o2) {
            return Integer.compare(o1.id, o2.id);
        }
    }

    public static class NameComparator implements Comparator<Setup> {
        @Override
        public int compare(Setup o1, Setup o2) {
            return o1.name.compareTo(o2.name);
        }
    }


    protected Setup(Parcel in) {
        id = in.readInt();
        groupId = in.readInt();
        name = in.readString();
        hardwareIdentifier = in.readInt();
        version = in.readInt();
        locked = in.readByte() != 0x00;
        if (in.readByte() == 0x01) {
            instrumentArrayList = new ArrayList<Instrument>();
            in.readList(instrumentArrayList, Instrument.class.getClassLoader());
        } else {
            instrumentArrayList = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(groupId);
        dest.writeString(name);
        dest.writeInt(hardwareIdentifier);
        dest.writeInt(version);
        dest.writeByte((byte) (locked ? 0x01 : 0x00));
        if (instrumentArrayList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(instrumentArrayList);
        }
    }

    @SuppressWarnings("unused")
    public static final Creator<Setup> CREATOR = new Creator<Setup>() {
        @Override
        public Setup createFromParcel(Parcel in) {
            return new Setup(in);
        }

        @Override
        public Setup[] newArray(int size) {
            return new Setup[size];
        }
    };
}
