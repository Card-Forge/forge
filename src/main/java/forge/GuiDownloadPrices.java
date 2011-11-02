package forge;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GuiDownloadPrices.DownloadPrices;
import forge.properties.NewConstants.Quest;

/**
 * <p>
 * Gui_DownloadPrices class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiDownloadPrices extends JFrame {

    /** Constant <code>serialVersionUID=1L</code>. */
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JButton jButton = null;

    /**
     * This is the default constructor.
     */
    public GuiDownloadPrices() {
        super();
        initialize();
    }

    /**
     * This method initializes this.
     */
    private void initialize() {
        this.setSize(386, 200);
        setContentPane(getJContentPane());
        setTitle(ForgeProps.getLocalized(DownloadPrices.TITLE));
    }

    /**
     * This method initializes jContentPane.
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(getJButton(), null);
        }
        return jContentPane;
    }

    /**
     * This method initializes jButton.
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText(ForgeProps.getLocalized(DownloadPrices.START_UPDATE));
            jButton.setLocation(new Point(120, 46));
            jButton.setSize(158, 89);

            jButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                    if (jButton.getText().equals("Done!")) {
                        GuiDownloadPrices.this.dispose();
                    }

                    BufferedInputStream in = null;
                    BufferedOutputStream out = null;

                    File f = new File(".//res//tmppl.txt");
                    String url = "http://www.magictraders.com/pricelists/current-magic-excel.txt";
                    Proxy p = Proxy.NO_PROXY;
                    byte[] buf = new byte[1024];
                    int x = 0;
                    String s = "Downloading";

                    try {
                        in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
                        out = new BufferedOutputStream(new FileOutputStream(f));

                        jButton.setText(ForgeProps.getLocalized(DownloadPrices.DOWNLOADING));
                        jContentPane.paintImmediately(jButton.getBounds());

                        int len = 0;
                        while ((len = in.read(buf)) != -1) {
                            out.write(buf, 0, len);

                            if (++x % 50 == 0) {
                                s += ".";
                                jButton.setText(s);
                                jContentPane.paintImmediately(jButton.getBounds());

                                if (x >= 300) {
                                    x = 0;
                                    s = "Downloading";
                                }
                            }
                        }
                        in.close();
                        out.flush();
                        out.close();
                    } catch (IOException e1) {
                        return;
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException ex) {
                            return;
                        }
                    } // while - read and write file

                    FileReader fr = null;
                    FileWriter fw = null;
                    try {
                        fr = new FileReader(".//res//tmppl.txt");

                        BufferedReader inBR = new BufferedReader(fr);
                        String line = null;

                        HashMap<String, Integer> prices = new HashMap<String, Integer>();

                        line = inBR.readLine();
                        line = inBR.readLine();

                        jButton.setText(ForgeProps.getLocalized(DownloadPrices.COMPILING));
                        jContentPane.paintImmediately(jButton.getBounds());

                        x = 0;
                        s = "Compiling";
                        while (line != null && !line.equals("")) {
                            String[] ll = line.split("\\|");

                            if (ll[0].contains("(")) {
                                int indx = ll[0].indexOf(" (");
                                ll[0] = ll[0].substring(0, indx);
                            }

                            Float np = Float.parseFloat(ll[3]) * 100;
                            int inp = np.intValue();

                            if (prices.containsKey(ll[0])) {
                                int cp = prices.get(ll[0]);
                                float fScl = 0;

                                if (cp >= inp) {
                                    fScl = 1 - (float) inp / (float) cp;
                                    if (fScl > .333) {
                                        cp = cp / 2;
                                    }
                                } else {
                                    fScl = 1 - (float) cp / (float) inp;
                                    if (fScl > .333) {
                                        inp = inp / 2;
                                    }
                                }

                                int ap = (cp + inp) / 2;
                                if (ap < 7) {
                                    ap += 10;
                                }
                                prices.put(ll[0], ap);
                            } else {
                                if (inp < 7) {
                                    inp += 10;
                                }

                                prices.put(ll[0], inp);
                            }

                            line = inBR.readLine();
                            // System.out.println(line);

                            if (++x % 100 == 0) {
                                s += ".";
                                jButton.setText(s);
                                jContentPane.paintImmediately(jButton.getBounds());

                                if (x >= 500) {
                                    x = 0;
                                    s = "Compiling";
                                }
                            }
                        }

                        String pfn = ForgeProps.getFile(Quest.PRICE).getAbsolutePath();
                        String pfnb = pfn.replace(".txt", ".bak");
                        File ff = new File(pfn);
                        ff.renameTo(new File(pfnb));

                        fw = new FileWriter(ForgeProps.getFile(Quest.PRICE));
                        BufferedWriter outBW = new BufferedWriter(fw);

                        // Collection<String> keys = prices.keySet();
                        ArrayList<String> keys = new ArrayList<String>();
                        keys.addAll(prices.keySet());
                        Collections.sort(keys);

                        for (int i = 0; i < keys.size(); i++) {
                            // keys.add(key);
                            String k = keys.get(i);
                            if (k.equals("Plains") || k.equals("Island") || k.equals("Swamp") || k.equals("Mountain")
                                    || k.equals("Forest")) {
                                outBW.write(k + "=5\r\n");
                            } else if (k.equals("Snow-Covered Plains") || k.equals("Snow-Covered Island")
                                    || k.equals("Snow-Covered Swamp") || k.equals("Snow-Covered Mountain")
                                    || k.equals("Snow-Covered Forest")) {
                                outBW.write(k + "=10\r\n");
                            } else {
                                outBW.write(keys.get(i) + "=" + prices.get(keys.get(i)) + "\r\n");
                            }

                            if (i % 100 == 0) {
                                outBW.flush();
                            }
                        }

                        outBW.flush();
                        outBW.close();
                        fw.close();

                        jButton.setText("Done!");
                        fr.close();
                        f.delete();
                    } catch (IOException e1) {
                        return;
                    } finally {
                        try {
                            if (fr != null) {
                                fr.close();
                            }
                            if (fw != null) {
                                fw.close();
                            }
                        } catch (IOException ex) {
                            return;
                        }
                    }
                    return;
                }
            });
        }
        return jButton;
    }

} // @jve:decl-index=0:visual-constraint="10,10"
