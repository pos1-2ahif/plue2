package pos1_2ahif.test2;

import java.io.File;
import java.util.*;

/**
 * Created by Florian on 08.12.2014.
 */
public final class Labyrinth implements Map<Labyrinth.Coords, Labyrinth.Tile> {
    public interface Exercises {
        public boolean hasAnyTreasure(Labyrinth labyrinth);

        public List<Coords> getTreasuresOrderedByValue(Labyrinth labyrinth);

        public List<Coords> getTreasuresOrderedByValuePerWeight(Labyrinth labyrinth);

        public Map<Coords, Tile> clearPassagesAlongPath(Labyrinth labyrinth, List<Direction> path);

        public List<Direction> joinPaths(List<List<Direction>> paths);

        public void printPlanForTreasureHunt(Labyrinth labyrinth, List<Direction> path, File file);
    }

    public static final class Coords {
        private int x;
        private int y;

        public Coords(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Coords go(Direction direction) {
            if (direction instanceof Left) {
                return new Coords(x - 1, y);
            } else if (direction instanceof Right) {
                return new Coords(x + 1, y);
            } else if (direction instanceof Up) {
                return new Coords(x, y - 1);
            } else if (direction instanceof Down) {
                return new Coords(x, y + 1);
            } else {
                throw new IllegalArgumentException("Unexpected direction to go: " + direction);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Coords coords = (Coords) o;

            if (x != coords.x) return false;
            if (y != coords.y) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

        @Override
        public String toString() {
            return "[" + x + " / " + y + ']';
        }
    }

    public interface Passage {
        boolean isOpen();
    }

    public static abstract class Tile {
        public abstract Passage getLeft();

        public abstract Passage getRight();

        public abstract Passage getUp();

        public abstract Passage getDown();

        public final Passage getDirection(Direction direction) {
            if (direction instanceof Left) {
                return getLeft();
            } else if (direction instanceof Right) {
                return getRight();
            } else if (direction instanceof Up) {
                return getUp();
            } else if (direction instanceof Down) {
                return getDown();
            } else {
                throw new IllegalArgumentException("Unexpected direction to get: " + direction);
            }
        }
    }

    public static abstract class Direction {
        private Direction() {
        }
    }

    public static final class Left extends Direction {
    }

    public static final class Right extends Direction {
    }

    public static final class Up extends Direction {
    }

    public static final class Down extends Direction {
    }

    public interface Treasure {
        float getValue();

        float getWeight();
    }

    public interface CollectedTreasure extends Treasure {
    }

    // see here, how your interface is used:

    public List<Direction> explore(Exercises e, float carryCapacity, File report) {
        if (!e.hasAnyTreasure(this)) {
            return Collections.emptyList();
        }

        List<List<Direction>> pathsForValue = new LinkedList<List<Direction>>();
        List<List<Direction>> pathsForValuePerWeight = new LinkedList<List<Direction>>();

        float treasuresCollectedWhenPrioritizingValue =
                collectAsMuchTreasureAsPossible(
                        e.getTreasuresOrderedByValue(this),
                        carryCapacity,
                        pathsForValue);

        float treasuresCollectedWhenPrioritizingValuePerWeight =
                collectAsMuchTreasureAsPossible(
                        e.getTreasuresOrderedByValuePerWeight(this),
                        carryCapacity,
                        pathsForValuePerWeight);

        List<List<Direction>> betterRoute;

        if (treasuresCollectedWhenPrioritizingValue > treasuresCollectedWhenPrioritizingValuePerWeight) {
            betterRoute = pathsForValue;
        } else {
            betterRoute = pathsForValuePerWeight;
        }

        List<Direction> path = e.joinPaths(betterRoute);

        clearPassages(e.clearPassagesAlongPath(this, path));

        e.printPlanForTreasureHunt(this, path, report);

        return path;
    }

    // -- no need to read further, not relevant for exercise!

    private float collectAsMuchTreasureAsPossible(List<Coords> treasurePositions, float carryCapacity, List<List<Direction>> pathsTaken) {
        float weight = 0f;
        float value = 0f;

        Coords current = new Coords(0, 0);

        if(!containsKey(current)) {
            throw new IllegalStateException("Error in labyrinth! No valid start! Ask for Prof. Hassanen to help you!");
        }

        Set<Coords> alreadyCollected = new HashSet<Coords>();

        for (Coords treasurePosition : treasurePositions) {
            List<Direction> path = findPath(current, treasurePosition);
            if (path == null) { // not reachable
                continue;
            }

            if (alreadyCollected.contains(treasurePosition)) {
                throw new IllegalStateException("Cannot pick up treasure at coord " + treasurePosition + " twice!");
            }

            final Tile tile = get(treasurePosition);
            if (!(tile instanceof Treasure)) {
                throw new IllegalStateException("Tile at coord " + treasurePosition + " is not a treasure!");
            }

            final Treasure treasure = (Treasure) tile;

            if (weight + treasure.getWeight() > carryCapacity) {
                continue; // too heavy, skip it
            }

            alreadyCollected.add(treasurePosition);
            map.put(treasurePosition, new TileWithCollectedTreasure() {
                @Override
                public Passage getLeft() {
                    return tile.getLeft();
                }

                @Override
                public Passage getRight() {
                    return tile.getRight();
                }

                @Override
                public Passage getUp() {
                    return tile.getUp();
                }

                @Override
                public Passage getDown() {
                    return tile.getDown();
                }

                @Override
                public float getValue() {
                    return treasure.getValue();
                }

                @Override
                public float getWeight() {
                    return treasure.getWeight();
                }
            });
            pathsTaken.add(path);
            weight += treasure.getWeight();
            value += treasure.getValue();
            current = treasurePosition;
        }

        // get out again
        List<Direction> pathOut = findPath(current, new Coords(0, 0));
        if(pathOut == null) {
            throw new IllegalStateException("Error in labyrinth! Expedition is trapped! Ask for Prof. Hassanen to help you!");
        }

        pathsTaken.add(pathOut);

        return value;
    }

    private static abstract class TileWithCollectedTreasure extends Tile implements CollectedTreasure {
    }

    private class PathNode {
        private PathNode parent;
        private Coords coords;
        private Direction direction;

        public PathNode(PathNode parent, Coords coords, Direction direction) {
            this.parent = parent;
            this.coords = coords;
            this.direction = direction;
        }

        public PathNode getParent() {
            return parent;
        }

        public Coords getCoords() {
            return coords;
        }

        public Direction getDirection() {
            return direction;
        }
    }

    private List<Direction> findPath(Coords start, Coords end) {
        if(start.equals(end)) {
            return Collections.emptyList();
        }

        final List<Direction> directions = Arrays.asList(new Left(), new Right(), new Up(), new Down());

        LinkedList<PathNode> stack = new LinkedList<PathNode>();
        stack.addLast(new PathNode(null, start, null));

        PathNode foundPath = null;

        bfs:
        while(!stack.isEmpty()) {
            PathNode node = stack.getFirst();
            Coords currentCoords = node.getCoords();

            tryDirections:
            for(Direction direction : directions) {
                if(!get(currentCoords).getDirection(direction).isOpen()) {
                    continue; // not open passage, skip
                }

                Coords nextCoords = currentCoords.go(direction);

                if(!containsKey(nextCoords)) {
                    throw new IllegalStateException("Labyrinth is broken! Passage leads to void! Ask for Prof. Hassanen to help you!");
                }

                PathNode prior = node;
                while(prior != null) {
                    if(prior.getCoords().equals(nextCoords)) {
                        continue tryDirections; // we do not want to run in circles
                    }
                    prior = prior.getParent();
                }

                PathNode newPath = new PathNode(node, nextCoords, direction);
                if(nextCoords.equals(end)) {
                    foundPath = newPath;
                    break bfs;
                }

                stack.addLast(newPath);
            }
        }

        if(foundPath == null) {
            return null;
        }

        LinkedList<Direction> path = new LinkedList<Direction>();
        while(foundPath != null) {
            if(foundPath.getDirection() != null) {
                path.addLast(foundPath.getDirection());
            }
            foundPath = foundPath.getParent();
        }
        return path;
    }

    public void clearPassages(Map<Coords, Tile> modifiedTiles) {
        for(Coords c : modifiedTiles.keySet()) {
            checkValidUpdate(c, modifiedTiles);
        }
    }

    private void checkValidUpdate(Coords c, Map<Coords, Tile> modifiedTiles) {
        final Left left = new Left();
        final Right right = new Right();
        final Up up = new Up();
        final Down down = new Down();

        checkValidUpdate(c, left, c.go(left), right, modifiedTiles);
        checkValidUpdate(c, right, c.go(right), left, modifiedTiles);
        checkValidUpdate(c, up, c.go(up), down, modifiedTiles);
        checkValidUpdate(c, down, c.go(down), up, modifiedTiles);
    }

    private void checkValidUpdate(Coords base, Direction baseDir, Coords neighbor, Direction neighborDir, Map<Coords, Tile> modifiedTiles) {
        final Tile oldBaseTile = get(base);
        final Tile baseTile = modifiedTiles.get(base);
        final Tile otherTile = modifiedTiles.containsKey(neighbor) ? modifiedTiles.get(neighbor) : get(neighbor);

        if(base == null) {
            throw new IllegalArgumentException("You cannot set the tile at " + base + " to be empty!");
        }

        if(oldBaseTile == null) {
            throw new IllegalArgumentException("You cannot add a new tile at " + base + " to the labyrinth");
        }

        if(oldBaseTile.getDirection(baseDir).isOpen() && !baseTile.getDirection(baseDir).isOpen()) {
            throw new IllegalArgumentException("You can only open up passages! Not close them!");
        }

        if(!(
                (!baseTile.getDirection(baseDir).isOpen() && otherTile == null)
                || baseTile.getDirection(baseDir).isOpen() == otherTile.getDirection(neighborDir).isOpen())) {
            throw new IllegalArgumentException("Tiles do not match at " + base + " and " + neighbor + "! Did you check to open up the passage from both directions?");
        }
    }

    private Map<Coords, Tile> map = new HashMap<Coords, Tile>();
    private Map<Coords, Tile> unmodifiableMap = Collections.unmodifiableMap(map);

    @Override
    public int size() {
        return unmodifiableMap.size();
    }

    @Override
    public boolean isEmpty() {
        return unmodifiableMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return unmodifiableMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return unmodifiableMap.containsValue(value);
    }

    @Override
    public Tile get(Object key) {
        return unmodifiableMap.get(key);
    }

    @Override
    public Tile put(Coords key, Tile value) {
        return unmodifiableMap.put(key, value);
    }

    @Override
    public Tile remove(Object key) {
        return unmodifiableMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends Coords, ? extends Tile> m) {
        unmodifiableMap.putAll(m);
    }

    @Override
    public void clear() {
        unmodifiableMap.clear();
    }

    @Override
    public Set<Coords> keySet() {
        return unmodifiableMap.keySet();
    }

    @Override
    public Collection<Tile> values() {
        return unmodifiableMap.values();
    }

    @Override
    public Set<Entry<Coords, Tile>> entrySet() {
        return unmodifiableMap.entrySet();
    }
}
