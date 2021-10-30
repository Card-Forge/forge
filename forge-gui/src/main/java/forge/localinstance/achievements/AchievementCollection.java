package forge.localinstance.achievements;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Maps;

import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.gui.GuiBase;
import forge.gui.interfaces.IComboBox;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.util.XmlUtil;

public abstract class AchievementCollection implements Iterable<Achievement> {
    protected final Map<String, Achievement> achievements = Maps.newLinkedHashMap();
    protected final String name, filename, path;
    protected final boolean isLimitedFormat;

    static {
        FileUtil.ensureDirectoryExists(ForgeConstants.ACHIEVEMENTS_DIR);
    }

    public static void updateAll(final PlayerControllerHuman controller) {
        //don't update achievements if player cheated during game
        if (controller.hasCheated()) {
            return;
        }

        final Match match = controller.getMatch();
        final Player player = controller.getPlayer();

        //update all achievements for GUI player after game finished
        //(we are doing it in different threads in different game ports to prevent freezing when processing multiple achievements)
        if (GuiBase.getInterface().isLibgdxPort()) {
            ThreadUtil.invokeInGameThread(new Runnable() {
                @Override
                public void run() {
                    doUpdateAllAchievements(match, player);
                }
            });
        } else {
            doUpdateAllAchievements(match, player);
        }
    }

    private static void doUpdateAllAchievements(final Match match, final Player player) {
        FModel.getAchievements(match.getRules().getGameType()).updateAll(player);
        AltWinAchievements.instance.updateAll(player);
        PlaneswalkerAchievements.instance.updateAll(player);
        ChallengeAchievements.instance.updateAll(player);
    }
    
    public static void buildComboBox(IComboBox<AchievementCollection> cb) {
        cb.addItem(FModel.getAchievements(GameType.Constructed));
        cb.addItem(FModel.getAchievements(GameType.Draft));
        cb.addItem(FModel.getAchievements(GameType.Sealed));
        cb.addItem(FModel.getAchievements(GameType.Quest));
        cb.addItem(FModel.getAchievements(GameType.PlanarConquest));
        cb.addItem(FModel.getAchievements(GameType.Puzzle));
        cb.addItem(AltWinAchievements.instance);
        cb.addItem(PlaneswalkerAchievements.instance);
        cb.addItem(ChallengeAchievements.instance);
    }

    protected AchievementCollection(String name0, String filename0, boolean isLimitedFormat0) {
        this(name0, filename0, isLimitedFormat0, null);
    }
    
    protected AchievementCollection(String name0, String filename0, boolean isLimitedFormat0, String path0) {
        name = Localizer.getInstance().getMessage(name0);
        filename = filename0;
        isLimitedFormat = isLimitedFormat0;
        path = path0;
        addSharedAchievements();
        addAchievements();
        load();
    }

    protected void addSharedAchievements() {
        add(new GameWinStreak(10, 25, 50, 100));
        add(new MatchWinStreak(10, 25, 50, 100));
        add(new TotalGameWins(250, 500, 1000, 2000));
        add(new TotalMatchWins(100, 250, 500, 1000));
        if (isLimitedFormat) { //make need for speed goal more realistic for limited formats
            add(new NeedForSpeed(8, 6, 4, 2));
        } else {
            add(new NeedForSpeed(5, 3, 1, 0));
        }
        add(new Overkill(-25, -50, -100, -200));
        add(new LifeToSpare(20, 40, 80, 160));
        add(new Hellbent());
        add(new ArcaneMaster());
        add(new StormChaser(5, 10, 20, 50));
        add(new ManaScrewed());
        if (isLimitedFormat) { //lower gold and mythic thresholds based on smaller decks
            add(new ManaFlooded(8, 11, 14, 17));
        } else {
            add(new ManaFlooded(8, 12, 18, 24));
        }
        add(new RagsToRiches());
    }

    protected void addAchievements() {
        if (path != null) {
            final List<String> achievementListFile = FileUtil.readFile(path);
            for (final String s : achievementListFile) {
                if (!s.isEmpty()) {
                    String[] k = StringUtils.split(s, "|");
                    add(k[0],k[1],k[2]);
                }
            }
        }
    }

    protected void add(Achievement achievement) {
        achievements.put(achievement.getKey(), achievement);
    }
    protected void add(String name, String title, String desc) {
        // to overwrite
    }

    public void updateAll(Player player) {
        for (Achievement achievement : achievements.values()) {
            achievement.update(player);
        }
        save();
    }

    public void load() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(new File(filename));
            final NodeList nodes = document.getElementsByTagName("a");
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element el = (Element)nodes.item(i);
                final Achievement achievement = achievements.get(el.getAttribute("name"));
                if (achievement != null) {
                    achievement.loadFromXml(el);
                }
            }
        }
        catch (FileNotFoundException e) {
            //ok if file not found
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void save() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("achievements");
            document.appendChild(root);

            for (Entry<String, Achievement> entry : achievements.entrySet()) {
                Achievement achievement = entry.getValue();
                if (achievement.needSave()) {
                    Element el = document.createElement("a");
                    el.setAttribute("name", entry.getKey());
                    achievement.saveToXml(el);
                    root.appendChild(el);
                }
            }
            XmlUtil.saveDocument(document, filename);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return achievements.size();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Achievement> iterator() {
        return achievements.values().iterator();
    }

    @Override
    public String toString() {
        return name;
    }
}
