package forge.achievement;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.interfaces.IComboBox;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import forge.util.XmlUtil;

public abstract class AchievementCollection implements Iterable<Achievement> {
    protected final Map<String, Achievement> achievements = new LinkedHashMap<String, Achievement>();
    protected final String name, filename;
    protected final boolean isLimitedFormat;

    static {
        FileUtil.ensureDirectoryExists(ForgeConstants.ACHIEVEMENTS_DIR);
    }

    public static void updateAll(final PlayerControllerHuman controller) {
        //don't update achievements if player cheated during game
        if (controller.hasCheated()) {
            return;
        }

        final Game game = controller.getGame();
        final Player player = controller.getPlayer();

        //update all achievements for GUI player after game finished
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                FModel.getAchievements(game.getRules().getGameType()).updateAll(player);
                AltWinAchievements.instance.updateAll(player);
                PlaneswalkerAchievements.instance.updateAll(player);
                ChallengeAchievements.instance.updateAll(player);
            }
        });
    }

    public static void buildComboBox(IComboBox<AchievementCollection> cb) {
        cb.addItem(FModel.getAchievements(GameType.Constructed));
        cb.addItem(FModel.getAchievements(GameType.Draft));
        cb.addItem(FModel.getAchievements(GameType.Sealed));
        cb.addItem(FModel.getAchievements(GameType.Quest));
        cb.addItem(AltWinAchievements.instance);
        cb.addItem(PlaneswalkerAchievements.instance);
        cb.addItem(ChallengeAchievements.instance);
    }

    protected AchievementCollection(String name0, String filename0, boolean isLimitedFormat0) {
        name = name0;
        filename = filename0;
        isLimitedFormat = isLimitedFormat0;
        addSharedAchivements();
        addAchievements();
        load();
    }

    protected void addSharedAchivements() {
        add(new GameWinStreak(10, 25, 50, 100));
        add(new MatchWinStreak(10, 25, 50, 100));
        add(new TotalGameWins(250, 500, 1000, 2000));
        add(new TotalMatchWins(100, 250, 500, 1000));
        if (isLimitedFormat) { //make need for speed goal more realistic for limited formats
            add(new NeedForSpeed(8, 6, 4, 2));
        }
        else {
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
        }
        else {
            add(new ManaFlooded(8, 12, 18, 24));
        }
        add(new RagsToRiches());
    }

    protected abstract void addAchievements();

    protected void add(Achievement achievement) {
        achievements.put(achievement.getKey(), achievement);
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
