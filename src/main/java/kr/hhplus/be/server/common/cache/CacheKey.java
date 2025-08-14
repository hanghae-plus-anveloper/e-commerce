package kr.hhplus.be.server.common.cache;

public enum CacheKey {
    TOP_PRODUCTS("CACHE:TOP_PRODUCTS"),
    PRODUCT("CACHE:PRODUCT"),
    BALANCE("CACHE:BALANCE");

    private final String prefix;

    CacheKey(String prefix) {
        this.prefix = prefix;
    }

    public String key(Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object p : parts) {
            sb.append(':').append(p);
        }
        return sb.toString();
    }
}
