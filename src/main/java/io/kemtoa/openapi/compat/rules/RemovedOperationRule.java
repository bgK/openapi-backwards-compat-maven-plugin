package io.kemtoa.openapi.compat.rules;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

/**
 * Removing endpoints is a backwards incompatible change as existing clients
 * could keep calling now missing endpoints.
 */
public class RemovedOperationRule extends Rule {

    @Override
    public void acceptPath(String key, PathItem left, PathItem right) {
        if (right == null) {
            addError("The path was removed in the new spec.");
        }
    }

    @Override
    public void acceptOperation(PathItem.HttpMethod operationKey, Operation left, Operation right) {
        if (right == null) {
            addError("The operation was removed in the new spec.");
        }
    }
}
