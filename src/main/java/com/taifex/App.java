package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.utility.MailService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class App {

    // 定義日期格式
    private static final DateTimeFormatter FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
//        final String url = "https://www.ccjhs.tp.edu.tw/category/news/";
        final String url = "https://www.ccjhs.tp.edu.tw/category/news/?keyword=%E7%90%83%E5%A0%B4";

        String targetDate = (args.length == 1) ? formatDate(args[0]) : getTodayString();
        System.out.println("DATE：" + targetDate);

        List<AnnouncementSummary> summaries = Crawler.fetchSummaries(targetDate, url);
        for (AnnouncementSummary s : summaries) {
            System.out.println("Summary: " + s.getTitle());
            System.out.println("  Link: " + s.getLink());

            AnnouncementDetail detail = Crawler.fetchAnnouncementDetail(s.getLink());

            if (detail != null) {
                System.out.println("  Subject: " + detail.getSubject());
                System.out.println("  Content: " + detail.getContent());
                System.out.println("  Attachments: " + detail.getAttachments());
                System.out.println("  GOOGLE_URL: " + detail.getAttachments().get(0).getUrl());
            }
            MailService mailService = new MailService("SendMaill", "16Password");
            sendAnnouncements(detail, mailService, "TOMAILL");
        }

    }

    /**
     * 1. 將 YYYYMMDD 字串轉換為 YYYY-MM-DD 字串
     * 例如: "20241125" → "2024-11-25"
     */
    public static String formatDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.isEmpty()) {
            throw new IllegalArgumentException("日期字串不能為空");
        }

        if (yyyymmdd.length() != 8) {
            throw new IllegalArgumentException("日期格式必須是 YYYYMMDD (8位數字)");
        }

        try {
            // 解析 YYYYMMDD 格式
            LocalDate date = LocalDate.parse(yyyymmdd, FORMATTER_YYYYMMDD);
            // 轉換為 YYYY-MM-DD 格式
            return date.format(FORMATTER_YYYY_MM_DD);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("無效的日期: " + yyyymmdd, e);
        }
    }

    /**
     * 2. 取得今日日期的 YYYY-MM-DD 字串
     * 例如: "2024-11-25"
     */
    public static String getTodayString() {
        LocalDate today = LocalDate.now();
        return today.format(FORMATTER_YYYY_MM_DD);
    }

    /**
     * 寄出郵件
     */
    public static void sendAnnouncements(AnnouncementDetail announcementDetail, MailService mailService, String to) throws Exception {

        if (announcementDetail == null) {
            return; // no data
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今日符合關鍵字的公告如下：\n\n");


        sb.append("【主旨】").append(announcementDetail.getSubject()).append("\n");
        sb.append("【日期】").append(announcementDetail.getDate()).append("\n");
        sb.append("【網址】").append(announcementDetail.getAttachments().get(0).getUrl()).append("\n\n");


        mailService.sendMail(
                to,
                "每日公告通知",
                sb.toString()
        );
    }

}
