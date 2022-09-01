package io.quarkus.amazon.dynamodb.enhanced.runtime;

import io.quarkus.amazon.common.runtime.*;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Recorder
public class DynamodbRecorder {

    final DynamodbEnhancedConfig config;

    public DynamodbRecorder(DynamodbEnhancedConfig config) {
        this.config = config;
    }

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig() {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<DynamoDbEnhancedClient.Builder> createSyncBuilder(RuntimeValue<SdkHttpClient.Builder> transport) {

        DynamoDbEnhancedClient.Builder builder = DynamoDbEnhancedClient.builder();

        DynamoDbClientBuilder builderDynamoDb = DynamoDbClient.builder();
        builderDynamoDb.endpointDiscoveryEnabled(config.enableEndpointDiscovery);

        if (transport != null) {
            builderDynamoDb.httpClientBuilder(transport.getValue());
        }

        builder.dynamoDbClient(builderDynamoDb.build());

        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<DynamoDbEnhancedAsyncClient.Builder> createAsyncBuilder(
            RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        DynamoDbEnhancedAsyncClient.Builder builderAsync = DynamoDbEnhancedAsyncClient.builder();

        DynamoDbAsyncClientBuilder builderAsyncDynamoDb = DynamoDbAsyncClient.builder();
        builderAsyncDynamoDb.endpointDiscoveryEnabled(config.enableEndpointDiscovery);

        if (transport != null) {
            builderAsyncDynamoDb.httpClientBuilder(transport.getValue());
        }
        if (!config.asyncClient.advanced.useFutureCompletionThreadPool) {
            builderAsyncDynamoDb.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run));
        }
        builderAsync.dynamoDbClient(builderAsyncDynamoDb.build());

        return new RuntimeValue<>(builderAsync);
    }
}
