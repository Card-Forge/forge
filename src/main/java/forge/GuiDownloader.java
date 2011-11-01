package forge;

import static java.lang.Integer.parseInt;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.esotericsoftware.minlog.Log;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * GuiDownloadQuestImages class.
 * </p>
 * 
 * @author Forge
 */
public abstract class GuiDownloader extends DefaultBoundedRangeModel implements Runnable, NewConstants,
        NewConstants.Lang.GuiDownloadPictures {

    private static final long serialVersionUID = -8596808503046590349L;

    /** Constant <code>types</code>. */
    public static final Proxy.Type[] TYPES = Proxy.Type.values();

    // proxy
    /** The type. */
    private int type;

    /** The port. */
    private JTextField addr, port;

    // progress
    /** The cards. */
    private DownloadObject[] cards;

    /** The card. */
    private int card;

    /** The cancel. */
    private boolean cancel;

    /** The bar. */
    private JProgressBar bar;

    /** The dlg. */
    private JOptionPane dlg;

    /** The close. */
    private JButton close;

    /** The times. */
    private long[] times = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    /** The tptr. */
    private int tptr = 0;

    /** The l time. */
    private long lTime = System.currentTimeMillis();

    /**
     * <p>
     * getAverageTimePerObject.
     * </p>
     * 
     * @return a int.
     */
    protected final int getAverageTimePerObject() {
        int aTime = 0;
        int nz = 10;

        if (tptr > 9) {
            tptr = 0;
        }

        times[tptr] = System.currentTimeMillis() - lTime;
        lTime = System.currentTimeMillis();

        int tTime = 0;
        for (int i = 0; i < 10; i++) {
            tTime += times[i];
            if (times[i] == 0) {
                nz--;
            }
        }
        aTime = tTime / nz;

        tptr++;

        return aTime;
    }

    /**
     * <p>
     * Constructor for GuiDownloader.
     * </p>
     * 
     * @param frame
     *            a {@link javax.swing.JFrame} object.
     */
    protected GuiDownloader(final JFrame frame) {

        cards = getNeededImages();

        if (cards.length == 0) {
            JOptionPane.showMessageDialog(frame, ForgeProps.getLocalized(NO_MORE));
            return;
        }

        addr = new JTextField(ForgeProps.getLocalized(PROXY_ADDRESS));
        port = new JTextField(ForgeProps.getLocalized(PROXY_PORT));
        bar = new JProgressBar(this);

        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));

        // Proxy Choice
        ButtonGroup bg = new ButtonGroup();
        String[] labels = { ForgeProps.getLocalized(NO_PROXY), ForgeProps.getLocalized(HTTP_PROXY),
                ForgeProps.getLocalized(SOCKS_PROXY) };
        for (int i = 0; i < TYPES.length; i++) {
            JRadioButton rb = new JRadioButton(labels[i]);
            rb.addChangeListener(new ProxyHandler(i));
            bg.add(rb);
            p0.add(rb);
            if (i == 0) {
                rb.setSelected(true);
            }
        }

        // Proxy config
        p0.add(addr);
        p0.add(port);

        // Start
        final JButton b = new JButton(ForgeProps.getLocalized(Buttons.START));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                new Thread(GuiDownloader.this).start();
                b.setEnabled(false);
            }
        });

        p0.add(Box.createVerticalStrut(5));

        // Progress
        p0.add(bar);
        bar.setStringPainted(true);
        // bar.setString(ForgeProps.getLocalized(BAR_BEFORE_START));
        bar.setString(card + "/" + cards.length);
        // bar.setString(String.format(ForgeProps.getLocalized(card ==
        // cards.length? BAR_CLOSE:BAR_WAIT), this.card, cards.length));
        Dimension d = bar.getPreferredSize();
        d.width = 300;
        bar.setPreferredSize(d);

        // JOptionPane
        close = new JButton(ForgeProps.getLocalized(Buttons.CANCEL));
        Object[] options = { b, close };
        dlg = new JOptionPane(p0, DEFAULT_OPTION, PLAIN_MESSAGE, null, options, options[1]);

        JDialog jdlg = getDlg(frame);
        jdlg.setVisible(true);
        jdlg.dispose();
        setCancel(true);
    }

    /** {@inheritDoc} */
    @Override
    public final int getMinimum() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int getValue() {
        return card;
    }

    /** {@inheritDoc} */
    @Override
    public final int getExtent() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int getMaximum() {
        return cards == null ? 0 : cards.length;
    }

    /**
     * <p>
     * update.
     * </p>
     * 
     * @param card
     *            a int.
     */
    private void update(final int card) {
        this.card = card;

        /**
         * 
         * TODO: Write javadoc for this type.
         *
         */
        final class Worker implements Runnable {
            private int card;

            /**
             * 
             * TODO: Write javadoc for Constructor.
             * @param card int
             */
            Worker(final int card) {
                this.card = card;
            }

            /**
             * 
             */
            public void run() {
                fireStateChanged();

                StringBuilder sb = new StringBuilder();

                int a = getAverageTimePerObject();

                if (card != cards.length) {
                    sb.append(card + "/" + cards.length + " - ");

                    long t2Go = (cards.length - card) * a;

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
                    sb.append(String.format(ForgeProps.getLocalized(BAR_CLOSE), card, cards.length));
                }

                bar.setString(sb.toString());
                System.out.println(card + "/" + cards.length + " - " + a);
            }
        }
        EventQueue.invokeLater(new Worker(card));
    }

    /**
     * <p>
     * Getter for the field <code>dlg</code>.
     * </p>
     * 
     * @param frame
     *            a {@link javax.swing.JFrame} object.
     * @return a {@link javax.swing.JDialog} object.
     */
    private JDialog getDlg(final JFrame frame) {
        final JDialog dlg = this.dlg.createDialog(frame, ForgeProps.getLocalized(TITLE));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                dlg.setVisible(false);
            }
        });
        return dlg;
    }

    /**
     * <p>
     * Setter for the field <code>cancel</code>.
     * </p>
     * 
     * @param cancel
     *            a boolean.
     */
    public final void setCancel(final boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * <p>
     * run.
     * </p>
     */
    public final void run() {
        BufferedInputStream in;
        BufferedOutputStream out;

        Random r = MyRandom.getRandom();

        Proxy p = null;
        if (type == 0) {
            p = Proxy.NO_PROXY;
        } else {
            try {
                p = new Proxy(TYPES[type], new InetSocketAddress(addr.getText(), parseInt(port.getText())));
            } catch (Exception ex) {
                ErrorViewer
                        .showError(ex, ForgeProps.getLocalized(Errors.PROXY_CONNECT), addr.getText(), port.getText());
                return;
            }
        }

        if (p != null) {
            byte[] buf = new byte[1024];
            int len;
            for (update(0); card < cards.length && !cancel; update(card + 1)) {
                try {
                    String url = cards[card].url;
                    String cName;
                    cName = cards[card].getName();
                    cName = cName.replace("%20", " ");

                    File base = new File(cards[card].dir);
                    File f = new File(base, cName);

                    // test for folder existence
                    if (!base.exists()) {
                        // create folder
                        if (!base.mkdir()) {
                            System.out.println("Can't create folder" + cards[card].dir);
                        }
                    }

                    try {
                        in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
                        out = new BufferedOutputStream(new FileOutputStream(f));

                        while ((len = in.read(buf)) != -1) {
                            // user cancelled
                            if (cancel) {
                                in.close();
                                out.flush();
                                out.close();

                                // delete what was written so far
                                f.delete();

                                return;
                            } // if - cancel

                            out.write(buf, 0, len);
                        } // while - read and write file

                        in.close();
                        out.flush();
                        out.close();
                    } catch (ConnectException ce) {
                        System.out.println("Connection refused for url: " + url);
                    } catch (MalformedURLException mURLe) {
                        System.out.println("Error - possibly missing URL for: " + cards[card].getName());
                    }
                } catch (FileNotFoundException fnfe) {
                    System.out.println("Error - the LQ picture for " + cards[card].getName()
                            + " could not be found on the server. [" + cards[card].url + "] - " + fnfe.getMessage());
                } catch (Exception ex) {
                    Log.error("LQ Pictures", "Error downloading pictures", ex);
                }

                // throttle
                try {
                    Thread.sleep(r.nextInt(750) + 420);
                } catch (InterruptedException e) {
                    Log.error("GuiDownloader", "Sleep Error", e);
                }
            } // for
        }
        close.setText(ForgeProps.getLocalized(Buttons.CLOSE));
    } // run

    /**
     * <p>
     * getNeededCards.
     * </p>
     * 
     * @return an array of {@link forge.GuiDownloader.DownloadObject} objects.
     */
    protected abstract DownloadObject[] getNeededImages();

    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @param dir
     *            a {@link java.util.File} object.
     * @return an array of {@link forge.GuiDownloader.DownloadObject} objects.
     */
    protected static DownloadObject[] readFile(final String filename, final File dir) {
        try {
            FileReader zrc = new FileReader(ForgeProps.getFile(filename));
            BufferedReader in = new BufferedReader(zrc);
            ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();

            String line;
            StringTokenizer tok;

            line = in.readLine();
            while (line != null && (!line.equals("")) && !line.startsWith("#")) {
                tok = new StringTokenizer(line, "/");

                // Maybe there's a better way to do this, but I just want the
                // filename from a URL
                String last = null;
                while (tok.hasMoreTokens()) {
                    last = tok.nextToken();
                }
                list.add(new DownloadObject(last, line, dir.getPath()));

                line = in.readLine();
            }

            DownloadObject[] out = new DownloadObject[list.size()];
            list.toArray(out);
            return out;

        } catch (Exception ex) {
            ErrorViewer.showError(ex, "GuiDownloader: readFile() error");
            throw new RuntimeException("GuiDownloader : readFile() error");
        }
    } // readFile()

    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @param dir
     *            a {@link java.util.File} object.
     * @return an array of {@link forge.GuiDownloader.DownloadObject} objects.
     */
    protected static DownloadObject[] readFileWithNames(final String filename, final File dir) {
        try {
            FileReader zrc = new FileReader(ForgeProps.getFile(filename));
            BufferedReader in = new BufferedReader(zrc);
            ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();

            String line;
            StringTokenizer tok;

            line = in.readLine();
            while (line != null && (!line.equals(""))) {
                if (line.startsWith("#")) {
                    line = in.readLine();
                    continue;
                }

                String name = null;
                String url = null;
                tok = new StringTokenizer(line, " ");

                if (tok.hasMoreTokens()) {
                    name = tok.nextToken();
                }
                if (tok.hasMoreTokens()) {
                    url = tok.nextToken();
                }
                list.add(new DownloadObject(name, url, dir.getPath()));

                line = in.readLine();
            }

            DownloadObject[] out = new DownloadObject[list.size()];
            list.toArray(out);
            return out;

        } catch (Exception ex) {
            ErrorViewer.showError(ex, "GuiDownloader: readFile() error");
            throw new RuntimeException("GuiDownloader : readFile() error");
        }
    } // readFile()

    /**
     * The Class ProxyHandler.
     */
    protected class ProxyHandler implements ChangeListener {
        private int type;

        /**
         * Instantiates a new proxy handler.
         * 
         * @param type
         *            the type
         */
        public ProxyHandler(final int type) {
            this.type = type;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.
         * ChangeEvent)
         */
        /**
         * @param e ChangeEvent
         */
        public final void stateChanged(final ChangeEvent e) {
            if (((AbstractButton) e.getSource()).isSelected()) {
                GuiDownloader.this.type = type;
                addr.setEnabled(type != 0);
                port.setEnabled(type != 0);
            }
        }
    }

    /**
     * The Class DownloadObject.
     */
    protected static class DownloadObject {

        /** The name. */
        private final String name;

        /** The url. */
        private final String url;

        /** The dir. */
        private final String dir;

        /**
         * Instantiates a new download object.
         * 
         * @param nameIn
         *            the name in
         * @param urlIn
         *            the url in
         * @param dirIn
         *            the dir in
         */
        DownloadObject(final String nameIn, final String urlIn, final String dirIn) {
            name = nameIn;
            url = urlIn;
            dir = dirIn;
            // System.out.println("Created download object: "+name+" "+url+" "+dir);
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
    } // DownloadObject
}
