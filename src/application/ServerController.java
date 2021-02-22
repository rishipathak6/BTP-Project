package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
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
import javafx.concurrent.Task;
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
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 *
 */
public class ServerController {
//	private int port;
//
//	public ServerController(int port, Consumer<Serializable> onReceiveCallback) {
//		super(onReceiveCallback);
//		this.port = port;
//	}

	// the FXML button
	@FXML
	private Button button;
	// the FXML image view
	@FXML
	private ImageView currentFrame;
	@FXML
	private ImageView encryptedFrame;
	@FXML
	private CheckBox grayscale;
	@FXML
	private CheckBox logoCheckBox;
	@FXML
	private ImageView histogram;
	@FXML
	private CheckBox haarClassifier;
	@FXML
	private CheckBox lbpClassifier;
	@FXML
	private Slider encPercent;
	@FXML
	private TextField encText;
	@FXML
	private CheckBox decryptCheck;
	private double percentage;
	private int frameno = 1;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	private Mat logo;
	// the id of the camera to be used
	private static int cameraId = 0;
	private CascadeClassifier faceCascade = new CascadeClassifier();
	private int absoluteFaceSize = 0;
	private BufferedImage obj;
	private byte[] byteArray;
	private byte[] byteBlock;
	private int len;
	private int numencblock;
	private int unencgap;
	private boolean onceStarted;
	private final int NONCE_LEN = 12; // 96 bits, 12 bytes
	private final int MAC_LEN = 16; // 128 bits, 16 bytes
	ChaCha20 cipherCC20 = new ChaCha20();
	ChaCha20Poly1305 cipherCCP = new ChaCha20Poly1305();
	SecretKey key;
	SecretKey key20;
	byte[] nonce20 = new byte[12]; // 96-bit nonce (12 bytes)
	int counter20 = 1; // 32-bit initial count (8 bytes)
	private byte[] cText20;
	private byte[] pText20;
	private ByteBuffer bb;
	private byte[] keyArray = new byte[32]; // 32 bytes , 256 bits
	private byte[] counterArray = new byte[4]; // 4 bytes , 32 bits

	private byte[] cText;
	private byte[] pText;

	private byte[] originalCText;

