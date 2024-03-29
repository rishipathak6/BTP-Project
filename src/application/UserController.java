package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
import org.opencv.videoio.VideoWriter;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
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
public class UserController {
//	private int port;
//
//	public ServerController(int port, Consumer<Serializable> onReceiveCallback) {
//		super(onReceiveCallback);
//		this.port = port;
//	}

	// the FXML cameraButton
	@FXML
	private Button cameraButton;
	@FXML
	private Button snapButton;
	@FXML
	private Button recordButton;
	// the FXML image view
	@FXML
	private ImageView currentFrame;
	@FXML
	private ImageView encryptedFrame;
	@FXML
	private ImageView recordIcon;
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
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture cameraCapture = new VideoCapture();
	// a flag to change the cameraButton behavior
	private boolean cameraActive = false;
	private boolean recordActive = false;
	private Mat logoMat;
	private CascadeClassifier faceCascade = new CascadeClassifier();
	private int absoluteFaceSize = 0;
	private int numEncBlock;
	private int unencGap;
	private ChaCha20 cipherCC20 = new ChaCha20();
	private RSA cipherRSA = new RSA();
	private KeyPair keypair;
	private SecretKey key20;
	byte[] nonce20 = new byte[12]; // 96-bit nonce (12 bytes)
	int counter20 = 1; // 32-bit initial count (8 bytes)
	private byte[] viewedByteArray;
	private ByteBuffer bb;
	private byte[] incomingByteArray;
	private byte[] encryptedByteArray;
	private byte[] encOverBytes20 = new byte[256]; // 48 bytes , 384 bits
	private byte[] overBytes20 = new byte[48]; // 48 bytes , 384 bits
	private byte[] keyArray = new byte[32]; // 32 bytes , 256 bits
	private byte[] counterArray = new byte[4]; // 4 bytes , 32 bits
	private Thread t;
	private Socket controlSocket;

	private Instruction instr = new Instruction(false, false, true, false, false, 5.0);
	private byte[] instrBytes;
	private byte[] encInstrbytes;

	private ServerSocket dataServerSocket;
	private Socket dataSocket;

	private Mat userFrame;
	private Mat encDecUserFrame;
	private Image userImage;
	private Image encDecUserImage;

	private VideoWriter writer;

	private long startTime;
	private long endTime;
	private long ccTime1;
	private long ccTime2;
	private double ccMinTime = Long.MAX_VALUE;
	private double ccSumTime;
	private long rsaTime1;
	private long rsaTime2;
	private double rsaMinTime = Long.MAX_VALUE;
	private double rsaSumTime;

	public class DataServer extends Thread {

		public DataServer(int port) throws IOException {
			dataServerSocket = new ServerSocket(port);
//			dataServerSocket.setSoTimeout(10000);
		}

