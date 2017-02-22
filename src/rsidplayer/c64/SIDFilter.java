package rsidplayer.c64;
//---------------------------------------------------------------------------
// reSID license for its use:
//This file is part of reSID, a MOS6581 SID emulator engine.
//Copyright (C) 2004  Dag Lem <resid@nimrod.no>
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//---------------------------------------------------------------------------

public class SIDFilter {
	public static final int MOS6581 = 0;
	public static final int MOS8580 = 1;
	
	/* Cutoff frequency curve determined from a real 6581 SID */
	int sidcurve6581[] = {
	    193, 193, 193, 193, 193, 193, 193, 193, 194, 193, 193, 193, 193, 193, 193, 193,
	    193, 193, 193, 193, 193, 193, 193, 193, 194, 193, 193, 193, 193, 193, 193, 193,
	    193, 193, 193, 193, 193, 193, 193, 193, 194, 194, 194, 194, 194, 194, 194, 194,
	    194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194,
	    194, 194, 194, 194, 194, 194, 194, 194, 195, 194, 194, 194, 194, 194, 194, 194,
	    194, 194, 194, 194, 194, 194, 194, 194, 195, 195, 195, 195, 195, 195, 195, 195,
	    195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195, 195,
	    195, 195, 195, 195, 195, 195, 195, 195, 196, 195, 195, 195, 195, 195, 195, 195,
	    195, 195, 195, 195, 196, 196, 196, 196, 197, 196, 196, 196, 196, 196, 196, 196,
	    196, 196, 196, 196, 196, 196, 196, 196, 197, 197, 197, 197, 197, 197, 197, 197,
	    198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198,
	    199, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198, 198,
	    199, 199, 199, 199, 199, 199, 199, 199, 200, 200, 200, 200, 200, 200, 200, 200,
	    201, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 201, 201, 201, 201,
	    202, 202, 202, 202, 202, 202, 202, 202, 203, 203, 203, 203, 203, 203, 203, 203,
	    203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203,
	    203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 204, 204, 204, 204,
	    205, 205, 205, 205, 205, 205, 205, 205, 206, 206, 206, 206, 206, 206, 206, 206,
	    207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207,
	    208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208,
	    208, 208, 208, 208, 209, 209, 209, 209, 210, 210, 210, 210, 210, 210, 210, 210,
	    210, 210, 210, 210, 211, 211, 211, 211, 212, 212, 212, 212, 212, 212, 212, 212,
	    212, 212, 212, 212, 212, 212, 212, 212, 213, 213, 213, 213, 213, 213, 213, 213,
	    213, 213, 213, 213, 213, 213, 213, 213, 214, 214, 214, 214, 214, 214, 214, 214,
	    214, 214, 214, 214, 214, 214, 214, 214, 215, 215, 215, 215, 215, 215, 215, 215,
	    216, 216, 216, 216, 217, 217, 217, 217, 218, 218, 218, 218, 218, 218, 218, 218,
	    219, 219, 219, 219, 220, 220, 220, 220, 221, 221, 221, 221, 221, 221, 221, 221,
	    222, 222, 222, 223, 223, 223, 224, 224, 225, 225, 225, 225, 225, 225, 225, 225,
	    225, 225, 225, 225, 226, 226, 226, 226, 227, 227, 227, 227, 228, 228, 228, 228,
	    229, 229, 229, 229, 229, 229, 229, 229, 230, 230, 230, 230, 231, 231, 231, 231,
	    232, 232, 232, 232, 232, 232, 232, 232, 233, 233, 233, 233, 234, 234, 234, 234,
	    235, 235, 235, 235, 236, 236, 236, 236, 237, 236, 235, 235, 234, 233, 233, 232,
	    232, 232, 232, 232, 233, 233, 233, 233, 234, 234, 234, 234, 235, 235, 235, 235,
	    236, 236, 236, 236, 237, 237, 237, 237, 238, 238, 238, 238, 239, 239, 239, 239,
	    240, 240, 240, 241, 241, 241, 242, 242, 243, 243, 243, 243, 244, 244, 244, 244,
	    245, 245, 245, 246, 246, 246, 247, 247, 248, 248, 248, 248, 249, 249, 249, 249,
	    250, 250, 251, 251, 252, 253, 253, 254, 255, 255, 255, 255, 256, 256, 256, 256,
	    257, 257, 257, 257, 258, 258, 258, 258, 259, 259, 259, 260, 260, 260, 261, 261,
	    262, 262, 262, 263, 263, 263, 264, 264, 265, 265, 266, 266, 267, 268, 268, 269,
	    270, 270, 271, 271, 272, 272, 273, 273, 274, 274, 274, 274, 274, 274, 274, 274,
	    275, 275, 276, 276, 277, 278, 278, 279, 280, 280, 280, 280, 281, 281, 281, 281,
	    282, 282, 283, 284, 285, 285, 286, 287, 288, 288, 288, 289, 289, 289, 290, 290,
	    291, 291, 292, 293, 294, 295, 296, 297, 298, 298, 299, 299, 300, 301, 301, 302,
	    303, 304, 305, 306, 307, 308, 309, 310, 311, 311, 311, 311, 311, 311, 311, 311,
	    311, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325,
	    326, 326, 327, 327, 328, 329, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338,
	    339, 340, 341, 342, 343, 344, 345, 346, 348, 348, 349, 350, 351, 352, 353, 354,
	    355, 356, 357, 359, 360, 361, 363, 364, 366, 365, 364, 363, 363, 362, 361, 360,
	    360, 361, 362, 363, 365, 366, 367, 368, 370, 371, 372, 373, 375, 376, 377, 378,
	    380, 381, 382, 384, 385, 386, 388, 389, 391, 392, 393, 394, 395, 396, 397, 398,
	    400, 401, 403, 404, 406, 407, 409, 410, 412, 413, 415, 416, 418, 419, 421, 422,
	    424, 425, 427, 429, 431, 432, 434, 436, 438, 439, 440, 441, 442, 443, 444, 445,
	    447, 448, 450, 452, 454, 456, 458, 460, 462, 463, 465, 467, 469, 470, 472, 474,
	    476, 478, 481, 483, 486, 489, 491, 494, 497, 498, 500, 502, 504, 505, 507, 509,
	    511, 513, 516, 518, 521, 524, 526, 529, 532, 534, 537, 540, 543, 545, 548, 551,
	    554, 557, 560, 563, 567, 570, 573, 576, 580, 580, 581, 582, 583, 584, 585, 586,
	    587, 590, 593, 596, 600, 603, 606, 609, 613, 616, 619, 622, 625, 628, 631, 634,
	    638, 642, 646, 650, 654, 658, 662, 666, 671, 674, 677, 680, 684, 687, 690, 693,
	    697, 701, 705, 709, 714, 718, 722, 726, 731, 735, 740, 745, 750, 754, 759, 764,
	    769, 774, 779, 784, 789, 794, 799, 804, 809, 812, 816, 820, 824, 828, 832, 836,
	    840, 847, 854, 861, 868, 875, 882, 889, 896, 900, 904, 908, 912, 916, 920, 924,
	    928, 935, 942, 950, 957, 964, 972, 979, 987, 993, 999, 1005, 1011, 1017, 1023, 1029,
	    1036, 1044, 1052, 1060, 1068, 1076, 1084, 1092, 1101, 1109, 1118, 1126, 1135, 1144, 1152, 1161,
	    1170, 1180, 1191, 1202, 1213, 1223, 1234, 1245, 1256, 1213, 1170, 1127, 1084, 1041, 998, 955,
	    913, 918, 924, 929, 935, 940, 946, 951, 957, 963, 969, 975, 982, 988, 994, 1000,
	    1007, 1013, 1019, 1026, 1032, 1038, 1045, 1051, 1058, 1063, 1069, 1074, 1080, 1086, 1091, 1097,
	    1103, 1110, 1118, 1125, 1133, 1141, 1148, 1156, 1164, 1171, 1179, 1186, 1194, 1201, 1209, 1216,
	    1224, 1233, 1242, 1252, 1261, 1270, 1280, 1289, 1299, 1303, 1307, 1311, 1316, 1320, 1324, 1328,
	    1333, 1342, 1352, 1362, 1372, 1381, 1391, 1401, 1411, 1419, 1427, 1435, 1443, 1451, 1459, 1467,
	    1476, 1487, 1499, 1510, 1522, 1533, 1545, 1556, 1568, 1576, 1584, 1593, 1601, 1609, 1618, 1626,
	    1635, 1646, 1658, 1670, 1682, 1693, 1705, 1717, 1729, 1742, 1755, 1768, 1782, 1795, 1808, 1821,
	    1835, 1845, 1856, 1867, 1878, 1889, 1900, 1911, 1922, 1924, 1926, 1928, 1930, 1932, 1934, 1936,
	    1939, 1951, 1964, 1977, 1990, 2003, 2016, 2029, 2042, 2054, 2067, 2079, 2092, 2105, 2117, 2130,
	    2143, 2157, 2171, 2186, 2200, 2214, 2229, 2243, 2258, 2270, 2283, 2295, 2308, 2320, 2333, 2345,
	    2358, 2373, 2389, 2404, 2420, 2436, 2451, 2467, 2483, 2499, 2515, 2531, 2547, 2563, 2579, 2595,
	    2611, 2629, 2647, 2666, 2684, 2702, 2721, 2739, 2758, 2768, 2779, 2790, 2801, 2811, 2822, 2833,
	    2844, 2862, 2880, 2898, 2916, 2934, 2952, 2970, 2988, 3005, 3022, 3040, 3057, 3074, 3092, 3109,
	    3127, 3145, 3164, 3182, 3201, 3219, 3238, 3256, 3275, 3293, 3311, 3329, 3347, 3365, 3383, 3401,
	    3420, 3440, 3461, 3481, 3502, 3523, 3543, 3564, 3585, 3604, 3624, 3644, 3664, 3683, 3703, 3723,
	    3743, 3764, 3786, 3807, 3829, 3851, 3872, 3894, 3916, 3904, 3893, 3881, 3870, 3858, 3847, 3835,
	    3824, 3844, 3864, 3884, 3904, 3924, 3944, 3964, 3984, 4000, 4017, 4034, 4051, 4068, 4085, 4102,
	    4119, 4139, 4159, 4179, 4200, 4220, 4240, 4260, 4281, 4296, 4312, 4328, 4344, 4360, 4376, 4392,
	    4408, 4435, 4462, 4490, 4517, 4544, 4572, 4599, 4627, 4653, 4679, 4705, 4731, 4757, 4783, 4809,
	    4835, 4862, 4889, 4917, 4944, 4971, 4999, 5026, 5054, 5067, 5081, 5094, 5108, 5122, 5135, 5149,
	    5163, 5185, 5208, 5231, 5254, 5277, 5300, 5323, 5346, 5367, 5388, 5409, 5431, 5452, 5473, 5494,
	    5516, 5540, 5564, 5588, 5612, 5636, 5660, 5684, 5709, 5728, 5748, 5767, 5787, 5807, 5826, 5846,
	    5866, 5890, 5915, 5940, 5965, 5989, 6014, 6039, 6064, 6090, 6116, 6143, 6169, 6195, 6222, 6248,
	    6275, 6303, 6332, 6360, 6389, 6417, 6446, 6474, 6503, 6515, 6527, 6539, 6552, 6564, 6576, 6588,
	    6601, 6625, 6649, 6674, 6698, 6722, 6747, 6771, 6796, 6821, 6847, 6872, 6898, 6924, 6949, 6975,
	    7001, 7027, 7054, 7080, 7107, 7134, 7160, 7187, 7214, 7235, 7256, 7277, 7299, 7320, 7341, 7362,
	    7384, 7409, 7434, 7459, 7485, 7510, 7535, 7560, 7586, 7610, 7635, 7659, 7684, 7709, 7733, 7758,
	    7783, 7811, 7840, 7868, 7897, 7925, 7954, 7982, 8011, 8030, 8050, 8069, 8089, 8109, 8128, 8148,
	    8168, 8194, 8221, 8248, 8275, 8302, 8329, 8356, 8383, 8408, 8433, 8459, 8484, 8509, 8535, 8560,
	    8586, 8613, 8640, 8667, 8695, 8722, 8749, 8776, 8804, 8827, 8851, 8874, 8898, 8921, 8945, 8968,
	    8992, 9019, 9046, 9074, 9101, 9128, 9156, 9183, 9211, 9251, 9292, 9332, 9373, 9413, 9454, 9494,
	    9535, 9559, 9584, 9609, 9634, 9659, 9684, 9709, 9734, 9672, 9610, 9548, 9487, 9425, 9363, 9301,
	    9240, 9265, 9290, 9315, 9341, 9366, 9391, 9416, 9442, 9467, 9492, 9517, 9543, 9568, 9593, 9618,
	    9644, 9675, 9706, 9737, 9769, 9800, 9831, 9862, 9894, 9918, 9943, 9968, 9993, 10017, 10042, 10067,
	    10092, 10122, 10152, 10182, 10212, 10242, 10272, 10302, 10333, 10360, 10388, 10415, 10443, 10470, 10498, 10525,
	    10553, 10582, 10611, 10640, 10669, 10698, 10727, 10756, 10785, 10800, 10816, 10832, 10848, 10864, 10880, 10896,
	    10912, 10938, 10964, 10990, 11016, 11042, 11068, 11094, 11120, 11143, 11167, 11191, 11215, 11239, 11263, 11287,
	    11311, 11337, 11363, 11389, 11415, 11441, 11467, 11493, 11520, 11541, 11563, 11585, 11607, 11629, 11651, 11673,
	    11695, 11720, 11746, 11772, 11798, 11824, 11850, 11876, 11902, 11927, 11953, 11978, 12004, 12029, 12055, 12080,
	    12106, 12137, 12168, 12200, 12231, 12262, 12294, 12325, 12357, 12360, 12363, 12366, 12369, 12372, 12375, 12378,
	    12381, 12404, 12427, 12450, 12474, 12497, 12520, 12543, 12567, 12591, 12615, 12639, 12664, 12688, 12712, 12736,
	    12761, 12786, 12812, 12837, 12863, 12889, 12914, 12940, 12966, 12988, 13010, 13032, 13054, 13076, 13098, 13120,
	    13143, 13167, 13191, 13215, 13239, 13263, 13287, 13311, 13336, 13362, 13389, 13416, 13443, 13469, 13496, 13523,
	    13550, 13579, 13608, 13637, 13667, 13696, 13725, 13754, 13784, 13801, 13819, 13836, 13854, 13872, 13889, 13907,
	    13925, 13950, 13975, 14001, 14026, 14051, 14077, 14102, 14128, 14152, 14176, 14200, 14225, 14249, 14273, 14297,
	    14322, 14349, 14377, 14404, 14432, 14460, 14487, 14515, 14543, 14564, 14585, 14607, 14628, 14649, 14671, 14692,
	    14714, 14739, 14765, 14790, 14816, 14842, 14867, 14893, 14919, 14948, 14978, 15008, 15038, 15068, 15098, 15128,
	    15158, 15184, 15211, 15238, 15265, 15292, 15319, 15346, 15373, 15369, 15365, 15362, 15358, 15354, 15351, 15347,
	    15344, 15365, 15386, 15407, 15428, 15449, 15470, 15491, 15512, 15539, 15566, 15593, 15620, 15647, 15674, 15701,
	    15728, 15754, 15780, 15806, 15833, 15859, 15885, 15911, 15938, 15958, 15978, 15998, 16018, 16038, 16058, 16078,
	    16098, 16125, 16152, 16179, 16207, 16234, 16261, 16288, 16316, 16338, 16361, 16383, 16406, 16429, 16451, 16474,
	    16497, 16525, 16554, 16582, 16611, 16640, 16668, 16697, 16726, 16739, 16752, 16765, 16778, 16791, 16804, 16817,
	    16831, 16857, 16883, 16909, 16935, 16961, 16987, 17013, 17039, 17061, 17084, 17107, 17130, 17153, 17176, 17199,
	    17222, 17240, 17259, 17278, 17297, 17316, 17335, 17354, 17373, 17395, 17418, 17441, 17464, 17487, 17510, 17533,
	    17556, 17571, 17587, 17602, 17618, 17633, 17649, 17664, 17680, 17702, 17725, 17748, 17771, 17793, 17816, 17839,
	    17862, 17878, 17894, 17911, 17927, 17943, 17960, 17976, 17993, 18001, 18010, 18018, 18027, 18035, 18044, 18052,
	    18061, 18078, 18096, 18113, 18131, 18148, 18166, 18183, 18201, 18212, 18224, 18235, 18247, 18258, 18270, 18281,
	    18293, 18309, 18325, 18341, 18358, 18374, 18390, 18406, 18423, 18434, 18445, 18456, 18467, 18478, 18489, 18500,
	    18511, 18525, 18540, 18554, 18569, 18584, 18598, 18613, 18628, 18640, 18652, 18664, 18677, 18689, 18701, 18713,
	    18726, 18735, 18744, 18753, 18762, 18771, 18780, 18789, 18799, 18810, 18822, 18833, 18845, 18856, 18868, 18879,
	    18891, 18898, 18906, 18914, 18922, 18929, 18937, 18945, 18953, 18963, 18973, 18983, 18993, 19003, 19013, 19023,
	    19034, 19040, 19046, 19053, 19059, 19065, 19072, 19078, 19085, 19091, 19098, 19104, 19111, 19117, 19124, 19130,
	    19137, 19142, 19148, 19153, 19159, 19164, 19170, 19175, 19181, 19186, 19191, 19196, 19201, 19206, 19211, 19216,
	    19221, 19231, 19241, 19252, 19262, 19272, 19283, 19293, 19304, 19304, 19304, 19304, 19304, 19304, 19304, 19304,
	};
	
