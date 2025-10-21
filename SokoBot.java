import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.HashSet;

public class SokoBot {
    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
        
        // Final because map stays final for the entirety of the code...I think.
        final int ROW = height;
        final int COL = width;

        Position playerPos = null;
        ArrayList<Position> boxesPos = new ArrayList<>();
        ArrayList<Position> goalsPos = new ArrayList<>();

        Heuristic heuristic = new Heuristic();
        int nodeHeuristic = 0;
        int minimumHeuristic;
        int currentDistance;

        Node initialNode = null;
        PriorityQueue<Node> queueNodes = null;
        HashSet<Node> hashSet = new HashSet<>();

        int playerX, playerY;
        int newTileX = 0, newTileY = 0;
        char directions[] = {'u', 'd', 'l', 'r'};

        Node newNode = null;
        Position newPlayer = null;
        ArrayList<Position> newBoxes;
        int newHeuristic, newWeight;
        Node newParent = null;
        char newMove;

        boolean boxFlag;
        int boxIdx = 0;

        int boxNewX = 0, boxNewY = 0;
        boolean blockedByBox;


        // Extract the player, crates, and goals
        for (int i = 0; i < ROW; i++){
            for (int j = 0; j < COL; j++){
                if (itemsData[i][j] == '@'){ // Player
                    playerPos = new Position(i, j);
                }

                if (itemsData[i][j] == '$'){ // Crates
                    boxesPos.add(new Position(i, j));
                }

                if (mapData[i][j] == '.'){ // goal
                    goalsPos.add(new Position(i, j));
                }
            }
        }

        // Calculate heauristic of initial node
        for (Position box: boxesPos){
            minimumHeuristic = Integer.MAX_VALUE;

            for (Position goal: goalsPos){
                currentDistance = heuristic.manhattanDistance(goal.getX(), goal.getY(), box.getX(), box.getY());
                if (minimumHeuristic > currentDistance){
                    minimumHeuristic = currentDistance;
                }
            }
            nodeHeuristic += minimumHeuristic;
        }

        // initializes the root node
        initialNode = new Node(playerPos, boxesPos, nodeHeuristic, 0, null, ' ');

        //sets the rule that queueNodes will sort its queue with top being the smallest to largest
        queueNodes = new PriorityQueue<>((nodeA, nodeB) -> Integer.compare(nodeA.getHeuristic() + nodeA.getWeight(), nodeB.getHeuristic()  + nodeB.getWeight()));

        // adds the thingy initial node
        queueNodes.add(initialNode);

