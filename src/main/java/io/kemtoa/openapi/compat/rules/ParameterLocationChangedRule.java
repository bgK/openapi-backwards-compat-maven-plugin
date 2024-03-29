package io.kemtoa.openapi.compat.rules;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;

/**
 * Changing the location of a request parameter is a backwards incompatible
 * change.
 *
 * For instance, changing a parameter from being in a 'header' to
 * being in the 'query' causes this rule to fail.
 */
public class ParameterLocationChangedRule extends Rule {

    @Override
    public void acceptParameter(Parameter left, Parameter right) {
        if (left == null || right == null) {
            return; // Handled in other rules
        }

        if (!StringUtils.equals(left.getIn(), right.getIn())) {
            addError("The location of parameter '" + left.getName() + "' has changed in the new spec: '"
                    + right.getIn() + "' previously was '" + left.getIn() + "'.");
        }
    }
}
