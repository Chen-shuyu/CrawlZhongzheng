package com.taifex.entity;

import java.util.List;

/**
 * Entity for the detail page content.
 */
public class AnnouncementDetail {

    private String subject;
    private String date;
    private String unit;
    private String category;
    private String level;
    private String views;
    private List<String> content; // each <p> stored as one entry
    private List<Attachment> attachments;   //  新增附件欄位

    public AnnouncementDetail() {
    }

    public AnnouncementDetail(
            String subject,
            String date,
            String unit,
            String category,
            String level,
            String views,
            List<String> content,
            List<Attachment> attachments
    ) {
        this.subject = subject;
        this.date = date;
        this.unit = unit;
        this.category = category;
        this.level = level;
        this.views = views;
        this.content = content;
        this.attachments = attachments;
    }

    // Getters / Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
