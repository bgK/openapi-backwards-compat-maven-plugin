package io.kemtoa.openapi.compat.walker;

import java.util.*;
import java.util.stream.Stream;

import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * OpenAPI tool for comparing two specification documents.
 *
 * Traverses a pair of OpenAPI 3.0 documents, calling the specified instance
 * of {@link OpenApiDiffVisitor} for each encountered node present in at least
 * one of the documents.
 *
 * Keeps track of the position in the tree using {@link Location}.
 */
public class OpenApiDiffWalker {

    private OpenAPI openApiLeft;
    private OpenAPI openApiRight;
    private final Location location = new Location();

    private final Set<Schema> visitedSchemas = new HashSet<>(); // Used to prevent infinite recursion

    public void walk(OpenApiDiffVisitor visitor, OpenAPI openApiLeft, OpenAPI openApiRight) {
        this.openApiLeft = openApiLeft;
        this.openApiRight = openApiRight;

        visitor.setLocation(location);

        Set<String> leftKeys  = openApiLeft.getPaths() != null ? openApiLeft.getPaths().keySet() : new HashSet<>();
        Set<String> rightKeys = openApiRight.getPaths() != null ? openApiRight.getPaths().keySet() : new HashSet<>();
        Stream.concat(
                leftKeys.stream(),
                rightKeys.stream()
        ).distinct().forEach(key -> doVisitAndRecurse(
                visitor, key,
                openApiLeft.getPaths().get(key),
                openApiRight.getPaths().get(key))
        );
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, String pathKey, PathItem left, PathItem right) {
        location.pushPath("Path " + pathKey);

        try {
            visitor.acceptPath(pathKey, left, right);

            if (left == null || right == null) {
                return;
            }

            Set<PathItem.HttpMethod> leftKeys = left.readOperationsMap().keySet();
            Set<PathItem.HttpMethod> rightKeys = right.readOperationsMap().keySet();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                    left.readOperationsMap().get(key),
                    right.readOperationsMap().get(key))
            );
        } finally {
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, PathItem.HttpMethod operationKey, Operation left, Operation right) {
        location.pushPath("Operation " + operationKey);

        try {
            visitor.acceptOperation(operationKey, left, right);

            if (left == null || right == null) {
                return;
            }

            List<Parameter> leftParameters = left.getParameters() != null ? left.getParameters() : new ArrayList<>();
            List<Parameter> rightParameters = right.getParameters() != null ? right.getParameters() : new ArrayList<>();
            Stream.concat(
                    leftParameters.stream().map(Parameter::getName),
                    rightParameters.stream().map(Parameter::getName)
            ).distinct().forEach(name -> {
                Parameter leftParam = leftParameters.stream().filter(param -> param.getName().equals(name)).findAny().orElse(null);
                Parameter rightParam = rightParameters.stream().filter(param -> param.getName().equals(name)).findAny().orElse(null);
                doVisitAndRecurse(visitor, leftParam, rightParam);
            });

            doVisitAndRecurse(visitor, left.getRequestBody(), right.getRequestBody());

            Set<String> leftKeys  = left.getResponses() != null ? left.getResponses().keySet() : new HashSet<>();
            Set<String> rightKeys = right.getResponses() != null ? right.getResponses().keySet() : new HashSet<>();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                    left.getResponses().get(key),
                    right.getResponses().get(key))
            );
        } finally {
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, Parameter left, Parameter right) {
        location.pushPath("Parameter " + (left != null ? left.getName() : right.getName()));
        location.setRequest(true);

        try {
            visitor.acceptParameter(left, right);
        } finally {
            visitedSchemas.clear();
            location.setRequest(false);
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, RequestBody left, RequestBody right) {
        location.pushPath("RequestBody");
        location.setRequest(true);

        try {
            visitor.acceptRequestBody(left, right);

            if (left == null || right == null) {
                return;
            }

            if (left.getContent() == null && right.getContent() == null) {
                return;
            }

            Set<String> leftKeys  = left.getContent() != null ? left.getContent().keySet() : new HashSet<>();
            Set<String> rightKeys = right.getContent() != null ? right.getContent().keySet() : new HashSet<>();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(mediaTypeKey -> doVisitAndRecurse(
                    visitor, mediaTypeKey,
                    left.getContent().get(mediaTypeKey),
                    right.getContent().get(mediaTypeKey))
            );

        } finally {
            visitedSchemas.clear();
            location.setRequest(false);
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, String key, ApiResponse left, ApiResponse right) {
        location.pushPath("Response " + key);
        location.setResponse(true);

        try {
            visitor.acceptResponse(key, left, right);

            if (left == null || right == null) {
                return;
            }

            if (left.getContent() == null && right.getContent() == null) {
                return;
            }

            Set<String> leftKeys  = left.getContent() != null ? left.getContent().keySet() : new HashSet<>();
            Set<String> rightKeys = right.getContent() != null ? right.getContent().keySet() : new HashSet<>();
            Stream.concat(
                    leftKeys.stream(),
                    rightKeys.stream()
            ).distinct().forEach(mediaTypeKey -> doVisitAndRecurse(
                    visitor, mediaTypeKey,
                    left.getContent().get(mediaTypeKey),
                    right.getContent().get(mediaTypeKey))
            );

        } finally {
            visitedSchemas.clear();
            location.setResponse(false);
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, String key, MediaType left, MediaType right) {
        location.pushPath("MediaType " + key);

        visitor.acceptMediaType(key, left, right);

        try {
            if (left == null || right == null) {
                return;
            }

            doVisitAndRecurse(visitor, left.getSchema(), right.getSchema());
        } finally {
            location.popPath();
        }
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, Schema left, Schema right) {
        if (visitedSchemas.contains(left) && visitedSchemas.contains(right)) {
            // Prevent infinite recursion
            return;
        }

        visitor.acceptSchema(left, right);

        if (left != null) {
            visitedSchemas.add(left);
        }
        if (right != null) {
            visitedSchemas.add(right);
        }

        if (left == null || right == null) {
            return;
        }

        if (left.get$ref() != null) {
            left = resolveSchema(openApiLeft, left.get$ref());
        }

        if (right.get$ref() != null) {
            right = resolveSchema(openApiRight, right.get$ref());
        }

        if (left.getItems() != null && right.getItems() != null) {
            doVisitAndRecurse(visitor, "items", left.getItems(), right.getItems());
        } else {
            doVisitEnumValues(visitor, left.getEnum(), right.getEnum());
            doVisitAndRecurse(visitor, left.getProperties(), right.getProperties());
        }
    }

    private Schema resolveSchema(OpenAPI openAPI, String ref) {
        Components components = openAPI.getComponents();
        if (components == null) {
            throw new IllegalStateException("Unable to resolve schema reference: " + ref);
        }
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            throw new IllegalStateException("Unable to resolve schema reference: " + ref);
        }

        String simpleRef = (String) RefUtils.extractSimpleName(ref).getKey();

        Schema resolved = schemas.get(simpleRef);
        if (resolved == null) {
            throw new IllegalStateException("Unable to resolve schema reference: " + ref);
        }

        return resolved;
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, Map<String, Schema> left, Map<String, Schema> right) {
        if (left == null || right == null) {
            return;
        }

        Set<String> leftKeys  = left.keySet();
        Set<String> rightKeys = right.keySet();
        Stream.concat(
                leftKeys.stream(),
                rightKeys.stream()
        ).distinct().forEach(key -> doVisitAndRecurse(visitor, key,
                left.get(key),
                right.get(key))
        );
    }

    private void doVisitAndRecurse(OpenApiDiffVisitor visitor, String name, Schema left, Schema right) {
        location.pushPath("Property " + name);

        try {
            visitor.acceptProperty(name, left, right);

            doVisitAndRecurse(visitor, left, right);
        } finally {
            location.popPath();
        }
    }

    private void doVisitEnumValues(OpenApiDiffVisitor visitor, List<String> leftValues, List<String> rightValues) {
        Set<String> enumValues = new HashSet<>();
        if (leftValues != null) {
            enumValues.addAll(leftValues);
        }
        if (rightValues != null) {
            enumValues.addAll(rightValues);
        }

        for (String value : enumValues) {
            visitor.acceptEnumValue(
                    leftValues != null && leftValues.contains(value) ? value : null,
                    rightValues != null && rightValues.contains(value) ? value : null
            );
        }
    }
}
