package org.awaitility.core;

public abstract class ConditionAwaiterFactory {

    private static ConditionAwaiterFactory conditionAwaiterFactory;

    public static ConditionAwaiterFactory getInstance() {
        if (conditionAwaiterFactory == null) {
            String factoryName = System.getProperty("awaitility-condition-awaiter-factory");
            if (factoryName != null) {
                try {
                    Class targetFactory = Class.forName(factoryName);
                    return (ConditionAwaiterFactory) targetFactory.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Could not create Factory with name: " + factoryName);
                }
            }
            conditionAwaiterFactory = new ConditionAwaiterFactoryDefault();
        }
        return conditionAwaiterFactory;
    }

    public abstract ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                         ConditionSettings conditionSettings,
                                                         TimeoutMessageSupplier timeoutMessageSupplier);

}
