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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.EditionCollection;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
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
    
    private void _analyzeResDir(File root) {
        for (File file : root.listFiles()) {
            if (_cb.checkCancel()) { return; }

            if ("pics".equals(file.getName())) {
                _analyzeCardPicsDir(file);
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
    private void _analyzeCardPicsDir(File root) {
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
        
        System.out.println("analyzing default card pics directory: " + root);
        for (File file : root.listFiles()) {
            if (_cb.checkCancel()) { return; }

            if (file.isFile()) {
                ++_numFilesAnalyzed;
                String fileName = file.getName();
                if (_defaultPicOldNameToCurrentName.containsKey(fileName)) {
                    fileName = _defaultPicOldNameToCurrentName.get(fileName);
                } else if (!_defaultPicNames.contains(fileName)) {
                    // TODO: track the unmappables and prompt to delete them at the end
                    System.out.println("skipping umappable default pic file: " + file);
                    continue;
                }
                
                File targetFile = new File(NewConstants.CACHE_CARD_PICS_DIR, fileName);
                if (!file.equals(targetFile)) {
                    _cb.addOp(OpType.DEFAULT_CARD_PIC, file, targetFile);
                }
            } else if (file.isDirectory()) {
                if ("icons".equals(file.getName())) {
                    _analyzeIconsPicsDir(file);
                } else if ("tokens".equals(file.getName())) {
                    _analyzeTokenPicsDir(file);
                } else {
                    _analyzeCardPicsSetDir(file);
                }
            }
        }
    }

    private static void _addSetCards(Set<String> cardFileNames, Iterable<CardPrinted> library, Predicate<CardPrinted> filter) {
        for (CardPrinted c : Iterables.filter(library, filter)) {
            boolean hasBackFace = null != c.getRules().getPictureOtherSideUrl();
            cardFileNames.add(c.getImageFilename(false, c.getArtIndex(), true) + ".jpg");
            if (hasBackFace) {
                cardFileNames.add(c.getImageFilename(true, c.getArtIndex(), true) + ".jpg");
            }
        }
    }
    
    Map<String, Set<String>> _cardFileNamesBySet;
    private void _analyzeCardPicsSetDir(File root) {
        if (null == _cardFileNamesBySet) {
            _cardFileNamesBySet = new HashMap<String, Set<String>>();
            for (CardEdition ce : Singletons.getModel().getEditions()) {
                Set<String> cardFileNames = new HashSet<String>();
                Predicate<CardPrinted> filter = IPaperCard.Predicates.printedInSets(ce.getCode());
                _addSetCards(cardFileNames, CardDb.instance().getAllCards(), filter);
                _addSetCards(cardFileNames, CardDb.variants().getAllCards(), filter);
                _cardFileNamesBySet.put(ce.getCode2(), cardFileNames);
            }
        }

        System.out.println("analyzing set card pics directory: " + root);
        EditionCollection editions = Singletons.getModel().getEditions();
        String editionCode = root.getName();
        CardEdition edition = editions.get(editionCode);
        if (null == edition) {
            // not a valid set name, skip
            System.out.println("skipping umappable set directory: " + root);
            _numFilesAnalyzed += _countFiles(root);
            return;
        }
        editionCode = edition.getCode2();
        
        Set<String> validFilenames = _cardFileNamesBySet.get(editionCode);
        for (File file : root.listFiles()) {
            if ( _cb.checkCancel()) { return; }

            if (file.isFile()) {
                ++_numFilesAnalyzed;
                if (validFilenames.contains(editionCode + "/" + file.getName())) {
                    File targetFile = new File(NewConstants.CACHE_CARD_PICS_DIR, editionCode + "/" + file.getName());
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.SET_CARD_PIC, file, targetFile);
                    }
                } else {
                    System.out.println("skipping umappable set pic file: " + file);
                }
            } else if (file.isDirectory()) {
                System.out.println("skipping umappable subdirectory: " + file);
                _numFilesAnalyzed += _countFiles(file);
            }
        }
    }

    private void _analyzeIconsPicsDir(File root) {
        // TODO: implement
        _numFilesAnalyzed += _countFiles(root);
    }
    
    private void _analyzeTokenPicsDir(File root) {
        // TODO: implement
        _numFilesAnalyzed += _countFiles(root);
    }
    
    private int _countFiles(File root) {
        int count = 0;
        for (File file : root.listFiles()) {
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
