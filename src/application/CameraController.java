package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
//import it.polito.elite.teachingg.cv.utils.Utils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the cameraButton for starting/stopping the camera and
 * the acquired video stream.
 *
 *
 */
public class CameraController {
//	private int port;
//	private String ip;
//
//	public SampleController(String ip, int port, Consumer<Serializable> onReceiveCallback) {
//		super(onReceiveCallback);
//		this.port = port;
//		this.ip = ip;
//	}

	// the FXML cameraButton
	@FXML
	private Button cameraButton;
	// the FXML image view
	@FXML
	private ImageView currentFrame;
	@FXML
	private ImageView encryptedFrame;
	@FXML
	private CheckBox grayCheckBox;
	@FXML
	private CheckBox logoCheckBox;
	@FXML
	private ImageView histogram;
	@FXML
	private CheckBox haarCheckBox;
	@FXML
	private CheckBox lbpCheckBox;
	@FXML
	private Slider encSlider;
	@FXML
	private TextField encPercent;
	@FXML
	private CheckBox decryptCheckBox;
	private double encPercentValue;
	private int frameNo = 1;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture cameraCapture = new VideoCapture();
	// a flag to change the cameraButton behavior
	private boolean cameraActive = false;
	private Mat logoMat;
	// the id of the camera to be used
	private static int cameraId = 0;
	private CascadeClassifier faceCascade = new CascadeClassifier();
	private int absoluteFaceSize = 0;
	private BufferedImage bufferedImage;
	private byte[] capturedByteArray;
	private int len;
	private int numEncBlock;
	private int unencGap;
	private ChaCha20 cipherCC20 = new ChaCha20();
	private RSA cipherRSA = new RSA();
	private PublicKey publicKey;
	private SecretKey key20;
	byte[] nonce20; // 96-bit nonce (12 bytes)
	int counter20 = 0; // 32-bit initial count (8 bytes)
	private byte[] encryptedByteArray20;
	private byte[] decryptedByteArray20;
	private byte[] overBytes20;
	private byte[] encOverBytes20;
	private byte[] sentBytes20;
	private Socket dataSocket;
	private Instruction instr;
	private byte[] instrBytes;
	private byte[] encInstrBytes;
	private EndsInstruction endsInstruction;
	private Mat cameraFrame;
	private Mat encDecCameraFrame;
	private Image cameraImage;
	private Image encDecCameraImage;

//	System.setOut(new PrintStream(new FileOutputStream("client.txt")));
	public class ControlServer extends Thread {
		private ServerSocket controlServerSocket;
		private String message;

		public ControlServer(int port) throws IOException {
			controlServerSocket = new ServerSocket(port);
//			controlServerSocket.setSoTimeout(10000);
		}

		public void run() {
			while (true) {
				try {
					System.out.println("----------------------------------------------------------------------------");
					System.out.println("Waiting for client to send instructions on port "
							+ controlServerSocket.getLocalPort() + "...");
					Socket server = controlServerSocket.accept();
					System.out.println("Just connected to " + server.getRemoteSocketAddress());
					ObjectInputStream in = new ObjectInputStream(server.getInputStream());

					try {
						endsInstruction = (EndsInstruction) in.readObject();
					} catch (ClassNotFoundException e) {
						System.out.println("Can't read endsInstruction object");
						e.printStackTrace();
					}
					message = endsInstruction.getMsg();
					publicKey = endsInstruction.getPublicKey();
					System.out.println("message = " + message);
					if (message.equals("Start Camera")) {
						cameraButton.fire();
					} else if (message.equals("Stop Camera")) {
						cameraButton.fire();
					}
//					server.close();

				} catch (SocketTimeoutException s) {
					System.out.println("Socket timed out!");
//					break;
				} catch (IOException e) {
					e.printStackTrace();
//					break;
				}
			}
		}
	}

