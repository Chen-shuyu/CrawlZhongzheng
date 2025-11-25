package com.taifex.entity;


/**
 * Entity for the listing page items.
 */
public class AnnouncementSummary {

    private String date;
    private String title;
    private String category;
    private String unit;
    private String link;

    public AnnouncementSummary() {
    }

    public AnnouncementSummary(String date, String title, String category, String unit, String link) {
        this.date = date;
        this.title = title;
        this.category = category;
        this.unit = unit;
        this.link = link;
    }

    // Getters / Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
