/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.MouseInputAdapter;

import forge.properties.NewConstants;
import forge.util.CopyFiles;
import forge.util.FileFinder;

@SuppressWarnings("serial")
public class GuiImportPicture extends JDialog {
    private JPanel jContentPane = null;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;
    private JLabel jLabelSource = null;
    private JButton jButtonSource = null;
    private JPanel jPanel = null;
    private JCheckBox jCheckBox = null;
    private JButton jButtonStart = null;

    private JLabel jLabelHDDFree = null;
    private JLabel jLabelNeedSpace = null;

    private JLabel jLabelTotalFiles = null;
    private List<File> listFiles;
    private ArrayList<File> fileCopyList;
    private long freeSpaceM;
    private int filesForCopy;
    private String oldText;
    private JProgressBar jProgressBar = null;

    public GuiImportPicture(final JFrame owner) {
        super(owner, "Import Pictures", true);
        
        setSize(400, 295);
        setResizable(false);
        setLocationRelativeTo(null);
        setContentPane(getJContentPane());
    }

    private JPanel getJContentPane() {
        this.jLabelTotalFiles = new JLabel();
        this.jLabelTotalFiles.setBounds(new Rectangle(15, 180, 248, 16));
        this.jLabelTotalFiles.setText("Total files for copying: Unknown.");
        this.jLabelNeedSpace = new JLabel();
        this.jLabelNeedSpace.setBounds(new Rectangle(15, 150, 177, 16));
        this.jLabelNeedSpace.setText("HDD Need Space: Unknown.");
        this.jLabelHDDFree = new JLabel();
        this.jLabelHDDFree.setBounds(new Rectangle(15, 119, 177, 16));

        final File file = new File(NewConstants.CACHE_CARD_PICS_DIR);

        final long freeSpace = file.getFreeSpace();
        this.freeSpaceM = freeSpace / 1024 / 1024;

        // MiB here is not a typo; it is the unit for megabytes calculated
        // by powers of 1024 instead of 1000.
        this.jLabelHDDFree.setText("HDD Free Space: " + this.freeSpaceM + " MiB");

        this.jLabelSource = new JLabel();
        this.jLabelSource.setBounds(new Rectangle(63, 45, 267, 17));
        this.jLabelSource.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        this.jLabelSource.setText("");
        this.jLabel1 = new JLabel();
        this.jLabel1.setBounds(new Rectangle(16, 45, 48, 17));
        this.jLabel1.setText("Source:");
        this.jLabel = new JLabel();
        this.jLabel.setBounds(new Rectangle(15, 15, 360, 19));
        this.jLabel.setText("Please select source directory:");
        this.jContentPane = new JPanel();
        this.jContentPane.setLayout(null);
        this.jContentPane.add(this.jLabel, null);
        this.jContentPane.add(this.jLabel1, null);
        this.jContentPane.add(this.jLabelSource, null);
        this.jContentPane.add(this.getJButtonSource(), null);
        this.jContentPane.add(this.getJPanel(), null);
        this.jContentPane.add(this.getJButtonStart(), null);
        this.jContentPane.add(this.jLabelHDDFree, null);
        this.jContentPane.add(this.jLabelNeedSpace, null);
        this.jContentPane.add(this.jLabelTotalFiles, null);
        this.jContentPane.add(this.getJProgressBar(), null);
        return this.jContentPane;
    }

