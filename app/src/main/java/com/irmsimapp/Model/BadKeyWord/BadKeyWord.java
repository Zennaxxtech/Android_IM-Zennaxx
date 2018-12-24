package com.irmsimapp.Model.BadKeyWord;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BadKeyWord implements Parcelable {

    @SerializedName("Status")
    @Expose
    private String status;
    @SerializedName("Msg")
    @Expose
    private String msg;
    @SerializedName("Data")
    @Expose
    private List<Datum> data = null;


    protected BadKeyWord(Parcel in) {
        status = in.readString();
        msg = in.readString();
        data = in.createTypedArrayList(Datum.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(status);
        dest.writeString(msg);
        dest.writeTypedList(data);
    }

    public static final Creator<BadKeyWord> CREATOR = new Creator<BadKeyWord>() {
        @Override
        public BadKeyWord createFromParcel(Parcel in) {
            return new BadKeyWord(in);
        }

        @Override
        public BadKeyWord[] newArray(int size) {
            return new BadKeyWord[size];
        }
    };

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static class Datum implements Parcelable {

        @SerializedName("ItemNo")
        @Expose
        private String itemNo;
        @SerializedName("ItemName")
        @Expose
        private String itemName;

        protected Datum(Parcel in) {
            itemNo = in.readString();
            itemName = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(itemNo);
            dest.writeString(itemName);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Datum> CREATOR = new Creator<Datum>() {
            @Override
            public Datum createFromParcel(Parcel in) {
                return new Datum(in);
            }

            @Override
            public Datum[] newArray(int size) {
                return new Datum[size];
            }
        };

        public String getItemNo() {
            return itemNo;
        }

        public void setItemNo(String itemNo) {
            this.itemNo = itemNo;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

    }
}