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

import forge.properties.NewConstants;

public class MigrationSourceAnalyzer {
    public static enum OpType {
        CONSTRUCTED_DECK,
        DRAFT_DECK,
        PLANAR_DECK,
        SCHEME_DECK,
        SEALED_DECK,
        UNKNOWN_DECK,
        DEFAULT_CARD_PIC,
        SET_CARD_PIC,
        TOKEN_PIC,
        QUEST_PIC,
        GAUNTLET_DATA,
        QUEST_DATA,
        PREFERENCE_FILE,
        DB_FILE
    }
    
    public static interface AnalysisCallback {
        boolean checkCancel();
        void    addOp(OpType type, File src, File dest);
    }
    
    private final File             _source;
    private final AnalysisCallback _cb;
    private final int              _numFilesToAnalyze;
    
    private int _numFilesAnalyzed;
    
    public MigrationSourceAnalyzer(String source, AnalysisCallback cb) {
        _source = new File(source);
        _cb     = cb;

        System.out.println("counting source files");
        _numFilesToAnalyze = _countFiles(_source);
        System.out.println("done counting source files: " + _numFilesToAnalyze);
    }
    
    public int getNumFilesToAnalyze() { return _numFilesToAnalyze; }
    public int getNumFilesAnalyzed()  { return _numFilesAnalyzed;  }
    
    public void doAnalysis() {
        // TODO: analyze source path tree and populate operation sets
        // ensure we ignore data that is already in the destination directory

        _analyzeResDir(_source);
    }
    
    private void _analyzeResDir(File resRoot) {
        for (File file : resRoot.listFiles()) {
            if (_cb.checkCancel()) { return; }

            if ("pics".equals(file.getName())) {
                _analyzePicsDir(file);
            }
            
            // ignore other files
            if (file.isFile()) {
                ++_numFilesAnalyzed;
            }
            if (file.isDirectory()) {
                _numFilesAnalyzed += _countFiles(file);
            }
        }
    }

    private void _analyzePicsDir(File picsRoot) {
        System.out.println("found pics dir: " + picsRoot);
        for (File file : picsRoot.listFiles()) {
            if (_cb.checkCancel()) { return; }
            
            System.out.println("analyzing dir entry: " + file.getAbsolutePath());
            if (file.isFile()) {
                ++_numFilesAnalyzed;
                // TODO: correct filename
                _cb.addOp(OpType.DEFAULT_CARD_PIC, file, new File(NewConstants.CACHE_CARD_PICS_DIR, file.getName()));
            }
            if (file.isDirectory()) {
                // skip set pics for now
                _numFilesAnalyzed += _countFiles(file);
            }
        }
    }
    
    private int _countFiles(File directory) {
        int count = 0;
        for (File file : directory.listFiles()) {
            if (_cb.checkCancel()) { return 0; }
            
            if (file.isFile()) {
                ++count;
            }
            if (file.isDirectory()) {
                count += _countFiles(file);
            }
        }
        return count;
    }
}
