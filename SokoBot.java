package solver;

import java.util.*;

public class SokoBot {
    private final int[] DIR_X = {-1, 1, 0, 0};
    private final int[] DIR_Y = {0, 0, -1, 1};
    private final char[] DIR_C = {'u', 'd', 'l', 'r'};

    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
        final int ROW = height;
        final int COL = width;
        final int CELLS = ROW * COL;
        long startTime = System.currentTimeMillis();
        final long TIME_LIMIT_MS = 14500;

        // primitive masks and lists
        boolean[] mapWall = new boolean[CELLS];
        int[] initialBoxesArrTmp = new int[CELLS]; int biCount = 0;
        int[] goalArrTmp = new int[CELLS]; int giCount = 0;
        int playerIdx = -1;

        for (int r = 0; r < ROW; r++) {
            for (int c = 0; c < COL; c++) {
                int idx = r * COL + c;
                mapWall[idx] = (mapData[r][c] == '#');
                char it = itemsData[r][c];
                if (it == '@') playerIdx = idx;
                if (it == '$') initialBoxesArrTmp[biCount++] = idx;
                if (mapData[r][c] == '.') goalArrTmp[giCount++] = idx;
            }
        }
        int[] initialBoxes = Arrays.copyOf(initialBoxesArrTmp, biCount);
        int[] goalIndices = Arrays.copyOf(goalArrTmp, giCount);
        boolean[] goalMask = new boolean[CELLS];
        for (int g : goalIndices) goalMask[g] = true;

        Heuristic heuristic = new Heuristic();
        int initialH = heuristic.hungarianAssignmentSum(initialBoxes, goalIndices, COL);

