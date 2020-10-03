package org.kascoder.jira;

import javax.swing.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

public class Launcher {
    // C:\Users\xlggc\Desktop\Tmp\video\PROOF-4439.mp4 pkasper@subject-7.com Pavel1997 subject7.atlassian.net

    public static void main(String[] args) {
        if (!Utils.validateArgs(args)) {
            return;
        }

        var videoFile = Utils.getFile(args);
        String apiBaseWebsite = args[3];
        var authentication = Utils.parseAuthentication(args)
                .orElse(null);
        if (videoFile.isEmpty() || authentication == null) {
            return;
        }

        String defaultComment = "Verification Video";
        if (args.length >= 5) {
            defaultComment = args[4];
        }

        String comment = JOptionPane.showInputDialog("Comment", defaultComment);

        var file = videoFile.get();
        var fileName = file.getName();
        var issueKey = fileName.substring(0, fileName.indexOf("."));

        var boundary = new BigInteger(256, new Random()).toString();
        var basicAuth = Utils.basicAuth(authentication.getUserName(), new String(authentication.getPassword()));
        var attachFileToIssueReq = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/rest/api/3/issue/%s/attachments", apiBaseWebsite, issueKey)))
                .headers("Content-Type", "multipart/form-data;boundary=" + boundary, "X-Atlassian-Token", "no-check", "Authorization", basicAuth)
                .POST(Utils.ofMultipartData(Map.of("file", file.getPath()), boundary))
                .build();

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        try {
            var response = httpClient.send(attachFileToIssueReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 500) {
                //throw new Exception(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        var body = "\"body\": {\n" +
                "    \"type\": \"doc\",\n" +
                "    \"version\": 1,\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"type\": \"paragraph\",\n" +
                "        \"content\": [\n" +
                "          {\n" +
                "            \"text\": \"%s[^%s].\",\n" +
                "            \"type\": \"text\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        var createCommentInIssueReq = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/rest/api/3/issue/%s/comment", apiBaseWebsite, issueKey)))
                .headers("Content-Type", "application/json", "X-Atlassian-Token", "no-check", "Authorization", basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"body\": {\"type\": \"text\", \"text\": \"%s[^%s]\"}}", comment, fileName)))
                .build();


        try {
            var response = httpClient.send(createCommentInIssueReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 500) {
                throw new Exception(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Do you want to delete source file?")) {
            file.delete();
        }
    }
}
