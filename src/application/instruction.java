package application;

import java.io.Serializable;

public class Instruction implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6872229225694980604L;
	private boolean grayBool;
	private boolean logoBool;
	private boolean haarBool;
	private boolean lbpBool;
	private boolean decryptBool;
	private double encryptDouble;

	public Instruction(boolean graybool, boolean logoBool, boolean haarBool, boolean lbpBool, boolean decryptBool,
			double encryptDouble) {
		this.grayBool = graybool;
		this.logoBool = logoBool;
		this.haarBool = haarBool;
		this.lbpBool = lbpBool;
		this.decryptBool = decryptBool;
		this.encryptDouble = encryptDouble;
	}

	public boolean isGrayBool() {
		return grayBool;
	}

	public void setGrayBool(boolean grayBool) {
		this.grayBool = grayBool;
	}

	public boolean isLogoBool() {
		return logoBool;
	}

	public void setLogoBool(boolean logoBool) {
		this.logoBool = logoBool;
	}

	public boolean isHaarBool() {
		return haarBool;
	}

	public void setHaarBool(boolean haarBool) {
		this.haarBool = haarBool;
	}

	public boolean isLbpBool() {
		return lbpBool;
	}

	public void setLbpBool(boolean lbpBool) {
		this.lbpBool = lbpBool;
	}

	public boolean isDecryptBool() {
		return decryptBool;
	}

	public void setDecryptBool(boolean decryptBool) {
		this.decryptBool = decryptBool;
	}

	public double getEncryptDouble() {
		return encryptDouble;
	}

	public void setEncryptDouble(double encryptDouble) {
		this.encryptDouble = encryptDouble;
	}

}
