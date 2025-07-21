package io.chandler.morajai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiFunction;

import org.json.JSONArray;
import org.json.JSONObject;

public class ResultStats {
	public static void main(String[] args) throws Exception {
		Path resultsFile = Paths.get("results", "results.json");

		int numCPU = 0;
		int numGPU = 0;
		int noResults = 0;
		int maxDepth = 0;
		ArrayList<String> maxDepthNames = new ArrayList<>();

		BiFunction<JSONArray, HashSet<String>, Boolean> filter = (data, colors) -> {
			// return colors.contains("PI"); // 388_C_GY_C_BK_C_BU_C_BU - Experimental 1
			// return !colors.contains("BK") && !colors.contains("GN") && !colors.contains("RD"); - Experimental 4
			// return !colors.contains("BU"); - Experimental 3
			// 4471_C_WH_C_WH_C_YE_C_RD - Experimental 4
			return (!colors.contains("PU") || !colors.contains("YE")) && (!colors.contains("GN"));
			
		};

		// Read into JSONObject
		JSONObject results = new JSONObject(Files.readString(resultsFile));

		Iterator<String> keys = results.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject result = results.getJSONObject(key);
			//System.out.println(key + ": " + result.toString());

			if (result.getString("executor").equals("CPU")) {
				numCPU++;
			} else if (result.getString("executor").equals("GPU")) {
				numGPU++;
			}

			boolean containsMaxResults = result.getJSONObject("resultMap").has(""+result.getInt("maxDepth"));
			
			if (!containsMaxResults) {
				System.out.println("No results at max depth for " + result.getInt("idx") + result.getString("name"));
				noResults++;
				continue;
			}

			boolean acceptedResult = false;

			JSONArray resultsAtMax = 
				result.getJSONObject("resultMap").getJSONArray(""+result.getInt("maxDepth"));
			for (int i = 0; i < resultsAtMax.length(); i++) {
				JSONArray resultAtMax = resultsAtMax.getJSONArray(i);
				HashSet<String> colors = new HashSet<>();
				for (int j = 0; j < resultAtMax.length(); j++) {
					String color = resultAtMax.getString(j);
					colors.add(color);
				}

				if (filter.apply(resultAtMax, colors)) {
					acceptedResult = true;
					break;
				}
			}

			if (!acceptedResult) continue;

			if (result.getInt("maxDepth") > maxDepth) {
				maxDepth = result.getInt("maxDepth");
				maxDepthNames.clear();
				maxDepthNames.add(result.getInt("idx") + result.getString("name"));
			} else if (result.getInt("maxDepth") == maxDepth) {
				maxDepthNames.add(result.getInt("idx") + result.getString("name"));
			}

			//break;
		}

		System.out.println("Num CPU: " + numCPU);
		System.out.println("Num GPU: " + numGPU);
		System.out.println("No results: " + noResults);
		System.out.println("Max depth: " + maxDepth);
		System.out.println("Max depth names: " + maxDepthNames);
	}
}
