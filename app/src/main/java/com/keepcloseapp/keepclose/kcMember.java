package com.keepcloseapp.keepclose;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;


@DynamoDBTable(tableName = "User-GPS")
public class kcMember
{
    private String UserID;
    private String GPS;
    private String Group;
    private String Lat;
    private String Lng;

    @DynamoDBHashKey(attributeName = "UserID")
    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        this.UserID = userID;
    }

    @DynamoDBAttribute(attributeName = "GPS")
    public String getGPS() {
        return GPS;
    }

    public void setGPS(String gps) {
        this.GPS = gps;
    }

    @DynamoDBAttribute(attributeName = "Group")
    public String getGroup() {
        return Group;
    }

    public void setGroup(String group) {
        this.Group = group;
    }

    @DynamoDBAttribute(attributeName = "Lat")
    public String getLat() {
        return Lat;
    }

    public void setLat(String lat) {
        this.Lat = lat;
    }

    @DynamoDBAttribute(attributeName = "Lng")
    public String getLng() {
        return Lng;
    }

    public void setLng(String lng) {
        this.Lng = lng;
    }
}