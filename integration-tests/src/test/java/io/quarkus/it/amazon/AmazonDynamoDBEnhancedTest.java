package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonDynamoDBEnhancedTest {

    private static final Logger LOG = Logger.getLogger(AmazonDynamoDBEnhancedTest.class);

    @Test
    public void testDynamoDbAsync() {
        RestAssured.when().get("/test/dynamodbenhanced/async").then().body(is("INTERCEPTED OK"));
    }

    @Test
    public void testDynamoDbBlocking() {
        RestAssured.when().get("/test/dynamodbenhanced/blocking").then().body(is("INTERCEPTED OK"));
    }
}
