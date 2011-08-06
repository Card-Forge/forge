    import java.io.File;

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
           
            ImageIcon i = new ImageIcon(f.getPath());
            this.add(new JLabel(i));
        }
    }