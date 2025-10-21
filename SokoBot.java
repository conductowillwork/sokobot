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
        final long TIME_LIMIT_MS = 14500; // safe margin under 15s

        // collect initial player, boxes, goals
        int playerIndex = -1;
        ArrayList<Integer> boxesList = new ArrayList<>();
        ArrayList<Integer> goalsList = new ArrayList<>();
        for (int r = 0; r < ROW; r++) {
            for (int c = 0; c < COL; c++) {
                char it = itemsData[r][c];
                char mt = mapData[r][c];
                if (it == '@') playerIndex = r * COL + c;
                if (it == '$') boxesList.add(r * COL + c);
                if (mt == '.') goalsList.add(r * COL + c);
            }
        }
        int[] initialBoxes = listToArray(boxesList);
        int[] goalIndices = listToArray(goalsList);
        boolean[] goalMask = new boolean[CELLS];
        for (int g : goalIndices) goalMask[g] = true;

        Heuristic heuristic = new Heuristic();
        int initialH = heuristic.greedyAssignmentSum(initialBoxes, goalIndices, COL);

        Node root = new Node(playerIndex, initialBoxes, initialH, 0, null, "");

        // A* priority queue over f = g + h
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.getG() + n.getHeuristic()));
        open.add(root);
        HashSet<Node> closed = new HashSet<>();

        // helper arrays
        boolean[] mapWall = new boolean[CELLS];
        for (int i = 0; i < ROW; i++) for (int j = 0; j < COL; j++) mapWall[i * COL + j] = mapData[i][j] == '#';

        while (!open.isEmpty()) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) return "";
            Node cur = open.poll();
            if (closed.contains(cur)) continue;
            closed.add(cur);

            // check goal: all boxes on goals
            if (allBoxesOnGoals(cur.getBoxes(), goalMask)) {
                // reconstruct full move string by chaining moveSeq from root->...->cur
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

            // compute player reachability given boxes as obstacles
            boolean[] boxOcc = new boolean[CELLS];
            for (int b : cur.getBoxes()) boxOcc[b] = true;
            BFSResult bfs = bfsReachable(cur.getPlayerIndex(), ROW, COL, mapWall, boxOcc);
            boolean[] reachable = bfs.reachable;
            int[] parent = bfs.parent;

            // enumerate pushes: for each box, for each direction, check if player can reach push cell and target free
            int[] boxes = cur.getBoxes();
            for (int bi = 0; bi < boxes.length; bi++) {
                int bIdx = boxes[bi];
                int bx = bIdx / COL, by = bIdx % COL;
                for (int d = 0; d < 4; d++) {
                    int px = bx - DIR_X[d], py = by - DIR_Y[d]; // player must stand here to push
                    int tx = bx + DIR_X[d], ty = by + DIR_Y[d]; // box target

                    if (px < 0 || px >= ROW || py < 0 || py >= COL) continue;
                    if (tx < 0 || tx >= ROW || ty < 0 || ty >= COL) continue;
                    int pIdx = px * COL + py;
                    int tIdx = tx * COL + ty;

                    if (mapWall[tIdx]) continue; // can't push into wall
                    if (boxOcc[tIdx]) continue;  // can't push into another box
                    if (!reachable[pIdx]) continue; // player cannot get to pushing position

                    // produce new boxes array
                    int[] newBoxes = Arrays.copyOf(boxes, boxes.length);
                    newBoxes[bi] = tIdx;
                    Arrays.sort(newBoxes);

                    // quick deadlock checks: corner and 2x2 block
                    if (isCornerDeadlock(tIdx, mapWall, goalMask, ROW, COL)) continue;
                    if (is2x2Deadlock(newBoxes, goalMask, ROW, COL)) continue;

                    // compute path from player to pIdx (sequence of moves), then append push move
                    String pathToPush = buildPathFromParent(parent, cur.getPlayerIndex(), pIdx, COL);
                    if (pathToPush == null) continue; // unexpected
                    String pushMove = String.valueOf(DIR_C[d]);
                    String moveSeq = pathToPush + pushMove;

                    int newPlayerIdx = bIdx; // after push, player stands at box's old cell
                    int newG = cur.getG() + moveSeq.length();
                    int newH = heuristic.greedyAssignmentSum(newBoxes, goalIndices, COL);

                    Node child = new Node(newPlayerIdx, newBoxes, newH, newG, cur, moveSeq);
                    if (closed.contains(child)) continue;
                    open.add(child);
                }
            }
        }
        return "";
    }

    private static int[] listToArray(ArrayList<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }

    private static boolean allBoxesOnGoals(int[] boxes, boolean[] goalMask) {
        for (int b : boxes) if (!goalMask[b]) return false;
        return true;
    }

    // BFSResult holds reachable[] and parent[] to reconstruct paths
    private static class BFSResult {
        final boolean[] reachable;
        final int[] parent;
        BFSResult(boolean[] reachable, int[] parent) { this.reachable = reachable; this.parent = parent; }
    }

    // BFS on grid treating walls and boxes as blocked; returns parent tree for path reconstruction
    private BFSResult bfsReachable(int startIdx, int ROW, int COL, boolean[] mapWall, boolean[] boxOcc) {
        int CELLS = ROW * COL;
        boolean[] vis = new boolean[CELLS];
        int[] parent = new int[CELLS];
        Arrays.fill(parent, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        vis[startIdx] = true;
        q.add(startIdx);
        while (!q.isEmpty()) {
            int cur = q.removeFirst();
            int cx = cur / COL, cy = cur % COL;
            for (int k = 0; k < 4; k++) {
                int nx = cx + DIR_X[k], ny = cy + DIR_Y[k];
                if (nx < 0 || nx >= ROW || ny < 0 || ny >= COL) continue;
                int nIdx = nx * COL + ny;
                if (vis[nIdx]) continue;
                if (mapWall[nIdx]) continue;
                if (boxOcc[nIdx]) continue;
                vis[nIdx] = true;
                parent[nIdx] = cur;
                q.addLast(nIdx);
            }
        }
        return new BFSResult(vis, parent);
    }

    // Reconstruct path of moves from 'from' to 'to' using parent map of indices. Returns move string.
    private String buildPathFromParent(int[] parent, int fromIdx, int toIdx, int COL) {
        if (fromIdx == toIdx) return "";
        if (parent[toIdx] == -1) {
            // toIdx might be start (fromIdx) or unreachable
            // attempt to walk backward from toIdx to fromIdx to verify connectedness
            return null;
        }
        LinkedList<Character> rev = new LinkedList<>();
        int cur = toIdx;
        while (cur != fromIdx) {
            int p = parent[cur];
            if (p == -1) return null;
            int px = p / COL, py = p % COL;
            int cx = cur / COL, cy = cur % COL;
            if (px == cx - 1 && py == cy) rev.addLast('d');
            else if (px == cx + 1 && py == cy) rev.addLast('u');
            else if (px == cx && py == cy - 1) rev.addLast('r');
            else if (px == cx && py == cy + 1) rev.addLast('l');
            else return null;
            cur = p;
        }
        StringBuilder sb = new StringBuilder(rev.size());
        for (char ch : rev) sb.append(ch);
        return sb.toString();
    }

    // Corner deadlock: box on non-goal and in corner of walls (two orthogonal walls)
    private boolean isCornerDeadlock(int idx, boolean[] mapWall, boolean[] goalMask, int ROW, int COL) {
        if (goalMask[idx]) return false;
        int x = idx / COL, y = idx % COL;
        boolean up = x - 1 < 0 || mapWall[(x - 1) * COL + y];
        boolean down = x + 1 >= ROW || mapWall[(x + 1) * COL + y];
        boolean left = y - 1 < 0 || mapWall[x * COL + (y - 1)];
        boolean right = y + 1 >= COL || mapWall[x * COL + (y + 1)];
        return (up && left) || (up && right) || (down && left) || (down && right);
    }

    // 2x2 deadlock: any 2x2 block fully occupied by boxes and no goal inside
    private boolean is2x2Deadlock(int[] boxes, boolean[] goalMask, int ROW, int COL) {
        boolean[] boxOcc = new boolean[ROW * COL];
        for (int b : boxes) boxOcc[b] = true;
        for (int r = 0; r < ROW - 1; r++) {
            for (int c = 0; c < COL - 1; c++) {
                int a = r * COL + c, b = r * COL + (c + 1), d = (r + 1) * COL + c, e = (r + 1) * COL + (c + 1);
                if (boxOcc[a] && boxOcc[b] && boxOcc[d] && boxOcc[e]) {
                    if (!goalMask[a] && !goalMask[b] && !goalMask[d] && !goalMask[e]) return true;
                }
            }
        }
        return false;
    }
}