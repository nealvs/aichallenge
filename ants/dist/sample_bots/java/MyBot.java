
import java.util.*;

public class MyBot implements Bot {

    public static void main(String[] args) {
        Ants.run(new MyBot());
    }

    // things to remember between turns
    private Map<Tile, Aim> antStraight = new HashMap<Tile, Aim>();
    private Map<Tile, Aim> antLefty = new HashMap<Tile, Aim>();
    
    public void do_turn(Ants ants) {
        
        Map<Tile, Aim> newStraight = new HashMap<Tile, Aim>();
        Map<Tile, Aim> newLefty = new HashMap<Tile, Aim>();
        
        Set<Tile> destinations = new HashSet<Tile>();
        Set<Tile> targets = new HashSet<Tile>();
        
        targets.addAll(ants.food());
        targets.addAll(ants.enemyHills());
        targets.addAll(ants.unseen());
        
        int maxPerTarget = ants.myAnts().size();
        if(targets.size() > 0) {
            maxPerTarget = ants.myAnts().size() / targets.size();
        }
        if(maxPerTarget <= 0) {
            maxPerTarget = 1;
        }
        
        System.out.println("Max Per Target: " + maxPerTarget);
        
        Map<Tile, List<Tile>> targettingAnts = new HashMap<Tile, List<Tile>>();
        
        for (Tile antLocation : ants.myAnts()) {
            boolean issued = false;
            Tile closestTarget = null;
            int closestDistance = 999999;
            for (Tile target : targets) {
                // Limit how many ants can go to this target
                if(targettingAnts.containsKey(target) && targettingAnts.get(target).size() > maxPerTarget) {
                    continue;
                }
                
                // Check the distance
                int distance = ants.distance(antLocation, target);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
            
            if (closestTarget != null) {
                // Attack/gather
                List<Aim> directions = ants.directions(antLocation, closestTarget);
                Collections.shuffle(directions);
                for (Aim direction : directions) {
                    Tile destination = ants.tile(antLocation, direction);
                    if (ants.ilk(destination).isUnoccupied() && !destinations.contains(destination)) {
                        ants.issueOrder(antLocation, direction);
                        destinations.add(destination);
                        issued = true;
                        if(targettingAnts.containsKey(closestTarget)) {
                            targettingAnts.get(closestTarget).add(antLocation);
                        } else {
                            List<Tile> antList = new ArrayList<Tile>();
                            antList.add(antLocation);
                            targettingAnts.put(closestTarget, antList);
                        }
                        break;
                    }
                }
            } 
            
            if (!issued) {
                // Explore
                // Send new ants in a straight line
                if (!antStraight.containsKey(antLocation) && !antLefty.containsKey(antLocation)) {
                    Aim direction;
                    if (antLocation.row() % 2 == 0) {
                        if (antLocation.col() % 2 == 0) {
                            direction = Aim.NORTH;
                        } else {
                            direction = Aim.SOUTH;
                        }
                    } else {
                        if (antLocation.col() % 2 == 0) {
                            direction = Aim.EAST;
                        } else {
                            direction = Aim.WEST;
                        }
                    }
                    antStraight.put(antLocation, direction);
                }
                // send ants going in a straight line in the same direction
                if (antStraight.containsKey(antLocation)) {
                    Aim direction = antStraight.get(antLocation);
                    Tile destination = ants.tile(antLocation, direction);
                    if (ants.ilk(destination).isPassable()) {
                        if (ants.ilk(destination).isUnoccupied() && !destinations.contains(destination)) {
                            ants.issueOrder(antLocation, direction);
                            newStraight.put(destination, direction);
                            destinations.add(destination);
                        } else {
                            // pause ant, turn and try again next turn
                            newStraight.put(antLocation, direction.left());
                            destinations.add(antLocation);
                        }
                    } else {
                        // hit a wall, start following it
                        antLefty.put(antLocation, direction.right());
                    }
                }
                // send ants following a wall, keeping it on their left
                if (antLefty.containsKey(antLocation)) {
                    Aim direction = antLefty.get(antLocation);
                    List<Aim> directions = new ArrayList<Aim>();
                    directions.add(direction.left());
                    directions.add(direction);
                    directions.add(direction.right());
                    directions.add(direction.behind());
                    // try 4 directions in order, attempting to turn left at corners
                    for (Aim new_direction : directions) {
                        Tile destination = ants.tile(antLocation, new_direction);
                        if (ants.ilk(destination).isPassable()) {
                            if (ants.ilk(destination).isUnoccupied() && !destinations.contains(destination)) {
                                ants.issueOrder(antLocation, new_direction);
                                newLefty.put(destination, new_direction);
                                destinations.add(destination);
                                break;
                            } else {
                                // pause ant, turn and send straight
                                newStraight.put(antLocation, direction.right());
                                destinations.add(antLocation);
                                break;
                            }
                        }
                    }
                }
            }
            
        }
        
        antStraight = newStraight;
        antLefty = newLefty;
    }
}
