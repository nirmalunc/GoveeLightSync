package com.nirmal.LANControlScripts;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import org.json.JSONObject;

public class GetLANControlDetails {

    public static void main(String[] args) throws Exception {
        // Step 1: Send request scan on port 4001
        InetAddress group = InetAddress.getByName("239.255.255.250");
        int port = 4001;

        MulticastSocket multicastSocket = new MulticastSocket(port);

        NetworkInterface netIf = NetworkInterface.getByName("your_network_interface_name");

        InetSocketAddress groupAddress = new InetSocketAddress(group, port);
        multicastSocket.joinGroup(groupAddress, netIf);

        String message = "{ \"msg\":{ \"cmd\":\"scan\", \"data\":{ \"account_topic\":\"reserve\" } } }";
        byte[] msgBytes = message.getBytes();

        DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, groupAddress.getAddress(), port);
        multicastSocket.send(packet);

        // Leave group after sending the message
        multicastSocket.leaveGroup(groupAddress, netIf);
        multicastSocket.close();

        // Step 2: Listen for response scan and print
        DatagramSocket udpServerSocket = new DatagramSocket(4002);
        byte[] receiveData = new byte[1024];

        System.out.println("UDP Server listening on port 4002...");

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            udpServerSocket.receive(receivePacket);

            // Receive response and print
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received response scan: " + receivedMessage);

            JSONObject jsonResponse = new JSONObject(receivedMessage);
            String ip = jsonResponse.getJSONObject("msg").getJSONObject("data").getString("ip");

            // Print the extracted IP
            System.out.println("Device IP: " + ip);
            break;
        }
    }
}