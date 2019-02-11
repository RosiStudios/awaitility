package org.awaitility.core;

public class ConditionAwaiterFactoryFrauscher extends ConditionAwaiterFactory {
    public ConditionAwaiterFactoryFrauscher() {

    }

    public ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                ConditionSettings conditionSettings,
                                                TimeoutMessageSupplier timeoutMessageSupplier) {
        String awaiterConf = System.getProperty("awaiter-config");
        if (awaiterConf != null) {
            if (awaiterConf == "awaiter-multiplier") {
                String envVar = System.getProperty("awaiter-multiplier");
                int multiplier = Integer.parseInt(envVar);
                return new ConditionAwaiterTest(conditionEvaluator, conditionSettings, timeoutMessageSupplier, multiplier);
            }
            if(awaiterConf=="awaiter-load-based"){
                return new ConditionAwaiterLoadBased(conditionEvaluator,conditionSettings,timeoutMessageSupplier);
            }
        }
        return new ConditionAwaiterImpl(conditionEvaluator, conditionSettings, timeoutMessageSupplier);
    }
}
