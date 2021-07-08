package com.nomade.android.nomadeapp.setups;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * User
 *
 * Class that describes a User.
 */
public class User implements Parcelable {

    private int userId;
    private String name;
    private int companyId;
    private String company;
    private int setupUserId;

    public User(int userId, String name, int companyId, String company, int setupUserId) {
        this.userId = userId;
        this.name = name;
        this.companyId = companyId;
        this.company = company;
        this.setupUserId = setupUserId;
    }

    public User(int userId, String name, int companyId) {
        this.userId = userId;
        this.name = name;
        this.companyId = companyId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getSetupUserId() {
        return setupUserId;
    }

    public void setSetupUserId(int setupUserId) {
        this.setupUserId = setupUserId;
    }

    protected User(Parcel in) {
        userId = in.readInt();
        name = in.readString();
        companyId = in.readInt();
        company = in.readString();
        setupUserId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(userId);
        dest.writeString(name);
        dest.writeInt(companyId);
        dest.writeString(company);
        dest.writeInt(setupUserId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
