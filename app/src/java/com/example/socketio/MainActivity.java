package com.example.socketio;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_URL = "http://192.168.29.53:3000";
    private static final String PREFS_NAME = "ChatPrefs";
    private static final String CONTACTS_KEY = "contacts";
    private static final String THEME_KEY = "isDarkMode";

    private Socket mSocket;
    private TextView statusText;
    private EditText usernameInput;
    private TextView chatLog;
    private ScrollView chatScrollView;
    private EditText messageInput;
    private Button sendButton;
    private DrawerLayout drawerLayout;
    private TextView currentChatTitle;
    private ListView contactsListView;
    
    private List<Contact> contactList = new ArrayList<>();
    private ArrayAdapter<Contact> contactAdapter;
    private String currentRoom = null; // null means global

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load Theme before setContentView
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(THEME_KEY, false);
        applyTheme(isDarkMode);

        setContentView(R.layout.activity_main);

        // Handle system bars insets properly
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Adjust the main content padding to avoid status bar and navigation bar overlap
            View mainContent = findViewById(R.id.mainContent);
            mainContent.setPadding(
                mainContent.getPaddingLeft(),
                systemBars.top,
                mainContent.getPaddingRight(),
                systemBars.bottom
            );
            
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize UI
        statusText = findViewById(R.id.statusText);
        usernameInput = findViewById(R.id.usernameInput);
        chatLog = findViewById(R.id.chatLog);
        chatScrollView = findViewById(R.id.chatScrollView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        currentChatTitle = findViewById(R.id.currentChatTitle);
        contactsListView = findViewById(R.id.contactsListView);
        
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setChecked(isDarkMode);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(THEME_KEY, isChecked).apply();
            applyTheme(isChecked);
        });

        findViewById(R.id.addContactButton).setOnClickListener(v -> showAddGroupDialog());

        // Setup Contacts List
        loadContacts();
        contactAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactList);
        contactsListView.setAdapter(contactAdapter);
        contactsListView.setOnItemClickListener((parent, view, position, id) -> {
            Contact selected = contactList.get(position);
            joinRoom(selected);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        try {
            mSocket = IO.socket(SERVER_URL);
        } catch (URISyntaxException e) {
            Log.e("MainActivity", "Invalid URL: " + SERVER_URL);
            return;
        }

        setupSocketListeners();
        
        sendButton.setOnClickListener(v -> sendMessage());
        mSocket.connect();
    }

    private void applyTheme(boolean isDark) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupSocketListeners() {
        mSocket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
            statusText.setText("Connected ✓");
            statusText.setTextColor(Color.GREEN);
            if (currentRoom != null) {
                mSocket.emit("joinRoom", currentRoom);
            }
        }));

        mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> runOnUiThread(() -> {
            statusText.setText("Connect Error !");
            statusText.setTextColor(Color.YELLOW);
        }));

        mSocket.on(Socket.EVENT_DISCONNECT, args -> runOnUiThread(() -> {
            statusText.setText("Disconnected ✗");
            statusText.setTextColor(Color.RED);
        }));

        mSocket.on("message", args -> runOnUiThread(() -> {
            JSONObject data = (JSONObject) args[0];
            try {
                String username = data.getString("username");
                String text = data.getString("text");
                String room = data.has("room") ? data.getString("room") : null;
                
                // --- AUTO-ADD CONTACT LOGIC ---
                // If we receive a message from someone, add them to contacts if not already there
                autoAddContact(username);
                
                // Only show if it's the current room (or both are global)
                if ((room == null && currentRoom == null) || (room != null && room.equals(currentRoom))) {
                    appendMessage(username + ": " + text + "\n");
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Error parsing message", e);
            }
        }));

        mSocket.on("system", args -> runOnUiThread(() -> {
            String message = (String) args[0];
            appendSystemMessage("--- " + message + " ---\n");
        }));
    }

    private void autoAddContact(String name) {
        String myName = usernameInput.getText().toString().trim().toLowerCase();
        if (name.equalsIgnoreCase("Anonymous") || name.equalsIgnoreCase(myName)) return;

        boolean exists = false;
        for (Contact c : contactList) {
            if (!c.isGroup() && c.getName().equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            // Generate private room ID
            List<String> participants = new ArrayList<>();
            participants.add(myName);
            participants.add(name.toLowerCase());
            Collections.sort(participants);
            String roomId = "private_" + participants.get(0) + "_" + participants.get(1);

            Contact newContact = new Contact(name, roomId, false);
            contactList.add(newContact);
            contactAdapter.notifyDataSetChanged();
            saveContacts();
        }
    }

    private void sendMessage() {
        String username = usernameInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (username.isEmpty()) {
            username = "Anonymous";
            usernameInput.setText(username);
        }

        if (!message.isEmpty()) {
            JSONObject data = new JSONObject();
            try {
                data.put("username", username);
                data.put("text", message);
                if (currentRoom != null) {
                    data.put("room", currentRoom);
                }
                
                mSocket.emit("message", data);
                messageInput.setText("");
            } catch (JSONException e) {
                Log.e("MainActivity", "Error creating JSON message", e);
            }
        }
    }

    private void joinRoom(Contact contact) {
        currentRoom = contact.getRoomId();
        currentChatTitle.setText(contact.getName());
        chatLog.setText(""); // Clear log when switching
        appendSystemMessage("Joined: " + contact.getName() + "\n");
        
        if (mSocket.connected()) {
            mSocket.emit("joinRoom", currentRoom);
        }
    }

    private void showAddGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Group / Join Room");

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Group Name (e.g. Family)");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);
        layout.addView(nameInput);
        builder.setView(layout);

        builder.setPositiveButton("Join", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            if (!name.isEmpty()) {
                String roomId = "group_" + name.toLowerCase().replaceAll("\\s+", "_");
                
                // Check if exists
                boolean exists = false;
                for(Contact c : contactList) {
                    if(c.getRoomId() != null && c.getRoomId().equals(roomId)) {
                        exists = true;
                        break;
                    }
                }

                if(!exists) {
                    Contact newGroup = new Contact(name, roomId, true);
                    contactList.add(newGroup);
                    contactAdapter.notifyDataSetChanged();
                    saveContacts();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveContacts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (Contact c : contactList) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", c.getName());
                obj.put("roomId", c.getRoomId());
                obj.put("isGroup", c.isGroup());
                array.put(obj);
            } catch (JSONException e) { e.printStackTrace(); }
        }
        prefs.edit().putString(CONTACTS_KEY, array.toString()).apply();
    }

    private void loadContacts() {
        contactList.clear();
        // Always add Global Chat
        contactList.add(new Contact("Global Chat", null, true));
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(CONTACTS_KEY, null);
        if (data != null) {
            try {
                JSONArray array = new JSONArray(data);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    contactList.add(new Contact(
                        obj.getString("name"),
                        obj.optString("roomId", null),
                        obj.getBoolean("isGroup")
                    ));
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    private void appendMessage(String text) {
        chatLog.append(text);
        scrollToBottom();
    }

    private void appendSystemMessage(String text) {
        SpannableString italicText = new SpannableString(text);
        italicText.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), 0);
        chatLog.append(italicText);
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}