	SIDFilter() {
		  fc = 0;

		  res = 0;

		  filt = 0;

		  voice3off = 0;

		  hp_bp_lp = 0;

		  vol = 0;

		  // State of filter.
		  //Vhp = 0;
		  //Vbp = 0;
		  //Vlp = 0;
		  fHPPrev = 0.0;
		  fBPPrev = 0.0;
		  fLPPrev = 0.0;
		  Vnf = 0;

		  enable_filter(true);

		  // Create mappings from FC to cutoff frequency.
		/*  interpolate(f0_points_6581, f0_points_6581
			      + sizeof(f0_points_6581)/sizeof(*f0_points_6581) - 1,
			      PointPlotter<sound_sample>(f0_6581), 1.0);
		  interpolate(f0_points_8580, f0_points_8580
			      + sizeof(f0_points_8580)/sizeof(*f0_points_8580) - 1,
			      PointPlotter<sound_sample>(f0_8580), 1.0);*/
		  
		  for(int i = 0; i < 0x800; ++i) {
				f0_8580[i] = (int) ((((12000.0-30.0)/2048.0)*i)+30.0);
		  }
		  
		  for(int i = 0; i < 0x800; ++i) {
				f0_6581[i] = sidcurve6581[i];//(int)(((Math.tanh((6.0*(i&0x7FF)/2048.0)-3.0)+1.0)*0.5*(10000.0-30.0)) + 30);
		  }

		  set_chip_model(MOS6581);
	}
	void enable_filter(boolean enable) {
		enabled = enable;
	}

