package forge.adventure.world;

/*
 * 2014 OpenSimplex Noise in Java.
 * by Kurt Spencer
 *
 * Updated Dec 2019 and Feb 2020:
 * - New lattice-symmetric gradient sets
 * - Optional alternate lattice orientation evaluators
 *
 * This implementation has been updated to slightly improve its output, but it is recommented to first
 * try the newer OpenSimplex2S or OpenSimplex2F noise. These are located in the OpenSimplex2 repo:
 * https://github.com/KdotJPG/OpenSimplex2
 *
 * In the event that the output of this OpenSimplex continues to better fit your project's needs than
 * either OpenSimplex2 variant, an updated backport of DigitalShadow's optimization is available here:
 * https://github.com/KdotJPG/OpenSimplex2/blob/master/java/legacy/OpenSimplex.java
 *
 * This is mostly kept here for reference. In particular, the 4D code is very slow.
 */

public class OpenSimplexNoise {

    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;    // (1/Math.sqrt(2+1)-1)/2;
    private static final double SQUISH_CONSTANT_2D = 0.366025403784439;      // (Math.sqrt(2+1)-1)/2;
    private static final double STRETCH_CONSTANT_3D = -1.0 / 6;              // (1/Math.sqrt(3+1)-1)/3;
    private static final double SQUISH_CONSTANT_3D = 1.0 / 3;                // (Math.sqrt(3+1)-1)/3;
    private static final double STRETCH_CONSTANT_4D = -0.138196601125011;    // (1/Math.sqrt(4+1)-1)/4;
    private static final double SQUISH_CONSTANT_4D = 0.309016994374947;      // (Math.sqrt(4+1)-1)/4;

    private static final long DEFAULT_SEED = 0;

    private static final int PSIZE = 2048;
    private static final int PMASK = 2047;
    private static final double N2 = 7.69084574549313;
    private static final double N3 = 26.92263139946168;
    private static final double N4 = 8.881759591352166;
    private static final Grad2[] GRADIENTS_2D = new Grad2[PSIZE];
    private static final Grad3[] GRADIENTS_3D = new Grad3[PSIZE];
    private static final Grad4[] GRADIENTS_4D = new Grad4[PSIZE];

