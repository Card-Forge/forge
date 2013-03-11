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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextArea;
import forge.gui.toolbox.FTextField;
import forge.properties.NewConstants;

public class DialogMigrateProfile {
    private final Runnable _onImportDone;
    private final FButton _btnStart;
    private final JPanel _selectionPanel;
    private volatile boolean _cancel;
    
    public DialogMigrateProfile(String srcDir, boolean showMigrationBlurb, final Runnable onImportDone) {
        FPanel p = new FPanel(new MigLayout("insets dialog, gap 0, center, wrap"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));

        // header
        p.add(new FLabel.Builder().text("Migrate profile data (in progress: not yet functional)").fontSize(15).build(), "center");
        
        if (showMigrationBlurb) {
            FPanel blurbPanel = new FPanel(new MigLayout("insets dialog, gap 10, center, wrap"));
            blurbPanel.setOpaque(false);
            blurbPanel.add(new FLabel.Builder().text("<html><b>What's this?</b></html>").build(), "growx");
            blurbPanel.add(new FLabel.Builder().text(
                    "<html>Over the last several years, people have had to jump through a lot of hoops to" +
                    " update to the most recent version.  We hope to reduce this workload to a point where a new" +
                    " user will find that it is fairly painless to update.  In order to make this happen, Forge" +
                    " has changed where it stores your data so that it is outside of the program installation directory." +
                    "  This way, when you upgrade, you will no longer need to import your data every time to get things" +
                    " working.  There are other benefits to having user data separate from program data, too, and it" +
                    " lays the groundwork for some cool new features.</html>").build());
            blurbPanel.add(new FLabel.Builder().text("<html><b>So where's my data going?</b></html>").build(), "growx");
            blurbPanel.add(new FLabel.Builder().text(
                    "<html>Forge will now store your data in the same place as other applications on your system." +
                    "  Specifically, your personal data, like decks, quest progress, and program preferences will be" +
                    " stored in <b>" + NewConstants.USER_DIR + "</b> and all downloaded content, such as card pictures," +
                    " skins, and quest world prices will be under <b>" + NewConstants.CACHE_DIR + "</b>.  If, for whatever" +
                    " reason, you need to set different paths, cancel out of this dialog, exit Forge, and find the <b>" +
                    NewConstants.PROFILE_TEMPLATE_FILE + "</b> file in the program installation directory.  Copy or rename" +
                    " it to <b>" + NewConstants.PROFILE_FILE + "</b> and edit the paths inside it.  Then restart Forge and use" +
                    " this dialog to move your data to the paths that you set.  Keep in mind that if you install a future" +
                    " version of Forge into a different directory, you'll need to copy this file over so Forge will know" +
                    " where to find your data.</html>").build());
            blurbPanel.add(new FLabel.Builder().text(
                    "<html><b>Remember, your data won't be available until you complete this step!</b></html>").build(), "growx");
            p.add(blurbPanel, "gap 10 10 20 20");
        }
        
        // import source widgets
        JPanel importSourcePanel = new JPanel(new MigLayout("insets 0, gap 5"));
        importSourcePanel.setOpaque(false);
        importSourcePanel.add(new FLabel.Builder().text("Import from:").build());
        boolean emptySrcDir = StringUtils.isEmpty(srcDir); 
        FTextField txfSrc = new FTextField.Builder().readonly(!emptySrcDir).build();
        importSourcePanel.add(txfSrc, "gap 5, pushx");
        if (!emptySrcDir) {
            File srcDirFile = new File(srcDir);
            txfSrc.setText(srcDirFile.getAbsolutePath());
        }
        importSourcePanel.add(new FLabel.ButtonBuilder().text("Choose directory...").enabled(emptySrcDir).build(), "h pref+8!, w pref+12!");
        p.add(importSourcePanel, "growx");
        
        // prepare import selection panel
        _selectionPanel = new JPanel();
        _selectionPanel.setOpaque(false);
        p.add(_selectionPanel, "growx");
        
        // action button widgets
        final Runnable cleanup = new Runnable() {
            @Override public void run() { SOverlayUtils.hideOverlay(); }
        };
        _btnStart = new FButton("Start import");
        _btnStart.setEnabled(false);

        final FButton btnCancel = new FButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { _cancel = true; cleanup.run(); }
        });

        _onImportDone = new Runnable() {
            @Override public void run() {
                cleanup.run();
                if (null != onImportDone) {
                    onImportDone.run();
                }
            }
        };
        
        JPanel southPanel = new JPanel(new MigLayout("gap 20, ax center"));
        southPanel.setOpaque(false);
        southPanel.add(_btnStart, "center, w 40%, h pref+12!");
        southPanel.add(btnCancel, "center, w 40%, h pref+12!");
        
        p.add(southPanel, "dock south");
      
        JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(p, "w 700!");
        SOverlayUtils.showOverlay();
        
        // focus cancel button
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { btnCancel.requestFocusInWindow(); }
        });
        
        _AnalyzerUpdater analyzer = new _AnalyzerUpdater(!emptySrcDir);
        analyzer.execute();
    }
    
    private enum OpType {
        CONSTRUCTED_DECK,
        UNKNOWN_DECK,
        GAUNTLET_DATA,
        QUEST_DATA,
        PREFERENCE_FILE
    }
    
    private class _AnalyzerUpdater extends SwingWorker<Void, Void> {
        private final Map<OpType, Pair<FCheckBox, ? extends Set<Pair<File, File>>>> _selections =
                new HashMap<DialogMigrateProfile.OpType, Pair<FCheckBox, ? extends Set<Pair<File, File>>>>();
        
        private final FCheckBox    _moveCheckbox;
        private final FTextArea    _operationLog;
        private final JProgressBar _progressBar;

        public _AnalyzerUpdater(boolean forced) {
            _selectionPanel.removeAll();
            _selectionPanel.setLayout(new MigLayout("insets 0, gap 5, wrap"));
            
            ChangeListener changeListener = new ChangeListener() {
                @Override public void stateChanged(ChangeEvent arg0) { _updateUI(); }
            };
            
            // add known deck checkboxes
            JPanel knownDeckPanel = new JPanel(new MigLayout("insets 0, gap 5, wrap"));
            knownDeckPanel.setOpaque(false);
            knownDeckPanel.add(new FLabel.Builder().text("Decks").build());
            FCheckBox constructed = new FCheckBox();
            constructed.setName("Constructed decks");
            constructed.addChangeListener(changeListener);
            _selections.put(OpType.CONSTRUCTED_DECK, Pair.of(constructed, new HashSet<Pair<File, File>>()));
            
            // add unknown deck combobox
            
            // add other data elements (gauntlets, quest data)
            
            // add move/copy checkbox
            _moveCheckbox = new FCheckBox("move files");
            _moveCheckbox.setSelected(true);
            _moveCheckbox.setEnabled(!forced);
            _moveCheckbox.addChangeListener(changeListener);
            _selectionPanel.add(_moveCheckbox);
            
            // add operation summary textfield
            _operationLog = new FTextArea();
            _operationLog.setFocusable(true);
            JScrollPane scroller = new JScrollPane(_operationLog);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            _selectionPanel.add(scroller, "w 100%!, h 10%!");
            
            // add progress bar
            _progressBar = new JProgressBar();
            _progressBar.setIndeterminate(true);
            _progressBar.setString("Analyzing source directory...");
            _progressBar.setStringPainted(true);
            _selectionPanel.add(_progressBar, "w 100%!");
        }
        
        private void _updateUI() {
            // set operation summary
            StringBuilder log = new StringBuilder();
            int totalOps = 0;
            for (Pair<FCheckBox, ? extends Set<Pair<File, File>>> selection : _selections.values()) {
                FCheckBox cb              = selection.getLeft();
                Set<Pair<File, File>> ops = selection.getRight();
                
                if (cb.isSelected()) {
                    totalOps += ops.size();
                }
                
                // update checkbox text with new totals
                cb.setText(String.format("%s (%d)", cb.getName(), ops.size()));
            }
            log.append(_moveCheckbox.isSelected() ? "Moving" : "Copying");
            log.append(" ").append(totalOps).append(" files\n\n");
            for (Pair<FCheckBox, ? extends Set<Pair<File, File>>> selection : _selections.values()) {
                if (selection.getLeft().isSelected()) {
                    for (Pair<File, File> op : selection.getRight()) {
                        log.append(String.format("%s -> %s\n",
                                op.getLeft().getAbsolutePath(), op.getRight().getAbsolutePath()));
                    }
                }
            }
            _operationLog.setText(log.toString());
        }
        
        private void _disableAll() {
            _moveCheckbox.setEnabled(false);
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            // TODO: analysis
            // ensure we ignore data that is already in the destination directory
            return null;
        }

        @Override
        protected void done() {
            if (_cancel) { return; }
            _btnStart.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent arg0) {
                    _btnStart.removeActionListener(this);
                    _btnStart.setEnabled(false);
                    
                    _disableAll();
                    
                    _Importer importer = new _Importer(_selections, _operationLog, _progressBar, _moveCheckbox.isSelected());
                    importer.execute();
                }
            });
            _btnStart.setEnabled(true);
        }
    }
    
    private class _Importer extends SwingWorker<Void, Void> {
        private final List<Pair<File, File>> _operations;
        private final FTextArea              _operationLog;
        private final JProgressBar           _progressBar;
        private final boolean                _move;
        
        public _Importer(Map<OpType, Pair<FCheckBox, ? extends Set<Pair<File, File>>>> selections,
                FTextArea operationLog, JProgressBar progressBar, boolean move) {
            _operationLog = operationLog;
            _progressBar  = progressBar;
            _move         = move;
            
            int totalOps = 0;
            for (Pair<FCheckBox, ? extends Set<Pair<File, File>>> selection : selections.values()) {
                if (selection.getLeft().isSelected()) {
                    totalOps += selection.getRight().size();
                }
            }
            _operations = new ArrayList<Pair<File, File>>(totalOps);
            for (Pair<FCheckBox, ? extends Set<Pair<File, File>>> selection : selections.values()) {
                if (selection.getLeft().isSelected()) {
                    _operations.addAll(selection.getRight());
                }
            }
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            _operationLog.setText("");
            
            // determine total number of operations and set progress bar bounds
            _progressBar.setString(_move ? "Moving files" : "Copying files");
            _progressBar.setMinimum(0);
            _progressBar.setMaximum(_operations.size());
            _progressBar.setIndeterminate(false);
            
            // assumes all destination directories have been created
            int numOps = 0;
            for (Pair<File, File> op : _operations) {
                _progressBar.setValue(++numOps);
                
                File srcFile  = op.getLeft();
                File destFile = op.getRight();

                try {
                    _copyFile(srcFile, destFile);
                    
                    if (_move) {
                        srcFile.delete();
                    }
                    
                    // this operation is thread safe
                    _operationLog.append(String.format("%s %s -> %s\n",
                            _move ? "Moved" : "Copied",
                            srcFile.getAbsolutePath(), destFile.getAbsolutePath()));
                } catch (IOException e) {
                    _operationLog.append(String.format("Failed to %s %s -> %s (%s)\n",
                            _move ? "move" : "copy",
                            srcFile.getAbsolutePath(), destFile.getAbsolutePath(),
                            e.getMessage()));
                }
            }
            
            return null;
        }

        @Override
        protected void done() {
            _onImportDone.run();
        }
        
        private void _copyFile(File srcFile, File destFile) throws IOException {
            if(!destFile.exists()) {
                destFile.createNewFile();
            }

            FileChannel src  = null;
            FileChannel dest = null;
            try {
                src  = new FileInputStream(srcFile).getChannel();
                dest = new FileOutputStream(destFile).getChannel();
                dest.transferFrom(src, 0, src.size());
            } finally {
                if (src  != null) { src.close();  }
                if (dest != null) { dest.close(); }
            }
        }
    }
}