	void set_chip_model(int model) {
		if (model == MOS6581) {
		    // The mixer has a small input DC offset. This is found as follows:
		    //
		    // The "zero" output level of the mixer measured on the SID audio
		    // output pin is 5.50V at zero volume, and 5.44 at full
		    // volume. This yields a DC offset of (5.44V - 5.50V) = -0.06V.
		    //
		    // The DC offset is thus -0.06V/1.05V ~ -1/18 of the dynamic range
		    // of one voice. See voice.cc for measurement of the dynamic
		    // range.

		    mixer_DC = -0xfff*0xff/18 >> 7;

		    f0 = f0_6581;
		  }
		  else {
		    // No DC offsets in the MOS8580.
		    mixer_DC = 0;

		    f0 = f0_8580;
		  }

		  set_w0();
		  set_Q();
	}

	void clock(int delta_t,
			int voice1, int voice2, int voice3,
			int ext_in) {
		// Scale each voice down from 20 to 13 bits.
		voice1 >>= 7;
		voice2 >>= 7;

		// NB! Voice 3 is not silenced by voice3off if it is routed through
		// the filter.
		if ((voice3off!=0) && ((filt & 0x04)==0)) {
			voice3 = 0;
		}
		else {
			voice3 >>= 7;
		}

		ext_in >>= 7;

		// Enable filter on/off.
		// This is not really part of SID, but is useful for testing.
		// On slow CPUs it may be necessary to bypass the filter to lower the CPU
		// load.
		if (!enabled) {
			Vnf = voice1 + voice2 + voice3 + ext_in;
			//Vhp = Vbp = Vlp = 0;
			fHPPrev = fBPPrev = fLPPrev = 0.0;
			return;
		}

		// Route voices into or around filter.
		// The code below is expanded to a switch for faster execution.
		// (filt1 ? Vi : Vnf) += voice1;
		// (filt2 ? Vi : Vnf) += voice2;
		// (filt3 ? Vi : Vnf) += voice3;

		int Vi;

		switch (filt) {
		default:
		case 0x0:
			Vi = 0;
			Vnf = voice1 + voice2 + voice3 + ext_in;
			break;
		case 0x1:
			Vi = voice1;
			Vnf = voice2 + voice3 + ext_in;
			break;
		case 0x2:
			Vi = voice2;
			Vnf = voice1 + voice3 + ext_in;
			break;
		case 0x3:
			Vi = voice1 + voice2;
			Vnf = voice3 + ext_in;
			break;
		case 0x4:
			Vi = voice3;
			Vnf = voice1 + voice2 + ext_in;
			break;
		case 0x5:
			Vi = voice1 + voice3;
			Vnf = voice2 + ext_in;
			break;
		case 0x6:
			Vi = voice2 + voice3;
			Vnf = voice1 + ext_in;
			break;
		case 0x7:
			Vi = voice1 + voice2 + voice3;
			Vnf = ext_in;
			break;
		case 0x8:
			Vi = ext_in;
			Vnf = voice1 + voice2 + voice3;
			break;
		case 0x9:
			Vi = voice1 + ext_in;
			Vnf = voice2 + voice3;
			break;
		case 0xa:
			Vi = voice2 + ext_in;
			Vnf = voice1 + voice3;
			break;
		case 0xb:
			Vi = voice1 + voice2 + ext_in;
			Vnf = voice3;
			break;
		case 0xc:
			Vi = voice3 + ext_in;
			Vnf = voice1 + voice2;
			break;
		case 0xd:
			Vi = voice1 + voice3 + ext_in;
			Vnf = voice2;
			break;
		case 0xe:
			Vi = voice2 + voice3 + ext_in;
			Vnf = voice1;
			break;
		case 0xf:
			Vi = voice1 + voice2 + voice3 + ext_in;
			Vnf = 0;
			break;
		}

		// Maximum delta cycles for the filter to work satisfactorily under current
		// cutoff frequency and resonance constraints is approximately 8.
		/*int delta_t_flt = 8;

		while (delta_t > 0) {
			if (delta_t < delta_t_flt) {
				delta_t_flt = delta_t;
			}

			// delta_t is converted to seconds given a 1MHz clock by dividing
			// with 1 000 000. This is done in two operations to avoid integer
			// multiplication overflow.

			// Calculate filter outputs.
			// Vhp = Vbp/Q - Vlp - Vi;
			// dVbp = -w0*Vhp*dt;
			// dVlp = -w0*Vbp*dt;
			int w0_delta_t = w0_ceil_dt*delta_t_flt >> 6;

			int dVbp = (w0_delta_t*Vhp >> 14);
			int dVlp = (w0_delta_t*Vbp >> 14);
			Vbp -= dVbp;
			Vlp -= dVlp;
			Vhp = (Vbp*_1024_div_Q >> 10) - Vlp - Vi;

			delta_t -= delta_t_flt;
		}*/
		
	   double fBP, fHP, fLP;
	   double fRes;

	   fBP = 0.0;
	   fHP = 0.0;
	   fLP = 0.0;
	   double out = 0.0;

	   int iterations;

	  /* for(iterations = 0; iterations < FILT_ITER; ++iterations)
	   {
	      fLP = nonlinearity(state->fBPPrev * state->f) + state->fLPPrev;

	      fHP = (double)in - state->fBPPrev * state->q - fLP;

	      fBP = nonlinearity(fHP * state->f) + state->fBPPrev;

	      state->fHPPrev = fHP;
	      state->fBPPrev = fBP;
	      state->fLPPrev = fLP;
	   }*/

	   fHP = fHPPrev;
	   fBP = fBPPrev;
	   fLP = fLPPrev;

	   /* Double integrator loop State Variable Filter */
	   
	   double dt = 1.0 / 1000000.0;

	   double f;
	   for(iterations = 0; iterations < delta_t; ++iterations)
	   {
	      /* Inverting summer stage */
	      /* Calculate resonance-regulating feedback:
	         band pass output scaled by q and inverted. */
	      //fRes = -(fBP * state->q);
	      //fHP = -((double)in + fRes + fLP);
	        fHP = -((double)Vi + fLP - fBP/Q);

	      /* inverting "integrator" 1 */
	      f = sid_freqconstant(w0, fHP);
	      fBP -= f * (fHP * dt);

	      /* inverting "integrator" 2 */
	      f = sid_freqconstant(w0, fBP);
	      fLP -= f * (fBP * dt);
	   }
	   
	   fHPPrev = fHP;
	   fBPPrev = fBP;
	   fLPPrev = fLP;
	}
	
