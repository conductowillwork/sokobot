package solver;

public class Heuristic {
    // Manhattan distance between two indices in row-major with width
    public int manhattan(int aIndex, int bIndex, int width) {
        int ax = aIndex / width, ay = aIndex % width;
        int bx = bIndex / width, by = bIndex % width;
        return Math.abs(ax - bx) + Math.abs(ay - by);
    }

    // Hungarian algorithm for minimal assignment cost (boxes -> goals).
    // Assumes goals.length >= boxes.length; if more goals than boxes, we match boxes to subset of goals.
    public int hungarianAssignmentSum(int[] boxes, int[] goals, int width) {
        int n = boxes.length;
        if (n == 0) return 0;
        int m = goals.length;
        // build cost matrix of size m x m where we embed boxes into top-left and pad with zeros
        // but simplest: create n x m costs, then run Hungarian by treating it as rectangular (we adapt)
        int[][] cost = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                cost[i][j] = manhattan(boxes[i], goals[j], width);
            }
        }
        return hungarianRectangular(cost, n, m);
    }

    // Hungarian for rectangular matrix n x m (n <= m): returns minimal sum of selecting n distinct columns for each row
    private int hungarianRectangular(int[][] cost, int n, int m) {
        // Transform to square by padding columns (m >= n usually). We'll run classic O(k^3) with k = max(n,m).
        int k = Math.max(n, m);
        int[][] a = new int[k][k];
        for (int i = 0; i < k; i++) for (int j = 0; j < k; j++) a[i][j] = (i < n && j < m) ? cost[i][j] : 0;

        // Arrays indexed from 1..k in classical implementation
        int[] u = new int[k + 1];
        int[] v = new int[k + 1];
        int[] p = new int[k + 1];
        int[] way = new int[k + 1];

        for (int i = 1; i <= k; ++i) {
            p[0] = i;
            int j0 = 0;
            int[] minv = new int[k + 1];
            boolean[] used = new boolean[k + 1];
            for (int j = 0; j <= k; ++j) minv[j] = Integer.MAX_VALUE;
            do {
                used[j0] = true;
                int i0 = p[j0], delta = Integer.MAX_VALUE, j1 = 0;
                for (int j = 1; j <= k; ++j) {
                    if (used[j]) continue;
                    int cur = a[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0; }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j; }
                }
                for (int j = 0; j <= k; ++j) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta; }
                    else minv[j] -= delta;
                }
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] match = new int[k + 1];
        for (int j = 1; j <= k; ++j) match[p[j]] = j;
        // compute result for original n rows matched to columns (only consider j <= m)
        int result = 0;
        for (int i = 1; i <= n; ++i) {
            int j = match[i];
            if (j >= 1 && j <= m) result += a[i - 1][j - 1];
            else result += 0;
        }
        return result;
    }
}