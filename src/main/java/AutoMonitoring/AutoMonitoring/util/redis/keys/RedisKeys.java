package AutoMonitoring.AutoMonitoring.util.redis.keys;


public final class RedisKeys {
    public static String argument_record_discontinuity(String traceId) {return "%s:record_discontinuity".formatted(traceId);}
    public static String queueFlag(String traceId, String resolution) {return "%s:queue_flag:%s".formatted(traceId, resolution); }
    public static String workerLock(String traceId, String resolution) {return "%s:worker_lock:%s".formatted(traceId, resolution); }
    public static String completeFlag(String traceId, String resolution, String mediaSeq) {return "%s:complete_flag:%s:%s".formatted(traceId, resolution, mediaSeq); }




    public static String state(String traceId, String resolution) {return "hls:%s:state:%s".formatted(traceId, resolution); }
    public static String hashState(String traceId, String resolution) {return "hls:%s:hashState:%s". formatted(traceId, resolution); }
    public static String hist(String traceId, String resolution) {return "hls:%s:hist:%s".formatted(traceId, resolution); }
    public static String alert(String traceId, String resolution) {return "hls:%s:alert:%s".formatted(traceId, resolution); }
}
