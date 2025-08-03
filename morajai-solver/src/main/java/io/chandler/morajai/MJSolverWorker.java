package io.chandler.morajai;

import io.chandler.morajai.tools.JSSet;

import java.util.List;

import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;

/**
 * TeaVM worker entry point
 */
public class MJSolverWorker {
	public static void main(String[] x) {
		Window.worker().onMessage((ev) -> {
			Window.worker().postMessage(JSString.valueOf("Processing..."));
			try {
				String[] args = ev.getDataAsString().trim().split(" ");
				if (args.length < 2) {
					throw new RuntimeException("Expected 2 arguments");
				}

				String[] puzzleState = args[0].split(",");
				String[] targetColor = args[1].split(",");

				List<Integer> solution = MJSolver.solve(puzzleState, targetColor, 85, new JSSet(), (curDepth) -> {
					Window.worker().postMessage(JSString.valueOf("Processing... (" + curDepth + ")"));
				}); 
				
				if (solution == null) {
					Window.worker().postMessage(JSString.valueOf("No solution found~"));
				} else {
					int len = solution.size();
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < len; i++) {
						sb.append(solution.get(i));
					}
					Window.worker().postMessage(JSString.valueOf("Optimal moves: " + len + "~" + sb.toString()));
				}
			} catch (Exception e) {
				Window.worker().postMessage(JSString.valueOf("Error: " + e.getMessage()));
			} finally {
				Window.worker().close();
			}
		});
	}
}
