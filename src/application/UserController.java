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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
 * implemented. It handles the cameraButton for starting/stopping the camera and the
 * acquired video stream.
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
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the cameraButton behavior
	private boolean cameraActive = false;
	private Mat logo;
	private CascadeClassifier faceCascade = new CascadeClassifier();
	private int absoluteFaceSize = 0;
	private int numencblock;
	private int unencgap;
	ChaCha20 cipherCC20 = new ChaCha20();
	ChaCha20Poly1305 cipherCCP = new ChaCha20Poly1305();
	SecretKey key;
	SecretKey key20;
	byte[] nonce20 = new byte[12]; // 96-bit nonce (12 bytes)
	int counter20 = 1; // 32-bit initial count (8 bytes)
	private byte[] pText20;
	private ByteBuffer bb;
	private byte[] keyArray = new byte[32]; // 32 bytes , 256 bits
	private byte[] counterArray = new byte[4]; // 4 bytes , 32 bits

	private Socket controlSocket;

	private instruction instr = new instruction(false, false, true, false, false, 6.25);
	private byte[] instrbytes;
	public class DataServer extends Thread {
		private ServerSocket serverSocket;

		public DataServer(int port) throws IOException {
			serverSocket = new ServerSocket(port);
//			serverSocket.setSoTimeout(10000);
		}

		public void run() {
//			while (true) {
			try {
				System.out.println("----------------------------------------------------------------------------");
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
				Socket server = serverSocket.accept();
				System.out.println("Just connected to " + server.getRemoteSocketAddress());

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameReceiver = new Runnable() {

					@Override
					public void run() {

						Mat frame = recieveAndDecryptFrame(server);
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(encryptedFrame, imageToShow);
					}
				};

				timer = Executors.newSingleThreadScheduledExecutor();
				timer.scheduleAtFixedRate(frameReceiver, 0, 33, TimeUnit.MILLISECONDS);
				System.out.println("----------------------------------------------------------------------------");
				System.out.println("\n");
//					server.close();

			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
//					break;
			} catch (IOException e) {
				e.printStackTrace();
//					break;
			}
//			}
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
			// start the video capture
//			this.capture.open(cameraId);
//			new EchoServer().start();
//			client = new EchoClient();
			// is the video stream available?
//		if (this.capture.isOpened()) {
			// Instantiating the File class
			File file = new File("Server.txt");
			// Instantiating the PrintStream class
			PrintStream stream = new PrintStream(file);
			System.out.println("From now on " + file.getAbsolutePath() + " will be your console");
			System.setOut(stream);

			controlSocket = new Socket(InetAddress.getLocalHost(), 8000);
			System.out.println("Just connected to " + controlSocket.getRemoteSocketAddress()
					+ " for sending start camera instruction");
			OutputStream outToServer = controlSocket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);

			out.writeUTF("Start Camera");
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
				Thread t = new DataServer(3000);
				t.setDaemon(true);
				t.start();
//			DataServer frameReceiver = new DataServer(3000);
//			this.timer = Executors.newSingleThreadScheduledExecutor();
//			this.timer.scheduleAtFixedRate(frameReceiver, 0, 33, TimeUnit.MILLISECONDS);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.cameraButton.setText("Stop Camera");
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the cameraButton content
			this.cameraButton.setText("Start Camera");
			controlSocket = new Socket(InetAddress.getLocalHost(), 8000);
			System.out.println("Just connected to " + controlSocket.getRemoteSocketAddress()
					+ " for sending stop camera instruction");
			OutputStream outToServer = controlSocket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);

			out.writeUTF("Stop Camera");
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
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		int len = 0;
		try {
			len = in.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't read length of array from bytes");
			e.printStackTrace();
		}
		byte[] buffer = new byte[len];
		if (len > 0) {
			try {
				in.readFully(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Can't read transmitted bytes");
				e.printStackTrace();
			}
		}

		System.out.println("----------------------------------------------------------------------------");
		bb = ByteBuffer.wrap(buffer);
		System.out.println("The Recieved image length is " + buffer.length);

		byte[] encryptedText = new byte[buffer.length - 48];
		bb.get(encryptedText);
		bb.get(keyArray);
		bb.get(nonce20);
		bb.get(counterArray);
		Mat frame = Imgcodecs.imdecode(new MatOfByte(encryptedText), Imgcodecs.IMREAD_UNCHANGED);
//		frameno++;
		// convert and show the frame
		if (!frame.empty()) {
			Image imageToShow = Utils.mat2Image(frame);
			updateImageView(currentFrame, imageToShow);
		} else {
			System.out.println("The transmitted frame is too encrypted to show at user");
		}

		len = encryptedText.length;
		numencblock = (int) ((len * instr.getEncryptDouble() + 6399) / 6400);
		unencgap = len / numencblock;
		key20 = new SecretKeySpec(keyArray, 0, keyArray.length, "ChaCha20");
		counter20 = ByteBuffer.wrap(counterArray).getInt();

		System.out.println("\n---Encrypted bytes recieved at user---");
		System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
		System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
		System.out.println("Counter        : " + counter20);

		try {
			pText20 = cipherCC20.decrypt(encryptedText, key20, nonce20, counter20, 0, unencgap, numencblock);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Output cant be decrypted");
			e.printStackTrace();
		} // decrypt
		if (!dataIsValidJPEG(pText20)) {
//			System.out.println("The decrypted image is not a valid JPEG");
		}

		System.out.println("\n---Decryption at user---");

		System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
		System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
		System.out.println("Counter        : " + counter20);
		Mat encMat = Imgcodecs.imdecode(new MatOfByte(pText20), Imgcodecs.IMREAD_UNCHANGED);

		if (logoCheckBox.isSelected()) {
			this.logo = Imgcodecs.imread("resources/Poli.png");
			if (this.logo != null) {
				// Rect roi = new Rect(frame.cols() - logo.cols(), frame.rows() - logo.rows(),
				// logo.cols(),
				// logo.rows());
				Rect roi = new Rect(0, 0, logo.cols(), logo.rows());
				Mat imageROI = encMat.submat(roi);
				// add the logo: method #1
				Core.addWeighted(imageROI, 1.0, logo, 0.2, 0.0, imageROI);

				// add the logo: method #2
				// logo.copyTo(imageROI, logo);
			}
			if (!instr.isLogoBool()) {
				instr.setLogoBool(true);
			}
		} else {
			if (instr.isLogoBool())
				instr.setLogoBool(false);
		}
		
		if (grayCheckBox.isSelected()) {
			Imgproc.cvtColor(encMat, encMat, Imgproc.COLOR_BGR2GRAY);
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
			Image imageToShow = Utils.mat2Image(encMat);
			updateImageView(currentFrame, imageToShow);
			if (!instr.isDecryptBool())
				instr.setDecryptBool(true);
		} else {
			if (instr.isDecryptBool())
				instr.setDecryptBool(false);
		}

		if (encSlider.getValue() != instr.getEncryptDouble()) {
			instr.setEncryptDouble(encSlider.getValue());
		}

		
		try {
			instrbytes = convertToBytes(instr);
		} catch (IOException e) {
			System.out.println("Can't convert instruction to bytes");
			e.printStackTrace();
		}

		try {
			sendBytes(instrbytes, 0, instrbytes.length, server);
		} catch (IOException e) {
			System.out.println("Can't send instruction bytes");
			e.printStackTrace();
		}

//		try {
//			out.flush();
//		} catch (IOException e) {
//			System.out.println("Can't flush dataoutputstream");
//			e.printStackTrace();
//		}
		// show the histogram
		this.showHistogram(encMat, grayCheckBox.isSelected());
		// face detection
		this.detectAndDisplay(encMat);

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("\n");
		return encMat;
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

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
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
	protected void setClosed() {
		this.stopAcquisition();
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
	private void detectAndDisplay(Mat frame) {
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

	private boolean dataIsValidJPEG(byte[] data) {
		if (data == null || data.length < 2)
			return false;

		int totalBytes = data.length;
		String bytes = convertBytesToHex(data);

		System.out.println("The first two bytes of decrypted image are " + bytes.charAt(0) + bytes.charAt(1) + " "
				+ bytes.charAt(2) + bytes.charAt(3) + " The last two bytes are " + bytes.charAt(totalBytes - 4)
				+ bytes.charAt(totalBytes - 3) + " " + bytes.charAt(totalBytes - 2) + bytes.charAt(totalBytes - 1));

		return (bytes.charAt(0) == (char) 0xff && bytes.charAt(1) == (char) 0xd8
				&& bytes.charAt(totalBytes - 2) == (char) 0xff && bytes.charAt(totalBytes - 1) == (char) 0xd9);
	}

	public static BufferedImage Mat2BufferedImage(Mat mat) {
		// Encoding the image
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		// Storing the encoded Mat in a byte array
		byte[] byteArray = matOfByte.toArray();
		// Preparing the Buffered Image
		InputStream in = new ByteArrayInputStream(byteArray);
		BufferedImage bufImage = null;
		try {
			bufImage = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Cant make mat to buffered image");
			e.printStackTrace();
		}
		return bufImage;
	}

	public static byte[] Mat2byteArray(Mat mat) {
		// Encoding the image
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, matOfByte);
		// Storing the encoded Mat in a byte array
		byte[] byteArray = matOfByte.toArray();
		// Preparing the Buffered Image
		return byteArray;
	}

	private static String convertBytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte temp : bytes) {
			result.append(String.format("%02x", temp));
		}
		return result.toString();
	}

	private byte[] convertToBytes(instruction instr) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(instr);
			return bos.toByteArray();
		}
	}

	public byte[] readBytes(Socket socket) {
		// Again, probably better to store these objects references in the support class
		InputStream in = null;
		try {
			in = socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't read bytes from socket");
			e.printStackTrace();
		}
		DataInputStream dis = new DataInputStream(in);

		int len = 0;
		try {
			len = dis.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't read length of array from bytes");
			e.printStackTrace();
		}
		byte[] data = new byte[len];
		if (len > 0) {
			try {
				dis.readFully(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Can't read transmitted bytes");
				e.printStackTrace();
			}
		}
		return data;
	}

}