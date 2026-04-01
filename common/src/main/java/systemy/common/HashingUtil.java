package systemy.common;

import static java.lang.Math.abs;

public class HashingUtil {
    public static int hash(String hostname, int min, int max) {
        int hash = (hostname.hashCode() + max) * (32768/max +abs(min));
        return hash;
    }

    public static int hash(String hostname) {
        int hash = (hostname.hashCode() + 2147483647) * (32768/2147483647 +abs(-2147483647));
        return hash;
    }
}
