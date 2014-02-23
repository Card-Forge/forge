package forge;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.utils.Utils;

public abstract class FScreen extends FContainer {
    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);

    private final FLabel btnBack, lblHeader, btnMenu;

    protected FScreen(boolean showBackButton, String headerCaption, boolean showMenuButton) {
    	if (showBackButton) {
    		btnBack = add(new FLabel.ButtonBuilder().icon(FSkinImage.BACK).command(new Runnable() {
				@Override
				public void run() {
					Forge.back();
				}
    		}).build());
    	}
    	else {
    		btnBack = null; 
    	}
    	if (headerCaption != null) {
    		lblHeader = add(new FLabel.Builder().text(headerCaption).fontSize(16).align(HAlignment.CENTER).build());
    	}
    	else {
    		lblHeader = null;
    	}
    	if (showMenuButton) {
    		btnMenu = add(new FLabel.ButtonBuilder().icon(FSkinImage.FAVICON).command(new Runnable() {
				@Override
				public void run() {
					showMenu();
				}
    		}).build());
    	}
    	else {
    		btnMenu = null;
    	}
    }

    public void onOpen() {
    }

    public boolean onSwitch() {
        return true;
    }

    public boolean onClose() {
        return true;
    }

    public void showMenu() {
    	buildMenu();
    }

    protected void buildMenu() {
    	
    }

    @Override
    protected final void doLayout(float width, float height) {
    	float headerX = 0;
    	float insets = 0;
    	float headerWidth = width;
    	float buttonWidth = Utils.AVG_FINGER_WIDTH;
    	float headerHeight = Utils.AVG_FINGER_HEIGHT;

    	if (btnBack != null) {
    		btnBack.setBounds(insets, insets, buttonWidth, headerHeight);
    		headerX = btnBack.getWidth();
    		headerWidth -= headerX;
    	}
    	if (btnMenu != null) {
    		btnMenu.setBounds(width - buttonWidth - insets, insets, buttonWidth, headerHeight);
    		headerWidth -= btnMenu.getWidth();
    	}
    	if (lblHeader != null) {
    		lblHeader.setBounds(headerX, 0, headerWidth, headerHeight);

        	doLayout(headerHeight, width, height);
    	}
    	else {
        	doLayout(0, width, height);
    	}
    }

    protected abstract void doLayout(float startY, float width, float height);

    @Override
    protected void drawBackground(Graphics g) {
        g.drawImage(FSkinImage.BG_TEXTURE, 0, 0, getWidth(), getHeight());
        g.fillRect(clrTheme, 0, 0, getWidth(), getHeight());
    }
}
