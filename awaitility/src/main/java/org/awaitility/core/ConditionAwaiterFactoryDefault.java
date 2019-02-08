package org.awaitility.core;

public class ConditionAwaiterFactoryDefault extends ConditionAwaiterFactory {
    public ConditionAwaiterFactoryDefault() {

    }

    public ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                ConditionSettings conditionSettings,
                                                TimeoutMessageSupplier timeoutMessageSupplier) {
        String envVar = System.getProperty("multiplier");
        if(envVar!=null) {
            int mult = Integer.parseInt(envVar);
            return new ConditionAwaiterTest(conditionEvaluator,conditionSettings,timeoutMessageSupplier,mult);
        }
        return new ConditionAwaiterImpl(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
    }
}
