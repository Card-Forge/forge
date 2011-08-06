package forge;
    import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
    import javax.swing.ImageIcon;
    import javax.swing.JLabel;
    import javax.swing.JPanel;

import forge.error.ErrorViewer;


    public class PicturePanel extends JPanel {
        private static final long serialVersionUID = 2282867940272644768L;
       
        public PicturePanel(File f) {
            if(!f.exists()) {
                ErrorViewer.showError("PicturePanel : file does not exist - %s", f);
                throw new RuntimeException("PicturePanel : file does not exist - " + f);
            }
            
            ImageIcon i = new ImageIcon();
            Image im ;
            im=null;
            try {
				im=ImageIO.read(f);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
            i.setImage(im);
            this.add(new JLabel(i));
           
        }
    }