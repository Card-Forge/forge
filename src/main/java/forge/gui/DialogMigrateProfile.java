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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
import forge.gui.toolbox.SmartScroller;
import forge.properties.NewConstants;

/**
 * This class implements an overlay-based dialog that imports data from a user-selected directory
 * into the correct locations in the user and cache directories.  There is a lot of I/O and data
 * processing done in this class, so most operations are asynchronous.
 */
public class DialogMigrateProfile {
    private final FButton _btnStart;
    private final FButton _btnCancel;
    private final FLabel  _btnChooseDir;
    private final JPanel  _selectionPanel;
    
    // volatile since it is checked from multiple threads
    private volatile boolean _cancel;
    
    @SuppressWarnings("serial")
    public DialogMigrateProfile(String forcedSrcDir, final Runnable onDialogClose) {
        FPanel p = new FPanel(new MigLayout("insets dialog, gap 0, center, wrap"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));

        final boolean isMigration = !StringUtils.isEmpty(forcedSrcDir);
        
        // header
        p.add(new FLabel.Builder().text((isMigration ? "Migrate" : "Import") + " profile data").fontSize(15).build(), "center");
        
        // add some help text if this is for the initial data migration
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
                // bring up a file open dialog and, if the OK button is selected, apply the filename
                // to the import source text field
                if (JFileChooser.APPROVE_OPTION == _fileChooser.showOpenDialog(null)) {
                    File f = _fileChooser.getSelectedFile();
                    if (!f.canRead()) {
                        JOptionPane.showMessageDialog(txfSrc, "Cannot access selected directory (Permission denied).");
                    } else {
                        txfSrc.setText(f.getAbsolutePath());
                    }
                }
            }
        });
        importSourcePanel.add(_btnChooseDir, "h pref+8!, w pref+12!");
        
        // add change handler to the import source text field that starts up a
        // new analyzer.  it also interacts with the current active analyzer,
        // if any, to make sure it cancels out before the new one is initiated
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
                // text field is read-only, so the only time this will get updated
                // is when _btnChooseDir does it 
                final String text = txfSrc.getText();
                if (text.equals(prevText)) {
                    // only restart the analyzer if the directory has changed
                    return;
                }
                prevText = text;
                
                // cancel any active analyzer
                _cancel = true;
                
                if (!text.isEmpty()) {
                    // ensure we don't get two instances of this function running at the same time
                    _btnChooseDir.setEnabled(false);
                    
                    // re-disable the start button.  it will be enabled if the previous analyzer has
                    // already successfully finished
                    _btnStart.setEnabled(false);
                    
                    // we have to wait in a background thread since we can't block in the GUI thread
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
                                // this will populate the panel with data selection widgets
                                _AnalyzerUpdater analyzer = new _AnalyzerUpdater(text, _onAnalyzerDone, isMigration);
                                analyzer.execute();
                                _analyzerActive = true;
                            }
                            if (!isMigration) {
                                // only enable the directory choosing button if this is not a migration dialog
                                // since in that case we're permanently locked to the starting directory
                                _btnChooseDir.setEnabled(true);
                            }
                        }
                    };
                    analyzerStarter.execute();
                }
            }
        });
        p.add(importSourcePanel, "gaptop 20, pushx, growx");
        
        // prepare import selection panel (will be cleared and filled in later by an analyzer)
        _selectionPanel = new JPanel();
        _selectionPanel.setOpaque(false);
        p.add(_selectionPanel, "growx, h 100%, gaptop 10");
        
        // action button widgets
        final Runnable cleanup = new Runnable() {
            @Override public void run() { SOverlayUtils.hideOverlay(); }
        };
        _btnStart = new FButton("Start import");
        _btnStart.setEnabled(false);
        _btnCancel = new FButton("Cancel");
        _btnCancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                _cancel = true;
                cleanup.run();
                if (null != onDialogClose) {
                    onDialogClose.run();
                }
            }
        });

        JPanel southPanel = new JPanel(new MigLayout("ax center"));
        southPanel.setOpaque(false);
        southPanel.add(_btnStart, "center, w pref+144!, h pref+12!");
        southPanel.add(_btnCancel, "center, w pref+144!, h pref+12!, gap 72");
        p.add(southPanel, "growx");
      
        JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(p, "w 500::80%, h 100::90%");
        SOverlayUtils.showOverlay();
        
        // focus cancel button after the dialog is shown
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { _btnCancel.requestFocusInWindow(); }
        });
        
        // if our source dir is provided, set the text, which will fire off an analyzer
        if (isMigration) {
            File srcDirFile = new File(forcedSrcDir);
            txfSrc.setText(srcDirFile.getAbsolutePath());
        }
    }
    
    // encapsulates the choices in the combobox for choosing the destination paths for
    // decks of unknown type
    private class _UnknownDeckChoice {
        public final String name;
        public final String path;
        
        public _UnknownDeckChoice(String name0, String path0) {
            name = name0;
            path = path0;
        }
        
        @Override public String toString() { return name; }
    }
    
    // this class owns the import selection widgets and bridges them with the running
    // MigrationSourceAnalyzer instance
    private class _AnalyzerUpdater extends SwingWorker<Void, Void> {
        // associates a file operation type with its enablement checkbox and the set
        // of file move/copy operations that enabling it would entail
        private final Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> _selections =
                new HashMap<OpType, Pair<FCheckBox, ? extends Map<File, File>>>();
        
        // attached to all changeable widgets to keep the UI in sync
        private final ChangeListener _stateChangedListener = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent arg0) { _updateUI(); }
        };
        
        private final String       _srcDir;
        private final Runnable     _onAnalyzerDone;
        private final boolean      _isMigration;
        private final JComboBox    _unknownDeckCombo;
        private final FCheckBox    _moveCheckbox;
        private final FCheckBox    _overwriteCheckbox;
        private final JTextArea    _operationLog;
        private final JProgressBar _progressBar;
        
        // updates the _operationLog widget asynchronously to keep the UI responsive
        private final _OperationLogAsyncUpdater _operationLogUpdater;
        
        public _AnalyzerUpdater(String srcDir, Runnable onAnalyzerDone, boolean isMigration) {
            _srcDir         = srcDir;
            _onAnalyzerDone = onAnalyzerDone;
            _isMigration    = isMigration;
            
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
            _moveCheckbox.setSelected(isMigration);
            _moveCheckbox.addChangeListener(_stateChangedListener);
            ioOptionPanel.add(_moveCheckbox);
            _overwriteCheckbox = new FCheckBox("Overwrite files in destination");
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
            // autoscroll when we set/add text unless the user has intentionally scrolled somewhere else
            JScrollPane scroller = new JScrollPane(_operationLog);
            new SmartScroller(scroller);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            _selectionPanel.add(scroller, "w 400:100%:100%, h 60:100%:100%");
            
            // add progress bar
            _progressBar = new JProgressBar();
            _progressBar.setString("Preparing to analyze source directory...");
            _progressBar.setStringPainted(true);
            _selectionPanel.add(_progressBar, "w 100%!");
            
            // start the op log updater
            _operationLogUpdater = new _OperationLogAsyncUpdater(_selections, _operationLog);
            _operationLogUpdater.start();
            
            // set initial checkbox labels
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
            
            // use a skip list map instead of a regular hashmap so that the files are sorted
            // alphabetically in the logs.  note that this is a concurrent data structure
            // since it will be modified and read simultaneously by different threads
            _selections.put(type, Pair.of(cb, new ConcurrentSkipListMap<File, File>()));
            parent.add(cb);
        }
        
        // must be called from GUI event loop thread
        private void _updateUI() {
            // update checkbox text labels with current totals
            Set<OpType> selectedOptions = new HashSet<OpType>();
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                FCheckBox cb = selection.getLeft();
                
                if (cb.isSelected()) {
                    selectedOptions.add(entry.getKey());
                }
                
                cb.setText(String.format("%s (%d)", cb.getName(), selection.getRight().size()));
            }
            
            // asynchronously update the text in the op log, which may be many tens of thousands of lines long
            // if this were done synchronously the UI would slow to a crawl
            _operationLogUpdater.requestUpdate(selectedOptions, (_UnknownDeckChoice)_unknownDeckCombo.getSelectedItem(),
                    _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
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
                        // add to concurrent map
                        _selections.get(type).getRight().put(src, dest);
                    }
                };
                
                final MigrationSourceAnalyzer msa = new MigrationSourceAnalyzer(_srcDir, cb);
                final int numFilesToAnalyze = msa.getNumFilesToAnalyze();
                
                // update only once every half-second so we're not flooding the UI with updates
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
    
                // update the progress bar widget from the GUI event loop
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
                
                // does not return until analysis is complete or has been canceled
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
                // ensure the UI update timer is stopped after analysis is complete
                if (null != timer) {
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
                
                // clear any previously-set action listeners on the start button
                // in case we've previously completed an analysis but changed the directory
                // instead of starting the import
                for (ActionListener a : _btnStart.getActionListeners()) {
                    _btnStart.removeActionListener(a);
                }
                
                // set up the start button to start the prepared import on click
                _btnStart.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent arg0) {
                        // if this is a migration, warn if active settings will not complete a migration and give the
                        // user an option to fix
                        if (_isMigration) {
                            // assemble a list of selections that need to be selected to complete a full migration
                            List<String> unselectedButShouldBe = new ArrayList<String>();
                            for (Pair<FCheckBox, ? extends Map<File, File>> entry : _selections.values()) {
                                // add name to list if checkbox is unselected, but contains operations
                                FCheckBox cb = entry.getLeft();
                                if (!cb.isSelected() && 0 < entry.getRight().size()) {
                                    unselectedButShouldBe.add(cb.getName());
                                }
                            }

                            if (!unselectedButShouldBe.isEmpty() || !_moveCheckbox.isSelected()) {
                                StringBuilder sb = new StringBuilder("<html>");
                                if (!unselectedButShouldBe.isEmpty()) {
                                    sb.append("It looks like the following options are not selected, which will result in an incomplete migration:");
                                    sb.append("<ul>");
                                    for (String cbName : unselectedButShouldBe) {
                                        sb.append("<li><b>").append(cbName).append("</b></li>");
                                    }
                                    sb.append("</ul>");
                                }
                                
                                if (!_moveCheckbox.isSelected()) {
                                    sb.append(unselectedButShouldBe.isEmpty() ? "It " : "It also ").append("looks like the <b>");
                                    sb.append(_moveCheckbox.getText()).append("</b> option is not selected.<br><br>");
                                }
                                
                                sb.append("You can continue anyway, but the migration will be incomplete, and the data migration prompt<br>");
                                sb.append("will come up again the next time you start Forge in order to migrate the remaining files<br>");
                                sb.append("unless you move or delete them manually.</html>");
                                
                                Object[] options = { "Whoops, let me fix that!", "Continue with the import, I know what I'm doing." };
                                int chosen = JOptionPane.showOptionDialog(_operationLog, sb.toString(), "Migration warning",
                                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                                
                                if (1 != chosen) {
                                    // i.e. option 0 was chosen or the dialog was otherwise closed
                                    return;
                                }
                            }
                        }
                        
                        // ensure no other actions (except for cancel) can be taken while the import is in progress
                        _btnStart.setEnabled(false);
                        _btnChooseDir.setEnabled(false);
                        
                        for (Pair<FCheckBox, ? extends Map<File, File>> selection : _selections.values()) {
                            selection.getLeft().setEnabled(false);
                        }
                        _unknownDeckCombo.setEnabled(false);
                        _moveCheckbox.setEnabled(false);
                        _overwriteCheckbox.setEnabled(false);
                        
                        // stop updating the operation log -- the importer needs it now
                        _operationLogUpdater.requestStop();
                        
                        // start importing!
                        _Importer importer = new _Importer(
                                _srcDir, _selections, _unknownDeckCombo, _operationLog, _progressBar,
                                _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
                        importer.execute();
                        
                        _btnCancel.requestFocusInWindow();
                    }
                });
                
                // import ready to proceed: enable the start button
                _btnStart.setEnabled(true);
            }
        
            // report to the Choose Directory button that this analysis run has stopped
            _onAnalyzerDone.run();
        }
    }
    
    // asynchronously iterates through the given concurrent maps and populates the operation log with
    // the proposed operations
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
            
            // remove references to FCheckBox when populating map -- we can't safely access it from a thread
            // anyway and it's better to keep our data structure clean to prevent mistakes
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                _selections.put(entry.getKey(), entry.getValue().getRight());
            }
        }

        // updates the synchronized data with values for the next iteration in _run
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
                    
                    // safely copy synchronized data to local values that we will use for this runthrough
                    lastUpdateCallCnt = _updateCallCnt;
                    selectedOptions   = _selectedOptions;
                    unknownDeckChoice = _unknownDeckChoice;
                    isMove            = _isMove;
                    isOverwrite       = _isOverwrite;
                }
                
                // build operation log
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
                        log.append(op.getKey().getAbsolutePath()).append(" -> ");
                        log.append(dest.getAbsolutePath()).append("\n");
                    }
                }
                
                // append summary
                if (0 < totalOps) {
                    log.append("\n");
                }
                log.append("Prepared to ").append(isMove ? "move" : "copy");
                log.append(" ").append(totalOps).append(" files\n");
                log.append(isOverwrite ? "O" : "Not o").append("verwriting existing files");

                // set the JTextArea text directly (no need to use invokeLater: setText is thread-safe)
                _operationLog.setText(log.toString());
            }
        }
        
        @Override
        public void run() {
            try { _run(); } catch (final InterruptedException e) {
                _cancel = true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        // we never interrupt the thread, so this is not expected to happen
                        BugReporter.reportException(e);
                    }
                });
            }
        }
    }

    // asynchronously completes the specified I/O operations and updates the progress bar and operation log
    private class _Importer extends SwingWorker<Void, Void> {
        private final String          _srcDir;
        private final Map<File, File> _operations;
        private final JTextArea       _operationLog;
        private final JProgressBar    _progressBar;
        private final boolean         _move;
        private final boolean         _overwrite;
        
        public _Importer(String srcDir, Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> selections, JComboBox unknownDeckCombo,
                JTextArea operationLog, JProgressBar progressBar, boolean move, boolean overwrite) {
            _srcDir       = srcDir;
            _operationLog = operationLog;
            _progressBar  = progressBar;
            _move         = move;
            _overwrite    = overwrite;
            
            // build local operations map that only includes data that we can access from the background thread
            // use a tree map to maintain alphabetical order
            _operations = new TreeMap<File, File>();
            for (Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                if (selection.getLeft().isSelected()) {
                    if (OpType.UNKNOWN_DECK != entry.getKey()) {
                        _operations.putAll(selection.getRight());
                    } else {
                        // map unknown decks to selected directory
                        for (Map.Entry<File, File> op : selection.getRight().entrySet()) {
                            _UnknownDeckChoice choice = (_UnknownDeckChoice)unknownDeckCombo.getSelectedItem();
                            _operations.put(op.getKey(), new File(choice.path, op.getValue().getName()));
                        }
                    }
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
                
                // only update the text box once very half second
                final long updateIntervalMs = 500;
                long lastUpdateTimestampMs = System.currentTimeMillis();
                StringBuffer opLogBuf = new StringBuffer();
                
                // only update the progress bar when we expect the visual value to change
                final long progressInterval = Math.max(1, _operations.size() / _progressBar.getWidth());
                
                // the length of the prefix to remove from source paths
                final int srcPathPrefixLen;
                if (_srcDir.endsWith("/") || _srcDir.endsWith(File.separator)) {
                    srcPathPrefixLen = _srcDir.length();
                } else
                {
                    srcPathPrefixLen = _srcDir.length() + 1;
                }
                
                // stats maintained during import sequence
                int numOps       = 0;
                int numExisting  = 0;
                int numSucceeded = 0;
                int numFailed    = 0;
                for (Map.Entry<File, File> op : _operations.entrySet()) {
                    if (_cancel) { break; }
                    
                    final int curOpNum = ++numOps;
                    if (0 == curOpNum % progressInterval) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                if (_cancel) { return; }
                                _progressBar.setValue(curOpNum);
                            }
                        });
                    }
                    
                    long curTimeMs = System.currentTimeMillis();
                    if (updateIntervalMs <= curTimeMs - lastUpdateTimestampMs) {
                        lastUpdateTimestampMs = curTimeMs;
                        
                        // working with textbox text is thread safe
                        _operationLog.append(opLogBuf.toString());
                        opLogBuf.setLength(0);
                    }
                    
                    File srcFile  = op.getKey();
                    File destFile = op.getValue();
    
                    try {
                        // simplify logged source path and log next attempted operation
                        String srcPath = srcFile.getAbsolutePath();
                        // I doubt that the srcPath will start with anything other than _srcDir, even with symlinks,
                        // hardlinks, or Windows junctioned nodes, but it's better to be safe than to have malformed output
                        if (srcPath.startsWith(_srcDir)) {
                            srcPath = srcPath.substring(srcPathPrefixLen);
                        }
                        opLogBuf.append(_move ? "Moving " : "Copying ").append(srcPath).append(" -> ");
                        opLogBuf.append(destFile.getAbsolutePath()).append("\n");

                        if (!destFile.exists()) {
                            _copyFile(srcFile, destFile);
                        } else {
                            if (_overwrite) {
                                opLogBuf.append("  Destination file exists; overwriting\n");
                                _copyFile(srcFile, destFile);
                            } else {
                                opLogBuf.append("  Destination file exists; skipping copy\n");
                            }
                            ++numExisting;
                        }
                        
                        
                        if (_move) {
                            opLogBuf.append("  Removing source file after successful copy\n");
                            srcFile.delete();
                        }
                        
                        ++numSucceeded;
                    } catch (IOException e) {
                        opLogBuf.append("  Operation failed: ").append(e.getMessage()).append("\n");
                        ++numFailed;
                    }
                }
                
                // append summary footer
                opLogBuf.append("\nImport complete: ");
                opLogBuf.append(numSucceeded).append(" operation").append(1 == numSucceeded ? "" : "s").append(" succeeded, ");
                opLogBuf.append(numFailed).append(" error").append(1 == numFailed ? "" : "s");
                if (0 < numExisting) {
                    opLogBuf.append(", ").append(numExisting);
                    if (_overwrite) {
                        opLogBuf.append(" existing destination files overwritten");
                    } else {
                        opLogBuf.append(" copy operations skipped due to existing destination files");
                    }
                }
                _operationLog.append(opLogBuf.toString());
            } catch (final Exception e) {
                _cancel = true;
                
                // report any exceptions in a standard dialog
                // note that regular I/O errors don't throw, they'll just be mentioned in the log
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
            _btnCancel.requestFocusInWindow();
            if (_cancel) { return; }
            
            _progressBar.setValue(_progressBar.getMaximum());
            _progressBar.setString("Import complete");
            _btnCancel.setText("Done");
        }
    }
    
    // actual file copy routine.  uses java.nio classes for ultra-fast copying
    private static void _copyFile(File srcFile, File destFile) throws IOException {
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
