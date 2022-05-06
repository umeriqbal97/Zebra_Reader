package com.fauji.zebrareader

import android.os.Parcel
import android.os.Parcelable

data class DailyData(
    var sheet: String = "",
    var date: String = "",
    var timestamp: Long = 0,
    var fileId: String = ""
):Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sheet)
        parcel.writeString(date)
        parcel.writeLong(timestamp)
        parcel.writeString(fileId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DailyData> {
        override fun createFromParcel(parcel: Parcel): DailyData {
            return DailyData(parcel)
        }

        override fun newArray(size: Int): Array<DailyData?> {
            return arrayOfNulls(size)
        }
    }
}

data class Items(
    var productCount: Int = 0,
    var serialNumber: String = "",
    var productInfo: String = ""
): Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(productCount)
        parcel.writeString(serialNumber)
        parcel.writeString(productInfo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Items> {
        override fun createFromParcel(parcel: Parcel): Items {
            return Items(parcel)
        }

        override fun newArray(size: Int): Array<Items?> {
            return arrayOfNulls(size)
        }
    }

}