package edu.susu.crypto;

import edu.susu.database.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class Session {
    private User user;
    private String token;
    public int tokenUsageCount;
    private final int tokenUsageLimit = 10;
    private LocalDateTime creationTime;
    private LocalDateTime expireTime;

    public Session(User user, long timeToLiveMinutes) {
        this.user = user;
        creationTime = LocalDateTime.now();
        expireTime = creationTime.plusMinutes(timeToLiveMinutes);
        tokenUsageCount = 0;
        updateToken();
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public boolean isExpired() {
       LocalDateTime currentTime = LocalDateTime.now();
       return currentTime.isAfter(expireTime);
    }

    public void extend(long minutes) {
        expireTime = LocalDateTime.now().plusMinutes(minutes);
    }

    public String getToken() {
        if (tokenUsageCount > tokenUsageLimit) {
            updateToken();
            tokenUsageCount = 0;
        }
        return token;
    }

    public void updateToken() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (user.getUsername() + LocalDateTime.now().toString()).getBytes();
            byte[] hash = messageDigest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for(byte b : hash)
                sb.append(String.format("%02x", b));
            token = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        return;
    }
}
