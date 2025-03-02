package io.quarkiverse.it.amazon.dynamodb.enhanced;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;

@Path("/dynamodbenhanced")
public class DynamoDbEnhancedResource extends DynamoDbEnhancedAbstractResource {

    private final static String ASYNC_TABLE = "enhancedasync";
    private final static String BLOCKING_TABLE = "enhancedblocking";

    @Inject
    DynamoDbEnhancedClient dynamoEnhancedClient;

    @Inject
    DynamoDbEnhancedAsyncClient dynamoEnhancedAsyncClient;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws IllegalAccessException, InterruptedException {

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(DynamoDBExampleTableEntry.class, MethodHandles.lookup());

        var beanParams = BeanTableSchemaParams.builder(DynamoDBExampleTableEntry.class)
                .lookup(lookup)
                .build();

        // when quarkus.dynamodbenhanced.create-table-schemas is true (default), TableSchema are cached at startup
        DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTableFromClient = dynamoEnhancedAsyncClient.table(ASYNC_TABLE,
                TableSchema
                        .fromBean(beanParams));
        return testAsyncDynamo(exampleAsyncTableFromClient);
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() throws IllegalAccessException {

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(DynamoDBExampleTableEntry.class, MethodHandles.lookup());

        var beanParams = BeanTableSchemaParams.builder(DynamoDBExampleTableEntry.class)
                .lookup(lookup)
                .build();

        DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTableFromClient = dynamoEnhancedClient.table(BLOCKING_TABLE,
                TableSchema
                        .fromBean(beanParams));
        return testBlockingDynamo(exampleBlockingTableFromClient);
    }
}
