package org.kascoder.jira;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Utils {
    private Utils() {
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

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isImgFile(String extension) {
        return Utils.isNotBlank(extension) && Set.of("png", "jpg").contains(extension);
    }

    public static boolean isVidFile(String extension) {
        return Utils.isNotBlank(extension) && Objects.equals("mp4", extension);
    }

    public static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
