package com.sjm.test.ev;

public class GlobalKeyListener {
    // public static class timeval extends Structure {
    // public long tv_sec; /* Seconds. */
    // public long tv_usec; /* Microseconds. */
    //
    // @Override
    // protected List<String> getFieldOrder() {
    // return Arrays.asList("tv_sec", "tv_usec");
    // }
    // };
    //
    // public static class input_event extends Structure {
    // public timeval time;// the timestamp, it returns the time at which the event happened.
    // public short type;// for example EV_REL for relative moment, EV_KEY for a keypress or
    // // release. More types are defined in include/linux/input-event-codes.h.
    // public short code;// event code, for example REL_X or KEY_BACKSPACE, again a complete
    // // list is in include/linux/input-event-codes.h.
    // public int value;// value is the value the event carries. Either a relative change for
    // // EV_REL, absolute new value for EV_ABS (joysticks ...), or 0 for EV_KEY
    // // for release, 1 for keypress and 2 for autorepeat.
    //
    // @Override
    // protected List<String> getFieldOrder() {
    // return Arrays.asList("time", "type", "code", "value");
    // }
    // };
    //
    // public interface Const {
    // int O_RDONLY = 00;
    // int O_WRONLY = 01;
    // int O_RDWR = 02;
    //
    // // Event types
    // int EV_SYN = 0x00;
    // int EV_KEY = 0x01;
    // int EV_REL = 0x02;
    // int EV_ABS = 0x03;
    // int EV_MSC = 0x04;
    // int EV_SW = 0x05;
    // int EV_LED = 0x11;
    // int EV_SND = 0x12;
    // int EV_REP = 0x14;
    // int EV_FF = 0x15;
    // int EV_PWR = 0x16;
    // int EV_FF_STATUS = 0x17;
    // int EV_MAX = 0x1f;
    // int EV_CNT = (EV_MAX + 1);
    //
    // /*
    // * Keys and buttons
    // *
    // * Most of the keys/buttons are modeled after USB HUT 1.12 (see
    // * http://www.usb.org/developers/hidpage)
    // */
    // int KEY_RESERVED = 0;
    // int KEY_ESC = 1;
    // int KEY_1 = 2;
    // int KEY_2 = 3;
    // int KEY_3 = 4;
    // int KEY_4 = 5;
    // int KEY_5 = 6;
    // int KEY_6 = 7;
    // int KEY_7 = 8;
    // int KEY_8 = 9;
    // int KEY_9 = 10;
    // int KEY_0 = 11;
    // int KEY_MINUS = 12;
    // int KEY_EQUAL = 13;
    // int KEY_BACKSPACE = 14;
    // int KEY_TAB = 15;
    // int KEY_Q = 16;
    // int KEY_W = 17;
    // int KEY_E = 18;
    // int KEY_R = 19;
    // int KEY_T = 20;
    // int KEY_Y = 21;
    // int KEY_U = 22;
    // int KEY_I = 23;
    // int KEY_O = 24;
    // int KEY_P = 25;
    // int KEY_LEFTBRACE = 26;
    // int KEY_RIGHTBRACE = 27;
    // int KEY_ENTER = 28;
    // int KEY_LEFTCTRL = 29;
    // int KEY_A = 30;
    // int KEY_S = 31;
    // int KEY_D = 32;
    // int KEY_F = 33;
    // int KEY_G = 34;
    // int KEY_H = 35;
    // int KEY_J = 36;
    // int KEY_K = 37;
    // int KEY_L = 38;
    // int KEY_SEMICOLON = 39;
    // int KEY_APOSTROPHE = 40;
    // int KEY_GRAVE = 41;
    // int KEY_LEFTSHIFT = 42;
    // int KEY_BACKSLASH = 43;
    // int KEY_Z = 44;
    // int KEY_X = 45;
    // int KEY_C = 46;
    // int KEY_V = 47;
    // int KEY_B = 48;
    // int KEY_N = 49;
    // int KEY_M = 50;
    // int KEY_COMMA = 51;
    // int KEY_DOT = 52;
    // int KEY_SLASH = 53;
    // int KEY_RIGHTSHIFT = 54;
    // int KEY_KPASTERISK = 55;
    // int KEY_LEFTALT = 56;
    // int KEY_SPACE = 57;
    // int KEY_CAPSLOCK = 58;
    // int KEY_F1 = 59;
    // int KEY_F2 = 60;
    // int KEY_F3 = 61;
    // int KEY_F4 = 62;
    // int KEY_F5 = 63;
    // int KEY_F6 = 64;
    // int KEY_F7 = 65;
    // int KEY_F8 = 66;
    // int KEY_F9 = 67;
    // int KEY_F10 = 68;
    // int KEY_NUMLOCK = 69;
    // int KEY_SCROLLLOCK = 70;
    // int KEY_KP7 = 71;
    // int KEY_KP8 = 72;
    // int KEY_KP9 = 73;
    // int KEY_KPMINUS = 74;
    // int KEY_KP4 = 75;
    // int KEY_KP5 = 76;
    // int KEY_KP6 = 77;
    // int KEY_KPPLUS = 78;
    // int KEY_KP1 = 79;
    // int KEY_KP2 = 80;
    // int KEY_KP3 = 81;
    // int KEY_KP0 = 82;
    // int KEY_KPDOT = 83;
    // int KEY_ZENKAKUHANKAKU = 85;
    // int KEY_102ND = 86;
    // int KEY_F11 = 87;
    // int KEY_F12 = 88;
    // int KEY_RO = 89;
    // int KEY_KATAKANA = 90;
    // int KEY_HIRAGANA = 91;
    // int KEY_HENKAN = 92;
    // int KEY_KATAKANAHIRAGANA = 93;
    // int KEY_MUHENKAN = 94;
    // int KEY_KPJPCOMMA = 95;
    // int KEY_KPENTER = 96;
    // int KEY_RIGHTCTRL = 97;
    // int KEY_KPSLASH = 98;
    // int KEY_SYSRQ = 99;
    // int KEY_RIGHTALT = 100;
    // int KEY_LINEFEED = 101;
    // int KEY_HOME = 102;
    // int KEY_UP = 103;
    // int KEY_PAGEUP = 104;
    // int KEY_LEFT = 105;
    // int KEY_RIGHT = 106;
    // int KEY_END = 107;
    // int KEY_DOWN = 108;
    // int KEY_PAGEDOWN = 109;
    // int KEY_INSERT = 110;
    // int KEY_DELETE = 111;
    // int KEY_MACRO = 112;
    // int KEY_MUTE = 113;
    // int KEY_VOLUMEDOWN = 114;
    // int KEY_VOLUMEUP = 115;
    // int KEY_POWER = 116;
    // int KEY_KPEQUAL = 117;
    // int KEY_KPPLUSMINUS = 118;
    // int KEY_PAUSE = 119;
    // int KEY_SCALE = 120;
    // }
    //
    // public interface Clibrary extends Library {
    // Clibrary INSTANTCE = (Clibrary) Native.loadLibrary("c", Clibrary.class);
    //
    // int open(String __file, int __oflag);
    //
    // int read(int __fd, Pointer __buf, int __nbytes);
    //
    // int write(int __fd, Pointer __buf, int __n);
    //
    // int close(int __fd);
    //
    // void perror(String __s);
    // }
    //
    // public static void main(String[] args) throws Exception {
    // Clibrary c = Clibrary.INSTANTCE;
    // int fd = c.open("/dev/input/event13", Const.O_RDONLY);
    // input_event ev = (input_event) Structure.newInstance(input_event.class);
    //
    // for (; c.read(fd, ev.getPointer(), ev.size()) != -1;) {
    // ev.read();
    // System.out.println(ev.code);
    // }
    // }
}
