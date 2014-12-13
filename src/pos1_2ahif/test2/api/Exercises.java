package pos1_2ahif.test2.api;

import java.io.File;
import java.util.List;

/**
* Created by Florian on 13.12.2014.
*/
public interface Exercises {
    public String getMyName();

    public String getMyExamAccountName();

    public boolean hasAnyTreasure(Labyrinth labyrinth);

    public List<Coords> getTreasuresOrderedByValue(Labyrinth labyrinth);

    public List<Coords> getTreasuresOrderedByValuePerWeight(Labyrinth labyrinth);

    public void clearPassagesAlongPath(Labyrinth labyrinth, List<Direction> path);

    public List<Direction> joinPaths(List<List<Direction>> paths);

    public void printPlanForTreasureHunt(Labyrinth labyrinth, List<Direction> path, File file);
}
