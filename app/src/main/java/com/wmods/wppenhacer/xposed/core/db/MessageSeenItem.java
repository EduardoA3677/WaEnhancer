package com.wmods.wppenhacer.xposed.core.db;

/**
 * Data class for message seen items
 */
public class MessageSeenItem {
    private final String jid;
    private final String messageId;
    public final boolean viewed;

    public MessageSeenItem(String jid, String messageId, boolean viewed) {
        this.jid = jid;
        this.messageId = messageId;
        this.viewed = viewed;
    }

    public String getJid() {
        return jid;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isViewed() {
        return viewed;
    }

    @Override
    public String toString() {
        return "MessageSeenItem{" +
                "jid='" + jid + '\'' +
                ", messageId='" + messageId + '\'' +
                ", viewed=" + viewed +
                '}';
    }
}
