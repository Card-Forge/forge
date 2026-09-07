package forge.card;

import java.util.HashMap;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgeConstants;
import forge.util.CardTranslation;

/**
 * Extends AITest only for its one-time card database initialization.
 */
public class CardTranslationTest extends AITest {

    private String firstActivatedAbilityText(Player p, String cardName) {
        Card c = addCard(cardName, p);
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                return sa.toUnsuppressedString();
            }
        }
        return null;
    }

    /**
     * Freed from the Real's translated oracle was a line short in six languages, so its two
     * abilities were both shown as the translation of "untap" and the card could not be used.
     * Issue #11795.
     */
    @Test
    public void testFreedFromTheRealTapAndUntapReadDifferently() {
        String[] languages = { "de-DE", "es-ES", "fr-FR", "it-IT", "pt-BR", "zh-CN" };
        try {
            for (String language : languages) {
                CardTranslation.preloadTranslation(language, ForgeConstants.LANG_DIR);
                Game game = initAndCreateGame();
                Player p = game.getPlayers().get(1);
                Card freed = addCard("Freed from the Real", p);

                Map<String, SpellAbility> byText = new HashMap<>();
                int activated = 0;
                for (SpellAbility sa : freed.getSpellAbilities()) {
                    if (!sa.isActivatedAbility()) {
                        continue;
                    }
                    activated++;
                    String text = sa.toUnsuppressedString();
                    AssertJUnit.assertFalse(language + ": ability has no description",
                            text.trim().isEmpty());
                    AssertJUnit.assertNull(language + ": two abilities are both shown as ["
                            + text + "]", byText.put(text, sa));
                }
                AssertJUnit.assertEquals(language + ": expected two activated abilities",
                        2, activated);

                String oracle = CardTranslation.getTranslatedOracle("Freed from the Real");
                AssertJUnit.assertEquals(language + ": translated oracle should have one line"
                        + " per English oracle line", 3, oracle.split("\r\n\r\n").length);
            }
        } finally {
            CardTranslation.preloadTranslation("en-US", ForgeConstants.LANG_DIR);
        }
    }

    /**
     * Oracle lines are paired with their translations by position, so a translated oracle that
     * is a line short pairs each line with its neighbour's translation, and the edit-distance
     * lookup then lets two abilities resolve to the same line. Pemmin's Aura is one of the
     * cards whose data is still short a line, so it exercises the guard rather than the data.
     */
    @Test
    public void testEveryAbilityOfPemminsAuraIsShownWithItsOwnText() {
        String[] languages = { "de-DE", "es-ES", "fr-FR", "it-IT", "pt-BR" };
        try {
            for (String language : languages) {
                CardTranslation.preloadTranslation(language, ForgeConstants.LANG_DIR);
                Game game = initAndCreateGame();
                Player p = game.getPlayers().get(1);
                Card aura = addCard("Pemmin's Aura", p);

                Map<String, SpellAbility> byText = new HashMap<>();
                int activated = 0;
                for (SpellAbility sa : aura.getSpellAbilities()) {
                    if (!sa.isActivatedAbility()) {
                        continue;
                    }
                    activated++;
                    String text = sa.toUnsuppressedString();
                    AssertJUnit.assertFalse(language + ": ability has no description",
                            text.trim().isEmpty());
                    AssertJUnit.assertNull(language + ": two abilities are both shown as ["
                            + text + "]", byText.put(text, sa));
                }
                AssertJUnit.assertEquals(language + ": expected four activated abilities",
                        4, activated);
            }
        } finally {
            CardTranslation.preloadTranslation("en-US", ForgeConstants.LANG_DIR);
        }
    }

    /**
     * Keen-Eyed Curator and Withered Wretch carry the same ability, worded differently in
     * Italian. The translation cache used to be keyed on the ability text alone, so whichever
     * card was built first decided how both of them read for the rest of the session.
     */
    @Test
    public void testTranslationDoesNotDependOnWhichCardWasBuiltFirst() {
        String curator = "Keen-Eyed Curator";
        String wretch = "Withered Wretch";
        try {
            CardTranslation.preloadTranslation("it-IT", ForgeConstants.LANG_DIR);
            Player p1 = initAndCreateGame().getPlayers().get(1);
            String curatorFirst = firstActivatedAbilityText(p1, curator);
            String wretchSecond = firstActivatedAbilityText(p1, wretch);

            CardTranslation.preloadTranslation("it-IT", ForgeConstants.LANG_DIR);
            Player p2 = initAndCreateGame().getPlayers().get(1);
            String wretchFirst = firstActivatedAbilityText(p2, wretch);
            String curatorSecond = firstActivatedAbilityText(p2, curator);

            AssertJUnit.assertNotNull(curator + " has no activated ability", curatorFirst);
            AssertJUnit.assertNotNull(wretch + " has no activated ability", wretchFirst);
            AssertJUnit.assertEquals(curator + " reads differently depending on build order",
                    curatorFirst, curatorSecond);
            AssertJUnit.assertEquals(wretch + " reads differently depending on build order",
                    wretchFirst, wretchSecond);
            AssertJUnit.assertFalse("these two cards word the ability differently in Italian,"
                    + " so the test cannot detect a shared cache if they match",
                    curatorFirst.equals(wretchFirst));
        } finally {
            CardTranslation.preloadTranslation("en-US", ForgeConstants.LANG_DIR);
        }
    }
}
