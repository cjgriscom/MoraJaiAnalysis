package io.chandler.morajai;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.BiFunction;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

public class ResultStats {
	public static void main(String[] args) throws Exception {
		Path resultsFile = Paths.get("results/v3", "results.json");

		int depthRange = 5; // Return results at [MAX - depthRange, MAX]

		int numCPU = 0;
		int numGPU = 0;
		int noResults = 0;
		int maxDepth = 0;
		ArrayList<String> maxDepthNames = new ArrayList<>();

		BiFunction<JsonArray, HashSet<String>, Boolean> filter = (data, colors) -> {
			// Grey mechanic
			boolean middleColGn = data.get(1).asString().equals("GN") || data.get(4).asString().equals("GN") || data.get(7).asString().equals("GN");
			boolean middleColYe = data.get(1).asString().equals("YE") || data.get(4).asString().equals("YE") || data.get(7).asString().equals("YE");
			boolean middleColBu = data.get(1).asString().equals("BU") || data.get(4).asString().equals("BU") || data.get(7).asString().equals("BU");
			boolean middleColPu = data.get(1).asString().equals("PU") || data.get(4).asString().equals("PU") || data.get(7).asString().equals("PU");
			
			boolean twoPurplesInMiddle = (data.get(1).asString().equals("PU") && data.get(4).asString().equals("PU")) ||
				(data.get(4).asString().equals("PU") && data.get(7).asString().equals("PU")) ||
				(data.get(7).asString().equals("PU") && data.get(1).asString().equals("PU"));
			
			boolean twoYellowsInMiddle = (data.get(1).asString().equals("YE") && data.get(4).asString().equals("YE")) ||
				(data.get(4).asString().equals("YE") && data.get(7).asString().equals("YE")) ||
				(data.get(7).asString().equals("YE") && data.get(1).asString().equals("YE"));
			
			boolean twoGreensInMiddle = (data.get(1).asString().equals("GN") && data.get(4).asString().equals("GN")) ||
				(data.get(4).asString().equals("GN") && data.get(7).asString().equals("GN")) ||
				(data.get(7).asString().equals("GN") && data.get(1).asString().equals("GN"));
			
			boolean greyMechanic = middleColGn && middleColBu && (middleColPu || middleColYe);
			greyMechanic |= twoPurplesInMiddle && middleColGn;
			greyMechanic |= twoYellowsInMiddle && middleColGn;
			greyMechanic |= twoGreensInMiddle && (middleColYe || middleColPu);

			boolean yellowInMiddleBottom =
				data.get(3).asString().equals("YE") || data.get(4).asString().equals("YE") || data.get(5).asString().equals("YE") ||
				data.get(6).asString().equals("YE") || data.get(7).asString().equals("YE") || data.get(8).asString().equals("YE");
			boolean putpleInTopMiddle =
				data.get(0).asString().equals("PU") || data.get(1).asString().equals("PU") || data.get(2).asString().equals("PU") ||
				data.get(3).asString().equals("PU") || data.get(4).asString().equals("PU") || data.get(5).asString().equals("PU");
			

			boolean blackOrRedWhite =  colors.contains("BK") || (colors.contains("RD") && colors.contains("WH"));
			boolean buBkPuYeMechanic = colors.contains("BU") && blackOrRedWhite && yellowInMiddleBottom && putpleInTopMiddle;
			
			boolean result = true;

			if (result && colors.contains("debug")) {
				System.out.println("data: " + data);
				System.out.println("twoPurplesInMiddle: " + twoPurplesInMiddle);
				System.out.println("twoYellowsInMiddle: " + twoYellowsInMiddle);
				System.out.println("twoGreensInMiddle: " + twoGreensInMiddle);
				System.out.println("middleColGn: " + middleColGn);
				System.out.println("middleColYe: " + middleColYe);
				System.out.println("middleColBu: " + middleColBu);
				System.out.println("yellowInMiddleBottom: " + yellowInMiddleBottom);
				System.out.println("putpleInTopMiddle: " + putpleInTopMiddle);
				System.out.println("greyMechanic: " + greyMechanic);
				System.out.println("buPuYeMechanic: " + buBkPuYeMechanic);
			}

			return result;
			

			//return colors.contains("PI"); // 388_C_GY_C_BK_C_BU_C_BU - Experimental 1
			//return !colors.contains("BK") && !colors.contains("GN") && !colors.contains("RD");// - Experimental 4
			// return !colors.contains("BU"); - Experimental 3
			// 4471_C_WH_C_WH_C_YE_C_RD - Experimental 4
			//return (!colors.contains("PU") || !colors.contains("YE")) && (!colors.contains("GN"));
		};

		ArrayList<String> acceptedNames = new ArrayList<>();

		// Read into JsonObject
		JsonObject results = 
			JsonObject.readHjson(
				new BufferedReader(
					new FileReader(resultsFile.toFile()), 1024*1024*10)).asObject();

		for (String key : results.names()) {
			JsonObject result = results.get(key).asObject();

			String executor = result.getString("executor", "");
			if (executor.equals("CPU")) {
				numCPU++;
			} else if (executor.equals("GPU")) {
				numGPU++;
			}

			JsonObject resultMap = result.get("resultMap").asObject();
			int maxDepthValue = result.getInt("maxDepth", 0);
			boolean containsMaxResults = resultMap.get(""+maxDepthValue) != null;

			if (!containsMaxResults) {
				System.out.println("No results at max depth for " + result.getInt("idx", 0) + result.getString("name", ""));
				noResults++;
				continue;
			}

			boolean acceptedResult = false;
			int acceptedValue = 0;
			// resultMap.get(""+maxDepth) is a JsonArray of arrays

			ArrayList<String> localResults = new ArrayList<>();
			for (int depth = maxDepthValue; depth > 0; depth--) {

				if (!localResults.isEmpty()) break;

				JsonValue v = resultMap.get(""+depth);
				if (v == null || v.isNull()) break;
				JsonArray resultsAtMax = v.asArray();
				if (resultsAtMax == null || resultsAtMax.size() == 0) continue;

				for (int i = 0; i < resultsAtMax.size(); i++) {
					int resultAtMax = resultsAtMax.get(i).asInt();
					// Convert int to colors
					String stateJson = MJAnalysis.stateToJson(resultAtMax);

					JsonArray stateJsonArray = JsonValue.readHjson(stateJson).asArray();
					HashSet<String> colors = new HashSet<>();
					for (int j = 0; j < stateJsonArray.size(); j++) {
						String color = stateJsonArray.get(j).asString();
						colors.add(color);
					}

					//if (depth == 69) {
					//	colors.add("debug");
					//}
					if (filter.apply(stateJsonArray, colors)) {
						acceptedResult = true;
						if (depth >= acceptedValue - depthRange) {
							acceptedValue = Math.max(acceptedValue, depth);
							localResults.add(result.getInt("idx", 0) + result.getString("name", "") + " " + resultAtMax + " (" + depth + ")");
						}
						if (colors.contains("debug")) {
							System.out.println(resultAtMax + " " + depth + " " + stateJsonArray);
						}
					}
				}
			}

			if (!acceptedResult) continue;

			if (acceptedValue > maxDepth) {
				maxDepth = acceptedValue;
				maxDepthNames.clear();
				maxDepthNames.add(result.getInt("idx", 0) + result.getString("name", ""));
			} else if (acceptedValue == maxDepth) {
				maxDepthNames.add(result.getInt("idx", 0) + result.getString("name", ""));
			}
			acceptedNames.addAll(localResults);
		}

		System.out.println("Num CPU: " + numCPU);
		System.out.println("Num GPU: " + numGPU);
		System.out.println("No results: " + noResults);
		System.out.println("Max depth: " + maxDepth);
		System.out.println("Max depth names: " + maxDepthNames);

		Collections.sort(acceptedNames);


		for (String name : acceptedNames) {
			boolean print = false;
			int curDepth = maxDepth;
			for (int i = 0; i < depthRange; i++) {

				if (name.endsWith("(" + curDepth + ")")) print = true;
				curDepth--;
			}

			if (print) System.out.println(name);

		}
		
	}
}
