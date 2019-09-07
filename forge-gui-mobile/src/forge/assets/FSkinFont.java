package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import forge.FThreads;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.TextBounds;
import forge.util.Utils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FSkinFont {
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 72;

    private static final String TTF_FILE = "font1.ttf";
    private static final Map<Integer, FSkinFont> fonts = new HashMap<Integer, FSkinFont>();
    private static final GlyphLayout layout = new GlyphLayout();

    static {
        FileUtil.ensureDirectoryExists(ForgeConstants.FONTS_DIR);
    }

    public static FSkinFont get(final int unscaledSize) {
        return _get((int)Utils.scale(unscaledSize));
    }
    public static FSkinFont _get(final int scaledSize) {
        FSkinFont skinFont = fonts.get(scaledSize);
        if (skinFont == null) {
            skinFont = new FSkinFont(scaledSize);
            fonts.put(scaledSize, skinFont);
        }
        return skinFont;
    }

    public static FSkinFont forHeight(final float height) {
        int size = MIN_FONT_SIZE + 1;
        while (true) {
            if (_get(size).getLineHeight() > height) {
                return _get(size - 1);
            }
            size++;
        }
    }

    //pre-load all supported font sizes
    public static void preloadAll() {
        for (int size = MIN_FONT_SIZE; size <= MAX_FONT_SIZE; size++) {
            _get(size);
        }
    }

    //delete all cached font files
    public static void deleteCachedFiles() {
        FileUtil.deleteDirectory(new File(ForgeConstants.FONTS_DIR));
        FileUtil.ensureDirectoryExists(ForgeConstants.FONTS_DIR);
    }

    public static void updateAll() {
        for (FSkinFont skinFont : fonts.values()) {
            skinFont.updateFont();
        }
    }

    private final int fontSize;
    private final float scale;
    private BitmapFont font;

    private FSkinFont(int fontSize0) {
        if (fontSize0 > MAX_FONT_SIZE) {
            scale = (float)fontSize0 / MAX_FONT_SIZE;
        }
        else if (fontSize0 < MIN_FONT_SIZE) {
            scale = (float)fontSize0 / MIN_FONT_SIZE;
        }
        else {
            scale = 1;
        }
        fontSize = fontSize0;
        updateFont();
    }

    // Expose methods from font that updates scale as needed
    public TextBounds getBounds(CharSequence str) {
        updateScale(); //must update scale before measuring text
        layout.setText(font, str);
        return new TextBounds(layout.width, layout.height);

    }
    public TextBounds getMultiLineBounds(CharSequence str) {
        updateScale();
        layout.setText(font, str);
        return new TextBounds(layout.width, layout.height);

    }
    public TextBounds getWrappedBounds(CharSequence str, float wrapWidth) {
        updateScale();
        layout.setText(font, str);
        layout.width = wrapWidth;
        return new TextBounds(layout.width, layout.height);

    }
    public float getAscent() {
        updateScale();
        return font.getAscent();
    }
    public float getCapHeight() {
        updateScale();
        return font.getCapHeight();
    }
    public float getLineHeight() {
        updateScale();
        return font.getLineHeight();
    }

    public void draw(SpriteBatch batch, String text, Color color, float x, float y, float w, boolean wrap, int horzAlignment) {
        updateScale();
        font.setColor(color);
        font.draw(batch, text, x, y, w, horzAlignment, wrap);
    }

    //update scale of font if needed
    private void updateScale() {
        if (font.getScaleX() != scale) {
            font.getData().setScale(scale);
        }
    }

    public boolean canShrink() {
        return fontSize > MIN_FONT_SIZE;
    }

    public FSkinFont shrink() {
        return _get(fontSize - 1);
    }

    private void updateFont() {
        if (scale != 1) { //re-use font inside range if possible
            if (fontSize > MAX_FONT_SIZE) {
                font = _get(MAX_FONT_SIZE).font;
            } else {
                font = _get(MIN_FONT_SIZE).font;
            }
            return;
        }

        String fontName = "f" + fontSize;
        FileHandle fontFile = Gdx.files.absolute(ForgeConstants.FONTS_DIR + fontName + ".fnt");
        if (fontFile != null && fontFile.exists()) {
            final BitmapFontData data = new BitmapFontData(fontFile, false);
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() { //font must be initialized on UI thread
                    font = new BitmapFont(data, (TextureRegion)null, true);
                }
            });
        } else {
            generateFont(FSkin.getSkinFile(TTF_FILE), fontName, fontSize);
        }
    }

    private void generateFont(final FileHandle ttfFile, final String fontName, final int fontSize) {
        if (!ttfFile.exists()) { return; }

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);

        //approximate optimal page size
        int pageSize;
        if (fontSize >= 28) {
            pageSize = 256;
        }
        else {
            pageSize = 128;
        }

        //only generate images for characters that could be used by Forge
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!?'.,;:()[]{}<>|/@\\^$-%+=#_&*\u2014\u2022";
        chars += "ÁÉÍÓÚáéíóúÀÈÌÒÙàèìòùÑñÄËÏÖÜäëïöüẞß";
        //generate from zh-CN.properties
        chars += "“”、一三上下不与专且东两个中临为主么义之乐也了争事二于五些交产"
              +  "亮人什仅从他付代以们件价任优会传伤但位低住体何作你使例供侧侵便"
              +  "保信倍候值偏做停偿像允元充先克入全关其具兼兽内再冒决况准减凑几"
              +  "出击分切列则创初删利到制刷前剪副力功加动助励势包化匹区十协单卖"
              +  "博卡印即原去参双发取受变叠口句只召可台史右号司合同名后向吗否含"
              +  "启告员周命和咒品哪唤售商喜器回因团困围图在地场均坊坏坟型域基堆"
              +  "塞境墓增声处备复多大天太失头夹奇奖套好如始子字存它完宏官定宝实"
              +  "宠客害家容对导将小少尝就局层屏展属巅左差已市布带帮常幕并幻序库"
              +  "应底店度建开异弊式引张弧强当录形征很得御徽必志快忽态性总恢息您"
              +  "悬情惊惰想意慢戏成我或战户所手才打执找把抓抗折报拉拟拥择括持指"
              +  "按挑挡挥损换据捷排探接控掷描提摩摸撤播操擎支收改攻放故效数文斗"
              +  "斯新方旅无日旧时明易星是显景暂暴更替最有服期未本术机权束来杯松"
              +  "板构果柄染查标栏样格框档检植模次止正此死殊段每比池没法注洗洛活"
              +  "测消涵混添清渐温渲游源滤灵灼点烁烧然片版牌物特状独率王玩环现珍"
              +  "理瓦生用由画界留略登白百的盒盖盘目直相省看着瞄瞬知破确磨示神票"
              +  "禁离种秘积称移稀程空窗立竞章第等筑筛简管箭类精系素索紧红级线组"
              +  "细终经结给络统继绩续维绿缓编缩缺网置者而联肥背胜能脚自至致舒航"
              +  "般艇色节英范莫获菜蓝薪藏行衍衡补表被西要覆观规视览觉角解触言誓"
              +  "警计认让记许论设证评试诗诡该详语误说请读调谋谜象败费资赌赛赢起"
              +  "距跳踢转轮轻载较辑输辨边过运近返还这进远连述退适选逐递速造道那"
              +  "部都配醒释里重量金钮铁银销锁锋错锦键长门闪闭间队防阴阶阻限除险"
              +  "隆随隐障难雄集雪需静非靠面音页顶项顺须顿预题颜风飞首验骰高魔鹏"
              +  "黑默鼠！（），：？";
        final PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 2, false);
        final FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.characters = chars;
        parameter.size = fontSize;
        parameter.packer = packer;
        final FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(parameter);
        final Array<PixmapPacker.Page> pages = packer.getPages();

        //finish generating font on UI thread
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                Array<TextureRegion> textureRegions = new Array<>();
                for (int i = 0; i < pages.size; i++) {
                    PixmapPacker.Page p = pages.get(i);
                    Texture texture = new Texture(new PixmapTextureData(p.getPixmap(), p.getPixmap().getFormat(), false, false)) {
                        @Override
                        public void dispose() {
                            super.dispose();
                            getTextureData().consumePixmap().dispose();
                        }
                    };
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    textureRegions.addAll(new TextureRegion(texture));
                }

                font = new BitmapFont(fontData, textureRegions, true);

                //create .fnt and .png files for font
                FileHandle pixmapDir = Gdx.files.absolute(ForgeConstants.FONTS_DIR);
                if (pixmapDir != null) {
                    FileHandle fontFile = pixmapDir.child(fontName + ".fnt");
                    BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

                    String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontName);
                    BitmapFontWriter.writeFont(font.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1);
                }

                generator.dispose();
                packer.dispose();
            }
        });
    }
}
