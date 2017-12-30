package com.example.shwetank.tcpdatatransfer;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public class ContactManager {

    private static final String LOG_TAG = "ContactManager";
    private static final int BROADCAST_PORT = 9760; // Socket on which packets are sent/received
    private static final int BROADCAST_INTERVAL = 5000; // Time interval
    private static final int BROADCAST_BUF_SIZE = 2048;
    private boolean BROADCAST = true;
    private boolean LISTEN = true;
    private HashMap<String, InetAddress> mPeerList;
    private InetAddress mBroadcastIP;

    public ContactManager(String name, InetAddress broadcastIP) {

        mPeerList = new HashMap<String, InetAddress>();
        this.mBroadcastIP = broadcastIP;
        listen();
        broadcastName(name, broadcastIP);
    }

    public HashMap<String, InetAddress> getmPeerList() {
        return mPeerList;
    }

    public void addContact(String name, InetAddress address) {
        if (!mPeerList.containsKey(name)) {
            mPeerList.put(name, address);
            return;
        }
        return;
    }

    public void removeContact(String name) {
        if (mPeerList.containsKey(name)) {
            mPeerList.remove(name);
        }
    }

    public void bye(final String name) {
        Thread byeThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String notification = "BYE:" + name;
                    byte[] message = notification.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, mBroadcastIP, BROADCAST_PORT);
                    socket.send(packet);
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {
                } catch (IOException e) {
                }
            }
        });
        byeThread.start();
    }

    public void broadcastName(final String name, final InetAddress broadcastIP) {
        Thread broadcastThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String request = "ADD:" + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
                    while (BROADCAST) {
                        socket.send(packet);
                        Thread.sleep(BROADCAST_INTERVAL);
                    }
                    socket.disconnect();
                    socket.close();
                } catch (SocketException e) {
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }
        });
        broadcastThread.start();
    }

    public void stopBroadcasting() {
        // Ends the broadcasting thread
        BROADCAST = false;
    }

    public void listen() {
        Thread listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket(BROADCAST_PORT);
                } catch (SocketException e) {
                    return;
                }
                byte[] buffer = new byte[BROADCAST_BUF_SIZE];
                while (LISTEN) {
                    listen(socket, buffer);
                }
                socket.disconnect();
                socket.close();
                return;
            }

            public void listen(DatagramSocket socket, byte[] buffer) {

                try {
                    DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
                    socket.setSoTimeout(15000);
                    socket.receive(packet);
                    String data = new String(buffer, 0, packet.getLength());
                    String action = data.substring(0, 4);
                    if (action.equals("ADD:")) {
                        addContact(data.substring(4, data.length()), packet.getAddress());
                    } else if (action.equals("BYE:")) {
                        removeContact(data.substring(4, data.length()));
                    } else {
                        Log.w(LOG_TAG, "Listener received invalid request: " + action);
                    }

                } catch (SocketTimeoutException e) {

                    Log.i(LOG_TAG, "No packet received!");
                    if (LISTEN) {
                        listen(socket, buffer);
                    }
                } catch (SocketException e) {
                } catch (IOException e) {
                }
            }
        });
        listenThread.start();
    }

    public void stopListening() {
        LISTEN = false;
    }
}
