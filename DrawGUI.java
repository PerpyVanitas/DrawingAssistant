package bugpatch.master;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Integer.parseInt;

public class DrawGUI extends JFrame {

	public JPanel jp;
	public DrawFunction df;
	public JFrame currentFrame;
	public JPanel statusJP;
	public JLabel progressLabel;
	public boolean isStatusPanel = false;
	public JButton backButton;

	public boolean dialogueOpen = false;

    public DrawGUI() {
		super("Drawing Assistant");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 80);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth();
		double height = screenSize.getHeight();
		setLocation((int) width / 5, (int) height / 5);
		setAlwaysOnTop(true);
		setResizable(false);
		jp = new JPanel(new FlowLayout());
		Color c = new Color(Color.WHITE.getRGB()); // Set background color to white
		jp.setBackground(c);
		jp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		jp.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				bringCorrectFrameToFront();
			}
		});
		add(jp);
		statusJP = new JPanel(new FlowLayout());
		statusJP.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		progressLabel = new JLabel("");
		backButton = new JButton("Back");
		backButton.setEnabled(false);  // Initially, set to false
		backButton.addActionListener(e -> disableStatusPanel());
		statusJP.add(progressLabel);
		statusJP.add(backButton);
		loadAndPrepareComponents();
		df = new DrawFunction(backButton, this); // Pass DrawGUI instance to DrawFunction
		setVisible(true);
	}

	public void bringCorrectFrameToFront() {
		if (dialogueOpen) {
			if (currentFrame != null) {
				currentFrame.toFront();
				currentFrame.repaint();
			}
		}
	}

	public void disableMainFrame() {
		if (!dialogueOpen) {
			for (Component c : jp.getComponents()) {
				if (!(c instanceof JButton)) {
					c.setEnabled(false);
					c.addMouseListener(new MouseAdapter() {
						@Override
						public void mousePressed(MouseEvent e) {
							bringCorrectFrameToFront();
						}
					});
				}
			}
			setFocusable(false);
			dialogueOpen = true;
		}
	}

	public void enableMainFrame() {
		if (dialogueOpen) {
			for (Component c : jp.getComponents()) {
				if (!(c instanceof JButton)) {
					c.setEnabled(true);
				}
			}
			setFocusable(true);
			dialogueOpen = false;
		}
	}

	public void enableStatusPanel() {
		if (!isStatusPanel) {
			this.remove(jp);
			jp.revalidate();
			jp.repaint();
			this.add(statusJP);
			progressLabel.setText("");
			jp.revalidate();
			jp.repaint();
			isStatusPanel = true;
			backButton.setVisible(false);  // Hide back button initially
		}
	}

	public void disableStatusPanel() {
		if (isStatusPanel) {
			this.remove(statusJP);
			jp.revalidate();
			jp.repaint();
			this.add(jp);
			jp.revalidate();
			jp.repaint();
			isStatusPanel = false;
			backButton.setVisible(true);  // Show back button after completion
		}
	}

	public void loadAndPrepareComponents() {
		JButton imageButton = new JButton("Choose Image");
		imageButton = configureBarButton(imageButton);
		jp.add(imageButton);
		imageButton.addActionListener(e -> {
			if (!dialogueOpen) {
				imageDialogue();
			}
		});
	}

	public void imageDialogue() {
		disableMainFrame();
		JFrame imageD = new JFrame();
		currentFrame = imageD;
		imageD.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				enableMainFrame();
			}
		});
		imageD.setAlwaysOnTop(true);
		imageD.setResizable(false);
		imageD.setLocation(DrawAssistant.drawGUI.getX(), DrawAssistant.drawGUI.getY());
		imageD.setLayout(new GridLayout(6, 2)); // Adjusted layout for 6 rows and 2 columns

		// Add components to the setter window
		JLabel redLabel = new JLabel("Red Tolerance");
		JTextArea redInput = new JTextArea("180"); // Preset value
		imageD.add(redLabel);
		imageD.add(redInput);

		JLabel greenLabel = new JLabel("Green Tolerance");
		JTextArea greenInput = new JTextArea("180"); // Preset value
		imageD.add(greenLabel);
		imageD.add(greenInput);

		JLabel blueLabel = new JLabel("Blue Tolerance");
		JTextArea blueInput = new JTextArea("180"); // Preset value
		imageD.add(blueLabel);
		imageD.add(blueInput);

		JLabel scaleLabel = new JLabel("Scale");
		JTextArea scaleInput = new JTextArea("1.0"); // Preset value
		imageD.add(scaleLabel);
		imageD.add(scaleInput);

		JLabel delayLabel = new JLabel("Delay");
		JTextArea delayInput = new JTextArea("5"); // Preset value
		imageD.add(delayLabel);
		imageD.add(delayInput);

		JButton drawimage = new JButton("Draw Image");
		drawimage.setBackground(Color.DARK_GRAY);
		drawimage.setForeground(Color.WHITE);
		imageD.add(drawimage);

		// Action listener for the Draw Image button
		drawimage.addActionListener(e -> {
			if (isDouble(redInput.getText()) && isDouble(greenInput.getText()) && isDouble(blueInput.getText())
					&& isDouble(scaleInput.getText()) && isDouble(delayInput.getText())) {

				int redTolerance = parseInt(redInput.getText());
				int greenTolerance = parseInt(greenInput.getText());
				int blueTolerance = parseInt(blueInput.getText());
				float scale = Float.parseFloat(scaleInput.getText());
				int delay = parseInt(delayInput.getText());

				imageD.setVisible(false);
				JFileChooser imageChooser = new JFileChooser();
				int fileChosen = imageChooser.showOpenDialog(null);
				if (fileChosen == JFileChooser.APPROVE_OPTION) {
					imageD.dispose(); // Hide the setter window
					enableMainFrame();
					File selectedImage = imageChooser.getSelectedFile();

					if (!DrawAssistant.isDrawing) {
						enableStatusPanel();
						drawWait();
						new Timer().schedule(new TimerTask() {
							@Override
							public void run() {
								df.drawImage(progressLabel, selectedImage, delay, null, true,
										redTolerance, greenTolerance, blueTolerance, scale);
							}
						}, 5000);
					} else {
						JOptionPane.showMessageDialog(null, "Program is still drawing", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			} else {
				imageD.dispose();
				enableMainFrame();
				JOptionPane.showMessageDialog(null, "Only numbers should be put into the textboxes", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});

		imageD.pack();
		imageD.setVisible(true);
	}


	private void drawWait() {
		backButton.setVisible(false);
		progressLabel.setText("<html>Please position your mouse<br>Drawing in " + (5 + 1) + "</html>");
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				String currentTimeText = progressLabel.getText();
				int begin = currentTimeText.indexOf("Drawing in ") + 11;
				int end = currentTimeText.length() - 7;
				int current = parseInt(currentTimeText.substring(begin, end));
				progressLabel.setText("<html>Please position your mouse<br>Drawing in " + (current - 1) + "</html>");
				if (current == 1) {
					t.cancel();
					backButton.setVisible(true);
					backButton.setEnabled(false);
				}
			}
		}, 0, 1000);
	}

	public boolean isDouble(String s) {
		try {
			Double.valueOf(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public JButton configureBarButton(JButton b) {
		Color c = new Color(Color.WHITE.getRGB()); // Set text color to black
		b.setBackground(c);
		b.setFocusable(false);
		b.setForeground(Color.DARK_GRAY);
		return b;
	}
}
