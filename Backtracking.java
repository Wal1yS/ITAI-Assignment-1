import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayDeque;

public class Backtracking {

    // Global Constants
    static final int MAX_CELL_INDEX = 12;
    static final int MIN_CELL_INDEX = 0;
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
     * Represents a state in the Breadth-First Search space.
     * A state is defined by coordinates, ring status, and coat status.
     */
    private static class BFSCell {
        private int xCoordinate;
        private int yCoordinate;
        private boolean isRingEquipped;
        private boolean isCoatEquipped;

        BFSCell(int x, int y, boolean hasRing, boolean hasCoat) {
            this.xCoordinate = x;
            this.yCoordinate = y;
            this.isRingEquipped = hasRing;
            this.isCoatEquipped = hasCoat;
        }

        /** Checks if two BFS cells represent the same state. */
        public boolean equals(Object anotherObject) {
            if (this == anotherObject) {
                return true;
            }
            if (anotherObject == null || getClass() != anotherObject.getClass()) {
                return false;
            }
            BFSCell anotherCell = (BFSCell) anotherObject;
            if (xCoordinate == anotherCell.xCoordinate && yCoordinate == anotherCell.yCoordinate
                    && isRingEquipped == anotherCell.isRingEquipped && isCoatEquipped == anotherCell.isCoatEquipped) {
                return true;
            }
            return false;
        }
        /** Generates a hash code based on the complete state. */
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
    /** Implements the Breadth-First Search (BFS) pathfinding algorithm. */
    static class BFSSolver {
        /**
         * Finds the shortest path (in number of steps) and returns the first action.
         */
        public Action findNextAction(Cell start, boolean startHasRing,
                                     boolean startHasCoat, Cell target, MapState map) {
            // A queue for cells to visit, following the FIFO principle of BFS.
            ArrayDeque<BFSCell> cellsToVisit = new ArrayDeque<>();
            // Stores the path by mapping a state to the state that came before it.
            HashMap<BFSCell, BFSCell> movementHistory = new HashMap<>();
            // Prevents cycles by keeping track of states already visited or in the queue.
            HashSet<BFSCell> visited = new HashSet<>();

            BFSCell startCell = new BFSCell(start.xCoordinate, start.yCoordinate, startHasRing, startHasCoat);
            cellsToVisit.add(startCell);
            visited.add(startCell);

            while (!cellsToVisit.isEmpty()) {
                BFSCell currentCell = cellsToVisit.poll();

                // Goal check: if we've reached the target, reconstruct the path.
                if (currentCell.xCoordinate == target.xCoordinate && currentCell.yCoordinate == target.yCoordinate) {
                    return reconstructAction(movementHistory, currentCell, startCell);
                }

                // Explore neighbors in 4 directions + 1 ring toggle action.
                for (int i = 0; i < DIRECTIONS_WITH_RING_TOGGLE; i++) {
                    int currentXCoordinate = currentCell.xCoordinate;
                    int currentYCoordinate = currentCell.yCoordinate;
                    boolean ifRingCurrentlyEquipped = currentCell.isRingEquipped;
                    boolean ifCoatCurrentlyEquipped = currentCell.isCoatEquipped;

                    if (i < NUMBER_OF_DIRECTIONS) { // Move action
                        currentXCoordinate += POSSIBLE_X_DIRECTIONS[i];
                        currentYCoordinate += POSSIBLE_Y_DIRECTIONS[i];
                        Cell coatLocation = map.getCoatLocation();
                        if (!ifCoatCurrentlyEquipped && coatLocation != null && currentXCoordinate
                                == coatLocation.xCoordinate && currentYCoordinate == coatLocation.yCoordinate) {
                            ifCoatCurrentlyEquipped = true;
                        }
                    } else { // Ring toggle action
                        ifRingCurrentlyEquipped = !currentCell.isRingEquipped;
                    }

                    if (map.isCellSafe(currentXCoordinate, currentYCoordinate,
                            ifCoatCurrentlyEquipped, ifRingCurrentlyEquipped)) {
                        BFSCell neighbor = new BFSCell(currentXCoordinate, currentYCoordinate,
                                ifRingCurrentlyEquipped, ifCoatCurrentlyEquipped);
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            movementHistory.put(neighbor, currentCell); // Record the path
                            cellsToVisit.add(neighbor); // Add to the end of the queue
                        }
                    }
                }
            }
            return null; // No path found.
        }
        /**
         * Backtracks from the goal to find the very first step of the path.
         */
        private Action reconstructAction(HashMap<BFSCell, BFSCell> movementHistory, BFSCell target, BFSCell start) {
            BFSCell targetCell = target;
            if (!movementHistory.containsKey(targetCell)) {
                return null;
            }
            // Traverse back up the path until we find the node right after the start.
            while (movementHistory.get(targetCell) != null && !movementHistory.get(targetCell).equals(start)) {
                targetCell = movementHistory.get(targetCell);
            }

            // Determine if the first action was a move or a ring toggle.
            TurnTypes type = TurnTypes.RINGEQUIPMENT;
            if (targetCell.xCoordinate != start.xCoordinate || targetCell.yCoordinate != start.yCoordinate) {
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
        private BFSSolver solver = new BFSSolver();

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
