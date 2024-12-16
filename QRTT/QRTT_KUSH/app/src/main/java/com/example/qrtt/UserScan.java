package com.example.qrtt;

import android.os.Parcel;
import android.os.Parcelable;

public class UserScan implements Parcelable { // интерфейс Parcelable для передачи объектов между компонентами (xml) приложения
    public String userId;
    public String entry_time;
    public String release_time;
    public long time_hoursDifference;
    public String user; // для хранения информации о пользователе

    public UserScan() {
        // пустой конструктор необходим для Firebase
    }

    public UserScan(String userId, String entry_time, String release_time, long time_hoursDifference, String user) {
        this.userId = userId;
        this.entry_time = entry_time;
        this.release_time = release_time;
        this.time_hoursDifference = time_hoursDifference;
        this.user = user;
    }

    protected UserScan(Parcel in) {
        userId = in.readString();
        entry_time = in.readString();
        release_time = in.readString();
        time_hoursDifference = in.readLong();
        user = in.readString();
    }

    public static final Creator<UserScan> CREATOR = new Creator<UserScan>() {
        @Override
        public UserScan createFromParcel(Parcel in) {
            return new UserScan(in);
        }

        @Override
        public UserScan[] newArray(int size) {
            return new UserScan[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(entry_time);
        dest.writeString(release_time);
        dest.writeLong(time_hoursDifference);
        dest.writeString(user);
    }
}