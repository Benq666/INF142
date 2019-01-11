package oblig1;

import java.io.*;
import java.net.*;
import java.util.stream.Collectors;

/**
 * Web Proxy Server class that tries to get header information from the address specified by client.
 * Sends received data back to the client and closes the connection.
 * Prints out errors if they occur in the process.
 *
 * @author Andrey Belinskiy
 *
 */
class WPS {
    public static void main(String args[]) {

        byte[] sendData;
        byte[] receiveData = new byte[8192];
        InetAddress clientIP;
        int clientPort;

        System.out.println("Waiting for request from the client ...");
        try {
            DatagramSocket serverSocket = new DatagramSocket(3333);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            clientIP = receivePacket.getAddress();
            clientPort = receivePacket.getPort();
            String lineFromClient = new String(receivePacket.getData());

            // initial check of request from the client
            if (!lineFromClient.contains(".")) {
                sendData = String.format("Invalid request from client: %s%nClosing connection.%n",
                        lineFromClient).getBytes();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                serverSocket.send(sendPacket);
                serverSocket.close();
            }

            System.out.printf("%nReceived following request line from the client at %s : %d%n%s%n",
                    clientIP, clientPort, lineFromClient);
            String tempS = lineFromClient.trim();

            // separating the host and the path to the page/file from the client message
            String exactAddress;
            if (lineFromClient.contains("https://") || lineFromClient.contains("http://")) {
                exactAddress = lineFromClient.substring(lineFromClient.indexOf("//") + 2);
                tempS = exactAddress.trim();
                if (exactAddress.contains("/")) {
                    exactAddress = exactAddress.substring(exactAddress.indexOf("/"));
                    exactAddress = exactAddress.trim();
                } else {
                    exactAddress = "/";
                }
            } else if (lineFromClient.contains("/")) {
                exactAddress = lineFromClient.substring(lineFromClient.indexOf("/"));
                exactAddress = exactAddress.trim();
            } else {
                exactAddress = "/";
            }

            String remoteHost;
            if (lineFromClient.contains("https://") || lineFromClient.contains("http://")) {
                remoteHost = tempS.substring(0, tempS.indexOf("/"));
            } else if (lineFromClient.contains("/")) {
                remoteHost = lineFromClient.substring(0, lineFromClient.indexOf("/"));
            } else {
                remoteHost = lineFromClient.trim();
            }

            // trying to connect to specified address
            Socket s = new Socket();
            s.setSoTimeout(120 * 1000); // 120 sec timeout to connect to the specified host
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            try {
                s.connect(new InetSocketAddress(remoteHost, 80));
                System.out.printf("%nConnected to remote address at %s : %d%n", remoteAddress , 80);
            } catch (Exception ex) {
                sendData = String.format("Something went wrong while trying to connect to %s%n%s%n",
                        remoteHost, ex.getClass().getSimpleName()).getBytes();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                serverSocket.send(sendPacket);
                serverSocket.close();
                s.close();
            }

            // sending HEAD request to the remote server
            PrintWriter pw = new PrintWriter(s.getOutputStream());
            System.out.printf("%nExecuting:%nHEAD %s HTTP/1.1%n%n%nHost: %s%n%n%n",
                    exactAddress, remoteHost);
            pw.print("HEAD " + exactAddress + " HTTP/1.1\r\n");
            pw.print("Host: " + remoteHost + "\r\n\r\n");
            pw.flush();

            // send the data to the client if the response was received
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                sendData = br.lines().collect(Collectors.joining("\n")).getBytes();
                System.out.printf("Sending response to the client at %s:%d%n",
                        clientIP, clientPort);
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                serverSocket.send(sendPacket);
                serverSocket.close();
                br.close();
                s.close();
            } catch (UncheckedIOException ex) {
                System.out.printf("%nConnection timed out. Closing the socket.%nException: <%s>%n", ex);
            }

            // exceptions handling
        } catch (SocketException ex) {
            System.out.printf("%nClosing the socket.%nException: <%s>%n", ex);
        } catch (IOException ex) {
            System.out.printf("%nI/O exception has occurred:%n <%s>", ex);
        }
    }
}