package systemy.common;

public class HashingUtil {

    public static int hash(String hostname) {
        if (hostname == null) {
            return 0;
        }

        int rawHash = hostname.hashCode();

        int positiveHash = rawHash & 0x7fffffff;

        return positiveHash % 32768;
    }
}