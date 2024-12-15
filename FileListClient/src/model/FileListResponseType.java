package model;

import java.security.InvalidParameterException;
import java.nio.ByteBuffer;

public class FileListResponseType extends ResponseType {
	private FileDescriptor[] files=null;
	
	public FileListResponseType(int responseType, int file_id, long start_byte, long end_byte,byte[] data) {
		super(responseType, file_id, start_byte, end_byte, data);
		setFileDescriptors();
	}
	
	public FileListResponseType(byte[] rawData){
		super(rawData);
		setFileDescriptors();
	}

	private void setFileDescriptors() {
		files = new FileDescriptor[this.getFile_id()];
		byte[] data = this.getData();

		int dataIndex = 0;
		for (int i = 0; i < files.length; i++) {
			int fileId = data[dataIndex++] & 0xFF;
			StringBuilder fileName = new StringBuilder();
			while (data[dataIndex] != 0) {
				fileName.append((char) data[dataIndex++]);
			}
			dataIndex++; // Skip the null terminator
			files[i] = new FileDescriptor(fileId, fileName.toString());
		}
	}


	public FileDescriptor[] getFileDescriptors(){
		return files;
	}
	
	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer("\nresponse_type:"+this.getResponseType());
		sb.append("\nfile_id:"+this.getFile_id());
		sb.append("\nstart_byte:"+this.getStart_byte());
		sb.append("\nend_byte:"+this.getEnd_byte());
		sb.append("\ndata:");
		for(FileDescriptor file:getFileDescriptors()){
			sb.append("\n"+file.toString());
		}
		return sb.toString();
	}
	
	
}
