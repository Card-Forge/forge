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
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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
import forge.util.FileUtil;

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
    
    private final Set<File> _unmappableFiles = new TreeSet<File>();
    
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
        // TODO: determine if this is really the res dir
        _analyzeResDir(_source);
    }
    
    //////////////////////////////////////////////////////////////////////////
    // pre-profile res dir
    //
    
    private void _analyzeResDir(File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(File dir) {
                String dirname = dir.getName();
                if ("decks".equals(dirname)) {
                    _analyzeDecksDir(dir);
                } else if ("gauntlet".equals(dirname)) {
                    _analyzeGauntletDataDir(dir);
                } else if ("layouts".equals(dirname)) {
                    _analyzeLayoutsDir(dir);
                } else if ("pics".equals(dirname)) {
                    _analyzeCardPicsDir(dir);
                } else if ("pics_product".equals(dirname)) {
                    _analyzeProductPicsDir(dir);
                } else if ("preferences".equals(dirname)) {
                    _analyzePreferencesDir(dir);
                } else if ("quest".equals(dirname)) {
                    _analyzeQuestDir(dir);
                } else {
                    return false;
                }
                return true;
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // decks
    //
    
    private void _analyzeDecksDir(File root) {
        System.out.println("analyzing decks directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
            }
            @Override boolean onDir(File dir) {
                return false;
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // gauntlet
    //
    
    private void _analyzeGauntletDataDir(File root) {
        System.out.println("analyzing gauntlet data directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                // find *.dat files, but exclude LOCKED_*
                String filename = file.getName();
                if (filename.endsWith(".dat") && !filename.startsWith("LOCKED_")) {
                    File targetFile = new File(NewConstants.GAUNTLET_DIR.userPrefLoc, file.getName());
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.GAUNTLET_DATA, file, targetFile);
                    }
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // gauntlet
    //
    
    private void _analyzeLayoutsDir(File root) {
        System.out.println("analyzing layouts directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                // find *_preferred.xml files
                String filename = file.getName();
                if (filename.endsWith("_preferred.xml")) {
                    File targetFile = new File(NewConstants.USER_PREFS_DIR, file.getName().replace("_preferred", ""));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.PREFERENCE_FILE, file, targetFile);
                    }
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // default card pics
    //
    
    private static String _oldCleanString(String in) {
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
        _analyzeListedDir(root, NewConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                if (_defaultPicOldNameToCurrentName.containsKey(filename)) {
                    return _defaultPicOldNameToCurrentName.get(filename);
                }
                return _defaultPicNames.contains(filename) ? filename : null;
            }
            
            @Override public OpType getOpType(String filename) { return OpType.DEFAULT_CARD_PIC; }
            
            @Override boolean onDir(File dir) {
                if ("icons".equals(dir.getName())) {
                    _analyzeIconsPicsDir(dir);
                } else if ("tokens".equals(dir.getName())) {
                    _analyzeTokenPicsDir(dir);
                } else {
                    _analyzeCardPicsSetDir(dir);
                }
                return true;
            } 
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // set card pics
    //
    
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
        
        final String editionCode2 = edition.getCode2();
        final Set<String> validFilenames = _cardFileNamesBySet.get(editionCode2);
        _analyzeListedDir(root, NewConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                filename = editionCode2 + "/" + filename;
                return validFilenames.contains(filename) ? filename : null;
            }
            @Override public OpType getOpType(String filename) { return OpType.SET_CARD_PIC; }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // other image dirs
    //

    Set<String> _iconFileNames;
    private void _analyzeIconsPicsDir(File root) {
        if (null == _iconFileNames) {
            _iconFileNames = new HashSet<String>();
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE)) {
                _iconFileNames.add(nameurl.getLeft());
            }
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE)) {
                _iconFileNames.add(nameurl.getLeft());
            }
        }
        
        System.out.println("analyzing icon pics directory: " + root);
        _analyzeListedDir(root, NewConstants.CACHE_ICON_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) { return _iconFileNames.contains(filename) ? filename : null; }
            @Override public OpType getOpType(String filename) { return OpType.QUEST_PIC; }
        });
    }
    
    Set<String> _tokenFileNames;
    Set<String> _questTokenFileNames;
    private void _analyzeTokenPicsDir(File root) {
        if (null == _tokenFileNames) {
            _tokenFileNames = new HashSet<String>();
            _questTokenFileNames = new HashSet<String>();
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_TOKENS_FILE)) {
                _tokenFileNames.add(nameurl.getLeft());
            }
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_TOKENS_FILE)) {
                _questTokenFileNames.add(nameurl.getLeft());
            }
        }
        
        System.out.println("analyzing token pics directory: " + root);
        _analyzeListedDir(root, NewConstants.CACHE_TOKEN_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                return (_questTokenFileNames.contains(filename) || _tokenFileNames.contains(filename)) ? filename : null;
            }
            @Override public OpType getOpType(String filename) {
                return _questTokenFileNames.contains(filename) ? OpType.QUEST_PIC : OpType.TOKEN_PIC;
            }
        });
    }
    
    private void _analyzeProductPicsDir(File root) {
        System.out.println("analyzing product pics directory: " + root);
        // we don't care about files in the root dir -- the new files are .png, not the current .jpg ones
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(File dir) {
                if ("booster".equals(dir.getName())) {
                } else if ("fatpacks".equals(dir.getName())) {
                } else if ("precons".equals(dir.getName())) {
                } else if ("tournamentpacks".equals(dir.getName())) {
                } else {
                    return false;
                }
                return true;
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // preferences
    //
    
    private void _analyzePreferencesDir(File root) {
        System.out.println("analyzing preferences directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                String filename = file.getName();
                if ("editor.preferences".equals(filename) || "forge.preferences".equals(filename)) {
                    File targetFile = new File(NewConstants.USER_PREFS_DIR, file.getName());
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.PREFERENCE_FILE, file, targetFile);
                    }
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // quest data
    //
    
    private void _analyzeQuestDir(File root) {
        System.out.println("analyzing quest directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(File dir) {
                if ("data".equals(dir.getName())) {
                    _analyzeQuestDataDir(dir);
                    return true;
                }
                return false;
            }
        });
    }
    
    private void _analyzeQuestDataDir(File root) {
        System.out.println("analyzing quest data directory: " + root);
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                if (file.getName().endsWith(".dat")) {
                    File targetFile = new File(NewConstants.QUEST_SAVE_DIR, file.getName());
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.QUEST_DATA, file, targetFile);
                    }
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // utility functions
    //
    
    private class _Analyzer {
        void onFile(File file) { }

        // returns whether the directory has been handled
        boolean onDir(File dir) { return false; } 
    }
    
    private void _analyzeDir(File root, _Analyzer analyzer) {
        for (File file : root.listFiles()) {
            if (_cb.checkCancel()) { return; }

            if (file.isFile()) {
                ++_numFilesAnalyzed;
                analyzer.onFile(file);
            } else if (file.isDirectory()) {
                if (!analyzer.onDir(file)) {
                    _numFilesAnalyzed += _countFiles(file);
                }
            }
        }
    }
    
    private abstract class _ListedAnalyzer {
        abstract String map(String filename);
        abstract OpType getOpType(String filename);
        
        // returns whether the directory has been handled
        boolean onDir(File dir) { return false; } 
    }
    
    private void _analyzeListedDir(File root, final String targetDir, final _ListedAnalyzer listedAnalyzer) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                String filename = listedAnalyzer.map(file.getName());
                if (null == filename) {
                    System.out.println("skipping umappable pic file: " + file);
                    _unmappableFiles.add(file);
                } else {
                    File targetFile = new File(targetDir, filename);
                    if (!file.equals(targetFile)) {
                        _cb.addOp(listedAnalyzer.getOpType(filename), file, targetFile);
                    }
                }
            }
            
            @Override boolean onDir(File dir) { return listedAnalyzer.onDir(dir); }
        });
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
