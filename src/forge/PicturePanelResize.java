package forge;
    import java.awt.image.BufferedImage;
import java.io.File;

    import javax.swing.ImageIcon;
    import javax.swing.JLabel;
    import javax.swing.JPanel;

import forge.error.ErrorViewer;


    public class PicturePanelResize extends JPanel {
    	
     
		private static final long serialVersionUID = 2535819542781719182L;

		public PicturePanelResize(File f) {
            if(!f.exists()) {
                ErrorViewer.showError("PicturePanel : file does not exist - %s", f);
                throw new RuntimeException("PicturePanel : file does not exist - " + f);
            }
          
            BufferedImage bimage = ImageEditor.ResizeImageBuffer(f.getAbsolutePath(), 230, 230);
            ImageIcon i = new ImageIcon(bimage);
            this.add(new JLabel(i));
        }
    }