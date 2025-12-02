package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.utility.CrawlerException;
import com.taifex.utility.MailService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class App {

    // 定義日期格式
    private static final DateTimeFormatter FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  中正國中公告爬蟲 v2.0");
        System.out.println("========================================\n");

        final String url = "https://www.ccjhs.tp.edu.tw/category/news/";

        try {
            // 1. 取得目標日期
            String targetDate = (args.length == 1) ? formatDate(args[0]) : getTodayString();
            System.out.println("DATE：" + targetDate);

            // 2. 抓取公告列表（加入錯誤處理）
            List<AnnouncementSummary> summaries;
            try {
                summaries = Crawler.fetchSummaries(targetDate, url);
            } catch (CrawlerException e) {
                System.err.println("\n========================================");
                System.err.println("錯誤：無法抓取公告列表");
                System.err.println("原因：" + e.getMessage());
                System.err.println("========================================");

                // 可以選擇：
                // 1. 直接結束程式
                return;

                // 2. 或發送錯誤通知郵件後結束
                // sendErrorNotification(e);
                // return;
            }

            // 檢查是否有符合的公告
            if (summaries.isEmpty()) {
                System.out.println("\n========================================");
                System.out.println("沒有符合條件的公告");
                System.out.println("========================================");
                return;
            }

            System.out.println("\n========================================");
            System.out.println("找到 " + summaries.size() + " 筆符合的公告");
            System.out.println("========================================\n");

            // 3. 建立郵件服務（只建立一次）
            MailService mailService = new MailService("SendMail", "16Password");

            // 4. 處理每個公告（改進：一個失敗不影響其他）
            int successCount = 0;
            int failureCount = 0;
            List<String> failedAnnouncements = new ArrayList<>();

            for (int i = 0; i < summaries.size(); i++) {
                AnnouncementSummary summary = summaries.get(i);
                System.out.println("\n----- 處理公告 " + (i + 1) + "/" + summaries.size() + " -----");
                System.out.println("標題: " + summary.getTitle());
                System.out.println("連結: " + summary.getLink());

                try {
                    // 4.1 抓取詳細內容
                    AnnouncementDetail detail = Crawler.fetchAnnouncementDetail(summary.getLink());

                    // 4.2 發送郵件
//                    sendAnnouncements(detail, mailService, "TOMAIL");

                    successCount++;
                    System.out.println("✓ 處理成功");

                } catch (CrawlerException e) {
                    failureCount++;
                    failedAnnouncements.add(summary.getTitle());
                    System.err.println("✗ 處理失敗: " + e.getMessage());
                    // 繼續處理下一個，不要中斷

                } catch (Exception e) {
                    failureCount++;
                    failedAnnouncements.add(summary.getTitle());
                    System.err.println("✗ 未預期的錯誤: " + e.getMessage());
                    e.printStackTrace();
                    // 繼續處理下一個
                }
            }

            // 5. 顯示最終統計
            System.out.println("\n========================================");
            System.out.println("  執行摘要");
            System.out.println("========================================");
            System.out.println("總公告數: " + summaries.size());
            System.out.println("成功處理: " + successCount + " 筆");
            System.out.println("失敗處理: " + failureCount + " 筆");

            if (failureCount > 0) {
                System.out.println("\n失敗的公告：");
                for (String title : failedAnnouncements) {
                    System.out.println("  - " + title);
                }
            }

            System.out.println("========================================\n");

            // 6. 如果有失敗，返回非零的退出碼
            if (failureCount > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("程式執行失敗");
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);
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
     * 寄出郵件（改進：檢查附件是否存在）
     */
    public static void sendAnnouncements(AnnouncementDetail announcementDetail, MailService mailService, String to) throws Exception {

        if (announcementDetail == null) {
            System.err.println("⚠ 公告詳細內容為空，跳過郵件發送");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今日符合關鍵字的公告如下：\n\n");

        sb.append("【主旨】").append(announcementDetail.getSubject()).append("\n");
        sb.append("【日期】").append(announcementDetail.getDate()).append("\n");

        // 改進：檢查附件是否存在
        if (announcementDetail.getAttachments() != null &&
                !announcementDetail.getAttachments().isEmpty()) {

            sb.append("【網址】").append(announcementDetail.getAttachments().get(0).getUrl()).append("\n");

            // 如果有多個附件，列出所有
            if (announcementDetail.getAttachments().size() > 1) {
                sb.append("\n其他附件：\n");
                for (int i = 1; i < announcementDetail.getAttachments().size(); i++) {
                    sb.append("  ").append(i).append(". ")
                            .append(announcementDetail.getAttachments().get(i).getName())
                            .append(" - ")
                            .append(announcementDetail.getAttachments().get(i).getUrl())
                            .append("\n");
                }
            }
        } else {
            sb.append("【網址】（無附件）\n");
        }

        sb.append("\n");

        try {
            mailService.sendMail(
                    to,
                    "每日公告通知",
                    sb.toString()
            );
            System.out.println("✓ 郵件發送成功");
        } catch (Exception e) {
            System.err.println("✗ 郵件發送失敗: " + e.getMessage());
            throw e;  // 重新拋出，讓上層知道失敗
        }
    }

    /**
     * 發送錯誤通知（選用）
     */
    private static void sendErrorNotification(Exception e) {
        try {
            MailService mailService = new MailService("SendMail", "16Password");

            String errorMessage = String.format(
                    "公告爬蟲執行失敗\n\n" +
                            "錯誤訊息：%s\n" +
                            "發生時間：%s\n",
                    e.getMessage(),
                    LocalDate.now().toString()
            );

            mailService.sendMail(
                    "ADMIN@MAIL",
                    "【錯誤】公告爬蟲執行失敗",
                    errorMessage
            );
        } catch (Exception mailError) {
            System.err.println("無法發送錯誤通知郵件: " + mailError.getMessage());
        }
    }

}
