package io.quarkus.amazon.dynamodb.enhanced.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@ApplicationScoped
public class DynamodbEnhancedClientProducer {

    private final DynamoDbEnhancedClient syncClient;
    private final DynamoDbEnhancedAsyncClient asyncClient;

    DynamodbEnhancedClientProducer(Instance<DynamoDbClientBuilder> syncClientBuilderInstance,
            Instance<DynamoDbAsyncClientBuilder> asyncClientBuilderInstance) {

        DynamoDbClient syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        DynamoDbAsyncClient asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build()
                : null;

        this.syncClient = DynamoDbEnhancedClient.builder().dynamoDbClient(syncClient).build();
        this.asyncClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncClient).build();

    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The DynamoDbEnhancedClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException(
                    "The DynamoDbEnhancedAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        // TODO the enhancedClient does not have a close
        /*
         * if (syncClient != null) {
         * syncClient.close();
         * }
         * if (asyncClient != null) {
         * asyncClient.close();
         * }
         */
    }
}
