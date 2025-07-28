package io.chandler.morajai;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.hjson.JsonObject;

public class ResultStatistics {
	public static void main(String[] args) throws IOException {
		File results = new File("/home/cjgriscom/Programming/morajai/results/v3/results-no-result-map.json");
		JsonObject json = JsonObject.readHjson(new FileReader(results)).asObject();

		long totalStates = 0;
		int minSum = Integer.MAX_VALUE;
		int maxSum = 0;
		ArrayList<String> minSumNames = new ArrayList<>();
		ArrayList<String> maxSumNames = new ArrayList<>();

		int minLength = Integer.MAX_VALUE;
		int maxLength = 0;
		ArrayList<String> minLengthNames = new ArrayList<>();
		ArrayList<String> maxLengthNames = new ArrayList<>();

		for (String key : json.names()) {
			JsonObject entry = json.get(key).asObject();
			// Sum the sizeMap
			int length = entry.get("sizeMap").asArray().size() - 1; // 0 doesn't count
			int sum = 0;
			for (int i = 0; i < entry.get("sizeMap").asArray().size(); i++) {
				sum += entry.get("sizeMap").asArray().get(i).asInt();
			}
			totalStates += sum;

			if (length < minLength) {
				minLength = length;
				minLengthNames.clear();
				minLengthNames.add(key);
			} else if (length == minLength) {
				minLengthNames.add(key);
			}

			if (length > maxLength) {
				maxLength = length;
				maxLengthNames.clear();
				maxLengthNames.add(key);
			} else if (length == maxLength) {
				maxLengthNames.add(key);
			}

			if (sum < minSum) {
				minSum = sum;
				minSumNames.clear();
				minSumNames.add(key);
			} else if (sum == minSum) {
				minSumNames.add(key);
			}

			if (sum > maxSum) {
				maxSum = sum;
				maxSumNames.clear();
				maxSumNames.add(key);
			} else if (sum == maxSum) {
				maxSumNames.add(key);
			}
		}
		System.out.println("Min sum: " + minSum + " " + minSumNames);
		System.out.println("Max sum: " + maxSum + " " + maxSumNames);
		System.out.println("Min length: " + minLength + " " + minLengthNames);
		System.out.println("Max length: " + maxLength + " " + maxLengthNames);

		System.out.println("Boxes: " + json.names().size());
		System.out.println("Total states: " + totalStates);
		System.out.println("Average states: " + totalStates / json.names().size());
	}
}
