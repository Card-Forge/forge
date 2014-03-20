package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector2;

import forge.model.FModel;
import forge.screens.match.views.VHeader.HeaderDropDown;
import forge.toolbox.FLabel;
import forge.utils.ForgePreferences;
import forge.utils.ForgePreferences.FPref;

public class VDev extends HeaderDropDown {
    private List<DevLabel> devLabels = new ArrayList<DevLabel>();

    public VDev() {
        final ForgePreferences prefs = FModel.getPreferences();
        boolean unlimitedLands = prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND);

        addDevLabel("Play Unlimited Lands: " + (unlimitedLands ? "Enabled" : "Disabled"), new Runnable() {
            @Override
            public void run() {
                
            }
        }, true, unlimitedLands);
        addDevLabel("Generate Mana", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Setup Game State", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Tutor for Card", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Add Counter to Permanent", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Tap Permanent", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Untap Permanent", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Set Player Life", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Add card to play", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Add card to hand", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Rigged planar roll", new Runnable() {
            @Override
            public void run() {
                
            }
        });
        addDevLabel("Planeswalk to", new Runnable() {
            @Override
            public void run() {
                
            }
        });
    }

    private void addDevLabel(final String text0, final Runnable command0) {
        addDevLabel(text0, command0, false, false);
    }
    private void addDevLabel(final String text0, final Runnable command0, boolean selectable0, boolean selected0) {
        devLabels.add(add(new DevLabel(text0, command0, selectable0, selected0)));
    }

    @Override
    public int getCount() {
        return -1;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return new ScrollBounds(visibleWidth, visibleHeight);
    }

    private class DevLabel extends FLabel {
        private static final float PADDING = 6f;

        public DevLabel(final String text0, final Runnable command0, boolean selectable0, boolean selected0) {
            super(new ButtonBuilder().text(text0).command(command0)
                    .insets(new Vector2(PADDING, PADDING))
                    .selectable(selectable0).selected(selected0));
        }
    }
}
