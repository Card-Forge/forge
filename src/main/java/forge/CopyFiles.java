package forge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * CopyFiles class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CopyFiles extends SwingWorker<Void, Integer> implements NewConstants {

    private List<File> FileList;

    /** The j lb. */
    JLabel jLb;

    /** The j b. */
    JProgressBar jB;

    /** The j check. */
    JCheckBox jCheck;

    /** The j source. */
    JButton jSource;

    /** The count. */
    int count;

    /**
     * <p>
     * Constructor for CopyFiles.
     * </p>
     * 
     * @param FileList
     *            a {@link java.util.List} object.
     * @param jLabelTotalFiles
     *            a {@link javax.swing.JLabel} object.
     * @param Jbar
     *            a {@link javax.swing.JProgressBar} object.
     * @param jCheckBox
     *            a {@link javax.swing.JCheckBox} object.
     * @param jButtonSource
     *            a {@link javax.swing.JButton} object.
     */
    public CopyFiles(final List<File> FileList, final JLabel jLabelTotalFiles, final JProgressBar Jbar, final JCheckBox jCheckBox,
            final JButton jButtonSource) {
        this.FileList = FileList;
        jLb = jLabelTotalFiles;
        jB = Jbar;
        jCheck = jCheckBox;
        jSource = jButtonSource;
    }

    /** {@inheritDoc} */
    @Override
    protected final Void doInBackground() {
        for (int i = 0; i < this.FileList.size(); i++) {
            publish();
            String cName, name, source;
            name = this.FileList.get(i).getName();
            source = this.FileList.get(i).getAbsolutePath();
            cName = name.substring(0, name.length() - 8);
            cName = GuiDisplayUtil.cleanString(cName) + ".jpg";
            File sourceFile = new File(source);
            File base = ForgeProps.getFile(IMAGE_BASE);
            File reciever = new File(base, cName);
            reciever.delete();

            try {
                reciever.createNewFile();
                FileOutputStream fos = new FileOutputStream(reciever);
                FileInputStream fis = new FileInputStream(sourceFile);
                byte[] buff = new byte[32 * 1024];
                int length;
                while (fis.available() > 0) {
                    length = fis.read(buff);
                    if (length > 0) {
                        fos.write(buff, 0, length);
                    }
                }
                fos.flush();
                fis.close();
                fos.close();
                count = i * 100 / this.FileList.size() + 1;
                setProgress(count);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
        return null;

    }

    /** {@inheritDoc} */
    @Override
    protected final void done() {
        jLb.setText("All files were copied successfully.");
        jB.setIndeterminate(false);
        jCheck.setEnabled(true);
        jSource.setEnabled(true);

    }

}
