package io.quarkus.amazon.dynamodb.enhanced.runtime;

import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;

@AutomaticFeature
public class DynamodbEnhancedFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        try {
            RuntimeReflection.register(DefaultAttributeConverterProvider.class.getConstructor());
            RuntimeReflection.register(BeanTableSchemaAttributeTags.class);
            RuntimeReflection.register(BeanTableSchemaAttributeTags.class.getMethods());
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(
                    "SVM Substitution: Unable to register method for reflection", ex);
        }
    }

}
