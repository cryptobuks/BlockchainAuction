/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 *
 * @author SHUBHAM
 */
public class Client {

    
    public static void main(String[] args) throws SocketException, IOException {
        
        BufferedReader clientRead =new BufferedReader(new InputStreamReader(System.in));
      
        InetAddress IP = InetAddress.getByName("127.0.0.1");
      
        DatagramSocket clientSocket = new DatagramSocket();
        while(true)    //true
        {
            byte[] sendbuffer = new byte[1024];
            byte[] receivebuffer = new byte[1024];

            System.out.print("\nClient: ");
            
            //Mensagem a mandar
            String clientData = clientRead.readLine();
            
            sendbuffer = clientData.getBytes();        
            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, IP, 9877);
            clientSocket.send(sendPacket);

            if(clientData.equalsIgnoreCase("bye"))
            {
                System.out.println("Connection ended by client");
                break;
            }
            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket);
            String serverData = new String(receivePacket.getData());
            System.out.print("\nServer: " + serverData);

            }
            clientSocket.close();
    }
    
}