        // Expands the nodes
        while(!queueNodes.isEmpty()){
            Node currentNode = queueNodes.poll(); // "Pops" and return the lowest value in the queue (Lowest value kaagad dahil sa algorithmn sa taas)

            if (hashSet.contains(currentNode)) {
                continue;
            }
            hashSet.add(currentNode);

            //Checks if all goals are reached
            boolean allGoals = true;
            for(Position box : currentNode.getBoxes()){ // for each box, check if that box is on the array list of boxes
                boolean eachBox = false;
                for(Position goal : goalsPos){
                    if(box.equals(goal)){
                        eachBox = true;
                        break; // put more breaks to be faster
                    }
                }

                if(!eachBox){
                    allGoals = false;
                    break;
                }
            }

            // Trace back to start if ALL GOALS ARE FOUND
            if(allGoals==true){
                Node traceBack = currentNode;
                StringBuilder path = new StringBuilder(); // Study later...?
                path.ensureCapacity(500);

                while(traceBack.getParent()!=null){ // "While there is a parent", so until initial node.
                    path.append(traceBack.getMove());
                    traceBack = traceBack.getParent();
                }

                path.reverse(); //STARTS FROM THE BEGINNING TO THE GOAL.
                return path.toString(); // ENDS HERE IF ALL GOALS ARE FOUND, returns the string direction.
            }

            // =========================================================================================================
            for (int i = 0; i < 4; i++){
                playerX = currentNode.getPlayer().getX();
                playerY = currentNode.getPlayer().getY();

                switch(directions[i]){
                    case 'u':
                        newTileX =  playerX - 1;
                        newTileY =  playerY;
                        break;

                    case 'd':
                        newTileX =  playerX + 1;
                        newTileY =  playerY;
                        break;

                    case 'l':
                        newTileX =  playerX;
                        newTileY =  playerY - 1;
                        break;

                    case 'r':
                        newTileX =  playerX;
                        newTileY =  playerY + 1;
                        break;
                }

                if ((newTileX < ROW) && (newTileY < COL) && (newTileX >= 0) && (newTileY >= 0) && (mapData[newTileX][newTileY] != '#')){

                    boxFlag = false;

                    for (int j = 0; j < currentNode.getBoxes().size(); j++){
                        if (currentNode.getBoxes().get(j).getX()== newTileX && currentNode.getBoxes().get(j).getY() == newTileY){
                            boxFlag = true;
                            boxIdx = j;
                            break;
                        }
                    }

                    if (boxFlag){

                      //calculate where box would go (one more step in same direction)

                      switch(directions[i]){
                          case 'u':
                              boxNewX = newTileX - 1;
                              boxNewY = newTileY;
                              break;

                          case 'd':
                              boxNewX = newTileX + 1;
                              boxNewY = newTileY;
                              break;

                          case 'l':
                              boxNewX = newTileX;
                              boxNewY = newTileY - 1;
                              break;

                          case 'r':
                              boxNewX = newTileX;
                              boxNewY = newTileY + 1;
                              break;

                      }

                      if ((boxNewX < ROW) && (boxNewY < COL) && (boxNewX >= 0) && (boxNewY >= 0) && (mapData[boxNewX][boxNewY] != '#')){

                          blockedByBox = false;
                          for (int j = 0; j < currentNode.getBoxes().size(); j++){
                              if (j != boxIdx){

                                  if ((currentNode.getBoxes().get(j).getX()== boxNewX) && (currentNode.getBoxes().get(j).getY() == boxNewY)){
                                      blockedByBox = true;
                                      break;
                                  }
                              }
                          }

                          if (blockedByBox == false){
                              newPlayer = new Position(newTileX, newTileY);
                              newBoxes = new ArrayList<>();

                              for (int j = 0; j < currentNode.getBoxes().size(); j++){
                                  Position oldBox = currentNode.getBoxes().get(j);
                                  if (j == boxIdx){
                                      newBoxes.add(new Position(boxNewX, boxNewY));
                                  }
                                  else{
                                      newBoxes.add(new Position(oldBox.getX(), oldBox.getY()));
                                  }
                              }

                              nodeHeuristic = 0;
                              for (Position newBox: newBoxes){
                                  minimumHeuristic = Integer.MAX_VALUE;

                                  for (Position goal: goalsPos){
                                      currentDistance = heuristic.manhattanDistance(goal.getX(), goal.getY(), newBox.getX(), newBox.getY());

                                      if (minimumHeuristic > currentDistance){
                                          minimumHeuristic = currentDistance;
                                      }
                                  }
                                  nodeHeuristic += minimumHeuristic;
                              }

                              newHeuristic = nodeHeuristic;
                              newWeight = currentNode.getWeight() + 1;
                              newParent = currentNode;
                              newMove = directions[i];

                              newNode = new Node(newPlayer, newBoxes, newHeuristic, newWeight, newParent, newMove);
                              queueNodes.add(newNode);
                          }
                      }
                    }
                      //Position boxNewPosition = new Position(boxNewX, boxNewY);

                      //check if box can be pushed to new position
                      //must not be wall or out of bounds or have another box there

                    else{
                        newPlayer = new Position(newTileX, newTileY);
                        newBoxes = currentNode.getBoxes();

                        nodeHeuristic = 0;
                        for (Position newBox: newBoxes){
                            minimumHeuristic = Integer.MAX_VALUE;

                            for (Position goal: goalsPos){
                                currentDistance = heuristic.manhattanDistance(goal.getX(), goal.getY(), newBox.getX(), newBox.getY());

                                if (minimumHeuristic > currentDistance){
                                    minimumHeuristic = currentDistance;
                                }
                            }
                            nodeHeuristic += minimumHeuristic;
                        }

                        newHeuristic = nodeHeuristic;
                        newWeight = currentNode.getWeight() + 1;
                        newParent = currentNode;
                        newMove = directions[i];

                        newNode = new Node(newPlayer, newBoxes, newHeuristic, newWeight, newParent, newMove);
                        queueNodes.add(newNode);
                    }
                }

            }
        }

        return "";
    }
}
