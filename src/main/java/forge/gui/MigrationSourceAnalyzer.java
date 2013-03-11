/*
 * Forge: Play Magic: the Gathering.
 * Copyright (c) 2013  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

public class MigrationSourceAnalyzer {
    public static enum OpType {
        CONSTRUCTED_DECK,
        DRAFT_DECK,
        PLANAR_DECK,
        SCHEME_DECK,
        SEALED_DECK,
        UNKNOWN_DECK,
        GAUNTLET_DATA,
        QUEST_DATA,
        PREFERENCE_FILE
    }
    
    private final String                             _source;
    private final Map<OpType, Set<Pair<File, File>>> _opDb;
    private final Callable<Boolean>                  _checkCancel;
    
    private int _numFilesToAnalyze;
    private int _numFilesAnalyzed;
    
    public MigrationSourceAnalyzer(String source, Map<OpType, Set<Pair<File, File>>> opDb, Callable<Boolean> checkCancel) {
        _source      = source;
        _opDb        = opDb;
        _checkCancel = checkCancel;
    }
    
    public int getNumFilesToAnalyze() { return _numFilesToAnalyze; }
    public int getNumFilesAnalyzed()  { return _numFilesAnalyzed;  }
    
    public void doAnalysis() {
        // TODO: analyze source path tree and populate operation sets
        // ensure we ignore data that is already in the destination directory
    }
}
