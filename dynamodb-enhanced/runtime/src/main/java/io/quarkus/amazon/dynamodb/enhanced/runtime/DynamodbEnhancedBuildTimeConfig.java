package io.quarkus.amazon.dynamodb.enhanced.runtime;

import io.quarkus.amazon.common.runtime.*;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dynamodb-enhanced", phase = ConfigPhase.RUN_TIME)
public class DynamodbEnhancedBuildTimeConfig {

    /**
     * SDK client configurations for AWS Dynamodb client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon Dynamodb client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
