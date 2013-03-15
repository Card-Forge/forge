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
        _identifyAndAnalyze(_source);
    }
    
    private void _identifyAndAnalyze(File root) {
        // see if we can figure out the likely identity of the source folder and
        // dispatch to the best analysis subroutine to handle it
        String dirname = root.getName();
        
        if ("res".equals(dirname))               { _analyzeOldResDir(root);          }
        else if ("constructed".equals(dirname))  { _analyzeConstructedDeckDir(root); }
        else if ("draft".equals(dirname))        { _analyzeDraftDeckDir(root);       }
        else if ("plane".equals(dirname) || "planar".equals(dirname)) { _analyzePlanarDeckDir(root); }
        else if ("scheme".equals(dirname))       { _analyzeSchemeDeckDir(root);      }
        else if ("sealed".equals(dirname))       { _analyzeSealedDeckDir(root);      }
        else if (StringUtils.containsIgnoreCase(dirname, "deck")) { _analyzeDecksDir(root);          }
        else if ("gauntlet".equals(dirname))     { _analyzeGauntletDataDir(root);    }
        else if ("layouts".equals(dirname))      { _analyzeLayoutsDir(root);         }
        else if ("pics".equals(dirname))         { _analyzeCardPicsDir(root);        }
        else if ("pics_product".equals(dirname)) { _analyzeProductPicsDir(root);     }
        else if ("preferences".equals(dirname))  { _analyzePreferencesDir(root);     }
        else if ("quest".equals(dirname))        { _analyzeQuestDir(root);           }
        else if (null != Singletons.getModel().getEditions().get(dirname)) { _analyzeCardPicsSetDir(root); }
        else {
            // look at files in directory and make a semi-educated guess based on file extensions
            int numUnhandledFiles = 0;
            for (File file : root.listFiles()) {
                if (_cb.checkCancel()) { return; }

                if (file.isFile()) {
                    String filename = file.getName();
                    if (filename.endsWith(".dck")) {
                        _analyzeDecksDir(root);
                        numUnhandledFiles = 0;
                        break;
                    } else if (filename.endsWith(".dat")) {
                        _analyzeQuestDataDir(root);
                        numUnhandledFiles = 0;
                        break;
                    }
                    
                    ++numUnhandledFiles;
                } else if (file.isDirectory()) {
                    _identifyAndAnalyze(file);
                }
            }
            _numFilesAnalyzed += numUnhandledFiles;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    // pre-profile res dir
    //
    
    private void _analyzeOldResDir(File root) {
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
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                // we don't really expect any files in here, but if we find a .dck file, add it to the unknown list
                String filename = file.getName();
                if (filename.endsWith(".dck")) {
                    File targetFile = new File(filename);
                    _cb.addOp(OpType.UNKNOWN_DECK, file, targetFile);
                }
            }
            
            @Override boolean onDir(File dir) {
                String dirname = dir.getName();
                if ("constructed".equals(dirname)) {
                    _analyzeConstructedDeckDir(dir);
                } else if ("cube".equals(dirname)) {
                    return false;
                } else if ("draft".equals(dirname)) {
                    _analyzeDraftDeckDir(dir);
                } else if ("plane".equals(dirname) || "planar".equals(dirname)) {
                    _analyzePlanarDeckDir(dir);
                } else if ("scheme".equals(dirname)) {
                    _analyzeSchemeDeckDir(dir);
                } else if ("sealed".equals(dirname)) {
                    _analyzeSealedDeckDir(dir);
                } else {
                    _analyzeKnownDeckDir(dir, null, OpType.UNKNOWN_DECK);
                }
                return true;
            }
        });
    }
    
    private void _analyzeConstructedDeckDir(File root) {
        _analyzeKnownDeckDir(root, NewConstants.DECK_CONSTRUCTED_DIR, OpType.CONSTRUCTED_DECK);
    }
    
    private void _analyzeDraftDeckDir(File root) {
        _analyzeKnownDeckDir(root, NewConstants.DECK_DRAFT_DIR, OpType.DRAFT_DECK);
    }
    
    private void _analyzePlanarDeckDir(File root) {
        _analyzeKnownDeckDir(root, NewConstants.DECK_PLANE_DIR, OpType.PLANAR_DECK);
    }
    
    private void _analyzeSchemeDeckDir(File root) {
        _analyzeKnownDeckDir(root, NewConstants.DECK_SCHEME_DIR, OpType.SCHEME_DECK);
    }
    
    private void _analyzeSealedDeckDir(File root) {
        _analyzeKnownDeckDir(root, NewConstants.DECK_SEALED_DIR, OpType.SEALED_DECK);
    }
    
    private void _analyzeKnownDeckDir(File root, final String targetDir, final OpType opType) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                String filename = file.getName();
                if (filename.endsWith(".dck")) {
                    File targetFile = new File(targetDir, filename);
                    _cb.addOp(opType, file, targetFile);
                }
            }
            
            @Override boolean onDir(File dir) {
                // if there's a dir beneath a known directory, assume the same kind of decks are in there
                _analyzeKnownDeckDir(dir, targetDir, opType);
                return true;
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // gauntlet
    //
    
    private void _analyzeGauntletDataDir(File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                // find *.dat files, but exclude LOCKED_*
                String filename = file.getName();
                if (filename.endsWith(".dat") && !filename.startsWith("LOCKED_")) {
                    File targetFile = new File(NewConstants.GAUNTLET_DIR.userPrefLoc, filename);
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
            String filename = c.getImageKey(backFace, artIdx, false) + ".jpg";
            _defaultPicNames.add(filename);
            
            final String oldFilenameBase;
            if (cardRules.getType().isPlane()) {
                oldFilenameBase = _oldCleanString(filename.replace(".jpg", ""));
            } else {
                oldFilenameBase = _oldCleanString(filename.replace(".full.jpg", ""));
            }
            
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
            cardFileNames.add(c.getImageKey(false, c.getArtIndex(), true) + ".jpg");
            if (hasBackFace) {
                cardFileNames.add(c.getImageKey(true, c.getArtIndex(), true) + ".jpg");
            }
        }
    }
    
    Map<String, Set<String>> _cardFileNamesBySet;
    Map<String, String>      _nameUpdates;
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
            
            // planar cards now don't have the ".full" part in their filenames
            _nameUpdates = new HashMap<String, String>();
            Predicate<CardPrinted> predPlanes = new Predicate<CardPrinted>() {
                @Override
                public boolean apply(CardPrinted arg0) {
                    return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                }
            };

            for (CardPrinted c : Iterables.filter(CardDb.variants().getAllCards(), predPlanes)) {
                boolean hasBackFace = null != c.getRules().getPictureOtherSideUrl();
                String baseName = c.getImageKey(false, c.getArtIndex(), true);
                _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                if (hasBackFace) {
                    baseName = c.getImageKey(true, c.getArtIndex(), true);
                    _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                }
            }
        }

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
                if (_nameUpdates.containsKey(filename)) {
                    filename = _nameUpdates.get(filename);
                }
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
        // we don't care about the files in the root dir -- the new files are .png, not the current .jpg ones
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(File dir) {
                if ("booster".equals(dir.getName())) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_BOOSTERS_FILE, NewConstants.CACHE_BOOSTER_PICS_DIR, OpType.QUEST_PIC);
                } else if ("fatpacks".equals(dir.getName())) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_FATPACKS_FILE, NewConstants.CACHE_FATPACK_PICS_DIR, OpType.QUEST_PIC);
                } else if ("precons".equals(dir.getName())) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_PRECONS_FILE, NewConstants.CACHE_PRECON_PICS_DIR, OpType.QUEST_PIC);
                } else if ("tournamentpacks".equals(dir.getName())) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE, NewConstants.CACHE_TOURNAMENTPACK_PICS_DIR, OpType.QUEST_PIC);
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
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                if ("all-prices.txt".equals(file.getName())) {
                    File targetFile = new File(NewConstants.DB_DIR, file.getName());
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.DB_FILE, file, targetFile);
                    }
                }
            }
            
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
    
    private Map<String, Set<String>> _fileNameDb = new HashMap<String, Set<String>>();
    private void _analyzeSimpleListedDir(File root, String listFile, String targetDir, final OpType opType) {
        if (!_fileNameDb.containsKey(listFile)) {
            Set<String> fileNames = new HashSet<String>();
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(listFile)) {
                fileNames.add(nameurl.getLeft());
            }
            _fileNameDb.put(listFile, fileNames);
        }
        
        final Set<String> dbSet = _fileNameDb.get(listFile);
        _analyzeListedDir(root, targetDir, new _ListedAnalyzer() {
            @Override public String map(String filename) { return dbSet.contains(filename) ? filename : null; }
            @Override public OpType getOpType(String filename) { return opType; }
        });
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
                if (null != filename) {
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
