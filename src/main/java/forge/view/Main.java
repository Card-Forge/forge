/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import java.util.ArrayList;

import forge.AllZone;
import forge.Constant;
import forge.ConstantStringArrayList;
import forge.FileUtil;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.view.toolbox.FSkin;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {

    /**
     * Do not instantiate.
     */
    private Main() {
        // intentionally blank
    }

    /**
     * main method for Forge's swing application view.
     * 
     * @param args
     *            an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        ExceptionHandler.registerErrorHandling();
        try {
            final FModel model = new FModel(null);
            Singletons.setModel(model);

            final FSkin skin = new FSkin(Singletons.getModel().getPreferences().getSkin());
            final FView view = new FView(skin);
            Singletons.setView(view);
            AllZone.setSkin(skin);

            // Need this soon after card factory is loaded
            Main.loadDynamicGamedata();

            // TODO this code should go elsewhere, like wherever we start a new
            // game.
            // It is only here to maintain semantic equality with the current
            // code base.

            model.resetGameState();

            view.setModel(model);

        } catch (final Throwable exn) {
            ErrorViewer.showError(exn);
        }
    }

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!Constant.CardTypes.LOADED[0]) {
            final ArrayList<String> typeListFile = FileUtil.readFile("res/gamedata/TypeLists.txt");

            ArrayList<String> tList = null;

            Constant.CardTypes.CARD_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SUPER_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.BASIC_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.LAND_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.CREATURE_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.INSTANT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SORCERY_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ENCHANTMENT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ARTIFACT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.WALKER_TYPES[0] = new ConstantStringArrayList();

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = Constant.CardTypes.CARD_TYPES[0].getList();
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = Constant.CardTypes.SUPER_TYPES[0].getList();
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = Constant.CardTypes.BASIC_TYPES[0].getList();
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = Constant.CardTypes.LAND_TYPES[0].getList();
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = Constant.CardTypes.CREATURE_TYPES[0].getList();
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = Constant.CardTypes.INSTANT_TYPES[0].getList();
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = Constant.CardTypes.SORCERY_TYPES[0].getList();
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = Constant.CardTypes.ENCHANTMENT_TYPES[0].getList();
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = Constant.CardTypes.ARTIFACT_TYPES[0].getList();
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = Constant.CardTypes.WALKER_TYPES[0].getList();
                    }

                    else if (s.length() > 1) {
                        tList.add(s);
                    }
                }
            }
            Constant.CardTypes.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.CardTypes.cardTypes[0].list);
             * System.out.println(Constant.CardTypes.superTypes[0].list);
             * System.out.println(Constant.CardTypes.basicTypes[0].list);
             * System.out.println(Constant.CardTypes.landTypes[0].list);
             * System.out.println(Constant.CardTypes.creatureTypes[0].list);
             * System.out.println(Constant.CardTypes.instantTypes[0].list);
             * System.out.println(Constant.CardTypes.sorceryTypes[0].list);
             * System.out.println(Constant.CardTypes.enchantmentTypes[0].list);
             * System.out.println(Constant.CardTypes.artifactTypes[0].list);
             * System.out.println(Constant.CardTypes.walkerTypes[0].list); }
             */
        }

        if (!Constant.Keywords.LOADED[0]) {
            final ArrayList<String> nskwListFile = FileUtil.readFile("res/gamedata/NonStackingKWList.txt");

            Constant.Keywords.NON_STACKING_LIST[0] = new ConstantStringArrayList();

            if (nskwListFile.size() > 1) {
                for (int i = 0; i < nskwListFile.size(); i++) {
                    final String s = nskwListFile.get(i);
                    if (s.length() > 1) {
                        Constant.Keywords.NON_STACKING_LIST[0].getList().add(s);
                    }
                }
            }
            Constant.Keywords.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.Keywords.NonStackingList[0].list); }
             */
        }

        /*
         * if (!Constant.Color.loaded[0]) { ArrayList<String> lcListFile =
         * FileUtil.readFile("res/gamedata/LandColorList");
         * 
         * if (lcListFile.size() > 1) { for (int i=0; i<lcListFile.size(); i++)
         * { String s = lcListFile.get(i); if (s.length() > 1)
         * Constant.Color.LandColor[0].map.add(s); } }
         * Constant.Keywords.loaded[0] = true; if (Constant.Runtime.DevMode[0])
         * { System.out.println(Constant.Keywords.NonStackingList[0].list); } }
         */
    }
}
