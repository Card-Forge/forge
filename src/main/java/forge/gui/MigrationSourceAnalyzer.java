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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ImageCache;
import forge.Singletons;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardSplitType;
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
        
        if ("res".equalsIgnoreCase(dirname))               { _analyzeOldResDir(root);          }
        else if ("constructed".equalsIgnoreCase(dirname))  { _analyzeConstructedDeckDir(root); }
        else if ("draft".equalsIgnoreCase(dirname))        { _analyzeDraftDeckDir(root);       }
        else if ("plane".equalsIgnoreCase(dirname) || "planar".equalsIgnoreCase(dirname)) { _analyzePlanarDeckDir(root); }
        else if ("scheme".equalsIgnoreCase(dirname))       { _analyzeSchemeDeckDir(root);      }
        else if ("sealed".equalsIgnoreCase(dirname))       { _analyzeSealedDeckDir(root);      }
        else if (StringUtils.containsIgnoreCase(dirname, "deck")) { _analyzeDecksDir(root);    }
        else if ("gauntlet".equalsIgnoreCase(dirname))     { _analyzeGauntletDataDir(root);    }
        else if ("layouts".equalsIgnoreCase(dirname))      { _analyzeLayoutsDir(root);         }
        else if ("pics".equalsIgnoreCase(dirname))         { _analyzeCardPicsDir(root);        }
        else if ("pics_product".equalsIgnoreCase(dirname)) { _analyzeProductPicsDir(root);     }
        else if ("preferences".equalsIgnoreCase(dirname))  { _analyzePreferencesDir(root);     }
        else if ("quest".equalsIgnoreCase(dirname))        { _analyzeQuestDir(root);           }
        else if (null != Singletons.getModel().getEditions().get(dirname)) { _analyzeCardPicsSetDir(root); }
        else {
            // look at files in directory and make a semi-educated guess based on file extensions
            int numUnhandledFiles = 0;
            for (File file : root.listFiles()) {
                if (_cb.checkCancel()) { return; }

                if (file.isFile()) {
                    String filename = file.getName();
                    if (StringUtils.endsWithIgnoreCase(filename, ".dck")) {
                        _analyzeDecksDir(root);
                        numUnhandledFiles = 0;
                        break;
                    } else if (StringUtils.endsWithIgnoreCase(filename, ".jpg")) {
                        _analyzeCardPicsDir(root);
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
                if ("decks".equalsIgnoreCase(dirname)) {
                    _analyzeDecksDir(dir);
                } else if ("gauntlet".equalsIgnoreCase(dirname)) {
                    _analyzeGauntletDataDir(dir);
                } else if ("layouts".equalsIgnoreCase(dirname)) {
                    _analyzeLayoutsDir(dir);
                } else if ("pics".equalsIgnoreCase(dirname)) {
                    _analyzeCardPicsDir(dir);
                } else if ("pics_product".equalsIgnoreCase(dirname)) {
                    _analyzeProductPicsDir(dir);
                } else if ("preferences".equalsIgnoreCase(dirname)) {
                    _analyzePreferencesDir(dir);
                } else if ("quest".equalsIgnoreCase(dirname)) {
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
                if (StringUtils.endsWithIgnoreCase(filename, ".dck")) {
                    File targetFile = new File(_lcaseExt(filename));
                    _cb.addOp(OpType.UNKNOWN_DECK, file, targetFile);
                }
            }
            
            @Override boolean onDir(File dir) {
                String dirname = dir.getName();
                if ("constructed".equalsIgnoreCase(dirname)) {
                    _analyzeConstructedDeckDir(dir);
                } else if ("cube".equalsIgnoreCase(dirname)) {
                    return false;
                } else if ("draft".equalsIgnoreCase(dirname)) {
                    _analyzeDraftDeckDir(dir);
                } else if ("plane".equalsIgnoreCase(dirname) || "planar".equalsIgnoreCase(dirname)) {
                    _analyzePlanarDeckDir(dir);
                } else if ("scheme".equalsIgnoreCase(dirname)) {
                    _analyzeSchemeDeckDir(dir);
                } else if ("sealed".equalsIgnoreCase(dirname)) {
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
                if (StringUtils.endsWithIgnoreCase(filename, ".dck")) {
                    File targetFile = new File(targetDir, _lcaseExt(filename));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(opType, file, targetFile);
                    }
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
                if (StringUtils.endsWithIgnoreCase(filename, ".dat") && !filename.startsWith("LOCKED_")) {
                    File targetFile = new File(NewConstants.GAUNTLET_DIR.userPrefLoc, _lcaseExt(filename));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.GAUNTLET_DATA, file, targetFile);
                    }
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // layouts
    //
    
    private void _analyzeLayoutsDir(File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(File file) {
                // find *_preferred.xml files
                String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, "_preferred.xml")) {
                    File targetFile = new File(NewConstants.USER_PREFS_DIR,
                            file.getName().toLowerCase(Locale.ENGLISH).replace("_preferred", ""));
                    _cb.addOp(OpType.PREFERENCE_FILE, file, targetFile);
                }
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////
    // default card pics
    //
    
    private static String _oldCleanString(String in) {
        final StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if ((c == ' ') || (c == '-')) {
                out.append('_');
            } else if (Character.isLetterOrDigit(c) || (c == '_')) {
                out.append(c);
            }
        }
        
        // usually we would want to pass Locale.ENGLISH to the toLowerCase() method to prevent unintentional
        // character mangling on some system locales, but we want to replicate the old code here exactly
        return out.toString().toLowerCase();
    }
    
    private void _addDefaultPicNames(CardPrinted c, boolean backFace) {
        CardRules card = c.getRules();
        String urls = backFace ? card.getPictureOtherSideUrl() : card.getPictureUrl();
        if (StringUtils.isEmpty(urls)) { return; }

        int numPics = 1 + StringUtils.countMatches(urls, "\\");
        if ( c.getArtIndex() >= numPics )
            return;

        String filenameBase = ImageCache.getImageKey(c, backFace, false);
        String filename = filenameBase + ".jpg";
        boolean alreadyHadIt = null != _defaultPicNames.put(filename, filename);
        if ( alreadyHadIt ) return;
        
        // Do you shift artIndex by one here?
        String newLastSymbol = 0 == c.getArtIndex() ? "" : String.valueOf(c.getArtIndex() /* + 1 */);
        String oldFilename = _oldCleanString(filenameBase.replaceAll("[0-9]?(\\.full)?$", "")) + newLastSymbol + ".jpg";
        //if ( numPics > 1 )
        //System.out.printf("Will move %s -> %s%n", oldFilename, filename);
        _defaultPicOldNameToCurrentName.put(oldFilename, filename);
    }

    
    private Map<String, String> _defaultPicNames;
    private Map<String, String> _defaultPicOldNameToCurrentName;
    private void _analyzeCardPicsDir(File root) {
        if (null == _defaultPicNames) {
            _defaultPicNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            _defaultPicOldNameToCurrentName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

            for (CardPrinted c : CardDb.instance().getAllCards()) {
                _addDefaultPicNames(c, false);
                if ( c.getRules().getSplitType() == CardSplitType.Transform)
                    _addDefaultPicNames(c, true);
            }
            
            for (CardPrinted c : CardDb.variants().getAllCards()) {
                _addDefaultPicNames(c, false);
                // variants never have backfaces
            }
        }
        
        _analyzeListedDir(root, NewConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                if (_defaultPicOldNameToCurrentName.containsKey(filename)) {
                    return _defaultPicOldNameToCurrentName.get(filename);
                }
                return _defaultPicNames.get(filename);
            }
            
            @Override public OpType getOpType(String filename) { return OpType.DEFAULT_CARD_PIC; }
            
            @Override boolean onDir(File dir) {
                if ("icons".equalsIgnoreCase(dir.getName())) {
                    _analyzeIconsPicsDir(dir);
                } else if ("tokens".equalsIgnoreCase(dir.getName())) {
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
    
    private static void _addSetCards(Map<String, String> cardFileNames, Iterable<CardPrinted> library, Predicate<CardPrinted> filter) {
        for (CardPrinted c : Iterables.filter(library, filter)) {
            boolean hasBackFacePicture = null != c.getRules().getPictureOtherSideUrl();
            String filename = ImageCache.getImageKey(c, false, true) + ".jpg";
            cardFileNames.put(filename, filename);
            if (hasBackFacePicture) {
                filename = ImageCache.getImageKey(c, true, true) + ".jpg";
                cardFileNames.put(filename, filename);
            }
        }
    }
    
    Map<String, Map<String, String>> _cardFileNamesBySet;
    Map<String, String>              _nameUpdates;
    private void _analyzeCardPicsSetDir(File root) {
        if (null == _cardFileNamesBySet) {
            _cardFileNamesBySet = new TreeMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
            for (CardEdition ce : Singletons.getModel().getEditions()) {
                Map<String, String> cardFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                Predicate<CardPrinted> filter = IPaperCard.Predicates.printedInSets(ce.getCode());
                _addSetCards(cardFileNames, CardDb.instance().getAllCards(), filter);
                _addSetCards(cardFileNames, CardDb.variants().getAllCards(), filter);
                _cardFileNamesBySet.put(ce.getCode2(), cardFileNames);
            }
            
            // planar cards now don't have the ".full" part in their filenames
            _nameUpdates = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            Predicate<CardPrinted> predPlanes = new Predicate<CardPrinted>() {
                @Override
                public boolean apply(CardPrinted arg0) {
                    return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                }
            };

            for (CardPrinted c : Iterables.filter(CardDb.variants().getAllCards(), predPlanes)) {
                boolean hasBackFacePciture = null != c.getRules().getPictureOtherSideUrl();
                String baseName = ImageCache.getImageKey(c,false, true);
                _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                if (hasBackFacePciture) {
                    baseName = ImageCache.getImageKey(c, true, true);
                    _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                }
            }
        }

        EditionCollection editions = Singletons.getModel().getEditions();
        String editionCode = root.getName();
        CardEdition edition = editions.get(editionCode);
        if (null == edition) {
            // not a valid set name, skip
            _numFilesAnalyzed += _countFiles(root);
            return;
        }
        
        final String editionCode2 = edition.getCode2();
        final Map<String, String> validFilenames = _cardFileNamesBySet.get(editionCode2);
        _analyzeListedDir(root, NewConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                filename = editionCode2 + "/" + filename;
                if (_nameUpdates.containsKey(filename)) {
                    filename = _nameUpdates.get(filename);
                }
                return validFilenames.containsKey(filename) ? validFilenames.get(filename) : null;
            }
            @Override public OpType getOpType(String filename) { return OpType.SET_CARD_PIC; }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // other image dirs
    //

    Map<String, String> _iconFileNames;
    private void _analyzeIconsPicsDir(File root) {
        if (null == _iconFileNames) {
            _iconFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE)) {
                _iconFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE)) {
                _iconFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
        }

        _analyzeListedDir(root, NewConstants.CACHE_ICON_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) { return _iconFileNames.containsKey(filename) ? _iconFileNames.get(filename) : null; }
            @Override public OpType getOpType(String filename) { return OpType.QUEST_PIC; }
        });
    }
    
    Map<String, String> _tokenFileNames;
    Map<String, String> _questTokenFileNames;
    private void _analyzeTokenPicsDir(File root) {
        if (null == _tokenFileNames) {
            _tokenFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            _questTokenFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_TOKENS_FILE)) {
                _tokenFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(NewConstants.IMAGE_LIST_QUEST_TOKENS_FILE)) {
                _questTokenFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
        }
        
        _analyzeListedDir(root, NewConstants.CACHE_TOKEN_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                if (_questTokenFileNames.containsKey(filename)) { return _questTokenFileNames.get(filename); }
                if (_tokenFileNames.containsKey(filename))      { return _tokenFileNames.get(filename); }
                return null;
            }
            @Override public OpType getOpType(String filename) {
                return _questTokenFileNames.containsKey(filename) ? OpType.QUEST_PIC : OpType.TOKEN_PIC;
            }
        });
    }
    
    private void _analyzeProductPicsDir(File root) {
        // we don't care about the files in the root dir -- the new files are .png, not the current .jpg ones
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(File dir) {
                String dirName = dir.getName();
                if ("booster".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_BOOSTERS_FILE, NewConstants.CACHE_BOOSTER_PICS_DIR, OpType.QUEST_PIC);
                } else if ("fatpacks".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_FATPACKS_FILE, NewConstants.CACHE_FATPACK_PICS_DIR, OpType.QUEST_PIC);
                } else if ("precons".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, NewConstants.IMAGE_LIST_QUEST_PRECONS_FILE, NewConstants.CACHE_PRECON_PICS_DIR, OpType.QUEST_PIC);
                } else if ("tournamentpacks".equalsIgnoreCase(dirName)) {
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
                if ("editor.preferences".equalsIgnoreCase(filename) || "forge.preferences".equalsIgnoreCase(filename)) {
                    File targetFile = new File(NewConstants.USER_PREFS_DIR, filename.toLowerCase(Locale.ENGLISH));
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
                String filename = file.getName();
                if ("all-prices.txt".equalsIgnoreCase(filename)) {
                    File targetFile = new File(NewConstants.DB_DIR, filename.toLowerCase(Locale.ENGLISH));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.DB_FILE, file, targetFile);
                    }
                }
            }
            @Override boolean onDir(File dir) {
                if ("data".equalsIgnoreCase(dir.getName())) {
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
                String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, ".dat")) {
                    File targetFile = new File(NewConstants.QUEST_SAVE_DIR, _lcaseExt(filename));
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
    
    private Map<String, Map<String, String>> _fileNameDb = new HashMap<String, Map<String, String>>();
    private void _analyzeSimpleListedDir(File root, String listFile, String targetDir, final OpType opType) {
        if (!_fileNameDb.containsKey(listFile)) {
            Map<String, String> fileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (Pair<String, String> nameurl : FileUtil.readNameUrlFile(listFile)) {
                // we use a map instead of a set since we need to match case-insensitively but still map to the correct case 
                fileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            _fileNameDb.put(listFile, fileNames);
        }
        
        final Map<String, String> fileDb = _fileNameDb.get(listFile);
        _analyzeListedDir(root, targetDir, new _ListedAnalyzer() {
            @Override public String map(String filename) { return fileDb.containsKey(filename) ? fileDb.get(filename) : null; }
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
    
    private String _lcaseExt(String filename) {
        int lastDotIdx = filename.lastIndexOf('.');
        if (0 > lastDotIdx) {
            return filename;
        }
        String basename = filename.substring(0, lastDotIdx);
        String ext      = filename.substring(lastDotIdx).toLowerCase(Locale.ENGLISH);
        if (filename.endsWith(ext)) {
            return filename;
        }
        return basename + ext;
    }
}
