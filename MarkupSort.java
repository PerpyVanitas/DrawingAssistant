package bugpatch.master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkupSort {

	private final List<String> titleOrder = new ArrayList<>();
	private final Map<String, String> titleAndData = new HashMap<>();
	private String shape = "";
	private String largeTitle = "";

	public void addData(String data, String title) {
		titleAndData.merge(title, data, (currentData, newData) -> currentData.isEmpty() ? newData : currentData + "!" + newData);
	}

	public void setLargeTitle(String title) {
		largeTitle = title;
	}

	public void addTitle(String title) {
		titleOrder.add(title);
		titleAndData.put(title, "");
	}

	public void setShape(String shape) {
		this.shape = shape;
	}

	public String exportMarkup() {
		StringBuilder markup = new StringBuilder();
		titleOrder.forEach(title -> {
			markup.append("<br><b>").append(title).append("</b>");
			String data = titleAndData.get(title);
			if (data.contains("!")) {
				String[] splitData = data.split("!");
				for (String curData : splitData) {
					markup.append("<br>").append(curData);
				}
			} else {
				markup.append("<br>").append(data);
			}
		});
		if (!largeTitle.isEmpty()) {
			markup.insert(0, "<center><b>" + largeTitle + "</b></center>");
		}
		markup.insert(0, "<html>");
		if (!shape.isEmpty()) {
			markup.insert(0, shape + "!");
		}
		markup.append("</html>");
		return markup.toString();
	}
}
