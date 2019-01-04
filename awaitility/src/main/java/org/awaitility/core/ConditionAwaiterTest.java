package org.awaitility.core;

import org.awaitility.Duration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.classpath.ClassPathResolver.existInCP;

public class ConditionAwaiterTest extends ConditionAwaiterImpl {
    protected final long multiplier = 3;

    /**
     * <p>Constructor for ConditionAwaiterImpl.</p>
     *
     * @param conditionEvaluator     a {@link ConditionEvaluator} object.
     * @param conditionSettings      a {@link ConditionSettings} object.
     * @param timeoutMessageSupplier
     */
    ConditionAwaiterTest(ConditionEvaluator conditionEvaluator, ConditionSettings conditionSettings, TimeoutMessageSupplier timeoutMessageSupplier) {
        super(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
    }

    @Override
    public <T> void await(ConditionEvaluationHandler<T> conditionEvaluationHandler) {
        final Duration maxWaitTime = conditionSettings.getMaxWaitTime();
        final Duration minWaitTime = conditionSettings.getMinWaitTime();
        final Duration gracePeriod = maxWaitTime.multiply(multiplier);

        evaluationDuration = new Duration(0, MILLISECONDS);
        ConditionEvaluationResult lastResult = evaluateCondition(conditionEvaluationHandler, gracePeriod);

        boolean succeededBeforeTimeout = maxWaitTime.compareTo(evaluationDuration) > 0;
        boolean succeededInGracePeriod = gracePeriod.compareTo(evaluationDuration) > 0;
        try {
            if (uncaughtThrowable.get() != null) {
                throw uncaughtThrowable.get();
            } else if (lastResult != null && lastResult.hasThrowable()) {
                throw lastResult.getThrowable();
            } else if (!succeededInGracePeriod) {
                final String message = this.getTimeoutString(maxWaitTime);

                Throwable cause = lastResult != null && lastResult.hasTrace() ? lastResult.getTrace() : null;
                // Not all systems support deadlock detection so ignore if ThreadMXBean & ManagementFactory is not in classpath
                if (existInCP("java.lang.management.ThreadMXBean") && existInCP("java.lang.management.ManagementFactory")) {
                    java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
                    try {
                        long[] threadIds = bean.findDeadlockedThreads();
                        if (threadIds != null) {
                            cause = new DeadlockException(threadIds);
                        }
                    } catch (UnsupportedOperationException ignored) {
                        // findDeadLockedThreads() not supported on this VM,
                        // don't init trace and move on.
                    }
                }
                throw new ConditionTimeoutException(message, cause);
            } else if (evaluationDuration.compareTo(minWaitTime) < 0) {

                String message = String.format("Condition was evaluated in %s %s which is earlier than expected " +
                                "minimum timeout %s %s", evaluationDuration.getValue(), evaluationDuration.getTimeUnit(),
                        minWaitTime.getValue(), minWaitTime.getTimeUnit());
                throw new ConditionTimeoutException(message);
            } else if (!succeededBeforeTimeout && succeededInGracePeriod) {
                String message = String.format("Condition was evaluated in %s %s which is later than expected " +
                                "max timeout %s %s", evaluationDuration.getValueInMS(), MILLISECONDS,
                        maxWaitTime.getValue(), maxWaitTime.getTimeUnit());
                System.out.println(message);
                throw new ConditionTimeoutException(message);
            }
        } catch (Throwable e) {
            CheckedExceptionRethrower.safeRethrow(e);
        } finally {
            uncaughtThrowable.set(null);
            conditionSettings.getExecutorLifecycle().executeNormalCleanupBehavior(executor);
        }

    }

}

