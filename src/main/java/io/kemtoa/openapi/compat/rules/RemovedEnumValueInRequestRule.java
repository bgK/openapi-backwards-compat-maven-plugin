package io.kemtoa.openapi.compat.rules;

/**
 * Removing an enum value from a request parameter is backward incompatible
 * as a previously valid request will not be valid. This happens because
 * a request containing the removed enum value, valid according to the 'old'
 * Swagger spec, is not valid according to the new specs.
 */
public class RemovedEnumValueInRequestRule extends Rule {

    @Override
    public <T> void acceptEnumValue(T left, T right) {
        if (right == null && location.isRequest()) {
            addError("The enum value '" + left + "' has been removed in the new spec.");
        }
    }
}
