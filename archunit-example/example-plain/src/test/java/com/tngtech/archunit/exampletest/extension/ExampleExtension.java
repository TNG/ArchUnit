package com.tngtech.archunit.exampletest.extension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

import static java.util.Collections.synchronizedList;

/**
 * This is a simple example, how to extend ArchUnit's rule evaluation.
 *
 * @see ArchUnitExtension
 */
public class ExampleExtension implements ArchUnitExtension {
    public static final String UNIQUE_IDENTIFIER = "archunit-example-extension";

    private static final ConcurrentHashMap<NewConfigurationEvent, Object> configurationEvents = new ConcurrentHashMap<>();
    private static final List<EvaluatedRuleEvent> evaluatedRuleEvents = synchronizedList(new ArrayList<EvaluatedRuleEvent>());

    @Override
    public String getUniqueIdentifier() {
        return UNIQUE_IDENTIFIER;
    }

    @Override
    public void configure(Properties properties) {
        configurationEvents.putIfAbsent(new NewConfigurationEvent(properties), "ignored");
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
        evaluatedRuleEvents.add(new EvaluatedRuleEvent(evaluatedRule));
    }

    public static Set<NewConfigurationEvent> getConfigurationEvents() {
        return new HashSet<>(configurationEvents.keySet());
    }

    public static List<EvaluatedRuleEvent> getEvaluatedRuleEvents() {
        return new ArrayList<>(evaluatedRuleEvents);
    }

    public static void reset() {
        configurationEvents.clear();
        evaluatedRuleEvents.clear();
    }
}