		public void run() {
//			while (true) {
			try {
				System.out.println("----------------------------------------------------------------------------");
				System.out.println("Waiting for client on port " + dataServerSocket.getLocalPort() + "...");
				dataSocket = dataServerSocket.accept();
				System.out.println("Just connected to " + dataSocket.getRemoteSocketAddress());
				endTime = System.nanoTime();
				System.out.println("Data server connected at " + (double) (endTime - startTime) / 1000000);
				writer = new VideoWriter("resources/capture.mp4", VideoWriter.fourcc('x', '2', '6', '4'), 30,
						new Size(640.0, 480.0), true);
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameReceiver = new Runnable() {

					@Override
					public void run() {

						userFrame = recieveAndDecryptFrame(dataSocket);
						System.out.println("recording on:" + recordActive);
						if (recordActive) {
							Imgproc.putText(userFrame, ("frame" + counter20), new Point(20, 460), 0, 0.75,
									new Scalar(200, 200, 200), 3);
							writer.write(userFrame);
						}
						// convert and show the frame
						userImage = Utils.mat2Image(userFrame);
						updateImageView(encryptedFrame, userImage);
						endTime = System.nanoTime();
						System.out.println("Decrypted frame displayed at " + (double) (endTime - startTime) / 1000000);
					}
				};

				timer = Executors.newSingleThreadScheduledExecutor();
				timer.scheduleAtFixedRate(frameReceiver, 0, 33, TimeUnit.MILLISECONDS);
				System.out.println("----------------------------------------------------------------------------");
				System.out.println("\n");

			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void exitserver() {

		}
	}

//	@FXML
//	protected void screenShot(ActionEvent event) throws IOException {
//		File outputFile = new File("resources/ss.jpg");
//        BufferedImage bImage = SwingFXUtils.fromFXImage(encryptedFrame.snapshot(null, null), null);
//        try {
//            ImageIO.write(bImage, "jpg", outputFile);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//	}

	@FXML
	protected void recordVideo(ActionEvent event) {
		if (!recordActive) {
			recordActive = true;
			File file = new File("resources/stop.png");
			Image image = new Image(file.toURI().toString());
			recordIcon.setImage(image);
		} else {
			recordActive = false;
			File file = new File("resources/record.png");
			Image image = new Image(file.toURI().toString());
			recordIcon.setImage(image);
			if (writer.isOpened()) {
				writer.release();
			}
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
			// Instantiating the File class
			File file = new File("Server.txt");
			// Instantiating the PrintStream class
			PrintStream stream = new PrintStream(file);
			System.out.println("From now on " + file.getAbsolutePath() + " will be your console");
			System.setOut(stream);
			startTime = System.nanoTime();
			System.out.println("Just trying to connect to camera at: " + startTime);
			controlSocket = new Socket(InetAddress.getLocalHost(), 8000);
			System.out.println("Just connected to " + controlSocket.getRemoteSocketAddress()
					+ " for sending start camera instruction");
			OutputStream outToServer = controlSocket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outToServer);

			try {
				keypair = RSA.generateRSAKkeyPair();
			} catch (Exception e1) {
				System.out.println("Cannot generate RSA keypair");
				e1.printStackTrace();
			}
			endTime = System.nanoTime();
			System.out.println("RSA keys generated at " + (double) (endTime - startTime) / 1000000);

			EndsInstruction firstInstruction = new EndsInstruction("Start Camera", keypair.getPublic());

			out.writeObject(firstInstruction);
			endTime = System.nanoTime();
			System.out.println("Public key sent at " + (double) (endTime - startTime) / 1000000);
			controlSocket.close();

			this.cameraActive = true;
			this.grayCheckBox.setSelected(false);
			this.logoCheckBox.setSelected(false);
			this.haarCheckBox.setSelected(true);
			this.checkboxSelection(
					"C:/Users/radha/Documents/MyFirstJFXApp/src/application/resources/haarcascades/haarcascade_frontalface_alt.xml");
			this.encSlider.setValue(instr.getEncryptDouble());
			this.encPercent.setText(String.valueOf(instr.getEncryptDouble()));
			encSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
				encPercent.setText(Double.toString(newValue.doubleValue()));
			});
			encPercent.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (newValue.matches("\\d{0,2}([\\.]\\d{0,1})?")) {
						encSlider.setValue(Double.parseDouble(newValue));
					}
				}
			});

			try {
				t = new DataServer(3000);
				t.setDaemon(true);
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.cameraButton.setText("Stop Camera");
		} else {
			endTime = System.nanoTime();
			System.out.println("Camera closed at " + (double) (endTime - startTime) / 1000000);
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the cameraButton content
			this.cameraButton.setText("Start Camera");
			controlSocket = new Socket(InetAddress.getLocalHost(), 8000);
			System.out.println("Just connected to " + controlSocket.getRemoteSocketAddress()
					+ " for sending stop camera instruction");
			OutputStream outToServer = controlSocket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outToServer);

			EndsInstruction lastInstruction = new EndsInstruction("Stop Camera", null);

			out.writeObject(lastInstruction);
			controlSocket.close();
			// stop the timer
			this.stopAcquisition();
		}
	}

	private Mat recieveAndDecryptFrame(Socket server) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(server.getInputStream());
		} catch (IOException e2) {
			System.out.println("Can't get datainputstream from Server at 3000");
			e2.printStackTrace();
		}

		int len = 0;
		try {
			len = in.readInt();
		} catch (IOException e) {
			System.out.println("Can't read length of array from bytes");
			e.printStackTrace();
		}
		incomingByteArray = new byte[len];
		if (len > 0) {
			try {
				in.readFully(incomingByteArray);
			} catch (IOException e) {
				System.out.println("Can't read transmitted bytes");
				e.printStackTrace();
			}
		}

		System.out.println("----------------------------------------------------------------------------");
		endTime = System.nanoTime();
		System.out.println("Frame recieved at " + (double) (endTime - startTime) / 1000000);
		bb = ByteBuffer.wrap(incomingByteArray);
		System.out.println("The Recieved image length is " + incomingByteArray.length);

		encryptedByteArray = new byte[incomingByteArray.length - 256];
		bb.get(encryptedByteArray);
		bb.get(encOverBytes20);

		try {
			overBytes20 = cipherRSA.decrypt(encOverBytes20, keypair.getPrivate());
		} catch (Exception e1) {
			System.out.println("Cannot decrypt the key, nonce and counter");
			e1.printStackTrace();
		}
		endTime = System.nanoTime();
		System.out.println("Key, nonce and counter decrypted at " + (double) (endTime - startTime) / 1000000);
		bb = ByteBuffer.wrap(overBytes20);
		bb.get(keyArray);
		bb.get(nonce20);
		bb.get(counterArray);

		len = encryptedByteArray.length;
		if (instr.getEncryptDouble() != 0) {
			numEncBlock = (int) ((len * instr.getEncryptDouble() + 6399) / 6400);
			unencGap = len / numEncBlock;
		} else {
			numEncBlock = 0;
			unencGap = len;
		}
		key20 = new SecretKeySpec(keyArray, 0, keyArray.length, "ChaCha20");
		counter20 = ByteBuffer.wrap(counterArray).getInt();

		System.out.println("\n---Encrypted bytes recieved at user---");
		System.out.println("Key       (hex): " + BytesToHex(key20.getEncoded()));
		System.out.println("Nonce     (hex): " + BytesToHex(nonce20));
		System.out.println("Counter        : " + counter20);
		endTime = System.nanoTime();
		System.out.println("Starting data decryption at " + (double) (endTime - startTime) / 1000000);
		ccTime1 = endTime;

		try {
			viewedByteArray = cipherCC20.decrypt(encryptedByteArray, key20, nonce20, counter20, 0, unencGap,
					numEncBlock, instr.getEncryptDouble());
		} catch (Exception e) {
			System.out.println("Output cant be decrypted");
			e.printStackTrace();
		} // decrypt

		endTime = System.nanoTime();
		System.out.println("Data decrypted at " + (double) (endTime - startTime) / 1000000);
		ccTime2 = endTime;
		if (ccMinTime > (double) ((ccTime2 - ccTime1) / 1000000.000000)) {
			ccMinTime = (double) ((ccTime2 - ccTime1) / 1000000.000000);
		}
		ccSumTime += (ccTime2 - ccTime1);
		System.out.println("- ChaCha20 decryption min time: " + ccMinTime);
		System.out.println("- ChaCha20 decryption avg time: " + ccSumTime / (1000000 * counter20));

		userFrame = Imgcodecs.imdecode(new MatOfByte(viewedByteArray), Imgcodecs.IMREAD_UNCHANGED);

		snapButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				File outputFile = new File("resources/ss.jpg");
				BufferedImage bImage = null;
				try {
					bImage = MatToBufferedImage(userFrame);
				} catch (IOException e1) {
					System.out.println("Decrypted frame can't be coverted to Buffered image");
					e1.printStackTrace();
				}
				try {
					ImageIO.write(bImage, "jpg", outputFile);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		if (logoCheckBox.isSelected()) {
			this.logoMat = Imgcodecs.imread("resources/dp.jpg");
			if (this.logoMat != null) {
				Rect roi = new Rect(0, 0, logoMat.cols(), logoMat.rows());
				Mat imageROI = userFrame.submat(roi);
				// add the logoMat: method #1
				Core.addWeighted(imageROI, 1.0, logoMat, 1.0, 0.0, imageROI);
			}
			if (!instr.isLogoBool()) {
				instr.setLogoBool(true);
			}
		} else {
			if (instr.isLogoBool())
				instr.setLogoBool(false);
		}

		if (grayCheckBox.isSelected()) {
			Imgproc.cvtColor(userFrame, userFrame, Imgproc.COLOR_BGR2GRAY);
			if (!instr.isGrayBool())
				instr.setGrayBool(true);
		} else {
			if (instr.isGrayBool())
				instr.setGrayBool(false);
		}

		if (haarCheckBox.isSelected()) {
			if (!instr.isHaarBool()) {
				instr.setHaarBool(true);
				instr.setLbpBool(false);
			}
		} else {
			if (instr.isHaarBool()) {
				instr.setHaarBool(false);
				instr.setLbpBool(true);
			}
		}

		if (lbpCheckBox.isSelected()) {
			if (!instr.isLbpBool()) {
				instr.setLbpBool(true);
				instr.setHaarBool(false);
			}
		} else {
			if (instr.isLbpBool()) {
				instr.setLbpBool(false);
				instr.setHaarBool(true);
			}
		}

		if (decryptCheckBox.isSelected()) {
			encDecUserFrame = Imgcodecs.imdecode(new MatOfByte(viewedByteArray), Imgcodecs.IMREAD_UNCHANGED);
			if (!encDecUserFrame.empty()) {
				encDecUserImage = Utils.mat2Image(encDecUserFrame);
				updateImageView(currentFrame, encDecUserImage);
			}
			if (!instr.isDecryptBool())
				instr.setDecryptBool(true);
		} else {
			encDecUserFrame = Imgcodecs.imdecode(new MatOfByte(encryptedByteArray), Imgcodecs.IMREAD_UNCHANGED);
//			frameNo++;
			// convert and show the frame
			if (!encDecUserFrame.empty()) {
				encDecUserImage = Utils.mat2Image(encDecUserFrame);
				updateImageView(currentFrame, encDecUserImage);
			} else {
				System.out.println("The transmitted frame is too encrypted to show at user");
			}
			if (instr.isDecryptBool())
				instr.setDecryptBool(false);
		}
		endTime = System.nanoTime();
		System.out.println("Encrypted frame displayed at " + (double) (endTime - startTime) / 1000000);
		if (encSlider.getValue() != instr.getEncryptDouble()) {
			instr.setEncryptDouble(encSlider.getValue());
		}

		try {
			instrBytes = objectToBytes(instr);
			System.out.println("The length of intruction byte array is " + instrBytes.length);
		} catch (IOException e) {
			System.out.println("Can't convert instruction to bytes");
			e.printStackTrace();
		}
		endTime = System.nanoTime();
		System.out.println("Starting Instruction encryption at " + (double) (endTime - startTime) / 1000000);
		rsaTime1 = endTime;

		try {
			encInstrbytes = cipherRSA.encrypt(instrBytes, keypair.getPrivate());
		} catch (Exception e1) {
			System.out.println("Can't encrypt instruction");
			e1.printStackTrace();
		}

		endTime = System.nanoTime();
		System.out.println("Instruction encrypted at " + (double) (endTime - startTime) / 1000000);
		rsaTime2 = endTime;
		if (rsaMinTime > (double) ((rsaTime2 - rsaTime1) / 1000000.000000)) {
			rsaMinTime = (double) ((rsaTime2 - rsaTime1) / 1000000.000000);
		}
		rsaSumTime += (rsaTime2 - rsaTime1);
		System.out.println("- RSA encryption min time: " + rsaMinTime);
		System.out.println("- RSA encryption avg time: " + rsaSumTime / (1000000 * counter20));
		try {
			sendBytes(encInstrbytes, 0, encInstrbytes.length, server);
		} catch (IOException e) {
			System.out.println("Can't send instruction bytes");
			e.printStackTrace();
		}
		endTime = System.nanoTime();
		System.out.println("Instruction sent at " + (double) (endTime - startTime) / 1000000);

		// show the histogram
		this.showHistogram(userFrame, grayCheckBox.isSelected());
		// face detection
		this.faceDetectAndDisplay(userFrame);

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("\n");
		return userFrame;
	}

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
		} catch (IOException e1) {
			System.out.println("Can't stop Dataserver socket");
			e1.printStackTrace();
		}

		try {
			if (dataServerSocket != null)
				dataServerSocket.close();
		} catch (IOException e) {
			System.out.println("Can't close serversocket");
			e.printStackTrace();
		}

		if (recordActive) {
			recordButton.fire();
		}

		if (this.cameraCapture.isOpened()) {
			// release the camera
			this.cameraCapture.release();
		}
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

	public byte[] readBytes(Socket socket) {
		// Again, probably better to store these objects references in the support class
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

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view  the {@link ImageView} to update
	 * @param image the {@link Image} to show
	 */
	private static void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */

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

	private static String BytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte temp : bytes) {
			result.append(String.format("%02x", temp));
		}
		return result.toString();
	}

	private byte[] objectToBytes(Instruction instr) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(instr);
			return bos.toByteArray();
		}
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
}