    static {
        Grad2[] grad2 = {
                new Grad2(0.130526192220052, 0.99144486137381),
                new Grad2(0.38268343236509, 0.923879532511287),
                new Grad2(0.608761429008721, 0.793353340291235),
                new Grad2(0.793353340291235, 0.608761429008721),
                new Grad2(0.923879532511287, 0.38268343236509),
                new Grad2(0.99144486137381, 0.130526192220051),
                new Grad2(0.99144486137381, -0.130526192220051),
                new Grad2(0.923879532511287, -0.38268343236509),
                new Grad2(0.793353340291235, -0.60876142900872),
                new Grad2(0.608761429008721, -0.793353340291235),
                new Grad2(0.38268343236509, -0.923879532511287),
                new Grad2(0.130526192220052, -0.99144486137381),
                new Grad2(-0.130526192220052, -0.99144486137381),
                new Grad2(-0.38268343236509, -0.923879532511287),
                new Grad2(-0.608761429008721, -0.793353340291235),
                new Grad2(-0.793353340291235, -0.608761429008721),
                new Grad2(-0.923879532511287, -0.38268343236509),
                new Grad2(-0.99144486137381, -0.130526192220052),
                new Grad2(-0.99144486137381, 0.130526192220051),
                new Grad2(-0.923879532511287, 0.38268343236509),
                new Grad2(-0.793353340291235, 0.608761429008721),
                new Grad2(-0.608761429008721, 0.793353340291235),
                new Grad2(-0.38268343236509, 0.923879532511287),
                new Grad2(-0.130526192220052, 0.99144486137381)
        };
        for (int i = 0; i < grad2.length; i++) {
            grad2[i].dx /= N2;
            grad2[i].dy /= N2;
        }
        for (int i = 0; i < PSIZE; i++) {
            GRADIENTS_2D[i] = grad2[i % grad2.length];
        }

        Grad3[] grad3 = {
                new Grad3(-1.4082482904633333, -1.4082482904633333, -2.6329931618533333),
                new Grad3(-0.07491495712999985, -0.07491495712999985, -3.29965982852),
                new Grad3(0.24732126143473554, -1.6667938651159684, -2.838945207362466),
                new Grad3(-1.6667938651159684, 0.24732126143473554, -2.838945207362466),
                new Grad3(-1.4082482904633333, -2.6329931618533333, -1.4082482904633333),
                new Grad3(-0.07491495712999985, -3.29965982852, -0.07491495712999985),
                new Grad3(-1.6667938651159684, -2.838945207362466, 0.24732126143473554),
                new Grad3(0.24732126143473554, -2.838945207362466, -1.6667938651159684),
                new Grad3(1.5580782047233335, 0.33333333333333337, -2.8914115380566665),
                new Grad3(2.8914115380566665, -0.33333333333333337, -1.5580782047233335),
                new Grad3(1.8101897177633992, -1.2760767510338025, -2.4482280932803),
                new Grad3(2.4482280932803, 1.2760767510338025, -1.8101897177633992),
                new Grad3(1.5580782047233335, -2.8914115380566665, 0.33333333333333337),
                new Grad3(2.8914115380566665, -1.5580782047233335, -0.33333333333333337),
                new Grad3(2.4482280932803, -1.8101897177633992, 1.2760767510338025),
                new Grad3(1.8101897177633992, -2.4482280932803, -1.2760767510338025),
                new Grad3(-2.6329931618533333, -1.4082482904633333, -1.4082482904633333),
                new Grad3(-3.29965982852, -0.07491495712999985, -0.07491495712999985),
                new Grad3(-2.838945207362466, 0.24732126143473554, -1.6667938651159684),
                new Grad3(-2.838945207362466, -1.6667938651159684, 0.24732126143473554),
                new Grad3(0.33333333333333337, 1.5580782047233335, -2.8914115380566665),
                new Grad3(-0.33333333333333337, 2.8914115380566665, -1.5580782047233335),
                new Grad3(1.2760767510338025, 2.4482280932803, -1.8101897177633992),
                new Grad3(-1.2760767510338025, 1.8101897177633992, -2.4482280932803),
                new Grad3(0.33333333333333337, -2.8914115380566665, 1.5580782047233335),
                new Grad3(-0.33333333333333337, -1.5580782047233335, 2.8914115380566665),
                new Grad3(-1.2760767510338025, -2.4482280932803, 1.8101897177633992),
                new Grad3(1.2760767510338025, -1.8101897177633992, 2.4482280932803),
                new Grad3(3.29965982852, 0.07491495712999985, 0.07491495712999985),
                new Grad3(2.6329931618533333, 1.4082482904633333, 1.4082482904633333),
                new Grad3(2.838945207362466, -0.24732126143473554, 1.6667938651159684),
                new Grad3(2.838945207362466, 1.6667938651159684, -0.24732126143473554),
                new Grad3(-2.8914115380566665, 1.5580782047233335, 0.33333333333333337),
                new Grad3(-1.5580782047233335, 2.8914115380566665, -0.33333333333333337),
                new Grad3(-2.4482280932803, 1.8101897177633992, -1.2760767510338025),
                new Grad3(-1.8101897177633992, 2.4482280932803, 1.2760767510338025),
                new Grad3(-2.8914115380566665, 0.33333333333333337, 1.5580782047233335),
                new Grad3(-1.5580782047233335, -0.33333333333333337, 2.8914115380566665),
                new Grad3(-1.8101897177633992, 1.2760767510338025, 2.4482280932803),
                new Grad3(-2.4482280932803, -1.2760767510338025, 1.8101897177633992),
                new Grad3(0.07491495712999985, 3.29965982852, 0.07491495712999985),
                new Grad3(1.4082482904633333, 2.6329931618533333, 1.4082482904633333),
                new Grad3(1.6667938651159684, 2.838945207362466, -0.24732126143473554),
                new Grad3(-0.24732126143473554, 2.838945207362466, 1.6667938651159684),
                new Grad3(0.07491495712999985, 0.07491495712999985, 3.29965982852),
                new Grad3(1.4082482904633333, 1.4082482904633333, 2.6329931618533333),
                new Grad3(-0.24732126143473554, 1.6667938651159684, 2.838945207362466),
                new Grad3(1.6667938651159684, -0.24732126143473554, 2.838945207362466)
        };
        for (int i = 0; i < grad3.length; i++) {
            grad3[i].dx /= N3;
            grad3[i].dy /= N3;
            grad3[i].dz /= N3;
        }
        for (int i = 0; i < PSIZE; i++) {
            GRADIENTS_3D[i] = grad3[i % grad3.length];
        }

        Grad4[] grad4 = {
                new Grad4(-0.753341017856078, -0.37968289875261624, -0.37968289875261624, -0.37968289875261624),
                new Grad4(-0.7821684431180708, -0.4321472685365301, -0.4321472685365301, 0.12128480194602098),
                new Grad4(-0.7821684431180708, -0.4321472685365301, 0.12128480194602098, -0.4321472685365301),
                new Grad4(-0.7821684431180708, 0.12128480194602098, -0.4321472685365301, -0.4321472685365301),
                new Grad4(-0.8586508742123365, -0.508629699630796, 0.044802370851755174, 0.044802370851755174),
                new Grad4(-0.8586508742123365, 0.044802370851755174, -0.508629699630796, 0.044802370851755174),
                new Grad4(-0.8586508742123365, 0.044802370851755174, 0.044802370851755174, -0.508629699630796),
                new Grad4(-0.9982828964265062, -0.03381941603233842, -0.03381941603233842, -0.03381941603233842),
                new Grad4(-0.37968289875261624, -0.753341017856078, -0.37968289875261624, -0.37968289875261624),
                new Grad4(-0.4321472685365301, -0.7821684431180708, -0.4321472685365301, 0.12128480194602098),
                new Grad4(-0.4321472685365301, -0.7821684431180708, 0.12128480194602098, -0.4321472685365301),
                new Grad4(0.12128480194602098, -0.7821684431180708, -0.4321472685365301, -0.4321472685365301),
                new Grad4(-0.508629699630796, -0.8586508742123365, 0.044802370851755174, 0.044802370851755174),
                new Grad4(0.044802370851755174, -0.8586508742123365, -0.508629699630796, 0.044802370851755174),
                new Grad4(0.044802370851755174, -0.8586508742123365, 0.044802370851755174, -0.508629699630796),
                new Grad4(-0.03381941603233842, -0.9982828964265062, -0.03381941603233842, -0.03381941603233842),
                new Grad4(-0.37968289875261624, -0.37968289875261624, -0.753341017856078, -0.37968289875261624),
                new Grad4(-0.4321472685365301, -0.4321472685365301, -0.7821684431180708, 0.12128480194602098),
                new Grad4(-0.4321472685365301, 0.12128480194602098, -0.7821684431180708, -0.4321472685365301),
                new Grad4(0.12128480194602098, -0.4321472685365301, -0.7821684431180708, -0.4321472685365301),
                new Grad4(-0.508629699630796, 0.044802370851755174, -0.8586508742123365, 0.044802370851755174),
                new Grad4(0.044802370851755174, -0.508629699630796, -0.8586508742123365, 0.044802370851755174),
                new Grad4(0.044802370851755174, 0.044802370851755174, -0.8586508742123365, -0.508629699630796),
                new Grad4(-0.03381941603233842, -0.03381941603233842, -0.9982828964265062, -0.03381941603233842),
                new Grad4(-0.37968289875261624, -0.37968289875261624, -0.37968289875261624, -0.753341017856078),
                new Grad4(-0.4321472685365301, -0.4321472685365301, 0.12128480194602098, -0.7821684431180708),
                new Grad4(-0.4321472685365301, 0.12128480194602098, -0.4321472685365301, -0.7821684431180708),
                new Grad4(0.12128480194602098, -0.4321472685365301, -0.4321472685365301, -0.7821684431180708),
                new Grad4(-0.508629699630796, 0.044802370851755174, 0.044802370851755174, -0.8586508742123365),
                new Grad4(0.044802370851755174, -0.508629699630796, 0.044802370851755174, -0.8586508742123365),
                new Grad4(0.044802370851755174, 0.044802370851755174, -0.508629699630796, -0.8586508742123365),
                new Grad4(-0.03381941603233842, -0.03381941603233842, -0.03381941603233842, -0.9982828964265062),
                new Grad4(-0.6740059517812944, -0.3239847771997537, -0.3239847771997537, 0.5794684678643381),
                new Grad4(-0.7504883828755602, -0.4004672082940195, 0.15296486218853164, 0.5029860367700724),
                new Grad4(-0.7504883828755602, 0.15296486218853164, -0.4004672082940195, 0.5029860367700724),
                new Grad4(-0.8828161875373585, 0.08164729285680945, 0.08164729285680945, 0.4553054119602712),
                new Grad4(-0.4553054119602712, -0.08164729285680945, -0.08164729285680945, 0.8828161875373585),
                new Grad4(-0.5029860367700724, -0.15296486218853164, 0.4004672082940195, 0.7504883828755602),
                new Grad4(-0.5029860367700724, 0.4004672082940195, -0.15296486218853164, 0.7504883828755602),
                new Grad4(-0.5794684678643381, 0.3239847771997537, 0.3239847771997537, 0.6740059517812944),
                new Grad4(-0.3239847771997537, -0.6740059517812944, -0.3239847771997537, 0.5794684678643381),
                new Grad4(-0.4004672082940195, -0.7504883828755602, 0.15296486218853164, 0.5029860367700724),
                new Grad4(0.15296486218853164, -0.7504883828755602, -0.4004672082940195, 0.5029860367700724),
                new Grad4(0.08164729285680945, -0.8828161875373585, 0.08164729285680945, 0.4553054119602712),
                new Grad4(-0.08164729285680945, -0.4553054119602712, -0.08164729285680945, 0.8828161875373585),
                new Grad4(-0.15296486218853164, -0.5029860367700724, 0.4004672082940195, 0.7504883828755602),
                new Grad4(0.4004672082940195, -0.5029860367700724, -0.15296486218853164, 0.7504883828755602),
                new Grad4(0.3239847771997537, -0.5794684678643381, 0.3239847771997537, 0.6740059517812944),
                new Grad4(-0.3239847771997537, -0.3239847771997537, -0.6740059517812944, 0.5794684678643381),
                new Grad4(-0.4004672082940195, 0.15296486218853164, -0.7504883828755602, 0.5029860367700724),
                new Grad4(0.15296486218853164, -0.4004672082940195, -0.7504883828755602, 0.5029860367700724),
                new Grad4(0.08164729285680945, 0.08164729285680945, -0.8828161875373585, 0.4553054119602712),
                new Grad4(-0.08164729285680945, -0.08164729285680945, -0.4553054119602712, 0.8828161875373585),
                new Grad4(-0.15296486218853164, 0.4004672082940195, -0.5029860367700724, 0.7504883828755602),
                new Grad4(0.4004672082940195, -0.15296486218853164, -0.5029860367700724, 0.7504883828755602),
                new Grad4(0.3239847771997537, 0.3239847771997537, -0.5794684678643381, 0.6740059517812944),
                new Grad4(-0.6740059517812944, -0.3239847771997537, 0.5794684678643381, -0.3239847771997537),
                new Grad4(-0.7504883828755602, -0.4004672082940195, 0.5029860367700724, 0.15296486218853164),
                new Grad4(-0.7504883828755602, 0.15296486218853164, 0.5029860367700724, -0.4004672082940195),
                new Grad4(-0.8828161875373585, 0.08164729285680945, 0.4553054119602712, 0.08164729285680945),
                new Grad4(-0.4553054119602712, -0.08164729285680945, 0.8828161875373585, -0.08164729285680945),
                new Grad4(-0.5029860367700724, -0.15296486218853164, 0.7504883828755602, 0.4004672082940195),
                new Grad4(-0.5029860367700724, 0.4004672082940195, 0.7504883828755602, -0.15296486218853164),
                new Grad4(-0.5794684678643381, 0.3239847771997537, 0.6740059517812944, 0.3239847771997537),
                new Grad4(-0.3239847771997537, -0.6740059517812944, 0.5794684678643381, -0.3239847771997537),
                new Grad4(-0.4004672082940195, -0.7504883828755602, 0.5029860367700724, 0.15296486218853164),
                new Grad4(0.15296486218853164, -0.7504883828755602, 0.5029860367700724, -0.4004672082940195),
                new Grad4(0.08164729285680945, -0.8828161875373585, 0.4553054119602712, 0.08164729285680945),
                new Grad4(-0.08164729285680945, -0.4553054119602712, 0.8828161875373585, -0.08164729285680945),
                new Grad4(-0.15296486218853164, -0.5029860367700724, 0.7504883828755602, 0.4004672082940195),
                new Grad4(0.4004672082940195, -0.5029860367700724, 0.7504883828755602, -0.15296486218853164),
                new Grad4(0.3239847771997537, -0.5794684678643381, 0.6740059517812944, 0.3239847771997537),
                new Grad4(-0.3239847771997537, -0.3239847771997537, 0.5794684678643381, -0.6740059517812944),
                new Grad4(-0.4004672082940195, 0.15296486218853164, 0.5029860367700724, -0.7504883828755602),
                new Grad4(0.15296486218853164, -0.4004672082940195, 0.5029860367700724, -0.7504883828755602),
                new Grad4(0.08164729285680945, 0.08164729285680945, 0.4553054119602712, -0.8828161875373585),
                new Grad4(-0.08164729285680945, -0.08164729285680945, 0.8828161875373585, -0.4553054119602712),
                new Grad4(-0.15296486218853164, 0.4004672082940195, 0.7504883828755602, -0.5029860367700724),
                new Grad4(0.4004672082940195, -0.15296486218853164, 0.7504883828755602, -0.5029860367700724),
                new Grad4(0.3239847771997537, 0.3239847771997537, 0.6740059517812944, -0.5794684678643381),
                new Grad4(-0.6740059517812944, 0.5794684678643381, -0.3239847771997537, -0.3239847771997537),
                new Grad4(-0.7504883828755602, 0.5029860367700724, -0.4004672082940195, 0.15296486218853164),
                new Grad4(-0.7504883828755602, 0.5029860367700724, 0.15296486218853164, -0.4004672082940195),
                new Grad4(-0.8828161875373585, 0.4553054119602712, 0.08164729285680945, 0.08164729285680945),
                new Grad4(-0.4553054119602712, 0.8828161875373585, -0.08164729285680945, -0.08164729285680945),
                new Grad4(-0.5029860367700724, 0.7504883828755602, -0.15296486218853164, 0.4004672082940195),
                new Grad4(-0.5029860367700724, 0.7504883828755602, 0.4004672082940195, -0.15296486218853164),
                new Grad4(-0.5794684678643381, 0.6740059517812944, 0.3239847771997537, 0.3239847771997537),
                new Grad4(-0.3239847771997537, 0.5794684678643381, -0.6740059517812944, -0.3239847771997537),
                new Grad4(-0.4004672082940195, 0.5029860367700724, -0.7504883828755602, 0.15296486218853164),
                new Grad4(0.15296486218853164, 0.5029860367700724, -0.7504883828755602, -0.4004672082940195),
                new Grad4(0.08164729285680945, 0.4553054119602712, -0.8828161875373585, 0.08164729285680945),
                new Grad4(-0.08164729285680945, 0.8828161875373585, -0.4553054119602712, -0.08164729285680945),
                new Grad4(-0.15296486218853164, 0.7504883828755602, -0.5029860367700724, 0.4004672082940195),
                new Grad4(0.4004672082940195, 0.7504883828755602, -0.5029860367700724, -0.15296486218853164),
                new Grad4(0.3239847771997537, 0.6740059517812944, -0.5794684678643381, 0.3239847771997537),
                new Grad4(-0.3239847771997537, 0.5794684678643381, -0.3239847771997537, -0.6740059517812944),
                new Grad4(-0.4004672082940195, 0.5029860367700724, 0.15296486218853164, -0.7504883828755602),
                new Grad4(0.15296486218853164, 0.5029860367700724, -0.4004672082940195, -0.7504883828755602),
                new Grad4(0.08164729285680945, 0.4553054119602712, 0.08164729285680945, -0.8828161875373585),
                new Grad4(-0.08164729285680945, 0.8828161875373585, -0.08164729285680945, -0.4553054119602712),
                new Grad4(-0.15296486218853164, 0.7504883828755602, 0.4004672082940195, -0.5029860367700724),
                new Grad4(0.4004672082940195, 0.7504883828755602, -0.15296486218853164, -0.5029860367700724),
                new Grad4(0.3239847771997537, 0.6740059517812944, 0.3239847771997537, -0.5794684678643381),
                new Grad4(0.5794684678643381, -0.6740059517812944, -0.3239847771997537, -0.3239847771997537),
                new Grad4(0.5029860367700724, -0.7504883828755602, -0.4004672082940195, 0.15296486218853164),
                new Grad4(0.5029860367700724, -0.7504883828755602, 0.15296486218853164, -0.4004672082940195),
                new Grad4(0.4553054119602712, -0.8828161875373585, 0.08164729285680945, 0.08164729285680945),
                new Grad4(0.8828161875373585, -0.4553054119602712, -0.08164729285680945, -0.08164729285680945),
                new Grad4(0.7504883828755602, -0.5029860367700724, -0.15296486218853164, 0.4004672082940195),
                new Grad4(0.7504883828755602, -0.5029860367700724, 0.4004672082940195, -0.15296486218853164),
                new Grad4(0.6740059517812944, -0.5794684678643381, 0.3239847771997537, 0.3239847771997537),
                new Grad4(0.5794684678643381, -0.3239847771997537, -0.6740059517812944, -0.3239847771997537),
                new Grad4(0.5029860367700724, -0.4004672082940195, -0.7504883828755602, 0.15296486218853164),
                new Grad4(0.5029860367700724, 0.15296486218853164, -0.7504883828755602, -0.4004672082940195),
                new Grad4(0.4553054119602712, 0.08164729285680945, -0.8828161875373585, 0.08164729285680945),
                new Grad4(0.8828161875373585, -0.08164729285680945, -0.4553054119602712, -0.08164729285680945),
                new Grad4(0.7504883828755602, -0.15296486218853164, -0.5029860367700724, 0.4004672082940195),
                new Grad4(0.7504883828755602, 0.4004672082940195, -0.5029860367700724, -0.15296486218853164),
                new Grad4(0.6740059517812944, 0.3239847771997537, -0.5794684678643381, 0.3239847771997537),
                new Grad4(0.5794684678643381, -0.3239847771997537, -0.3239847771997537, -0.6740059517812944),
                new Grad4(0.5029860367700724, -0.4004672082940195, 0.15296486218853164, -0.7504883828755602),
                new Grad4(0.5029860367700724, 0.15296486218853164, -0.4004672082940195, -0.7504883828755602),
                new Grad4(0.4553054119602712, 0.08164729285680945, 0.08164729285680945, -0.8828161875373585),
                new Grad4(0.8828161875373585, -0.08164729285680945, -0.08164729285680945, -0.4553054119602712),
                new Grad4(0.7504883828755602, -0.15296486218853164, 0.4004672082940195, -0.5029860367700724),
                new Grad4(0.7504883828755602, 0.4004672082940195, -0.15296486218853164, -0.5029860367700724),
                new Grad4(0.6740059517812944, 0.3239847771997537, 0.3239847771997537, -0.5794684678643381),
                new Grad4(0.03381941603233842, 0.03381941603233842, 0.03381941603233842, 0.9982828964265062),
                new Grad4(-0.044802370851755174, -0.044802370851755174, 0.508629699630796, 0.8586508742123365),
                new Grad4(-0.044802370851755174, 0.508629699630796, -0.044802370851755174, 0.8586508742123365),
                new Grad4(-0.12128480194602098, 0.4321472685365301, 0.4321472685365301, 0.7821684431180708),
                new Grad4(0.508629699630796, -0.044802370851755174, -0.044802370851755174, 0.8586508742123365),
                new Grad4(0.4321472685365301, -0.12128480194602098, 0.4321472685365301, 0.7821684431180708),
                new Grad4(0.4321472685365301, 0.4321472685365301, -0.12128480194602098, 0.7821684431180708),
                new Grad4(0.37968289875261624, 0.37968289875261624, 0.37968289875261624, 0.753341017856078),
                new Grad4(0.03381941603233842, 0.03381941603233842, 0.9982828964265062, 0.03381941603233842),
                new Grad4(-0.044802370851755174, 0.044802370851755174, 0.8586508742123365, 0.508629699630796),
                new Grad4(-0.044802370851755174, 0.508629699630796, 0.8586508742123365, -0.044802370851755174),
                new Grad4(-0.12128480194602098, 0.4321472685365301, 0.7821684431180708, 0.4321472685365301),
                new Grad4(0.508629699630796, -0.044802370851755174, 0.8586508742123365, -0.044802370851755174),
                new Grad4(0.4321472685365301, -0.12128480194602098, 0.7821684431180708, 0.4321472685365301),
                new Grad4(0.4321472685365301, 0.4321472685365301, 0.7821684431180708, -0.12128480194602098),
                new Grad4(0.37968289875261624, 0.37968289875261624, 0.753341017856078, 0.37968289875261624),
                new Grad4(0.03381941603233842, 0.9982828964265062, 0.03381941603233842, 0.03381941603233842),
                new Grad4(-0.044802370851755174, 0.8586508742123365, -0.044802370851755174, 0.508629699630796),
                new Grad4(-0.044802370851755174, 0.8586508742123365, 0.508629699630796, -0.044802370851755174),
                new Grad4(-0.12128480194602098, 0.7821684431180708, 0.4321472685365301, 0.4321472685365301),
                new Grad4(0.508629699630796, 0.8586508742123365, -0.044802370851755174, -0.044802370851755174),
                new Grad4(0.4321472685365301, 0.7821684431180708, -0.12128480194602098, 0.4321472685365301),
                new Grad4(0.4321472685365301, 0.7821684431180708, 0.4321472685365301, -0.12128480194602098),
                new Grad4(0.37968289875261624, 0.753341017856078, 0.37968289875261624, 0.37968289875261624),
                new Grad4(0.9982828964265062, 0.03381941603233842, 0.03381941603233842, 0.03381941603233842),
                new Grad4(0.8586508742123365, -0.044802370851755174, -0.044802370851755174, 0.508629699630796),
                new Grad4(0.8586508742123365, -0.044802370851755174, 0.508629699630796, -0.044802370851755174),
                new Grad4(0.7821684431180708, -0.12128480194602098, 0.4321472685365301, 0.4321472685365301),
                new Grad4(0.8586508742123365, 0.508629699630796, -0.044802370851755174, -0.044802370851755174),
                new Grad4(0.7821684431180708, 0.4321472685365301, -0.12128480194602098, 0.4321472685365301),
                new Grad4(0.7821684431180708, 0.4321472685365301, 0.4321472685365301, -0.12128480194602098),
                new Grad4(0.753341017856078, 0.37968289875261624, 0.37968289875261624, 0.37968289875261624)
        };
        for (int i = 0; i < grad4.length; i++) {
            grad4[i].dx /= N4;
            grad4[i].dy /= N4;
            grad4[i].dz /= N4;
            grad4[i].dw /= N4;
        }
        for (int i = 0; i < PSIZE; i++) {
            GRADIENTS_4D[i] = grad4[i % grad4.length];
        }
    }

