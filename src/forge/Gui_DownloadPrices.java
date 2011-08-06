package forge;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.QUEST;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Gui_DownloadPrices extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JButton jButton = null;

	/**
	 * This is the default constructor
	 */
	public Gui_DownloadPrices() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(386, 200);
		this.setContentPane(getJContentPane());
		this.setTitle("Update Prices");
	}

	/**
	 * This method initializes jContentPane
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
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Start Update");
			jButton.setLocation(new Point(120, 46));
			jButton.setSize(158, 89);

			jButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (jButton.getText().equals("Done!"))
						Gui_DownloadPrices.this.dispose();
					
			        BufferedInputStream in = null;
			        BufferedOutputStream out = null;
			        
			        File f = new File(".//res//tmppl.txt");
                    String url = "http://www.magictraders.com/pricelists/current-magic-excel.txt";
                    Proxy p = Proxy.NO_PROXY;
                    byte[] buf = new byte[1024];
			        
                    try {
						in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
					} catch (MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    try {
						out = new BufferedOutputStream(new FileOutputStream(f));
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    
					jButton.setText("Downloading");
					jContentPane.paintImmediately(jButton.getBounds());
					
					int x = 0;
					String s = new String("Downloading");
                    int len = 0;
                    try {
                    	while((len = in.read(buf)) != -1) {                        
						    out.write(buf, 0, len);
						    
						    if ((++x % 50) == 0)
						    {
						    	s += ".";
								jButton.setText(s);
								jContentPane.paintImmediately(jButton.getBounds());

						    	if (x >= 300)
						    	{
						    		x = 0;
						    		s = "Downloading";
						    	}
						    }
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}//while - read and write file
                    
                    try {
						in.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    try {
						out.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    try {
						out.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
		        	FileReader fr = null;
					try {
						fr = new FileReader(".//res//tmppl.txt");
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
		        	BufferedReader inBR = new BufferedReader(fr);
		            String line = null;
		            
		            HashMap<String, Integer> prices = new HashMap<String, Integer>();
		            
		            try {
						line = inBR.readLine();
						line = inBR.readLine();
						
						jButton.setText("Compiling");
						jContentPane.paintImmediately(jButton.getBounds());
						
			            x = 0;
			            s = "Compiling";
						while (line != null && (!line.equals("")))
			            {
							String ll[] = line.split("\\|");
							
							if (ll[0].contains("("))
							{
								int indx = ll[0].indexOf(" (");
								ll[0] = ll[0].substring(0, indx);
							}

							Float np = Float.parseFloat(ll[3]) * 100;
							int inp = np.intValue();
							
							if (prices.containsKey(ll[0]))
							{
								int cp = prices.get(ll[0]);
								float fScl = 0;
								
								if (cp >= inp)
								{
									fScl = (1 - ((float)inp / (float)cp));
									if (fScl > .333)
										cp = cp / 2;
								}
								else
								{
									fScl = (1 - ((float)cp / (float)inp));
									if (fScl > .333)
										inp = inp / 2;
								}
																	
								int ap = (cp + inp) / 2;
								if (ap < 7)
									ap += 10;
								prices.put(ll[0], ap);
							}
							else
							{
								if (inp < 7)
									inp += 10;
								
								prices.put(ll[0], inp);
							}
							
							line = inBR.readLine();
							//System.out.println(line);
							
							if ((++x % 100) == 0)
							{
								s += ".";
								jButton.setText(s);
								jContentPane.paintImmediately(jButton.getBounds());

								if (x >= 500)
								{
									x = 0;
									s = "Compiling";
								}
							}
			            }
			            
						String pfn = ForgeProps.getFile(QUEST.PRICE).getAbsolutePath();
						String pfnb = pfn.replace(".txt", ".bak");
						File ff = new File(pfn);
						ff.renameTo(new File(pfnb));
						
			            FileWriter fw = new FileWriter(ForgeProps.getFile(QUEST.PRICE));
			            BufferedWriter outBW = new BufferedWriter(fw);
			            
			            //Collection<String> keys = prices.keySet();
			            ArrayList<String> keys = new ArrayList<String>();
			            keys.addAll(prices.keySet());
			            Collections.sort(keys);
			            
			            for (int i=0; i<keys.size(); i++)
			            {
			            	//keys.add(key);
			            	String k = keys.get(i);
			            	if (k.equals("Plains") ||
			            		k.equals("Island") ||
			            		k.equals("Swamp")  ||
			            		k.equals("Mountain") ||
			            		k.equals("Forest"))
			            		outBW.write(k + "=5\r\n");
			            	
			            	else if (k.equals("Snow-Covered Plains") ||
			            		k.equals("Snow-Covered Isnald") ||
			            		k.equals("Snow-Covered Swamp")  ||
			            		k.equals("Snow-Covered Mountain") ||
			            		k.equals("Snow-Covered Forest"))
			            		outBW.write(k + "=10\r\n");
			            	else
			            		outBW.write(keys.get(i) + "=" + prices.get(keys.get(i)) + "\r\n");
			            	
			            	if ((i % 100) == 0)
			            		outBW.flush();
			            }
			            
			            outBW.flush();
			            outBW.close();
			            fw.close();
			            
			            jButton.setText("Done!");

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					try {
						fr.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					f.delete();
					
					return;
				}
			});
		}
		return jButton;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
