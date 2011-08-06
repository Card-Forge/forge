package forge;

import static java.lang.Integer.*;
import static javax.swing.JOptionPane.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
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

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;



public class Gui_DownloadPictures_LQ extends DefaultBoundedRangeModel implements Runnable, NewConstants, NewConstants.LANG.Gui_DownloadPictures {

	private static final long serialVersionUID = -7890794857949935256L;

	public static void main(String[] args) {
        startDownload(null);
    }
    
    public static final Proxy.Type[] types = Proxy.Type.values();
    
    //proxy
    private int                      type;
    private JTextField               addr, port;
    
    //progress
    private Card[]                   cards;
    private int                      card;
    private boolean                  cancel;
    private JProgressBar             bar;
    
    private JOptionPane              dlg;
    private JButton                  close;
    
    private Gui_DownloadPictures_LQ(Card[] c) {
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
        for(int i = 0; i < types.length; i++) {
            JRadioButton rb = new JRadioButton(labels[i]);
            rb.addChangeListener(new ProxyHandler(i));
            bg.add(rb);
            p0.add(rb);
            if(i == 0) rb.setSelected(true);
        }
        
        //Proxy config
        p0.add(addr);
        p0.add(port);
//        JTextField[] tfs = {addr, port};
//        String[] labels = {"Address", "Port"};
//        for(int i = 0; i < labels.length; i++) {
//            JPanel p1 = new JPanel(new BorderLayout());
//            p0.add(p1);
////            p1.add(new JLabel(labels[i]), WEST);
//            p1.add(tfs[i]);
//        }
        
        //Start
        final JButton b = new JButton(ForgeProps.getLocalized(BUTTONS.START));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new Thread(Gui_DownloadPictures_LQ.this).start();
                b.setEnabled(false);
            }
        });
//        p0.add(b);
        
        p0.add(Box.createVerticalStrut(5));
        
        //Progress
        p0.add(bar);
        bar.setStringPainted(true);
        //bar.setString(ForgeProps.getLocalized(BAR_BEFORE_START));
        bar.setString(String.format(ForgeProps.getLocalized(card == cards.length? BAR_CLOSE:BAR_WAIT), this.card, cards.length));
        Dimension d = bar.getPreferredSize();
        d.width = 300;
        bar.setPreferredSize(d);
        
        //JOptionPane
        Object[] options = {b, close = new JButton(ForgeProps.getLocalized(BUTTONS.CANCEL))};
        dlg = new JOptionPane(p0, DEFAULT_OPTION, PLAIN_MESSAGE, null, options, options[1]);
    }
    
    @Override
    public int getMinimum() {
        return 0;
    }
    
    @Override
    public int getValue() {
        return card;
    }
    
    @Override
    public int getExtent() {
        return 0;
    }
    
    @Override
    public int getMaximum() {
        return cards == null? 0:cards.length;
    }
    
    private void update(int card) {
        this.card = card;
        fireStateChanged();
        bar.setString(String.format(ForgeProps.getLocalized(card == cards.length? BAR_CLOSE:BAR_WAIT), this.card,
                cards.length));
        System.out.println(card + "/" + cards.length);
    }
    
    public JDialog getDlg(JFrame frame) {
        final JDialog dlg = this.dlg.createDialog(frame, ForgeProps.getLocalized(TITLE));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });
        return dlg;
    }
    
    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
    

    public void run() {
        BufferedInputStream in;
        BufferedOutputStream out;
        
        File base = ForgeProps.getFile(IMAGE_BASE);
        
        Proxy p = null;
        if(type == 0) p = Proxy.NO_PROXY;
        else try {
            p = new Proxy(types[type], new InetSocketAddress(addr.getText(), parseInt(port.getText())));
        } catch(Exception ex) {
            ErrorViewer.showError(ex, ForgeProps.getLocalized(ERRORS.PROXY_CONNECT), addr.getText(),
                    port.getText());
//            throw new RuntimeException("Gui_DownloadPictures : error 1 - " +ex);
            return;
        }
        
        if(p != null) {
            byte[] buf = new byte[1024];
            int len;
            System.out.println("basedir: " + base);
            for(update(0); card < cards.length && !cancel; update(card + 1)) {
                try {
                    String url = cards[card].url;
                    File f = new File(base, cards[card].name);
                    
                    in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    
                    while((len = in.read(buf)) != -1) {
                        //user cancelled
                        if(cancel) {
                            in.close();
                            out.flush();
                            out.close();
                            
                            //delete what was written so far
                            f.delete();
                            
                            return;
                        }//if - cancel
                        
                        out.write(buf, 0, len);
                    }//while - read and write file
                    
                    in.close();
                    out.flush();
                    out.close();
                } catch(Exception ex) {
                    ErrorViewer.showError(ex, ForgeProps.getLocalized(ERRORS.OTHER), cards[card].name,
                            cards[card].url);
//                    throw new RuntimeException("Gui_DownloadPictures : error 1 - " +ex);
                    break;
                }
            }//for
        }
        close.setText(ForgeProps.getLocalized(BUTTONS.CLOSE));
    }//run
    
    public static void startDownload(JFrame frame) {
        final Card[] card = getNeededCards();
       
        if(card.length == 0) {
            JOptionPane.showMessageDialog(frame, ForgeProps.getLocalized(NO_MORE));
            return;
        }
       
        Gui_DownloadPictures_LQ download = new Gui_DownloadPictures_LQ(card);
        JDialog dlg = download.getDlg(frame);
        dlg.setVisible(true);
        dlg.dispose();
        download.setCancel(true);
    }//startDownload()
    
    private static Card[] getNeededCards() {
        //read all card names and urls
        Card[] card = readFile();
        ArrayList<Card> list = new ArrayList<Card>();
        File file;
        
        File base = ForgeProps.getFile(IMAGE_BASE);
        
        //check to see which cards we already have
        for(int i = 0; i < card.length; i++) {
            file = new File(base, card[i].name);
            if(!file.exists()) list.add(card[i]);
        }
        //return all card names and urls that are needed
        Card[] out = new Card[list.size()];
        list.toArray(out);
        
//    for(int i = 0; i < out.length; i++)
//      System.out.println(out[i].name +" " +out[i].url);
        return out;
    }//getNeededCards()
    
    private static Card[] readFile() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(ForgeProps.getFile(CARD_PICTURES)));
            String line;
            ArrayList<Card> list = new ArrayList<Card>();
            StringTokenizer tok;
            
            line = in.readLine();
            while(line != null && (!line.equals(""))) {
                tok = new StringTokenizer(line);
                list.add(new Card(tok.nextToken(), tok.nextToken()));
                
                line = in.readLine();
            }
            
            Card[] out = new Card[list.size()];
            list.toArray(out);
            return out;
            
        } catch(Exception ex) {
            ErrorViewer.showError(ex, "Gui_DownloadPictures: readFile() error");
            throw new RuntimeException("Gui_DownloadPictures : readFile() error");
        }
    }//readFile()
    
    private class ProxyHandler implements ChangeListener {
        private int type;
        
        public ProxyHandler(int type) {
            this.type = type;
        }
        
        public void stateChanged(ChangeEvent e) {
            if(((AbstractButton) e.getSource()).isSelected()) {
                Gui_DownloadPictures_LQ.this.type = type;
                addr.setEnabled(type != 0);
                port.setEnabled(type != 0);
            }
        }
    }
    
    private static class Card {
        final public String name;
        final public String url;
        
        Card(String cardName, String cardURL) {
            name = cardName;
            url = cardURL;
        }
    }//Card
} 