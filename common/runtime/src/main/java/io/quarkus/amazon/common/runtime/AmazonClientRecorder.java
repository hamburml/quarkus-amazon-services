package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.utils.StringUtils;

@Recorder
public class AmazonClientRecorder {
    private static final Log LOG = LogFactory.getLog(AmazonClientRecorder.class);

    public RuntimeValue<AwsClientBuilder> configure(RuntimeValue<? extends AwsClientBuilder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig, SdkBuildTimeConfig sdkBuildTimeConfig,
            String awsServiceName) {
        AwsClientBuilder builder = clientBuilder.getValue();

        initAwsClient(builder, awsServiceName, awsConfig.getValue());
        initSdkClient(builder, awsServiceName, sdkConfig.getValue(), sdkBuildTimeConfig);

        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<DynamoDbEnhancedClient.Builder> configure2(
            RuntimeValue<? extends DynamoDbEnhancedClient.Builder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig, SdkBuildTimeConfig sdkBuildTimeConfig,
            String awsServiceName) {
        DynamoDbEnhancedClient.Builder builder = clientBuilder.getValue();

        initAwsClient2(builder, awsServiceName, awsConfig.getValue());
        initSdkClient2(builder, awsServiceName, sdkConfig.getValue(), sdkBuildTimeConfig);

        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<DynamoDbEnhancedAsyncClient.Builder> configure3(
            RuntimeValue<? extends DynamoDbEnhancedAsyncClient.Builder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig, SdkBuildTimeConfig sdkBuildTimeConfig,
            String awsServiceName) {
        DynamoDbEnhancedAsyncClient.Builder builder = clientBuilder.getValue();

        initAwsClient3(builder, awsServiceName, awsConfig.getValue());
        initSdkClient3(builder, awsServiceName, sdkConfig.getValue(), sdkBuildTimeConfig);

        return new RuntimeValue<>(builder);
    }

    public void initAwsClient(AwsClientBuilder builder, String extension, AwsConfig config) {
        config.region.ifPresent(builder::region);

        builder.credentialsProvider(config.credentials.type.create(config.credentials, "quarkus." + extension));
    }

    public void initAwsClient2(DynamoDbEnhancedClient.Builder builder, String extension, AwsConfig config) {
        //config.region.ifPresent(builder::region); // that does not exist in this builder...

        //builder.credentialsProvider(config.credentials.type.create(config.credentials, "quarkus." + extension));
    }

    public void initAwsClient3(DynamoDbEnhancedAsyncClient.Builder builder, String extension, AwsConfig config) {
        //config.region.ifPresent(builder::region); // that does not exist in this builder...

        //builder.credentialsProvider(config.credentials.type.create(config.credentials, "quarkus." + extension));
    }

    public void initSdkClient(SdkClientBuilder builder, String extension, SdkConfig config, SdkBuildTimeConfig buildConfig) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);

        final ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
        config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
        config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);

        buildConfig.interceptors.orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .map(this::createInterceptor)
                .filter(Objects::nonNull)
                .forEach(overrides::addExecutionInterceptor);
        builder.overrideConfiguration(overrides.build());
    }

    public void initSdkClient2(DynamoDbEnhancedClient.Builder builder, String extension, SdkConfig config,
            SdkBuildTimeConfig buildConfig) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }
        /*
         * config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);
         *
         * final ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
         * config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
         * config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);
         *
         * buildConfig.interceptors.orElse(Collections.emptyList()).stream()
         * .map(String::trim)
         * .map(this::createInterceptor)
         * .filter(Objects::nonNull)
         * .forEach(overrides::addExecutionInterceptor);
         * builder.overrideConfiguration(overrides.build());
         */

    }

    public void initSdkClient3(DynamoDbEnhancedAsyncClient.Builder builder, String extension, SdkConfig config,
            SdkBuildTimeConfig buildConfig) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        /*
         * config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);
         *
         * final ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
         * config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
         * config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);
         *
         * buildConfig.interceptors.orElse(Collections.emptyList()).stream()
         * .map(String::trim)
         * .map(this::createInterceptor)
         * .filter(Objects::nonNull)
         * .forEach(overrides::addExecutionInterceptor);
         * builder.overrideConfiguration(overrides.build());
         *
         */
    }

    public RuntimeValue<SdkPresigner.Builder> configurePresigner(
            RuntimeValue<? extends SdkPresigner.Builder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig,
            String awsServiceName) {
        SdkPresigner.Builder builder = clientBuilder.getValue();

        initAwsPresigner(builder, awsServiceName, awsConfig.getValue());
        initSdkPresigner(builder, awsServiceName, sdkConfig.getValue());

        return new RuntimeValue<>(builder);
    }

    public void initAwsPresigner(SdkPresigner.Builder builder, String extension, AwsConfig config) {
        config.region.ifPresent(builder::region);

        builder.credentialsProvider(config.credentials.type.create(config.credentials, "quarkus." + extension));
    }

    public void initSdkPresigner(SdkPresigner.Builder builder, String extension, SdkConfig config) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);
    }

    private ExecutionInterceptor createInterceptor(String interceptorClassName) {
        try {
            return (ExecutionInterceptor) Class
                    .forName(interceptorClassName, false, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create interceptor " + interceptorClassName, e);
            return null;
        }
    }
}
