package AutoMonitoring.AutoMonitoring.util.redis.keys;


public final class RedisKeys {
    public static String state(String traceId, String resolution) {return "hls:%s:state:%s".formatted(traceId, resolution); }
    public static String hist(String traceId, String resolution) {return "hls:%s:hist:%s".formatted(traceId, resolution); }
    public static String alert(String traceId, String resolution) {return "hls:%s:alert:%s".formatted(traceId, resolution); }
}
