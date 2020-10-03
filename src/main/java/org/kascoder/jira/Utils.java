package org.kascoder.jira;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class Utils {
    private Utils() {
    }

    public static boolean validateArgs(String[] args) {
        if (args.length < 4) {
            System.out.println("Incorrect number of arguments. Expected: at least 4, Actual: " + args.length);
            System.out.println("Arguments template: %path_to_file% %username% %api_key% %api_base_website% [%default_comment%]");
            return false;
        }

        return true;
    }

    public static HttpRequest.BodyPublisher ofMultipartData(Map<Object, Object> data, String boundary) {
        try {
            var byteArrays = new ArrayList<byte[]>();
            byte[] separator = ("--" + boundary
                    + "\r\nContent-Disposition: form-data; name=")
                    .getBytes(StandardCharsets.UTF_8);
            for (Map.Entry<Object, Object> entry : data.entrySet()) {
                byteArrays.add(separator);

                if (entry.getValue() instanceof Path) {
                    var path = (Path) entry.getValue();
                    String mimeType = Files.probeContentType(path);
                    byteArrays.add(("\"" + entry.getKey() + "\"; filename=\""
                            + path.getFileName() + "\"\r\nContent-Type: " + mimeType
                            + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    byteArrays.add(Files.readAllBytes(path));
                    byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
                } else {
                    byteArrays.add(
                            ("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue()
                                    + "\r\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            byteArrays
                    .add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
            return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Optional<File> getFile(String[] args) {
        if (args.length < 1) {
            return Optional.empty();
        }

        var videoFile = new File(args[0]);
        if (!videoFile.exists() || videoFile.isDirectory()) {
            return Optional.empty();
        }

        var fileName = videoFile.getName().toLowerCase();
        if (!fileName.startsWith("proof-") || !fileName.endsWith(".mp4")) {
            return Optional.empty();
        }

        return Optional.of(videoFile);
    }

    public static Optional<PasswordAuthentication> parseAuthentication(String[] args) {
        if (args.length < 3) {
            return Optional.empty();
        }

        return Optional.of(new PasswordAuthentication(args[1], args[2].toCharArray()));
    }

    public static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
