package forge;

import static java.lang.Integer.*;
import static javax.swing.JOptionPane.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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



public class Gui_DownloadPictures extends DefaultBoundedRangeModel implements Runnable, NewConstants, NewConstants.LANG.Gui_DownloadPictures {

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
    private JComboBox jComboBox1;
    private JLabel jLabel1;
    private String url;
    
    private Gui_DownloadPictures(Card[] c) {
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
        
        p0.add(Box.createVerticalStrut(5));
        jLabel1 = new JLabel();
  		jLabel1.setText("Please select server:");
  		
  		jLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
  		
  		p0.add(jLabel1);
  		p0.add(Box.createVerticalStrut(5));
  		    ComboBoxModel jComboBox1Model = 
      		new DefaultComboBoxModel(
      				new String[] { "mtgpics.chutography.com", "pics.slightlymagic.net" });
      	jComboBox1 = new JComboBox();
      	
      	jComboBox1.setModel(jComboBox1Model);
    		jComboBox1.setAlignmentX(Component.LEFT_ALIGNMENT);
          p0.add(jComboBox1);
          p0.add(Box.createVerticalStrut(5));
        
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
                new Thread(Gui_DownloadPictures.this).start();
                b.setEnabled(false);
            }
        });
//        p0.add(b);
        
        p0.add(Box.createVerticalStrut(5));
        
        //Progress
        p0.add(bar);
        bar.setStringPainted(true);
      // bar.setString(ForgeProps.getLocalized(BAR_BEFORE_START));
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
        final class Worker implements Runnable{
			private int card;
			Worker(int card){
				this.card = card;
			}

			public void run() {
		        fireStateChanged();
		        bar.setString(String.format(ForgeProps.getLocalized(card == cards.length? BAR_CLOSE:BAR_WAIT), card,
		                cards.length));
		        System.out.println(card + "/" + cards.length);
			}
		};
		EventQueue.invokeLater(new Worker(card));
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
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        
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
            for(update(0); card < cards.length && !cancel; update(card + 1)) {
                try {
                	
                	String tsr;
                	tsr=cards[card].url.substring(7,15);
                	
                	if(tsr.equals("[server]"))
                    {                    	
                			
                			String b = cards[card].url.substring(0,7)+jComboBox1.getSelectedItem().toString()+cards[card].url.substring(15,cards[card].url.length());
                    		url=b;
                    	                    	
                    }else
                    {
                    	 url = cards[card].url;
                    }
                    
                    
                    String cName;
                    if(cards[card].name.substring(0, 3).equals("[T]")){
                    	base = ForgeProps.getFile(IMAGE_TOKEN);
                    	cName = cards[card].name.substring(3, cards[card].name.length());
                    }else
                    {
                    	base = ForgeProps.getFile(IMAGE_BASE);
                    	cName=cards[card].name;
                    }                    
                    
                    File f = new File(base, cName);
                    
                    in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    
                    while((len = in.read(buf)) != -1) {
                        //user cancelled
                        if(cancel) {
                            flushAndCloseStreams(in, out);
                            
                            //delete what was written so far
                            f.delete();
                            
                            return;
                        }//if - cancel
                        
                        out.write(buf, 0, len);
                    }//while - read and write file
                    
                } catch(Exception ex) {
                	Log.error("HQ Pictures", "Error downloading pictures", ex);
                }
                finally{
                	try {
						flushAndCloseStreams(in, out);
					} catch (IOException e) {
						e.printStackTrace();
					}                	
                }
            }//for
        }
        close.setText(ForgeProps.getLocalized(BUTTONS.CLOSE));
    }//run

	private void flushAndCloseStreams(BufferedInputStream in, BufferedOutputStream out) throws IOException {
		if(in != null)
			in.close();
		if(out != null){
			out.flush();
			out.close();
		}
	}
    
    public static void startDownload(JFrame frame) {
        final Card[] card = getNeededCards();
       
        if(card.length == 0) {
            JOptionPane.showMessageDialog(frame, ForgeProps.getLocalized(NO_MORE));
            return;
        }
       
        Gui_DownloadPictures download = new Gui_DownloadPictures(card);
        JDialog dlg = download.getDlg(frame);
        dlg.setVisible(true);
        dlg.dispose();
        download.setCancel(true);
    }//startDownload()
    
    private static Card[] getNeededCards() {
        //read all card names and urls
        Card[] cardA = readFile(CARD_PICTURES_A);
        Card[] cardB = readFile(CARD_PICTURES_B);
        Card[] cardC = readFile(CARD_PICTURES_C);
        Card[] cardD = readFile(CARD_PICTURES_D);
        Card[] cardE = readFile(CARD_PICTURES_E);
        Card[] cardF = readFile(CARD_PICTURES_F);
        Card[] cardG = readFile(CARD_PICTURES_G);
        Card[] cardH = readFile(CARD_PICTURES_H);
        Card[] cardI = readFile(CARD_PICTURES_I);
        Card[] cardJ = readFile(CARD_PICTURES_J);
        Card[] cardK = readFile(CARD_PICTURES_K);
        Card[] cardL = readFile(CARD_PICTURES_L);
        Card[] cardM = readFile(CARD_PICTURES_M);
        Card[] cardN = readFile(CARD_PICTURES_N);
        Card[] cardO = readFile(CARD_PICTURES_O);
        Card[] cardP = readFile(CARD_PICTURES_P);
        Card[] cardQ = readFile(CARD_PICTURES_Q);
        Card[] cardR = readFile(CARD_PICTURES_R);
        Card[] cardS = readFile(CARD_PICTURES_S);
        Card[] cardT = readFile(CARD_PICTURES_T);
        Card[] cardU = readFile(CARD_PICTURES_U);
        Card[] cardV = readFile(CARD_PICTURES_V);
        Card[] cardW = readFile(CARD_PICTURES_W);
        Card[] cardX = readFile(CARD_PICTURES_X);
        Card[] cardY = readFile(CARD_PICTURES_Y);
        Card[] cardZ = readFile(CARD_PICTURES_Z);
        Card[] cardOther = readFile(CARD_PICTURES_OTHER);
        Card[] cardTokenHQ = readFile(CARD_PICTURES_TOKEN_HQ);
        ArrayList<Card> list = new ArrayList<Card>();
        File file;
        
        CardList all = AllZone.CardFactory.getAllCards(); 
        
        File base = ForgeProps.getFile(IMAGE_BASE);
        
        //check to see which cards we already have and which cards Forge support
        String cCard;
        int simbol;
      
        for(int k=0;k < all.size();k++){
        	cCard = GuiDisplayUtil.cleanString(all.getCard(k).getName().toLowerCase())+".jpg";
        	File fileTest = new File(base, cCard);
        	if(!fileTest.exists())
        	{
        	simbol = cCard.codePointAt(0);
        	switch (simbol) {
        	case 97:  //a
        	for(int i = 0; i < cardA.length; i++) {
                            if(cCard.equals(cardA[i].name.toLowerCase()))
                    	{
                    		list.add(cardA[i]);
                			break; 
                    	}
               
                }
        	break;
        	case 98:  //b	
        	for(int i = 0; i < cardB.length; i++) {
                    	if(cCard.equals(cardB[i].name.toLowerCase()))
                    	{
                    	list.add(cardB[i]);
            			break;
                    	}
                	}
        		break;
        	case 99:  //c	
        	for(int i = 0; i < cardC.length; i++) {
                	if(cCard.equals(cardC[i].name.toLowerCase()))
                	{
                	list.add(cardC[i]);
        			break;
                	}
                	
       			}
        		break;
        	case 100:  //D	
            	for(int i = 0; i < cardD.length; i++) {
                    
                    	if(cCard.equals(cardD[i].name.toLowerCase()))
                    	{
                    	list.add(cardD[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 101:  //E	
            	for(int i = 0; i < cardE.length; i++) {
                    
                    	if(cCard.equals(cardE[i].name.toLowerCase()))
                    	{
                    	list.add(cardE[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 102:  //F	
            	for(int i = 0; i < cardF.length; i++) {
                    
                    	if(cCard.equals(cardF[i].name.toLowerCase()))
                    	{
                    	list.add(cardF[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 103:  //G	
            	for(int i = 0; i < cardG.length; i++) {
                   
                    	if(cCard.equals(cardG[i].name.toLowerCase()))
                    	{
                    	list.add(cardG[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 104:  //H	
            	for(int i = 0; i < cardH.length; i++) {
                    
                    	if(cCard.equals(cardH[i].name.toLowerCase()))
                    	{
                    	list.add(cardH[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 105:  //I	
            	for(int i = 0; i < cardI.length; i++) {
                    
                    	if(cCard.equals(cardI[i].name.toLowerCase()))
                    	{
                    	list.add(cardI[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 106:  //J	
            	for(int i = 0; i < cardJ.length; i++) {
                   
                    	if(cCard.equals(cardJ[i].name.toLowerCase()))
                    	{
                    	list.add(cardJ[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 107:  //K	
            	for(int i = 0; i < cardK.length; i++) {
                  
                    	if(cCard.equals(cardK[i].name.toLowerCase()))
                    	{
                    	list.add(cardK[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 108:  //L	
            	for(int i = 0; i < cardL.length; i++) {
                   
                    	if(cCard.equals(cardL[i].name.toLowerCase()))
                    	{
                    	list.add(cardL[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 109:  //M	
            	for(int i = 0; i < cardM.length; i++) {
                   
                    	if(cCard.equals(cardM[i].name.toLowerCase()))
                    	{
                    	list.add(cardM[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 110:  //N	
            	for(int i = 0; i < cardN.length; i++) {
                   
                    	if(cCard.equals(cardN[i].name.toLowerCase()))
                    	{
                    	list.add(cardN[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 111:  //O	
            	for(int i = 0; i < cardO.length; i++) {
                   
                    	if(cCard.equals(cardO[i].name.toLowerCase()))
                    	{
                    	list.add(cardO[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 112:  //P	
            	for(int i = 0; i < cardP.length; i++) {
                   
                    	if(cCard.equals(cardP[i].name.toLowerCase()))
                    	{
                    	list.add(cardP[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 113:  //Q	
            	for(int i = 0; i < cardQ.length; i++) {
                    
                    	if(cCard.equals(cardQ[i].name.toLowerCase()))
                    	{
                    	list.add(cardQ[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 114:  //R	
            	for(int i = 0; i < cardR.length; i++) {
                   
                    	if(cCard.equals(cardR[i].name.toLowerCase()))
                    	{
                    	list.add(cardR[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 115:  //S	
            	for(int i = 0; i < cardS.length; i++) {
                   
                    	if(cCard.equals(cardS[i].name.toLowerCase()))
                    	{
                    	list.add(cardS[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 116:  //T	
            	for(int i = 0; i < cardT.length; i++) {
                   
                    	if(cCard.equals(cardT[i].name.toLowerCase()))
                    	{
                    	list.add(cardT[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 117:  //U	
            	for(int i = 0; i < cardU.length; i++) {
                   
                    	if(cCard.equals(cardU[i].name.toLowerCase()))
                    	{
                    	list.add(cardU[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 118:  //V	
            	for(int i = 0; i < cardV.length; i++) {
                   
                    	if(cCard.equals(cardV[i].name.toLowerCase()))
                    	{
                    	list.add(cardV[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 119:  //W	
            	for(int i = 0; i < cardW.length; i++) {
                   
                    	if(cCard.equals(cardW[i].name.toLowerCase()))
                    	{
                    	list.add(cardW[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 120:  //X	
            	for(int i = 0; i < cardX.length; i++) {
                  
                    	if(cCard.equals(cardX[i].name.toLowerCase()))
                    	{
                    	list.add(cardX[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 121:  //Y	
            	for(int i = 0; i < cardY.length; i++) {
                   
                    	if(cCard.equals(cardY[i].name.toLowerCase()))
                    	{
                    	list.add(cardY[i]);
            			break;                    	
                    	}
           			}
            	break;
        	case 122:  //Z	
            	for(int i = 0; i < cardZ.length; i++) {
                   
                    	if(cCard.equals(cardZ[i].name.toLowerCase()))
                    	{
                    	list.add(cardZ[i]);
            			break;                    	
                    	}
           			}
            	break;
        	default: break;	
        	}
       }
        } 	
        for(int i = 0; i < cardOther.length; i++) {
            file = new File(base, cardOther[i].name);
            if(!file.exists()) list.add(cardOther[i]);
        }
        base = ForgeProps.getFile(IMAGE_TOKEN);
        for(int i = 0; i < cardTokenHQ.length; i++) {
        	 file = new File(base, cardTokenHQ[i].name.substring(3, cardTokenHQ[i].name.length()));
            if(!file.exists()) list.add(cardTokenHQ[i]);
        }
        
        
        //return all card names and urls that are needed
        Card[] out = new Card[list.size()];
        list.toArray(out);
        
//    for(int i = 0; i < out.length; i++)
//      System.out.println(out[i].name +" " +out[i].url);
        return out;
    }//getNeededCards()
    
    private static Card[] readFile(String ABC) {
        try {
        	FileReader zrc = new FileReader(ForgeProps.getFile(ABC));
            BufferedReader in = new BufferedReader(zrc);
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
                Gui_DownloadPictures.this.type = type;
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