    private final short[] perm;
    private final Grad2[] permGrad2;
    private final Grad3[] permGrad3;
    private final Grad4[] permGrad4;

    public OpenSimplexNoise() {
        this(DEFAULT_SEED);
    }

    public OpenSimplexNoise(short[] perm) {
        this.perm = perm;
        permGrad2 = new Grad2[PSIZE];
        permGrad3 = new Grad3[PSIZE];
        permGrad4 = new Grad4[PSIZE];

        for (int i = 0; i < PSIZE; i++) {
            permGrad2[i] = GRADIENTS_2D[perm[i]];
            permGrad3[i] = GRADIENTS_3D[perm[i]];
            permGrad4[i] = GRADIENTS_4D[perm[i]];
        }
    }

    public OpenSimplexNoise(long seed) {
        perm = new short[PSIZE];
        permGrad2 = new Grad2[PSIZE];
        permGrad3 = new Grad3[PSIZE];
        permGrad4 = new Grad4[PSIZE];
        short[] source = new short[PSIZE];
        for (short i = 0; i < PSIZE; i++)
            source[i] = i;
        for (int i = PSIZE - 1; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((seed + 31) % (i + 1));
            if (r < 0)
                r += (i + 1);
            perm[i] = source[r];
            permGrad2[i] = GRADIENTS_2D[perm[i]];
            permGrad3[i] = GRADIENTS_3D[perm[i]];
            permGrad4[i] = GRADIENTS_4D[perm[i]];
            source[r] = source[i];
        }
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    // 2D OpenSimplex Noise.
    public double eval(double x, double y) {

        // Place input coordinates onto grid.
        double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;

        // Floor to get grid coordinates of rhombus (stretched square) super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);

        // Compute grid coordinates relative to rhombus origin.
        double xins = xs - xsb;
        double yins = ys - ysb;

        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins;

        // Positions relative to origin point.
        double squishOffsetIns = inSum * SQUISH_CONSTANT_2D;
        double dx0 = xins + squishOffsetIns;
        double dy0 = yins + squishOffsetIns;

        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext, dy_ext;
        int xsv_ext, ysv_ext;

        double value = 0;

        // Contribution (1,0)
        double dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
        double dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, dx1, dy1);
        }

