package iteration3;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A class used to write byte[] to a file
 * uses FileOutputStream
 */
public class Writer {
	private FileOutputStream output;
	
	public Writer(String path, boolean append) throws IOException {
		output = new FileOutputStream(path, append);
	}
	
	/**
	 * Write data to file
	 * @param data	byte[] being written to file
	 * @throws IOException
	 */
	public void write(byte[] data) throws IOException {
		output.write(data);
	}
	
	/**
	 * Close output
	 * @throws IOException
	 */
	public void close() throws IOException {
		output.close();
	}
}
