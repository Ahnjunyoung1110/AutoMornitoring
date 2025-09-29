package AutoMonitoring.AutoMonitoring.config;

public final class RabbitNames {
    private RabbitNames() {}

    // 4 Exchanges
    public static final String EX_PROVISIONING  = "ex.provisioning";
    public static final String EX_MONITORING    = "ex.monitoring";
    public static final String EX_DELAY         = "ex.delay";
    public static final String EX_DEAD_LETTER   = "ex.dead-letter";

    // Provisioning Queues & R-Keys
    public static final String Q_STAGE1         = "queue.stage1";
    public static final String Q_STAGE2         = "queue.stage2";
    public static final String Q_STAGE3         = "queue.stage3";
    public static final String RK_STAGE1        = "route.stage1";
    public static final String RK_STAGE2        = "route.stage2";
    public static final String RK_STAGE3        = "route.stage3";

    public static final String Q_VALID          = "queue.valid";
    public static final String RK_VALID         = "route.valid";

    // Delay Queues & R-Keys (for monitoring loop)
    public static final String Q_DELAY_DEFAULT  = "ttl.delay.queue";
    public static final String Q_DELAY_1S       = "ttl.delay.queue.1s";
    public static final String Q_DELAY_2S       = "ttl.delay.queue.2s";
    public static final String Q_DELAY_3S       = "ttl.delay.queue.3s";
    public static final String Q_DELAY_4S       = "ttl.delay.queue.4s";
    public static final String RK_DELAY_DEFAULT = "route.delay.default";
    public static final String RK_DELAY_1S      = "route.delay.1s";
    public static final String RK_DELAY_2S      = "route.delay.2s";
    public static final String RK_DELAY_3S      = "route.delay.3s";
    public static final String RK_DELAY_4S      = "route.delay.4s";

    // Delay Queue & R-Key (for retry mechanism)
    public static final String Q_RETRY_DELAY_1S = "ttl.retry.delay.1s";
    public static final String RK_RETRY_DELAY_1S = "route.retry.delay.1s";

    // Monitoring Queues & R-Keys
    public static final String Q_WORK           = "working.queue.stage1";
    public static final String RK_WORK          = "working.route.stage1";
    public static final String Q_WORK_DLX       = "working.queue.stage2"; // for retrying
    public static final String RK_WORK_DLX      = "working.route.stage2";

    // Dead Letter Queue & R-Key
    public static final String Q_DEAD           = "dead.queue";
    public static final String RK_DEAD          = "route.dead"; // Generic dead-letter key
}
