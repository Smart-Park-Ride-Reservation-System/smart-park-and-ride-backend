package com.project.smartparking.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Component
public class FirebaseConfig {

    @Value("${firebase.credentials.path:classpath:firebase-service-account.json}")
    private Resource credentialsResource;

    @Value("${firebase.credentials.base64:}")
    private String credentialsBase64;

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            try (InputStream serviceAccount = resolveCredentialsStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to initialize Firebase — check firebase.credentials.base64/firebase.credentials.path and firebase.database.url", e);
        }
    }

    private InputStream resolveCredentialsStream() throws IOException {
        if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(credentialsBase64));
        }
        return credentialsResource.getInputStream();
    }
}