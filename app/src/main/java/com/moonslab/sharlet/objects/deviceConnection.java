package com.moonslab.sharlet.objects;

public class deviceConnection {
    private String payload;
    private String user;
    private String photo;
    private String ssid;
    private String link_speed;

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getLink_speed() {
        return link_speed;
    }

    public void setLink_speed(String link_speed) {
        this.link_speed = link_speed;
    }
}
