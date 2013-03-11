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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardRules;
import forge.item.CardDb;
import forge.item.CardPrinted;
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

        _numFilesToAnalyze = _countFiles(_source);
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
            } else  if (file.isDirectory()) {
                _numFilesAnalyzed += _countFiles(file);
            }
        }
    }

    private static String _oldCleanString(final String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == ' ') || (c == '-')) {
                out.append('_');
            } else if (Character.isLetterOrDigit(c) || (c == '_')) {
                out.append(c);
            }
        }
        return out.toString().toLowerCase();
    }
    
    private void _addDefaultPicNames(CardPrinted c, boolean backFace) {
        CardRules cardRules = c.getRules();
        String urls = backFace ? cardRules.getPictureOtherSideUrl() : cardRules.getPictureUrl();
        if (StringUtils.isEmpty(urls)) { return; }

        int numPics = urls.split("\\\\").length;
        for (int artIdx = 0; numPics > artIdx; ++artIdx) {
            String filename = c.getImageFilename(backFace, artIdx, false) + ".jpg";
            _defaultPicNames.add(filename);
            
            String oldFilenameBase = _oldCleanString(filename.replace(".full.jpg", ""));
            if (0 == artIdx) {
                // remove trailing "1" from first art index
                String oldFilename = oldFilenameBase.replaceAll("1$", "") + ".jpg";
                _defaultPicOldNameToCurrentName.put(oldFilename, filename);
            } else {
                // offset art indices by one
                String oldFilename = oldFilenameBase.replaceAll("[0-9]+$", String.valueOf(artIdx)) + ".jpg";
                _defaultPicOldNameToCurrentName.put(oldFilename, filename);
            }
        }
    }
    
    private Set<String>         _defaultPicNames;
    private Map<String, String> _defaultPicOldNameToCurrentName;
    private void _analyzePicsDir(File picsRoot) {
        if (null == _defaultPicNames) {
            // build structures
            _defaultPicNames = new HashSet<String>();
            _defaultPicOldNameToCurrentName = new HashMap<String, String>();

            for (CardPrinted c : CardDb.instance().getUniqueCards()) {
                _addDefaultPicNames(c, false);
                _addDefaultPicNames(c, true);
            }
            
            for (CardPrinted c : CardDb.variants().getUniqueCards()) {
                _addDefaultPicNames(c, false);
                _addDefaultPicNames(c, true);
            }
        }
        
        for (File file : picsRoot.listFiles()) {
            if (_cb.checkCancel()) { return; }
            
            if (file.isFile()) {
                ++_numFilesAnalyzed;
                String fileName = file.getName();
                if (_defaultPicOldNameToCurrentName.containsKey(fileName)) {
                    fileName = _defaultPicOldNameToCurrentName.get(fileName);
                } else if (!_defaultPicNames.contains(fileName)) {
                    System.out.println("skipping umappable pic file: " + file);
                    continue;
                }
                _cb.addOp(OpType.DEFAULT_CARD_PIC, file, new File(NewConstants.CACHE_CARD_PICS_DIR, fileName));
            } else  if (file.isDirectory()) {
                _analyzePicsSetDir(file);
            }
        }
    }
    
    private void _analyzePicsSetDir(File setRoot) {
        // if not a valid set name, skip
        _numFilesAnalyzed += _countFiles(setRoot);
    }
    
    private int _countFiles(File directory) {
        int count = 0;
        for (File file : directory.listFiles()) {
            if (_cb.checkCancel()) { return 0; }
            
            if (file.isFile()) {
                ++count;
            } else if (file.isDirectory()) {
                count += _countFiles(file);
            }
        }
        return count;
    }
}
