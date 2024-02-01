package io.kemtoa.openapi.compat.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.kemtoa.openapi.compat.walker.Location;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Composite {@link Rule}
 *
 * Delegates all operations to a set of specified Rules. Using this class
 * allows to process multiple rules while walking the Swagger documents
 * only once.
 */
public class CompositeRule extends Rule {
    private final List<Rule> rules;

    public CompositeRule(Rule... rules) {
        this.rules = Arrays.asList(rules);
    }

    @Override
    public List<String> getErrors() {
        List<String> errors = new ArrayList<>();

        rules.forEach(v -> errors.addAll(v.getErrors()));

        return errors;
    }

    @Override
    public void setLocation(Location location) {
        rules.forEach(v -> v.setLocation(location));
    }

    @Override
    public void acceptPath(String key, PathItem left, PathItem right) {
        rules.forEach(v -> v.acceptPath(key, left, right));
    }

    @Override
    public void acceptOperation(HttpMethod operationKey, Operation left, Operation right) {
        rules.forEach(v -> v.acceptOperation(operationKey, left, right));
    }

    @Override
    public void acceptParameter(Parameter left, Parameter right) {
        rules.forEach(v -> v.acceptParameter(left, right));
    }

    @Override
    public void acceptRequestBody(RequestBody left, RequestBody right) {
        rules.forEach(v -> v.acceptRequestBody(left, right));
    }

    @Override
    public void acceptResponse(String key, ApiResponse left, ApiResponse right) {
        rules.forEach(v -> v.acceptResponse(key, left, right));
    }

    @Override
    public void acceptMediaType(String key, MediaType left, MediaType right) {
        rules.forEach(v -> v.acceptMediaType(key, left, right));
    }

    @Override
    public void acceptSchema(Schema left, Schema right) {
        rules.forEach(v -> v.acceptSchema(left, right));
    }

    @Override
    public void acceptProperty(String key, Schema left, Schema right) {
        rules.forEach(v -> v.acceptProperty(key, left, right));
    }

    @Override
    public void acceptEnumValue(String left, String right) {
        rules.forEach(v -> v.acceptEnumValue(left, right));
    }
}
