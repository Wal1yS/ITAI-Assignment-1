import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Scanner;

public class Astar {

    // Global Constants
    static final int MAX_CELL_INDEX = 12;
    static final int MIN_CELL_INDEX = 0;
    static final int INFINITE_DISTANCE = 1000000000;
    static final int NUMBER_OF_DIRECTIONS = 4;
    static final int DIRECTIONS_WITH_RING_TOGGLE = 5;
    static final int TOWER_AND_NAZGUL_MAXIMUM_P_ZONE = 3;
    static final int[] POSSIBLE_X_DIRECTIONS = {0, 0, 1, -1};
    static final int[] POSSIBLE_Y_DIRECTIONS = {1, -1, 0, 0};

    /**
     * Calculates the Manhattan (or "von Neumann") distance between two points.
     * Used for enemy perception zones.
     */
    public static int neumannDistance(int xCurrent, int yCurrent, int xDestination, int yDesignation) {
        return Math.abs(xCurrent - xDestination) + Math.abs(yCurrent - yDesignation);
    }

    /**
     * Calculates the Chebyshev (or "Moore") distance between two points.
     * Used for enemy perception zones.
     */
    public static int mooreDistance(int xCurrent, int yCurrent, int xDestination, int yDesignation) {
        return Math.max(Math.abs(xCurrent - xDestination), Math.abs(yCurrent - yDesignation));
    }

    /**
     * Represents a single cell on the 13x13 map.
     */
    static class Cell {
        private int xCoordinate;
        private int yCoordinate;
        Cell(int x, int y) {
            this.xCoordinate = x;
            this.yCoordinate = y;
        }
        /** Checks if two Cell have the same coordinates. */
        public boolean equals(Object anotherObject) {
            if (this == anotherObject) {
                return true;
            }
            if (anotherObject == null || getClass() != anotherObject.getClass()) {
                return false;
            }
            Cell anotherCell = (Cell) anotherObject;
            boolean ifEqual = false;
            if (xCoordinate == anotherCell.xCoordinate && yCoordinate == anotherCell.yCoordinate) {
                ifEqual = true;
            }
            return ifEqual;
        }
        /** Generates a hash code based on the cell's coordinates. */
        public int hashCode() {
            return Objects.hash(xCoordinate, yCoordinate);
        }
    }
    /**
     * Represents a cell in the A* search space.
     * Implements Comparable to be used in a PriorityQueue.
     */
    static class AStarCell implements Comparable<AStarCell> {
        private int xCoordinate;
        private int yCoordinate;
        private int gCost; // g: cost from start
        private int hCost; // h: heuristic cost to target
        private AStarCell parent;
        private boolean isRingEquipped;
        private boolean isCoatEquipped;

