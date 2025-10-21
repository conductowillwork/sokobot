package solver;

public class Position {
    private final int x, y;
    public Position(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    public int toIndex(int width) {
        return x * width + y;
    }

    public static Position fromIndex(int index, int width) {
        int rx = index / width;
        int ry = index % width;
        return new Position(rx, ry);
    }
}