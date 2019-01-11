package oblig1;

import java.io.*;
import java.net.*;

/**
 * Client class that sends the URL to WPS and receives the response with a header information.
 * Closes the connection afterwards, prints out any errors that occur in the process.
 *
 * @author Andrey Belinskiy
 *
 */
class K {
    public static void main(String args[]) {

        byte[] sendData;
        byte[] receiveData = new byte[8192];

        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName("localhost");

            System.out.printf("Please enter the website or file URL you want to reach:%n");
            String input = inFromUser.readLine();
            sendData = input.getBytes();
            DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, IPAddress, 3333);
            clientSocket.send(sendPacket);
            System.out.printf("%nSent request to the server at %s : %d%n", IPAddress, 3333);
            clientSocket.setSoTimeout(40 * 1000); // 40 sec timeout to update the terminal

            // if there is no response after 120 seconds - timeout and close connection
            int count = 0;
            while (count < 3) {
                try {
                    if (!clientSocket.isClosed())
                        System.out.printf("%nWaiting for response from the server ...%n");
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String replyFromServer = new String(receivePacket.getData());
                    System.out.printf("%nResponse from the server:%n%n%s", replyFromServer);
                    clientSocket.close();
                } catch (SocketTimeoutException ex) {
                    System.out.printf("%nServer is not responding ...%nException: <%s>%n", ex);
                    count++;
                    if (count == 3) {
                        System.out.printf("%nConnection timed out. Closing the socket.%n");
                    }
                }
            }

            // exceptions handling
        } catch (SocketException ex) {
            System.out.printf("%nThe socket was closed.%n");
        } catch (IOException ex) {
            System.out.printf("I/O exception has occurred:%n<%s>", ex);
        }
    }
}