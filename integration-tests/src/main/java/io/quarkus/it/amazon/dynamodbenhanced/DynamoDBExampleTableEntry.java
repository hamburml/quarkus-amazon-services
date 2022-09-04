package io.quarkus.it.amazon.dynamodbenhanced;

import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@RegisterForReflection
public class DynamoDBExampleTableEntry {

    private String tablePartitionKey;

    private String name;

    @DynamoDbPartitionKey
    public String getTablePartitionKey() {
        return tablePartitionKey;
    }

    public void setTablePartitionKey(String partitionKey) {
        this.tablePartitionKey = partitionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