	final double AMPLITUDE_DEPENDENCE_COEFFICIENT= 1.0;
	double mosfet_nonlinearity(double x)
	{

	   double result = 1+AMPLITUDE_DEPENDENCE_COEFFICIENT*x;

	   return result;
	}

	final double DISTORTION_LEVEL= (3*2048);
	final double DISTORTION_LEVEL2=(2.5*2048);
	double sid_freqconstant(double w0, double prevstage)
	{
	    //#ifdef SID_6581
	    if(prevstage >= 0.0)
	        return w0 * mosfet_nonlinearity(Math.abs(prevstage)/DISTORTION_LEVEL);
	    else
	        return w0 * mosfet_nonlinearity(Math.abs(prevstage)/DISTORTION_LEVEL2);
	    //#endif // SID_6581

	    //#ifdef SID_8580
	    //return state->w0;
	    //#endif // SID_8580
	}
	
	void reset() {
		  fc = 0;

		  res = 0;

		  filt = 0;

		  voice3off = 0;

		  hp_bp_lp = 0;

		  vol = 0;

		  // State of filter.
		  //Vhp = 0;
		  //Vbp = 0;
		  //Vlp = 0;
		  fHPPrev = 0.0;
		  fBPPrev = 0.0;
		  fLPPrev = 0.0;
		  Vnf = 0;

		  set_w0();
		  set_Q();
	}

