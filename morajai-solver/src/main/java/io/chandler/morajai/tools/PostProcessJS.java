package io.chandler.morajai.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PostProcessJS {
	public static void main(String[] args) throws IOException {
		boolean minify = args[0].equals("true");

		String file = args[1];
		String baseName = args[2];

		String content = new String(Files.readAllBytes(Paths.get(file))) + "\nmain();";

		String outName = baseName + (minify ? ".min.js" : ".js");
		Files.write(Paths.get(outName), content.getBytes());

		// Delete the original file
		Files.delete(Paths.get(file));
	}
}
