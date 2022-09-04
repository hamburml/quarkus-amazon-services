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

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    DynamodbEnhancedClientProducer(Instance<DynamoDbClientBuilder> syncClientBuilderInstance,
            Instance<DynamoDbAsyncClientBuilder> asyncClientBuilderInstance) {

        this.dynamoDbClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.dynamoDbAsyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build()
                : null;

        this.dynamoDbEnhancedClient = this.dynamoDbClient != null
                ? DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build()
                : null;
        this.dynamoDbEnhancedAsyncClient = this.dynamoDbAsyncClient != null
                ? DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build()
                : null;

    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient client() {
        if (dynamoDbClient == null) {
            throw new IllegalStateException("The DynamoDbClient is required but has not been detected/configured.");
        }
        return dynamoDbClient;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient clientAsync() {
        if (dynamoDbAsyncClient == null) {
            throw new IllegalStateException(
                    "The DynamoDbAsyncClient is required but has not been detected/configured.");
        }
        return dynamoDbAsyncClient;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient enhancedClient() {
        if (dynamoDbEnhancedClient == null) {
            throw new IllegalStateException("The DynamoDbEnhancedClient is required but has not been detected/configured.");
        }
        return dynamoDbEnhancedClient;
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedAsyncClient asyncEnhancedClient() {
        if (dynamoDbEnhancedAsyncClient == null) {
            throw new IllegalStateException(
                    "The DynamoDbEnhancedAsyncClient is required but has not been detected/configured.");
        }
        return dynamoDbEnhancedAsyncClient;
    }

    @PreDestroy
    public void destroy() {

        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
        if (dynamoDbAsyncClient != null) {
            dynamoDbAsyncClient.close();
        }

    }
}
