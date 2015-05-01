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

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.ImageUtil;

public class ImportSourceAnalyzer {
    public static enum OpType {
        CONSTRUCTED_DECK,
        DRAFT_DECK,
        PLANAR_DECK,
        SCHEME_DECK,
        SEALED_DECK,
        UNKNOWN_DECK,
        DEFAULT_CARD_PIC,
        SET_CARD_PIC,
        POSSIBLE_SET_CARD_PIC,
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

    public ImportSourceAnalyzer(final String source, final AnalysisCallback cb) {
        _source = new File(source);
        _cb     = cb;

        _numFilesToAnalyze = _countFiles(_source);
    }

    public int getNumFilesToAnalyze() { return _numFilesToAnalyze; }
    public int getNumFilesAnalyzed()  { return _numFilesAnalyzed;  }

    public void doAnalysis() {
        _identifyAndAnalyze(_source);
    }

    private void _identifyAndAnalyze(final File root) {
        // see if we can figure out the likely identity of the source folder and
        // dispatch to the best analysis subroutine to handle it
        final String dirname = root.getName();

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
        else if (null != FModel.getMagicDb().getEditions().get(dirname)) { _analyzeCardPicsSetDir(root); }
        else {
            // look at files in directory and make a semi-educated guess based on file extensions
            int numUnhandledFiles = 0;
            for (final File file : root.listFiles()) {
                if (_cb.checkCancel()) { return; }

                if (file.isFile()) {
                    final String filename = file.getName();
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

    private void _analyzeOldResDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(final File dir) {
                final String dirname = dir.getName();
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

    private void _analyzeDecksDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                // we don't really expect any files in here, but if we find a .dck file, add it to the unknown list
                final String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, ".dck")) {
                    final File targetFile = new File(_lcaseExt(filename));
                    _cb.addOp(OpType.UNKNOWN_DECK, file, targetFile);
                }
            }

            @Override boolean onDir(final File dir) {
                final String dirname = dir.getName();
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

    private void _analyzeConstructedDeckDir(final File root) {
        _analyzeKnownDeckDir(root, ForgeConstants.DECK_CONSTRUCTED_DIR, OpType.CONSTRUCTED_DECK);
    }

    private void _analyzeDraftDeckDir(final File root) {
        _analyzeKnownDeckDir(root, ForgeConstants.DECK_DRAFT_DIR, OpType.DRAFT_DECK);
    }

    private void _analyzePlanarDeckDir(final File root) {
        _analyzeKnownDeckDir(root, ForgeConstants.DECK_PLANE_DIR, OpType.PLANAR_DECK);
    }

    private void _analyzeSchemeDeckDir(final File root) {
        _analyzeKnownDeckDir(root, ForgeConstants.DECK_SCHEME_DIR, OpType.SCHEME_DECK);
    }

    private void _analyzeSealedDeckDir(final File root) {
        _analyzeKnownDeckDir(root, ForgeConstants.DECK_SEALED_DIR, OpType.SEALED_DECK);
    }

    private void _analyzeKnownDeckDir(final File root, final String targetDir, final OpType opType) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                final String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, ".dck")) {
                    final File targetFile = new File(targetDir, _lcaseExt(filename));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(opType, file, targetFile);
                    }
                }
            }

