const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    },
    allowEIO3: true // Allow compatibility with older Socket.IO clients (like your Android v2.1.0)
});

// Serve the web client files
app.use(express.static(path.join(__dirname, 'public')));

let userCount = 0;

io.on('connection', (socket) => {
    userCount++;
    console.log('A user joined. ' + userCount + ' users online.');

    // Listen for room joining
    socket.on('joinRoom', (room) => {
        socket.join(room);
        console.log(`User joined room: ${room}`);
        socket.emit('system', `You joined room: ${room}`);
    });

    // Broadcast to everyone that a user joined
    io.emit('system', 'A user joined. ' + userCount + ' users online.');

    // Listen for chat messages
    socket.on('message', (data) => {
        const { room, username, text } = data;
        console.log(`Message in [${room || 'global'}] from ${username}: ${text}`);

        if (room) {
            // Send to a specific room
            io.to(room).emit('message', data);
        } else {
            // Broadcast to everyone
            io.emit('message', data);
        }
    });

    // Handle disconnection
    socket.on('disconnect', () => {
        userCount--;
        console.log('A user left. ' + userCount + ' users online.');
        // Broadcast to everyone that a user left
        io.emit('system', 'A user left. ' + userCount + ' users online.');
    });
});

const PORT = 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log('Server running on port ' + PORT);
    console.log('Access server overview at: http://localhost:' + PORT);
});
