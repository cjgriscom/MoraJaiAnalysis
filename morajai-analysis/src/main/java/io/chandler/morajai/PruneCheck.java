package io.chandler.morajai;

import java.io.File;
import java.io.IOException;

import org.hjson.JsonArray;
import org.hjson.JsonObject;

// Verify that pruned output is identical to non-pruned output
public class PruneCheck {
	public static void main(String[] args) throws IOException {
		// /home/cjgriscom/Programming/morajai/results/v3_noprune/results-no-result-map.json

		File prune = new File("/home/cjgriscom/Programming/morajai/results/v3/results-no-result-map.json");
		File noprune = new File("/home/cjgriscom/Programming/morajai/results/v3_noprune/results-no-result-map.json");

		// Load jsons
		JsonObject pruneJson = JsonObject.readHjson(new java.io.FileReader(prune)).asObject();
		JsonObject nopruneJson = JsonObject.readHjson(new java.io.FileReader(noprune)).asObject();

		// Compare
		for (String key : nopruneJson.names()) {
			JsonObject pruneEntry = pruneJson.get(key).asObject();
			JsonObject nopruneEntry = nopruneJson.get(key).asObject();
			JsonArray pruneSizeMap = pruneEntry.get("sizeMap").asArray();
			JsonArray nopruneSizeMap = nopruneEntry.get("sizeMap").asArray();
			boolean isSame = true;
			if (pruneSizeMap.size() != nopruneSizeMap.size()) {
				System.out.println(key + " " + pruneSizeMap.size() + " " + nopruneSizeMap.size());
				isSame = false;
			} else {
				for (int i = 0; i < pruneSizeMap.size(); i++) {
					if (pruneSizeMap.get(i).asInt() != nopruneSizeMap.get(i).asInt()) {
						System.out.println(key + " " + i + " " + pruneSizeMap.get(i).asInt() + " " + nopruneSizeMap.get(i).asInt() + " " + (pruneSizeMap.get(i).asInt() - nopruneSizeMap.get(i).asInt()));
						isSame = false;
					}
				}
			}
			if (isSame) {
				//System.out.println(key + " is the same");
			}
		}
	}
}
