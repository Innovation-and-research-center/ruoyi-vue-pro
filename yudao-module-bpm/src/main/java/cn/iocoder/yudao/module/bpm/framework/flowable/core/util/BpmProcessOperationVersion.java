package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

/** Process rule version fixed when the process instance starts; absent means legacy. */
public final class BpmProcessOperationVersion {
    public static final String KEY = "bpm_dispatch_process_operation_version";
    public static final int CURRENT = 2;

    private BpmProcessOperationVersion() {}

    public static boolean isCurrent(Object value) {
        return Integer.valueOf(CURRENT).equals(value) || Long.valueOf(CURRENT).equals(value);
    }
}
