package forge;


import java.io.FileWriter;
import java.util.ArrayList;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;



public class PrintCardPictures implements NewConstants {
    public static void main(String[] args) throws Exception {
        ReadCard read = new ReadCard(ForgeProps.getFile(CARDSFOLDER));
        
        //javax.swing.SwingUtilities.invokeAndWait(read);
        read.run();
        

        ArrayList<Card> list = read.getCards();
        FileWriter out = new FileWriter("c:\\new-pictures.txt");
        
        Card c;
        String string;
        
        for(int i = 0; i < list.size(); i++) {
            c = (Card) list.get(i);
            string = GuiDisplayUtil.cleanString(c.getName()) + ".jpg";
            string = string + "\t http://mi.wizards.com/global/images/magic/general/" + string;
            
            System.out.println(string);
            out.write(string + "\r\n");
        }
        
        out.flush();
        out.close();
        
    }//main()
}