	private OutputStream destStream;
	private InetAddress receiverAddress;

//	private static class service extends ScheduledService<byte[]> {
//		private ServerSocket serverSocket;
//
//		public service(ServerSocket serverSocket) {
//			this.serverSocket = serverSocket;
//		}
//
//		@Override
//		protected Task<byte[]> createTask() {
//			return new Task<byte[]>() {
//
//				public byte[] readBytes(Socket socket) {
//					// Again, probably better to store these objects references in the support class
//					InputStream in = null;
//					try {
//						in = socket.getInputStream();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						System.out.println("Can't read bytes from socket");
//						e.printStackTrace();
//					}
//					DataInputStream dis = new DataInputStream(in);
//
//					int len = 0;
//					try {
//						len = dis.readInt();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						System.out.println("Can't read length of array from bytes");
//						e.printStackTrace();
//					}
//					byte[] data = new byte[len];
//					if (len > 0) {
//						try {
//							dis.readFully(data);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							System.out.println("Can't read transmitted bytes");
//							e.printStackTrace();
//						}
//					}
//					return data;
//
//				}
//
////				private void updateImageView(ImageView view, Image image) {
////					Utils.onFXThread(view.imageProperty(), image);
////				}
//
//				@Override
//				protected byte[] call() {
////					DatagramSocket datagramSocket = new DatagramSocket(80);
//					byte[] buffer = new byte[100000];
////					DatagramPacket packet = null;
//
////					while (true) {
////						packet = new DatagramPacket(buffer, buffer.length);
////					private ServerSocket serverSocket = new ServerSocket(3000);
//					Socket clientSocket = null;
//					while (true) {
//						try {
//							clientSocket = serverSocket.accept();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							System.out.println("Some error while waiting for connection");
//							e.printStackTrace();
//						}
//						// Step 3 : revieve the data in byte buffer.
////							datagramSocket.receive(packet);
//
//						buffer = readBytes(clientSocket);
////						System.out.println("Transmitted (hex): " + convertBytesToHex(buffer));
//						System.out.println("Transmission done");
//						ByteBuffer bb = ByteBuffer.wrap(buffer);
//						byte[] encryptedText = new byte[buffer.length - 48];
//						byte[] nonce20 = new byte[12]; // 12 bytes , 96 bits
//						byte[] keyArray = new byte[32]; // 32 bytes , 256 bits
//						byte[] counterArray = new byte[4]; // 4 bytes , 32 bits
//						bb.get(encryptedText);
//						bb.get(keyArray);
//						bb.get(nonce20);
//						bb.get(counterArray);
//						System.out.println("The transmitted image length is " + buffer.length);
//						Mat frame = Imgcodecs.imdecode(new MatOfByte(encryptedText), Imgcodecs.IMREAD_UNCHANGED);
////						frameno++;
////						 convert and show the frame
//						Image imageToShow = Utils.mat2Image(frame);
//						updateImageView(currentFrame, imageToShow);
//
//						int len = buffer.length;
//						numencblock = (int) ((len * 6.25 + 6399) / 6400);
//						unencgap = len / numencblock;
//						key20 = new SecretKeySpec(keyArray, 0, keyArray.length, "ChaCha20");
//						bb = ByteBuffer.wrap(counterArray);
//						counter20 = bb.getInt();
//						System.out.println("\n---Decryption at user---");
//
//						try {
//							pText20 = cipherCC20.decrypt(encryptedText, key20, nonce20, counter20, 0, unencgap,
//									numencblock);
//						} catch (Exception e) {
//							// TODO Auto-generated catch block
//							System.out.println("Output cant be decrypted");
//							e.printStackTrace();
//						} // decrypt
//						System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
//						System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
//						System.out.println("Counter        : " + counter20);
//						Mat encMat = Imgcodecs.imdecode(new MatOfByte(pText20), Imgcodecs.IMREAD_UNCHANGED);
//						if (!encMat.empty()) {
//							Image imageToShow2 = Utils.mat2Image(encMat);
//							updateImageView(encryptedFrame, imageToShow2);
//						} else {
//							System.out.println("The frame is too encrypted to show at user");
//						}
//
//					}
//
//					// grab a frame every 33 ms (30 frames/sec)
////							Runnable frameGrabber = new Runnable() {
//					//
////								@Override
////								public void run() {
//
////								}
////							};
//					// effectively grab and process a single frame
////					Mat frame = Imgcodecs.imdecode(new MatOfByte(buffer), Imgcodecs.IMREAD_UNCHANGED);
//////								frameno++;
////					// convert and show the frame
////					Image imageToShow = Utils.mat2Image(frame);
////					updateImageView(currentFrame, imageToShow);
////							
////					}
////					serverSocket.close();
////					return buffer;
//
//				}
//			};
//
//		}
//	}

	public class GreetingServer extends Thread {
		private ServerSocket serverSocket;

