public class Position{
    private int x, y;

    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    @Override
    public boolean equals(Object pos){
        Position newPos = (Position) pos;

        if (this.x == newPos.x && this.y == newPos.y){
            return true;
        }else{
            return false;
        }
    }

    // adds hash code to x and y
    @Override
    public int hashCode(){
        int hash;

        // adds hash on x and y
        hash = x * 10000 + y;

        return hash;
    }
}
