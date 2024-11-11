package cn.threeant.lottery;

import cn.threeant.lottery.domian.Lottery;
import cn.threeant.lottery.domian.LotteryResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.mail.*;
import javax.mail.internet.*;


public class LotteryReminder {

    private static final String CSV_FILE_PATH = "ssq_record.csv";
    private static final String SSQ_API_URL = System.getenv("SSQ_API_URL");
    private static final String EMAIL_HOST = System.getenv("EMAIL_HOST");
    private static final String EMAIL_PORT = System.getenv("EMAIL_PORT");
    private static final String EMAIL_USER = System.getenv("EMAIL_USER");
    private static final String EMAIL_PASS = System.getenv("EMAIL_PASS");

    private LotteryReminder() {
        // default implementation ignored
    }

    public static Lottery getLatestLotteryResult() {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = getHttpGet();
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String result = EntityUtils.toString(response.getEntity(), "UTF-8");

                JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
                Gson gson = new Gson();
                Type resultListType = new TypeToken<List<Lottery>>() {}.getType();
                List<Lottery> resultList = gson.fromJson(jsonObject.get("result"), resultListType);

                return resultList.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HttpGet getHttpGet() {
        HttpGet httpGet = new HttpGet(SSQ_API_URL);
        httpGet.addHeader("Connection", "keep-alive");
        httpGet.addHeader("Cache-Control", "max-age=0");
        httpGet.addHeader("Host", "www.cwl.gov.cn");
        httpGet.addHeader("Cookie", "HMF_CI=37998609adfce8602f6c6d82c45423fe78bc96adc7914485f2c536c109c2793973eb89940ab3cb34403cb5aeeb7a485be983dd0be52895e256572bce7f6969d26d");
        httpGet.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0) ");
        httpGet.addHeader("Accept", "*/*");
        return httpGet;
    }

    public static void compareResults(Lottery latestResult) {
        Set<String> winningRed = new HashSet<>(List.of(latestResult.getRed().split(",")));
        String winningBlue = latestResult.getBlue();
        List<LotteryResult> list = new ArrayList<>();
        // 读取CSV文件
        try (BufferedReader br = Files.newBufferedReader(Paths.get(CSV_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\\|");
                String nickName = fields[1];
                String userEmail = fields[2];
                String userReds = fields[3];
                Set<String> userRed = new HashSet<>(Arrays.asList(userReds.split(",")));
                String userBlue = fields[4];
                long redMatches = winningRed.stream().filter(userRed::contains).count();
                boolean blueMatch = winningBlue.equals(userBlue);
                String info = getWinningInfo(blueMatch, redMatches);
                LotteryResult lotteryResult = new LotteryResult(nickName, userEmail, userReds, userBlue, info);
                list.add(lotteryResult);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 按email分组
        Map<String, List<LotteryResult>> groupedByEmail = list.stream()
                .collect(Collectors.groupingBy(LotteryResult::getEmail));

        // 按分组发送邮件
        for (Map.Entry<String, List<LotteryResult>> entry : groupedByEmail.entrySet()) {
            List<LotteryResult> lotteryResult = entry.getValue();
            String nickName = lotteryResult.get(0).getNickName();
            String toUser = lotteryResult.get(0).getEmail();

            StringBuilder emailBody = new StringBuilder();
            emailBody.append(nickName)
                    .append(",您好！\n本期")
                    .append(latestResult.getCode())
                    .append(",开奖号码：")
                    .append(latestResult.getRed())
                    .append("-")
                    .append(latestResult.getBlue())
                    .append("\n您的中奖信息如下：\n");

            sendEmails(toUser, emailBody, lotteryResult);
        }

    }

    private static String getWinningInfo(boolean blueMatch, long redMatches) {
        if (blueMatch && redMatches == 6) return "恭喜您本次投注中得一等奖。";
        if (!blueMatch && redMatches == 6) return "恭喜您本次投注中得二等奖。";
        if (blueMatch && redMatches == 5) return "恭喜您本次投注中得三等奖。";
        if ((blueMatch && redMatches == 4) || (!blueMatch && redMatches == 5)) return "恭喜您本次投注中得四等奖。";
        if ((blueMatch && redMatches == 3) || (!blueMatch && redMatches == 4)) return "恭喜您本次投注中得五等奖。";
        if (blueMatch) return "恭喜您本次投注中得六等奖。";
        return "很遗憾您本次投注未中奖。";
    }

    private static void sendEmails(String toUser, StringBuilder emailBody, List<LotteryResult> lotteryResultList) {
        Properties props = new Properties();
        props.put("mail.smtp.host", EMAIL_HOST);
        props.put("mail.smtp.port", EMAIL_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USER, EMAIL_PASS);
            }
        });

        for (LotteryResult lotteryResult : lotteryResultList) {
            emailBody.append(lotteryResult.getRed())
                    .append("-")
                    .append(lotteryResult.getBlue())
                    .append(":")
                    .append(lotteryResult.getWinningInfo())
                    .append("\n");
        }
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toUser));
            message.setSubject("双色球开奖通知");
            message.setText(emailBody.toString());
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
