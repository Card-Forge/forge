package forge.model;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.properties.ForgeConstants;
import forge.util.XmlUtil;
import forge.util.gui.SOptionPane;

public enum Achievement {
    WinStreak("Win Streak", true,
            "Win 10 games in a row.", 10,
            "Win 25 games in a row.", 25,
            "Win 50 games in a row.", 50,
            new Evaluator() {
                @Override
                public int evaluate(Player player, Game game, int current) {
                    if (player.getOutcome().hasWon()) {
                        return current + 1;
                    }
                    return 0; //reset if player didn't win
                }
            }),
    TotalWins("Total Wins", true,
            "Win 100 games.", 100,
            "Win 250 games.", 250,
            "Win 500 games.", 500,
            new Evaluator() {
                @Override
                public int evaluate(Player player, Game game, int current) {
                    if (player.getOutcome().hasWon()) {
                        return current + 1;
                    }
                    return current;
                }
            }),
    Hellbent("Hellbent", false,
            "Win game with no cards in hand.", 1,
            "Win game with no cards in hand or library.", 2,
            "Win game with no cards in hand, library, or graveyard.", 3,
            new Evaluator() {
                @Override
                public int evaluate(Player player, Game game, int current) {
                    if (player.getOutcome().hasWon()) {
                        if (player.getZone(ZoneType.Hand).size() == 0) {
                            if (player.getZone(ZoneType.Library).size() == 0) {
                                if (player.getZone(ZoneType.Graveyard).size() == 0) {
                                    return 3;
                                }
                                return 2;
                            }
                            return 1;
                        }
                    }
                    return 0;
                }
            });

    private final String displayName, bronzeDesc, silverDesc, goldDesc;
    private final int bronzeThreshold, silverThreshold, goldThreshold;
    private final boolean showBest;
    private final Evaluator evaluator;
    private int best, current;

    private Achievement(String displayName0, boolean showBest0,
            String bronzeDesc0, int bronzeThreshold0,
            String silverDesc0, int silverThreshold0,
            String goldDesc0, int goldThreshold0,
            Evaluator evaluator0) {
        displayName = displayName0;
        showBest = showBest0;
        bronzeDesc = bronzeDesc0;
        bronzeThreshold = bronzeThreshold0;
        silverDesc = silverDesc0;
        silverThreshold = silverThreshold0;
        goldDesc = goldDesc0;
        goldThreshold = goldThreshold0;
        evaluator = evaluator0;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getBronzeDesc() {
        return bronzeDesc;
    }
    public String getSilverDesc() {
        return silverDesc;
    }
    public String getGoldDesc() {
        return goldDesc;
    }
    public int getBest() {
        return best;
    }
    public boolean showBest() {
        return showBest;
    }
    public boolean earnedGold() {
        return best >= goldThreshold;
    }
    public boolean earnedSilver() {
        return best >= silverThreshold;
    }
    public boolean earnedBronze() {
        return best >= bronzeThreshold;
    }

    public static void updateAll(Player player) {
        for (Achievement achievement : Achievement.values()) {
            achievement.update(player, false);
        }
        save();
    }

    public void update(Player player) {
        update(player, true);
    }
    private void update(Player player, boolean save) {
        current = evaluator.evaluate(player, player.getGame(), current);
        if (current > best) {
            int oldBest = best;
            best = current;

            String type = null;
            FSkinProp image = null;
            String desc = null;
            if (earnedGold()) {
                if (oldBest < goldThreshold) {
                    type = "Gold";
                    image = FSkinProp.ICO_QUEST_GOLD;
                    desc = goldDesc;
                }
            }
            else if (earnedSilver()) {
                if (oldBest < silverThreshold) {
                    type = "Silver";
                    image = FSkinProp.ICO_QUEST_GOLD;
                    desc = silverDesc;
                }
            }
            else if (earnedBronze()) {
                if (oldBest < bronzeThreshold) {
                    type = "Bronze";
                    image = FSkinProp.ICO_QUEST_GOLD;
                    desc = bronzeDesc;
                }
            }
            if (type != null) {
                SOptionPane.showMessageDialog("You've earned a " + type + " trophy!\n\n" +
                        displayName + " - " + desc, "Achievement Earned", image);
            }
        }
        if (save) {
            save();
        }
    }

    public static void load() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(ForgeConstants.ACHIEVEMENTS_FILE);
            final NodeList cards = document.getElementsByTagName("a");
            for (int i = 0; i < cards.getLength(); i++) {
                final Element el = (Element)cards.item(i);
                final Achievement achievement = Achievement.valueOf(el.getAttribute("name"));
                achievement.best = getIntAttribute(el, "best");
                achievement.current = getIntAttribute(el, "current");
            }
        }
        catch (FileNotFoundException e) {
            //ok if file not found
        }
        catch (MalformedURLException e) {
            //ok if file not found
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getIntAttribute(Element el, String name) {
        String value = el.getAttribute(name);
        if (value.length() > 0) {
            try {
                return Integer.parseInt(value);
            }
            catch (Exception ex) {}
        }
        return 0;
    }

    private static void save() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("achievements");
            document.appendChild(root);

            for (Achievement achievement : Achievement.values()) {
                if (achievement.best > 0 || achievement.current > 0) {
                    Element a = document.createElement("a");
                    a.setAttribute("name", achievement.name());
                    a.setAttribute("best", String.valueOf(achievement.best));
                    a.setAttribute("current", String.valueOf(achievement.current));
                    root.appendChild(a);
                }
            }
            XmlUtil.saveDocument(document, ForgeConstants.ACHIEVEMENTS_FILE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private interface Evaluator {
        int evaluate(Player player, Game game, int current);
    }
}
