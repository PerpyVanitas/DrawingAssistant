package bugpatch.master;

import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DrawAssistant {

	public static JFrame drawGUI;

	public static int runningTime = 0;

	public static boolean isDrawing = false;

	public static void main(String[] args) {
		drawGUI = new DrawGUI();

		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		ses.scheduleAtFixedRate(DrawAssistant::addRunningTime, 0, 1, TimeUnit.SECONDS);
	}

	public static void addRunningTime() {
		runningTime++;
	}
}
