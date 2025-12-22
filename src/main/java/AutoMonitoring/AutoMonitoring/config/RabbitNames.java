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

    public static final String EX_VALID = "ex.valid";

    public static final int VALID_PARTITIONS = 8;

    public static final String Q_VALID_PREFIX = "q.valid.";

    public static String qValid(int partition) {
        return Q_VALID_PREFIX + partition;
    }

    public static String routingValid(int partition) {
        return "valid." + partition;
    }

    // Program 도메인에 커맨드를 입력하는 큐
    public static final String Q_PROGRAM_COMMAND  = "queue.program.command";
    public static final String RK_PROGRAM_COMMAND = "route.program.command";
    public static final String EX_PROGRAM_COMMAND = "ex.program.command";

    // MonitoringWorker 도메인에 커맨드를 입력하는 큐
    public static final String Q_MONITORING_COMMAND  = "queue.monitoring.command";
    public static final String RK_MONITORING_COMMAND = "route.monitoring.command";
    public static final String EX_MONITORING_COMMAND = "ex.monitoring.command";

    // CheckValid 도메인에 커맨드를 입력하는 큐
    public static final String Q_CHECKVALID_COMMAND  = "queue.checkvalid.command";
    public static final String RK_CHECKVALID_COMMAND = "route.checkvalid.command";
    public static final String EX_CHECKVALID_COMMAND = "ex.checkvalid.command";

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
    public static final String Q_RETRY_DELAY = "ttl.retry.delay";
    public static final String RK_RETRY_DELAY = "route.retry.delay";

    // Monitoring Queues & R-Keys
    public static final String Q_WORK           = "working.queue.stage1";
    public static final String RK_WORK          = "working.route.stage1";
    public static final String Q_WORK_DLX       = "working.queue.stage2"; // for retrying
    public static final String RK_WORK_DLX      = "working.route.stage2";

    // Dead Letter Queue & R-Key
    public static final String Q_DEAD           = "dead.queue";
    public static final String RK_DEAD          = "route.dead"; // Generic dead-letter key
}
