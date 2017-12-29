package forge.puzzle;

import com.google.common.collect.Lists;
import forge.properties.ForgeConstants;
import forge.util.FileSection;
import forge.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PuzzleIO {

    public static final String TXF_PROMPT = "[New Puzzle]";
    public static final String SUFFIX_DATA = ".pzl";

    public static ArrayList<Puzzle> loadPuzzles() {
        String[] pList;
        // get list of puzzles
        final File pFolder = new File(ForgeConstants.PUZZLE_DIR);
        if (!pFolder.exists()) {
            throw new RuntimeException("Puzzles : folder not found -- folder is " + pFolder.getAbsolutePath());
        }

        if (!pFolder.isDirectory()) {
            throw new RuntimeException("Puzzles : not a folder -- " + pFolder.getAbsolutePath());
        }

        pList = pFolder.list();

        ArrayList<Puzzle> puzzles = Lists.newArrayList();
        for (final String element : pList) {
            if (element.endsWith(SUFFIX_DATA)) {
                final List<String> pfData = FileUtil.readFile(ForgeConstants.PUZZLE_DIR + element);
                puzzles.add(new Puzzle(parsePuzzleSections(pfData)));
            }
        }
        return puzzles;
    }

    public static final Map<String, List<String>> parsePuzzleSections(List<String> pfData) {
        return FileSection.parseSections(pfData);
    }


    public static File getPuzzleFile(final String name) {
        return new File(ForgeConstants.PUZZLE_DIR, name + SUFFIX_DATA);
    }
}
