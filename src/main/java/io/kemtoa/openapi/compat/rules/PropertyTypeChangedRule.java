package io.kemtoa.openapi.compat.rules;

import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

/**
 * Changing the type of a field is not backward compatible as a client using
 * 'old' Swagger specs will send the field with a different type leading the
 * service to fail to validate the request. On the other end, if the object
 * containing the updated field is used in the response, it will lead to
 * unexpected client errors when parsing the response and/or using the
 * updated property.
 */
public class PropertyTypeChangedRule extends Rule {

    @Override
    public void acceptProperty(String key, Schema left, Schema right) {
        if (left == null || right == null) {
            return; // Handled in other rules
        }

        if (!StringUtils.equals(left.getType(), right.getType())) {
            addError("The type of property '" + key + "' has changed in the new spec: '"
                    + right.getType() + "' was previously '" + left.getType() + "'.");
        }

        if (!StringUtils.equals(left.getFormat(), right.getFormat())) {
            addError("The format of property '" + key + "' has changed in the new spec: '"
                    + right.getFormat() + "' was previously '" + left.getFormat() + "'.");
        }
    }
}
