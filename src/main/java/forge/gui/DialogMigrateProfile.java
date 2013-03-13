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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import forge.Command;
import forge.error.BugReporter;
import forge.gui.MigrationSourceAnalyzer.OpType;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextField;
import forge.properties.NewConstants;

public class DialogMigrateProfile {
    private final Runnable _onImportSuccessful;
    private final FButton  _btnStart;
    private final FLabel   _btnChooseDir;
    private final JPanel   _selectionPanel;
    
    private volatile boolean _cancel;
    
    @SuppressWarnings("serial")
    public DialogMigrateProfile(String forcedSrcDir, final Runnable onDialogClose) {
        FPanel p = new FPanel(new MigLayout("insets dialog, gap 0, center, wrap"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));

        final boolean isMigration = !StringUtils.isEmpty(forcedSrcDir);
        
        // header
        p.add(new FLabel.Builder().text((isMigration ? "Migrate" : "Import") + " profile data").fontSize(15).build(), "center");
        
        if (isMigration) {
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
            p.add(blurbPanel, "gap 10 10 20 0");
        }
        
        // import source widgets
        JPanel importSourcePanel = new JPanel(new MigLayout("insets 0, gap 10"));
        importSourcePanel.setOpaque(false);
        importSourcePanel.add(new FLabel.Builder().text("Import from:").build());
        final FTextField txfSrc = new FTextField.Builder().readonly().build();
        importSourcePanel.add(txfSrc, "pushx, growx");
        _btnChooseDir = new FLabel.ButtonBuilder().text("Choose directory...").enabled(!isMigration).build();
        final JFileChooser _fileChooser = new JFileChooser();
        _fileChooser.setMultiSelectionEnabled(false);
        _fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        _btnChooseDir.setCommand(new Command() {
            @Override public void execute() {
                if (JFileChooser.APPROVE_OPTION == _fileChooser.showOpenDialog(null)) {
                    txfSrc.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        importSourcePanel.add(_btnChooseDir, "h pref+8!, w pref+12!");
        txfSrc.getDocument().addDocumentListener(new DocumentListener() {
            boolean _analyzerActive; // access synchronized on _onAnalyzerDone
            String prevText;
            
            private final Runnable _onAnalyzerDone = new Runnable() {
                public synchronized void run() {
                    _analyzerActive = false;
                    notify();
                }
            };
            
            @Override public void removeUpdate(DocumentEvent e)  { }
            @Override public void changedUpdate(DocumentEvent e) { }
            @Override public void insertUpdate(DocumentEvent e)  {
                final String text = txfSrc.getText();
                if (text.equals(prevText)) {
                    return;
                }
                prevText = text;
                
                // cancel any active analyzer
                _cancel = true;
                
                if (!text.isEmpty()) {
                    _btnChooseDir.setEnabled(false);
                    _btnStart.setEnabled(false);
                    
                    SwingWorker<Void, Void> analyzerStarter = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            // wait for active analyzer (if any) to quit
                            synchronized (_onAnalyzerDone) {
                                while (_analyzerActive) {
                                    _onAnalyzerDone.wait();
                                }
                            }
                            
                            return null;
                        }
                        
                        // executes in gui event loop thread
                        @Override
                        protected void done() {
                            _cancel = false;
                            synchronized (_onAnalyzerDone) {
                                _AnalyzerUpdater analyzer = new _AnalyzerUpdater(text, _onAnalyzerDone);
                                analyzer.execute();
                                _analyzerActive = true;
                            }
                            if (!isMigration) {
                                _btnChooseDir.setEnabled(true);
                            }
                        }
                    };
                    analyzerStarter.execute();
                }
            }
        });
        p.add(importSourcePanel, "gaptop 20, pushx, growx");
        
        // prepare import selection panel
        _selectionPanel = new JPanel();
        _selectionPanel.setOpaque(false);
        p.add(_selectionPanel, "growx, h 100%, gaptop 10");
        
        // action button widgets
        final Runnable cleanup = new Runnable() {
            @Override public void run() { SOverlayUtils.hideOverlay(); }
        };
        _btnStart = new FButton("Start import");
        _btnStart.setEnabled(false);

        final FButton btnCancel = new FButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                _cancel = true;
                cleanup.run();
                if (null != onDialogClose) {
                    onDialogClose.run();
                }
            }
        });

        _onImportSuccessful = new Runnable() {
            @Override public void run() {
                btnCancel.setText("Done");
            }
        };
        
