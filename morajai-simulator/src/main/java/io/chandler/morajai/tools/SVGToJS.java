package io.chandler.morajai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class SVGToJS {
	public static void main(String[] args) throws IOException {
		File file = new File(args[0]);
		String varName = args[1];

		if (!file.exists()) {
			System.err.println("File does not exist: " + file.getAbsolutePath());
			System.exit(1);
		}

		File outputFile = new File(args[2]);
		if (outputFile.exists()) {
			Files.delete(outputFile.toPath());
		}

		// Base64 compress and output to JS
		String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
		Files.write(outputFile.toPath(), ("const " + varName + " = '" + base64 + "';\n\nexport default " + varName + ";").getBytes());
	}
}
