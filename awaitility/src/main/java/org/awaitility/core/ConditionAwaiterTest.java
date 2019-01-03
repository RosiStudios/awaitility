package org.awaitility.core;

import org.awaitility.Duration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.classpath.ClassPathResolver.existInCP;

public class ConditionAwaiterTest extends ConditionAwaiterImpl {
    private final ExecutorService executor;
    private final ConditionEvaluator conditionEvaluator;
    private final AtomicReference<Throwable> uncaughtThrowable;
    private final ConditionSettings conditionSettings;
    private final TimeoutMessageSupplier timeoutMessageSupplier;
    private final long multiplier;

    /**
     * <p>Constructor for ConditionAwaiterImpl.</p>
     *  @param conditionEvaluator     a {@link ConditionEvaluator} object.
     * @param conditionSettings      a {@link ConditionSettings} object.
     * @param timeoutMessageSupplier
     */
    ConditionAwaiterTest(ConditionEvaluator conditionEvaluator, ConditionSettings conditionSettings, TimeoutMessageSupplier timeoutMessageSupplier) {
        super(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
        this.conditionSettings = conditionSettings;
        this.conditionEvaluator = conditionEvaluator;
        this.executor = conditionSettings.getExecutorLifecycle().supplyExecutorService();
        this.uncaughtThrowable = new AtomicReference<Throwable>();
        this.timeoutMessageSupplier = timeoutMessageSupplier;
        this.multiplier = 2;
    }

    @Override
    public <T> void await(ConditionEvaluationHandler<T> conditionEvaluationHandler) {
        final Duration pollDelay = conditionSettings.getPollDelay();
        final Duration maxWaitTime = conditionSettings.getMaxWaitTime();
        final Duration minWaitTime = conditionSettings.getMinWaitTime();

        final Duration gracePeriod = maxWaitTime.multiply(multiplier);

        final long maxTimeout = gracePeriod.getValue();
        long pollingStartedNanos = System.nanoTime() - pollDelay.getValueInNS();

        int pollCount = 0;
        boolean succeededBeforeTimeout = false;
        boolean succeededInGracePeriod = false;

        ConditionEvaluationResult lastResult = null;
        Duration evaluationDuration = new Duration(0, MILLISECONDS);
        Future<ConditionEvaluationResult> currentConditionEvaluation = null;


        try {
            if (executor.isShutdown() || executor.isTerminated()) {
                throw new IllegalStateException("The executor service that Awaitility is instructed to use has been shutdown so condition evaluation cannot be performed. Is there something wrong the thread or executor configuration?");
            }

            conditionEvaluationHandler.start();
            if (!pollDelay.isZero()) {
                Thread.sleep(pollDelay.getValueInMS());
            }
            Duration pollInterval = pollDelay;
            while (gracePeriod.compareTo(evaluationDuration) > 0) {
                pollCount = pollCount + 1;
                // Only wait for the next condition evaluation for at most what's remaining of
                Duration maxWaitTimeForThisCondition = gracePeriod.minus(evaluationDuration);
                currentConditionEvaluation = executor.submit(new ConditionPoller(pollInterval));
                // Wait for condition evaluation to complete with "maxWaitTimeForThisCondition" or else throw TimeoutException
                lastResult = currentConditionEvaluation.get(maxWaitTimeForThisCondition.getValue(), maxWaitTimeForThisCondition.getTimeUnit());
                if (lastResult.isSuccessful() || lastResult.hasThrowable()) {
                    break;
                }
                pollInterval = conditionSettings.getPollInterval().next(pollCount, pollInterval);
                Thread.sleep(pollInterval.getValueInMS());
                evaluationDuration = calculateConditionEvaluationDuration(pollDelay, pollingStartedNanos);
            }
            evaluationDuration = calculateConditionEvaluationDuration(pollDelay, pollingStartedNanos);
            succeededBeforeTimeout = maxWaitTime.compareTo(evaluationDuration) > 0;
            succeededInGracePeriod = gracePeriod.compareTo(evaluationDuration) > 0;
        } catch (TimeoutException e) {
            lastResult = new ConditionEvaluationResult(false, null, e);
        } catch (ExecutionException e) {
            lastResult = new ConditionEvaluationResult(false, e.getCause(), null);
        } catch (Throwable e) {
            lastResult = new ConditionEvaluationResult(false, e, null);
        }  finally {
            if (currentConditionEvaluation != null) {
                // Cancelling future in order to avoid race-condition with last result for Hamcrest matchers
                // See https://github.com/awaitility/awaitility/issues/109
                currentConditionEvaluation.cancel(true);
            }
        }
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
            }else if(evaluationDuration.compareTo(maxWaitTime)>0&&evaluationDuration.compareTo(gracePeriod)<0&&succeededInGracePeriod){
                String message = String.format("Condition was evaluated in %s %s which is later than expected " +
                                "max timeout %s %s", evaluationDuration.getValue(), evaluationDuration.getTimeUnit(),
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

