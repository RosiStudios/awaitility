package org.awaitility.core;

public class ConditionAwaiterFactory{
    private static ConditionAwaiter condidtionAwaiterInstance;

    public static ConditionAwaiter getInstance() {
        return condidtionAwaiterInstance;
    }

    public ConditionAwaiterFactory() {

    }

    private static void createConditionAwaiterImpl(ConditionEvaluator conditionEvaluator,ConditionSettings conditionSettings,Supplier<String> getTimeoutMessage){
        condidtionAwaiterInstance= new ConditionAwaiterImpl(conditionEvaluator,conditionSettings,getTimeoutMessage);
    }
}
