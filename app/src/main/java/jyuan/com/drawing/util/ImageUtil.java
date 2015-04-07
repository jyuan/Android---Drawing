package jyuan.com.drawing.util;

public class ImageUtil {

    /**
     * Change RGB to Hex
     *
     * @param r 0-255
     * @param g 0-255
     * @param b 0-255
     * @return 255, 0, 253 return FF00FD
     */
    public static String getColorInHexFromRGB(int r, int g, int b) {
        return vali(getHexNum(r)) + vali(getHexNum(g)) + vali(getHexNum(b));
    }

    private static String vali(String s) {
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    private static String getHexNum(int num) {
        int result = num / 16;
        int mod = num % 16;
        StringBuilder s = new StringBuilder();
        hexHelp(result, mod, s);
        return s.toString();
    }

    private static void hexHelp(int result, int mod, StringBuilder s) {
        char[] H = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        if (result > 0) {
            hexHelp(result / 16, result % 16, s);
        }
        s.append(H[mod]);
    }
}
