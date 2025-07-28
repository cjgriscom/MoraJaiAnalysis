package io.chandler.morajai;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

import io.chandler.morajai.MJAnalysis.DepthTracker;

public class MJDepthsBacktracker {

	public static int THRESHOLD = 2000;

	private final MoraJaiBox.Color[] targetColors;
	{
		targetColors = new MoraJaiBox.Color[4];
		for (int i = 0; i < 4; i++) {
			targetColors[i] = MoraJaiBox.Color.C_GY;
		}
	}

	private final DepthTracker depths;
	private final long[] eliminated = new long[1000000000/64];
	private int maxDepth = 0;

	public MJDepthsBacktracker(DepthTracker depths) {
		this.depths = depths;
	}

	public void backtrack() {
		MoraJaiBox box = new MoraJaiBox();
		for (int i = 0; i < 1000000000; i++) {
			int state = i;
			int depth = depths.getDepth(state);
			maxDepth = Math.max(maxDepth, depth);
			if (depth > 0) {
				box.initFromState(targetColors, state);
				for (int j = 0; j < 9; j++) {
					box.reset();
					box.pressTile(j);
					int newState = box.getState();
					if (newState != state) {
						int newDepth = depths.getDepth(newState);
						if (newDepth == depth-1) {
							set(eliminated, newState);
						}
					}
				}
			} else {
				set(eliminated, i);
			}
		}
	}

	public void reportResults(PrintStream out) {
		int[] results = new int[250];
		TreeMap<Integer, ArrayList<Integer>> resultsMap = new TreeMap<>();
		for (int i = 0; i < eliminated.length; i++) {
			long e = eliminated[i];
			if (e != -1L) {
				for (int j = 0; j < 64; j++) {
					if (!isSet(eliminated, i * 64 + j)) {
						int depth = depths.getDepth(i * 64 + j);
						results[depth]++;
						if (results[depth] <= THRESHOLD + 1) {
							ArrayList<Integer> list = resultsMap.get(depth);
							if (list == null) {
								list = new ArrayList<>();
								resultsMap.put(depth, list);
							}
							list.add(i * 64 + j);
						}
					}
				}
			}
		}
		for (int i = 0; i <= maxDepth; i++) {
			out.println("Backedtracked depth " + i + " has " + results[i] + " states");
			ArrayList<Integer> list = resultsMap.get(i);
			if (list != null && list.size() <= THRESHOLD ) {
				String delim = "";
				for (int j = 0; j < list.size(); j++) {
					out.print(delim);
					out.print(list.get(j));
					delim = ",";
				}
				out.println();
			}
		}
	}

	private void set(long[] depths, int state) {
		int idx = state >> 6;             // state / 64
		depths[idx] |= 1L << (state & 63);
	}

	private boolean isSet(long[] depths, int state) {
		int idx = state >> 6;
		return (depths[idx] & (1L << (state & 63))) != 0;
	}
}
