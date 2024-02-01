package io.kemtoa.openapi.compat.rules;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.Map;

/**
 * Adding a required property to a request parameter causes client requests
 * to fail if the property is not present.
 */
public class AddedRequiredRequestParameterRule extends Rule {

    @Override
    public void acceptParameter(Parameter left, Parameter right) {
        if (left == null && right.getRequired()) {
            addError("The required parameter '" + right.getName() + "' has been added in the new spec.");
        }
    }

    @Override
    public void acceptSchema(Schema left, Schema right) {
        if (right == null || right.getProperties() == null) {
            return;
        }

        for (Object rightPropertyObject : right.getProperties().entrySet()) {
            Map.Entry<String, Schema> rightProperty = (Map.Entry<String, Schema>) rightPropertyObject;
            String propertyName = rightProperty.getKey();
            boolean newPropertyIsRequired = right.getRequired() != null && right.getRequired().contains(propertyName);

            Schema leftProperty = null;
            if (left.getProperties() != null) {
                leftProperty = (Schema) left.getProperties().get(propertyName);
            }

            if (leftProperty == null && newPropertyIsRequired && location.isRequest()) {
                addError("The required property '" + propertyName + "' has been added in the new spec.");
            }
        }
    }
}