		public GreetingServer(int port) throws IOException {
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

						System.out.println(
								"----------------------------------------------------------------------------");
						bb = ByteBuffer.wrap(buffer);
						System.out.println("The Recieved image length is " + buffer.length);

						byte[] encryptedText = new byte[buffer.length - 48];
						bb.get(encryptedText);
						bb.get(keyArray);
						bb.get(nonce20);
						bb.get(counterArray);
						Mat frame = Imgcodecs.imdecode(new MatOfByte(encryptedText), Imgcodecs.IMREAD_UNCHANGED);
//						frameno++;
						// convert and show the frame
						if (!frame.empty()) {
							Image imageToShow = Utils.mat2Image(frame);
							updateImageView(currentFrame, imageToShow);
						} else {
							System.out.println("The transmitted frame is too encrypted to show at user");
						}

						len = encryptedText.length;
						numencblock = (int) ((len * 6.25 + 6399) / 6400);
						unencgap = len / numencblock;
						key20 = new SecretKeySpec(keyArray, 0, keyArray.length, "ChaCha20");
						counter20 = ByteBuffer.wrap(counterArray).getInt();

						System.out.println("\n---Encrypted bytes recieved at user---");
						System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
						System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
						System.out.println("Counter        : " + counter20);

						try {
							pText20 = cipherCC20.decrypt(encryptedText, key20, nonce20, counter20, 0, unencgap,
									numencblock);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							System.out.println("Output cant be decrypted");
							e.printStackTrace();
						} // decrypt
						if (!dataIsValidJPEG(pText20)) {
							System.out.println("The decrypted image is not a valid JPEG");
						}
						System.out.println("The first two bytes of decrypted image are " + pText20[0] + pText20[1]
								+ " The last two bytes are " + pText20[len - 2] + pText20[len - 1]);
						System.out.println("\n---Decryption at user---");

						System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
						System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
						System.out.println("Counter        : " + counter20);
						Mat encMat = Imgcodecs.imdecode(new MatOfByte(pText20), Imgcodecs.IMREAD_UNCHANGED);
						if (!encMat.empty()) {
							Image imageToShow2 = Utils.mat2Image(encMat);
							updateImageView(encryptedFrame, imageToShow2);
						} else {
							System.out.println("The frame is too encrypted to show at user");
						}

						DataOutputStream out = null;
						try {
							out = new DataOutputStream(server.getOutputStream());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							out.writeUTF(
									"Thank you for connecting to " + server.getLocalSocketAddress() + "\nGoodbye!");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println(
								"----------------------------------------------------------------------------");
						System.out.println("\n");

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
	 * The action triggered by pushing the button on the GUI
	 *
	 * @param event the push button event
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

			this.cameraActive = true;
			this.haarClassifier.setSelected(true);
			this.checkboxSelection(
					"C:/Users/radha/Documents/MyFirstJFXApp/src/application/resources/haarcascades/haarcascade_frontalface_alt.xml");

			encPercent.valueProperty().addListener((observable, oldValue, newValue) -> {
				encText.setText(Double.toString(newValue.doubleValue()));
			});
			encText.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (newValue.matches("\\d{0,2}([\\.]\\d{0,1})?")) {
						encPercent.setValue(Double.parseDouble(newValue));
					}
				}
			});

