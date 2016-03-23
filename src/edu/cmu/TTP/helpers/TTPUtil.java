/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author apurv
 *
 */
public class TTPUtil {
	
	public String calculateMd5(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(data);
		byte[] digest = md.digest();
		//convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
          sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }
		return sb.toString();
	}
}
