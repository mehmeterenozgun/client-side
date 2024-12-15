package model;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.io.IOException;
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

public class FileSizeResponseType extends ResponseType {
	
	long fileSize=-1;
	
	public FileSizeResponseType(int responseType, int file_id, long start_byte, long end_byte,long file_size) {
		super(responseType, file_id, start_byte, end_byte, null);
		if (fileSize < 0) {
			throw new InvalidParameterException("Invalid file size: " + fileSize);
		}

		this.fileSize=file_size;
		setFileSizeToData();
	}
	
	public FileSizeResponseType(byte[] rawData){
		super(rawData);
		setFileSize();
	}
	
	private void setFileSize(){
		if (this.getResponseType()==ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			byte[] data=this.getData();
			fileSize=0;
			for(int i=0;i<4;i++){
				fileSize=(fileSize << 8)|((int)data[i] & 0xFF);
			}
		}
	}
	
	private void setFileSizeToData(){
		this.data=new byte[4];
		long tmp=fileSize;
		for(int i=3;i>=0;i--){
			this.data[i]=(byte)(tmp & 0xFF);
			tmp>>=8;
		}
	}
	
	public long getFileSize(){
		return fileSize;
	}
	private long getFileSize(String ip, int port, int fileId) throws IOException {
		InetAddress serverAddress = InetAddress.getByName(ip);
		RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, fileId, 0, 0, null);

		// Send the request and receive a response
		byte[] sendData = req.toByteArray();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
		DatagramSocket socket = new DatagramSocket();

		socket.send(sendPacket);

		byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		try {
			socket.setSoTimeout(2000); // Timeout of 2 seconds
			socket.receive(receivePacket);

			// Parse the response
			ResponseType response = new ResponseType(receivePacket.getData());
			if (response.getResponseType() == ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS) {
				byte[] data = response.getData();
				if (data == null || data.length < 4) {
					throw new IOException("Invalid response: Missing file size data.");
				}

				// Extract file size (4 bytes)
				long fileSize = 0;
				for (int i = 0; i < 4; i++) {
					fileSize = (fileSize << 8) | (data[i] & 0xFF);
				}
				return fileSize;
			} else {
				throw new IOException("Error: Server returned invalid response type: " + response.getResponseType());
			}
		} catch (SocketTimeoutException e) {
			throw new IOException("Error: Timeout while waiting for file size response.");
		} finally {
			socket.close();
		}
	}

	@Override
	public String toString() {
		StringBuffer resultBuf=new StringBuffer("\nresponse_type:"+this.getResponseType());
		resultBuf.append("\nfile_id:"+this.getFile_id());
		resultBuf.append("\nstart_byte:"+this.getStart_byte());
		resultBuf.append("\nend_byte:"+this.getEnd_byte());
		resultBuf.append("\ndata:");
		if (this.getResponseType()==ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			resultBuf.append(fileSize);
		}
		return resultBuf.toString();
	}
}
