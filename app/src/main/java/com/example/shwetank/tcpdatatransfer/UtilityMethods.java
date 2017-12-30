package com.example.shwetank.tcpdatatransfer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by shwetank on 31/12/17.
 */

public class UtilityMethods {

    public static InetAddress getBroadcastIp(Context context) {
        // Function to return the broadcast address, based on the IP address of the device
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String addressString = toBroadcastIp(ipAddress);
            InetAddress broadcastAddress = InetAddress.getByName(addressString);
            return broadcastAddress;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String toBroadcastIp(int ip) {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }
}
