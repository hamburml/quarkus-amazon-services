package io.quarkus.it.amazon.dynamodbenhanced;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.enhanced.dynamodb.*;

@Path("/dynamodbenhanced")
public class DynamoDBEnhancedResource {
    private final static String ASYNC_TABLE = "async";
    private final static String BLOCKING_TABLE = "blocking";

    private static final Logger LOG = Logger.getLogger(DynamoDBEnhancedResource.class);

    @Inject
    DynamoDbEnhancedClient dynamoEnhancedClient;

    @Inject
    DynamoDbEnhancedAsyncClient dynamoEnhancedAsyncClient;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws InterruptedException {
        LOG.info("Testing Async Dynamodb client with table: " + ASYNC_TABLE);
        String partitionKeyAsString = UUID.randomUUID().toString();

        DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTable = dynamoEnhancedAsyncClient.table(ASYNC_TABLE,
                TableSchema.fromBean(DynamoDBExampleTableEntry.class));

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setTablePartitionKey(partitionKeyAsString);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        return exampleAsyncTable.putItem(exampleTableEntry)
                .thenCompose(t -> exampleAsyncTable.getItem(partitionKey))
                .thenApply(p -> p.getTablePartitionKey())
                .exceptionally(th -> {
                    LOG.error("Error during async Dynamodb operations", th.getCause());
                    return "ERROR";
                });
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() {
        LOG.info("Testing Blocking Dynamodb client with table: " + BLOCKING_TABLE);

        String partitionKeyAsString = UUID.randomUUID().toString();

        DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTable = dynamoEnhancedClient.table(BLOCKING_TABLE,
                TableSchema.fromBean(DynamoDBExampleTableEntry.class));

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setTablePartitionKey(partitionKeyAsString);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        exampleBlockingTable.putItem(exampleTableEntry);

        DynamoDBExampleTableEntry existingTableEntry = exampleBlockingTable.getItem(partitionKey);

        if (existingTableEntry != null) {
            return existingTableEntry.getTablePartitionKey();
        } else {
            return "ERROR";
        }
    }
}