	// Write registers.
	void writeFC_LO(int fc_lo) {
		fc = fc & 0x7f8 | fc_lo & 0x007;
		set_w0();
	}
	void writeFC_HI(int fc_hi) {
		fc = (fc_hi << 3) & 0x7f8 | fc & 0x007;
		set_w0();
	}
	void writeRES_FILT(int res_filt) {
		res = (res_filt >> 4) & 0x0f;
		set_Q();

		filt = res_filt & 0x0f;
	}
	void writeMODE_VOL(int mode_vol) {
		voice3off = mode_vol & 0x80;

		hp_bp_lp = (mode_vol >> 4) & 0x07;

		vol = mode_vol & 0x0f;
	}

	// SID audio output (16 bits).
	int output() {
		  // This is handy for testing.
		  if (!enabled) {
		    return (Vnf + mixer_DC)*(int)(vol)/15;
		  }

		  // Mix highpass, bandpass, and lowpass outputs. The sum is not
		  // weighted, this can be confirmed by sampling sound output for
		  // e.g. bandpass, lowpass, and bandpass+lowpass from a SID chip.

		  // The code below is expanded to a switch for faster execution.
		  // if (hp) Vf += Vhp;
		  // if (bp) Vf += Vbp;
		  // if (lp) Vf += Vlp;

		  //int Vf;
          double Vf;
          
		  switch (hp_bp_lp) {
		  default:
		  case 0x0:
		    Vf = 0;
		    break;
		  case 0x1:
		    Vf = fLPPrev;//Vlp;
		    break;
		  case 0x2:
		    Vf = fBPPrev;//Vbp;
		    break;
		  case 0x3:
		    Vf = fLPPrev + fBPPrev;//Vlp + Vbp;
		    break;
		  case 0x4:
		    Vf = fHPPrev;//Vhp;
		    break;
		  case 0x5:
		    Vf = fLPPrev + fHPPrev;//Vlp + Vhp;
		    break;
		  case 0x6:
		    Vf = fBPPrev + fHPPrev;//Vbp + Vhp;
		    break;
		  case 0x7:
		    Vf = fLPPrev + fBPPrev + fHPPrev;//Vlp + Vbp + Vhp;
		    break;
		  }

		  // Sum non-filtered and filtered output.
		  // Multiply the sum with volume.
		  return (Vnf + (int)Vf + mixer_DC)*(int)(vol)/15;
	}