            @Override boolean onDir(final File dir) {
                // if there's a dir beneath a known directory, assume the same kind of decks are in there
                _analyzeKnownDeckDir(dir, targetDir, opType);
                return true;
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // gauntlet
    //

    private void _analyzeGauntletDataDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                // find *.dat files, but exclude LOCKED_*
                final String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, ".dat") && !filename.startsWith("LOCKED_")) {
                    final File targetFile = new File(ForgeConstants.GAUNTLET_DIR.userPrefLoc, _lcaseExt(filename));
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

    private void _analyzeLayoutsDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                // find *_preferred.xml files
                final String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, "_preferred.xml")) {
                    final File targetFile = new File(ForgeConstants.USER_PREFS_DIR,
                            file.getName().toLowerCase(Locale.ENGLISH).replace("_preferred", ""));
                    _cb.addOp(OpType.PREFERENCE_FILE, file, targetFile);
                }
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // default card pics
    //

    private static String _oldCleanString(final String in) {
        final StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            final char c = in.charAt(i);
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

    private void _addDefaultPicNames(final PaperCard c, final boolean backFace) {
        final CardRules card = c.getRules();
        final String urls = card.getPictureUrl(backFace);
        if (StringUtils.isEmpty(urls)) { return; }

        final int numPics = 1 + StringUtils.countMatches(urls, "\\");
        if (c.getArtIndex() > numPics) {
            return;
        }

        final String filenameBase = ImageUtil.getImageKey(c, backFace, false);
        final String filename = filenameBase + ".jpg";
        final boolean alreadyHadIt = null != _defaultPicNames.put(filename, filename);
        if ( alreadyHadIt ) {
            return;
        }

        // Do you shift artIndex by one here?
        final String newLastSymbol = 0 == c.getArtIndex() ? "" : String.valueOf(c.getArtIndex() /* + 1 */);
        final String oldFilename = _oldCleanString(filenameBase.replaceAll("[0-9]?(\\.full)?$", "")) + newLastSymbol + ".jpg";
        //if ( numPics > 1 )
        //System.out.printf("Will move %s -> %s%n", oldFilename, filename);
        _defaultPicOldNameToCurrentName.put(oldFilename, filename);
    }


    private Map<String, String> _defaultPicNames;
    private Map<String, String> _defaultPicOldNameToCurrentName;
    private void _analyzeCardPicsDir(final File root) {
        if (null == _defaultPicNames) {
            _defaultPicNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            _defaultPicOldNameToCurrentName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

            for (final PaperCard c : FModel.getMagicDb().getCommonCards().getAllCards()) {
                _addDefaultPicNames(c, false);
                if (ImageUtil.hasBackFacePicture(c)) {
                    _addDefaultPicNames(c, true);
                }
            }

            for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                _addDefaultPicNames(c, false);
                // variants never have backfaces
            }
        }

        _analyzeListedDir(root, ForgeConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(final String filename) {
                if (_defaultPicOldNameToCurrentName.containsKey(filename)) {
                    return _defaultPicOldNameToCurrentName.get(filename);
                }
                return _defaultPicNames.get(filename);
            }

            @Override public OpType getOpType(final String filename) {
                return OpType.DEFAULT_CARD_PIC;
            }

            @Override boolean onDir(final File dir) {
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

    private static void _addSetCards(final Map<String, String> cardFileNames, final Iterable<PaperCard> library, final Predicate<PaperCard> filter) {
        for (final PaperCard c : Iterables.filter(library, filter)) {
            String filename = ImageUtil.getImageKey(c, false, true) + ".jpg";
            cardFileNames.put(filename, filename);
            if (ImageUtil.hasBackFacePicture(c)) {
                filename = ImageUtil.getImageKey(c, true, true) + ".jpg";
                cardFileNames.put(filename, filename);
            }
        }
    }

    Map<String, Map<String, String>> _cardFileNamesBySet;
    Map<String, String>              _nameUpdates;
    private void _analyzeCardPicsSetDir(final File root) {
        if (null == _cardFileNamesBySet) {
            _cardFileNamesBySet = new TreeMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
            for (final CardEdition ce : FModel.getMagicDb().getEditions()) {
                final Map<String, String> cardFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                final Predicate<PaperCard> filter = IPaperCard.Predicates.printedInSet(ce.getCode());
                _addSetCards(cardFileNames, FModel.getMagicDb().getCommonCards().getAllCards(), filter);
                _addSetCards(cardFileNames, FModel.getMagicDb().getVariantCards().getAllCards(), filter);
                _cardFileNamesBySet.put(ce.getCode2(), cardFileNames);
            }

            // planar cards now don't have the ".full" part in their filenames
            _nameUpdates = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            final Predicate<PaperCard> predPlanes = new Predicate<PaperCard>() {
                @Override
                public boolean apply(final PaperCard arg0) {
                    return arg0.getRules().getType().isPlane() || arg0.getRules().getType().isPhenomenon();
                }
            };

            for (final PaperCard c : Iterables.filter(FModel.getMagicDb().getVariantCards().getAllCards(), predPlanes)) {
                String baseName = ImageUtil.getImageKey(c,false, true);
                _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                if (ImageUtil.hasBackFacePicture(c)) {
                    baseName = ImageUtil.getImageKey(c, true, true);
                    _nameUpdates.put(baseName + ".full.jpg", baseName + ".jpg");
                }
            }
        }

        final CardEdition.Collection editions = FModel.getMagicDb().getEditions();
        final String editionCode = root.getName();
        final CardEdition edition = editions.get(editionCode);
        if (null == edition) {
            // not a valid set name, skip
            _numFilesAnalyzed += _countFiles(root);
            return;
        }

        final String editionCode2 = edition.getCode2();
        final Map<String, String> validFilenames = _cardFileNamesBySet.get(editionCode2);
        _analyzeListedDir(root, ForgeConstants.CACHE_CARD_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(String filename) {
                filename = editionCode2 + "/" + filename;
                if (_nameUpdates.containsKey(filename)) {
                    filename = _nameUpdates.get(filename);
                }
                if (validFilenames.containsKey(filename)) {
                    return validFilenames.get(filename);
                } else if (StringUtils.endsWithIgnoreCase(filename, ".jpg")
                        || StringUtils.endsWithIgnoreCase(filename, ".png")) {
                    return filename;
                }
                return null;
            }
            @Override public OpType getOpType(final String filename) {
                return validFilenames.containsKey(filename) ? OpType.SET_CARD_PIC : OpType.POSSIBLE_SET_CARD_PIC;
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // other image dirs
    //

    Map<String, String> _iconFileNames;
    private void _analyzeIconsPicsDir(final File root) {
        if (null == _iconFileNames) {
            _iconFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (final Pair<String, String> nameurl : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_QUEST_OPPONENT_ICONS_FILE)) {
                _iconFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            for (final Pair<String, String> nameurl : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_QUEST_PET_SHOP_ICONS_FILE)) {
                _iconFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
        }

        _analyzeListedDir(root, ForgeConstants.CACHE_ICON_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(final String filename) { return _iconFileNames.containsKey(filename) ? _iconFileNames.get(filename) : null; }
            @Override public OpType getOpType(final String filename) { return OpType.QUEST_PIC; }
        });
    }

    Map<String, String> _tokenFileNames;
    Map<String, String> _questTokenFileNames;
    private void _analyzeTokenPicsDir(final File root) {
        if (null == _tokenFileNames) {
            _tokenFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            _questTokenFileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (final Pair<String, String> nameurl : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_TOKENS_FILE)) {
                _tokenFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            for (final Pair<String, String> nameurl : FileUtil.readNameUrlFile(ForgeConstants.IMAGE_LIST_QUEST_TOKENS_FILE)) {
                _questTokenFileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
        }

        _analyzeListedDir(root, ForgeConstants.CACHE_TOKEN_PICS_DIR, new _ListedAnalyzer() {
            @Override public String map(final String filename) {
                if (_questTokenFileNames.containsKey(filename)) { return _questTokenFileNames.get(filename); }
                if (_tokenFileNames.containsKey(filename))      { return _tokenFileNames.get(filename); }
                return null;
            }
            @Override public OpType getOpType(final String filename) {
                return _questTokenFileNames.containsKey(filename) ? OpType.QUEST_PIC : OpType.TOKEN_PIC;
            }
        });
    }

    private void _analyzeProductPicsDir(final File root) {
        // we don't care about the files in the root dir -- the new booster files are .png, not the current .jpg ones
        _analyzeDir(root, new _Analyzer() {
            @Override boolean onDir(final File dir) {
                final String dirName = dir.getName();
                if ("booster".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, ForgeConstants.IMAGE_LIST_QUEST_BOOSTERS_FILE, ForgeConstants.CACHE_BOOSTER_PICS_DIR, OpType.QUEST_PIC);
                } else if ("fatpacks".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, ForgeConstants.IMAGE_LIST_QUEST_FATPACKS_FILE, ForgeConstants.CACHE_FATPACK_PICS_DIR, OpType.QUEST_PIC);
                } else if ("boosterboxes".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, ForgeConstants.IMAGE_LIST_QUEST_BOOSTERBOXES_FILE, ForgeConstants.CACHE_BOOSTERBOX_PICS_DIR, OpType.QUEST_PIC);
                } else if ("precons".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, ForgeConstants.IMAGE_LIST_QUEST_PRECONS_FILE, ForgeConstants.CACHE_PRECON_PICS_DIR, OpType.QUEST_PIC);
                } else if ("tournamentpacks".equalsIgnoreCase(dirName)) {
                    _analyzeSimpleListedDir(dir, ForgeConstants.IMAGE_LIST_QUEST_TOURNAMENTPACKS_FILE, ForgeConstants.CACHE_TOURNAMENTPACK_PICS_DIR, OpType.QUEST_PIC);
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

    private void _analyzePreferencesDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                final String filename = file.getName();
                if ("editor.preferences".equalsIgnoreCase(filename) || "forge.preferences".equalsIgnoreCase(filename)) {
                    final File targetFile = new File(ForgeConstants.USER_PREFS_DIR, filename.toLowerCase(Locale.ENGLISH));
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

    private void _analyzeQuestDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                final String filename = file.getName();
                if ("all-prices.txt".equalsIgnoreCase(filename)) {
                    final File targetFile = new File(ForgeConstants.DB_DIR, filename.toLowerCase(Locale.ENGLISH));
                    if (!file.equals(targetFile)) {
                        _cb.addOp(OpType.DB_FILE, file, targetFile);
                    }
                }
            }
            @Override boolean onDir(final File dir) {
                if ("data".equalsIgnoreCase(dir.getName())) {
                    _analyzeQuestDataDir(dir);
                    return true;
                }
                return false;
            }
        });
    }

    private void _analyzeQuestDataDir(final File root) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                final String filename = file.getName();
                if (StringUtils.endsWithIgnoreCase(filename, ".dat")) {
                    final File targetFile = new File(ForgeConstants.QUEST_SAVE_DIR, _lcaseExt(filename));
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
        void onFile(final File file) { }

        // returns whether the directory has been handled
        boolean onDir(final File dir) { return false; }
    }

    private void _analyzeDir(final File root, final _Analyzer analyzer) {
        for (final File file : root.listFiles()) {
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

    private final Map<String, Map<String, String>> _fileNameDb = new HashMap<String, Map<String, String>>();
    private void _analyzeSimpleListedDir(final File root, final String listFile, final String targetDir, final OpType opType) {
        if (!_fileNameDb.containsKey(listFile)) {
            final Map<String, String> fileNames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (final Pair<String, String> nameurl : FileUtil.readNameUrlFile(listFile)) {
                // we use a map instead of a set since we need to match case-insensitively but still map to the correct case
                fileNames.put(nameurl.getLeft(), nameurl.getLeft());
            }
            _fileNameDb.put(listFile, fileNames);
        }

        final Map<String, String> fileDb = _fileNameDb.get(listFile);
        _analyzeListedDir(root, targetDir, new _ListedAnalyzer() {
            @Override public String map(final String filename) { return fileDb.containsKey(filename) ? fileDb.get(filename) : null; }
            @Override public OpType getOpType(final String filename) { return opType; }
        });
    }

    private abstract class _ListedAnalyzer {
        abstract String map(String filename);
        abstract OpType getOpType(String filename);

        // returns whether the directory has been handled
        boolean onDir(final File dir) { return false; }
    }

    private void _analyzeListedDir(final File root, final String targetDir, final _ListedAnalyzer listedAnalyzer) {
        _analyzeDir(root, new _Analyzer() {
            @Override void onFile(final File file) {
                final String filename = listedAnalyzer.map(file.getName());
                if (null != filename) {
                    final File targetFile = new File(targetDir, filename);
                    if (!file.equals(targetFile)) {
                        _cb.addOp(listedAnalyzer.getOpType(filename), file, targetFile);
                    }
                }
            }

            @Override boolean onDir(final File dir) { return listedAnalyzer.onDir(dir); }
        });
    }

    private int _countFiles(final File root) {
        int count = 0;
        for (final File file : root.listFiles()) {
            if (_cb.checkCancel()) { return 0; }

            if (file.isFile()) {
                ++count;
            } else if (file.isDirectory()) {
                count += _countFiles(file);
            }
        }
        return count;
    }

    private static String _lcaseExt(final String filename) {
        final int lastDotIdx = filename.lastIndexOf('.');
        if (0 > lastDotIdx) {
            return filename;
        }
        final String basename = filename.substring(0, lastDotIdx);
        final String ext      = filename.substring(lastDotIdx).toLowerCase(Locale.ENGLISH);
        if (filename.endsWith(ext)) {
            return filename;
        }
        return basename + ext;
    }
}
