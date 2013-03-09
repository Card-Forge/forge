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
package forge.gui.download;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.esotericsoftware.minlog.Log;

import forge.Command;
import forge.error.BugReporter;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FProgressBar;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.JXButtonPanel;
import forge.util.FileUtil;
import forge.util.MyRandom;

@SuppressWarnings("serial")
public abstract class GuiDownloader extends DefaultBoundedRangeModel implements Runnable {
    public static final Proxy.Type[] TYPES = Proxy.Type.values(); /** */

    // Actions and commands
    private final ActionListener actStartDownload = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            new Thread(GuiDownloader.this).start();
            btnStart.setEnabled(false);
        }
    };

    private final ActionListener actOK = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            close();
        }
    };

    private final Command cmdClose = new Command() { @Override
        public void execute() { close(); } };

    // Swing components
    private final FPanel pnlDialog = new FPanel(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
    private final FProgressBar barProgress = new FProgressBar();
    private final FButton btnStart = new FButton("Start");
    private final JTextField txfAddr = new JTextField("Proxy Address");
    private final JTextField txfPort = new JTextField("Proxy Port");

    private final FLabel btnClose = new FLabel.Builder().text("X")
            .hoverable(true).fontAlign(SwingConstants.CENTER).cmdClick(cmdClose).build();

    private final JRadioButton radProxyNone = new FRadioButton("No Proxy");
    private final JRadioButton radProxySocks = new FRadioButton("SOCKS Proxy");
    private final JRadioButton radProxyHTTP = new FRadioButton("HTTP Proxy");

    // Proxy info
    private int type;

    // Progress variables
    private ArrayList<DownloadObject> cards;
    private int card;
    private boolean cancel;
    private final long[] times = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private int tptr = 0;
    private long lTime = System.currentTimeMillis();

    protected GuiDownloader() {
        String radConstraints = "w 100%!, h 30px!, gap 2% 0 0 10px";
        JXButtonPanel grpPanel = new JXButtonPanel();
        grpPanel.add(radProxyNone, radConstraints);
        grpPanel.add(radProxyHTTP, radConstraints);
        grpPanel.add(radProxySocks, radConstraints);

        radProxyNone.addChangeListener(new ProxyHandler(0));
        radProxyHTTP.addChangeListener(new ProxyHandler(1));
        radProxySocks.addChangeListener(new ProxyHandler(2));
        radProxyNone.setSelected(true);

        btnClose.setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_TEXT), 1));
        btnStart.setFont(FSkin.getFont(18));
        btnStart.setVisible(false);

        barProgress.reset();
        barProgress.setString("Scanning for existing items...");
        pnlDialog.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));

        // Layout
        pnlDialog.add(grpPanel, "w 50%!");
        pnlDialog.add(txfAddr, "w 95%!, h 30px!, gap 2% 0 0 10px");
        pnlDialog.add(txfPort, "w 95%!, h 30px!, gap 2% 0 0 10px");
        pnlDialog.add(barProgress, "w 95%!, h 40px!, gap 2% 0 20px 0");
        pnlDialog.add(btnStart, "w 200px!, h 40px!, gap 0 0 20px 0, ax center");
        pnlDialog.add(btnClose, "w 20px!, h 20px!, pos 370px 10px");

        final JPanel pnl = FOverlay.SINGLETON_INSTANCE.getPanel();
        pnl.removeAll();
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        pnl.add(pnlDialog, "w 400px!, h 350px!, ax center, ay center");
        SOverlayUtils.showOverlay();

        // Free up the EDT by assembling card list in the background
        SwingWorker<Void, Void> thrGetImages = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    GuiDownloader.this.cards = GuiDownloader.this.getNeededImages();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                GuiDownloader.this.readyToStart();
            }
        };

        thrGetImages.execute();
    }

    private void readyToStart() {
        if (this.cards.size() == 0) {
            barProgress.setString("All items have been downloaded.");
            btnStart.setVisible(true);
            btnStart.setText("OK");
            btnStart.addActionListener(actOK);
        }
        else {
            barProgress.setMaximum(this.cards.size());
            barProgress.setString(
                    this.cards.size() == 1 ? "1 item found." : this.cards.size() + " items found.");
            btnStart.setVisible(true);
            btnStart.addActionListener(actStartDownload);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                btnStart.requestFocusInWindow();
            }
        });
    }

    private void setCancel(final boolean cancel) {
        this.cancel = cancel;
    }

    private void close() {
        setCancel(true);

        // Kill overlay
        SOverlayUtils.hideOverlay();
        FOverlay.SINGLETON_INSTANCE.getPanel().removeAll();
    }

    protected final int getAverageTimePerObject() {
        int numNonzero = 10;

        if (this.tptr > 9) {
            this.tptr = 0;
        }

        this.times[this.tptr] = System.currentTimeMillis() - this.lTime;
        this.lTime = System.currentTimeMillis();

        int tTime = 0;
        for (int i = 0; i < 10; i++) {
            tTime += this.times[i];
            if (this.times[i] == 0) {
                numNonzero--;
            }
        }
        
        this.tptr++;
        return tTime / Math.max(1, numNonzero);
    }

    private void update(final int card) {
        this.card = card;

        final class Worker implements Runnable {
            private final int card;

            Worker(final int card) {
                this.card = card;
            }

            /**
             * 
             */
            @Override
            public void run() {
                GuiDownloader.this.fireStateChanged();

                final StringBuilder sb = new StringBuilder();

                final int a = GuiDownloader.this.getAverageTimePerObject();

                if (this.card != GuiDownloader.this.cards.size()) {
                    sb.append(this.card + "/" + GuiDownloader.this.cards.size() + " - ");

                    long t2Go = (GuiDownloader.this.cards.size() - this.card) * a;

                    boolean secOnly = true;
                    if (t2Go > 3600000) {
                        sb.append(String.format("%02d:", t2Go / 3600000));
                        t2Go = t2Go % 3600000;
                        secOnly = false;
                    }
                    if (t2Go > 60000) {
                        sb.append(String.format("%02d:", t2Go / 60000));
                        t2Go = t2Go % 60000;
                        secOnly = false;
                    }
                    if (!secOnly) {
                        sb.append(String.format("%02d remaining.", t2Go / 1000));
                    } else {
                        sb.append(String.format("0:%02d remaining.", t2Go / 1000));
                    }
                } else {
                    sb.append(String.format("%d of %d items finished! Please close!",
                            this.card, GuiDownloader.this.cards.size()));
                }

                GuiDownloader.this.barProgress.setString(sb.toString());
                System.out.println(this.card + "/" + GuiDownloader.this.cards.size() + " - " + a);
            }
        }
        EventQueue.invokeLater(new Worker(card));
    }

    public final void run() {
        BufferedInputStream in;
        BufferedOutputStream out;

        final Random r = MyRandom.getRandom();

        Proxy p = null;
        if (this.type == 0) {
            p = Proxy.NO_PROXY;
        } else {
            try {
                p = new Proxy(GuiDownloader.TYPES[this.type], new InetSocketAddress(this.txfAddr.getText(),
                        Integer.parseInt(this.txfPort.getText())));
            } catch (final Exception ex) {
                BugReporter.reportException(ex,
                        "Proxy connection could not be established!\nProxy address: %s\nProxy port: %s",
                        this.txfAddr.getText(), this.txfPort.getText());
                return;
            }
        }

        if (p != null) {
            final byte[] buf = new byte[1024];
            int len;
            for (this.update(0); (this.card < this.cards.size()) && !this.cancel; this.update(this.card + 1)) {
                final String url = this.cards.get(this.card).getSource();
                final File fileDest =  this.cards.get(this.card).getDestination();
                final File base = fileDest.getParentFile();

                //System.out.println(String.format("Downloading %s to %s", url, fileDest.getPath()));
                try {
                    // test for folder existence
                    if (!base.exists() && !base.mkdir()) { // create folder if not found
                        System.out.println("Can't create folder" + base.getAbsolutePath());
                    }
                    
                    URL imageUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                    // don't allow redirections here -- they indicate 'file not found' on the server
                    conn.setInstanceFollowRedirects(false);
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        conn.disconnect();
                        System.out.println("Skipped Download for: " + fileDest.getPath());
                        continue;
                    }

                    in = new BufferedInputStream(conn.getInputStream());
                    out = new BufferedOutputStream(new FileOutputStream(fileDest));

                    while ((len = in.read(buf)) != -1) {
                        // user cancelled
                        if (this.cancel) {
                            in.close();
                            out.flush();
                            out.close();

                            // delete what was written so far
                            fileDest.delete();
                            this.close();
                            return;
                        } // if - cancel

                        out.write(buf, 0, len);
                    } // while - read and write file
                    in.close();
                    out.flush();
                    out.close();
                } catch (final ConnectException ce) {
                    System.out.println("Connection refused for url: " + url);
                } catch (final MalformedURLException mURLe) {
                    System.out.println("Error - possibly missing URL for: " + fileDest.getName());
                } catch (final FileNotFoundException fnfe) {
                    String formatStr = "Error - the LQ picture %s could not be found on the server. [%s] - %s";
                    System.out.println(String.format(formatStr, fileDest.getName(), url, fnfe.getMessage()));
                } catch (final Exception ex) {
                    Log.error("LQ Pictures", "Error downloading pictures", ex);
                }

                // throttle to reduce load on the server
                try {
                    Thread.sleep(r.nextInt(250) + 250);
                } catch (final InterruptedException e) {
                    Log.error("GuiDownloader", "Sleep Error", e);
                }
            } // for
        }
    }

    protected abstract ArrayList<DownloadObject> getNeededImages();

    protected static List<DownloadObject> readFile(final String urlsFile, String dir) {
        List<String> fileLines = FileUtil.readFile(urlsFile);
        final ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();
        final Pattern splitter = Pattern.compile(Pattern.quote("/"));
        final Pattern replacer = Pattern.compile(Pattern.quote("%20"));

        for (String line : fileLines) {

            if (line.equals("") || line.startsWith("#")) {
                continue;
            }

            String[] parts = splitter.split(line);

            // Maybe there's a better way to do this, but I just want the
            // filename from a URL
            String last = parts[parts.length - 1];
            list.add(new DownloadObject(line, new File(dir, replacer.matcher(last).replaceAll(" "))));
        }
        return list;
    }

    protected static ArrayList<DownloadObject> readFileWithNames(final String urlNamesFile, final String dir) {
        List<String> fileLines = FileUtil.readFile(urlNamesFile);
        final ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();
        final Pattern splitter = Pattern.compile(Pattern.quote(" "));
        final Pattern replacer = Pattern.compile(Pattern.quote("%20"));

        for (String line : fileLines) {

            if (StringUtils.isBlank(line) || line.startsWith("#")) {
                continue;
            }
            String[] parts = splitter.split(line, 2);
            String url = parts.length > 1 ? parts[1] : null;
            list.add(new DownloadObject(url, new File(dir, replacer.matcher(parts[0]).replaceAll(" "))));
        }

        return list;
    } // readFile()

    /**
     * The Class ProxyHandler.
     */
    protected class ProxyHandler implements ChangeListener {
        private final int type;

        public ProxyHandler(final int type) {
            this.type = type;
        }

        @Override
        public final void stateChanged(final ChangeEvent e) {
            if (((AbstractButton) e.getSource()).isSelected()) {
                GuiDownloader.this.type = this.type;
                GuiDownloader.this.txfAddr.setEnabled(this.type != 0);
                GuiDownloader.this.txfPort.setEnabled(this.type != 0);
            }
        }
    }

    /**
     * The Class DownloadObject.
     */
    protected static class DownloadObject {

        private final String source;
        private final File destination;

        /**
         * @param srcUrl {@link java.lang.String}
         * @param destFile {@link java.io.File}
         */
        DownloadObject(final String srcUrl, final File destFile) {
            source = srcUrl;
            destination = destFile;
            // System.out.println("Created download object: "+name+" "+url+" "+dir);
        }

        /** @return {@link java.lang.String} */
        public String getSource() {
            return source;
        }

        /** @return {@link java.io.File} */
        public File getDestination() {
            return destination;
        }
    }
}
