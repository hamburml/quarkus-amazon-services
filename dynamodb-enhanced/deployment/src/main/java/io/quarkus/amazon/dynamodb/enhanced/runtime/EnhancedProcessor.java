package io.quarkus.amazon.dynamodb.enhanced.runtime;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.runtime.LaunchMode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

public class EnhancedProcessor {
    private static final String FEATURE = "amazon-dynamodb-enhanced";
    public static final String CLASS_NAME_BEAN_TABLE_SCHEMA = "software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(DynamodbEnhancedClientProducer.class);
    }

    @BuildStep
    public void registerClassesForReflectiveAccess(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (AnnotationInstance i : combinedIndexBuildItem
                .getIndex()
                .getAnnotations(DotName.createSimple(DynamoDbBean.class.getName()))) {
            ClassInfo classInfo = i.target().asClass();
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, classInfo.name().toString()));
        }

        // When we add DefaultAttributeConverterProvider and BeanTableSchemaAttributeTags here, we do not need the DynamodbEnhancedFeature in runtime module.
        //reflectiveClass.produce(
        //        new ReflectiveClassBuildItem(true, false,
        //                "software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider"));
        //reflectiveClass.produce(
        //        new ReflectiveClassBuildItem(true, false,
        //                "software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags"));

    }

    @BuildStep
    void maybeApplyClassTransformation(
            BuildTimeConfig buildTimeConfig,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            LaunchModeBuildItem launchModeBuildItem) {
        if (shouldApplyClassTransformation(buildTimeConfig, launchModeBuildItem)) {
            applyClassTransformation(transformers);
        }
    }

    private void applyClassTransformation(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(
                new BytecodeTransformerBuildItem(
                        CLASS_NAME_BEAN_TABLE_SCHEMA, new MethodCallRedirectionVisitor()));
    }

    /**
     * Decides if the transformation should be applied. Predominantly this should only happen in
     * non-production builds because only a single {@link ClassLoader} is used there making this "fix"
     * useless and probably worsens performance. Second, we do not apply the transformation when
     * requested through config.
     */
    private boolean shouldApplyClassTransformation(
            BuildTimeConfig buildTimeConfig, LaunchModeBuildItem launchModeBuildItem) {

        return buildTimeConfig.jvmTransformation
                && launchModeBuildItem.getLaunchMode() != LaunchMode.NORMAL;
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
                        return new ReplaceMethodBody(
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
                        return new ReplaceMethodBody(
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
                        return new ReplaceMethodBody(
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
}
