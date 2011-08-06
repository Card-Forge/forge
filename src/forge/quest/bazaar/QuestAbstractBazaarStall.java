package forge.quest.bazaar;

import forge.AllZone;
import forge.QuestData;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import java.io.File;

public abstract class QuestAbstractBazaarStall extends JPanel implements NewConstants{
    String stallName;
    String blurb;
    ImageIcon icon;

    JPanel stallPanel;

    JLabel stallNameLabel;

    protected  QuestData questData = AllZone.QuestData;

    protected QuestAbstractBazaarStall(String stallName, String iconName, String blurb) {
        this.blurb = blurb;
        this.icon = getIcon(iconName);
        this.stallName = stallName;
        this.stallPanel = new JPanel();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        stallNameLabel = new JLabel();

        this.add(stallPanel);
    }

    
    ImageIcon getIcon(String fileName){
    	File base = ForgeProps.getFile(IMAGE_ICON);
    	File file = new File(base, fileName);
    	ImageIcon icon = new ImageIcon(file.toString());
    	return icon;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getStallName() {
        return stallName;
    }
}
