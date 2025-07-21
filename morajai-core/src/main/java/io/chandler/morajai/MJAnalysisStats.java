package io.chandler.morajai;

public class MJAnalysisStats {

	final String filename;
	final int idx;
	boolean begun = false;
	int initalPruned = 0;
	boolean pruning = false;
	boolean backtracking = false;

	int unreached = 0;
	int depth = 0;
	int dead = 0;
	int statesAtDepth = 0;

	boolean complete = false;

	public MJAnalysisStats(int idx, String filename) {
		this.filename = filename;
		this.idx = idx;
	}
}