	void set_w0() {
		//const double pi = 3.1415926535897932385;

		// Multiply with 1.048576 to facilitate division by 1 000 000 by right-
		// shifting 20 times (2 ^ 20 = 1048576).
		/*w0 = (int)(2*Math.PI*f0[fc]*1.048576);

		// Limit f0 to 16kHz to keep 1 cycle filter stable.
		final int w0_max_1 = (int)(2*Math.PI*16000*1.048576);
		w0_ceil_1 = w0 <= w0_max_1 ? w0 : w0_max_1;

		// Limit f0 to 4kHz to keep delta_t cycle filter stable.
		final int w0_max_dt = (int)(2*Math.PI*4000*1.048576);
		w0_ceil_dt = w0 <= w0_max_dt ? w0 : w0_max_dt;*/
		
		f = 2.0 * Math.sin(Math.PI * f0[fc]);
		w0 = 2.0*Math.PI*f0[fc];
	}
	void set_Q() {
		// Q is controlled linearly by res. Q has approximate range [0.707, 1.7].
		// As resonance is increased, the filter must be clocked more often to keep
		// stable.

		// The coefficient 1024 is dispensed of later by right-shifting 10 times
		// (2 ^ 10 = 1024).
		//_1024_div_Q = (int)(1024.0/(0.707 + 1.0*res/0x0f));
		Q = (0.707 + 1.0*res/0x0f);
		q = 1/Q;
	}

	// Filter enabled.
	boolean enabled;

	// Filter cutoff frequency.
	int fc;

	// Filter resonance.
	int res;

	// Selects which inputs to route through filter.
	int filt;

	// Switch voice 3 off.
	int voice3off;

	// Highpass, bandpass, and lowpass filter modes.
	int hp_bp_lp;

	// Output master volume.
	int vol;

	// Mixer DC offset.
	int mixer_DC;

	// State of filter.
    double fHPPrev;
    double fLPPrev;
    double fBPPrev;
    
    double f;
    double w0;
    double freq;
    double RC;
    double q;
    double Q;
	int Vnf; // not filtered

	// Cutoff frequency, resonance.
	//int w0, w0_ceil_1, w0_ceil_dt;
	//int _1024_div_Q;

	// Cutoff frequency tables.
	// FC is an 11 bit register.
	int[] f0_6581 = new int[2048];
	int[] f0_8580 = new int[2048];
	int[] f0;
}
