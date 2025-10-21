import java.lang.Math;

public class Heuristic{
    // goalRow = Goal row value
    // goalCol = Goal column value

    // row = node/box row value
    // col = node/box column value

    // manhattan distance formula
    public int manhattanDistance(int goalRow, int goalCol, int row, int col){
        int distance;

        distance = Math.abs(goalRow - row) + Math.abs(goalCol - col);

        return distance;
    }
}
