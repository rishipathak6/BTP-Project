package application;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.ChaCha20ParameterSpec;

/*
The inputs to ChaCha20 encryption, specified by RFC 7539, are:
 - A 256-bit secret key (32 bytes)
 - A 96-bit nonce (12 bytes)
 - A 32-bit initial count (4 bytes)
*/
public class ChaCha20 {

	private static final String ENCRYPT_ALGO = "ChaCha20";

	public byte[] encrypt(byte[] pText, SecretKey key, byte[] nonce, int counter, int offset, int length, int times,
			double encPercent) throws Exception {

		Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

		ChaCha20ParameterSpec param = new ChaCha20ParameterSpec(nonce, counter);

		cipher.init(Cipher.ENCRYPT_MODE, key, param);

		byte[] encryptedText = new byte[pText.length];
		if (encPercent != 100.0 && encPercent != 0.0) {
			System.arraycopy(pText, 0, encryptedText, 0, pText.length);
			// encryptedText = pText;
			for (int i = 0; i < times - 1; i++) {
				cipher.update(pText, offset + (i + 1) * (length) - 64, 64, encryptedText,
						offset + (i + 1) * (length) - 64);
				// compr(pText, encryptedText, offset + (i) * (length));
			}
			cipher.doFinal(pText, offset + (times - 1) * length, 64, encryptedText, offset + (times - 1) * length);
		} else if (encPercent == 100.0) {
			encryptedText = cipher.doFinal(pText);
		} else {
			encryptedText = pText;
		}
		return encryptedText;
	}

	public byte[] decrypt(byte[] cText, SecretKey key, byte[] nonce, int counter, int offset, int length, int times,
			double encPercent) throws Exception {

		Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

		ChaCha20ParameterSpec param = new ChaCha20ParameterSpec(nonce, counter);

		cipher.init(Cipher.DECRYPT_MODE, key, param);

		byte[] decryptedText = new byte[cText.length];
		if (encPercent != 100.0 && encPercent != 0.0) {
			System.arraycopy(cText, 0, decryptedText, 0, cText.length);
//			encryptedText = pText;
			for (int i = 0; i < times - 1; i++) {
				cipher.update(cText, offset + (i + 1) * (length) - 64, 64, decryptedText,
						offset + (i + 1) * (length) - 64);
//			compr(pText, encryptedText, offset + (i) * (length));
			}
			cipher.doFinal(cText, offset + (times - 1) * length, 64, decryptedText, offset + (times - 1) * length);
		} else if (encPercent == 100.0) {
			decryptedText = cipher.doFinal(cText);
		} else {
			decryptedText = cText;
		}
		return decryptedText;

	}

//	private static void compr(byte[] bytes, byte[] encbytes, int ind) {
//		StringBuilder result1 = new StringBuilder();
//		StringBuilder result2 = new StringBuilder();
//		for (int i = ind; i < ind + 64; i++) {
//			result1.append(String.format("%02x", bytes[i]));
//		}
//		for (int i = ind; i < ind + 64; i++) {
//			result2.append(String.format("%02x", encbytes[i]));
//		}
//		System.out.println(ind + " " + (ind + 64));
//		System.out.println(result1.toString());
//		System.out.println(result2.toString());
//	}
}