        JPanel southPanel = new JPanel(new MigLayout("ax center"));
        southPanel.setOpaque(false);
        southPanel.add(_btnStart, "center, w pref+72!, h pref+12!");
        southPanel.add(btnCancel, "center, w pref+72!, h pref+12!");
        
        p.add(southPanel, "growx");
      
        JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(p, "w 400::80%, h 100::90%");
        SOverlayUtils.showOverlay();
        
        // focus cancel button
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { btnCancel.requestFocusInWindow(); }
        });
        
        if (isMigration) {
            File srcDirFile = new File(forcedSrcDir);
            txfSrc.setText(srcDirFile.getAbsolutePath());
        }
    }
    
    private class _UnknownDeckChoice {
        public final String name;
        public final String path;
        
        public _UnknownDeckChoice(String name0, String path0) {
            name = name0;
            path = path0;
        }
        
        @Override
        public String toString() { return name; }
    }
    
    private class _AnalyzerUpdater extends SwingWorker<Void, Void> {
        private final Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> _selections =
                new HashMap<OpType, Pair<FCheckBox, ? extends Map<File, File>>>();
        
        private final ChangeListener _stateChangedListener = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent arg0) { _updateUI(); }
        };
        
        private final String       _srcDir;
        private final Runnable     _onAnalyzerDone;
        private final JComboBox    _unknownDeckCombo;
        private final FCheckBox    _moveCheckbox;
        private final FCheckBox    _overwriteCheckbox;
        private final JTextArea    _operationLog;
        private final JProgressBar _progressBar;
        
        private final _OperationLogAsyncUpdater _operationLogUpdater;
        
        public _AnalyzerUpdater(String srcDir, Runnable onAnalyzerDone) {
            _srcDir         = srcDir;
            _onAnalyzerDone = onAnalyzerDone;
            
            _selectionPanel.removeAll();
            _selectionPanel.setLayout(new MigLayout("insets 0, gap 5, wrap"));
            
            JPanel cbPanel = new JPanel(new MigLayout("insets 0, gap 5"));
            cbPanel.setOpaque(false);
            
            // add deck selections
            JPanel knownDeckPanel = new JPanel(new MigLayout("insets 0, gap 5, wrap 2"));
            knownDeckPanel.setOpaque(false);
            knownDeckPanel.add(new FLabel.Builder().text("Decks").build(), "wrap");
            _addSelectionWidget(knownDeckPanel, OpType.CONSTRUCTED_DECK, "Constructed decks");
            _addSelectionWidget(knownDeckPanel, OpType.DRAFT_DECK,       "Draft decks");
            _addSelectionWidget(knownDeckPanel, OpType.PLANAR_DECK,      "Planar decks");
            _addSelectionWidget(knownDeckPanel, OpType.SCHEME_DECK,      "Scheme decks");
            _addSelectionWidget(knownDeckPanel, OpType.SEALED_DECK,      "Sealed decks");
            _addSelectionWidget(knownDeckPanel, OpType.UNKNOWN_DECK,     "Unknown decks");
            JPanel unknownDeckPanel = new JPanel(new MigLayout("insets 0, gap 5"));
            unknownDeckPanel.setOpaque(false);
            _unknownDeckCombo = new JComboBox();
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Constructed", NewConstants.DECK_CONSTRUCTED_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Draft",       NewConstants.DECK_DRAFT_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Planar",      NewConstants.DECK_PLANE_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Scheme",      NewConstants.DECK_SCHEME_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Sealed",      NewConstants.DECK_SEALED_DIR));
            _unknownDeckCombo.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent arg0) { _updateUI(); }
            });
            unknownDeckPanel.add(new FLabel.Builder().text("Treat unknown decks as:").build());
            unknownDeckPanel.add(_unknownDeckCombo);
            knownDeckPanel.add(unknownDeckPanel, "span");
            cbPanel.add(knownDeckPanel, "aligny top");
            
            // add other userDir data elements
            JPanel dataPanel = new JPanel(new MigLayout("insets 0, gap 5, wrap"));
            dataPanel.setOpaque(false);
            dataPanel.add(new FLabel.Builder().text("Other data").build());
            _addSelectionWidget(dataPanel, OpType.GAUNTLET_DATA,   "Gauntlet data");
            _addSelectionWidget(dataPanel, OpType.QUEST_DATA,      "Quest saves");
            _addSelectionWidget(dataPanel, OpType.PREFERENCE_FILE, "Preference files");
            cbPanel.add(dataPanel, "aligny top");
            
            // add cacheDir data elements
            JPanel cachePanel = new JPanel(new MigLayout("insets 0, gap 5, wrap 2"));
            cachePanel.setOpaque(false);
            cachePanel.add(new FLabel.Builder().text("Cached data").build(), "wrap");
            _addSelectionWidget(cachePanel, OpType.DEFAULT_CARD_PIC, "Default card pics");
            _addSelectionWidget(cachePanel, OpType.SET_CARD_PIC,     "Set-specific card pics");
            _addSelectionWidget(cachePanel, OpType.TOKEN_PIC,        "Card token pics");
            _addSelectionWidget(cachePanel, OpType.QUEST_PIC,        "Quest-related pics");
            _addSelectionWidget(cachePanel, OpType.DB_FILE,          "Database files");
            cbPanel.add(cachePanel, "aligny top");
            _selectionPanel.add(cbPanel, "center");
            
            // add move/copy and overwrite checkboxes
            JPanel ioOptionPanel = new JPanel(new MigLayout("insets 0, gap 10"));
            ioOptionPanel.setOpaque(false);
            _moveCheckbox = new FCheckBox("Remove source files after copy");
            _moveCheckbox.setSelected(true);
            _moveCheckbox.addChangeListener(_stateChangedListener);
            ioOptionPanel.add(_moveCheckbox);
            _overwriteCheckbox = new FCheckBox("Overwrite existing files");
            _overwriteCheckbox.addChangeListener(_stateChangedListener);
            ioOptionPanel.add(_overwriteCheckbox);
            _selectionPanel.add(ioOptionPanel);
            
            // add operation summary textfield
            _operationLog = new JTextArea();
            _operationLog.setFont(new Font("Monospaced", Font.PLAIN, 10));
            _operationLog.setOpaque(false);
            _operationLog.setWrapStyleWord(true);
            _operationLog.setLineWrap(true);
            _operationLog.setEditable(false);
            // autoscroll to bottom when we append text
            DefaultCaret caret = (DefaultCaret)_operationLog.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JScrollPane scroller = new JScrollPane(_operationLog);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            _selectionPanel.add(scroller, "w 400:100%:100%, h 60:100%:100%");
            
            // add progress bar
            _progressBar = new JProgressBar();
            _progressBar.setString("Preparing to analyze source directory...");
            _progressBar.setStringPainted(true);
            _selectionPanel.add(_progressBar, "w 100%!");
            
            _operationLogUpdater = new _OperationLogAsyncUpdater(_selections, _operationLog);
            _operationLogUpdater.start();
            
            // set checkbox labels
            _updateUI();
            
            // resize the panel properly now that the _selectionPanel is filled in
            _selectionPanel.getParent().validate();
            _selectionPanel.getParent().invalidate();
        }
        
        private void _addSelectionWidget(JPanel parent, OpType type, String name) {
            FCheckBox cb = new FCheckBox();
            cb.setName(name);
            cb.setSelected(true);
            cb.addChangeListener(_stateChangedListener);
            
            // use a skip list map instead of a regular hashmap so that the files are sorted alphabetically
            _selections.put(type, Pair.of(cb, new ConcurrentSkipListMap<File, File>()));
            parent.add(cb);
        }
        
        // must be called from GUI event loop thread
        private void _updateUI() {
            // update checkbox text lables with current totals
            Set<OpType> selectedOptions = new HashSet<OpType>();
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                FCheckBox cb = selection.getLeft();
                
                if (cb.isSelected()) {
                    selectedOptions.add(entry.getKey());
                }
                
                cb.setText(String.format("%s (%d)", cb.getName(), selection.getRight().size()));
            }
            
            _operationLogUpdater.requestUpdate(selectedOptions, (_UnknownDeckChoice)_unknownDeckCombo.getSelectedItem(),
                    _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
        }
        
        private void _disableAll() {
            for (Pair<FCheckBox, ? extends Map<File, File>> selection : _selections.values()) {
                selection.getLeft().setEnabled(false);
            }
            _unknownDeckCombo.setEnabled(false);
            _moveCheckbox.setEnabled(false);
            _overwriteCheckbox.setEnabled(false);
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            Timer timer = null;
            
            try {
                Map<OpType, Map<File, File>> selections = new HashMap<OpType, Map<File, File>>();
                for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                    selections.put(entry.getKey(), entry.getValue().getRight());
                }
                
                MigrationSourceAnalyzer.AnalysisCallback cb = new MigrationSourceAnalyzer.AnalysisCallback() {
                    @Override
                    public boolean checkCancel() { return _cancel; }
                    
                    @Override
                    public void addOp(OpType type, File src, File dest) {
                        _selections.get(type).getRight().put(src, dest);
                    }
                };
                
                final MigrationSourceAnalyzer msa = new MigrationSourceAnalyzer(_srcDir, cb);
                final int numFilesToAnalyze = msa.getNumFilesToAnalyze();
                
                timer = new Timer(500, null);
                final Timer finalTimer = timer;
                timer.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent arg0) {
                        if (_cancel) {
                            finalTimer.stop();
                            return;
                        }
                        
                        // timers run in the gui event loop, so it's ok to interact with widgets
                        _progressBar.setValue(msa.getNumFilesAnalyzed());
                        _updateUI();
                    }
                });
    
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        if (_cancel) { return; }
                        _progressBar.setString("Analyzing...");
                        _progressBar.setMaximum(numFilesToAnalyze);
                        _progressBar.setValue(0);
                        _progressBar.setIndeterminate(false);
                        
                        // start update timer
                        finalTimer.start();
                    }
                });
                
                msa.doAnalysis();
            } catch (final Exception e) {
                _cancel = true;
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        _progressBar.setString("Error");
                        BugReporter.reportException(e);
                    }
                });
            } finally {
                if (null != timer)
                {
                    timer.stop();
                }
            }
            
            return null;
        }

        // executes in gui event loop thread
        @Override
        protected void done() {
            if (!_cancel) {
                _progressBar.setValue(_progressBar.getMaximum());
                _updateUI();
                _progressBar.setString("Analysis complete");
                
                _btnStart.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent arg0) {
                        _btnStart.removeActionListener(this);
                        _btnStart.setEnabled(false);
                        _btnChooseDir.setEnabled(false);
                        
                        _disableAll();
                        _operationLogUpdater.requestStop();
                        
                        _Importer importer = new _Importer(
                                _selections, _unknownDeckCombo, _operationLog, _progressBar,
                                _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
                        importer.execute();
                    }
                });
                _btnStart.setEnabled(true);
            }
        
            _onAnalyzerDone.run();
        }
    }
    
    private class _OperationLogAsyncUpdater extends Thread {
        final Map<OpType, Map<File, File>> _selections;
        final JTextArea                    _operationLog; // safe to set text from another thread
        
        // synchronized-access data
        private int                _updateCallCnt = 0;
        private Set<OpType>        _selectedOptions;
        private _UnknownDeckChoice _unknownDeckChoice;
        private boolean            _isMove;
        private boolean            _isOverwrite;
        private boolean            _stop;
        
        public _OperationLogAsyncUpdater(Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> selections, JTextArea operationLog) {
            super("OperationLogUpdater");
            setDaemon(true);
            
            _selections   = new HashMap<OpType, Map<File, File>>();
            _operationLog = operationLog;
            
            // remove references to FCheckBox when adding map -- we can't access it from the other thread anyway
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                _selections.put(entry.getKey(), entry.getValue().getRight());
            }
        }

        public synchronized void requestUpdate(
                Set<OpType> selectedOptions, _UnknownDeckChoice unknownDeckChoice, boolean isMove, boolean isOverwrite) {
            ++_updateCallCnt;
            _selectedOptions   = selectedOptions;
            _unknownDeckChoice = unknownDeckChoice;
            _isMove            = isMove;
            _isOverwrite       = isOverwrite;
            
            // notify waiter
            notify();
        }
        
        public synchronized void requestStop() {
            _stop = true;
            
            // notify waiter
            notify();
        }
        
        private void _run() throws InterruptedException {
            int lastUpdateCallCnt = _updateCallCnt;
            Set<OpType>        selectedOptions;
            _UnknownDeckChoice unknownDeckChoice;
            boolean isMove;
            boolean isOverwrite;
            
            while (true) {
                synchronized (this) {
                    if (_stop) { break; }
                    while (lastUpdateCallCnt == _updateCallCnt) {
                        wait();
                        if (_stop) { break; }
                    }
                    
                    lastUpdateCallCnt = _updateCallCnt;
                    selectedOptions   = _selectedOptions;
                    unknownDeckChoice = _unknownDeckChoice;
                    isMove            = _isMove;
                    isOverwrite       = _isOverwrite;
                }
                
                // set operation summary
                StringBuilder log = new StringBuilder();
                int totalOps = 0;
                for (OpType opType : selectedOptions) {
                    Map<File, File> ops = _selections.get(opType);
                    totalOps += ops.size();
                    
                    for (Map.Entry<File, File> op : ops.entrySet()) {
                        File dest = op.getValue();
                        if (OpType.UNKNOWN_DECK == opType) {
                            dest = new File(unknownDeckChoice.path, dest.getName());
                        }
                        log.append(String.format("%s -> %s\n",
                                op.getKey().getAbsolutePath(), dest.getAbsolutePath()));
                    }
                }
                
                if (0 < totalOps) {
                    log.append("\n");
                }
                log.append(isMove ? "Moving" : "Copying");
                log.append(" ").append(totalOps).append(" files\n");
                log.append(isOverwrite ? "O" : "Not o");
                log.append("verwriting existing files");

                _operationLog.setText(log.toString());
            }
        }
        
        @Override
        public void run() {
            try { _run(); } catch (final InterruptedException e) {
                // we never interrupt the thread, so this is not expected to happen
                _cancel = true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        BugReporter.reportException(e);
                    }
                });
            }
        }
    }

    private class _Importer extends SwingWorker<Void, Void> {
        private final Map<File, File> _operations;
        private final JTextArea       _operationLog;
        private final JProgressBar    _progressBar;
        private final boolean         _move;
        private final boolean         _overwrite;
        
        public _Importer(Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> selections, JComboBox unknownDeckCombo,
                JTextArea operationLog, JProgressBar progressBar, boolean move, boolean overwrite) {
            _operationLog = operationLog;
            _progressBar  = progressBar;
            _move         = move;
            _overwrite    = overwrite;
            
            int totalOps = 0;
            for (Pair<FCheckBox, ? extends Map<File, File>> selection : selections.values()) {
                if (selection.getLeft().isSelected()) {
                    totalOps += selection.getRight().size();
                }
            }
            _operations = new HashMap<File, File>(totalOps);
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                if (selection.getLeft().isSelected()) {
                    if (OpType.UNKNOWN_DECK != entry.getKey()) {
                        _operations.putAll(selection.getRight());
                    } else {
                        for (Map.Entry<File, File> op : selection.getRight().entrySet()) {
                            _UnknownDeckChoice choice = (_UnknownDeckChoice)unknownDeckCombo.getSelectedItem();
                            _operations.put(op.getKey(), new File(choice.path, op.getValue().getName()));
                        }
                    }
                }
            }
            for (Pair<FCheckBox, ? extends Map<File, File>> selection : selections.values()) {
                if (selection.getLeft().isSelected()) {
                    _operations.putAll(selection.getRight());
                }
            }
            
            // set progress bar bounds
            _progressBar.setString(_move ? "Moving files..." : "Copying files...");
            _progressBar.setMinimum(0);
            _progressBar.setMaximum(_operations.size());
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            try {
                // working with textbox text is thread safe
                _operationLog.setText("");
                
                int numOps = 0;
                int numSucceeded = 0;
                int numFailed = 0;
                for (Map.Entry<File, File> op : _operations.entrySet()) {
                    if (_cancel) { break; }
                    
                    final int curOpNum = ++numOps;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            if (_cancel) { return; }
                            _progressBar.setValue(curOpNum);
                        }
                    });
                    
                    File srcFile  = op.getKey();
                    File destFile = op.getValue();
    
                    try {
                        if (_overwrite || !destFile.exists()) {
                            _copyFile(srcFile, destFile);
                        }
                        
                        if (_move) {
                            srcFile.delete();
                        }
                        
                        // working with textbox text is thread safe
                        _operationLog.append(String.format("%s %s -> %s\n",
                                _move ? "Moved" : "Copied",
                                srcFile.getAbsolutePath(), destFile.getAbsolutePath()));
                        ++numSucceeded;
                    } catch (IOException e) {
                        _operationLog.append(String.format("Failed to %s %s -> %s (%s)\n",
                                _move ? "move" : "copy",
                                srcFile.getAbsolutePath(), destFile.getAbsolutePath(),
                                e.getMessage()));
                        ++numFailed;
                    }
                }
                
                _operationLog.append(String.format("\nImport complete.  %d files %s, %d errors",
                        numSucceeded, _move ? "moved" : "copied", numFailed));
            } catch (final Exception e) {
                _cancel = true;
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        _progressBar.setString("Error");
                        BugReporter.reportException(e);
                    }
                });
            }
            
            return null;
        }

        @Override
        protected void done() {
            if (_cancel) { return; }
            
            _progressBar.setValue(_progressBar.getMaximum());
            _progressBar.setString("Import complete");
            _onImportSuccessful.run();
        }
        
        private void _copyFile(File srcFile, File destFile) throws IOException {
            destFile.getParentFile().mkdirs();
            
            if (!destFile.exists()) {
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
