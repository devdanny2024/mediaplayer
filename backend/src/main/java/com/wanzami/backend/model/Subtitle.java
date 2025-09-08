package com.wanzami.backend.model;

public class Subtitle {
    private String lang;
    private String url;

    public Subtitle(String lang, String url) {
        this.lang = lang;
        this.url = url;
    }

    public String getLang() { return lang; }
    public String getUrl() { return url; }
}