    private JButton getJButtonSource() {
        if (this.jButtonSource == null) {
            this.jButtonSource = new JButton();
            this.jButtonSource.setBounds(new Rectangle(329, 45, 47, 17));
            this.jButtonSource.setText("...");
            this.jButtonSource.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(final java.awt.event.MouseEvent e) {
                    JFileChooser chooser;
                    String choosertitle;
                    choosertitle = "Select source directory.";
                    chooser = new JFileChooser();
                    chooser.setCurrentDirectory(new java.io.File("."));
                    chooser.setDialogTitle(choosertitle);
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    chooser.setAcceptAllFileFilterUsed(false);
                    GuiImportPicture.this.oldText = GuiImportPicture.this.jLabelSource.getText();
                    GuiImportPicture.this.jLabelSource.setText("Please wait...");
                    if (chooser.showOpenDialog(GuiImportPicture.this) == JFileChooser.APPROVE_OPTION) {
                        final FileFinder ff = new FileFinder();
                        try {
                            GuiImportPicture.this.listFiles = ff.findFiles(chooser.getSelectedFile().toString(),
                                    ".+\\.jpg");
                        } catch (final Exception e2) {
                            e2.printStackTrace();
                        }
                        GuiImportPicture.this.jLabelSource.setText(chooser.getSelectedFile().toString());
                        if (GuiImportPicture.this.jCheckBox.isSelected()) {
                            GuiImportPicture.this.filesForCopy = ff.getFilesNumber();
                            GuiImportPicture.this.jLabelTotalFiles.setText("Total files for copying: "
                                    + GuiImportPicture.this.filesForCopy);
                            GuiImportPicture.this.jLabelNeedSpace.setText("HDD Need Space: "
                                    + (ff.getDirectorySize() / 1024 / 1024) + " MB");
                            GuiImportPicture.this.jProgressBar.setValue(0);
                            if ((GuiImportPicture.this.freeSpaceM > (ff.getDirectorySize() / 1024 / 1024))
                                    && (GuiImportPicture.this.filesForCopy > 0)) {
                                GuiImportPicture.this.jButtonStart.setEnabled(true);
                            }

                        } else {
                            String fName;
                            int start;
                            long filesToCopySize;
                            GuiImportPicture.this.filesForCopy = 0;
                            filesToCopySize = 0;
                            GuiImportPicture.this.fileCopyList = new ArrayList<File>();

                            for (int i = 0; i < GuiImportPicture.this.listFiles.size(); i++) {

                                fName = GuiImportPicture.this.listFiles.get(i).getName();
                                start = fName.indexOf("full");
                                fName = fName.substring(0, start - 1) + fName.substring(start + 4, fName.length() - 4);
                                //fName = GuiDisplayUtil.cleanString(fName) + ".jpg";
                                final File file = new File(NewConstants.CACHE_CARD_PICS_DIR, fName);
                                if (!file.exists()) {
                                    GuiImportPicture.this.filesForCopy = GuiImportPicture.this.filesForCopy + 1;
                                    filesToCopySize = filesToCopySize + GuiImportPicture.this.listFiles.get(i).length();
                                    GuiImportPicture.this.fileCopyList.add(GuiImportPicture.this.listFiles.get(i));
                                }
                            }
                            GuiImportPicture.this.jLabelTotalFiles.setText("Total files for copying: "
                                    + GuiImportPicture.this.filesForCopy);
                            GuiImportPicture.this.jLabelNeedSpace.setText("HDD Need Space: "
                                    + (filesToCopySize / 1024 / 1024) + " MB");
                            GuiImportPicture.this.jProgressBar.setValue(0);
                            if ((GuiImportPicture.this.freeSpaceM > (filesToCopySize / 1024 / 1024))
                                    && (GuiImportPicture.this.filesForCopy > 0)) {
                                GuiImportPicture.this.jButtonStart.setEnabled(true);
                            }
                        }

                    } else {
                        if (GuiImportPicture.this.oldText.equals("")) {
                            GuiImportPicture.this.jLabelSource.setText("");
                        } else {
                            GuiImportPicture.this.jLabelSource.setText(GuiImportPicture.this.oldText);
                        }

                    }
                }
            });
        }
        return this.jButtonSource;
    }

    private JPanel getJPanel() {
        if (this.jPanel == null) {
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 0, 120);
            gridBagConstraints.gridy = 0;
            this.jPanel = new JPanel();
            this.jPanel.setLayout(new GridBagLayout());
            this.jPanel.setBounds(new Rectangle(15, 74, 362, 31));
            this.jPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1));
            this.jPanel.add(this.getJCheckBox(), gridBagConstraints);
        }
        return this.jPanel;
    }

    private JCheckBox getJCheckBox() {
        if (this.jCheckBox == null) {
            this.jCheckBox = new JCheckBox();
            this.jCheckBox.setSelected(false);
            this.jCheckBox.setText("Overwriting picture in resource folder");
            this.jCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(final java.awt.event.MouseEvent e) {
                    GuiImportPicture.this.jButtonStart.setEnabled(false);
                    if (GuiImportPicture.this.jLabelSource.getText().equals("")) {
                        final FileFinder ff = new FileFinder();
                        try {
                            GuiImportPicture.this.listFiles = ff.findFiles(GuiImportPicture.this.jLabelSource.getText()
                                    .toString(), ".+\\.jpg");
                        } catch (final Exception e2) {
                            e2.printStackTrace();
                        }
                        if (GuiImportPicture.this.jCheckBox.isSelected()) {
                            GuiImportPicture.this.filesForCopy = ff.getFilesNumber();
                            GuiImportPicture.this.jLabelTotalFiles.setText("Total files for copying: "
                                    + GuiImportPicture.this.filesForCopy);
                            GuiImportPicture.this.jLabelNeedSpace.setText("HDD Need Space: "
                                    + (ff.getDirectorySize() / 1024 / 1024) + " MB");
                            GuiImportPicture.this.jProgressBar.setValue(0);
                            if ((GuiImportPicture.this.freeSpaceM > (ff.getDirectorySize() / 1024 / 1024))
                                    && (GuiImportPicture.this.filesForCopy > 0)) {
                                GuiImportPicture.this.jButtonStart.setEnabled(true);
                            }
                        } else {

                            String fName;
                            int start;
                            long filesToCopySize;
                            GuiImportPicture.this.filesForCopy = 0;
                            filesToCopySize = 0;
                            GuiImportPicture.this.fileCopyList = new ArrayList<File>();

                            for (int i = 0; i < GuiImportPicture.this.listFiles.size(); i++) {

                                fName = GuiImportPicture.this.listFiles.get(i).getName();
                                start = fName.indexOf("full");
                                fName = fName.substring(0, start - 1) + fName.substring(start + 4, fName.length() - 4);
                                //fName = GuiDisplayUtil.cleanString(fName) + ".jpg";
                                final File file = new File(NewConstants.CACHE_CARD_PICS_DIR, fName);
                                if (!file.exists()) {
                                    GuiImportPicture.this.filesForCopy = GuiImportPicture.this.filesForCopy + 1;
                                    filesToCopySize = filesToCopySize + GuiImportPicture.this.listFiles.get(i).length();
                                    GuiImportPicture.this.fileCopyList.add(GuiImportPicture.this.listFiles.get(i));
                                }
                            }
                            GuiImportPicture.this.jLabelTotalFiles.setText("Total files for copying: "
                                    + GuiImportPicture.this.filesForCopy);
                            GuiImportPicture.this.jLabelNeedSpace.setText("HDD Need Space: "
                                    + (filesToCopySize / 1024 / 1024) + " MB");
                            GuiImportPicture.this.jProgressBar.setValue(0);
                            if ((GuiImportPicture.this.freeSpaceM > (filesToCopySize / 1024 / 1024))
                                    && (GuiImportPicture.this.filesForCopy > 0)) {
                                GuiImportPicture.this.jButtonStart.setEnabled(true);
                            }
                        }
                    }
                }
            });
        }
        return this.jCheckBox;
    }

    private JButton getJButtonStart() {
        if (this.jButtonStart == null) {
            this.jButtonStart = new JButton();
            this.jButtonStart.setEnabled(false);
            this.jButtonStart.setBounds(new Rectangle(136, 239, 123, 17));
            this.jButtonStart.setText("Import");
            // jButtonStart.addMouseListener(new CustomListener());
            this.jButtonStart.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {

                    if (GuiImportPicture.this.jButtonStart.isEnabled()) {
                        GuiImportPicture.this.jLabelTotalFiles.setText("Please wait while all files are copying.");
                        if (GuiImportPicture.this.jCheckBox.isSelected()) {
                            GuiImportPicture.this.jButtonStart.setEnabled(false);
                            GuiImportPicture.this.jCheckBox.setEnabled(false);
                            GuiImportPicture.this.jButtonSource.setEnabled(false);
                            final CopyFiles cFiles = new CopyFiles(GuiImportPicture.this.listFiles,
                                    GuiImportPicture.this.jLabelTotalFiles, GuiImportPicture.this.jProgressBar,
                                    GuiImportPicture.this.jCheckBox, GuiImportPicture.this.jButtonSource);
                            cFiles.addPropertyChangeListener(new PropertyChangeListener() {
                                @Override
                                public void propertyChange(final PropertyChangeEvent evt) {
                                    if ("progress".equals(evt.getPropertyName())) {
                                        GuiImportPicture.this.jProgressBar.setValue((Integer) evt.getNewValue());
                                    }
                                }
                            });
                            cFiles.execute();
                        } else {
                            GuiImportPicture.this.jButtonStart.setEnabled(false);
                            GuiImportPicture.this.jCheckBox.setEnabled(false);
                            GuiImportPicture.this.jButtonSource.setEnabled(false);
                            final CopyFiles cFiles = new CopyFiles(GuiImportPicture.this.fileCopyList,
                                    GuiImportPicture.this.jLabelTotalFiles, GuiImportPicture.this.jProgressBar,
                                    GuiImportPicture.this.jCheckBox, GuiImportPicture.this.jButtonSource);
                            cFiles.addPropertyChangeListener(new PropertyChangeListener() {
                                @Override
                                public void propertyChange(final PropertyChangeEvent evt) {
                                    if ("progress".equals(evt.getPropertyName())) {
                                        GuiImportPicture.this.jProgressBar.setValue((Integer) evt.getNewValue());
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
        return this.jButtonStart;
    }

    private JProgressBar getJProgressBar() {
        if (this.jProgressBar == null) {
            this.jProgressBar = new JProgressBar();
            this.jProgressBar.setBounds(new Rectangle(15, 210, 363, 18));
            this.jProgressBar.setMinimum(0);
            this.jProgressBar.setMaximum(100);
        }
        return this.jProgressBar;
    }
}
