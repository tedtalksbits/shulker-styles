package dev.whizzle.shulker.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Mistakes that compile fine and only show up in the game. */
class SourceRulesTest {
	static List<Path> sources() throws IOException {
		Path base = Path.of(System.getProperty("shulker.client.dir", ""), "src/main/java");
		try (Stream<Path> files = Files.walk(base)) { return files.filter(f -> f.toString().endsWith(".java")).toList(); }
	}

	/**
	 * 26.x numbers mouse buttons like SDL (left 1, middle 2, right 3), not 0/1/2 as before; comparing with a literal
	 * made the icon slot treat a left click as a right click (client 0.3.1). Use InputConstants.MOUSE_BUTTON_*.
	 */
	@Test void mouseButtonsByName() throws IOException {
		Pattern literal = Pattern.compile("button\\(\\)\\s*[=!]=\\s*\\d");
		for (Path f : sources())
			for (String line : Files.readAllLines(f))
				assertFalse(literal.matcher(line).find(), f.getFileName() + ": compare mouse buttons with InputConstants.MOUSE_BUTTON_*: " + line.trim());
	}
}
