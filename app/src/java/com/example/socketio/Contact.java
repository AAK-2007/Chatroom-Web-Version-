package com.example.socketio;

public class Contact {
    private String name;
    private String roomId;
    private boolean isGroup;

    public Contact(String name, String roomId, boolean isGroup) {
        this.name = name;
        this.roomId = roomId;
        this.isGroup = isGroup;
    }

    public String getName() { return name; }
    public String getRoomId() { return roomId; }
    public boolean isGroup() { return isGroup; }

    @Override
    public String toString() {
        return name + (isGroup ? " (Group)" : "");
    }
}
