package application;

import java.io.Serializable;
import java.security.PublicKey;

public class EndsInstruction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2732175185741438048L;
	private String msg;
	private PublicKey publicKey;

	public EndsInstruction(String msg, PublicKey publicKey) {
		this.msg = msg;
		this.publicKey = publicKey;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
}
