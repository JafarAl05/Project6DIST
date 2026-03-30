package systemy.common;

import static java.lang.Math.abs;

public class HashingUtil {
    public int hash(String hostname, int min, int max) {
        int hash = (hostname.hashCode() + max) * (32768/max +abs(min));
        return hash;
    }
}