        Node root = new Node(playerIdx, initialBoxes, initialH, 0, null, "");

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.getG() + n.getHeuristic()));
        open.add(root);
        HashSet<Node> closed = new HashSet<>();

        // precompute reachability-to-goals ignoring boxes for goal-alignment pruning
        boolean[] reachToAnyGoal = computeReachToGoals(mapWall, goalMask, ROW, COL);

        // Temporary reusable arrays to reduce allocations (per iteration we copy as needed)
        boolean[] boxOcc = new boolean[CELLS];
        boolean[] reachable = new boolean[CELLS];
        int[] parent = new int[CELLS];

        while (!open.isEmpty()) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) return "";
            Node cur = open.poll();
            if (closed.contains(cur)) continue;
            closed.add(cur);

            int[] boxes = cur.getBoxes();

            // goal check
            if (allBoxesOnGoals(boxes, goalMask)) {
                LinkedList<String> parts = new LinkedList<>();
                Node node = cur;
                while (node != null && node.getParent() != null) {
                    parts.addFirst(node.getMoveSeq());
                    node = node.getParent();
                }
                StringBuilder full = new StringBuilder();
                for (String p : parts) full.append(p);
                return full.toString();
            }

            // build box occupancy fast
            Arrays.fill(boxOcc, false);
            for (int b : boxes) boxOcc[b] = true;

            // compute player reachability BFS (reuse arrays)
            BFSResult bfs = bfsReachable(cur.getPlayerIndex(), ROW, COL, mapWall, boxOcc, reachable, parent);
            boolean[] reachableNow = bfs.reachable; // alias

            // enumerate pushes
            for (int bi = 0; bi < boxes.length; bi++) {
                int bIdx = boxes[bi];
                int bx = bIdx / COL, by = bIdx % COL;
                // optional micro-prune: if this box already on goal, still allow moves since moving off goals can be necessary
                for (int d = 0; d < 4; d++) {
                    int px = bx - DIR_X[d], py = by - DIR_Y[d];
                    int tx = bx + DIR_X[d], ty = by + DIR_Y[d];
                    if (px < 0 || px >= ROW || py < 0 || py >= COL) continue;
                    if (tx < 0 || tx >= ROW || ty < 0 || ty >= COL) continue;
                    int pIdx = px * COL + py;
                    int tIdx = tx * COL + ty;
                    if (mapWall[tIdx]) continue;
                    if (boxOcc[tIdx]) continue;
                    if (!reachableNow[pIdx]) continue;

                    // form newBoxes
                    int[] newBoxes = Arrays.copyOf(boxes, boxes.length);
                    newBoxes[bi] = tIdx;
                    Arrays.sort(newBoxes);

                    // cheap deadlocks first
                    if (isCornerDeadlock(tIdx, mapWall, goalMask, ROW, COL)) continue;
                    if (is2x2Deadlock(newBoxes, goalMask, ROW, COL)) continue;
                    if (!reachToAnyGoal[tIdx]) continue; // cannot reach any goal at all

                    // frozen pair/tunnel detection
                    if (isFrozenPair(newBoxes, mapWall, goalMask, ROW, COL)) continue;
                    if (isTunnelDeadlock(newBoxes, mapWall, goalMask, ROW, COL)) continue;

                    // build path to push (parent array), then append push char
                    String pathToPush = buildPathFromParent(parent, cur.getPlayerIndex(), pIdx, COL);
                    if (pathToPush == null) continue;
                    String pushMove = String.valueOf(DIR_C[d]);
                    String moveSeq = pathToPush + pushMove;

                    int newPlayerIdx = bIdx;
                    int newG = cur.getG() + moveSeq.length();

                    // compute heuristic only after deadlocks pruned
                    int newH = heuristic.hungarianAssignmentSum(newBoxes, goalIndices, COL);

                    Node child = new Node(newPlayerIdx, newBoxes, newH, newG, cur, moveSeq);
                    if (closed.contains(child)) continue;
                    open.add(child);
                }
            }
        }
        return "";
    }

    // compute if each cell can reach any goal (ignoring boxes) - used for early pruning
    private boolean[] computeReachToGoals(boolean[] mapWall, boolean[] goalMask, int ROW, int COL) {
        int CELLS = ROW * COL;
        boolean[] vis = new boolean[CELLS];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < CELLS; i++) {
            if (goalMask[i]) {
                vis[i] = true;
                q.add(i);
            }
        }
        while (!q.isEmpty()) {
            int cur = q.removeFirst();
            int cx = cur / COL, cy = cur % COL;
            int nx, ny, nIdx;
            // neighbors
            nx = cx - 1; ny = cy; if (nx >= 0) { nIdx = nx * COL + ny; if (!mapWall[nIdx] && !vis[nIdx]) { vis[nIdx] = true; q.add(nIdx); } }
            nx = cx + 1; ny = cy; if (nx < ROW)  { nIdx = nx * COL + ny; if (!mapWall[nIdx] && !vis[nIdx]) { vis[nIdx] = true; q.add(nIdx); } }
            nx = cx; ny = cy - 1; if (ny >= 0) { nIdx = nx * COL + ny; if (!mapWall[nIdx] && !vis[nIdx]) { vis[nIdx] = true; q.add(nIdx); } }
            nx = cx; ny = cy + 1; if (ny < COL)  { nIdx = nx * COL + ny; if (!mapWall[nIdx] && !vis[nIdx]) { vis[nIdx] = true; q.add(nIdx); } }
        }
        return vis;
    }

    private static boolean allBoxesOnGoals(int[] boxes, boolean[] goalMask) {
        for (int b : boxes) if (!goalMask[b]) return false;
        return true;
    }

    private static class BFSResult {
        final boolean[] reachable;
        final int[] parent;
        BFSResult(boolean[] reachable, int[] parent) { this.reachable = reachable; this.parent = parent; }
    }

    // bfsReachable reuses arrays provided to minimize allocation
    private BFSResult bfsReachable(int startIdx, int ROW, int COL, boolean[] mapWall, boolean[] boxOcc, boolean[] reachableReuse, int[] parentReuse) {
        int CELLS = ROW * COL;
        Arrays.fill(reachableReuse, false);
        Arrays.fill(parentReuse, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        reachableReuse[startIdx] = true;
        q.add(startIdx);
        while (!q.isEmpty()) {
            int cur = q.removeFirst();
            int cx = cur / COL, cy = cur % COL;
            // four neighbors inline
            int nx = cx - 1, ny = cy; if (nx >= 0) { int ni = nx * COL + ny; if (!reachableReuse[ni] && !mapWall[ni] && !boxOcc[ni]) { reachableReuse[ni] = true; parentReuse[ni] = cur; q.add(ni);} }
            nx = cx + 1; ny = cy; if (nx < ROW)  { int ni = nx * COL + ny; if (!reachableReuse[ni] && !mapWall[ni] && !boxOcc[ni]) { reachableReuse[ni] = true; parentReuse[ni] = cur; q.add(ni);} }
            nx = cx; ny = cy - 1; if (ny >= 0) { int ni = nx * COL + ny; if (!reachableReuse[ni] && !mapWall[ni] && !boxOcc[ni]) { reachableReuse[ni] = true; parentReuse[ni] = cur; q.add(ni);} }
            nx = cx; ny = cy + 1; if (ny < COL)  { int ni = nx * COL + ny; if (!reachableReuse[ni] && !mapWall[ni] && !boxOcc[ni]) { reachableReuse[ni] = true; parentReuse[ni] = cur; q.add(ni);} }
        }
        return new BFSResult(reachableReuse, parentReuse);
    }

    // Reconstruct path of moves from 'from' to 'to' using parent map of indices.
    private String buildPathFromParent(int[] parent, int fromIdx, int toIdx, int COL) {
        if (fromIdx == toIdx) return "";
        if (parent[toIdx] == -1 && fromIdx != toIdx) {
            // if toIdx is unreachable, parent[toIdx] stays -1
            // attempt to walk backward: if toIdx == fromIdx then fine else unreachable
            return null;
        }
        // gather reversed moves
        StringBuilder rev = new StringBuilder();
        int cur = toIdx;
        while (cur != fromIdx) {
            int p = parent[cur];
            if (p == -1) return null;
            int px = p / COL, py = p % COL;
            int cx = cur / COL, cy = cur % COL;
            if (px == cx - 1 && py == cy) rev.append('d');
            else if (px == cx + 1 && py == cy) rev.append('u');
            else if (px == cx && py == cy - 1) rev.append('r');
            else if (px == cx && py == cy + 1) rev.append('l');
            else return null;
            cur = p;
        }
        // reverse
        rev.reverse();
        return rev.toString();
    }

    // Corner deadlock: tile blocked by two orthogonal walls and not a goal
    private boolean isCornerDeadlock(int idx, boolean[] mapWall, boolean[] goalMask, int ROW, int COL) {
        if (goalMask[idx]) return false;
        int x = idx / COL, y = idx % COL;
        boolean up = x - 1 < 0 || mapWall[(x - 1) * COL + y];
        boolean down = x + 1 >= ROW || mapWall[(x + 1) * COL + y];
        boolean left = y - 1 < 0 || mapWall[x * COL + (y - 1)];
        boolean right = y + 1 >= COL || mapWall[x * COL + (y + 1)];
        return (up && left) || (up && right) || (down && left) || (down && right);
    }

    private boolean is2x2Deadlock(int[] boxes, boolean[] goalMask, int ROW, int COL) {
        boolean[] occ = new boolean[ROW * COL];
        for (int b : boxes) occ[b] = true;
        for (int r = 0; r < ROW - 1; r++) for (int c = 0; c < COL - 1; c++) {
            int a = r * COL + c, b = r * COL + (c + 1), d = (r + 1) * COL + c, e = (r + 1) * COL + (c + 1);
            if (occ[a] && occ[b] && occ[d] && occ[e]) {
                if (!goalMask[a] && !goalMask[b] && !goalMask[d] && !goalMask[e]) return true;
            }
        }
        return false;
    }

    // frozen pair detection: detect two boxes adjacent that are both stuck against walls or each other with no goal reachable
    private boolean isFrozenPair(int[] boxes, boolean[] mapWall, boolean[] goalMask, int ROW, int COL) {
        boolean[] occ = new boolean[ROW * COL];
        for (int b : boxes) occ[b] = true;
        for (int i = 0; i < boxes.length; i++) {
            int a = boxes[i];
            int ax = a / COL, ay = a % COL;
            // check neighbors
            int[] nxs = {ax - 1, ax + 1, ax, ax};
            int[] nys = {ay, ay, ay - 1, ay + 1};
            for (int k = 0; k < 4; k++) {
                int nx = nxs[k], ny = nys[k];
                if (nx < 0 || nx >= ROW || ny < 0 || ny >= COL) continue;
                int bidx = nx * COL + ny;
                if (!occ[bidx]) continue;
                // now a and bidx are adjacent boxes; check if they form a frozen pair against walls
                // if both boxes are not on goals, and for both boxes two orthogonal sides are walls/boxes (not allowing movement), mark frozen
                if (!goalMask[a] && !goalMask[bidx]) {
                    if (isBoxImmovable(a, occ, mapWall, ROW, COL) && isBoxImmovable(bidx, occ, mapWall, ROW, COL)) return true;
                }
            }
        }
        return false;
    }

    private boolean isBoxImmovable(int idx, boolean[] occ, boolean[] mapWall, int ROW, int COL) {
        int x = idx / COL, y = idx % COL;
        // if both up/down are blocked and both left/right are blocked, it's immovable
        boolean upBlock = (x - 1 < 0) || mapWall[(x - 1) * COL + y] || occ[(x - 1) * COL + y];
        boolean downBlock = (x + 1 >= ROW) || mapWall[(x + 1) * COL + y] || occ[(x + 1) * COL + y];
        boolean leftBlock = (y - 1 < 0) || mapWall[x * COL + (y - 1)] || occ[x * COL + (y - 1)];
        boolean rightBlock = (y + 1 >= COL) || mapWall[x * COL + (y + 1)] || occ[x * COL + (y + 1)];
        // immovable if can't move vertically and can't move horizontally
        return (upBlock && downBlock) || (leftBlock && rightBlock);
    }

    // tunnel deadlock: detect single-width corridor endings where box pushed away cannot be pulled back, or path to a goal is blocked
    private boolean isTunnelDeadlock(int[] boxes, boolean[] mapWall, boolean[] goalMask, int ROW, int COL) {
        boolean[] occ = new boolean[ROW * COL];
        for (int b : boxes) occ[b] = true;
        for (int b : boxes) {
            if (goalMask[b]) continue;
            int x = b / COL, y = b % COL;
            // check horizontal tunnel single width
            boolean leftWall = (y - 1 < 0) || mapWall[x * COL + (y - 1)];
            boolean rightWall = (y + 1 >= COL) || mapWall[x * COL + (y + 1)];
            if (leftWall && rightWall) {
                // vertical corridor: if above or below is dead-end without a goal reachable, and box not on goal, flag
                boolean upWall = (x - 1 < 0) || mapWall[(x - 1) * COL + y];
                boolean downWall = (x + 1 >= ROW) || mapWall[(x + 1) * COL + y];
                // if one side is wall and the corridor is long with no goal in same column, may be dead
                if ((upWall && !downWall) || (downWall && !upWall)) {
                    // check whether there exists any goal in same corridor column reachable (quick scan)
                    boolean hasGoalInCol = false;
                    for (int rx = 0; rx < ROW; rx++) {
                        int idx = rx * COL + y;
                        if (!mapWall[idx] && goalMask[idx]) { hasGoalInCol = true; break; }
                    }
                    if (!hasGoalInCol) return true;
                }
            }
            // vertical tunnel single width
            boolean upWall = (x - 1 < 0) || mapWall[(x - 1) * COL + y];
            boolean downWall = (x + 1 >= ROW) || mapWall[(x + 1) * COL + y];
            if (upWall && downWall) {
                boolean leftW = (y - 1 < 0) || mapWall[x * COL + (y - 1)];
                boolean rightW = (y + 1 >= COL) || mapWall[x * COL + (y + 1)];
                if ((leftW && !rightW) || (rightW && !leftW)) {
                    boolean hasGoalInRow = false;
                    for (int cy = 0; cy < COL; cy++) {
                        int idx = x * COL + cy;
                        if (!mapWall[idx] && goalMask[idx]) { hasGoalInRow = true; break; }
                    }
                    if (!hasGoalInRow) return true;
                }
            }
        }
        return false;
    }
}