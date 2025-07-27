package io.chandler.morajai;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class CopyCompleteResults {
	public static void main(String[] args) throws Exception {
		Path src = Paths.get("results/v3");
		System.out.println(src.toAbsolutePath());

		// Loop thru the txt files in the src directory
		List<Path> txtFiles = new ArrayList<>();
		Files.walk(src)
			.filter(Files::isRegularFile)
			.filter(path -> path.getFileName().toString().endsWith(".txt"))
			//.filter(path -> path.getFileName().toString().contains("4954"))
			.forEach(txtFiles::add);

		TreeMap<Integer, JSONObject> results = new TreeMap<>();
		int completeCount = 0;

		for (Path txtFile : txtFiles) {
			JSONObject entry = new JSONObject();
			boolean iscomplete = false;
			JSONObject resultMap = new JSONObject();
			ArrayList<Integer> backtrackedSizeMap = new ArrayList<>();
			ArrayList<Integer> sizeMap = new ArrayList<>();
			int maxDepth = 0;
			int currentBacktrackedDepth = 0;

			
			try (Scanner scanner = new Scanner(txtFile.toFile());) { 
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String header;
				if (line.startsWith(header = "Starting analysis for ")) {
					line = line.substring(header.length());
					String[] parts = line.split(" ");
					String idx = parts[0];
					String name = parts[1];
					String executor = parts[3];
					String pruned = parts[6];
					entry.put("idx", Integer.parseInt(idx));
					entry.put("name", name);
					entry.put("executor", executor);
					entry.put("pruned", Boolean.parseBoolean(pruned));
				} else if (line.startsWith("Complete")) {
					iscomplete = true;
				} else if (line.startsWith(header = "Backedtracked depth ")) {
					line = line.substring(header.length());
					String[] parts = line.split(" ");
					String depth = parts[0];
					String states = parts[2];
					if (Integer.parseInt(depth) != backtrackedSizeMap.size()) throw new RuntimeException("Depth mismatch");
					int size = Integer.parseInt(states);
					backtrackedSizeMap.add(size);
					if (size == 0) resultMap.put(depth, new JSONArray());
					currentBacktrackedDepth = Integer.parseInt(depth);
				} else if (line.startsWith(header = "Depth ")) {
					line = line.substring(header.length());
					String[] parts = line.split(" ");
					String depth = parts[0];
					String states = parts[2];
					if (Integer.parseInt(depth) != sizeMap.size()) throw new RuntimeException("Depth mismatch");
					sizeMap.add(Integer.parseInt(states));
					maxDepth = Math.max(maxDepth, Integer.parseInt(depth));
				} else if (!line.isEmpty() && Character.isDigit(line.charAt(0))) {
					JSONArray result = new JSONArray("[" + line + "]");
					// Bug in GPU getState() leads to reversed state codes
					/*if (entry.getString("executor").equals("GPU")) {
						for (int i = 0; i < result.length(); i++) {
							String code = result.getInt(i) + "";
							// Pad to 9 digits
							while (code.length() < 9) code = "0" + code;
							// Reverse the code
							code = new StringBuilder(code).reverse().toString();
							result.put(i, Integer.parseInt(code));
						}
					}*/
					String key = currentBacktrackedDepth + "";
					resultMap.put(key, result);
				}
			}
			}

			entry.put("maxDepth", maxDepth);
			entry.put("sizeMap", sizeMap);
			entry.put("backtrackedSizeMap", backtrackedSizeMap);
			entry.put("resultMap", resultMap);

			if (iscomplete) {
				results.put(entry.getInt("idx"), entry);
				completeCount++;
			} else {
				System.out.println("Incomplete: " + txtFile.getFileName());
			}
		}

		Path dest = src.resolve("results.json");
		FileWriter fileWriter = new FileWriter(dest.toFile());
		String delim = "{\n";

		Iterator<Map.Entry<Integer, JSONObject>> iterator = results.entrySet().iterator();
		while (iterator.hasNext()) {
			fileWriter.write(delim);
			Map.Entry<Integer, JSONObject> entry = iterator.next();
			fileWriter.write("\"" + entry.getKey() + "\": ");
			fileWriter.write(entry.getValue().toString());
			
			iterator.remove();
			delim = "\n,\n";
		}
		fileWriter.write("\n}");
		fileWriter.close();

		System.out.println("Complete count: " + completeCount);
		System.out.println("Wrote JSON to: " + dest.toAbsolutePath());
	}
}
