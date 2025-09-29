package AutoMonitoring.AutoMonitoring.config;

public final class RabbitNames {
    private RabbitNames() {}
    public static final String EX_PIPELINE      = "ex.pipeline";
    public static final String Q_STAGE1         = "queue.stage1";
    public static final String Q_STAGE2         = "queue.stage2";
    public static final String Q_STAGE3         = "queue.stage3";
    public static final String RK_STAGE1        = "route.stage1";
    public static final String RK_STAGE2        = "route.stage2";
    public static final String RK_STAGE3        = "route.stage3";

    public static final String Q_VALID          = "queue.valid";
    public static final String RK_VALID         = "route.valid";

    public static final String ONLY_DELAY_QUEUE = "ttl.delay.queue";
    public static final String ONLY_DELAY_QUEUE_1S = "ttl.delay.queue.1s";
    public static final String ONLY_DELAY_QUEUE_2S = "ttl.delay.queue.2s";
    public static final String ONLY_DELAY_QUEUE_3S = "ttl.delay.queue.3s";
    public static final String ONLY_DELAY_QUEUE_4S = "ttl.delay.queue.4s";

    public static final String ONLY_DELAY_QUEUE_1S_DELAY = "ttl.delay.queue.1s.delay";

    public static final String WORK_QUEUE = "working.queue.stage1";
    public static final String WORK_STAGE1        = "working.route.stage1";
    public static final String WORK_DLX_QUEUE = "working.queue.stage2";
    public static final String WORK_STAGE2        = "working.route.stage2";

    public static final String DEAD_QUEUE = "dead.queue";
    public static final String DEAD_RK = "route.daed";

}
