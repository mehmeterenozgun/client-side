package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import model.FileDescriptor;
import model.FileDataResponseType;
import model.FileListResponseType;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;
import model.ResponseType.RESPONSE_TYPES;
import client.loggerManager;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

public class dummyClient {

    private void getFileDataDual(String ip1, int port1, String ip2, int port2, int file_id, long fileSize) throws IOException {
        InetAddress addr1 = InetAddress.getByName(ip1);
        InetAddress addr2 = InetAddress.getByName(ip2);
        DatagramSocket socket1 = new DatagramSocket();
        DatagramSocket socket2 = new DatagramSocket();
        long chunkSize = fileSize / 2; // Split file between interfaces.
        // Parallel threads for data fetching.
        Thread t1 = new Thread(() -> fetchDataChunk(socket1, addr1, port1, file_id, 0, chunkSize));
        Thread t2 = new Thread(() -> fetchDataChunk(socket2, addr2, port2, file_id, chunkSize + 1, fileSize));
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }

    }
    private void fetchDataChunk(DatagramSocket socket, InetAddress address, int port, int fileId, long start, long end) {
        long startTime = System.currentTimeMillis();
        try {
            RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, fileId, start, end, null);
            byte[] sendData = req.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

            socket.send(sendPacket);
            byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            socket.setSoTimeout(1000);
            socket.receive(receivePacket);

            FileDataResponseType response = new FileDataResponseType(receivePacket.getData());
            long elapsedTime = System.currentTimeMillis() - startTime;
            double speed = (response.getEnd_byte() - response.getStart_byte() + 1) / (elapsedTime / 1000.0);

            System.out.printf("Chunk %d-%d received (%.2f KB/s)\n",
                    response.getStart_byte(), response.getEnd_byte(), speed / 1024.0);

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout while fetching chunk. Retrying...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInvalidRequest(String ip, int port) throws IOException{
		 InetAddress IPAddress = InetAddress.getByName(ip);
         RequestType req=new RequestType(4, 0, 0, 0, null);
         byte[] sendData = req.toByteArray();
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
         DatagramSocket dsocket = new DatagramSocket();
         dsocket.send(sendPacket);
         byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
         DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.setSoTimeout(1000); // Timeout set to 1 second.
        try {
            dsocket.receive(receivePacket);
        } catch (SocketTimeoutException e) {
            loggerManager.getInstance(this.getClass()).warn("Packet timeout, retransmitting...");
            dsocket.send(sendPacket); // Retransmit.
        }

        ResponseType response=new ResponseType(receivePacket.getData());

	}
    public String computeFileMD5(int fileId) throws Exception {
        // Simulate reading the downloaded file
        byte[] fileData = getFileData(fileId); // Implement this to retrieve the downloaded data.
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] hash = md5.digest(fileData);
        return bytesToHex(hash);
    }

    private byte[] getFileData(int fileId) {
        // Placeholder for reading the downloaded file. Replace with actual implementation.
        return new byte[0];
    }

    public void getFileList(String ip, int port) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        DatagramSocket socket = new DatagramSocket();
        socket.send(new DatagramPacket(req.toByteArray(), req.toByteArray().length, serverAddress, port));

        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.setSoTimeout(2000);
        socket.receive(receivePacket);

        FileListResponseType response = new FileListResponseType(receivePacket.getData());
        System.out.println("File List:");
        for (FileDescriptor file : response.getFileDescriptors()) {
            System.out.println(file.getFile_id() + " " + file.getFile_name());
        }
    }


    private long getFileSize(String ip, int port, int fileId) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, fileId, 0, 0, null);
        FileSizeResponseType response = (FileSizeResponseType) sendAndReceive(req, serverAddress, port);
        return response.getFileSize();
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
	private void getFileData(String ip, int port, int file_id, long start, long end) throws IOException{
		InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte=-1;
        while(maxReceivedByte<end){
        	DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
            dsocket.setSoTimeout(1000); // Timeout set to 1 second.
            try {
                dsocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                loggerManager.getInstance(this.getClass()).warn("Packet timeout, retransmitting...");
                dsocket.send(sendPacket); // Retransmit.
            }

            FileDataResponseType response=new FileDataResponseType(receivePacket.getData());

            if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
            	break;
            }
            if (response.getEnd_byte()>maxReceivedByte){
            	maxReceivedByte=response.getEnd_byte();
            };
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hash = md5.digest(receiveData);
                System.out.println("MD5: " + bytesToHex(hash));
            } catch (NoSuchAlgorithmException e) {
                System.err.println("MD5 algorithm not found: " + e.getMessage());
            }


        }
	}
    private void downloadFile(String ip, int port, int fileId, long fileSize) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(ip);
        DatagramSocket socket = new DatagramSocket();
        long chunkSize = 1000; // Chunk size for each packet
        long start = 0;

        while (start < fileSize) {
            long end = Math.min(start + chunkSize - 1, fileSize);
            fetchDataChunk(socket, serverAddress, port, fileId, start, end);
            start = end + 1;
        }
        loggerManager.getInstance(this.getClass()).info("File download completed. File ID: " + fileId);
    }
    private Object sendAndReceive(RequestType req, InetAddress serverAddress, int port) throws IOException {
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        DatagramSocket socket = new DatagramSocket();

        socket.send(sendPacket);

        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        socket.setSoTimeout(2000); // Set timeout
        socket.receive(receivePacket);

        ResponseType response = new ResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());

        return response;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java dummyClient <server_IP1:port1> <server_IP2:port2>");
            System.exit(1);
        }

        try {
            // Parse command-line arguments

            String[] server1 = args[0].split(":");
            String[] server2 = args[1].split(":");

            String ip1 = server1[0];
            int port1 = Integer.parseInt(server1[1]);

            String ip2 = server2[0];
            int port2 = Integer.parseInt(server2[1]);

            dummyClient client = new dummyClient();

            while (true) {
                // Step 1: Get the file list
                System.out.println("Requesting file list...");
                client.getFileList(ip1, port1);

                // Step 2: User selects a file
                System.out.println("Enter the file ID to download (or -1 to exit): ");
                int fileId = new java.util.Scanner(System.in).nextInt();

                if (fileId == -1) {
                    System.out.println("Exiting...");
                    break;
                }

                // Step 3: Get file size
                System.out.println("Getting file size...");
                long fileSize = client.getFileSize(ip1, port1, fileId);
                System.out.println("File ID " + fileId + " is " + fileSize + " bytes.");

                // Step 4: Start the file download
                System.out.println("Starting download...");
                long startTime = System.currentTimeMillis();
                client.getFileDataDual(ip1, port1, ip2, port2, fileId, fileSize);
                long endTime = System.currentTimeMillis();

                // Step 5: Compute MD5 checksum and verify integrity
                System.out.println("Computing MD5 checksum...");
                String md5Hash = client.computeFileMD5(fileId);
                System.out.println("MD5 hash: " + md5Hash);

                // Step 6: Display download statistics
                long elapsedTime = endTime - startTime;
                System.out.println("File downloaded in " + elapsedTime + " ms.");
                System.out.println("Transfer statistics:");
                // (Include additional statistics if implemented)
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

}
