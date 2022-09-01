package io.quarkus.amazon.dynamodb.enhanced.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@ApplicationScoped
public class DynamodbEnhancedClientProducer {

    private final DynamoDbEnhancedClient syncClient;
    private final DynamoDbEnhancedAsyncClient asyncClient;

    DynamodbEnhancedClientProducer(Instance<DynamoDbEnhancedClient.Builder> syncClientBuilderInstance,
            Instance<DynamoDbEnhancedAsyncClient.Builder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        //this.syncClient = DynamoDbEnhancedClient.create(); // TODO maybe also possible?
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
        //this.asyncClient = DynamoDbEnhancedAsyncClient.create(); // TODO maybe also possible?
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