	@FXML
	public void initialize() {
		try {
			Thread threadInitialise = new ControlServer(8000);
			threadInitialise.setDaemon(true);
			threadInitialise.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The action triggered by pushing the cameraButton on the GUI
	 *
	 * @param event the push cameraButton event
	 * @throws IOException
	 */
	@FXML
	protected void startCamera(ActionEvent event) throws IOException {
		if (!this.cameraActive) {
			File file = new File("Client.txt");
			// Instantiating the PrintStream class
			PrintStream stream = new PrintStream(file);
			System.out.println("From now on " + file.getAbsolutePath() + " will be your console");
			System.setOut(stream);
			// start the video capture
			this.cameraCapture.open(cameraId, 700);

			// is the video stream available?
			if (this.cameraCapture.isOpened()) {
				this.cameraActive = true;
				this.haarCheckBox.setSelected(true);
				this.checkboxSelection(
						"C:/Users/radha/Documents/MyFirstJFXApp/src/application/resources/haarcascades/haarcascade_frontalface_alt.xml");
				this.encSlider.setValue(6.25);
				this.encPercent.setText("6.25");
				encSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
					encPercent.setText(Double.toString(newValue.doubleValue()));
				});
				encPercent.textProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue,
							String newValue) {
						if (newValue.matches("\\d{0,2}([\\.]\\d{0,1})?")) {
							encSlider.setValue(Double.parseDouble(newValue));
						}
					}
				});

