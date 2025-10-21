package solver;

import java.util.Arrays;

public class Heuristic {
    // Manhattan distance between two indices in row-major with width
    public int manhattan(int aIndex, int bIndex, int width) {
        int ax = aIndex / width, ay = aIndex % width;
        int bx = bIndex / width, by = bIndex % width;
        return Math.abs(ax - bx) + Math.abs(ay - by);
    }

    // Greedy unique assignment sum: match each box to a distinct nearest goal
    public int greedyAssignmentSum(int[] boxIndices, int[] goalIndices, int width) {
        if (boxIndices.length == 0 || goalIndices.length == 0) return 0;
        boolean[] used = new boolean[goalIndices.length];
        int sum = 0;
        Integer[] order = new Integer[boxIndices.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (i, j) -> {
            int maxI = 0, maxJ = 0;
            for (int g : goalIndices) {
                maxI = Math.max(maxI, manhattan(boxIndices[i], g, width));
                maxJ = Math.max(maxJ, manhattan(boxIndices[j], g, width));
            }
            return Integer.compare(maxJ, maxI);
        });

        for (int idx : order) {
            int best = Integer.MAX_VALUE;
            int bestG = -1;
            for (int gi = 0; gi < goalIndices.length; gi++) {
                if (used[gi]) continue;
                int d = manhattan(boxIndices[idx], goalIndices[gi], width);
                if (d < best) { best = d; bestG = gi; }
            }
            if (bestG >= 0) {
                used[bestG] = true;
                sum += best;
            } else {
                sum += best;
            }
        }
        return sum;
    }
}