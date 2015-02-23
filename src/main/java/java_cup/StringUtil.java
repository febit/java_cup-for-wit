package java_cup;

public class StringUtil {

    public static String replace(String s, String sub, String with) {
        int c = 0;
        int i = s.indexOf(sub, c);
        if (i == -1) {
            return s;
        }
        int length = s.length();
        StringBuilder sb = new StringBuilder(length + with.length());
        do {
            sb.append(s.substring(c, i));
            sb.append(with);
            c = i + sub.length();
        } while ((i = s.indexOf(sub, c)) != -1);
        if (c < length) {
            sb.append(s.substring(c, length));
        }
        return sb.toString();
    }

    public static int[] indexOf(String s, String[] arr, int start) {
        int arrLen = arr.length;
        int index = Integer.MAX_VALUE;
        int last = -1;
        for (int j = 0; j < arrLen; j++) {
            int i = s.indexOf(arr[j], start);
            if (i != -1) {
                if (i < index) {
                    index = i;
                    last = j;
                }
            }
        }
        return last == -1 ? null : new int[]{last, index};
    }

    public static String replace(String s, String[] sub, String[] with) {
        if ((sub.length != with.length) || (sub.length == 0)) {
            return s;
        }
        int start = 0;
        StringBuilder buf = new StringBuilder(s.length());
        while (true) {
            int[] res = indexOf(s, sub, start);
            if (res == null) {
                break;
            }
            int end = res[1];
            buf.append(s.substring(start, end));
            buf.append(with[res[0]]);
            start = end + sub[res[0]].length();
        }
        buf.append(s.substring(start));
        return buf.toString();
    }
}
