package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.entity.Attachment;
import com.taifex.utility.LinePushMessage;
import com.taifex.utility.MailService;

import java.io.*;
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

        final String url = "https://www.ccjhs.tp.edu.tw/category/news/";
//        final String url = "https://www.ccjhs.tp.edu.tw/category/news/?keyword=%E7%90%83%E5%A0%B4";

        String targetDate = "";
        String type = "";

        /**
         *  無參數：當日、關鍵字
         *  1參數：指定日、關鍵字
         *  2參數：指定日、全部
        * */
        if (args.length == 0){
            targetDate = getTodayString();
            type = "";
        }else if(args.length == 1){
            targetDate = formatDate(args[0]);
            type = "";
        }else if(args.length == 2){
            targetDate = formatDate(args[0]);
            type = "ALL";
        }else {
            return;
        }

//        System.out.println("DATE：" + targetDate);

        // 判斷該日期下是否有抓到URL，原本是為了抓到google表單後，就不繼續跑了
        String urlFilePath = "D:\\shuyu\\1.Code\\RUN\\Crawl_Zhongzheng\\url_"+targetDate+".txt";
        File file = new File(urlFilePath);
        if (file.exists() && file.length() > 0) {
            return;
        }


        List<AnnouncementSummary> summaries = Crawler.fetchSummaries(targetDate, url, type);
        List<AnnouncementDetail> sendMSGs = new ArrayList<AnnouncementDetail>();
        for (AnnouncementSummary s : summaries) {

            AnnouncementDetail detail = Crawler.fetchAnnouncementDetail(s.getLink());

            if (detail == null) {
                return;
            }
            sendMSGs.add(detail);

        }

        // 整理訊息內容
        StringBuilder msgStringBuider = new StringBuilder();
        if(sendMSGs.size() == 0){
            return;
        }

        msgStringBuider.append("今日符合關鍵字的公告如下：\n\n");
        for(AnnouncementDetail announcementDetail:sendMSGs){
            msgStringBuider.append("【日期】").append(announcementDetail.getDate()).append("\n");
            msgStringBuider.append("【主旨】").append(announcementDetail.getSubject()).append("\n");
            if(!announcementDetail.getAttachments().isEmpty()){
                msgStringBuider.append("【網址】").append(announcementDetail.getAttachments().get(0).getUrl()).append("\n\n");
            }
            msgStringBuider.append("======================\n\n");
        }

        // 發Line
        LinePushMessage.broadcastMessage(msgStringBuider.toString());
//        LinePushMessage.broadcastMessage("TEST");

        // 發Email
        String mailToFilePath = "D:\\shuyu\\1.Code\\RUN\\Crawl_Zhongzheng\\MailTo.txt";
        String mailToString = readEmailsToString(mailToFilePath);

        MailService mailService = new MailService("SendMaill", "16Password");
        sendAnnouncements(msgStringBuider.toString(), mailService, mailToString );


        try (FileWriter writer = new FileWriter(urlFilePath)) {
            if(type.equals("ALL")){
                writer.write(type+"\n");
            }
            for(AnnouncementDetail sendMSG:sendMSGs){
                if(!sendMSG.getAttachments().isEmpty()){
                    for(Attachment attachment:sendMSG.getAttachments()){

                        writer.write(attachment.getUrl()+"\n");
                    }
                    System.out.println("URL 已寫入到檔案！");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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
    public static void sendAnnouncements(String announcementDetails, MailService mailService, String to) throws Exception {

        if (announcementDetails == null) {
            return; // no data
        }

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

}
