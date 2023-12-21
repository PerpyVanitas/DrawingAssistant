package bugpatch.master;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class DrawFunction {
	public HashMap<Integer, ImageHData> imageDrawHistory = new HashMap<>();
	public JButton backButton;
	public DrawGUI drawGUI; // Reference to DrawGUI instance

	private volatile boolean drawingCanceled = false;

	public DrawFunction(JButton b, DrawGUI drawGUI) {
		backButton = b;
		this.drawGUI = drawGUI;
	}

	public void drawImage(JLabel progressLabel, File selectedImage, int delay, BufferedImage i, boolean history,
						  int redTolerance, int greenTolerance, int blueTolerance, float scale) {
		drawingCanceled = false; // Reset the flag
		MarkupSort data = new MarkupSort();
		data.setShape("Image");
		if (selectedImage != null) {
			data.setLargeTitle(selectedImage.getName());
		}
		data.addTitle("Information");
		data.addData("Delay- " + delay, "Information");

		BufferedImage image = loadImage(selectedImage, progressLabel, i);
		if (image == null) {
			return;
		}

		data.addTitle("Actual Size");
		data.addData("Width - " + image.getWidth(), "Actual Size");
		data.addData("Height - " + image.getHeight(), "Actual Size");

		progressLabel.setText("Resizing image");
		if (image.getHeight() > 300) {
			int scaleFactor = image.getHeight() / 300;
			int height = image.getHeight() / scaleFactor;
			int width = image.getWidth() / scaleFactor;
			image = toBufferedImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
		}

		int xS = (int) MouseInfo.getPointerInfo().getLocation().getX();
		int yS = (int) MouseInfo.getPointerInfo().getLocation().getY();

		Robot bot = createRobot();
		if (bot == null) {
			return;
		}

		HashMap<Integer, Point> totalPoints = new HashMap<>();
		ArrayList<Point> pressList = new ArrayList<>();
		ArrayList<Point> releaseList = new ArrayList<>();

		progressLabel.setText("Processing drawing points...0%");

		for (int y = 0; y < image.getHeight(); y++) {
			boolean lastPress = false;
			for (int x = 0; x < image.getWidth(); x++) {
				int totalLoop = image.getHeight() * image.getWidth();
				int current = (y * image.getWidth()) + x;
				float percent = (current * 100.0f) / totalLoop;
				progressLabel.setText("Processing drawing points..." + percent + "%");
				int RGB = image.getRGB(x, y);
				int red = (RGB & 0x00ff0000) >> 16;
				int blue = RGB & 0x000000ff;
				int green = (RGB & 0x0000ff00) >> 8;
				boolean press = false;

				if (red < redTolerance && green < greenTolerance && blue < blueTolerance) {
					if ((RGB >> 24) != 0x00) { // Not transparent
						press = true;
					}
				}

				if (x != 0) {
					if (lastPress != press) {
						lastPress = press;
						if (press) {
							pressList.add(new Point(x, y));
							totalPoints.put(totalPoints.size() + 1, new Point(x, y));
						} else {
							releaseList.add(new Point(x, y));
							totalPoints.put(totalPoints.size() + 1, new Point(x, y));
						}
					}
				} else {
					lastPress = press;
					if (press) {
						pressList.add(new Point(x, y));
						totalPoints.put(totalPoints.size() + 1, new Point(x, y));
					}
				}
			}
		}

		progressLabel.setText("Drawing image...0%");

		for (int x = 1; x != totalPoints.size(); x++) {
			if (!drawingCanceled) {
				float percent = (x * 100.0f) / totalPoints.size();
				progressLabel.setText("Drawing image..." + percent + "%");
				Point currentPoint = totalPoints.get(x);
				if (pressList.contains(currentPoint)) {
					bot.delay(delay);
					bot.mouseRelease(16);
				} else if (releaseList.contains(currentPoint)) {
					bot.delay(delay);
					bot.mousePress(16);
				}

				bot.delay(delay);
				int pointX = (int) (currentPoint.x * scale);
				int pointY = (int) (currentPoint.y * scale);
				bot.mouseMove(xS + pointX, yS + pointY);
			} else {
				// Drawing is canceled
				progressLabel.setText("Drawing canceled.");
				break;
			}
		}

		if (!drawingCanceled) {
			progressLabel.setText("Drawing complete.");

			if (history) {
				saveToHistory(data, image, delay, selectedImage);
			}

			backButton.setEnabled(true);
		} else {
			// Reset the flag and enable back button
			drawingCanceled = false;
			backButton.setEnabled(true);
		}
	}

	private BufferedImage loadImage(File selectedImage, JLabel progressLabel, BufferedImage i) {
		BufferedImage image = (i == null) ? loadImageFromFile(selectedImage, progressLabel) : i;
		if (image == null) {
			handleImageLoadError(progressLabel);
		}
		return image;
	}

	private BufferedImage loadImageFromFile(File selectedImage, JLabel progressLabel) {
		BufferedImage image = null;
		try {
			assert selectedImage != null;
			image = ImageIO.read(selectedImage);
		} catch (IOException e) {
			handleImageLoadError(progressLabel);
		}
		return image;
	}

	private void handleImageLoadError(JLabel progressLabel) {
		progressLabel.setText("An error had occurred");
		backButton.setEnabled(true);
		System.out.println("Exception thrown loading image");
	}

	private void saveToHistory(MarkupSort data, BufferedImage image, int delay, File selectedImage) {
		int runningTime = DrawAssistant.runningTime;
		String markup = data.exportMarkup();

		saveToFile(markup, selectedImage, delay);

		ImageHData idata = new ImageHData(image, delay);
		imageDrawHistory.put(runningTime, idata);
	}

	@SuppressWarnings({"ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
	private void saveToFile(String markup, File selectedImage, int delay) {
		try {
			// Save to Draw History file
			String fileName = "Draw History.txt";
			File file = new File(fileName);
			file.createNewFile();

			// Writing to file
			try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
				// Add Image Name, Drawing Duration, and Date Drawn
				writer.println("1. Image Name: " + (selectedImage != null ? selectedImage.getName() : "N/A"));
				writer.println("2. Drawing Duration: " + delay + " milliseconds");
				writer.println("3. Date Drawn: " + getCurrentDate());
				writer.println(); // Add an empty line for better readability

				// Add markup
				writer.println(markup);
				// Add more data as needed
			}

		} catch (IOException e) {
			System.err.println("Warning: Error saving to Draw History file");
			e.printStackTrace();
		}
	}

	private String getCurrentDate() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(new Date());
	}

	private Robot createRobot() {
		Robot bot = null;
		try {
			bot = new Robot();
		} catch (AWTException e) {
			System.out.println("Problem creating robot");
			backButton.setEnabled(true);
		}
		return bot;
	}

	public BufferedImage toBufferedImage(Image image) {
		if (!(image instanceof BufferedImage)) {
			BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bufferedImage.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
			return bufferedImage;
		} else {
			return (BufferedImage) image;
		}
	}
}
