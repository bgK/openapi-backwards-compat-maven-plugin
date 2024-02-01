package io.kemtoa.openapi.compat;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.kemtoa.openapi.compat.walker.OpenApiDiffWalker;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.kemtoa.openapi.compat.rules.AddedEnumValueInResponseRule;
import io.kemtoa.openapi.compat.rules.AddedRequiredRequestParameterRule;
import io.kemtoa.openapi.compat.rules.CompositeRule;
import io.kemtoa.openapi.compat.rules.ParameterLocationChangedRule;
import io.kemtoa.openapi.compat.rules.PropertyRemovedInResponseRule;
import io.kemtoa.openapi.compat.rules.PropertyTypeChangedRule;
import io.kemtoa.openapi.compat.rules.RemovedEnumValueInRequestRule;
import io.kemtoa.openapi.compat.rules.RemovedOperationRule;

@RunWith(Parameterized.class)
public class SwaggerCompatibilityCheckTest {

    private static class TestCase {
        public String oldPath;
        public String newPath;
        public List<String> errors;

        public TestCase(String oldPath, String newPath, String... errors) {
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.errors = Arrays.asList(errors);
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "oldPath='" + oldPath + '\'' +
                    ", newPath='" + newPath + '\'' +
                    '}';
        }
    }

    @Parameters
    public static Collection<TestCase> data() {
        return Arrays.asList(
                new TestCase("specs/petstore.yml", "specs/petstore.yml"),
                new TestCase("specs/petstore.yml", "specs/petstore-added-parameter.yml",
                        "Path /store/order/{orderId}, Operation GET, Parameter newParam : The required parameter 'newParam' has been added in the new spec."
                ),
                new TestCase("specs/petstore.yml", "specs/petstore-removed-enum-value.yml",
                        "Path /user/createWithArray, Operation POST, RequestBody, MediaType */*, Property sex : The enum value 'UNKNOWN' has been removed in the new spec.",
                        "Path /user/{username}, Operation PUT, RequestBody, MediaType */*, Property sex : The enum value 'UNKNOWN' has been removed in the new spec.",
                        "Path /user, Operation POST, RequestBody, MediaType */*, Property sex : The enum value 'UNKNOWN' has been removed in the new spec."
                ),
                new TestCase("specs/petstore.yml", "specs/petstore-removed-operation.yml",
                        "Path /pet, Operation POST : The operation was removed in the new spec.",
                        "Path /user/createWithList : The path was removed in the new spec."
                ),
                new TestCase("specs/recursive.yml", "specs/recursive.yml"),
                new TestCase("specs/uber.yml", "specs/uber.yml"),
                new TestCase("specs/uber.yml", "specs/uber-removed-property.yml",
                        "Path /products, Operation GET, Response 200, MediaType application/json, Property items, Property image : The property 'image' has been removed in the new spec.",
                        "Path /estimates/time, Operation GET, Response 200, MediaType application/json, Property items, Property image : The property 'image' has been removed in the new spec."
                ),
                new TestCase("specs/uber.yml", "specs/uber-type-changed.yml",
                        "Path /me, Operation GET, Response 200, MediaType application/json, Property promo_code : The type of property 'promo_code' has changed in the new spec: 'integer' was previously 'string'."
                ),
                new TestCase("specs/uber.yml", "specs/uber-parameter-location-changed.yml",
                        "Path /products, Operation GET, Parameter latitude : The location of parameter 'latitude' has changed in the new spec: 'header' previously was 'query'."
                ),
                new TestCase("specs/uber.yml", "specs/uber-added-enum-value.yml",
                        "Path /me, Operation GET, Response 200, MediaType application/json, Property status : The enum value 'IN_BETWEEN' has been added in the new spec."
                )
        );
    }

    @Parameter
    public TestCase testCase;

    @Test
    public void test() {
        OpenAPI openApiLeft = new OpenAPIV3Parser().read(testCase.oldPath);
        OpenAPI openApiRight = new OpenAPIV3Parser().read(testCase.newPath);

        CompositeRule rules = new CompositeRule(
                new PropertyRemovedInResponseRule(),
                new PropertyTypeChangedRule(),
                new RemovedEnumValueInRequestRule(),
                new AddedEnumValueInResponseRule(),
                new AddedRequiredRequestParameterRule(),
                new ParameterLocationChangedRule(),
                new RemovedOperationRule()
        );

        OpenApiDiffWalker walker = new OpenApiDiffWalker();
        walker.walk(rules, openApiLeft, openApiRight);

        assertEquals(testCase.errors.size(), rules.getErrors().size());

        for (String error : testCase.errors) {
            assertThat(rules.getErrors(), hasItem(error));
        }
    }
}