			try {
				Thread t = new GreetingServer(3000);
				t.setDaemon(true);
				t.start();
//			GreetingServer frameReceiver = new GreetingServer(3000);
//			this.timer = Executors.newSingleThreadScheduledExecutor();
//			this.timer.scheduleAtFixedRate(frameReceiver, 0, 33, TimeUnit.MILLISECONDS);
			} catch (IOException e) {
				e.printStackTrace();
			}

//		Thread thread = new Thread(task);
//		thread.setDaemon(true);
//		thread.start();
//		service.setDelay(Duration.seconds(5));

//		ServerSocket serverSocket = new ServerSocket(3000);

//		service service = new service(serverSocket);
//		service.setPeriod(Duration.seconds(1));
////		service.restartOnFailure(true);
//		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
//
//			@Override
//			public void handle(WorkerStateEvent arg0) {
//				// TODO Auto-generated method stub
//				System.out.println("Transmission successful");
////				bb = ByteBuffer.wrap(service.getValue());
////				byte[] encryptedText = new byte[service.getValue().length - 48];
////				bb.get(encryptedText);
////				bb.get(keyArray);
////				bb.get(nonce20);
////				bb.get(counterArray);
////				System.out.println("The transmitted image length is " + service.getValue().length);
////				Mat frame = Imgcodecs.imdecode(new MatOfByte(encryptedText), Imgcodecs.IMREAD_UNCHANGED);
//////				frameno++;
////				// convert and show the frame
////				Image imageToShow = Utils.mat2Image(frame);
////				updateImageView(currentFrame, imageToShow);
////
////				len = service.getValue().length;
////				numencblock = (int) ((len * 6.25 + 6399) / 6400);
////				unencgap = len / numencblock;
////				key20 = new SecretKeySpec(keyArray, 0, keyArray.length, "ChaCha20");
////				bb = ByteBuffer.wrap(counterArray);
////				counter20 = bb.getInt();
////				System.out.println("\n---Decryption at user---");
////
////				try {
////					pText20 = cipherCC20.decrypt(encryptedText, key20, nonce20, counter20, 0, unencgap, numencblock);
////				} catch (Exception e) {
////					// TODO Auto-generated catch block
////					System.out.println("Output cant be decrypted");
////					e.printStackTrace();
////				} // decrypt
////				System.out.println("Key       (hex): " + convertBytesToHex(key20.getEncoded()));
////				System.out.println("Nonce     (hex): " + convertBytesToHex(nonce20));
////				System.out.println("Counter        : " + counter20);
////				Mat encMat = Imgcodecs.imdecode(new MatOfByte(pText20), Imgcodecs.IMREAD_UNCHANGED);
////				if (!encMat.empty()) {
////					Image imageToShow2 = Utils.mat2Image(encMat);
////					updateImageView(encryptedFrame, imageToShow2);
////				} else {
////					System.out.println("The frame is too encrypted to show at user");
////				}
////				service.restart();
//			}
//		});
//
//		service.start();
			// update the button content
//		if (onceStarted) {
//			service.restart();
//		} else {
//			service.start();
//			onceStarted = true;
			button.setText("Stop Camera");
//		}
//		this.button.setText("Stop Camera");

//		} else {
//			// log the error
//			System.err.println("Transmission not started yet...");
//		}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	@FXML
	protected void loadLogo() {
		if (logoCheckBox.isSelected())
			this.logo = Imgcodecs.imread("resources/Poli.png");
	}
//
//	Task<byte[]> task = new Task<byte[]>() {
//		@Override
//		protected byte[] call() throws Exception {
////			DatagramSocket datagramSocket = new DatagramSocket(80);
//			byte[] buffer = new byte[100000];
////			DatagramPacket packet = null;
//			ServerSocket serverSocket = new ServerSocket(3000);
//			Socket clientSocket = serverSocket.accept();
////			while (true) {
////				packet = new DatagramPacket(buffer, buffer.length);
//
//			// Step 3 : revieve the data in byte buffer.
////				datagramSocket.receive(packet);
//
//			buffer = readBytes(clientSocket);
////				System.out.println("Transmitted (hex): " + convertBytesToHex(buffer));
//			// grab a frame every 33 ms (30 frames/sec)
////					Runnable frameGrabber = new Runnable() {
//			//
////						@Override
////						public void run() {
//
////						}
////					};
//			// effectively grab and process a single frame
//			Mat frame = Imgcodecs.imdecode(new MatOfByte(buffer), Imgcodecs.IMREAD_UNCHANGED);
////						frameno++;
//			// convert and show the frame
//			Image imageToShow = Utils.mat2Image(frame);
//			updateImageView(currentFrame, imageToShow);
////					
////			}
//			serverSocket.close();
//			return buffer;
//		}
//
//		@Override
//		protected void succeeded() {
//			super.succeeded();
//			updateMessage("Done!");
//		}
//
//		@Override
//		protected void cancelled() {
//			super.cancelled();
//			updateMessage("Cancelled!");
//		}
//
//		@Override
//		protected void failed() {
//			super.failed();
//			updateMessage("Failed!");
//		}
//	};

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
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);

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
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);

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
		this.button.setDisable(false);
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
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
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

	// A 256-bit secret key (32 bytes)
	private static SecretKey getKey() {
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("ChaCha20");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Problem in identifying algo in keygen");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			keyGen.init(256, SecureRandom.getInstanceStrong());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot init keygen");
			e.printStackTrace();
		}
		return keyGen.generateKey();
	}

	// 96-bit nonce (12 bytes)
	private static byte[] getNonce() {
		byte[] newNonce = new byte[12];
		new SecureRandom().nextBytes(newNonce);
		return newNonce;
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

//	@Override
//	protected boolean isServer() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	@Override
//	protected String getIP() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	protected int getPort() {
//		// TODO Auto-generated method stub
//		return port;
//	}

}