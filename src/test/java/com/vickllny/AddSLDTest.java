package com.vickllny;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AddSLDTest {
    private static final String GEOSERVER_URL = "http://localhost:8080/geoserver";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "geoserver";

    @Test
    public void test() {
        try {
            File sldFile = new File("/Users/vickllny/gis/file.sld");
            uploadSLDFile(sldFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void uploadSLDFile(File file) throws IOException {
        String urlString = GEOSERVER_URL + "/rest/styles";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/vnd.ogc.sld+xml");
        String auth = USERNAME + ":" + PASSWORD;
        byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + new String(encodedAuth));

        try (OutputStream outputStream = connection.getOutputStream();
             FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 201) {
            System.out.println("SLD file uploaded successfully");
        } else {
            System.out.println("Failed to upload SLD file: " + responseCode);
        }

        connection.disconnect();
    }

}
