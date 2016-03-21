/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author apurv
 *
 */
public class ServerHelper {
	public byte[] getFileContents(String fileName) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("serverFiles/" +  fileName));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			br.close();
			return sb.toString().getBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
