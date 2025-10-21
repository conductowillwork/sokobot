import java.util.ArrayList;

// dynamic
public class Node{
    private Position player;
    private ArrayList<Position> boxes;
    private int heuristicCost, weightCost;
    private Node parent;
    private char move;

    public Node(Position player, ArrayList<Position> boxes, int heuristicCost, int weightCost, Node parent, char move){ // NOTE: CONSTRUCTOR, stupid.
        this.player = player;
        this.boxes = copyBoxes(boxes);
        this.heuristicCost = heuristicCost;
        this.weightCost = weightCost;
        this.parent = parent;
        this.move = move;
    }

    public Position getPlayer(){
        return player;
    }

    public ArrayList<Position> getBoxes(){
        return boxes;
    }

    public int getHeuristic(){
        return heuristicCost;
    }

    public int getWeight(){
        return weightCost;
    }

    public Node getParent(){
        return parent;
    }

    public char getMove(){
        return move;
    }



    // checks if other node is similar to this node
    @Override
    public boolean equals(Object node){

        Node newNode = (Node) node;

        int ctr = 0;

        if (this.player.equals(newNode.player)){
            for (int i = 0; i < newNode.boxes.size(); i++){
                for (int j = 0; j < this.boxes.size();j++){
                    if (this.boxes.get(j).equals(newNode.boxes.get(i))){//So, this
                        ctr++;
                        break;
                    }
                }
            }

            if (ctr == newNode.boxes.size()){
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // adds hash code to both player and boxes (kind of like a serial to differentiate)
    @Override
    public int hashCode(){
        int hash;

        // adds hash on player
        hash = player.hashCode();

        // adds hash on EACH of the crates/boxes, not as a 1 whole set of boxes
        for (Position box: boxes){
            hash += box.hashCode();
        }

        return hash;
    }

    private ArrayList<Position> copyBoxes(ArrayList<Position> originalBoxes){
        ArrayList<Position> copiedBoxes = new ArrayList<>();
        for (Position box : originalBoxes){ //using a for each loop (for each Position object in the originalBoxes list, call it box and do something with it)
            copiedBoxes.add(new Position(box.getX(), box.getY())); //makecopies and add it to copiedBoxes ArrayList
        }
        return copiedBoxes;
    }
}
