package org.awaitility.core;

public class ConditionAwaiterFactoryDefault extends ConditionAwaiterFactory {
    public ConditionAwaiterFactoryDefault() {

    }

    public ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                ConditionSettings conditionSettings,
                                                TimeoutMessageSupplier timeoutMessageSupplier) {
        return new ConditionAwaiterImpl(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
    }
}
