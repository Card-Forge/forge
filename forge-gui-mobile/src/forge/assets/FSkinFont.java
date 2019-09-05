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
    private static final Map<Integer, FSkinFont> fonts = new HashMap<>();
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
        //common chinese charsets
        chars += "阿啊哎哀唉埃挨癌矮艾爱碍安氨俺岸按案暗昂凹熬傲奥澳八巴叭吧拔把"
              +  "坝爸罢霸白百柏摆败拜班般颁斑搬板版办半伴扮瓣邦帮膀傍棒包胞宝饱"
              +  "保堡报抱豹暴爆卑杯悲碑北贝备背倍被辈奔本崩逼鼻比彼笔币必毕闭辟"
              +  "碧蔽壁避臂边编蝙鞭扁便变遍辨辩标表别宾滨冰兵丙柄饼并病拨波玻剥"
              +  "播脖伯驳泊勃博搏膊薄卜补捕不布步部擦猜才材财裁采彩踩菜蔡参餐残"
              +  "蚕惨灿仓苍舱藏操曹槽草册侧测策层叉插查茶察差拆柴缠产阐颤昌长肠"
              +  "尝偿常厂场畅倡唱抄超巢朝潮吵炒车扯彻撤尘臣沉陈闯衬称趁撑成呈承"
              +  "诚城乘惩程橙吃池驰迟持匙尺齿斥赤翅充冲虫崇抽仇绸愁筹酬丑瞅臭出"
              +  "初除厨础储楚处触川穿传船喘串窗床晨创吹垂锤春纯唇醇词瓷慈辞磁雌"
              +  "此次刺从匆葱聪丛凑粗促催脆翠村存寸措错搭达答打大呆代带待袋逮戴"
              +  "丹单担胆旦但诞弹淡蛋氮当挡党荡刀导岛倒蹈到盗道稻得德的灯登等邓"
              +  "凳瞪低堤滴迪敌笛底抵地弟帝递第颠典点电店垫淀殿雕吊钓调掉爹跌叠"
              +  "蝶丁叮盯钉顶订定丢东冬懂动冻洞都斗抖陡豆督毒读独堵赌杜肚度渡端"
              +  "短段断锻堆队对吨敦蹲盾顿多夺朵躲俄鹅额恶饿鳄恩儿而尔耳二发乏伐"
              +  "罚阀法帆番翻凡烦繁反返犯泛饭范贩方坊芳防妨房肪仿访纺放飞非啡菲"
              +  "肥废沸肺费分纷芬坟粉份奋愤粪丰风枫封疯峰锋蜂冯逢缝凤奉佛否夫肤"
              +  "孵弗伏扶服浮符幅福辐蝠抚府辅腐父付妇负附复赴副傅富赋腹覆该改钙"
              +  "盖溉概干甘杆肝赶敢感刚岗纲缸钢港高搞稿告戈哥胳鸽割歌阁革格葛隔"
              +  "个各给根跟更耕工弓公功攻供宫恭巩拱共贡勾沟钩狗构购够估咕姑孤菇"
              +  "古谷股骨鼓固故顾瓜刮挂拐怪关观官冠馆管贯惯灌罐光广归龟规硅轨鬼"
              +  "柜贵桂滚棍郭锅国果裹过哈孩海害含函寒韩罕喊汉汗旱杭航毫豪好号浩"
              +  "耗呵喝合何和河核荷盒贺褐赫鹤黑嘿痕很狠恨哼恒横衡轰哄红宏洪虹鸿"
              +  "侯喉猴吼后厚候乎呼忽狐胡壶湖葫糊蝴虎互户护花华哗滑化划画话桦怀"
              +  "淮坏欢还环缓幻唤换患荒慌皇黄煌晃灰恢挥辉徽回毁悔汇会绘惠慧昏婚"
              +  "浑魂混活火伙或货获祸惑霍击饥圾机肌鸡积基迹绩激及吉级即极急疾集"
              +  "辑籍几己挤脊计记纪忌技际剂季既济继寂寄加夹佳家嘉甲贾钾价驾架假"
              +  "嫁稼尖坚间肩艰兼监减剪检简碱见件建剑健舰渐践鉴键箭江姜将浆僵疆"
              +  "讲奖蒋匠降交郊娇浇骄胶焦礁角脚搅叫轿较教阶皆接揭街节劫杰洁结捷"
              +  "截竭姐解介戒届界借巾今斤金津筋仅紧锦尽劲近进晋浸禁京经茎惊晶睛"
              +  "精鲸井颈景警净径竞竟敬境静镜纠究九久酒旧救就舅居局菊橘举矩句巨"
              +  "拒具俱剧惧据距聚卷倦决绝觉掘嚼军君均菌俊峻卡开凯慨刊堪砍看康抗"
              +  "炕考烤靠科棵颗壳咳可渴克刻客课肯坑空孔恐控口扣枯哭苦库裤酷夸跨"
              +  "块快宽款狂况矿亏葵愧溃昆困扩括阔垃拉啦喇腊蜡辣来莱赖兰拦栏蓝篮"
              +  "览懒烂滥郎狼廊朗浪捞劳牢老乐勒雷蕾泪类累冷愣厘梨离莉犁璃黎礼李"
              +  "里哩理鲤力历厉立丽利励例隶粒俩连帘怜莲联廉脸练炼恋链良凉梁粮两"
              +  "亮辆量辽疗聊僚了料列劣烈猎裂邻林临淋磷灵玲凌铃陵羚零龄领岭令另"
              +  "溜刘流留硫瘤柳六龙笼隆垄拢楼漏露卢芦炉鲁陆录鹿碌路驴旅铝履律虑"
              +  "率绿氯滤卵乱掠略伦轮论罗萝逻螺裸洛络骆落妈麻马玛码蚂骂吗嘛埋买"
              +  "迈麦卖脉蛮满曼慢漫忙芒盲茫猫毛矛茅茂冒贸帽貌么没枚玫眉梅媒煤霉"
              +  "每美妹门闷们萌盟猛蒙孟梦弥迷谜米泌秘密蜜眠绵棉免勉面苗描秒妙庙"
              +  "灭民敏名明鸣命摸模膜摩磨蘑魔抹末沫陌莫漠墨默谋某母亩牡姆拇木目"
              +  "牧墓幕慕穆拿哪内那纳娜钠乃奶奈耐男南难囊恼脑闹呢嫩能尼泥你拟逆"
              +  "年念娘酿鸟尿捏您宁凝牛扭纽农浓弄奴努怒女暖挪诺哦欧偶爬帕怕拍排"
              +  "牌派攀盘判叛盼庞旁胖抛炮跑泡胚陪培赔佩配喷盆朋棚蓬鹏膨捧碰批披"
              +  "皮疲脾匹屁譬片偏篇骗漂飘瓢票拼贫频品平评凭苹屏瓶萍坡泼颇婆迫破"
              +  "剖扑铺葡蒲朴浦普谱七妻栖戚期欺漆齐其奇歧骑棋旗企岂启起气弃汽契"
              +  "砌器恰千迁牵铅谦签前钱潜浅遣欠枪腔强墙抢悄敲乔桥瞧巧切茄且窃亲"
              +  "侵秦琴禽勤青氢轻倾清情晴顷请庆穷丘秋蚯求球区曲驱屈躯趋取娶去趣"
              +  "圈全权泉拳犬劝券缺却雀确鹊裙群然燃染嚷壤让饶扰绕惹热人仁忍认任"
              +  "扔仍日绒荣容溶熔融柔肉如儒乳辱入软锐瑞润若弱撒洒萨塞赛三伞散桑"
              +  "嗓丧扫嫂色森僧杀沙纱刹砂傻啥晒山杉衫珊闪陕扇善伤商赏上尚梢烧稍"
              +  "少绍哨舌蛇舍设社射涉摄申伸身深神审婶肾甚渗慎升生声牲胜绳省圣盛"
              +  "剩尸失师诗施狮湿十什石时识实拾蚀食史使始驶士氏世市示式事侍势视"
              +  "试饰室是适逝释收手守首寿受兽售授瘦书抒叔枢殊疏舒输蔬熟暑署属鼠"
              +  "薯术束述树竖数刷耍衰摔甩帅双霜爽谁水税睡顺瞬说丝司私思斯撕死四"
              +  "寺似饲松耸宋送颂搜艘苏俗诉肃素速宿塑酸蒜算虽随髓岁遂碎穗孙损笋"
              +  "缩所索锁他它她塌塔踏胎台抬太态泰贪摊滩坛谈潭坦叹炭探碳汤唐堂塘"
              +  "糖躺趟涛掏逃桃陶淘萄讨套特疼腾藤梯踢啼提题蹄体替天添田甜填挑条"
              +  "跳贴铁厅听廷亭庭停蜓挺艇通同桐铜童统桶筒痛偷头投透突图徒涂途屠"
              +  "土吐兔团推腿退吞托拖脱驼妥拓唾挖哇蛙娃瓦歪外弯湾丸完玩顽挽晚碗"
              +  "万汪亡王网往忘旺望危威微为围违唯惟维伟伪尾纬委萎卫未位味胃谓喂"
              +  "慰魏温文纹闻蚊吻稳问翁窝我沃卧握乌污屋无吴吾五午伍武舞务物误悟"
              +  "雾夕西吸希析息牺悉惜晰稀溪锡熙嘻膝习席袭媳洗喜戏系细隙虾瞎峡狭"
              +  "辖霞下吓夏厦仙先纤掀鲜闲弦贤咸衔嫌显险县现线限宪陷献腺乡相香厢"
              +  "湘箱详祥翔享响想向巷项象像橡削消萧硝销小晓孝效校笑些歇协胁斜谐"
              +  "携鞋写泄泻卸屑械谢蟹心辛欣新信兴星猩刑行形型醒杏姓幸性凶兄匈胸"
              +  "雄熊休修羞朽秀绣袖嗅须虚需徐许序叙畜绪续蓄宣玄悬旋选穴学雪血寻"
              +  "巡询循训讯迅压呀鸦鸭牙芽崖哑雅亚咽烟淹延严言岩沿炎研盐颜衍掩眼"
              +  "演厌宴艳验焰雁燕央扬羊阳杨洋仰养氧痒样腰邀摇遥咬药要耀爷也冶野"
              +  "业叶页夜液一伊衣医依仪夷宜姨移遗疑乙已以矣蚁椅义亿忆艺议亦异役"
              +  "抑译易疫益谊逸意溢毅翼因阴音吟银引饮蚓隐印应英婴鹰迎盈营蝇赢影"
              +  "映硬哟拥永泳勇涌用优忧幽悠尤犹由邮油游友有又右幼诱于予余鱼娱渔"
              +  "愉愚与宇羽雨语玉吁育郁狱浴预域欲喻寓御裕遇愈誉豫元员园原圆袁援"
              +  "缘源远怨院愿曰约月岳钥悦阅跃越云匀允孕运晕韵蕴杂砸灾栽宰载再在"
              +  "咱暂赞脏葬遭糟早枣藻灶皂造噪燥躁则择泽责贼怎曾增赠渣扎眨炸摘宅"
              +  "窄债沾粘展占战站张章涨掌丈仗帐胀账障招找召兆赵照罩遮折哲者这浙"
              +  "针侦珍真诊枕阵振镇震争征挣睁蒸整正证郑政症之支汁芝枝知织肢脂蜘"
              +  "执直值职植殖止只旨址纸指趾至志制治质致智置中忠终钟肿种仲众重州"
              +  "舟周洲轴宙皱骤朱株珠诸猪蛛竹烛逐主煮嘱住助注贮驻柱祝著筑抓爪专"
              +  "砖转赚庄桩装壮状撞追准捉桌着仔兹姿资滋籽子紫字自宗综棕踪总纵走"
              +  "奏租足族阻组祖钻嘴最罪醉尊遵昨左作坐座做蔼隘庵鞍黯肮拗袄懊扒芭"
              +  "疤捌跋靶掰扳拌绊梆绑榜蚌谤磅镑苞褒雹鲍狈悖惫笨绷泵蹦匕鄙庇毙痹"
              +  "弊璧贬匾辫彪憋鳖瘪彬斌缤濒鬓秉禀菠舶渤跛簸哺怖埠簿睬惭沧糙厕蹭"
              +  "茬岔豺掺搀禅馋蝉铲猖敞钞嘲澈忱辰铛澄逞秤痴弛侈耻宠畴稠锄雏橱矗"
              +  "揣囱疮炊捶椿淳蠢戳绰祠赐醋簇窜篡崔摧悴粹搓撮挫瘩歹怠贷耽档叨捣"
              +  "祷悼蹬嘀涤缔蒂掂滇巅碘佃甸玷惦奠刁叼迭谍碟鼎董栋兜蚪逗痘睹妒镀"
              +  "缎兑墩盹囤钝咄哆踱垛堕舵惰跺讹娥峨蛾扼鄂愕遏噩饵贰筏矾妃匪诽吠"
              +  "吩氛焚忿讽敷芙拂俘袱甫斧俯脯咐缚尬丐柑竿尴秆橄赣冈肛杠羔膏糕镐"
              +  "疙搁蛤庚羹埂耿梗蚣躬汞苟垢沽辜雇寡卦褂乖棺逛闺瑰诡癸跪亥骇酣憨"
              +  "涵悍捍焊憾撼翰夯嚎皓禾烘弘弧唬沪猾徊槐宦涣焕痪凰惶蝗簧恍谎幌卉"
              +  "讳诲贿晦秽荤豁讥叽唧缉畸箕稽棘嫉妓祭鲫冀颊奸歼煎拣俭柬茧捡荐贱"
              +  "涧溅槛缰桨酱椒跤蕉侥狡绞饺矫剿缴窖酵秸睫芥诫藉襟谨荆兢靖窘揪灸"
              +  "玖韭臼疚拘驹鞠桔沮炬锯娟捐鹃绢眷诀倔崛爵钧骏竣咖揩楷勘坎慷糠扛"
              +  "亢拷铐坷苛磕蝌垦恳啃吭抠叩寇窟垮挎筷筐旷框眶盔窥魁馈坤捆廓睐婪"
              +  "澜揽缆榄琅榔唠姥涝烙酪垒磊肋擂棱狸漓篱吏沥俐荔栗砾痢雳镰敛粱谅"
              +  "晾寥嘹撩缭瞭咧琳鳞凛吝赁躏拎伶聆菱浏琉馏榴咙胧聋窿娄搂篓陋庐颅"
              +  "卤虏赂禄吕侣屡缕峦抡仑沦啰锣箩骡蟆馒瞒蔓莽锚卯昧媚魅氓朦檬锰咪"
              +  "靡眯觅缅瞄渺藐蔑皿闽悯冥铭谬馍摹茉寞沐募睦暮捺挠瑙呐馁妮匿溺腻"
              +  "捻撵碾聂孽拧狞柠泞钮脓疟虐懦糯殴鸥呕藕趴啪耙徘湃潘畔乓螃刨袍沛"
              +  "砰烹彭澎篷坯劈霹啤僻翩撇聘乒坪魄仆菩圃瀑曝柒凄祈脐崎鳍乞迄泣掐"
              +  "洽钳乾黔谴嵌歉呛跷锹侨憔俏峭窍翘撬怯钦芹擒寝沁卿蜻擎琼囚岖渠痊"
              +  "瘸冉瓤壬刃纫韧戎茸蓉榕冗揉蹂蠕汝褥蕊闰腮叁搔骚臊涩瑟鲨煞霎筛删"
              +  "煽擅赡裳晌捎勺奢赦呻绅沈笙甥矢屎恃拭柿嗜誓梳淑赎蜀曙恕庶墅漱蟀"
              +  "拴栓涮吮烁硕嗽嘶巳伺祀肆讼诵酥粟溯隋祟隧唆梭嗦琐蹋苔汰瘫痰谭檀"
              +  "毯棠膛倘淌烫滔誊剔屉剃涕惕恬舔迢帖彤瞳捅凸秃颓蜕褪屯豚臀驮鸵椭"
              +  "洼袜豌宛婉惋皖腕枉妄偎薇巍帷苇畏尉猬蔚瘟紊嗡涡蜗呜巫诬芜梧蜈侮"
              +  "捂鹉勿戊昔犀熄蟋徙匣侠暇馅羡镶宵潇箫霄嚣淆肖哮啸蝎邪挟懈芯锌薪"
              +  "馨衅腥汹锈戌墟旭恤酗婿絮轩喧癣炫绚渲靴薛勋熏旬驯汛逊殉丫押涯衙"
              +  "讶焉阎蜒檐砚唁谚堰殃秧鸯漾夭吆妖尧肴姚窑谣舀椰腋壹怡贻胰倚屹邑"
              +  "绎姻茵荫殷寅淫瘾莺樱鹦荧莹萤颖佣庸咏踊酉佑迂淤渝隅逾榆舆屿禹芋"
              +  "冤鸳渊猿苑粤耘陨酝哉赃凿蚤澡憎咋喳轧闸乍诈栅榨斋寨毡瞻斩盏崭辗"
              +  "栈绽彰樟杖昭沼肇辙蔗贞斟疹怔狰筝拯吱侄帜挚秩掷窒滞稚衷粥肘帚咒"
              +  "昼拄瞩蛀铸拽撰妆幢椎锥坠缀赘谆卓拙灼茁浊酌啄琢咨姊揍卒佐佘赊";
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
