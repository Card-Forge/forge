package forge;

import java.awt.Color;
import java.awt.Dimension;
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

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * GUI_ImportPicture class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GUI_ImportPicture extends JDialog implements NewConstants {
    /** Constant <code>serialVersionUID=-4191539152208389089L</code> */
    private static final long serialVersionUID = -4191539152208389089L;
    private JPanel jContentPane = null;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;
    private JLabel jLabelSource = null;
    private JButton jButtonSource = null;
    private JPanel jPanel = null;
    private JCheckBox jCheckBox = null;
    private JButton jButtonStart = null;

    /** The frame. */
    GUI_ImportPicture frame;
    private JLabel jLabelHDDFree = null;
    private JLabel jLabelNeedSpace = null;

    /** The j label total files. */
    public JLabel jLabelTotalFiles = null;
    private List<File> listFiles;
    private ArrayList<File> fileCopyList;
    private long freeSpaceM;
    private int filesForCopy;
    private String oldText;
    private JProgressBar jProgressBar = null;

    /**
     * <p>
     * Constructor for GUI_ImportPicture.
     * </p>
     * 
     * @param owner
     *            a {@link javax.swing.JFrame} object.
     */
    public GUI_ImportPicture(final JFrame owner) {
        super(owner);
        frame = this;
        initialize();
    }

    /**
     * This method initializes this
     */
    private void initialize() {
        Dimension screen = getToolkit().getScreenSize();
        Rectangle bounds = getBounds();

        bounds.width = 400;
        bounds.height = 295;
        this.setSize(new Dimension(400, 295));
        this.setResizable(false);
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        setBounds(bounds);
        this.setModal(true);
        this.setTitle("Import Picture");
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jLabelTotalFiles = new JLabel();
            jLabelTotalFiles.setBounds(new Rectangle(15, 180, 248, 16));
            jLabelTotalFiles.setText("Total files for copying: Unknown.");
            jLabelNeedSpace = new JLabel();
            jLabelNeedSpace.setBounds(new Rectangle(15, 150, 177, 16));
            jLabelNeedSpace.setText("HDD Need Space: Unknown.");
            jLabelHDDFree = new JLabel();
            jLabelHDDFree.setBounds(new Rectangle(15, 119, 177, 16));

            File file = ForgeProps.getFile(IMAGE_BASE);

            long freeSpace = file.getFreeSpace();
            freeSpaceM = freeSpace / 1024 / 1024;

            // MiB here is not a typo; it is the unit for megabytes calculated
            // by powers of 1024 instead of 1000.
            jLabelHDDFree.setText("HDD Free Space: " + freeSpaceM + " MiB");

            jLabelSource = new JLabel();
            jLabelSource.setBounds(new Rectangle(63, 45, 267, 17));
            jLabelSource.setBorder(BorderFactory.createLineBorder(Color.black, 1));
            jLabelSource.setText("");
            jLabel1 = new JLabel();
            jLabel1.setBounds(new Rectangle(16, 45, 48, 17));
            jLabel1.setText("Source:");
            jLabel = new JLabel();
            jLabel.setBounds(new Rectangle(15, 15, 360, 19));
            jLabel.setText("Please select source directory:");
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(jLabel, null);
            jContentPane.add(jLabel1, null);
            jContentPane.add(jLabelSource, null);
            jContentPane.add(getJButtonSource(), null);
            jContentPane.add(getJPanel(), null);
            jContentPane.add(getJButtonStart(), null);
            jContentPane.add(jLabelHDDFree, null);
            jContentPane.add(jLabelNeedSpace, null);
            jContentPane.add(jLabelTotalFiles, null);
            jContentPane.add(getJProgressBar(), null);
        }
        return jContentPane;
    }

    /**
     * This method initializes jButtonSource
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonSource() {
        if (jButtonSource == null) {
            jButtonSource = new JButton();
            jButtonSource.setBounds(new Rectangle(329, 45, 47, 17));
            jButtonSource.setText("...");
            jButtonSource.addMouseListener(new java.awt.event.MouseAdapter() {
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
                    oldText = jLabelSource.getText();
                    jLabelSource.setText("Please wait...");
                    if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        FileFinder ff = new FileFinder();
                        try {
                            listFiles = ff.findFiles(chooser.getSelectedFile().toString(), ".+\\.jpg");
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        jLabelSource.setText(chooser.getSelectedFile().toString());
                        if (jCheckBox.isSelected()) {
                            filesForCopy = ff.getFilesNumber();
                            jLabelTotalFiles.setText("Total files for copying: " + filesForCopy);
                            jLabelNeedSpace.setText("HDD Need Space: " + ff.getDirectorySize() / 1024 / 1024 + " MB");
                            jProgressBar.setValue(0);
                            if ((freeSpaceM > (ff.getDirectorySize() / 1024 / 1024)) && (filesForCopy > 0)) {
                                jButtonStart.setEnabled(true);
                            }

                        } else {
                            String fName;
                            int start;
                            long filesToCopySize;
                            filesForCopy = 0;
                            filesToCopySize = 0;
                            fileCopyList = new ArrayList<File>();

                            for (int i = 0; i < listFiles.size(); i++) {

                                fName = listFiles.get(i).getName();
                                start = fName.indexOf("full");
                                fName = fName.substring(0, start - 1) + fName.substring(start + 4, fName.length() - 4);
                                fName = GuiDisplayUtil.cleanString(fName) + ".jpg";
                                File file = new File(ForgeProps.getFile(IMAGE_BASE), fName);
                                if (!file.exists()) {
                                    filesForCopy = filesForCopy + 1;
                                    filesToCopySize = filesToCopySize + listFiles.get(i).length();
                                    fileCopyList.add(listFiles.get(i));
                                }
                            }
                            jLabelTotalFiles.setText("Total files for copying: " + filesForCopy);
                            jLabelNeedSpace.setText("HDD Need Space: " + filesToCopySize / 1024 / 1024 + " MB");
                            jProgressBar.setValue(0);
                            if ((freeSpaceM > (filesToCopySize / 1024 / 1024)) && (filesForCopy > 0)) {
                                jButtonStart.setEnabled(true);
                            }
                        }

                    } else {
                        if (oldText.equals("")) {
                            jLabelSource.setText("");
                        } else {
                            jLabelSource.setText(oldText);
                        }

                    }
                }
            });
        }
        return jButtonSource;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 0, 120);
            gridBagConstraints.gridy = 0;
            jPanel = new JPanel();
            jPanel.setLayout(new GridBagLayout());
            jPanel.setBounds(new Rectangle(15, 74, 362, 31));
            jPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1));
            jPanel.add(getJCheckBox(), gridBagConstraints);
        }
        return jPanel;
    }

    /**
     * This method initializes jCheckBox
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBox() {
        if (jCheckBox == null) {
            jCheckBox = new JCheckBox();
            jCheckBox.setSelected(false);
            jCheckBox.setText("Overwriting picture in resource folder");
            jCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(final java.awt.event.MouseEvent e) {
                    jButtonStart.setEnabled(false);
                    if (jLabelSource.getText() != "") {
                        FileFinder ff = new FileFinder();
                        try {
                            listFiles = ff.findFiles(jLabelSource.getText().toString(), ".+\\.jpg");
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        if (jCheckBox.isSelected()) {
                            filesForCopy = ff.getFilesNumber();
                            jLabelTotalFiles.setText("Total files for copying: " + filesForCopy);
                            jLabelNeedSpace.setText("HDD Need Space: " + ff.getDirectorySize() / 1024 / 1024 + " MB");
                            jProgressBar.setValue(0);
                            if ((freeSpaceM > (ff.getDirectorySize() / 1024 / 1024)) && (filesForCopy > 0)) {
                                jButtonStart.setEnabled(true);
                            }
                        } else {

                            String fName;
                            int start;
                            long filesToCopySize;
                            filesForCopy = 0;
                            filesToCopySize = 0;
                            fileCopyList = new ArrayList<File>();

                            for (int i = 0; i < listFiles.size(); i++) {

                                fName = listFiles.get(i).getName();
                                start = fName.indexOf("full");
                                fName = fName.substring(0, start - 1) + fName.substring(start + 4, fName.length() - 4);
                                fName = GuiDisplayUtil.cleanString(fName) + ".jpg";
                                File file = new File(ForgeProps.getFile(IMAGE_BASE), fName);
                                if (!file.exists()) {
                                    filesForCopy = filesForCopy + 1;
                                    filesToCopySize = filesToCopySize + listFiles.get(i).length();
                                    fileCopyList.add(listFiles.get(i));
                                }
                            }
                            jLabelTotalFiles.setText("Total files for copying: " + filesForCopy);
                            jLabelNeedSpace.setText("HDD Need Space: " + filesToCopySize / 1024 / 1024 + " MB");
                            jProgressBar.setValue(0);
                            if ((freeSpaceM > (filesToCopySize / 1024 / 1024)) && (filesForCopy > 0)) {
                                jButtonStart.setEnabled(true);
                            }
                        }
                    }
                }
            });
        }
        return jCheckBox;
    }

    /**
     * This method initializes jButtonStart
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonStart() {
        if (jButtonStart == null) {
            jButtonStart = new JButton();
            jButtonStart.setEnabled(false);
            jButtonStart.setBounds(new Rectangle(136, 239, 123, 17));
            jButtonStart.setText("Import");
            // jButtonStart.addMouseListener(new CustomListener());
            jButtonStart.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {

                    if (jButtonStart.isEnabled()) {
                        jLabelTotalFiles.setText("Please wait while all files are copying.");
                        if (jCheckBox.isSelected()) {
                            jButtonStart.setEnabled(false);
                            jCheckBox.setEnabled(false);
                            jButtonSource.setEnabled(false);
                            CopyFiles cFiles = new CopyFiles(listFiles, jLabelTotalFiles, jProgressBar, jCheckBox,
                                    jButtonSource);
                            cFiles.addPropertyChangeListener(new PropertyChangeListener() {
                                @Override
                                public void propertyChange(final PropertyChangeEvent evt) {
                                    if ("progress".equals(evt.getPropertyName())) {
                                        jProgressBar.setValue((Integer) evt.getNewValue());
                                    }
                                }
                            });
                            cFiles.execute();
                        } else {
                            jButtonStart.setEnabled(false);
                            jCheckBox.setEnabled(false);
                            jButtonSource.setEnabled(false);
                            CopyFiles cFiles = new CopyFiles(fileCopyList, jLabelTotalFiles, jProgressBar, jCheckBox,
                                    jButtonSource);
                            cFiles.addPropertyChangeListener(new PropertyChangeListener() {
                                public void propertyChange(final PropertyChangeEvent evt) {
                                    if ("progress".equals(evt.getPropertyName())) {
                                        jProgressBar.setValue((Integer) evt.getNewValue());
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
        return jButtonStart;
    }

    /**
     * This method initializes jProgressBar
     * 
     * @return javax.swing.JProgressBar
     */
    private JProgressBar getJProgressBar() {
        if (jProgressBar == null) {
            jProgressBar = new JProgressBar();
            jProgressBar.setBounds(new Rectangle(15, 210, 363, 18));
            jProgressBar.setMinimum(0);
            jProgressBar.setMaximum(100);
        }
        return jProgressBar;
    }
} // @jve:decl-index=0:visual-constraint="10,10"