        AStarCell(int x, int y, boolean hasRing, boolean hasCoat, AStarCell parent, int xTarget, int yTarget) {
            this.xCoordinate = x;
            this.yCoordinate = y;
            this.isRingEquipped = hasRing;
            this.isCoatEquipped = hasCoat;
            this.parent = parent;
            this.gCost = 0;
            if (parent != null) {
                this.gCost = parent.gCost + 1;
            }
            this.hCost = neumannDistance(x, y, xTarget, yTarget);
        }
        /** Calculates the total estimated cost (g + h) for this node. */
        int fCost() {
            return gCost + hCost;
        }
        /** Compares cells based on their fCost for the PriorityQueue. */
        public int compareTo(AStarCell anotherCell) {
            if (this.fCost() < anotherCell.fCost()) {
                return -1;
            } else if (this.fCost() > anotherCell.fCost()) {
                return 1;
            } else {
                return 0;
            }
        }
        /** Checks if two nodes represent the same cell state (position + items). */
        public boolean equals(Object anotherObject) {
            if (this == anotherObject) {
                return true;
            }
            if (anotherObject == null || getClass() != anotherObject.getClass()) {
                return false;
            }
            AStarCell anotherCell = (AStarCell) anotherObject;
            if (xCoordinate == anotherCell.xCoordinate && yCoordinate == anotherCell.yCoordinate
                    && isRingEquipped == anotherCell.isRingEquipped && isCoatEquipped == anotherCell.isCoatEquipped) {
                return true;
            }
            return false;
        }
        /** Generates a hash code based on the cell complete state. */
        public int hashCode() {
            return Objects.hash(xCoordinate, yCoordinate, isRingEquipped, isCoatEquipped);
        }
    }
    /** Defines the possible types of actions the agent can take. */
    enum TurnTypes {
        MOVEMENT,
        RINGEQUIPMENT
    }
    /** Represents the single next action that agent will do. */
    static class Action {
        private TurnTypes type;
        private int xCoordinate;
        private int yCoordinate;
        private boolean isRingEquipped;
        private boolean isCoatEquipped;
        Action(TurnTypes type, int x, int y, boolean hasRing, boolean hasCoat) {
            this.type = type;
            this.xCoordinate = x;
            this.yCoordinate = y;
            this.isRingEquipped = hasRing;
            this.isCoatEquipped = hasCoat;
        }
    }
    /** Manages all knowledge about the map, including enemy positions and danger zones. */
    static class MapState {
        private HashSet<Cell> orcLocation = new HashSet<>();
        private HashSet<Cell> urukLocation = new HashSet<>();
        private HashSet<Cell> nazgulLocation = new HashSet<>();
        private HashSet<Cell> towerLocation = new HashSet<>();
        private HashSet<Cell> pZoneLocation = new HashSet<>();
        private Cell coatLocation = null;
        /** Reads new perception data from the interactor and updates enemy locations. */
        public void updateState(Scanner scanner) {
            int count = scanner.nextInt();
            for (int i = 0; i < count; i++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                char enemy = scanner.next().charAt(0);
                Cell pos = new Cell(x, y);
                switch (enemy) {
                    case 'O':
                        orcLocation.add(pos);
                        break;
                    case 'U':
                        urukLocation.add(pos);
                        break;
                    case 'N':
                        nazgulLocation.add(pos);
                        break;
                    case 'W':
                        towerLocation.add(pos);
                        break;
                    case 'P':
                        pZoneLocation.add(pos);
                        break;
                    case 'C':
                        coatLocation = pos;
                        break;
                    default:
                        break;
                }
            }
        }
        /** Returns the known location of the coat. */
        public Cell getCoatLocation() {
            return coatLocation;
        }
        /** Checks if a cell is free from Orcs based on the agent's state. */
        private boolean isOrcFree(int x, int y, boolean isCoatEquipped, boolean isRingEquipped) {
            int radius;
            if (isCoatEquipped || isRingEquipped) {
                radius = 0;
            } else {
                radius = 1;
            }
            for (int i = 0; i < orcLocation.size(); i++) {
                Cell current = (Cell) orcLocation.toArray()[i];
                if (neumannDistance(x, y, current.xCoordinate, current.yCoordinate) <= radius) {
                    return false;
                }
            }
            return true;
        }
        /** Checks if a cell is free from Uruk. */
        private boolean isUrukFree(int x, int y, boolean isCoatEquipped, boolean isRingEquipped) {
            int radius;
            if (isCoatEquipped || isRingEquipped) {
                radius = 1;
            } else {
                radius = 2;
            }
            for (int i = 0; i < urukLocation.size(); i++) {
                Cell current = (Cell) urukLocation.toArray()[i];
                if (neumannDistance(x, y, current.xCoordinate, current.yCoordinate) <= radius) {
                    return false;
                }
            }
            return true;
        }
        /** Checks if a cell is free from Watchtower. */
        private boolean isTowerFree(int x, int y, boolean isRingEquipped) {
            for (int i = 0; i < towerLocation.size(); i++) {
                Cell current = (Cell) towerLocation.toArray()[i];
                int xDistance = Math.abs(x - current.xCoordinate);
                int yDistance = Math.abs(y - current.yCoordinate);
                if (isRingEquipped) {
                    if (mooreDistance(x, y, current.xCoordinate, current.yCoordinate) <= 2
                            || (xDistance == TOWER_AND_NAZGUL_MAXIMUM_P_ZONE && yDistance == 0)
                            || (xDistance == 0 && yDistance == TOWER_AND_NAZGUL_MAXIMUM_P_ZONE)) {
                        return false;
                    }
                } else {
                    if (mooreDistance(x, y, current.xCoordinate, current.yCoordinate) <= 2) {
                        return false;
                    }
                }
            }
            return true;
        }
        /** Checks if a cell is free from Nazgul. */
        private boolean isNazgulFree(int x, int y, boolean isCoatEquipped, boolean isRingEquipped) {
            for (int i = 0; i < nazgulLocation.size(); i++) {
                Cell current = (Cell) nazgulLocation.toArray()[i];
                int xDistance = Math.abs(x - current.xCoordinate);
                int yDistance = Math.abs(y - current.yCoordinate);
                if (isCoatEquipped) {
                    if (mooreDistance(x, y, current.xCoordinate, current.yCoordinate) <= 1) {
                        return false;
                    }
                } else if (isRingEquipped) {
                    if (mooreDistance(x, y, current.xCoordinate, current.yCoordinate) <= 2
                            || (xDistance == TOWER_AND_NAZGUL_MAXIMUM_P_ZONE && yDistance == 0)
                            || (xDistance == 0 && yDistance == TOWER_AND_NAZGUL_MAXIMUM_P_ZONE)) {
                        return false;
                    }
                } else {
                    if (mooreDistance(x, y, current.xCoordinate, current.yCoordinate) <= 1
                            || (xDistance == 2 && yDistance == 0) || (xDistance == 0 && yDistance == 2)) {
                        return false;
                    }
                }
            }
            return true;
        }
        /** The main safety check, combining all individual checks. */
        public boolean isCellSafe(int x, int y, boolean isCoatEquipped, boolean isRingEquipped) {
            if (x < MIN_CELL_INDEX || x > MAX_CELL_INDEX || y < MIN_CELL_INDEX || y > MAX_CELL_INDEX) {
                return false;
            }
            if (pZoneLocation.contains(new Cell(x, y))) {
                return false;
            }
            boolean orcSafety = isOrcFree(x, y, isCoatEquipped, isRingEquipped);
            boolean urukSafety = isUrukFree(x, y, isCoatEquipped, isRingEquipped);
            boolean towersSafety = isTowerFree(x, y, isRingEquipped);
            boolean nazgulSafety = isNazgulFree(x, y, isCoatEquipped, isRingEquipped);
            if (orcSafety && urukSafety && towersSafety && nazgulSafety) {
                return true;
            }
            return false;
        }
    }
    /** Implements the A* pathfinding algorithm. */
    static class AStarSolver {
        private int xTargetCoordinate;
        private int yTargetCoordinate;
        /** Finds the optimal path and returns the first action to take. */
        public Action findNextAction(Cell start, boolean startHasRing,
                                     boolean startHasCoat, Cell target, MapState map) {
            xTargetCoordinate = target.xCoordinate;
            yTargetCoordinate = target.yCoordinate;
            AStarCell startCell = new AStarCell(start.xCoordinate, start.yCoordinate,
                    startHasRing, startHasCoat, null, xTargetCoordinate, yTargetCoordinate);
            /** The "cellsToVisit" implemented as a Priority Queue. It automatically keeps the cell
             with the lowest fCost at the front */
            PriorityQueue<AStarCell> cellsToVisit = new PriorityQueue<>();
            // The "gCost Map" stores the cost of the cheapest path from the start to a given cell
            HashMap<AStarCell, Integer> gCostMap = new HashMap<>();
            cellsToVisit.add(startCell);
            gCostMap.put(startCell, 0);
            // Main A* loop: continues as long as there are discovered cells to evaluate.
            while (!cellsToVisit.isEmpty()) {
                AStarCell currentCell = cellsToVisit.poll();
                // Goal check: if we've reached the target, reconstruct the path.
                if (currentCell.xCoordinate == xTargetCoordinate && currentCell.yCoordinate == yTargetCoordinate) {
                    return reconstructAction(currentCell);
                }
                // Explore all valid neighbors of the current cell.
                addNeighbors(currentCell, cellsToVisit, gCostMap, map);
            }
            return null; // No path found.
        }
        /** Generates and processes all valid neighbors for the current node. */
        private void addNeighbors(AStarCell currentCell, PriorityQueue<AStarCell> cellsToVisit,
                                  HashMap<AStarCell, Integer> gCostMap, MapState map) {
            // Explore neighbors in 4 directions + 1 ring toggle action.
            for (int i = 0; i < DIRECTIONS_WITH_RING_TOGGLE; i++) {
                int currentXCoordinate = currentCell.xCoordinate;
                int currentYCoordinate = currentCell.yCoordinate;
                boolean ifRingCurrentlyEquipped = currentCell.isRingEquipped;
                boolean ifCoatCurrentlyEquipped = currentCell.isCoatEquipped;
                if (i < NUMBER_OF_DIRECTIONS) { // Move actions
                    currentXCoordinate += POSSIBLE_X_DIRECTIONS[i];
                    currentYCoordinate += POSSIBLE_Y_DIRECTIONS[i];
                    Cell coatLocation = map.getCoatLocation();
                    if (!ifCoatCurrentlyEquipped && coatLocation != null && currentXCoordinate
                            == coatLocation.xCoordinate && currentYCoordinate == coatLocation.yCoordinate) {
                        ifCoatCurrentlyEquipped = true;
                    }
                } else {  // Ring toggle action
                    ifRingCurrentlyEquipped = !currentCell.isRingEquipped;
                }

                // If the neighbor state is not safe, ignore it.
                if (!map.isCellSafe(currentXCoordinate, currentYCoordinate,
                        ifCoatCurrentlyEquipped, ifRingCurrentlyEquipped)) {
                    continue;
                }
                AStarCell neighbor = new AStarCell(currentXCoordinate, currentYCoordinate, ifRingCurrentlyEquipped,
                        ifCoatCurrentlyEquipped, currentCell, xTargetCoordinate, yTargetCoordinate);
                int newGCost = currentCell.gCost + 1;
                /** This is the core of A*'s optimality. We only consider this new path to the
                 neighbor if it's cheaper than any previously found path to the same state. */
                if (newGCost < gCostMap.getOrDefault(neighbor, INFINITE_DISTANCE)) {
                    gCostMap.put(neighbor, newGCost);
                    cellsToVisit.add(neighbor);
                }
            }
        }
        /** Go back from the goal node to find the first step of the path. */
        private Action reconstructAction(AStarCell target) {
            AStarCell targetCell = target;
            if (targetCell.parent == null) {
                return null;
            }
            // Traverse back up the path until we find the node right after the start.
            while (targetCell.parent != null && targetCell.parent.parent != null) {
                targetCell = targetCell.parent;
            }
            // Determine if the first action was a move or a ring toggle.
            TurnTypes type = TurnTypes.RINGEQUIPMENT;
            if (targetCell.xCoordinate != targetCell.parent.xCoordinate
                    || targetCell.yCoordinate != targetCell.parent.yCoordinate) {
                type = TurnTypes.MOVEMENT;
            }
            return new Action(type, targetCell.xCoordinate, targetCell.yCoordinate,
                    targetCell.isRingEquipped, targetCell.isCoatEquipped);
        }
    }
    /** The main "conductor" class that directs the game loop. */
    static class Journey {
        private Scanner scanner = new Scanner(System.in);
        private MapState map = new MapState();
        private AStarSolver solver = new AStarSolver();
        private int currentX = 0;
        private int currentY = 0;
        private int moveCount = 0;
        private boolean isCoatEquipped = false;
        private boolean isRingEquipped = false;
        private boolean gollumFound = false;
        private int coordinateXOfGollum = -1;
        private int coordinateYOfGollum = -1;
        private int coordinateXOfMount = -1;
        private int coordinateYOfMount = -1;
        /** Initializes the game by reading the start state. */
        public void startJourney() {
            try {
                scanner.nextInt();
                coordinateXOfGollum = scanner.nextInt();
                coordinateYOfGollum = scanner.nextInt();
                map.updateState(scanner);
                while (true) {
                    executeNextAction();
                }
            } catch (Exception e) {
                System.out.println("e -1");
            }
        }

