package org.awaitility.core;

public class ConditionAwaiterFactory{
    private static final ConditionAwaiterFactory CONDITION_AWAITER_FACTORY = new ConditionAwaiterFactory();

    public static ConditionAwaiterFactory getInstance() {
        return CONDITION_AWAITER_FACTORY;
    }

    private ConditionAwaiterFactory() { }

    public ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                ConditionSettings conditionSettings,
                                                TimeoutMessageSupplier timeoutMessageSupplier) {
        return new ConditionAwaiterImpl(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
    }
}
