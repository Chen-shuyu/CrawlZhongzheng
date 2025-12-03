package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.entity.Attachment;
import com.taifex.utility.CrawlerException;
import com.taifex.utility.LinePushMessage;
import com.taifex.utility.MailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class App {

    // Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(App.class);

    // 定義日期格式
    private static final DateTimeFormatter FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MAIN_PATH = "D:\\ShuYuChen\\3.Practice\\3.1.Project\\CrawlZhongzheng\\";

    public static void main(String[] args) throws Exception {
        logger.info("========================================");
        logger.info("  中正國中公告爬蟲 v2.0");
        logger.info("========================================\n");

        String targetDate = "";
        String type = "";

        /**
         *  無參數：當日、關鍵字
         *  1參數：指定日、關鍵字
         *  2參數：指定日、全部
         * */
        if (args.length == 0) {
            targetDate = getTodayString();
            type = "";
        } else if (args.length == 1) {
            targetDate = formatDate(args[0]);
            type = "";
        } else if (args.length == 2) {
            targetDate = formatDate(args[0]);
            type = "ALL";
        } else {
            return;
        }

        // 判斷該日期下是否有抓到URL，原本是為了抓到google表單後，就不繼續跑了
        String urlFilePath = MAIN_PATH + "url_" + targetDate + ".txt";
        File file = new File(urlFilePath);
        if (file.exists() && file.length() > 0) {
            return;
        }

        final String url = "https://www.ccjhs.tp.edu.tw/category/news/";

        try {
            // 1. 取得目標日期
            logger.info("DATE：" + targetDate);

            // 2. 抓取公告列表（加入錯誤處理）
            List<AnnouncementSummary> summaries;
            try {
                summaries = Crawler.fetchSummaries(targetDate, url, type);
            } catch (CrawlerException e) {
                logger.error("\n========================================");
                logger.error("錯誤：無法抓取公告列表");
                logger.error("原因：" + e.getMessage());
                logger.error("========================================");

                // 可以選擇：
                // 1. 直接結束程式
                return;

                // 2. 或發送錯誤通知郵件後結束
                // sendErrorNotification(e);
                // return;
            }

            // 檢查是否有符合的公告
            if (summaries.isEmpty()) {
                logger.info("\n========================================");
                logger.info("沒有符合條件的公告");
                logger.info("========================================");
                return;
            }

            logger.info("\n========================================");
            logger.info("找到 " + summaries.size() + " 筆符合的公告");
            logger.info("========================================\n");

            // 3. 處理每個公告（改進：一個失敗不影響其他）
            int successCount = 0;
            int failureCount = 0;
            List<String> failedAnnouncements = new ArrayList<>();

            List<AnnouncementDetail> sendMSGs = new ArrayList<AnnouncementDetail>();

            for (int i = 0; i < summaries.size(); i++) {
                AnnouncementSummary summary = summaries.get(i);
                logger.info("\n----- 處理公告 " + (i + 1) + "/" + summaries.size() + " -----");
                logger.info("標題: " + summary.getTitle());
                logger.info("連結: " + summary.getLink());

                try {
                    // 3.1 抓取詳細內容
                    AnnouncementDetail detail = Crawler.fetchAnnouncementDetail(summary.getLink());

                    if (detail == null) {
                        return;
                    }
                    sendMSGs.add(detail);

                    successCount++;
                    logger.info("✓ 處理成功");

                } catch (CrawlerException e) {
                    failureCount++;
                    failedAnnouncements.add(summary.getTitle());
                    logger.error("✗ 處理失敗: " + e.getMessage());
                    // 繼續處理下一個，不要中斷

                } catch (Exception e) {
                    failureCount++;
                    failedAnnouncements.add(summary.getTitle());
                    logger.error("✗ 未預期的錯誤: " + e.getMessage());
                    e.printStackTrace();
                    // 繼續處理下一個
                }
            }

            // 3.2 整理訊息內容
            StringBuilder msgStringBuider = new StringBuilder();
            if (sendMSGs.size() == 0) {
                return;
            }

            msgStringBuider.append("今日符合關鍵字的公告如下：\n\n");
            for (AnnouncementDetail announcementDetail : sendMSGs) {
                msgStringBuider.append("【日期】").append(announcementDetail.getDate()).append("\n");
                msgStringBuider.append("【主旨】").append(announcementDetail.getSubject()).append("\n");
                if (!announcementDetail.getAttachments().isEmpty()) {
                    msgStringBuider.append("【網址】").append(announcementDetail.getAttachments().get(0).getUrl()).append("\n\n");
                }
                msgStringBuider.append("======================\n\n");
            }

            // 3.3 發Line
            LinePushMessage.broadcastMessage(msgStringBuider.toString());

            // 3.4 發Email
            String mailToFilePath = MAIN_PATH + "MailTo.txt";
            String mailToString = readEmailsToString(mailToFilePath);

            MailService mailService = new MailService("SendMaill", "16Password");
            sendAnnouncements(msgStringBuider.toString(), mailService, mailToString);

            // 4. 將URL寫入檔案
            try (FileWriter writer = new FileWriter(urlFilePath)) {
                if (type.equals("ALL")) {
                    writer.write(type + "\n");
                }
                for (AnnouncementDetail sendMSG : sendMSGs) {
                    if (!sendMSG.getAttachments().isEmpty()) {
                        for (Attachment attachment : sendMSG.getAttachments()) {

                            writer.write(attachment.getUrl() + "\n");
                        }
                        logger.info("URL 已寫入到檔案！");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            // 5. 顯示最終統計
            logger.info("\n========================================");
            logger.info("  執行摘要");
            logger.info("========================================");
            logger.info("總公告數: " + summaries.size());
            logger.info("成功處理: " + successCount + " 筆");
            logger.info("失敗處理: " + failureCount + " 筆");

            if (failureCount > 0) {
                logger.info("\n失敗的公告：");
                for (String title : failedAnnouncements) {
                    logger.info("  - " + title);
                }
            }
            logger.info("========================================\n");

            // 6. 如果有失敗，返回非零的退出碼
            if (failureCount > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("\n========================================");
            logger.error("程式執行失敗");
            logger.error("========================================");
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
    public static void sendAnnouncements(String announcementDetails, MailService mailService, String to) throws Exception {

        if (announcementDetails == null) {
            logger.error("⚠ 公告詳細內容為空，跳過郵件發送");
            return; // no data
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今日符合關鍵字的公告如下：\n\n");

        mailService.sendMail(
                to,
                "(非社交工程)中正國中 每日公告通知",
                announcementDetails
        );
    }

    public static String readEmailsToString(String filePath) throws IOException {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean first = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append(line);
                    first = false;
                }
            }
        }
        return result.toString();
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
            logger.error("無法發送錯誤通知郵件: " + mailError.getMessage());
        }
    }

}
