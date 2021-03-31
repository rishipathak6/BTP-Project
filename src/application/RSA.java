package application;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;

public class RSA {
	private static final String ENCRYPT_ALGO = "RSA";

// Generating public & private keys
// using RSA algorithm.
	public static KeyPair generateRSAKkeyPair() throws Exception {
		SecureRandom secureRandom = new SecureRandom();
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ENCRYPT_ALGO);

		keyPairGenerator.initialize(2048, secureRandom);
		return keyPairGenerator.generateKeyPair();
	}

// Encryption function which converts
// the plainText into a cipherText
// using private Key.
	public byte[] encrypt(byte[] pText, PublicKey publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

		cipher.init(Cipher.ENCRYPT_MODE, publicKey);

		return cipher.doFinal(pText);
	}

// Decryption function which converts
// the ciphertext back to the
// orginal plaintext.
	public byte[] decrypt(byte[] cipherText, PrivateKey privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] result = cipher.doFinal(cipherText);

		return result;
	}

}