        // Contribution (0,1)
        double dx2 = dx0 - 0 - SQUISH_CONSTANT_2D;
        double dy2 = dy0 - 1 - SQUISH_CONSTANT_2D;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, dx2, dy2);
        }

        if (inSum <= 1) { // We're inside the triangle (2-Simplex) at (0,0)
            double zins = 1 - inSum;
            if (zins > xins || zins > yins) { // (0,0) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 1;
                    ysv_ext = ysb - 1;
                    dx_ext = dx0 - 1;
                    dy_ext = dy0 + 1;
                } else {
                    xsv_ext = xsb - 1;
                    ysv_ext = ysb + 1;
                    dx_ext = dx0 + 1;
                    dy_ext = dy0 - 1;
                }
            } else { // (1,0) and (0,1) are the closest two vertices.
                xsv_ext = xsb + 1;
                ysv_ext = ysb + 1;
                dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
                dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
            }
        } else { // We're inside the triangle (2-Simplex) at (1,1)
            double zins = 2 - inSum;
            if (zins < xins || zins < yins) { // (0,0) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 2;
                    ysv_ext = ysb + 0;
                    dx_ext = dx0 - 2 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 + 0 - 2 * SQUISH_CONSTANT_2D;
                } else {
                    xsv_ext = xsb + 0;
                    ysv_ext = ysb + 2;
                    dx_ext = dx0 + 0 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 - 2 - 2 * SQUISH_CONSTANT_2D;
                }
            } else { // (1,0) and (0,1) are the closest two vertices.
                dx_ext = dx0;
                dy_ext = dy0;
                xsv_ext = xsb;
                ysv_ext = ysb;
            }
            xsb += 1;
            ysb += 1;
            dx0 = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
            dy0 = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
        }

        // Contribution (0,0) or (1,1)
        double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
        }

        // Extra Vertex
        double attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext;
        if (attn_ext > 0) {
            attn_ext *= attn_ext;
            value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
        }

        return value;
    }

    // 3D OpenSimplex Noise.
    public double eval(double x, double y, double z) {

        // Place input coordinates on simplectic honeycomb.
        double stretchOffset = (x + y + z) * STRETCH_CONSTANT_3D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;

        return eval3_Base(xs, ys, zs);
    }

    // Not as good as in SuperSimplex/OpenSimplex2S, since there are more visible differences between different slices.
    // The Z coordinate should always be the "different" coordinate in your use case.
    public double eval3_XYBeforeZ(double x, double y, double z) {
        // Combine rotation with skew transform.
        double xy = x + y;
        double s2 = xy * 0.211324865405187;
        double zz = z * 0.288675134594813;
        double xs = s2 - x + zz, ys = s2 - y + zz;
        double zs = xy * 0.577350269189626 + zz;

        return eval3_Base(xs, ys, zs);
    }

    // Similar to the above, except the Y coordinate should always be the "different" coordinate in your use case.
    public double eval3_XZBeforeY(double x, double y, double z) {
        // Combine rotation with skew transform.
        double xz = x + z;
        double s2 = xz * 0.211324865405187;
        double yy = y * 0.288675134594813;
        double xs = s2 - x + yy, zs = s2 - z + yy;
        double ys = xz * 0.577350269189626 + yy;

        return eval3_Base(xs, ys, zs);
    }

    // 3D OpenSimplex Noise (base which takes skewed coordinates directly).
    private double eval3_Base(double xs, double ys, double zs) {

        // Floor to get simplectic honeycomb coordinates of rhombohedron (stretched cube) super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);

        // Compute simplectic honeycomb coordinates relative to rhombohedral origin.
        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;

        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins + zins;

        // Positions relative to origin point.
        double squishOffsetIns = inSum * SQUISH_CONSTANT_3D;
        double dx0 = xins + squishOffsetIns;
        double dy0 = yins + squishOffsetIns;
        double dz0 = zins + squishOffsetIns;

        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext0, dy_ext0, dz_ext0;
        double dx_ext1, dy_ext1, dz_ext1;
        int xsv_ext0, ysv_ext0, zsv_ext0;
        int xsv_ext1, ysv_ext1, zsv_ext1;

        double value = 0;
        if (inSum <= 1) { // We're inside the tetrahedron (3-Simplex) at (0,0,0)

            // Determine which two of (0,0,1), (0,1,0), (1,0,0) are closest.
            byte aPoint = 0x01;
            double aScore = xins;
            byte bPoint = 0x02;
            double bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 0x04;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 0x04;
            }

            // Now we determine the two lattice points not part of the tetrahedron that may contribute.
            // This depends on the closest two tetrahedral vertices, including (0,0,0)
            double wins = 1 - inSum;
            if (wins > aScore || wins > bScore) { // (0,0,0) is one of the closest two tetrahedral vertices.
                byte c = (bScore > aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.

                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsb - 1;
                    xsv_ext1 = xsb;
                    dx_ext0 = dx0 + 1;
                    dx_ext1 = dx0;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb + 1;
                    dx_ext0 = dx_ext1 = dx0 - 1;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0;
                    if ((c & 0x01) == 0) {
                        ysv_ext1 -= 1;
                        dy_ext1 += 1;
                    } else {
                        ysv_ext0 -= 1;
                        dy_ext0 += 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsb;
                    zsv_ext1 = zsb - 1;
                    dz_ext0 = dz0;
                    dz_ext1 = dz0 + 1;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz0 - 1;
                }
            } else { // (0,0,0) is not one of the closest two tetrahedral vertices.
                byte c = (byte) (aPoint | bPoint); // Our two extra vertices are determined by the closest two.

                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsb;
                    xsv_ext1 = xsb - 1;
                    dx_ext0 = dx0 - 2 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb + 1;
                    dx_ext0 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysb;
                    ysv_ext1 = ysb - 1;
                    dy_ext0 = dy0 - 2 * SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsb;
                    zsv_ext1 = zsb - 1;
                    dz_ext0 = dz0 - 2 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                }
            }

            // Contribution (0,0,0)
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb + 0, ysb + 0, zsb + 0, dx0, dy0, dz0);
            }

            // Contribution (1,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_3D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_3D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_3D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1);
            }

            // Contribution (0,1,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_3D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_3D;
            double dz2 = dz1;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz2);
            }

            // Contribution (0,0,1)
            double dx3 = dx2;
            double dy3 = dy1;
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, dx3, dy3, dz3);
            }
        } else if (inSum >= 2) { // We're inside the tetrahedron (3-Simplex) at (1,1,1)

            // Determine which two tetrahedral vertices are the closest, out of (1,1,0), (1,0,1), (0,1,1) but not (1,1,1).
            byte aPoint = 0x06;
            double aScore = xins;
            byte bPoint = 0x05;
            double bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 0x03;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 0x03;
            }

            // Now we determine the two lattice points not part of the tetrahedron that may contribute.
            // This depends on the closest two tetrahedral vertices, including (1,1,1)
            double wins = 3 - inSum;
            if (wins < aScore || wins < bScore) { // (1,1,1) is one of the closest two tetrahedral vertices.
                byte c = (bScore < aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsb + 2;
                    xsv_ext1 = xsb + 1;
                    dx_ext0 = dx0 - 2 - 3 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb;
                    dx_ext0 = dx_ext1 = dx0 - 3 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    if ((c & 0x01) != 0) {
                        ysv_ext1 += 1;
                        dy_ext1 -= 1;
                    } else {
                        ysv_ext0 += 1;
                        dy_ext0 -= 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0 - 3 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsb + 1;
                    zsv_ext1 = zsb + 2;
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 - 3 * SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz_ext1 = dz0 - 3 * SQUISH_CONSTANT_3D;
                }
            } else { // (1,1,1) is not one of the closest two tetrahedral vertices.
                byte c = (byte) (aPoint & bPoint); // Our two extra vertices are determined by the closest two.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsb + 1;
                    xsv_ext1 = xsb + 2;
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb;
                    dx_ext0 = dx0 - SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysb + 1;
                    ysv_ext1 = ysb + 2;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy0 - SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsb + 1;
                    zsv_ext1 = zsb + 2;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz0 - SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                }
            }

            // Contribution (1,1,0)
            double dx3 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dy3 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dz3 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, zsb + 0, dx3, dy3, dz3);
            }

            // Contribution (1,0,1)
            double dx2 = dx3;
            double dy2 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dz2 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 1, ysb + 0, zsb + 1, dx2, dy2, dz2);
            }

            // Contribution (0,1,1)
            double dx1 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dy1 = dy3;
            double dz1 = dz2;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 0, ysb + 1, zsb + 1, dx1, dy1, dz1);
            }

            // Contribution (1,1,1)
            dx0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dy0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dz0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0);
            }
        } else { // We're inside the octahedron (Rectified 3-Simplex) in between.
            double aScore;
            byte aPoint;
            boolean aIsFurtherSide;
            double bScore;
            byte bPoint;
            boolean bIsFurtherSide;

            // Decide between point (0,0,1) and (1,1,0) as closest
            double p1 = xins + yins;
            if (p1 > 1) {
                aScore = p1 - 1;
                aPoint = 0x03;
                aIsFurtherSide = true;
            } else {
                aScore = 1 - p1;
                aPoint = 0x04;
                aIsFurtherSide = false;
            }

            // Decide between point (0,1,0) and (1,0,1) as closest
            double p2 = xins + zins;
            if (p2 > 1) {
                bScore = p2 - 1;
                bPoint = 0x05;
                bIsFurtherSide = true;
            } else {
                bScore = 1 - p2;
                bPoint = 0x02;
                bIsFurtherSide = false;
            }

            // The closest out of the two (1,0,0) and (0,1,1) will replace the furthest out of the two decided above, if closer.
            double p3 = yins + zins;
            if (p3 > 1) {
                double score = p3 - 1;
                if (aScore <= bScore && aScore < score) {
                    aScore = score;
                    aPoint = 0x06;
                    aIsFurtherSide = true;
                } else if (aScore > bScore && bScore < score) {
                    bScore = score;
                    bPoint = 0x06;
                    bIsFurtherSide = true;
                }
            } else {
                double score = 1 - p3;
                if (aScore <= bScore && aScore < score) {
                    aScore = score;
                    aPoint = 0x01;
                    aIsFurtherSide = false;
                } else if (aScore > bScore && bScore < score) {
                    bScore = score;
                    bPoint = 0x01;
                    bIsFurtherSide = false;
                }
            }

            // Where each of the two closest points are determines how the extra two vertices are calculated.
            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) { // Both closest points on (1,1,1) side

                    // One of the two extra points is (1,1,1)
                    dx_ext0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb + 1;

                    // Other extra point is based on the shared axis.
                    byte c = (byte) (aPoint & bPoint);
                    if ((c & 0x01) != 0) {
                        dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 2;
                        ysv_ext1 = ysb;
                        zsv_ext1 = zsb;
                    } else if ((c & 0x02) != 0) {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb;
                        ysv_ext1 = ysb + 2;
                        zsv_ext1 = zsb;
                    } else {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb;
                        ysv_ext1 = ysb;
                        zsv_ext1 = zsb + 2;
                    }
                } else {// Both closest points on (0,0,0) side

                    // One of the two extra points is (0,0,0)
                    dx_ext0 = dx0;
                    dy_ext0 = dy0;
                    dz_ext0 = dz0;
                    xsv_ext0 = xsb;
                    ysv_ext0 = ysb;
                    zsv_ext0 = zsb;

                    // Other extra point is based on the omitted axis.
                    byte c = (byte) (aPoint | bPoint);
                    if ((c & 0x01) == 0) {
                        dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb - 1;
                        ysv_ext1 = ysb + 1;
                        zsv_ext1 = zsb + 1;
                    } else if ((c & 0x02) == 0) {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 1;
                        ysv_ext1 = ysb - 1;
                        zsv_ext1 = zsb + 1;
                    } else {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 1;
                        ysv_ext1 = ysb + 1;
                        zsv_ext1 = zsb - 1;
                    }
                }
            } else { // One point on (0,0,0) side, one point on (1,1,1) side
                byte c1, c2;
                if (aIsFurtherSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }

                // One contribution is a permutation of (1,1,-1)
                if ((c1 & 0x01) == 0) {
                    dx_ext0 = dx0 + 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb - 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb + 1;
                } else if ((c1 & 0x02) == 0) {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 + 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb - 1;
                    zsv_ext0 = zsb + 1;
                } else {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 + 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb - 1;
                }

                // One contribution is a permutation of (0,0,2)
                dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                xsv_ext1 = xsb;
                ysv_ext1 = ysb;
                zsv_ext1 = zsb;
                if ((c2 & 0x01) != 0) {
                    dx_ext1 -= 2;
                    xsv_ext1 += 2;
                } else if ((c2 & 0x02) != 0) {
                    dy_ext1 -= 2;
                    ysv_ext1 += 2;
                } else {
                    dz_ext1 -= 2;
                    zsv_ext1 += 2;
                }
            }

            // Contribution (1,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_3D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_3D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_3D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1);
            }

            // Contribution (0,1,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_3D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_3D;
            double dz2 = dz1;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz2);
            }

            // Contribution (0,0,1)
            double dx3 = dx2;
            double dy3 = dy1;
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, dx3, dy3, dz3);
            }

            // Contribution (1,1,0)
            double dx4 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dy4 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dz4 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, zsb + 0, dx4, dy4, dz4);
            }

            // Contribution (1,0,1)
            double dx5 = dx4;
            double dy5 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dz5 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double attn5 = 2 - dx5 * dx5 - dy5 * dy5 - dz5 * dz5;
            if (attn5 > 0) {
                attn5 *= attn5;
                value += attn5 * attn5 * extrapolate(xsb + 1, ysb + 0, zsb + 1, dx5, dy5, dz5);
            }

            // Contribution (0,1,1)
            double dx6 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dy6 = dy4;
            double dz6 = dz5;
            double attn6 = 2 - dx6 * dx6 - dy6 * dy6 - dz6 * dz6;
            if (attn6 > 0) {
                attn6 *= attn6;
                value += attn6 * attn6 * extrapolate(xsb + 0, ysb + 1, zsb + 1, dx6, dy6, dz6);
            }
        }

        // First extra vertex
        double attn_ext0 = 2 - dx_ext0 * dx_ext0 - dy_ext0 * dy_ext0 - dz_ext0 * dz_ext0;
        if (attn_ext0 > 0) {
            attn_ext0 *= attn_ext0;
            value += attn_ext0 * attn_ext0 * extrapolate(xsv_ext0, ysv_ext0, zsv_ext0, dx_ext0, dy_ext0, dz_ext0);
        }

        // Second extra vertex
        double attn_ext1 = 2 - dx_ext1 * dx_ext1 - dy_ext1 * dy_ext1 - dz_ext1 * dz_ext1;
        if (attn_ext1 > 0) {
            attn_ext1 *= attn_ext1;
            value += attn_ext1 * attn_ext1 * extrapolate(xsv_ext1, ysv_ext1, zsv_ext1, dx_ext1, dy_ext1, dz_ext1);
        }

        return value;
    }

    public double eval(double x, double y, double z, double w) {

        // Get points for A4 lattice
        double s = -0.138196601125011 * (x + y + z + w);
        double xs = x + s, ys = y + s, zs = z + s, ws = w + s;

        return eval4_Base(xs, ys, zs, ws);
    }

    public double eval4_XYBeforeZW(double x, double y, double z, double w) {

        double s2 = (x + y) * -0.178275657951399372 + (z + w) * 0.215623393288842828;
        double t2 = (z + w) * -0.403949762580207112 + (x + y) * -0.375199083010075342;
        double xs = x + s2, ys = y + s2, zs = z + t2, ws = w + t2;

        return eval4_Base(xs, ys, zs, ws);
    }

    public double eval4_XZBeforeYW(double x, double y, double z, double w) {

        double s2 = (x + z) * -0.178275657951399372 + (y + w) * 0.215623393288842828;
        double t2 = (y + w) * -0.403949762580207112 + (x + z) * -0.375199083010075342;
        double xs = x + s2, ys = y + t2, zs = z + s2, ws = w + t2;

        return eval4_Base(xs, ys, zs, ws);
    }

    public double eval4_XYZBeforeW(double x, double y, double z, double w) {

        double xyz = x + y + z;
        double ww = w * 0.2236067977499788;
        double s2 = xyz * -0.16666666666666666 + ww;
        double xs = x + s2, ys = y + s2, zs = z + s2, ws = -0.5 * xyz + ww;

        return eval4_Base(xs, ys, zs, ws);
    }

    // 4D OpenSimplex Noise.
    private double eval4_Base(double xs, double ys, double zs, double ws) {

        // Floor to get simplectic honeycomb coordinates of rhombo-hypercube super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);
        int wsb = fastFloor(ws);

        // Compute simplectic honeycomb coordinates relative to rhombo-hypercube origin.
        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;
        double wins = ws - wsb;

        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins + zins + wins;

        // Positions relative to origin point.
        double squishOffsetIns = inSum * SQUISH_CONSTANT_4D;
        double dx0 = xins + squishOffsetIns;
        double dy0 = yins + squishOffsetIns;
        double dz0 = zins + squishOffsetIns;
        double dw0 = wins + squishOffsetIns;

        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext0, dy_ext0, dz_ext0, dw_ext0;
        double dx_ext1, dy_ext1, dz_ext1, dw_ext1;
        double dx_ext2, dy_ext2, dz_ext2, dw_ext2;
        int xsv_ext0, ysv_ext0, zsv_ext0, wsv_ext0;
        int xsv_ext1, ysv_ext1, zsv_ext1, wsv_ext1;
        int xsv_ext2, ysv_ext2, zsv_ext2, wsv_ext2;

        double value = 0;
        if (inSum <= 1) { // We're inside the pentachoron (4-Simplex) at (0,0,0,0)

            // Determine which two of (0,0,0,1), (0,0,1,0), (0,1,0,0), (1,0,0,0) are closest.
            byte aPoint = 0x01;
            double aScore = xins;
            byte bPoint = 0x02;
            double bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 0x04;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 0x04;
            }
            if (aScore >= bScore && wins > bScore) {
                bScore = wins;
                bPoint = 0x08;
            } else if (aScore < bScore && wins > aScore) {
                aScore = wins;
                aPoint = 0x08;
            }

            // Now we determine the three lattice points not part of the pentachoron that may contribute.
            // This depends on the closest two pentachoron vertices, including (0,0,0,0)
            double uins = 1 - inSum;
            if (uins > aScore || uins > bScore) { // (0,0,0,0) is one of the closest two pentachoron vertices.
                byte c = (bScore > aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.
                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsb - 1;
                    xsv_ext1 = xsv_ext2 = xsb;
                    dx_ext0 = dx0 + 1;
                    dx_ext1 = dx_ext2 = dx0;
                } else {
                    xsv_ext0 = xsv_ext1 = xsv_ext2 = xsb + 1;
                    dx_ext0 = dx_ext1 = dx_ext2 = dx0 - 1;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb;
                    dy_ext0 = dy_ext1 = dy_ext2 = dy0;
                    if ((c & 0x01) == 0x01) {
                        ysv_ext0 -= 1;
                        dy_ext0 += 1;
                    } else {
                        ysv_ext1 -= 1;
                        dy_ext1 += 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy_ext2 = dy0 - 1;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb;
                    dz_ext0 = dz_ext1 = dz_ext2 = dz0;
                    if ((c & 0x03) != 0) {
                        if ((c & 0x03) == 0x03) {
                            zsv_ext0 -= 1;
                            dz_ext0 += 1;
                        } else {
                            zsv_ext1 -= 1;
                            dz_ext1 += 1;
                        }
                    } else {
                        zsv_ext2 -= 1;
                        dz_ext2 += 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz_ext2 = dz0 - 1;
                }

                if ((c & 0x08) == 0) {
                    wsv_ext0 = wsv_ext1 = wsb;
                    wsv_ext2 = wsb - 1;
                    dw_ext0 = dw_ext1 = dw0;
                    dw_ext2 = dw0 + 1;
                } else {
                    wsv_ext0 = wsv_ext1 = wsv_ext2 = wsb + 1;
                    dw_ext0 = dw_ext1 = dw_ext2 = dw0 - 1;
                }
            } else { // (0,0,0,0) is not one of the closest two pentachoron vertices.
                byte c = (byte) (aPoint | bPoint); // Our three extra vertices are determined by the closest two.

                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsv_ext2 = xsb;
                    xsv_ext1 = xsb - 1;
                    dx_ext0 = dx0 - 2 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_4D;
                    dx_ext2 = dx0 - SQUISH_CONSTANT_4D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsv_ext2 = xsb + 1;
                    dx_ext0 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx_ext2 = dx0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb;
                    dy_ext0 = dy0 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext1 = dy_ext2 = dy0 - SQUISH_CONSTANT_4D;
                    if ((c & 0x01) == 0x01) {
                        ysv_ext1 -= 1;
                        dy_ext1 += 1;
                    } else {
                        ysv_ext2 -= 1;
                        dy_ext2 += 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb + 1;
                    dy_ext0 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext1 = dy_ext2 = dy0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb;
                    dz_ext0 = dz0 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext1 = dz_ext2 = dz0 - SQUISH_CONSTANT_4D;
                    if ((c & 0x03) == 0x03) {
                        zsv_ext1 -= 1;
                        dz_ext1 += 1;
                    } else {
                        zsv_ext2 -= 1;
                        dz_ext2 += 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb + 1;
                    dz_ext0 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext1 = dz_ext2 = dz0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c & 0x08) == 0) {
                    wsv_ext0 = wsv_ext1 = wsb;
                    wsv_ext2 = wsb - 1;
                    dw_ext0 = dw0 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw0 - SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 + 1 - SQUISH_CONSTANT_4D;
                } else {
                    wsv_ext0 = wsv_ext1 = wsv_ext2 = wsb + 1;
                    dw_ext0 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw_ext2 = dw0 - 1 - SQUISH_CONSTANT_4D;
                }
            }

            // Contribution (0,0,0,0)
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0 - dw0 * dw0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 0, dx0, dy0, dz0, dw0);
            }

            // Contribution (1,0,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_4D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_4D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_4D;
            double dw1 = dw0 - 0 - SQUISH_CONSTANT_4D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1, dy1, dz1, dw1);
            }

            // Contribution (0,1,0,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_4D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_4D;
            double dz2 = dz1;
            double dw2 = dw1;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2 - dw2 * dw2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2, dy2, dz2, dw2);
            }

            // Contribution (0,0,1,0)
            double dx3 = dx2;
            double dy3 = dy1;
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_4D;
            double dw3 = dw1;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3 - dw3 * dw3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx3, dy3, dz3, dw3);
            }

            // Contribution (0,0,0,1)
            double dx4 = dx2;
            double dy4 = dy1;
            double dz4 = dz1;
            double dw4 = dw0 - 1 - SQUISH_CONSTANT_4D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx4, dy4, dz4, dw4);
            }
        } else if (inSum >= 3) { // We're inside the pentachoron (4-Simplex) at (1,1,1,1)
            // Determine which two of (1,1,1,0), (1,1,0,1), (1,0,1,1), (0,1,1,1) are closest.
            byte aPoint = 0x0E;
            double aScore = xins;
            byte bPoint = 0x0D;
            double bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 0x0B;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 0x0B;
            }
            if (aScore <= bScore && wins < bScore) {
                bScore = wins;
                bPoint = 0x07;
            } else if (aScore > bScore && wins < aScore) {
                aScore = wins;
                aPoint = 0x07;
            }

            // Now we determine the three lattice points not part of the pentachoron that may contribute.
            // This depends on the closest two pentachoron vertices, including (0,0,0,0)
            double uins = 4 - inSum;
            if (uins < aScore || uins < bScore) { // (1,1,1,1) is one of the closest two pentachoron vertices.
                byte c = (bScore < aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsb + 2;
                    xsv_ext1 = xsv_ext2 = xsb + 1;
                    dx_ext0 = dx0 - 2 - 4 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx_ext2 = dx0 - 1 - 4 * SQUISH_CONSTANT_4D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsv_ext2 = xsb;
                    dx_ext0 = dx_ext1 = dx_ext2 = dx0 - 4 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy_ext2 = dy0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    if ((c & 0x01) != 0) {
                        ysv_ext1 += 1;
                        dy_ext1 -= 1;
                    } else {
                        ysv_ext0 += 1;
                        dy_ext0 -= 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb;
                    dy_ext0 = dy_ext1 = dy_ext2 = dy0 - 4 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz_ext2 = dz0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    if ((c & 0x03) != 0x03) {
                        if ((c & 0x03) == 0) {
                            zsv_ext0 += 1;
                            dz_ext0 -= 1;
                        } else {
                            zsv_ext1 += 1;
                            dz_ext1 -= 1;
                        }
                    } else {
                        zsv_ext2 += 1;
                        dz_ext2 -= 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb;
                    dz_ext0 = dz_ext1 = dz_ext2 = dz0 - 4 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x08) != 0) {
                    wsv_ext0 = wsv_ext1 = wsb + 1;
                    wsv_ext2 = wsb + 2;
                    dw_ext0 = dw_ext1 = dw0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 - 2 - 4 * SQUISH_CONSTANT_4D;
                } else {
                    wsv_ext0 = wsv_ext1 = wsv_ext2 = wsb;
                    dw_ext0 = dw_ext1 = dw_ext2 = dw0 - 4 * SQUISH_CONSTANT_4D;
                }
            } else { // (1,1,1,1) is not one of the closest two pentachoron vertices.
                byte c = (byte) (aPoint & bPoint); // Our three extra vertices are determined by the closest two.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsv_ext2 = xsb + 1;
                    xsv_ext1 = xsb + 2;
                    dx_ext0 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx0 - 2 - 3 * SQUISH_CONSTANT_4D;
                    dx_ext2 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsv_ext2 = xsb;
                    dx_ext0 = dx0 - 2 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx_ext2 = dx0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb + 1;
                    dy_ext0 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext1 = dy_ext2 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    if ((c & 0x01) != 0) {
                        ysv_ext2 += 1;
                        dy_ext2 -= 1;
                    } else {
                        ysv_ext1 += 1;
                        dy_ext1 -= 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysv_ext2 = ysb;
                    dy_ext0 = dy0 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext1 = dy_ext2 = dy0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb + 1;
                    dz_ext0 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext1 = dz_ext2 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    if ((c & 0x03) != 0) {
                        zsv_ext2 += 1;
                        dz_ext2 -= 1;
                    } else {
                        zsv_ext1 += 1;
                        dz_ext1 -= 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsv_ext2 = zsb;
                    dz_ext0 = dz0 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext1 = dz_ext2 = dz0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c & 0x08) != 0) {
                    wsv_ext0 = wsv_ext1 = wsb + 1;
                    wsv_ext2 = wsb + 2;
                    dw_ext0 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 - 2 - 3 * SQUISH_CONSTANT_4D;
                } else {
                    wsv_ext0 = wsv_ext1 = wsv_ext2 = wsb;
                    dw_ext0 = dw0 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw_ext2 = dw0 - 3 * SQUISH_CONSTANT_4D;
                }
            }

            // Contribution (1,1,1,0)
            double dx4 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dy4 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dz4 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dw4 = dw0 - 3 * SQUISH_CONSTANT_4D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4, dy4, dz4, dw4);
            }

            // Contribution (1,1,0,1)
            double dx3 = dx4;
            double dy3 = dy4;
            double dz3 = dz0 - 3 * SQUISH_CONSTANT_4D;
            double dw3 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3 - dw3 * dw3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx3, dy3, dz3, dw3);
            }

            // Contribution (1,0,1,1)
            double dx2 = dx4;
            double dy2 = dy0 - 3 * SQUISH_CONSTANT_4D;
            double dz2 = dz4;
            double dw2 = dw3;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2 - dw2 * dw2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx2, dy2, dz2, dw2);
            }

            // Contribution (0,1,1,1)
            double dx1 = dx0 - 3 * SQUISH_CONSTANT_4D;
            double dz1 = dz4;
            double dy1 = dy4;
            double dw1 = dw3;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1, dy1, dz1, dw1);
            }

            // Contribution (1,1,1,1)
            dx0 = dx0 - 1 - 4 * SQUISH_CONSTANT_4D;
            dy0 = dy0 - 1 - 4 * SQUISH_CONSTANT_4D;
            dz0 = dz0 - 1 - 4 * SQUISH_CONSTANT_4D;
            dw0 = dw0 - 1 - 4 * SQUISH_CONSTANT_4D;
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0 - dw0 * dw0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 1, dx0, dy0, dz0, dw0);
            }
        } else if (inSum <= 2) { // We're inside the first dispentachoron (Rectified 4-Simplex)
            double aScore;
            byte aPoint;
            boolean aIsBiggerSide = true;
            double bScore;
            byte bPoint;
            boolean bIsBiggerSide = true;

            // Decide between (1,1,0,0) and (0,0,1,1)
            if (xins + yins > zins + wins) {
                aScore = xins + yins;
                aPoint = 0x03;
            } else {
                aScore = zins + wins;
                aPoint = 0x0C;
            }

            // Decide between (1,0,1,0) and (0,1,0,1)
            if (xins + zins > yins + wins) {
                bScore = xins + zins;
                bPoint = 0x05;
            } else {
                bScore = yins + wins;
                bPoint = 0x0A;
            }

            // Closer between (1,0,0,1) and (0,1,1,0) will replace the further of a and b, if closer.
            if (xins + wins > yins + zins) {
                double score = xins + wins;
                if (aScore >= bScore && score > bScore) {
                    bScore = score;
                    bPoint = 0x09;
                } else if (aScore < bScore && score > aScore) {
                    aScore = score;
                    aPoint = 0x09;
                }
            } else {
                double score = yins + zins;
                if (aScore >= bScore && score > bScore) {
                    bScore = score;
                    bPoint = 0x06;
                } else if (aScore < bScore && score > aScore) {
                    aScore = score;
                    aPoint = 0x06;
                }
            }

            // Decide if (1,0,0,0) is closer.
            double p1 = 2 - inSum + xins;
            if (aScore >= bScore && p1 > bScore) {
                bScore = p1;
                bPoint = 0x01;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p1 > aScore) {
                aScore = p1;
                aPoint = 0x01;
                aIsBiggerSide = false;
            }

            // Decide if (0,1,0,0) is closer.
            double p2 = 2 - inSum + yins;
            if (aScore >= bScore && p2 > bScore) {
                bScore = p2;
                bPoint = 0x02;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p2 > aScore) {
                aScore = p2;
                aPoint = 0x02;
                aIsBiggerSide = false;
            }

            // Decide if (0,0,1,0) is closer.
            double p3 = 2 - inSum + zins;
            if (aScore >= bScore && p3 > bScore) {
                bScore = p3;
                bPoint = 0x04;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p3 > aScore) {
                aScore = p3;
                aPoint = 0x04;
                aIsBiggerSide = false;
            }

            // Decide if (0,0,0,1) is closer.
            double p4 = 2 - inSum + wins;
            if (aScore >= bScore && p4 > bScore) {
                bScore = p4;
                bPoint = 0x08;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p4 > aScore) {
                aScore = p4;
                aPoint = 0x08;
                aIsBiggerSide = false;
            }

            // Where each of the two closest points are determines how the extra three vertices are calculated.
            if (aIsBiggerSide == bIsBiggerSide) {
                if (aIsBiggerSide) { // Both closest points on the bigger side
                    byte c1 = (byte) (aPoint | bPoint);
                    byte c2 = (byte) (aPoint & bPoint);
                    if ((c1 & 0x01) == 0) {
                        xsv_ext0 = xsb;
                        xsv_ext1 = xsb - 1;
                        dx_ext0 = dx0 - 3 * SQUISH_CONSTANT_4D;
                        dx_ext1 = dx0 + 1 - 2 * SQUISH_CONSTANT_4D;
                    } else {
                        xsv_ext0 = xsv_ext1 = xsb + 1;
                        dx_ext0 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        dx_ext1 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    }

                    if ((c1 & 0x02) == 0) {
                        ysv_ext0 = ysb;
                        ysv_ext1 = ysb - 1;
                        dy_ext0 = dy0 - 3 * SQUISH_CONSTANT_4D;
                        dy_ext1 = dy0 + 1 - 2 * SQUISH_CONSTANT_4D;
                    } else {
                        ysv_ext0 = ysv_ext1 = ysb + 1;
                        dy_ext0 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        dy_ext1 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    }

                    if ((c1 & 0x04) == 0) {
                        zsv_ext0 = zsb;
                        zsv_ext1 = zsb - 1;
                        dz_ext0 = dz0 - 3 * SQUISH_CONSTANT_4D;
                        dz_ext1 = dz0 + 1 - 2 * SQUISH_CONSTANT_4D;
                    } else {
                        zsv_ext0 = zsv_ext1 = zsb + 1;
                        dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        dz_ext1 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    }

                    if ((c1 & 0x08) == 0) {
                        wsv_ext0 = wsb;
                        wsv_ext1 = wsb - 1;
                        dw_ext0 = dw0 - 3 * SQUISH_CONSTANT_4D;
                        dw_ext1 = dw0 + 1 - 2 * SQUISH_CONSTANT_4D;
                    } else {
                        wsv_ext0 = wsv_ext1 = wsb + 1;
                        dw_ext0 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        dw_ext1 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    }

                    // One combination is a permutation of (0,0,0,2) based on c2
                    xsv_ext2 = xsb;
                    ysv_ext2 = ysb;
                    zsv_ext2 = zsb;
                    wsv_ext2 = wsb;
                    dx_ext2 = dx0 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext2 = dy0 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext2 = dz0 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 - 2 * SQUISH_CONSTANT_4D;
                    if ((c2 & 0x01) != 0) {
                        xsv_ext2 += 2;
                        dx_ext2 -= 2;
                    } else if ((c2 & 0x02) != 0) {
                        ysv_ext2 += 2;
                        dy_ext2 -= 2;
                    } else if ((c2 & 0x04) != 0) {
                        zsv_ext2 += 2;
                        dz_ext2 -= 2;
                    } else {
                        wsv_ext2 += 2;
                        dw_ext2 -= 2;
                    }

                } else { // Both closest points on the smaller side
                    // One of the two extra points is (0,0,0,0)
                    xsv_ext2 = xsb;
                    ysv_ext2 = ysb;
                    zsv_ext2 = zsb;
                    wsv_ext2 = wsb;
                    dx_ext2 = dx0;
                    dy_ext2 = dy0;
                    dz_ext2 = dz0;
                    dw_ext2 = dw0;

                    // Other two points are based on the omitted axes.
                    byte c = (byte) (aPoint | bPoint);

                    if ((c & 0x01) == 0) {
                        xsv_ext0 = xsb - 1;
                        xsv_ext1 = xsb;
                        dx_ext0 = dx0 + 1 - SQUISH_CONSTANT_4D;
                        dx_ext1 = dx0 - SQUISH_CONSTANT_4D;
                    } else {
                        xsv_ext0 = xsv_ext1 = xsb + 1;
                        dx_ext0 = dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x02) == 0) {
                        ysv_ext0 = ysv_ext1 = ysb;
                        dy_ext0 = dy_ext1 = dy0 - SQUISH_CONSTANT_4D;
                        if ((c & 0x01) == 0x01) {
                            ysv_ext0 -= 1;
                            dy_ext0 += 1;
                        } else {
                            ysv_ext1 -= 1;
                            dy_ext1 += 1;
                        }
                    } else {
                        ysv_ext0 = ysv_ext1 = ysb + 1;
                        dy_ext0 = dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x04) == 0) {
                        zsv_ext0 = zsv_ext1 = zsb;
                        dz_ext0 = dz_ext1 = dz0 - SQUISH_CONSTANT_4D;
                        if ((c & 0x03) == 0x03) {
                            zsv_ext0 -= 1;
                            dz_ext0 += 1;
                        } else {
                            zsv_ext1 -= 1;
                            dz_ext1 += 1;
                        }
                    } else {
                        zsv_ext0 = zsv_ext1 = zsb + 1;
                        dz_ext0 = dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x08) == 0) {
                        wsv_ext0 = wsb;
                        wsv_ext1 = wsb - 1;
                        dw_ext0 = dw0 - SQUISH_CONSTANT_4D;
                        dw_ext1 = dw0 + 1 - SQUISH_CONSTANT_4D;
                    } else {
                        wsv_ext0 = wsv_ext1 = wsb + 1;
                        dw_ext0 = dw_ext1 = dw0 - 1 - SQUISH_CONSTANT_4D;
                    }

                }
            } else { // One point on each "side"
                byte c1, c2;
                if (aIsBiggerSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }

                // Two contributions are the bigger-sided point with each 0 replaced with -1.
                if ((c1 & 0x01) == 0) {
                    xsv_ext0 = xsb - 1;
                    xsv_ext1 = xsb;
                    dx_ext0 = dx0 + 1 - SQUISH_CONSTANT_4D;
                    dx_ext1 = dx0 - SQUISH_CONSTANT_4D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb + 1;
                    dx_ext0 = dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x02) == 0) {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0 - SQUISH_CONSTANT_4D;
                    if ((c1 & 0x01) == 0x01) {
                        ysv_ext0 -= 1;
                        dy_ext0 += 1;
                    } else {
                        ysv_ext1 -= 1;
                        dy_ext1 += 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x04) == 0) {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz_ext1 = dz0 - SQUISH_CONSTANT_4D;
                    if ((c1 & 0x03) == 0x03) {
                        zsv_ext0 -= 1;
                        dz_ext0 += 1;
                    } else {
                        zsv_ext1 -= 1;
                        dz_ext1 += 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x08) == 0) {
                    wsv_ext0 = wsb;
                    wsv_ext1 = wsb - 1;
                    dw_ext0 = dw0 - SQUISH_CONSTANT_4D;
                    dw_ext1 = dw0 + 1 - SQUISH_CONSTANT_4D;
                } else {
                    wsv_ext0 = wsv_ext1 = wsb + 1;
                    dw_ext0 = dw_ext1 = dw0 - 1 - SQUISH_CONSTANT_4D;
                }

                // One contribution is a permutation of (0,0,0,2) based on the smaller-sided point
                xsv_ext2 = xsb;
                ysv_ext2 = ysb;
                zsv_ext2 = zsb;
                wsv_ext2 = wsb;
                dx_ext2 = dx0 - 2 * SQUISH_CONSTANT_4D;
                dy_ext2 = dy0 - 2 * SQUISH_CONSTANT_4D;
                dz_ext2 = dz0 - 2 * SQUISH_CONSTANT_4D;
                dw_ext2 = dw0 - 2 * SQUISH_CONSTANT_4D;
                if ((c2 & 0x01) != 0) {
                    xsv_ext2 += 2;
                    dx_ext2 -= 2;
                } else if ((c2 & 0x02) != 0) {
                    ysv_ext2 += 2;
                    dy_ext2 -= 2;
                } else if ((c2 & 0x04) != 0) {
                    zsv_ext2 += 2;
                    dz_ext2 -= 2;
                } else {
                    wsv_ext2 += 2;
                    dw_ext2 -= 2;
                }
            }

            // Contribution (1,0,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_4D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_4D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_4D;
            double dw1 = dw0 - 0 - SQUISH_CONSTANT_4D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1, dy1, dz1, dw1);
            }

            // Contribution (0,1,0,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_4D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_4D;
            double dz2 = dz1;
            double dw2 = dw1;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2 - dw2 * dw2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2, dy2, dz2, dw2);
            }

            // Contribution (0,0,1,0)
            double dx3 = dx2;
            double dy3 = dy1;
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_4D;
            double dw3 = dw1;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3 - dw3 * dw3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx3, dy3, dz3, dw3);
            }

            // Contribution (0,0,0,1)
            double dx4 = dx2;
            double dy4 = dy1;
            double dz4 = dz1;
            double dw4 = dw0 - 1 - SQUISH_CONSTANT_4D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx4, dy4, dz4, dw4);
            }

            // Contribution (1,1,0,0)
            double dx5 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy5 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz5 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw5 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn5 = 2 - dx5 * dx5 - dy5 * dy5 - dz5 * dz5 - dw5 * dw5;
            if (attn5 > 0) {
                attn5 *= attn5;
                value += attn5 * attn5 * extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5, dy5, dz5, dw5);
            }

            // Contribution (1,0,1,0)
            double dx6 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy6 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz6 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw6 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn6 = 2 - dx6 * dx6 - dy6 * dy6 - dz6 * dz6 - dw6 * dw6;
            if (attn6 > 0) {
                attn6 *= attn6;
                value += attn6 * attn6 * extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6, dy6, dz6, dw6);
            }

            // Contribution (1,0,0,1)
            double dx7 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy7 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz7 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw7 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn7 = 2 - dx7 * dx7 - dy7 * dy7 - dz7 * dz7 - dw7 * dw7;
            if (attn7 > 0) {
                attn7 *= attn7;
                value += attn7 * attn7 * extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7, dy7, dz7, dw7);
            }

            // Contribution (0,1,1,0)
            double dx8 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy8 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz8 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw8 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn8 = 2 - dx8 * dx8 - dy8 * dy8 - dz8 * dz8 - dw8 * dw8;
            if (attn8 > 0) {
                attn8 *= attn8;
                value += attn8 * attn8 * extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8, dy8, dz8, dw8);
            }

            // Contribution (0,1,0,1)
            double dx9 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy9 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz9 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw9 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn9 = 2 - dx9 * dx9 - dy9 * dy9 - dz9 * dz9 - dw9 * dw9;
            if (attn9 > 0) {
                attn9 *= attn9;
                value += attn9 * attn9 * extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9, dy9, dz9, dw9);
            }

            // Contribution (0,0,1,1)
            double dx10 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy10 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz10 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw10 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn10 = 2 - dx10 * dx10 - dy10 * dy10 - dz10 * dz10 - dw10 * dw10;
            if (attn10 > 0) {
                attn10 *= attn10;
                value += attn10 * attn10 * extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        } else { // We're inside the second dispentachoron (Rectified 4-Simplex)
            double aScore;
            byte aPoint;
            boolean aIsBiggerSide = true;
            double bScore;
            byte bPoint;
            boolean bIsBiggerSide = true;

            // Decide between (0,0,1,1) and (1,1,0,0)
            if (xins + yins < zins + wins) {
                aScore = xins + yins;
                aPoint = 0x0C;
            } else {
                aScore = zins + wins;
                aPoint = 0x03;
            }

            // Decide between (0,1,0,1) and (1,0,1,0)
            if (xins + zins < yins + wins) {
                bScore = xins + zins;
                bPoint = 0x0A;
            } else {
                bScore = yins + wins;
                bPoint = 0x05;
            }

            // Closer between (0,1,1,0) and (1,0,0,1) will replace the further of a and b, if closer.
            if (xins + wins < yins + zins) {
                double score = xins + wins;
                if (aScore <= bScore && score < bScore) {
                    bScore = score;
                    bPoint = 0x06;
                } else if (aScore > bScore && score < aScore) {
                    aScore = score;
                    aPoint = 0x06;
                }
            } else {
                double score = yins + zins;
                if (aScore <= bScore && score < bScore) {
                    bScore = score;
                    bPoint = 0x09;
                } else if (aScore > bScore && score < aScore) {
                    aScore = score;
                    aPoint = 0x09;
                }
            }

            // Decide if (0,1,1,1) is closer.
            double p1 = 3 - inSum + xins;
            if (aScore <= bScore && p1 < bScore) {
                bScore = p1;
                bPoint = 0x0E;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p1 < aScore) {
                aScore = p1;
                aPoint = 0x0E;
                aIsBiggerSide = false;
            }

            // Decide if (1,0,1,1) is closer.
            double p2 = 3 - inSum + yins;
            if (aScore <= bScore && p2 < bScore) {
                bScore = p2;
                bPoint = 0x0D;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p2 < aScore) {
                aScore = p2;
                aPoint = 0x0D;
                aIsBiggerSide = false;
            }

            // Decide if (1,1,0,1) is closer.
            double p3 = 3 - inSum + zins;
            if (aScore <= bScore && p3 < bScore) {
                bScore = p3;
                bPoint = 0x0B;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p3 < aScore) {
                aScore = p3;
                aPoint = 0x0B;
                aIsBiggerSide = false;
            }

            // Decide if (1,1,1,0) is closer.
            double p4 = 3 - inSum + wins;
            if (aScore <= bScore && p4 < bScore) {
                bScore = p4;
                bPoint = 0x07;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p4 < aScore) {
                aScore = p4;
                aPoint = 0x07;
                aIsBiggerSide = false;
            }

            // Where each of the two closest points are determines how the extra three vertices are calculated.
            if (aIsBiggerSide == bIsBiggerSide) {
                if (aIsBiggerSide) { // Both closest points on the bigger side
                    byte c1 = (byte) (aPoint & bPoint);
                    byte c2 = (byte) (aPoint | bPoint);

                    // Two contributions are permutations of (0,0,0,1) and (0,0,0,2) based on c1
                    xsv_ext0 = xsv_ext1 = xsb;
                    ysv_ext0 = ysv_ext1 = ysb;
                    zsv_ext0 = zsv_ext1 = zsb;
                    wsv_ext0 = wsv_ext1 = wsb;
                    dx_ext0 = dx0 - SQUISH_CONSTANT_4D;
                    dy_ext0 = dy0 - SQUISH_CONSTANT_4D;
                    dz_ext0 = dz0 - SQUISH_CONSTANT_4D;
                    dw_ext0 = dw0 - SQUISH_CONSTANT_4D;
                    dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw0 - 2 * SQUISH_CONSTANT_4D;
                    if ((c1 & 0x01) != 0) {
                        xsv_ext0 += 1;
                        dx_ext0 -= 1;
                        xsv_ext1 += 2;
                        dx_ext1 -= 2;
                    } else if ((c1 & 0x02) != 0) {
                        ysv_ext0 += 1;
                        dy_ext0 -= 1;
                        ysv_ext1 += 2;
                        dy_ext1 -= 2;
                    } else if ((c1 & 0x04) != 0) {
                        zsv_ext0 += 1;
                        dz_ext0 -= 1;
                        zsv_ext1 += 2;
                        dz_ext1 -= 2;
                    } else {
                        wsv_ext0 += 1;
                        dw_ext0 -= 1;
                        wsv_ext1 += 2;
                        dw_ext1 -= 2;
                    }

                    // One contribution is a permutation of (1,1,1,-1) based on c2
                    xsv_ext2 = xsb + 1;
                    ysv_ext2 = ysb + 1;
                    zsv_ext2 = zsb + 1;
                    wsv_ext2 = wsb + 1;
                    dx_ext2 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dy_ext2 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dz_ext2 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
                    if ((c2 & 0x01) == 0) {
                        xsv_ext2 -= 2;
                        dx_ext2 += 2;
                    } else if ((c2 & 0x02) == 0) {
                        ysv_ext2 -= 2;
                        dy_ext2 += 2;
                    } else if ((c2 & 0x04) == 0) {
                        zsv_ext2 -= 2;
                        dz_ext2 += 2;
                    } else {
                        wsv_ext2 -= 2;
                        dw_ext2 += 2;
                    }
                } else { // Both closest points on the smaller side
                    // One of the two extra points is (1,1,1,1)
                    xsv_ext2 = xsb + 1;
                    ysv_ext2 = ysb + 1;
                    zsv_ext2 = zsb + 1;
                    wsv_ext2 = wsb + 1;
                    dx_ext2 = dx0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    dy_ext2 = dy0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    dz_ext2 = dz0 - 1 - 4 * SQUISH_CONSTANT_4D;
                    dw_ext2 = dw0 - 1 - 4 * SQUISH_CONSTANT_4D;

                    // Other two points are based on the shared axes.
                    byte c = (byte) (aPoint & bPoint);

                    if ((c & 0x01) != 0) {
                        xsv_ext0 = xsb + 2;
                        xsv_ext1 = xsb + 1;
                        dx_ext0 = dx0 - 2 - 3 * SQUISH_CONSTANT_4D;
                        dx_ext1 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    } else {
                        xsv_ext0 = xsv_ext1 = xsb;
                        dx_ext0 = dx_ext1 = dx0 - 3 * SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x02) != 0) {
                        ysv_ext0 = ysv_ext1 = ysb + 1;
                        dy_ext0 = dy_ext1 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        if ((c & 0x01) == 0) {
                            ysv_ext0 += 1;
                            dy_ext0 -= 1;
                        } else {
                            ysv_ext1 += 1;
                            dy_ext1 -= 1;
                        }
                    } else {
                        ysv_ext0 = ysv_ext1 = ysb;
                        dy_ext0 = dy_ext1 = dy0 - 3 * SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x04) != 0) {
                        zsv_ext0 = zsv_ext1 = zsb + 1;
                        dz_ext0 = dz_ext1 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        if ((c & 0x03) == 0) {
                            zsv_ext0 += 1;
                            dz_ext0 -= 1;
                        } else {
                            zsv_ext1 += 1;
                            dz_ext1 -= 1;
                        }
                    } else {
                        zsv_ext0 = zsv_ext1 = zsb;
                        dz_ext0 = dz_ext1 = dz0 - 3 * SQUISH_CONSTANT_4D;
                    }

                    if ((c & 0x08) != 0) {
                        wsv_ext0 = wsb + 1;
                        wsv_ext1 = wsb + 2;
                        dw_ext0 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
                        dw_ext1 = dw0 - 2 - 3 * SQUISH_CONSTANT_4D;
                    } else {
                        wsv_ext0 = wsv_ext1 = wsb;
                        dw_ext0 = dw_ext1 = dw0 - 3 * SQUISH_CONSTANT_4D;
                    }
                }
            } else { // One point on each "side"
                byte c1, c2;
                if (aIsBiggerSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }

                // Two contributions are the bigger-sided point with each 1 replaced with 2.
                if ((c1 & 0x01) != 0) {
                    xsv_ext0 = xsb + 2;
                    xsv_ext1 = xsb + 1;
                    dx_ext0 = dx0 - 2 - 3 * SQUISH_CONSTANT_4D;
                    dx_ext1 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb;
                    dx_ext0 = dx_ext1 = dx0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x02) != 0) {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    if ((c1 & 0x01) == 0) {
                        ysv_ext0 += 1;
                        dy_ext0 -= 1;
                    } else {
                        ysv_ext1 += 1;
                        dy_ext1 -= 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x04) != 0) {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    if ((c1 & 0x03) == 0) {
                        zsv_ext0 += 1;
                        dz_ext0 -= 1;
                    } else {
                        zsv_ext1 += 1;
                        dz_ext1 -= 1;
                    }
                } else {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz_ext1 = dz0 - 3 * SQUISH_CONSTANT_4D;
                }

                if ((c1 & 0x08) != 0) {
                    wsv_ext0 = wsb + 1;
                    wsv_ext1 = wsb + 2;
                    dw_ext0 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
                    dw_ext1 = dw0 - 2 - 3 * SQUISH_CONSTANT_4D;
                } else {
                    wsv_ext0 = wsv_ext1 = wsb;
                    dw_ext0 = dw_ext1 = dw0 - 3 * SQUISH_CONSTANT_4D;
                }

                // One contribution is a permutation of (1,1,1,-1) based on the smaller-sided point
                xsv_ext2 = xsb + 1;
                ysv_ext2 = ysb + 1;
                zsv_ext2 = zsb + 1;
                wsv_ext2 = wsb + 1;
                dx_ext2 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
                dy_ext2 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
                dz_ext2 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
                dw_ext2 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
                if ((c2 & 0x01) == 0) {
                    xsv_ext2 -= 2;
                    dx_ext2 += 2;
                } else if ((c2 & 0x02) == 0) {
                    ysv_ext2 -= 2;
                    dy_ext2 += 2;
                } else if ((c2 & 0x04) == 0) {
                    zsv_ext2 -= 2;
                    dz_ext2 += 2;
                } else {
                    wsv_ext2 -= 2;
                    dw_ext2 += 2;
                }
            }

            // Contribution (1,1,1,0)
            double dx4 = dx0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dy4 = dy0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dz4 = dz0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double dw4 = dw0 - 3 * SQUISH_CONSTANT_4D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4, dy4, dz4, dw4);
            }

            // Contribution (1,1,0,1)
            double dx3 = dx4;
            double dy3 = dy4;
            double dz3 = dz0 - 3 * SQUISH_CONSTANT_4D;
            double dw3 = dw0 - 1 - 3 * SQUISH_CONSTANT_4D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3 - dw3 * dw3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx3, dy3, dz3, dw3);
            }

            // Contribution (1,0,1,1)
            double dx2 = dx4;
            double dy2 = dy0 - 3 * SQUISH_CONSTANT_4D;
            double dz2 = dz4;
            double dw2 = dw3;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2 - dw2 * dw2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx2, dy2, dz2, dw2);
            }

            // Contribution (0,1,1,1)
            double dx1 = dx0 - 3 * SQUISH_CONSTANT_4D;
            double dz1 = dz4;
            double dy1 = dy4;
            double dw1 = dw3;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1, dy1, dz1, dw1);
            }

            // Contribution (1,1,0,0)
            double dx5 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy5 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz5 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw5 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn5 = 2 - dx5 * dx5 - dy5 * dy5 - dz5 * dz5 - dw5 * dw5;
            if (attn5 > 0) {
                attn5 *= attn5;
                value += attn5 * attn5 * extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5, dy5, dz5, dw5);
            }

            // Contribution (1,0,1,0)
            double dx6 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy6 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz6 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw6 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn6 = 2 - dx6 * dx6 - dy6 * dy6 - dz6 * dz6 - dw6 * dw6;
            if (attn6 > 0) {
                attn6 *= attn6;
                value += attn6 * attn6 * extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6, dy6, dz6, dw6);
            }

            // Contribution (1,0,0,1)
            double dx7 = dx0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dy7 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz7 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw7 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn7 = 2 - dx7 * dx7 - dy7 * dy7 - dz7 * dz7 - dw7 * dw7;
            if (attn7 > 0) {
                attn7 *= attn7;
                value += attn7 * attn7 * extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7, dy7, dz7, dw7);
            }

            // Contribution (0,1,1,0)
            double dx8 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy8 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz8 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw8 = dw0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double attn8 = 2 - dx8 * dx8 - dy8 * dy8 - dz8 * dz8 - dw8 * dw8;
            if (attn8 > 0) {
                attn8 *= attn8;
                value += attn8 * attn8 * extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8, dy8, dz8, dw8);
            }

            // Contribution (0,1,0,1)
            double dx9 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy9 = dy0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dz9 = dz0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dw9 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn9 = 2 - dx9 * dx9 - dy9 * dy9 - dz9 * dz9 - dw9 * dw9;
            if (attn9 > 0) {
                attn9 *= attn9;
                value += attn9 * attn9 * extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9, dy9, dz9, dw9);
            }

            // Contribution (0,0,1,1)
            double dx10 = dx0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dy10 = dy0 - 0 - 2 * SQUISH_CONSTANT_4D;
            double dz10 = dz0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double dw10 = dw0 - 1 - 2 * SQUISH_CONSTANT_4D;
            double attn10 = 2 - dx10 * dx10 - dy10 * dy10 - dz10 * dz10 - dw10 * dw10;
            if (attn10 > 0) {
                attn10 *= attn10;
                value += attn10 * attn10 * extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        }

        // First extra vertex
        double attn_ext0 = 2 - dx_ext0 * dx_ext0 - dy_ext0 * dy_ext0 - dz_ext0 * dz_ext0 - dw_ext0 * dw_ext0;
        if (attn_ext0 > 0) {
            attn_ext0 *= attn_ext0;
            value += attn_ext0 * attn_ext0 * extrapolate(xsv_ext0, ysv_ext0, zsv_ext0, wsv_ext0, dx_ext0, dy_ext0, dz_ext0, dw_ext0);
        }

        // Second extra vertex
        double attn_ext1 = 2 - dx_ext1 * dx_ext1 - dy_ext1 * dy_ext1 - dz_ext1 * dz_ext1 - dw_ext1 * dw_ext1;
        if (attn_ext1 > 0) {
            attn_ext1 *= attn_ext1;
            value += attn_ext1 * attn_ext1 * extrapolate(xsv_ext1, ysv_ext1, zsv_ext1, wsv_ext1, dx_ext1, dy_ext1, dz_ext1, dw_ext1);
        }

        // Third extra vertex
        double attn_ext2 = 2 - dx_ext2 * dx_ext2 - dy_ext2 * dy_ext2 - dz_ext2 * dz_ext2 - dw_ext2 * dw_ext2;
        if (attn_ext2 > 0) {
            attn_ext2 *= attn_ext2;
            value += attn_ext2 * attn_ext2 * extrapolate(xsv_ext2, ysv_ext2, zsv_ext2, wsv_ext2, dx_ext2, dy_ext2, dz_ext2, dw_ext2);
        }

        return value;
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        Grad2 grad = permGrad2[perm[xsb & PMASK] ^ (ysb & PMASK)];
        return grad.dx * dx + grad.dy * dy;
    }

    private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        Grad3 grad = permGrad3[perm[perm[xsb & PMASK] ^ (ysb & PMASK)] ^ (zsb & PMASK)];
        return grad.dx * dx + grad.dy * dy + grad.dz * dz;
    }

    private double extrapolate(int xsb, int ysb, int zsb, int wsb, double dx, double dy, double dz, double dw) {
        Grad4 grad = permGrad4[perm[perm[perm[xsb & PMASK] ^ (ysb & PMASK)] ^ (zsb & PMASK)] ^ (wsb & PMASK)];
        return grad.dx * dx + grad.dy * dy + grad.dz * dz + grad.dw * dw;
    }

    public static class Grad2 {
        double dx, dy;

        public Grad2(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public static class Grad3 {
        double dx, dy, dz;

        public Grad3(double dx, double dy, double dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public static class Grad4 {
        double dx, dy, dz, dw;

        public Grad4(double dx, double dy, double dz, double dw) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.dw = dw;
        }
    }

}