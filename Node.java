package solver;

import java.util.Arrays;

public class Node {
    private final int playerIndex;
    private final int[] boxes;         // canonical sorted box indices
    private final int heuristicCost;
    private final int gCost;
    private final Node parent;
    private final String moveSeq;      // moves (u/d/l/r) taken from parent to reach this node

    public Node(int playerIndex, int[] boxes, int heuristicCost, int gCost, Node parent, String moveSeq) {
        this.playerIndex = playerIndex;
        this.boxes = Arrays.copyOf(boxes, boxes.length);
        Arrays.sort(this.boxes);
        this.heuristicCost = heuristicCost;
        this.gCost = gCost;
        this.parent = parent;
        this.moveSeq = moveSeq;
    }

    public int getPlayerIndex() { return playerIndex; }
    public int[] getBoxes() { return boxes; }
    public int getHeuristic() { return heuristicCost; }
    public int getG() { return gCost; }
    public Node getParent() { return parent; }
    public String getMoveSeq() { return moveSeq; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node other = (Node) o;
        return Arrays.equals(this.boxes, other.boxes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(boxes);
    }
}