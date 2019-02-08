package org.awaitility.core;

public abstract class ConditionAwaiterFactory {

    public static ConditionAwaiterFactory getInstance()
    {
        System.getProperties().toString();
        String factoryName = System.getProperty("awaitility-condition-awaiter-factory");
        if (factoryName != null) {
            try {
                Class targetFactory = Class.forName(factoryName);
                return (ConditionAwaiterFactory) targetFactory.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not create Factory with name: "+factoryName);
            }
        }
        return new ConditionAwaiterFactoryDefault();
    }

    public abstract ConditionAwaiter newConditionAwaiter(ConditionEvaluator conditionEvaluator,
                                                         ConditionSettings conditionSettings,
                                                         TimeoutMessageSupplier timeoutMessageSupplier);

}
