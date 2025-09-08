package com.wanzami.backend.model;

import java.util.List;

public class Movie {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private List<VideoQuality> videoUrls;
    private List<Subtitle> subtitles;

    // constructor
    public Movie(String id, String title, String description, String thumbnailUrl,
                 List<VideoQuality> videoUrls, List<Subtitle> subtitles) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrls = videoUrls;
        this.subtitles = subtitles;
    }

    // getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<VideoQuality> getVideoUrls() { return videoUrls; }
    public List<Subtitle> getSubtitles() { return subtitles; }
}
