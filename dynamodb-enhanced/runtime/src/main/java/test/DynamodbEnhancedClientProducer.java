package test;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.Arc;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ApplicationScoped
public class DynamodbEnhancedClientProducer {

    private final DynamoDbEnhancedClient syncEnhancedClient;

    private final DynamoDbEnhancedAsyncClient asyncEnhancedClient;

    DynamodbEnhancedClientProducer() {
        this.syncEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(Arc.container().instance(DynamoDbClient.class).get()).build();
        this.asyncEnhancedClient = DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(Arc.container().instance(DynamoDbAsyncClient.class).get()).build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient client() {
        if (syncEnhancedClient == null) {
            throw new IllegalStateException("The DynamoDbEnhancedClient is required but has not been detected/configured.");
        }
        return syncEnhancedClient;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedAsyncClient asyncClient() {
        if (asyncEnhancedClient == null) {
            throw new IllegalStateException(
                    "The DynamoDbEnhancedAsyncClient is required but has not been detected/configured.");
        }
        return asyncEnhancedClient;
    }

    @PreDestroy
    public void destroy() {

    }
}
