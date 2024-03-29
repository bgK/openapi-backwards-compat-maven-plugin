package io.kemtoa.openapi.compat.rules;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Removing a required property from an object leads to false expectation
 * on the client receiving the object. If the client is using 'old' service's
 * spec it will expect the property to be present and so it could throw
 * errors. It could be valid to assume that the client won't perform response
 * validation and this could lead to unexpected errors while parsing the
 * response and/or using the missing property.
 */
public class PropertyRemovedInResponseRule extends Rule {

    @Override
    public void acceptProperty(String key, Schema left, Schema right) {
        if (right == null && location.isResponse()) {
            addError("The property '" + key + "' has been removed in the new spec.");
        }
    }
}
