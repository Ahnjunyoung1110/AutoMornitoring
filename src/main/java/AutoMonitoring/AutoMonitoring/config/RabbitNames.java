package AutoMonitoring.AutoMonitoring.config;

public final class RabbitNames {
    private RabbitNames() {}
    public static final String EX_PIPELINE      = "ex.pipeline";
    public static final String Q_STAGE1         = "queue.stage1";
    public static final String Q_STAGE2         = "queue.stage2";
    public static final String RK_STAGE1        = "route.stage1";
    public static final String RK_STAGE2        = "route.stage2";

    public static final String DELAY_PIPELINE = "dex.pileline";
    public static final String DELAY_STAGE1         = "delay.queue.stage1";
    public static final String DELAY_STAGE2         = "delay.queue.stage2";
    public static final String DRK_STAGE1        = "delay.route.stage1";
    public static final String DRK_STAGE2        = "delay.route.stage2";
}
