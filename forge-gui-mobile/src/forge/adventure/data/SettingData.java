package forge.adventure.data;


/**
 * Data class that will be used to read Json configuration files
 * SettingData
 * contains settings outside of the chosen adventure
 */
public class SettingData {

    public int width;
    public int height;
    public String plane;
    public boolean fullScreen;
    public String videomode;
    public String lastActiveSave;
    public Float rewardCardAdj;
    public Float cardTooltipAdj;
    public Float rewardCardAdjLandscape;
    public Float cardTooltipAdjLandscape;
    public boolean dayNightBG;
    public boolean disableWinLose;
    public boolean showShopOverlay;
}
