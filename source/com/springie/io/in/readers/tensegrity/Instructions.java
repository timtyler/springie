//* Read in information from files...

package com.springie.io.in.readers.tensegrity;

public class Instructions {

  static final String[] INS_NAMES = {
    "N",  // 0 // Node
    "X",  // 1
    "Y",  // 2
    "Z",  // 3
    "DX", // 4
    "DY", // 5
    "DZ", // 6
    "R",  // 7 // radius
    "M",  // 8 // mass
    "LK", // 9 // link
    "E",  // 10 // Elasticity
    "DA", // 11 // Damping
    "S",  // 12 // Status
    "L",  // 13 // Length
    "NA", // 14
    "CR", // 15 // Creature
    "D",  // 16 // Disabled
    "FX", // 17 // Fixed
    "F",  // 18 // ?? frequency ??
    "NG", // 19 // Node group
    "LG", // 20 // Link group
    "GO", // 21 // Gravity active
    "GS", // 22 // Gravity strength
    "PG", // 23 // Face group
    "P",  // 24 // Face
    "C",  // 25 // Colour
    "V",  // 26 // Value
    "H",  // 27 // Hidden
    "DIM", // 28 // Dimensions
    "CO", // 29 // Charge active
    "CH", // 30 // Charge extent
    "G",  // 31 // Gravity
    "SEL_ALL_LNK", // 32
    "DES_ALL_LNK", // 33
    "RST_LNK_LEN", // 34
    "TE", // 35 // Tension
    "CP", // 36 // Compression
    "TMP", // 37 // Temperature
    "VS", // 38 // Viscosity
    "CC", // 39 // Collision check
    "LD", // 40 // Links disabled
    "CE", // 41 // Continuously centre
    "NW", // 42 // Node growth
    "SL", // 43 // Speed limit
    "EX", // 44 // Excite
    "ME", // 45 // Muscles enabled
    "MA", // 46 // Muscles amplitude
    "MP", // 47 // Muscles period
    "PH", // 48 // Link phase (ticks)
    "CB", // 49 // Compass bias size
    "CX", // 50 // Continuously centre X
    "CY", // 51 // Continuously centre Y
    "CZ", // 52 // Continuously centre Z
    "CD", // 53 // Node compass heading
    "L1P", // 54 // Light 1 intensity %
    "L2P", // 55 // Light 2 intensity %
    "L3P", // 56 // Light 3 intensity %
    "L4P", // 57 // Light 4 intensity %
    "L1C", // 58 // Light 1 colour
    "L2C", // 59 // Light 2 colour
    "L3C", // 60 // Light 3 colour
    "L4C", // 61 // Light 4 colour
    "L1X", // 62 // Light 1 x %
    "L1Y", // 63 // Light 1 y %
    "L2X", // 64 // Light 2 x %
    "L2Y", // 65 // Light 2 y %
    "L3X", // 66 // Light 3 x %
    "L3Y", // 67 // Light 3 y %
    "L4X", // 68 // Light 4 x %
    "L4Y", // 69 // Light 4 y %
    "LC", // 70 // Light count (N lights follow)
    "LI", // 71 // Current light index
    "LP", // 72 // Light intensity %
    "LO", // 73 // Light colour
    "LX", // 74 // Light x %
    "LY", // 75 // Light y %
//  "CS", // 29 // Charge strength
  };

  static final int N   = 0;
  static final int X   = 1;
  static final int Y   = 2;
  static final int Z   = 3;
  static final int DX  = 4;
  static final int DY  = 5;
  static final int DZ  = 6;
  static final int R   = 7;
  static final int M   = 8;
  static final int LK  = 9;
  static final int E   = 10;
  static final int DA  = 11;
  static final int S   = 12;
  static final int L   = 13;
  static final int NA = 14;
  static final int CR  = 15;
  static final int D   = 16;
  static final int FX  = 17;
  static final int F   = 18;
  static final int NG  = 19;
  static final int LG  = 20;
  static final int GO  = 21;
  static final int GS  = 22;
  static final int PG  = 23;
  static final int P   = 24;
  static final int C   = 25;
  static final int V   = 26;
  static final int H   = 27;
  static final int DIM = 28;
  static final int CO  = 29;
  static final int CH  = 30;
  static final int G   = 31;
  static final int SEL_ALL_LNK = 32;
  static final int DES_ALL_LNK = 33;
  static final int RST_LNK_LEN = 34;
  static final int TE  = 35;
  static final int CP  = 36;
  static final int TMP = 37;
  static final int VS  = 38;
  static final int CC  = 39;
  static final int LD  = 40;
  static final int CE  = 41;
  static final int NW  = 42;
  static final int SL  = 43;
  static final int EX  = 44;
  static final int ME  = 45;
  static final int MA  = 46;
  static final int MP  = 47;
  static final int PH  = 48;
  static final int CB  = 49;
  static final int CX  = 50;
  static final int CY  = 51;
  static final int CZ  = 52;
  static final int CD  = 53;
  static final int L1P = 54;
  static final int L2P = 55;
  static final int L3P = 56;
  static final int L4P = 57;
  static final int L1C = 58;
  static final int L2C = 59;
  static final int L3C = 60;
  static final int L4C = 61;
  static final int L1X = 62;
  static final int L1Y = 63;
  static final int L2X = 64;
  static final int L2Y = 65;
  static final int L3X = 66;
  static final int L3Y = 67;
  static final int L4X = 68;
  static final int L4Y = 69;
  static final int LC = 70;
  static final int LI = 71;
  static final int LP = 72;
  static final int LO = 73;
  static final int LX = 74;
  static final int LY = 75;
}
