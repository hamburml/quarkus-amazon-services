package io.quarkus.amazon.dynamodb.enhanced.runtime;

import io.quarkus.amazon.common.runtime.*;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dynamodb-enhanced", phase = ConfigPhase.RUN_TIME)
public class DynamodbEnhancedConfig {

    /**
     * Enable DynamoDB service endpoint discovery.
     */
    @ConfigItem
    public boolean enableEndpointDiscovery;

    /**
     * AWS SDK client configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocSection
    public SdkConfig sdk;

    /**
     * AWS services configurations
     */
    @ConfigItem
    @ConfigDocSection
    public AwsConfig aws;

    /**
     * Sync HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public SyncHttpClientConfig syncClient;

    /**
     * Netty HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public NettyHttpClientConfig asyncClient;

    /** Apply patch for crashing tests. Reduces performance in JVM mode. */
    @ConfigItem(defaultValue = "true")
    public boolean jvmTransformation;
}
