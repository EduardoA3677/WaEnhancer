package com.wmods.wppenhacer.xposed.core.db;

/**
 * Data class for message items in history
 */
public class MessageItem {
    private final long id;
    private final String message;
    private final long timestamp;

    public MessageItem(long id, String message, long timestamp) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MessageItem{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
