package edu.susu.crypto;

import edu.susu.database.User;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class Session {
    private User user;
    //private String key;
    private LocalDateTime creationTime;
    private LocalDateTime expireTime;

    public Session(User user, long timeToLiveMinutes) {
        this.user = user;
        creationTime = LocalDateTime.now();
        expireTime = creationTime.plusMinutes(timeToLiveMinutes);
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
}