        /** The main game loop: plan, act, update, observe. */
        private void executeNextAction() {
            Cell target;
            if (gollumFound) {
                target = new Cell(coordinateXOfMount, coordinateYOfMount);
            } else {
                target = new Cell(coordinateXOfGollum, coordinateYOfGollum);
            }

            if (currentX == target.xCoordinate && currentY == target.yCoordinate) {
                if (gollumFound) {
                    System.out.println("e " + moveCount);
                    System.exit(0);
                }
            }

            // 1. Plan: Ask the pathfinder for the next best action.
            Action nextAction = solver.findNextAction(new Cell(currentX, currentY),
                    isRingEquipped, isCoatEquipped, target, map);
            if (nextAction == null) {
                System.out.println("e -1");
                System.exit(0);
            }

            // 2. Act: Send the command to the interactor.
            if (nextAction.type == TurnTypes.MOVEMENT) {
                System.out.println("m " + nextAction.xCoordinate + " " + nextAction.yCoordinate);
            } else {
                System.out.println(nextAction.isRingEquipped ? "r" : "rr");
            }
            moveCount++;

            // 3. Update: Synchronize the agent's real state with the state from the plan.
            currentX = nextAction.xCoordinate;
            currentY = nextAction.yCoordinate;
            isRingEquipped = nextAction.isRingEquipped;
            isCoatEquipped = nextAction.isCoatEquipped;

            // 4. Observe: Check for consequences and get new info.
            if (!gollumFound && currentX == coordinateXOfGollum && currentY == coordinateYOfGollum) {
                map.updateState(scanner);
                gollumFound = true;
                String line = "";
                do {
                    if (scanner.hasNextLine()) {
                        line = scanner.nextLine().trim();
                    }
                } while (line.isEmpty());

                String[] parts = line.replaceAll("[^0-9 ]", " ").trim().split("\\s+");
                if (parts.length >= 2) {
                    coordinateXOfMount = Integer.parseInt(parts[0]);
                    coordinateYOfMount = Integer.parseInt(parts[1]);
                } else {
                    coordinateXOfMount = scanner.nextInt();
                    coordinateYOfMount = scanner.nextInt();
                    if (scanner.hasNextLine()) {
                        scanner.nextLine();
                    }
                }

            } else {
                map.updateState(scanner);
            }
        }
    }
    /** The main entry point of the program. */
    public static void main(String[] args) {
        new Journey().startJourney();
    }
}
