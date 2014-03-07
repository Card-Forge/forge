package forge.utils;

import com.badlogic.gdx.Gdx;

import forge.card.CardDb;
import forge.deck.Deck;
import forge.deck.generation.DeckGenerator2Color;
import forge.deck.generation.DeckGenerator3Color;
import forge.deck.generation.DeckGenerator5Color;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.DeckGeneratorMonoColor;
import forge.model.FModel;

public class Utils {
    private final static float ppcX = Gdx.graphics.getPpcX();
    private final static float ppcY = Gdx.graphics.getPpcY();
    private final static float AVG_FINGER_SIZE_CM = 1.1f;

    public final static float AVG_FINGER_WIDTH = Math.round(cmToPixelsX(AVG_FINGER_SIZE_CM)); //round to nearest int to reduce floating point display issues
    public final static float AVG_FINGER_HEIGHT = Math.round(cmToPixelsY(AVG_FINGER_SIZE_CM));

    public static float cmToPixelsX(float cm) {
        return ppcX * cm;
    }
    public static float cmToPixelsY(float cm) {
        return ppcY * cm;
    }

    public static Deck generateRandomDeck(final int colorCount0) {
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        DeckGeneratorBase gen = null;
        switch (colorCount0) {
            case 1: gen = new DeckGeneratorMonoColor(cardDb, null);             break;
            case 2: gen = new DeckGenerator2Color(cardDb, null, null);          break;
            case 3: gen = new DeckGenerator3Color(cardDb, null, null, null);    break;
            case 5: gen = new DeckGenerator5Color(cardDb);                      break;
        }

        if (gen != null) {
            final Deck deck = new Deck();
            gen.setSingleton(false);
            gen.setUseArtifacts(false);
            deck.getMain().addAll(gen.getDeck(60, false));
            return deck;
        }
        return null;
    }
}
