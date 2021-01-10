package org.kascoder.jira;

import org.tinylog.Logger;
import picocli.CommandLine;

import javax.swing.*;
import java.io.File;
import java.math.BigInteger;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

@CommandLine.Command(name = "")
public class EntryPoint implements Runnable {
    public static void main(String[] args) {
        new CommandLine(new EntryPoint())
                .setUseSimplifiedAtFiles(true)
                .execute(args);
    }

    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private File file;

    @CommandLine.Option(names = "--server", required = true)
    private String serverUrl;

    @CommandLine.Option(names = {"-u", "--username"}, required = true)
    private String username;

    @CommandLine.Option(names = {"-p", "--password"}, interactive = true)
    private String password;

    @CommandLine.Option(names = "--prefix")
    private String prefix;

    @CommandLine.Option(names = "--default-image-comment")
    private String defaultImageComment;

    @CommandLine.Option(names = "--default-video-comment")
    private String defaultVideoComment;

    @Override
    public void run() {
        var authentication = new PasswordAuthentication(username, password.toCharArray());
        if (!file.exists() || file.isDirectory()) {
            Logger.info("File path isn't provided or points to the directory. Exit");
            return;
        }

        var fileName = file.getName();
        var tokens = fileName.split("\\.");
        var issueKey = tokens[0];
        if (issueKey.contains("#")) {
            issueKey = issueKey.substring(0, issueKey.indexOf("#"));
        }
        var fileExtension = tokens[1];

        Logger.info("File Name: {}", fileName);
        Logger.info("File Extension: {}", fileExtension);
        Logger.info("Issue Key: {}", issueKey);

        if (Utils.isNotBlank(prefix) && !issueKey.startsWith(prefix)) {
            Logger.error("Issue key doesn't start with required prefix - {}. Exit", prefix);
            return;
        }

        if (!Utils.isImgFile(fileExtension) && !Utils.isVidFile(fileExtension)) {
            Logger.error("File should be either video or image. Exit");
            return;
        }

        String defaultComment = Utils.isImgFile(fileExtension) ? defaultImageComment : defaultVideoComment;
        if (Utils.isNotBlank(defaultComment)) {
            Logger.info("Default Comment: {}", defaultComment);
        }

        String comment = JOptionPane.showInputDialog("Comment", defaultComment);
        if (Utils.isBlank(comment)) {
            Logger.warn("Comment is empty. Exit");
            return;
        }

        var boundary = new BigInteger(35, new Random()).toString();
        var basicAuth = Utils.basicAuth(authentication.getUserName(), new String(authentication.getPassword()));
        var attachFileToIssueReq = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/rest/api/3/issue/%s/attachments", serverUrl, issueKey)))
                .headers("Content-Type", "multipart/form-data;boundary=" + boundary, "X-Atlassian-Token", "no-check", "Authorization", basicAuth)
                .POST(Utils.ofMultipartData(Map.of("file", file.toPath()), boundary))
                .build();

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        try {
            var response = httpClient.send(attachFileToIssueReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() > 300) {
                throw new Exception(response.body());
            }
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        var body = "{\"body\": \"%s [^%s]\"}";
        var createCommentInIssueReq = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://%s/rest/api/2/issue/%s/comment", serverUrl, issueKey)))
                .headers("Content-Type", "application/json", "X-Atlassian-Token", "no-check", "Authorization", basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(String.format(body, comment, fileName)))
                .build();

        try {
            var response = httpClient.send(createCommentInIssueReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() > 300) {
                throw new Exception(response.body());
            }
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        {
            var message = String.format("Do you want to delete source file: '%s'?", file.getPath());
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, message)) {
                Logger.info("Removing file.....");
                if (!file.delete()) {
                    Logger.warn("File wasn't removed");
                } else {
                    Logger.info("File was removed");
                }
            }
        }
    }
}
