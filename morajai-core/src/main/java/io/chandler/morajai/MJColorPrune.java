package io.chandler.morajai;

import static io.chandler.morajai.MoraJaiBox.Color.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.chandler.morajai.MoraJaiBox.Color;

public class MJColorPrune {

	private static final int blueOrd = C_BU.ordinal();
	private static final int redOrd = C_RD.ordinal();
	private static final int greenOrd = C_GN.ordinal();
	private static final int yellowOrd = C_YE.ordinal();
	private static final int purpleOrd = C_PU.ordinal();
	private static final int orangeOrd = C_OR.ordinal();
	private static final int pinkOrd = C_PI.ordinal();
	private static final int blackOrd = C_BK.ordinal();
	private static final int whiteOrd = C_WH.ordinal();
	private static final int grayOrd = C_GY.ordinal();

	public static int prune(ExecutorService executor, boolean noBlue, Color[] targetColors, Consumer<List<Integer>> markDead) {

		int prunedDead = 0;
	
		// Process states in parallel chunks
		int numChunks = 1000;
		int chunkSize = 1000000000 / numChunks;

		AtomicInteger atomicProgressCount = new AtomicInteger(0);
		AtomicInteger atomicDeadStatesCount = new AtomicInteger(0);

		Object updateLock = new Object();

		for (int worker = 0; worker < numChunks; worker++) {
			int chunkIndex = worker;
			executor.submit(() -> {

				int startState = chunkIndex * chunkSize;
				int endState = Math.min(startState + chunkSize, 1000000000);

				BitSet states = new BitSet();

				int localProgressCount = 0;

				byte[] cnt = new byte[10];

				for (int state = startState; state < endState; state++) {
					fillCounts(state, cnt);
					localProgressCount++;
					int targetColorCount = 0;

					if (noBlue && cnt[blueOrd] > 0) {
						states.set(state-startState);
					}
					boolean containsWhite = false;
					if ((targetColorCount = countTarget(targetColors, C_WH)) != 0) {
						containsWhite = true;
						// Can't materialize white
						if (cnt[whiteOrd] == 0) states.set(state-startState);
					}
					if ((targetColorCount = countTarget(targetColors, C_GY)) != 0 || containsWhite) {
						int graysAndWhites = cnt[grayOrd] + cnt[whiteOrd];
						if (graysAndWhites == 0) {
							states.set(state-startState);
						} else if (graysAndWhites < targetColorCount) {
							// Orange and blue can both generate more white/gry
							if (	cnt[orangeOrd] +
										cnt[blueOrd] +
										graysAndWhites
										< targetColorCount) {
								states.set(state-startState);
							}
						}
					}
					if (basicColorPrune(targetColors, C_GN, greenOrd, cnt)) states.set(state-startState);
					if (basicColorPrune(targetColors, C_YE, yellowOrd, cnt)) states.set(state-startState);
					if (basicColorPrune(targetColors, C_PU, purpleOrd, cnt)) states.set(state-startState);
					if (basicColorPrune(targetColors, C_PI, pinkOrd, cnt)) states.set(state-startState);

					// Orange
					if ((targetColorCount = countTarget(targetColors, C_OR)) != 0) {
						int oranges = cnt[orangeOrd];
						if (oranges == 0) {
							states.set(state-startState);
						} else if (oranges == 1) {
							if (targetColorCount != 1) states.set(state-startState); 
						} else if (oranges < targetColorCount) {
							// Sum blue and gray and white
							int bgw = cnt[blueOrd] > 0 ? cnt[grayOrd] + cnt[blueOrd] + cnt[whiteOrd] : 0;
							int black = cnt[redOrd] > 0 && cnt[blueOrd] > 0 ? cnt[blackOrd] : 0;
							// Simplified but good enough
							if (oranges + bgw + black < targetColorCount) {
								states.set(state-startState);
							}
						}
					}

					int reds = cnt[redOrd];
					// Red is maybe the most complicated, keep it simple for now
					if ((targetColorCount = countTarget(targetColors, C_RD)) != 0) {
						// Ensure at least one red
						if (reds == 0) states.set(state-startState);
						else {
							// There's at least one red
							if (reds < targetColorCount) {
								// Not enough red, for simplicity just sum all morphing combos
								int orangeBluesBlack = cnt[orangeOrd] + cnt[blueOrd] + cnt[blackOrd];
								int whitesAndGrays = cnt[whiteOrd] + cnt[grayOrd];
								if (orangeBluesBlack + whitesAndGrays + reds < targetColorCount) {
									states.set(state-startState);
								}
							}
						}
					}

					// Black
					if ((targetColorCount = countTarget(targetColors, C_BK)) != 0) {
						if (reds == 0) {
							// No red so black is a basic color
							if (basicColorPrune(targetColors, C_BK, blackOrd, cnt)) {
								states.set(state-startState);
							}
						} else {
							// For simplicity just sum all morphing combos
							int orangeBluesBlack = cnt[orangeOrd] + cnt[blueOrd] + cnt[blackOrd];
							int whitesAndGrays = cnt[whiteOrd] + cnt[grayOrd];
							if (orangeBluesBlack + whitesAndGrays < targetColorCount) {
								states.set(state-startState);
							}
						}
					}

					// Blue is also complicated
					if ((targetColorCount = countTarget(targetColors, C_BU)) != 0) {
						// Sum blue, orange, white, gray
						int blue = cnt[blueOrd];
						int whiteGray = cnt[whiteOrd] + cnt[grayOrd];
						int orange = cnt[orangeOrd];

						if (reds == 0) {
							if (blue + whiteGray + orange < targetColorCount) {
								states.set(state-startState);
							}
						} else {
							int black = cnt[blackOrd];
							if (black + blue + whiteGray + orange < targetColorCount) {
								states.set(state-startState);
							}

						}
					}
				}



				int countSetBits = 0;
				List<Integer> deadStates = new ArrayList<>(1024);

				// Iterate over all set bits
				for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i+1)) {
					countSetBits++;
					deadStates.add(i + startState);
					if (deadStates.size() >= 1024) {
						synchronized (updateLock) {
							markDead.accept(deadStates);
						}
						deadStates.clear();
					}
				}

				synchronized (updateLock) {
					markDead.accept(deadStates);
					atomicDeadStatesCount.addAndGet(countSetBits);
					atomicProgressCount.addAndGet(localProgressCount);
				}
			});
		}

		while (true) {
			int completed = atomicProgressCount.get();
			if (completed == 1000000000) break;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		prunedDead = atomicDeadStatesCount.get();
		return prunedDead;
	
	}


	private static int countTarget(Color[] targetColors, Color color) {
		int count = 0;
		if (targetColors[0] == color) count++;
		if (targetColors[1] == color) count++;
		if (targetColors[2] == color) count++;
		if (targetColors[3] == color) count++;
		return count;
	}

	private static void fillCounts(int state, byte[] cnt) {
		Arrays.fill(cnt, (byte)0);
		for (int i = 0; i < 9; i++) {
			cnt[state % 10]++;
			state /= 10;
		}
	}

	// Prune colors that don't have any weird interactions
	private static boolean basicColorPrune(Color[] targetColors, Color targetColor, int colorOrd, byte[] cnt) {
		int targetColorCount = 0;
		if ((targetColorCount = countTarget(targetColors, targetColor)) != 0) {
			int basics = cnt[colorOrd];
			if (basics == 0) {
				return true;
			} else if (basics < targetColorCount) {
				// targetColorCount must be 2+
				int oranges = cnt[orangeOrd];
				int blues = cnt[blueOrd];
				int whitesAndGraysMinus2 = Math.max(0, cnt[whiteOrd] + cnt[grayOrd] - 2);
				if (basics < 2
						|| oranges == 0
						|| (oranges +
							blues + whitesAndGraysMinus2 +
							basics)
							< targetColorCount) {
					return true;
				}
			}
		}
		return false;
	}

}
