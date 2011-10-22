package forge;

import com.esotericsoftware.minlog.Log;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import static java.lang.Integer.parseInt;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;


/**
 * <p>Gui_DownloadPictures_LQ class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_DownloadPictures_LQ extends DefaultBoundedRangeModel implements Runnable, NewConstants, NewConstants.LANG.Gui_DownloadPictures {

    /** Constant <code>serialVersionUID=-7890794857949935256L</code>. */
    private static final long serialVersionUID = -7890794857949935256L;

    /** Constant <code>types</code>. */
    public static final Proxy.Type[] types = Proxy.Type.values();

    //proxy
    private int type;
    private JTextField addr, port;

    //progress
    private mCard[] cards;
    private int card;
    private boolean cancel;
    private JProgressBar bar;

    private JOptionPane dlg;
    private JButton close;

    /**
     * <p>Constructor for Gui_DownloadPictures_LQ.</p>
     *
     * @param c an array of {@link forge.Gui_DownloadPictures_LQ.mCard} objects.
     */
    private Gui_DownloadPictures_LQ(final mCard[] c) {
        this.cards = c;
        addr = new JTextField(ForgeProps.getLocalized(PROXY_ADDRESS));
        port = new JTextField(ForgeProps.getLocalized(PROXY_PORT));
        bar = new JProgressBar(this);

        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));

        //Proxy Choice
        ButtonGroup bg = new ButtonGroup();
        String[] labels = {
                ForgeProps.getLocalized(NO_PROXY), ForgeProps.getLocalized(HTTP_PROXY),
                ForgeProps.getLocalized(SOCKS_PROXY)};
        for (int i = 0; i < types.length; i++) {
            JRadioButton rb = new JRadioButton(labels[i]);
            rb.addChangeListener(new ProxyHandler(i));
            bg.add(rb);
            p0.add(rb);
            if (i == 0) {
                rb.setSelected(true);
            }
        }

        //Proxy config
        p0.add(addr);
        p0.add(port);

        //Start
        final JButton b = new JButton(ForgeProps.getLocalized(BUTTONS.START));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                new Thread(Gui_DownloadPictures_LQ.this).start();
                b.setEnabled(false);
            }
        });

        p0.add(Box.createVerticalStrut(5));

        //Progress
        p0.add(bar);
        bar.setStringPainted(true);
        bar.setString(String.format(ForgeProps.getLocalized(card == cards.length ? BAR_CLOSE : BAR_WAIT), this.card, cards.length));
        Dimension d = bar.getPreferredSize();
        d.width = 300;
        bar.setPreferredSize(d);

        //JOptionPane
        Object[] options = {b, close = new JButton(ForgeProps.getLocalized(BUTTONS.CANCEL))};
        dlg = new JOptionPane(p0, DEFAULT_OPTION, PLAIN_MESSAGE, null, options, options[1]);
    }

    /** {@inheritDoc} */
    @Override
    public int getMinimum() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getValue() {
        return card;
    }

    /** {@inheritDoc} */
    @Override
    public int getExtent() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximum() {
        return cards == null ? 0 : cards.length;
    }

    /**
     * <p>update.</p>
     *
     * @param card a int.
     */
    private void update(final int card) {
        this.card = card;
        final class Worker implements Runnable {
            private int card;

            Worker(int card) {
                this.card = card;
            }

            public void run() {
                fireStateChanged();
                bar.setString(String.format(ForgeProps.getLocalized(card == cards.length ? BAR_CLOSE : BAR_WAIT), card,
                        cards.length));
                System.out.println(card + "/" + cards.length);
            }
        }

        EventQueue.invokeLater(new Worker(card));
    }

    /**
     * <p>Getter for the field <code>dlg</code>.</p>
     *
     * @param frame a {@link javax.swing.JFrame} object.
     * @return a {@link javax.swing.JDialog} object.
     */
    public JDialog getDlg(final JFrame frame) {
        final JDialog dlg = this.dlg.createDialog(frame, ForgeProps.getLocalized(TITLE));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });
        return dlg;
    }

    /**
     * <p>Setter for the field <code>cancel</code>.</p>
     *
     * @param cancelIn a boolean.
     */
    public void setCancel(final boolean cancelIn) {
        this.cancel = cancelIn;
    }


    /**
     * <p>run.</p>
     */
    public void run() {
        BufferedInputStream in;
        BufferedOutputStream out;

        File base = ForgeProps.getFile(IMAGE_BASE);
        File tokenBase = ForgeProps.getFile(IMAGE_TOKEN);
        if (!tokenBase.exists()) {
            tokenBase.mkdirs();
        }
        
        Random r = MyRandom.random;

        Proxy p = null;
        if (type == 0) {
            p = Proxy.NO_PROXY;
        } else {
            try {
                p = new Proxy(types[type], new InetSocketAddress(addr.getText(), parseInt(port.getText())));
            } catch (Exception ex) {
                ErrorViewer.showError(ex, ForgeProps.getLocalized(ERRORS.PROXY_CONNECT), addr.getText(),
                        port.getText());
                return;
            }
        }

        if (p != null) {
            byte[] buf = new byte[1024];
            int len;
            System.out.println("basedir: " + base);
            for (update(0); card < cards.length && !cancel; update(card + 1)) {
                try {
                    String url = cards[card].url;
                    String cName;

                    if (cards[card].name.substring(0, 3).equals("[T]")) {
                        base = ForgeProps.getFile(IMAGE_TOKEN);
                        cName = cards[card].name.substring(3, cards[card].name.length());
                    } else {
                        base = ForgeProps.getFile(IMAGE_BASE);
                        cName = cards[card].name;
                    }
                    

                    File f = new File(base, cName);

                    try {
                        in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
                        out = new BufferedOutputStream(new FileOutputStream(f));

                        while ((len = in.read(buf)) != -1) {
                            //user cancelled
                            if (cancel) {
                                in.close();
                                out.flush();
                                out.close();

                                //delete what was written so far
                                f.delete();

                                return;
                            } //if - cancel

                            out.write(buf, 0, len);
                        } //while - read and write file

                        in.close();
                        out.flush();
                        out.close();
                    } catch (MalformedURLException mURLe) {
                        System.out.println("Error - possibly missing URL for: " + cards[card].name);
                    }
                } catch (FileNotFoundException fnfe) {
                    System.out.println("Error - the LQ picture for " + cards[card].name + " could not be found on the server.");
                } catch (Exception ex) {
                    System.out.println("General error - downloading LQ picture for " + cards[card].name);
                    Log.error("LQ Pictures", "Error downloading pictures", ex);
                }
                
                try {
                    Thread.sleep(r.nextInt(750) + 420);
                } catch (InterruptedException e) {
                    Log.error("LQ Set Pictures", "Sleep Error", e);
                }
            } //for
        }
        close.setText(ForgeProps.getLocalized(BUTTONS.CLOSE));
    } //run

    /**
     * <p>startDownload.</p>
     *
     * @param frame a {@link javax.swing.JFrame} object.
     */
    public static void startDownload(final JFrame frame) {
        final mCard[] card = getNeededCards();

        if (card.length == 0) {
            JOptionPane.showMessageDialog(frame, ForgeProps.getLocalized(NO_MORE));
            return;
        }

        Gui_DownloadPictures_LQ download = new Gui_DownloadPictures_LQ(card);
        JDialog dlg = download.getDlg(frame);
        dlg.setVisible(true);
        dlg.dispose();
        download.setCancel(true);
    } //startDownload()

    /**
     * <p>getNeededCards.</p>
     *
     * @return an array of {@link forge.Gui_DownloadPictures_LQ.mCard} objects.
     */
    private static mCard[] getNeededCards() {
        //read token names and urls
        mCard[] cardTokenLQ = readFile(TOKEN_IMAGES);

        ArrayList<mCard> cList = new ArrayList<mCard>();

        for (Card c : AllZone.getCardFactory()) {
            String url = c.getSVar("Picture");
            String[] URLs = url.split("\\\\");

            String iName = GuiDisplayUtil.cleanString(c.getImageName());
            cList.add(new mCard(iName + ".jpg", URLs[0]));
            //Log.error(iName + ".jpg" + "\t" + URLs[0]);

            if (URLs.length > 1) {
                for (int j = 1; j < URLs.length; j++) {
                    cList.add(new mCard(iName + j + ".jpg", URLs[j]));
                }
            }
        }

        ArrayList<mCard> list = new ArrayList<mCard>();
        File file;

        File base = ForgeProps.getFile(IMAGE_BASE);
        mCard[] a = {new mCard("", "")};
        mCard[] cardPlay = cList.toArray(a);
        //check to see which cards we already have
        for (int i = 0; i < cardPlay.length; i++) {
            file = new File(base, cardPlay[i].name);
            if (!file.exists()) {
                list.add(cardPlay[i]);
            }
        }
        base = ForgeProps.getFile(IMAGE_TOKEN);
        for (int i = 0; i < cardTokenLQ.length; i++) {
            file = new File(base, cardTokenLQ[i].name.substring(3, cardTokenLQ[i].name.length()));
            if (!file.exists()) list.add(cardTokenLQ[i]);
        }

        //return all card names and urls that are needed
        mCard[] out = new mCard[list.size()];
        list.toArray(out);

        return out;
    } //getNeededCards()

   /**
     * <p>readFile.</p>
     *
     * @param ABC a {@link java.lang.String} object.
     * @return an array of {@link forge.Gui_DownloadPictures_LQ.mCard} objects.
     */
    private static mCard[] readFile(String ABC) {
        try {
            FileReader zrc = new FileReader(ForgeProps.getFile(ABC));
            BufferedReader in = new BufferedReader(zrc);
            String line;
            ArrayList<mCard> list = new ArrayList<mCard>();
            StringTokenizer tok;

            line = in.readLine();
            while (line != null && (!line.equals("")) && !line.startsWith("#")) {
                tok = new StringTokenizer(line);
                list.add(new mCard(tok.nextToken(), tok.nextToken()));

                line = in.readLine();
            }

            mCard[] out = new mCard[list.size()];
            list.toArray(out);
            return out;

        } catch (Exception ex) {
            ErrorViewer.showError(ex, "Gui_DownloadPictures: readFile() error");
            throw new RuntimeException("Gui_DownloadPictures : readFile() error");
        }
    }//readFile()

    private class ProxyHandler implements ChangeListener {
        private int type;

        public ProxyHandler(final int typeIn) {
            this.type = typeIn;
        }

        public void stateChanged(final ChangeEvent e) {
            if (((AbstractButton) e.getSource()).isSelected()) {
                Gui_DownloadPictures_LQ.this.type = type;
                addr.setEnabled(type != 0);
                port.setEnabled(type != 0);
            }
        }
    }

    private static class mCard {
        public final String name;
        public final String url;

        mCard(final String cardName, final String cardURL) {
            name = cardName;
            url = cardURL;
        }
    } //mCard
    
}