				dataSocket = new Socket(InetAddress.getLocalHost(), 3000);
				System.out.println("Just connected to " + dataSocket.getRemoteSocketAddress());

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						// effectively grab and process a single frame
						cameraFrame = grabFrame(frameNo, dataSocket);
						frameNo++;
						// convert and show the frame
						cameraImage = Utils.mat2Image(cameraFrame);
						updateImageView(currentFrame, cameraImage);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the cameraButton content
				Utils.onFXThread(this.cameraButton.textProperty(), "Stop Camera");
			} else {
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the cameraButton content
			Utils.onFXThread(this.cameraButton.textProperty(), "Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @param frameNo
	 * @param dataSocket
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame(int frameNo, Socket dataSocket) {
		// init everything
		Mat frame = new Mat();

		// check if the capture is open
		if (this.cameraCapture.isOpened()) {
			try {
				// read the current frame
				this.cameraCapture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					// add a logoMat...

					this.bufferedImage = MatToBufferedImage(frame);
					this.capturedByteArray = MatToByteArray(frame);
					System.out.println("----------------------------------------------------------------------------");
					System.out.println("The transmitted image length is " + this.capturedByteArray.length);
					System.out.println("");
					System.out.println(this.bufferedImage);
					System.out.println("");
//					if (!dataIsValidJPEG(capturedByteArray)) {
////						System.out.println("The original image is not a valid JPEG");
//					}

					encPercentValue = encSlider.getValue();
					len = capturedByteArray.length;
					key20 = getKey(); // 256-bit secret key (32 bytes)
					nonce20 = getNonce(); // 96-bit nonce (12 bytes)
					counter20++; // 32-bit initial count (8 bytes)
					if (encPercentValue != 0) {
						numEncBlock = (int) ((len * encPercentValue + 6399) / 6400);
						unencGap = len / numEncBlock;
						System.out.println("Percentage = " + encPercentValue + " number of blocks encrypted = "
								+ numEncBlock + " Gap between encrypted blocks = " + unencGap);

						System.out.println("\n---Encryption---");
						encryptedByteArray20 = cipherCC20.encrypt(capturedByteArray, key20, nonce20, counter20, 0,
								unencGap, numEncBlock); // encrypt
						System.out.println("Key       (hex): " + BytesToHex(key20.getEncoded()));
						System.out.println("Nonce     (hex): " + BytesToHex(nonce20));
						System.out.println("Counter        : " + counter20);
//						System.out.println("Original  (hex): " + BytesToHex(capturedByteArray));
//						System.out.println("Encrypted (hex): " + BytesToHex(encryptedByteArray20));

						System.out.println("\n---Decryption---");

						decryptedByteArray20 = cipherCC20.decrypt(encryptedByteArray20, key20, nonce20, counter20, 0,
								unencGap, numEncBlock); // decrypt
						System.out.println("Key       (hex): " + BytesToHex(key20.getEncoded()));
						System.out.println("Nonce     (hex): " + BytesToHex(nonce20));
						System.out.println("Counter        : " + counter20);
//							 System.out.println("Decrypted (hex): " + BytesToHex(decryptedByteArray20));
//							 System.out.println("Decrypted : " + new String(decryptedByteArray20));

//						}
//						
//					}
//						System.out.println("Trying to allocate bytebuffer for overbyte20");
						overBytes20 = ByteBuffer.allocate(48).put(key20.getEncoded()).put(nonce20).putInt(counter20)
								.array();
//						System.out.println("Length of overByte20 is " + overBytes20.length);
						encOverBytes20 = cipherRSA.encrypt(overBytes20, publicKey);
//						System.out.println("Length of encOverByte20 is " + encOverBytes20.length);
						sentBytes20 = ByteBuffer.allocate(encryptedByteArray20.length + 256).put(encryptedByteArray20)
								.put(encOverBytes20).array();
						sendBytes(sentBytes20, 0, sentBytes20.length, dataSocket);
						System.out.println("Bytes sent");
//						DataInputStream in = new DataInputStream(dataSocket.getInputStream());
//
						encInstrBytes = readBytes(dataSocket);
						instrBytes = cipherRSA.decrypt(encInstrBytes, publicKey);
						instr = (Instruction) bytesToObject(instrBytes);

						System.out.println("Gray    = " + instr.isGrayBool());
						System.out.println("Logo    = " + instr.isLogoBool());
						System.out.println("Haar    = " + instr.isHaarBool());
						System.out.println("LBP     = " + instr.isLbpBool());
						System.out.println("Decrypt = " + instr.isDecryptBool());
						System.out.println("Encpsnt = " + instr.getEncryptDouble());

//						
						if (instr.isGrayBool()) {
							this.grayCheckBox.setSelected(true);
						} else {
							this.grayCheckBox.setSelected(false);
						}

						if (instr.isLogoBool()) {
							this.logoCheckBox.setSelected(true);
						} else {
							this.logoCheckBox.setSelected(false);
						}

						if (instr.isHaarBool()) {
							this.haarCheckBox.setSelected(true);
						} else {
							this.haarCheckBox.setSelected(false);
						}

						if (instr.isLbpBool()) {
							this.lbpCheckBox.setSelected(true);
						} else {
							this.lbpCheckBox.setSelected(false);
						}

						if (instr.isDecryptBool()) {
							this.decryptCheckBox.setSelected(true);
						} else {
							this.decryptCheckBox.setSelected(false);
						}

						if (instr.getEncryptDouble() != this.encSlider.getValue()) {
							this.encSlider.setValue(instr.getEncryptDouble());
						}

						if (logoCheckBox.isSelected()) {
							this.logoMat = Imgcodecs.imread("resources/dp.jpg");
							if (this.logoMat != null) {
								Rect roi = new Rect(0, 0, logoMat.cols(), logoMat.rows());
								Mat imageROI = frame.submat(roi);
								// add the logoMat: method #1
								Core.addWeighted(imageROI, 1.0, logoMat, 1.0, 0.0, imageROI);
							}
						}
						if (grayCheckBox.isSelected()) {
							Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
						}

						if (this.decryptCheckBox.isSelected()) {
							encDecCameraFrame = Imgcodecs.imdecode(new MatOfByte(decryptedByteArray20),
									Imgcodecs.IMREAD_UNCHANGED);
							if (!encDecCameraFrame.empty()) {
								encDecCameraImage = Utils.mat2Image(encDecCameraFrame);
								updateImageView(encryptedFrame, encDecCameraImage);
							}
						} else {
							encDecCameraFrame = Imgcodecs.imdecode(new MatOfByte(encryptedByteArray20),
									Imgcodecs.IMREAD_UNCHANGED);
							if (!encDecCameraFrame.empty()) {
								encDecCameraImage = Utils.mat2Image(encDecCameraFrame);
								updateImageView(encryptedFrame, encDecCameraImage);
							} else {
								System.out.println("The frame is too encrypted to show");
							}
						}

						// show the histogram
						this.showHistogram(frame, grayCheckBox.isSelected());
						// face detection
						this.faceDetectAndDisplay(frame);
						System.out.println(
								"----------------------------------------------------------------------------");
						System.out.println("\n");
					}

				}

			} catch (Exception e) {
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		try {
			if (dataSocket != null)
				dataSocket.close();
		} catch (IOException e) {
			System.out.println("Connection not open to close");
			e.printStackTrace();
		}

		if (this.cameraCapture.isOpened()) {
			// release the camera
			this.cameraCapture.release();
		}
	}

	public byte[] readBytes(Socket socket) {
		InputStream in = null;
		try {
			in = socket.getInputStream();
		} catch (IOException e) {
			System.out.println("Can't read bytes from socket");
			e.printStackTrace();
		}
		DataInputStream dis = new DataInputStream(in);

		int len = 0;
		try {
			len = dis.readInt();
		} catch (IOException e) {
			System.out.println("Can't read length of array from bytes");
			e.printStackTrace();
		}
		byte[] data = new byte[len];
		if (len > 0) {
			try {
				dis.readFully(data);
			} catch (IOException e) {
				System.out.println("Can't read transmitted bytes");
				e.printStackTrace();
			}
		}
		return data;
	}

	public void sendBytes(byte[] myByteArray, int start, int len, Socket socket) throws IOException {
		if (len < 0)
			throw new IllegalArgumentException("Negative length not allowed");
		if (start < 0 || start >= myByteArray.length)
			throw new IndexOutOfBoundsException("Out of bounds: " + start);
		// Other checks if needed.

		// May be better to save the streams in the support class;
		// just like the socket variable.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);

		dos.writeInt(len);
		if (len > 0) {
			dos.write(myByteArray, start, len);
		}
	}

	@FXML
	protected void loadLogo() {
		if (logoCheckBox.isSelected())
			this.logoMat = Imgcodecs.imread("resources/dp.jpg");
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view  the {@link ImageView} to update
	 * @param image the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	private Object bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				ObjectInputStream in = new ObjectInputStream(bis)) {
			return in.readObject();
		}
	}

	private void showHistogram(Mat frame, boolean gray) {
		// split the frames in multiple images
		List<Mat> images = new ArrayList<Mat>();
		Core.split(frame, images);

		// set the number of bins at 256
		MatOfInt histSize = new MatOfInt(256);
		// only one channel
		MatOfInt channels = new MatOfInt(0);
		// set the ranges
		MatOfFloat histRange = new MatOfFloat(0, 256);

		// compute the histograms for the B, G and R components
		Mat hist_b = new Mat();
		Mat hist_g = new Mat();
		Mat hist_r = new Mat();

		// B component or gray image
		Imgproc.calcHist(images.subList(0, 1), channels, new Mat(), hist_b, histSize, histRange, false);

		// G and R components (if the image is not in gray scale)
		if (!gray) {
			Imgproc.calcHist(images.subList(1, 2), channels, new Mat(), hist_g, histSize, histRange, false);
			Imgproc.calcHist(images.subList(2, 3), channels, new Mat(), hist_r, histSize, histRange, false);
		}

		// draw the histogram
		int hist_w = 150; // width of the histogram image
		int hist_h = 150; // height of the histogram image
		int bin_w = (int) Math.round(hist_w / histSize.get(0, 0)[0]);

		Mat histImage = new Mat(hist_h, hist_w, CvType.CV_8UC3, new Scalar(0, 0, 0));
		// normalize the result to [0, histImage.rows()]
		Core.normalize(hist_b, hist_b, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());

		// for G and R components
		if (!gray) {
			Core.normalize(hist_g, hist_g, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
			Core.normalize(hist_r, hist_r, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
		}

		// effectively draw the histogram(s)
		for (int i = 1; i < histSize.get(0, 0)[0]; i++) {
			// B component or gray image
			Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_b.get(i - 1, 0)[0])),
					new Point(bin_w * (i), hist_h - Math.round(hist_b.get(i, 0)[0])), new Scalar(255, 0, 0), 2, 8, 0);
			// G and R components (if the image is not in gray scale)
			if (!gray) {
				Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_g.get(i - 1, 0)[0])),
						new Point(bin_w * (i), hist_h - Math.round(hist_g.get(i, 0)[0])), new Scalar(0, 255, 0), 2, 8,
						0);
				Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_r.get(i - 1, 0)[0])),
						new Point(bin_w * (i), hist_h - Math.round(hist_r.get(i, 0)[0])), new Scalar(0, 0, 255), 2, 8,
						0);
			}
		}

		// display the histogram...
		Image histImg = Utils.mat2Image(histImage);
		updateImageView(histogram, histImg);
	}

	/**
	 * The action triggered by selecting the Haar Classifier checkbox. It loads the
	 * trained set to be used for frontal face detection.
	 */
	@FXML
	protected void haarSelected(Event event) {
		// check whether the lpb checkbox is selected and deselect it
		if (this.lbpCheckBox.isSelected())
			this.lbpCheckBox.setSelected(false);

		this.checkboxSelection(
				"C:/Users/radha/Documents/MyFirstJFXApp/src/application/resources/haarcascades/haarcascade_frontalface_alt.xml");
	}

	/**
	 * The action triggered by selecting the LBP Classifier checkbox. It loads the
	 * trained set to be used for frontal face detection.
	 */
	@FXML
	protected void lbpSelected(Event event) {
		// check whether the haar checkbox is selected and deselect it
		if (this.haarCheckBox.isSelected())
			this.haarCheckBox.setSelected(false);

		this.checkboxSelection(
				"C:/Users/radha/Documents/MyFirstJFXApp/src/application/resources/lbpcascades/lbpcascade_frontalface.xml");
	}

	/**
	 * Method for loading a classifier trained set from disk
	 * 
	 * @param classifierPath the path on disk where a classifier trained set is
	 *                       located
	 */
	private void checkboxSelection(String classifierPath) {
		// load the classifier(s)
		this.faceCascade.load(classifierPath);

		// now the video capture can start
		this.cameraButton.setDisable(false);
	}

	/**
	 * Method for face detection and tracking
	 * 
	 * @param frame it looks for faces in this frame
	 */
	private void faceDetectAndDisplay(Mat frame) {
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();

		// convert the frame in gray scale
		if (!grayCheckBox.isSelected()) {
			Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		}
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);

		// compute minimum face size (20% of the frame height, in our case)
		if (this.absoluteFaceSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0) {
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}

		// detect faces
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

		// each rectangle in faces is a face: draw them!
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
	}

	public static BufferedImage MatToBufferedImage(Mat mat) throws IOException {
		// Encoding the image
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		// Storing the encoded Mat in a byte array
		byte[] byteArray = matOfByte.toArray();
		// Preparing the Buffered Image
		InputStream in = new ByteArrayInputStream(byteArray);
		BufferedImage bufImage = ImageIO.read(in);
		return bufImage;
	}

	public static byte[] MatToByteArray(Mat mat) throws IOException {
		// Encoding the image
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		// Storing the encoded Mat in a byte array
		byte[] byteArray = matOfByte.toArray();
		// Preparing the Buffered Image
		return byteArray;
	}

	private static String BytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte temp : bytes) {
			result.append(String.format("%02x", temp));
		}
		return result.toString();
	}

	// A 256-bit secret key (32 bytes)
	private static SecretKey getKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("ChaCha20");
		keyGen.init(256, SecureRandom.getInstanceStrong());
		return keyGen.generateKey();
	}

	// 96-bit nonce (12 bytes)
	private static byte[] getNonce() {
		byte[] newNonce = new byte[12];
		new SecureRandom().nextBytes(newNonce);
		return newNonce;
	}
}