# Real-Time WhatsApp-Style Chat (Socket.IO)

A cross-platform real-time chat system featuring an **Android Client** and a **Node.js Web Client**. The system supports private messaging, group chats, automatic contact discovery, and a customizable Dark Theme.

## 🚀 Features

- **Real-Time Communication:** Powered by Socket.IO for instant message delivery.
- **WhatsApp-Style Side Panel:** Easily switch between Global Chat, Private Chats, and Groups.
- **Auto-Contact Discovery:** Anyone who sends a message is automatically added to your contact list for future private chats.
- **Group Chats:** Create or join custom rooms for group discussions.
- **Dark Mode Support:** Smooth transition between Light and Dark themes on both Android and Web.
- **Persistent Storage:** Contacts and theme preferences are saved locally on your device.

## 🛠️ Tech Stack

- **Backend:** Node.js, Express, Socket.IO
- **Android:** Java, ConstraintLayout, Navigation Drawer, Material Design
- **Web:** HTML5, CSS3 (Modern Flexbox/CSS Variables), Vanilla JavaScript

## 📦 Project Structure

```
SocketIO/
├── app/                # Android Application Source
├── chat-server/        # Node.js Server & Web Client
│   ├── public/         # Web Client (index.html)
│   └── server.js       # Backend Logic
├── build.gradle.kts    # Project Build Config
└── README.md           # You are here
```

## ⚙️ Setup Instructions

### 1. Backend Setup
1. Navigate to the `chat-server` directory.
2. Install dependencies: `npm install`
3. Start the server: `node server.js`
4. The server will run on port `3000`. Access the web client at `http://localhost:3000`.

### 2. Android Setup
1. Open the root `SocketIO` folder in Android Studio.
2. Locate `MainActivity.java`.
3. Update the `SERVER_URL` variable with your computer's local IP address (e.g., `http://123.456.789.000:3000` — obviously fake, use your own).
4. Build and run on your physical device.

### 3. Networking & Firewall
- Ensure both your laptop and phone are on the **same Wi-Fi network**.
- **Important:** Allow Port `3000` through your computer's firewall to enable mobile connectivity.
