package io.kemtoa.openapi.compat.walker;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Diff visitor interface for the element types found in OpenAPI specifications
 *
 * To be used in conjunction with {@link OpenApiDiffWalker}.
 *
 * Methods are called by the walker for each node found in at least one of the
 * documents to compare. When a node is found only in one of the documents,
 * the corresponding parameter is null.
 */
public interface OpenApiDiffVisitor {
    default void acceptPath(String key, PathItem left, PathItem right) {
    }

    default void acceptOperation(HttpMethod operationKey, Operation left, Operation right) {
    }

    default void acceptParameter(Parameter left, Parameter right) {
    }

    default void acceptRequestBody(RequestBody left, RequestBody right) {
    }

    default void acceptResponse(String key, ApiResponse left, ApiResponse right) {
    }

    default void acceptMediaType(String key, MediaType left, MediaType right) {
    }

    default void acceptSchema(Schema left, Schema right) {
    }

    default void acceptProperty(String key, Schema left, Schema right) {
    }

    default void acceptEnumValue(String left, String right) {
    }

    default void setLocation(Location location) {
    }
}
