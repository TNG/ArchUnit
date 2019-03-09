package com.tngtech.archunit.testutil.syntax;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingList;

class Parameters extends ForwardingList<Parameter> {
    private final List<Parameter> parameters;
    private final String description;

    private Parameters(List<Parameter> parameters, String description) {
        this.parameters = parameters;
        this.description = description;
    }

    Parameters(String methodName, List<Parameter> parameters) {
        this.parameters = parameters;
        description = getDescription(methodName);
    }

    private String getDescription(String methodName) {
        List<String> result = new ArrayList<>();
        result.add(verbalize(methodName));
        for (Parameter parameter : this) {
            result.add(parameter.getDescription());
        }
        return Joiner.on(" ").join(result);
    }

    Object[] getValues() {
        Object[] params = new Object[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            params[i] = parameters.get(i).getValue();
        }
        return params;
    }

    public String getDescription() {
        return description;
    }

    Parameters withDescription(String description) {
        return new Parameters(this, description);
    }

    @Override
    protected List<Parameter> delegate() {
        return parameters;
    }

    static String verbalize(String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name).replace("_", " ");
    }
}
