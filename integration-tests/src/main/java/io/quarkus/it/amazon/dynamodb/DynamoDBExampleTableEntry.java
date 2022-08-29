package io.quarkus.it.amazon.dynamodb;

import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@RegisterForReflection
public class DynamoDBExampleTableEntry {

    private String tablePartitionKey;

    @DynamoDbPartitionKey
    public String getTablePartitionKey() {
        return tablePartitionKey;
    }

    public void setTablePartitionKey(String partitionKey) {
        this.tablePartitionKey = partitionKey;
    }
}
