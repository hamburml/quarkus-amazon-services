package io.quarkus.amazon.dynamodb.enhanced.deployment;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.jandex.DotName;
import org.objectweb.asm.*;

import io.quarkus.amazon.common.deployment.*;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.dynamodb.enhanced.BeanTableSchemaSubstitutionImplementation;
import io.quarkus.amazon.dynamodb.enhanced.runtime.DynamodbEnhancedBuildTimeConfig;
import io.quarkus.amazon.dynamodb.enhanced.runtime.DynamodbEnhancedClientProducer;
import io.quarkus.amazon.dynamodb.enhanced.runtime.DynamodbRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.runtime.LaunchMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

public class DynamodbEnhancedProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_DYNAMODB_ENHANCED = "amazon-dynamodb-enhanced";

    DynamodbEnhancedBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_DYNAMODB_ENHANCED;
    }

    @Override
    protected String configName() {
        return "dynamodb-enhanced";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(DynamoDbEnhancedClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(DynamoDbEnhancedAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/dynamodb/execution.interceptors";
    }

    public static final String CLASS_NAME_BEAN_TABLE_SCHEMA = "software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema";

    @BuildStep
    void maybeApplyClassTransformation(
            DynamodbEnhancedBuildTimeConfig buildTimeConfig,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            LaunchModeBuildItem launchModeBuildItem) {
        if (shouldApplyClassTransformation(buildTimeConfig, launchModeBuildItem)) {
            applyClassTransformation(transformers);
        }
    }

    private boolean shouldApplyClassTransformation(
            DynamodbEnhancedBuildTimeConfig buildTimeConfig, LaunchModeBuildItem launchModeBuildItem) {
        return buildTimeConfig.jvmTransformation && launchModeBuildItem.getLaunchMode() != LaunchMode.NORMAL;
    }

    private void applyClassTransformation(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(
                new BytecodeTransformerBuildItem(
                        CLASS_NAME_BEAN_TABLE_SCHEMA, new DynamodbEnhancedProcessor.MethodCallRedirectionVisitor()));
    }

    private static class MethodCallRedirectionVisitor
            implements BiFunction<String, ClassVisitor, ClassVisitor> {

        public static final String TARGET_METHOD_OWNER = BeanTableSchemaSubstitutionImplementation.class.getName().replace('.',
                '/');
        // "org/acme/aws/dynamodb/fix/runtime/SubstitutionImplementation";

        @Override
        public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, outputClassVisitor) {
                /**
                 * This effectively does the same as {@link com.oracle.svm.core.annotate.Substitute} for
                 * native-image but a lot less automated. Replaces the method bodies with a redirect to
                 * their matching substitution counterpart.
                 */
                @Override
                public MethodVisitor visitMethod(
                        int access, String name, String descriptor, String signature, String[] exceptions) {
                    // https://stackoverflow.com/questions/45180625/how-to-remove-method-body-at-runtime-with-asm-5-2
                    MethodVisitor originalMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("newObjectSupplierForClass")) {
                        return new DynamodbEnhancedProcessor.ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else if (name.equals("getterForProperty")) {
                        return new DynamodbEnhancedProcessor.ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else if (name.equals("setterForProperty")) {
                        return new DynamodbEnhancedProcessor.ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else {
                        return originalMethodVisitor;
                    }
                }

                private int getMaxLocals(String descriptor) {
                    return (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1;
                }
            };
        }
    }

    private static class ReplaceMethodBody extends MethodVisitor {
        private final MethodVisitor targetWriter;
        private final int newMaxLocals;
        private final Consumer<MethodVisitor> code;

        public ReplaceMethodBody(
                MethodVisitor writer, int newMaxL, Consumer<MethodVisitor> methodCode) {
            super(Opcodes.ASM5);
            this.targetWriter = writer;
            this.newMaxLocals = newMaxL;
            this.code = methodCode;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetWriter.visitMaxs(0, newMaxLocals);
        }

        @Override
        public void visitCode() {
            code.accept(targetWriter);
        }

        @Override
        public void visitEnd() {
            targetWriter.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetWriter.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetWriter.visitParameter(name, access);
        }
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(DynamodbEnhancedClientProducer.class);
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // This class triggers initialization of FullJitterBackoffStragegy so needs to get runtime-initialized
        // as well
        producer.produce(
                new RuntimeInitializedClassBuildItem("software.amazon.awssdk.services.dynamodb.DynamoDbRetryPolicy"));
    }

    @BuildStep
    void setup(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupExtension(beanRegistrationPhase, extensionSslNativeSupport, feature, interceptors, clientProducer,
                buildTimeConfig.sdk, buildTimeConfig.syncClient);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                recorder.getAsyncConfig(),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(DynamodbRecorder recorder,
            AmazonClientRecorder commonRecorder,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        createClientBuilders(commonRecorder,
                recorder.getAwsConfig(),
                recorder.getSdkConfig(),
                buildTimeConfig.sdk,
                syncTransports,
                asyncTransports,
                DynamoDbClientBuilder.class,
                (syncTransport) -> recorder.createSyncBuilder(syncTransport),
                DynamoDbAsyncClientBuilder.class,
                (asyncTransport) -> recorder.createAsyncBuilder(asyncTransport),
                null,
                null,
                syntheticBeans);

    }
}
