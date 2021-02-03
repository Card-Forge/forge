package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import forge.FThreads;
import forge.Forge;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.TextBounds;
import forge.util.Utils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FSkinFont {
    private static final int MIN_FONT_SIZE = 8;
    private static int MAX_FONT_SIZE = 72;

    private static final int MAX_FONT_SIZE_LESS_GLYPHS = 72;
    private static final int MAX_FONT_SIZE_MANY_GLYPHS = 36;

    private static final String TTF_FILE = "font1.ttf";
    private static final Map<Integer, FSkinFont> fonts = new HashMap<>();

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
    public static void preloadAll(String language) {
        //todo:really check the language glyph is a lot
        MAX_FONT_SIZE = (language.equals("zh-CN")) ? MAX_FONT_SIZE_MANY_GLYPHS : MAX_FONT_SIZE_LESS_GLYPHS;
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
    static int indexOf (CharSequence text, char ch, int start) {
        final int n = text.length();
        for (; start < n; start++)
            if (text.charAt(start) == ch) return start;
        return n;

    }
    public int computeVisibleGlyphs (CharSequence str, int start, int end, float availableWidth) {
        BitmapFontData data = font.getData();
        int index = start;
        float width = 0;
        Glyph lastGlyph = null;
        availableWidth /= data.scaleX;

        for (; index < end; index++) {
            char ch = str.charAt(index);
            if (ch == '[' && data.markupEnabled) {
                index++;
                if (!(index < end && str.charAt(index) == '[')) { // non escaped '['
                    while (index < end && str.charAt(index) != ']')
                        index++;
                    continue;
                }
            }

            Glyph g = data.getGlyph(ch);

            if (g != null) {
                if (lastGlyph != null) width += lastGlyph.getKerning(ch);
                if ((width + g.xadvance) - availableWidth > 0.001f) break;
                width += g.xadvance;
                lastGlyph = g;
            }
        }

        return index - start;
    }
    public boolean isBreakChar (char c) {
        BitmapFontData data = font.getData();
        if (data.breakChars == null) return false;
        for (char br : data.breakChars)
            if (c == br) return true;
        return false;
    }
    static boolean isWhitespace (char c) {
        switch (c) {
            case '\n':
            case '\r':
            case '\t':
            case ' ':
                return true;
            default:
                return false;
        }
    }
    // Expose methods from font that updates scale as needed
    public TextBounds getBounds(CharSequence str) {
        updateScale(); //must update scale before measuring text
        return getBounds(str, 0, str.length());
    }
    public TextBounds getBounds(CharSequence str, int start, int end) {
        BitmapFontData data = font.getData();
        //int start = 0;
        //int end = str.length();
        int width = 0;
        Glyph lastGlyph = null;

        while (start < end) {
            char ch = str.charAt(start++);
            if (ch == '[' && data.markupEnabled) {
                if (!(start < end && str.charAt(start) == '[')) { // non escaped '['
                    while (start < end && str.charAt(start) != ']')
                        start++;
                    start++;
                    continue;
                }
                start++;
            }
            lastGlyph = data.getGlyph(ch);
            if (lastGlyph != null) {
                width = lastGlyph.xadvance;
                break;
            }
        }
        while (start < end) {
            char ch = str.charAt(start++);
            if (ch == '[' && data.markupEnabled) {
                if (!(start < end && str.charAt(start) == '[')) { // non escaped '['
                    while (start < end && str.charAt(start) != ']')
                        start++;
                    start++;
                    continue;
                }
                start++;
            }

            Glyph g = data.getGlyph(ch);
            if (g != null) {
                width += lastGlyph.getKerning(ch);
                lastGlyph = g;
                width += g.xadvance;
            }
        }

        return new TextBounds(width * data.scaleX, data.capHeight);

    }
    public TextBounds getMultiLineBounds(CharSequence str) {
        updateScale();
        BitmapFontData data = font.getData();
        int start = 0;
        float maxWidth = 0;
        int numLines = 0;
        int length = str.length();

        while (start < length) {
            int lineEnd = indexOf(str, '\n', start);
            float lineWidth = getBounds(str, start, lineEnd).width;
            maxWidth = Math.max(maxWidth, lineWidth);
            start = lineEnd + 1;
            numLines++;
        }

        return new TextBounds(maxWidth, data.capHeight + (numLines - 1) * data.lineHeight);

    }
    public TextBounds getWrappedBounds(CharSequence str, float wrapWidth) {
        updateScale();
        BitmapFontData data = font.getData();
        if (wrapWidth <= 0) wrapWidth = Integer.MAX_VALUE;
        int start = 0;
        int numLines = 0;
        int length = str.length();
        float maxWidth = 0;
        while (start < length) {
            int newLine = indexOf(str, '\n', start);
            int lineEnd = start + computeVisibleGlyphs(str, start, newLine, wrapWidth);
            int nextStart = lineEnd + 1;
            if (lineEnd < newLine) {
                // Find char to break on.
                while (lineEnd > start) {
                    if (isWhitespace(str.charAt(lineEnd))) break;
                    if (isBreakChar(str.charAt(lineEnd - 1))) break;
                    lineEnd--;
                }

                if (lineEnd == start) {

                    if (nextStart > start + 1) nextStart--;

                    lineEnd = nextStart; // If no characters to break, show all.

                } else {
                    nextStart = lineEnd;

                    // Eat whitespace at start of wrapped line.

                    while (nextStart < length) {
                        char c = str.charAt(nextStart);
                        if (!isWhitespace(c)) break;
                        nextStart++;
                        if (c == '\n') break; // Eat only the first wrapped newline.
                    }

                    // Eat whitespace at end of line.
                    while (lineEnd > start) {

                        if (!isWhitespace(str.charAt(lineEnd - 1))) break;
                        lineEnd--;
                    }
                }
            }

            if (lineEnd > start) {
                float lineWidth = getBounds(str, start, lineEnd).width;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            start = nextStart;
            numLines++;
        }

        return new TextBounds(maxWidth, data.capHeight + (numLines - 1) * data.lineHeight);
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
        chars += "ÁÉÍÓÚáéíóúÀÈÌÒÙàèìòùÑñÄËÏÖÜäëïöüẞß¿¡";
        //generate from zh-CN.properties,and cardnames-zh-CN.txt
        //forge generate 3000+ characters cache need Take some time(MIN_FONT_SIZE - MAX_FONT_SIZE all size)
        if (Forge.locale.equals("zh-CN"))
            chars   += "~·Æâû​‘’“”‧−●、。「」『』【】一丁七万三上下"
                    +  "不与丑专且世丘业丛东丝丢两严丧个中丰串临丸丹为主丽举乃久么义"
                    +  "之乌乍乎乐乔乖乘乙九也习乡书买乱乳乾了予争事二于云互五井亘亚"
                    +  "些亡亢交亥亦产亨享京亭亮亲亵人亿什仁仅仆仇今介仍从仑仓仕他仗"
                    +  "付仙代令以仪们仰仲件价任份仿伊伍伏伐休众优伙会伟传伤伦伪伯伱"
                    +  "伴伶伸伺似伽但位低住佐佑体何余佚佛作你佣佩佬佯佳使例侍侏供依"
                    +  "侠侣侥侦侧侪侬侮侯侵便促俄俊俐俑俗俘保信修俯俸個倍倒候借倡倦"
                    +  "倨倪债值倾假偏做停健偶偷偿傀傅傍储催傲像僚僧僭僵僻儒儡儿兀允"
                    +  "元兄充兆兇先光克免兑兔兕党入全八公六兰共关兴兵其具典兹养兼兽"
                    +  "内册再冑冒冕写军农冠冢冥冬冰冲决况冶冷冻净准凋凌减凑凛凝几凡"
                    +  "凤凭凯凰凶凸出击凿刀刃分切刈刍刑划列刘则刚创初删判利别刮到制"
                    +  "刷券刹刺刻刽剁剂剃削剌前剎剐剑剖剜剥剧剩剪副割剽劈力劝办功加"
                    +  "务劣动助努劫励劲劳势勃勇勉勋勒勘募勤勾包匍匐匕化北匙匝匠匪匹"
                    +  "区医匿十千升午半华协卑卒卓单卖南博卜占卡卢卤卦卧卫印危即却卵"
                    +  "卷卸厂厄厅历厉压厌厚原厢厥厦厨去参叉及友双反发叔取受变叙叛叠"
                    +  "口古句另叨只叫召叮可台史右叶号司叹吁吃各合吉吊同名后吏吐向吓"
                    +  "吕吗君吞吟否含听吮启吱吴吸吹吻吼呆告呕员周味呼命咆和咏咒咕咬"
                    +  "咯咳咸咽哀品哄哇哈响哑哔哗哥哨哩哪哭哮哲哺唐唤售唯唱啃啄商啜"
                    +  "啪啮啸喀喂善喉喊喋喘喙喜喝喧喰喷嗄嗅嗔嗜嗡嗣嗫嘉嘎嘘嘲嘴嘶噜"
                    +  "噤器噬嚎嚣嚼囊囚四回因团囤园困囱围固国图圆圈團土圣在圮地场圾"
                    +  "址均坊坍坎坏坐坑块坚坛坝坞坟坠坡坤坦坪坷垂垃垄型垒垛垠垢垣垦"
                    +  "垮埃埋城埔域培基堂堆堌堑堕堡堤堪堰塌塑塔塘塞填境墓墙增墟墨壁"
                    +  "壅壕壤士壬壮声壳壶处备复夏外多夜够大天太夫央失头夷夸夹夺奇奈"
                    +  "奉奋奎契奔奖套奚奠奢奥女奴她好如妃妄妆妇妈妍妖妙妣妥妪妮妲妹"
                    +  "姆姊始姓委姜姥姬姿威娃娅娘娜娥娱婆婉婚婪婴婶媒嫁嫩嬉子孑孔孕"
                    +  "字存孙孚孟孢季孤学孪孳孵孽宁它宅宇守安完宏宗官宙定宛宜宝实宠"
                    +  "审客宣室宪宫宰害宴宵家容宽宾宿寂寄密寇富寒寓寝察寡寨寰寸对寺"
                    +  "寻导封射将尉尊小少尔尖尘尚尝尤尬就尸尹尺尼尽尾局屁层居屈届屋"
                    +  "屏屑展属屠履屯山屹屿岁岑岔岖岗岚岛岩岭岱岳岸峋峡峭峰峻崇崎崔"
                    +  "崖崛崩崽嵌嶙巅巍川州巡巢工左巧巨巩巫差己已巳巴巷巾币市布帅帆"
                    +  "师希帐帕帖帘帚帜帝带席帮帷常帼帽幄幅幔幕干平年并幸幻幼幽广庄"
                    +  "庆庇床序庐库应底店庙府庞废度座庭庶康庸廉廊廓延廷建开异弃弄弊"
                    +  "式弑弒弓引弗弘弟张弥弦弧弩弯弱張弹强归当录彗形彩彰影役彻彼往"
                    +  "征径待很徊律後徒徕得徘徙從御復循微徵德徽心必忆忌忍忏忒志忘忠"
                    +  "忤忧快忱念忽忾忿怀态怎怒怖思怠急性怨怪怯总恋恍恐恒恕恢恣恨恩"
                    +  "恫恭息恰恳恶恸恼悉悍悔悖悟患悦悬悯悲悼情惇惊惑惘惚惠惧惨惩惫"
                    +  "惯惰想惶惹愁愈愎意愚感愣愤愧愿慈慌慎慑慕慢慧慨慰慷憎憩懈懒懦"
                    +  "懿戈戍戎戏成我戒戕或战戟截戮戳戴户戾房所扁扇扈扉手才扎扑扒打"
                    +  "托扣执扩扫扬扭扮扯扰找承技抄抉把抑抓投抖抗折抚抛抢护报披抱抵"
                    +  "抹押抽拂拆拇拉拌拍拒拓拔拖拗拘拙招拜拟拣拥拦拧拨择括拯拱拳拷"
                    +  "拼拽拾拿持挂指按挑挖挚挛挞挟挠挡挣挤挥挪挫振挺挽捆捉捍捐捕捞"
                    +  "损换捣捧据捷捻掀授掉掌掐排掘掠探接控推掩措掮掳掷揍描提插握揭"
                    +  "援揽搁搅搏搐搜搞搧搬搭携摄摆摇摈摘摧摩摸摹撒撕撞撤撬播撵撼擂"
                    +  "擅操擎擒擞擢擦攀攫支收攸改攻放政故效敌敏救敕教敞敢散敦敬数敲"
                    +  "整文斐斑斓斗料斜斤斥斧斩断斯新方於施旁旅旋族旗无既日旧旨早旭"
                    +  "旱时旷旸旺昂昆昌明昏易昔昙星映春昨昭是昵昼显晃晋晒晓晕晖晚晦"
                    +  "晨普景晰晴晶晷智暂暖暗暦暮暴曙曜曝曦曲曳更曹曼曾替最月有服朗"
                    +  "望朝期朦木未末本札术朵机朽杀杂权杉李杏材村杖杜束条来杨杯杰松"
                    +  "板极构析林枚果枝枢枪枭枯架枷柄柏某染柜查柩柯柱柳柴栅标栈栋栏"
                    +  "树栓栖栗校株样核根格栽桂桃框案桌桎桑桓桠档桥桦桨桩桶梁梅梏梓"
                    +  "梢梣梦梧梨梭梯械检棄棍棒棕棘棚森棱棺椁植椎椒椽楂楔楚楣楼概榄"
                    +  "榆榔榜榨榴槁槌槛槽模横樱樵橇橡橫檀檄檐次欢欣欧欲欺款歇歉歌止"
                    +  "正此步武歧歪死歼殁殆殇殉殊残殍殒殓殖殡殴段殷殿毁毅母每毒比毕"
                    +  "毛毡毯氅氏民氓气氛氤氦氧氲水永汀汁求汇汉汐汗汛池污汤汨汪汰汲"
                    +  "汹汽沃沈沉沌沐沙沟没沥沦沫沮河沸油治沼沾沿泄泉泊法泛泞泡波泣"
                    +  "泥注泪泯泰泳泻泽洁洋洒洗洛洞津洪洲活洼派流浅浆浇浊测济浏浑浓"
                    +  "浚浩浪浮浴海浸涂涅消涉涌涎涛涟涡涤润涨涩液涵涸淆淋淘淝淡淤淬"
                    +  "深混淹添清渊渎渐渔渗渝渠渡渣渥温港渲渴游渺湍湖湛湮湾湿溃溅源"
                    +  "溜溢溪溯溶溺滋滑滓滔滚滞满滤滥滨滩滴漂漏演漠漩漫漾潘潜潭潮澄"
                    +  "澈澜澹激濑濒瀑瀚灌火灭灯灰灵灶灼灾灿炉炎炙炫炬炭炮炸点炼炽烁"
                    +  "烂烈烙烛烟烤烦烧烨烫烬热烽焉焊焚焦焰然煌煎煞煤照煮煽熄熊熏熔"
                    +  "熙熟熠熵燃燎燕燧爆爪爬爱爵父片版牌牒牙牛牝牡牢牦牧物牲牵特牺"
                    +  "犀犁犄犒犧犬犯状狂狄狈狐狒狗狙狞狠狡狨狩独狭狮狰狱狷狸狼猁猎"
                    +  "猖猛猜猧猩猪猫献猴猾猿獒獗獠獭獴獾玄率玉王玖玛玩玫环现玷玺玻"
                    +  "珀珂珊珍珑珠班球理琉琐琥琪琳琴琵琼瑕瑙瑚瑛瑜瑞瑟瑰瑾璃璞璧瓜"
                    +  "瓣瓦瓮瓯瓶瓷甄甘甜生用甩甫田由甲申电男画畅界畏留畜略番畸畿疆"
                    +  "疏疑疗疚疡疣疤疫疮疯疲疵疹疼疽疾病症痉痍痕痛痞痢痣痨痪痴痹瘟"
                    +  "瘠瘤瘫瘴癖癣癫癸登白百的皆皇皈皮皱皿盆盈益盎盐监盒盔盖盗盘盛"
                    +  "盟目盲直相盾省看真眠眨眩眷眺眼着睁睡督睥睨睿瞄瞌瞒瞥瞪瞬瞭瞰"
                    +  "瞳矛矢知矫短矮石矾矿码砂砍研砖砦砧破砸砾础硌硕硫硬确碍碎碑碛"
                    +  "碟碧碰碳碴碻碾磁磊磐磨磷磺礁示礼社祀祈祓祖祝神祟祠祥票祭祷祸"
                    +  "禁禄福禳离禽私秃秉秋种科秘秣秤秩积称移秽稀程稍税稚稳稻稼穆穗"
                    +  "穰穴究穷穹空穿突窃窄窍窑窒窖窗窘窜窝窟窥立竖站竞章童竭端竹笏"
                    +  "笑笔笛笞符笨第笼等筏筑筒答策筛筝筱筹签简箔算箝管箭箱箴篇篓篮"
                    +  "篱篷簇簧簪簸籍米类粉粒粖粗粘粮粹精糊糖糙糜糟系素索紧紫累絮縛"
                    +  "繁纂纠红约级纪纬纯纱纳纵纶纷纸纹纺纽线练组绅细织终绊绍经绑绒"
                    +  "结绕绘给绚络绝绞统绣绥继绩绪续绮绯绳维绵综绽绿缀缄缅缆缇缉缍"
                    +  "缎缓缕编缘缚缝缠缤缧缩缪缰缸缺罅网罔罕罗罚罡罩罪置署罵羁羊美"
                    +  "羔羚羞群羽翁翅翎翔翘翟翠翡翦翰翱翻翼耀老考者而耍耐耕耗耘耙耳"
                    +  "耶耸耽聊聋职聒联聚聪肃肆肇肉肋肌肖肝肠肢肤肥肩肮肯育肴肺肿胀"
                    +  "胁胃胆背胎胖胜胞胡胧胫胶胸能脂脆脉脊脏脐脑脓脚脱脸腐腑腔腕腥"
                    +  "腱腹腾腿膂膏膛膜膝臂臃臣自臭至致舌舍舒舞舟航般舰舱船艇良艰色"
                    +  "艺艾节芒芙芜芥芬芭芮花芳芽苇苍苏苔苗苛苜苟若苦英茁茂范茅茉茎"
                    +  "茜茧茨茫茵茸茹荀荆草荒荚荡荣荨荫药荷荻莉莎莓莫莱莲莳获莽菁菇"
                    +  "菈菊菌菜菲萃萌萍萎萝萤营萦萧萨萼落著葛董葬葱葵蒂蒙蒸蒺蓄蓑蓓"
                    +  "蓝蓟蓿蔑蔓蔚蔷蔻蔽蕈蕊蕨蕴蕾薄薇薙薛薪薮藉藏藐藓藜藤藻虎虏虐"
                    +  "虑虔虚虫虱虹蚀蚁蚂蚊蚋蚣蚪蚺蛆蛇蛊蛋蛎蛙蛛蛞蛭蛮蛰蛸蛾蜀蜂蜃"
                    +  "蜇蜈蜉蜍蜒蜓蜕蜗蜘蜜蜡蜥蜴蜷蜻蜿蝇蝉蝌蝎蝓蝗蝙蝠蝣蝾螂螃螅融"
                    +  "螫螯螳螺蟀蟋蟑蟒蟥蟹蟾蠕蠢蠹血衅行衍街衡衣补表衫衰衷袁袂袋袍"
                    +  "袖被袭袱裁裂装裔裕裘裸褐褛褪褫褴褶襄西要覆见观规觅视览觉觊角"
                    +  "解觥触言詹誉誓警计认讧讨让训议讯记讲许论讽设访诀证评诅识诈诉"
                    +  "词译试诗诘诚诛话诞诡该详诫语误诱诲说诵请诸诺读谀谁调谆谈谊谋"
                    +  "谍谎谐谕谗谜谟谢谣谤谦谧谨谬谭谱谴谵谷豁豆豚象豢豪豹豺貂貌贝"
                    +  "贞负贡财责贤败货质贩贪贫贬购贮贯贱贴贵贷贸费贺贼贾贿赂赃资赋"
                    +  "赌赎赏赐赔赖赘赛赞赠赢赤赦赫走赵赶起趁超越趋足趾跃跄跑跖跚跛"
                    +  "距跟跨路跳践跺踉踏踝踢踩踪踵踽蹂蹄蹊蹋蹒蹦蹬躁躏身躯躲車车轨"
                    +  "轩转轭轮软轰轴轻载较辉辏辐辑输辖辗辙辛辜辞辟辨辩辫辰辱边辽达"
                    +  "迁迂迅过迈迎运近返还这进远违连迟迦迩迪迫迭述迳迷迸迹追退送适"
                    +  "逃逆选逊透逐递途逗通逝逞速造逡逢逮逸逻逼遁遂遇遍遏道遗遣遥遨"
                    +  "遭遮遵遽避邀邃還邑那邦邪邬邸郁郊郎部都鄙酋配酒酬酷酸酿醉醒采"
                    +  "釉释里重野量金鉴鑫针钉钓钗钙钛钜钝钟钢钥钦钨钩钮钯钱钳钴钵钻"
                    +  "钽铁铃铅铎铐铜铠铬铭铲银铸铺链销锁锄锅锈锋锐错锡锢锤锥锦锭键"
                    +  "锯锹锺锻镇镋镐镕镖镜镬镰镳镶长間闇门闩闪闭问闯闲间闷闸闹闻阀"
                    +  "阁阅队阱防阳阴阵阶阻阿陀附际陆陋降限院除陨险陪陲陵陶陷隆随隐"
                    +  "隔隘隙障隧隶隼难雀雄雅集雇雉雏雕雨雪雯雳零雷雹雾需霆震霉霍霓"
                    +  "霖霜霞霰露霸霹霾青靖静非靠靡面革靴靶靼鞋鞍鞑鞭韦韧韩音韵韶页"
                    +  "顶项顺须顽顾顿颂预颅领颈颊颌频颓题颚颜额颠颤风飒飓飘飙飞食飨"
                    +  "餍餐餮饕饥饭饮饰饱饴饵饶饼饿馆馈馐馑首香馨马驭驮驯驰驱驳驹驻"
                    +  "驼驽驾驿骁骂骄骆骇验骏骐骑骗骚骡骤骨骰骶骷骸骼髅髑髓高鬃鬓鬣"
                    +  "鬼魁魂魄魅魇魈魏魔魟鰴鱆鱼鲁鲜鲤鲨鲮鲸鲽鳃鳄鳍鳐鳖鳗鳝鳞鴶鵰"
                    +  "鸟鸠鸡鸢鸣鸥鸦鸽鸿鹂鹅鹉鹊鹏鹗鹞鹤鹦鹩鹫鹭鹰鹿麋麒麟麦麻黄黎"
                    +  "黏黑默黛黜黝點黠黯鼎鼓鼠鼬鼹鼻齐齑齿龇龙龟！（），．／：；？"
                    +  "～";

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
