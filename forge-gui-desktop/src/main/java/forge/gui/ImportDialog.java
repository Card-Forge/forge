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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

import forge.gui.ImportSourceAnalyzer.OpType;
import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextField;
import forge.toolbox.SmartScroller;
import forge.util.TextUtil;
import net.miginfocom.swing.MigLayout;

/**
 * This class implements an overlay-based dialog that imports data from a user-selected directory
 * into the correct locations in the user and cache directories.  There is a lot of I/O and data
 * processing done in this class, so most operations are asynchronous.
 */
public class ImportDialog {
    private final FButton _btnStart;
    private final FButton _btnCancel;
    private final FLabel  _btnChooseDir;
    private final FPanel  _topPanel;
    private final JPanel  _selectionPanel;
    private final FTextField _txfSrc;

    private final String forcedSrcDir;
    private final boolean isMigration;

    // volatile since it is checked from multiple threads
    private volatile boolean _cancel;

    private static final ImmutableList<String> fixOrContinue = ImmutableList.of("Whoops, let me fix that!", "Continue with the import, I know what I'm doing.");

    @SuppressWarnings("serial")
    public ImportDialog(final String forcedSrcDir, final Runnable onDialogClose) {
        this.forcedSrcDir = forcedSrcDir;

        _topPanel = new FPanel(new MigLayout("insets dialog, gap 0, center, wrap, fill"));
        _topPanel.setOpaque(false);
        _topPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

        isMigration = !StringUtils.isEmpty(forcedSrcDir);

        // header
        _topPanel.add(new FLabel.Builder().text((isMigration ? "Migrate" : "Import") + " profile data").fontSize(15).build(), "center");

        // add some help text if this is for the initial data migration
        if (isMigration) {
            final FPanel blurbPanel = new FPanel(new MigLayout("insets panel, gap 10, fill"));
            blurbPanel.setOpaque(false);
            final JPanel blurbPanelInterior = new JPanel(new MigLayout("insets dialog, gap 10, center, wrap, fill"));
            blurbPanelInterior.setOpaque(false);
            blurbPanelInterior.add(new FLabel.Builder().text("<html><b>What's this?</b></html>").build(), "growx, w 50:50:");
            blurbPanelInterior.add(new FLabel.Builder().text(
                    "<html>Over the last several years, people have had to jump through a lot of hoops to" +
                            " update to the most recent version.  We hope to reduce this workload to a point where a new" +
                            " user will find that it is fairly painless to update.  In order to make this happen, Forge" +
                            " has changed where it stores your data so that it is outside of the program installation directory." +
                            "  This way, when you upgrade, you will no longer need to import your data every time to get things" +
                            " working.  There are other benefits to having user data separate from program data, too, and it" +
                    " lays the groundwork for some cool new features.</html>").build(), "growx, w 50:50:");
            blurbPanelInterior.add(new FLabel.Builder().text("<html><b>So where's my data going?</b></html>").build(), "growx, w 50:50:");
            blurbPanelInterior.add(new FLabel.Builder().text(
                    "<html>Forge will now store your data in the same place as other applications on your system." +
                            "  Specifically, your personal data, like decks, quest progress, and program preferences will be" +
                            " stored in <b>" + ForgeConstants.USER_DIR + "</b> and all downloaded content, such as card pictures," +
                            " skins, and quest world prices will be under <b>" + ForgeConstants.CACHE_DIR + "</b>.  If, for whatever" +
                            " reason, you need to set different paths, cancel out of this dialog, exit Forge, and find the <b>" +
                            ForgeConstants.PROFILE_TEMPLATE_FILE + "</b> file in the program installation directory.  Copy or rename" +
                            " it to <b>" + ForgeConstants.PROFILE_FILE + "</b> and edit the paths inside it.  Then restart Forge and use" +
                            " this dialog to move your data to the paths that you set.  Keep in mind that if you install a future" +
                            " version of Forge into a different directory, you'll need to copy this file over so Forge will know" +
                    " where to find your data.</html>").build(), "growx, w 50:50:");
            blurbPanelInterior.add(new FLabel.Builder().text(
                    "<html><b>Remember, your data won't be available until you complete this step!</b></html>").build(), "growx, w 50:50:");

            final FScrollPane blurbScroller = new FScrollPane(blurbPanelInterior, true,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            blurbPanel.add(blurbScroller, "hmin 150, growy, growx, center, gap 0 0 5 5");
            _topPanel.add(blurbPanel, "gap 10 10 20 0, growy, growx, w 50:50:");
        }

        // import source widgets
        final JPanel importSourcePanel = new JPanel(new MigLayout("insets 0, gap 10"));
        importSourcePanel.setOpaque(false);
        importSourcePanel.add(new FLabel.Builder().text("Import from:").build());
        _txfSrc = new FTextField.Builder().readonly().build();
        importSourcePanel.add(_txfSrc, "pushx, growx");
        _btnChooseDir = new FLabel.ButtonBuilder().text("Choose directory...").enabled(!isMigration).build();
        final JFileChooser _fileChooser = new JFileChooser();
        _fileChooser.setMultiSelectionEnabled(false);
        _fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        _btnChooseDir.setCommand((UiCommand) () -> {
            // bring up a file open dialog and, if the OK button is selected, apply the filename
            // to the import source text field
            if (JFileChooser.APPROVE_OPTION == _fileChooser.showOpenDialog(JOptionPane.getRootFrame())) {
                final File f = _fileChooser.getSelectedFile();
                if (!f.canRead()) {
                    FOptionPane.showErrorDialog("Cannot access selected directory (Permission denied).");
                }
                else {
                    _txfSrc.setText(f.getAbsolutePath());
                }
            }
        });
        importSourcePanel.add(_btnChooseDir, "h pref+8!, w pref+12!");

        // add change handler to the import source text field that starts up a
        // new analyzer.  it also interacts with the current active analyzer,
        // if any, to make sure it cancels out before the new one is initiated
        _txfSrc.getDocument().addDocumentListener(new DocumentListener() {
            boolean _analyzerActive; // access synchronized on _onAnalyzerDone
            String prevText;

            private final Runnable _onAnalyzerDone = new Runnable() {
                @Override
                public synchronized void run() {
                    _analyzerActive = false;
                    notify();
                }
            };

            @Override public void removeUpdate(final DocumentEvent e)  { }
            @Override public void changedUpdate(final DocumentEvent e) { }
            @Override public void insertUpdate(final DocumentEvent e)  {
                // text field is read-only, so the only time this will get updated
                // is when _btnChooseDir does it
                final String text = _txfSrc.getText();
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
                    final SwingWorker<Void, Void> analyzerStarter = new SwingWorker<Void, Void>() {
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
                                final _AnalyzerUpdater analyzer = new _AnalyzerUpdater(text, _onAnalyzerDone, isMigration);
                                analyzer.run();
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
        _topPanel.add(importSourcePanel, "gaptop 20, pushx, growx");

        // prepare import selection panel (will be cleared and filled in later by an analyzer)
        _selectionPanel = new JPanel();
        _selectionPanel.setOpaque(false);
        _topPanel.add(_selectionPanel, "growx, growy, gaptop 10");

        // action button widgets
        final Runnable cleanup = SOverlayUtils::hideOverlay;
        _btnStart = new FButton("Start import");
        _btnStart.setEnabled(false);
        _btnCancel = new FButton("Cancel");
        _btnCancel.addActionListener(e -> {
            _cancel = true;
            cleanup.run();
            if (null != onDialogClose) {
                onDialogClose.run();
            }
        });

        final JPanel southPanel = new JPanel(new MigLayout("ax center"));
        southPanel.setOpaque(false);
        southPanel.add(_btnStart, "center, w pref+144!, h pref+12!");
        southPanel.add(_btnCancel, "center, w pref+144!, h pref+12!, gap 72");
        _topPanel.add(southPanel, "growx");
    }

    public void show() {
        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(_topPanel, "w 500::90%, h 100::90%");
        SOverlayUtils.showOverlay();

        // focus cancel button after the dialog is shown
        SwingUtilities.invokeLater(_btnCancel::requestFocusInWindow);

        // if our source dir is provided, set the text, which will fire off an analyzer
        if (isMigration) {
            final File srcDirFile = new File(forcedSrcDir);
            _txfSrc.setText(srcDirFile.getAbsolutePath());
        }
    }

    // encapsulates the choices in the combobox for choosing the destination paths for
    // decks of unknown type
    private class _UnknownDeckChoice {
        public final String name;
        public final String path;

        public _UnknownDeckChoice(final String name0, final String path0) {
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
                new HashMap<>();

        // attached to all changeable widgets to keep the UI in sync
        private final ChangeListener _stateChangedListener = arg0 -> _updateUI();

        private final String       _srcDir;
        private final Runnable     _onAnalyzerDone;
        private final boolean      _isMigration;
        private final FLabel       _unknownDeckLabel;
        private final FComboBoxWrapper<_UnknownDeckChoice>    _unknownDeckCombo;
        private final FCheckBox    _moveCheckbox;
        private final FCheckBox    _overwriteCheckbox;
        private final JTextArea    _operationLog;
        private final JScrollPane  _operationLogScroller;
        private final JProgressBar _progressBar;

        // updates the _operationLog widget asynchronously to keep the UI responsive
        private final _OperationLogAsyncUpdater _operationLogUpdater;

        public _AnalyzerUpdater(final String srcDir, final Runnable onAnalyzerDone, final boolean isMigration) {
            _srcDir         = srcDir;
            _onAnalyzerDone = onAnalyzerDone;
            _isMigration    = isMigration;

            _selectionPanel.removeAll();
            _selectionPanel.setLayout(new MigLayout("insets 0, gap 5, wrap, fill"));

            final JPanel cbPanel = new JPanel(new MigLayout("insets 0, gap 5"));
            cbPanel.setOpaque(false);

            // add deck selections
            final JPanel knownDeckPanel = new JPanel(new MigLayout("insets 0, gap 5, wrap 2"));
            knownDeckPanel.setOpaque(false);
            knownDeckPanel.add(new FLabel.Builder().text("Decks").build(), "wrap");
            _addSelectionWidget(knownDeckPanel, OpType.CONSTRUCTED_DECK, "Constructed decks");
            _addSelectionWidget(knownDeckPanel, OpType.DRAFT_DECK,       "Draft decks");
            _addSelectionWidget(knownDeckPanel, OpType.PLANAR_DECK,      "Planar decks");
            _addSelectionWidget(knownDeckPanel, OpType.SCHEME_DECK,      "Scheme decks");
            _addSelectionWidget(knownDeckPanel, OpType.SEALED_DECK,      "Sealed decks");
            _addSelectionWidget(knownDeckPanel, OpType.UNKNOWN_DECK,     "Unknown decks");
            final JPanel unknownDeckPanel = new JPanel(new MigLayout("insets 0, gap 5"));
            unknownDeckPanel.setOpaque(false);
            _unknownDeckCombo = new FComboBoxWrapper<>();
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Constructed", ForgeConstants.DECK_CONSTRUCTED_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Draft",       ForgeConstants.DECK_DRAFT_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Planar",      ForgeConstants.DECK_PLANE_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Scheme",      ForgeConstants.DECK_SCHEME_DIR));
            _unknownDeckCombo.addItem(new _UnknownDeckChoice("Sealed",      ForgeConstants.DECK_SEALED_DIR));
            _unknownDeckCombo.addActionListener(arg0 -> _updateUI());
            _unknownDeckLabel = new FLabel.Builder().text("Treat unknown decks as:").build();
            unknownDeckPanel.add(_unknownDeckLabel);
            _unknownDeckCombo.addTo(unknownDeckPanel);
            knownDeckPanel.add(unknownDeckPanel, "span");
            cbPanel.add(knownDeckPanel, "aligny top");

            // add other userDir data elements
            final JPanel dataPanel = new JPanel(new MigLayout("insets 0, gap 5, wrap"));
            dataPanel.setOpaque(false);
            dataPanel.add(new FLabel.Builder().text("Other data").build());
            _addSelectionWidget(dataPanel, OpType.GAUNTLET_DATA,   "Gauntlet data");
            _addSelectionWidget(dataPanel, OpType.QUEST_DATA,      "Quest saves");
            _addSelectionWidget(dataPanel, OpType.PREFERENCE_FILE, "Preference files");
            cbPanel.add(dataPanel, "aligny top");

            // add cacheDir data elements
            final JPanel cachePanel = new JPanel(new MigLayout("insets 0, gap 5, wrap 2"));
            cachePanel.setOpaque(false);
            cachePanel.add(new FLabel.Builder().text("Cached data").build(), "wrap");
            _addSelectionWidget(cachePanel, OpType.DEFAULT_CARD_PIC, "Default card pics");
            _addSelectionWidget(cachePanel, OpType.SET_CARD_PIC,     "Set-specific card pics");
            _addSelectionWidget(cachePanel, OpType.TOKEN_PIC,        "Card token pics");
            _addSelectionWidget(cachePanel, OpType.QUEST_PIC,        "Quest-related pics");
            _addSelectionWidget(cachePanel, OpType.DB_FILE,          "Database files", true, null, "wrap");

            _addSelectionWidget(cachePanel, OpType.POSSIBLE_SET_CARD_PIC,
                    "Import possible set pics from as-yet unsupported cards", false,
                    "<html>Picture files that are not recognized as belonging to any known card.<br>" +
                            "It could be that these pictures belong to cards that are not yet supported<br>" +
                            "by Forge.  If you know this to be the case and want the pictures imported for<br>" +
                            "future use, select this option.<html>", "span");
            cbPanel.add(cachePanel, "aligny top");
            _selectionPanel.add(cbPanel, "center");

            // add move/copy and overwrite checkboxes
            final JPanel ioOptionPanel = new JPanel(new MigLayout("insets 0, gap 10"));
            ioOptionPanel.setOpaque(false);
            _moveCheckbox = new FCheckBox("Remove source files after copy");
            _moveCheckbox.setToolTipText("Move files into the data directories instead of just copying them");
            _moveCheckbox.setSelected(isMigration);
            _moveCheckbox.addChangeListener(_stateChangedListener);
            ioOptionPanel.add(_moveCheckbox);
            _overwriteCheckbox = new FCheckBox("Overwrite files in destination");
            _overwriteCheckbox.setToolTipText("Overwrite existing data with the imported data");
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
            _operationLogScroller = new JScrollPane(_operationLog);
            _operationLogScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            new SmartScroller(_operationLogScroller).attach();
            _selectionPanel.add(_operationLogScroller, "w 400:400:, hmin 60, growy, growx");

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

        private void _addSelectionWidget(final JPanel parent, final OpType type, final String name) {
            _addSelectionWidget(parent, type, name, true, null, null);
        }

        private void _addSelectionWidget(final JPanel parent, final OpType type, final String name, final boolean selected,
                final String tooltip, final String constraints) {
            final FCheckBox cb = new FCheckBox();
            cb.setName(name);
            cb.setSelected(selected);
            cb.setToolTipText(tooltip);
            cb.addChangeListener(_stateChangedListener);

            // use a skip list map instead of a regular hashmap so that the files are sorted
            // alphabetically in the logs.  note that this is a concurrent data structure
            // since it will be modified and read simultaneously by different threads
            _selections.put(type, Pair.of(cb, new ConcurrentSkipListMap<>()));
            parent.add(cb, constraints);
        }

        // must be called from GUI event loop thread
        private void _updateUI() {
            // update checkbox text labels with current totals
            final Set<OpType> selectedOptions = new HashSet<>();
            for (final Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                final Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                final FCheckBox cb = selection.getLeft();

                if (cb.isSelected()) {
                    selectedOptions.add(entry.getKey());
                }

                cb.setText(TextUtil.concatWithSpace(cb.getName(), TextUtil.enclosedParen(String.valueOf(selection.getRight().size()))));
            }

            // asynchronously update the text in the op log, which may be many tens of thousands of lines long
            // if this were done synchronously the UI would slow to a crawl
            _operationLogUpdater.requestUpdate(selectedOptions, _unknownDeckCombo.getSelectedItem(),
                    _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
        }

        @Override
        protected Void doInBackground() {
            Timer timer = null;

            try {
                final Map<OpType, Map<File, File>> selections = new HashMap<>();
                for (final Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                    selections.put(entry.getKey(), entry.getValue().getRight());
                }

                final ImportSourceAnalyzer.AnalysisCallback cb = new ImportSourceAnalyzer.AnalysisCallback() {
                    @Override public boolean checkCancel() {
                        return _cancel;
                    }

                    @Override public void addOp(final OpType type, final File src, final File dest) {
                        // add to concurrent map
                        _selections.get(type).getRight().put(src, dest);
                    }
                };

                final ImportSourceAnalyzer msa = new ImportSourceAnalyzer(_srcDir, cb);
                final int numFilesToAnalyze = msa.getNumFilesToAnalyze();

                // update only once every half-second so we're not flooding the UI with updates
                timer = new Timer(500, null);
                timer.setInitialDelay(100);
                final Timer finalTimer = timer;
                timer.addActionListener(arg0 -> {
                    if (_cancel) {
                        finalTimer.stop();
                        return;
                    }

                    // timers run in the gui event loop, so it's ok to interact with widgets
                    _progressBar.setValue(msa.getNumFilesAnalyzed());
                    _updateUI();

                    // allow the the panel to resize to accommodate additional text
                    _selectionPanel.getParent().validate();
                    _selectionPanel.getParent().invalidate();
                });

                // update the progress bar widget from the GUI event loop
                SwingUtilities.invokeLater(() -> {
                    if (_cancel) { return; }
                    _progressBar.setString("Analyzing...");
                    _progressBar.setMaximum(numFilesToAnalyze);
                    _progressBar.setValue(0);
                    _progressBar.setIndeterminate(false);

                    // start update timer
                    finalTimer.start();
                });

                // does not return until analysis is complete or has been canceled
                msa.doAnalysis();
            } catch (final Exception e) {
                _cancel = true;

                SwingUtilities.invokeLater(() -> {
                    _progressBar.setString("Error");
                    BugReporter.reportException(e);
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
                for (final ActionListener a : _btnStart.getActionListeners()) {
                    _btnStart.removeActionListener(a);
                }

                // deselect and disable all options that have 0 operations associated with
                // them to highlight the important options
                for (final Pair<FCheckBox, ? extends Map<File, File>> p : _selections.values()) {
                    final FCheckBox cb = p.getLeft();
                    if (0 == p.getRight().size()) {
                        cb.removeChangeListener(_stateChangedListener);
                        cb.setSelected(false);
                        cb.setEnabled(false);
                    }
                }

                if (0 == _selections.get(OpType.UNKNOWN_DECK).getRight().size()) {
                    _unknownDeckLabel.setEnabled(false);
                    _unknownDeckCombo.setEnabled(false);
                }

                // set up the start button to start the prepared import on click
                _btnStart.addActionListener(arg0 -> {
                    // if this is a migration, warn if active settings will not complete a migration and give the
                    // user an option to fix
                    if (_isMigration) {
                        // assemble a list of selections that need to be selected to complete a full migration
                        final List<String> unselectedButShouldBe = new ArrayList<>();
                        for (final Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : _selections.entrySet()) {
                            if (OpType.POSSIBLE_SET_CARD_PIC == entry.getKey()) {
                                continue;
                            }

                            // add name to list if checkbox is unselected, but contains operations
                            final Pair<FCheckBox, ? extends Map<File, File>> p = entry.getValue();
                            final FCheckBox cb = p.getLeft();
                            if (!cb.isSelected() && 0 < p.getRight().size()) {
                                unselectedButShouldBe.add(cb.getName());
                            }
                        }

                        if (!unselectedButShouldBe.isEmpty() || !_moveCheckbox.isSelected()) {
                            final StringBuilder sb = new StringBuilder("<html>");
                            if (!unselectedButShouldBe.isEmpty()) {
                                sb.append("It looks like the following options are not selected, which will result in an incomplete migration:");
                                sb.append("<ul>");
                                for (final String cbName : unselectedButShouldBe) {
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

                            final int chosen = FOptionPane.showOptionDialog(sb.toString(), "Migration warning", FOptionPane.WARNING_ICON, fixOrContinue);

                            if (chosen != 1) {
                                // i.e. option 0 was chosen or the dialog was otherwise closed
                                return;
                            }
                        }
                    }

                    // ensure no other actions (except for cancel) can be taken while the import is in progress
                    _btnStart.setEnabled(false);
                    _btnChooseDir.setEnabled(false);

                    for (final Pair<FCheckBox, ? extends Map<File, File>> selection : _selections.values()) {
                        selection.getLeft().setEnabled(false);
                    }
                    _unknownDeckCombo.setEnabled(false);
                    _moveCheckbox.setEnabled(false);
                    _overwriteCheckbox.setEnabled(false);

                    // stop updating the operation log -- the importer needs it now
                    _operationLogUpdater.requestStop();

                    // jump to the bottom of the log text area so it starts autoscrolling again
                    // note that since it is controlled by a SmartScroller, just setting the caret position will not work
                    final JScrollBar scrollBar = _operationLogScroller.getVerticalScrollBar();
                    scrollBar.setValue(scrollBar.getMaximum());

                    // start importing!
                    final _Importer importer = new _Importer(
                            _srcDir, _selections, _unknownDeckCombo, _operationLog, _progressBar,
                            _moveCheckbox.isSelected(), _overwriteCheckbox.isSelected());
                    importer.run();

                    _btnCancel.requestFocusInWindow();
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

        // only accessed from the event loop thread
        int _maxLogLength = 0;

        public _OperationLogAsyncUpdater(final Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> selections, final JTextArea operationLog) {
            super("OperationLogUpdater");
            setDaemon(true);

            _selections   = new HashMap<>();
            _operationLog = operationLog;

            // remove references to FCheckBox when populating map -- we can't safely access it from a thread
            // anyway and it's better to keep our data structure clean to prevent mistakes
            for (final Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                _selections.put(entry.getKey(), entry.getValue().getRight());
            }
        }

        // updates the synchronized data with values for the next iteration in _run
        public synchronized void requestUpdate(final Set<OpType> selectedOptions, final _UnknownDeckChoice unknownDeckChoice, final boolean isMove, final boolean isOverwrite) {
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
                    // can't check _stop in the while condition since we have to do it in a synchronized block
                    if (_stop) { break; }

                    // if we're stopped while looping here, run through the update one last time
                    // before returning
                    while (lastUpdateCallCnt == _updateCallCnt && !_stop) {
                        wait();
                    }

                    // safely copy synchronized data to local values that we will use for this runthrough
                    lastUpdateCallCnt = _updateCallCnt;
                    selectedOptions   = _selectedOptions;
                    unknownDeckChoice = _unknownDeckChoice;
                    isMove            = _isMove;
                    isOverwrite       = _isOverwrite;
                }

                // build operation log
                final StringBuilder log = new StringBuilder();
                int totalOps = 0;
                for (final OpType opType : selectedOptions) {
                    final Map<File, File> ops = _selections.get(opType);
                    totalOps += ops.size();

                    for (final Map.Entry<File, File> op : ops.entrySet()) {
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

                SwingUtilities.invokeLater(() -> {
                    final String logText = log.toString();

                    // setText is thread-safe, but the resizing is not, so might as well do this in the swing event loop thread
                    _operationLog.setText(log.toString());

                    if (_maxLogLength < logText.length()) {
                        _maxLogLength = logText.length();

                        // resize the panel properly for the new log contents
                        _selectionPanel.getParent().validate();
                        _selectionPanel.getParent().invalidate();
                        _topPanel.getParent().validate();
                        _topPanel.getParent().invalidate();
                    }
                });
            }
        }

        @Override
        public void run() {
            try { _run(); } catch (final InterruptedException e) {
                _cancel = true;
                // we never interrupt the thread, so this is not expected to happen
                SwingUtilities.invokeLater(() -> BugReporter.reportException(e));
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

        public _Importer(final String srcDir, final Map<OpType, Pair<FCheckBox, ? extends Map<File, File>>> selections, final FComboBoxWrapper<_UnknownDeckChoice> unknownDeckCombo,
                final JTextArea operationLog, final JProgressBar progressBar, final boolean move, final boolean overwrite) {
            _srcDir       = srcDir;
            _operationLog = operationLog;
            _progressBar  = progressBar;
            _move         = move;
            _overwrite    = overwrite;

            // build local operations map that only includes data that we can access from the background thread
            // use a tree map to maintain alphabetical order
            _operations = new TreeMap<>();
            for (final Map.Entry<OpType, Pair<FCheckBox, ? extends Map<File, File>>> entry : selections.entrySet()) {
                final Pair<FCheckBox, ? extends Map<File, File>> selection = entry.getValue();
                if (selection.getLeft().isSelected()) {
                    if (OpType.UNKNOWN_DECK != entry.getKey()) {
                        _operations.putAll(selection.getRight());
                    } else {
                        // map unknown decks to selected directory
                        for (final Map.Entry<File, File> op : selection.getRight().entrySet()) {
                            final _UnknownDeckChoice choice = unknownDeckCombo.getSelectedItem();
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
        protected Void doInBackground() {
            try {
                // working with textbox text is thread safe
                _operationLog.setText("");

                // only update the text box once very half second, but make the first
                // update after only 100ms
                final long updateIntervalMs = 500;
                long lastUpdateTimestampMs = System.currentTimeMillis() - 400;
                final StringBuffer opLogBuf = new StringBuffer();

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
                for (final Map.Entry<File, File> op : _operations.entrySet()) {
                    if (_cancel) { break; }

                    final int curOpNum = ++numOps;
                    if (0 == curOpNum % progressInterval) {
                        SwingUtilities.invokeLater(() -> {
                            if (_cancel) { return; }
                            _progressBar.setValue(curOpNum);
                        });
                    }

                    final long curTimeMs = System.currentTimeMillis();
                    if (updateIntervalMs <= curTimeMs - lastUpdateTimestampMs) {
                        lastUpdateTimestampMs = curTimeMs;

                        // working with textbox text is thread safe
                        _operationLog.append(opLogBuf.toString());
                        opLogBuf.setLength(0);
                    }

                    final File srcFile  = op.getKey();
                    final File destFile = op.getValue();

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
                            _copyFile(srcFile, destFile, _move);
                        } else {
                            if (_overwrite) {
                                opLogBuf.append("  Destination file exists; overwriting\n");
                                _copyFile(srcFile, destFile, _move);
                            } else {
                                opLogBuf.append("  Destination file exists; skipping copy\n");
                            }
                            ++numExisting;
                        }

                        if (_move) {
                            // source file may have been deleted already if _copyFile was called
                            srcFile.delete();
                            opLogBuf.append("  Removed source file after successful copy\n");
                        }

                        ++numSucceeded;
                    } catch (final IOException e) {
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
                SwingUtilities.invokeLater(() -> {
                    _progressBar.setString("Error");
                    BugReporter.reportException(e);
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

    // when copying is required, uses java nio classes for ultra-fast I/O
    private static void _copyFile(final File srcFile, final File destFile, final boolean deleteSrcAfter) throws IOException {
        destFile.getParentFile().mkdirs();

        // if this is a move, try a simple rename first
        if (deleteSrcAfter) {
            if (srcFile.renameTo(destFile)) {
                return;
            }
        }

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileInputStream srcStream = new FileInputStream(srcFile);
             FileChannel src = srcStream.getChannel();
             FileOutputStream destStream = new FileOutputStream(destFile);
             FileChannel dest = destStream.getChannel()
             ) {
            dest.transferFrom(src, 0, src.size());
        }

        if (deleteSrcAfter) {
            srcFile.delete();
        }
    }
}
