package forge.screens.deckeditor.controllers;

import forge.deck.Deck;
import forge.item.PaperCard;
import forge.properties.ForgeConstants;
import forge.util.ImageUtil;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class DeckHtmlSerializer {
    public static void writeDeckHtml(final Deck d, final File f) {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writeDeckHtml(d, writer);
            writer.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * writeDeck.
     * </p>
     *
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @param out
     *            a {@link java.io.BufferedWriter} object.
     */
    private static void writeDeckHtml(final Deck d, final BufferedWriter out) {
        Template temp;
        final int cardBorder = 0;
        final int height = 319;
        final int width = 222;

        /* Create and adjust the configuration */
        final Configuration cfg = new Configuration();
        try {
            cfg.setClassForTemplateLoading(DeckHtmlSerializer.class, "/");
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            /*
             * ------------------------------------------------------------------
             * -
             */
            /*
             * You usually do these for many times in the application
             * life-cycle:
             */

            /* Get or create a template */
            temp = cfg.getTemplate("proxy-template.ftl");

            /* Create a data-model */
            final Map<String, Object> root = new HashMap<>();
            root.put("title", d.getName());
            final List<String> list = new ArrayList<>();
            for (final Entry<PaperCard, Integer> card : d.getMain()) {
                // System.out.println(card.getSets().get(card.getSets().size() - 1).URL);
                for (int i = card.getValue(); i > 0; --i ) {
                    final PaperCard r = card.getKey();
                    final String url = ForgeConstants.URL_PIC_DOWNLOAD + ImageUtil.getDownloadUrl(r, false);
                    list.add(url);
                }
            }

            final Map<String, Integer> map = new TreeMap<>();
            for (final Entry<PaperCard, Integer> entry : d.getMain()) {
                map.put(entry.getKey().getName(), entry.getValue());
                // System.out.println(entry.getValue() + " " +
                // entry.getKey().getName());
            }

            root.put("urls", list);
            root.put("cardBorder", cardBorder);
            root.put("height", height);
            root.put("width", width);
            root.put("cardlistWidth", width - 11);
            root.put("cardList", map);

            /* Merge data-model with template */
            temp.process(root, out);
            out.flush();
        } catch (final IOException | TemplateException e) {
            System.out.println(e.toString());
        }
    }
}