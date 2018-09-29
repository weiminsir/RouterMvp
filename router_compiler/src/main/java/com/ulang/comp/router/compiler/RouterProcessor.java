package com.ulang.comp.router.compiler;
import android.os.Bundle;

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.ulang.comp.router.annotation.Args;
import com.ulang.comp.router.annotation.Router;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by WangQi on 2017/2/27.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.ulang.comp.router.annotation.Args",
        "com.ulang.comp.router.annotation.Router"
})
public class RouterProcessor extends AbstractProcessor {

    private static final boolean DEBUG = true;

    private static final String NULLABLE_ANNOTATION_NAME = "Nullable";

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private RoundEnvironment roundEnvironment;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }

        this.roundEnvironment = roundEnvironment;

        debug("start process with:" + set.toString());

        HashMap<String, ClassInfo> classInfos = new HashMap<>();


        Set<? extends Element> routerElements = roundEnvironment.getElementsAnnotatedWith(Router.class);
        Set<TypeElement> typeElements = ElementFilter.typesIn(routerElements);
        for (TypeElement typeElement : typeElements) {
            ClassInfo classInfo = classInfos.get(typeElement.getSimpleName().toString());
            if (classInfo == null) {
                createClassInfo(typeElement, classInfos);
            }
        }

        Set<? extends Element> argsElements = roundEnvironment.getElementsAnnotatedWith(Args.class);
        Set<VariableElement> variableElements = ElementFilter.fieldsIn(argsElements);
        for (VariableElement variableElement : variableElements) {

            Element enclosingElement = variableElement.getEnclosingElement();
            if (enclosingElement == null || !(enclosingElement instanceof TypeElement)) continue;
            ClassInfo classInfo = classInfos.get(enclosingElement.getSimpleName().toString());
            if (classInfo == null) {
                classInfo = createClassInfo((TypeElement) enclosingElement, classInfos);
            }
            classInfo.variableElements.add(variableElement);
        }

        for (Map.Entry<String, ClassInfo> entry : classInfos.entrySet()) {
            createNodeFile(entry.getValue());
        }
        return true;
    }

    private void createNodeFile(ClassInfo classInfo) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(classInfo.className)
                .addModifiers(Modifier.PUBLIC);
        builder.addSuperinterface(ParameterizedTypeName.get(ClassName.get("com.ulang.comp.router", "RouterNode"),
                ClassName.get(classInfo.packageName, classInfo.typeElement.getSimpleName().toString())));

        for (VariableElement element : classInfo.variableElements) {
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeName.get(element.asType()), element.getSimpleName().toString(), Modifier.PUBLIC);
            if (isFieldOptional(element)) {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("android.support.annotation", "Nullable")).build());
            }
            builder.addField(fieldBuilder.build());
            String s = element.getSimpleName().toString();
            FieldSpec fieldSpec = FieldSpec.builder(TypeName.get(String.class), getBundleKey(s), Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                    .initializer("$S", s)
                    .build();
            builder.addField(fieldSpec);
        }


        builder.addMethod(createConstructor(classInfo));
        if (classInfo.variableElements.size() != 0) {
            builder.addMethod(createDefaultConstructor(classInfo));
        }
        builder.addMethod(createBindMethod(classInfo))
                .addMethod(createGetBundleMethod(classInfo))
                .addMethod(createGetNameMethod(classInfo))
                .addMethod(createNeedLoginMethod(classInfo))
                .addMethod(createIsFragmentMethod(classInfo));
        TypeSpec typeSpec = builder.build();
        try {
            JavaFile.builder(classInfo.packageName, typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            debug(e.getMessage());
        }
    }

    private MethodSpec createNeedLoginMethod(ClassInfo classInfo) {
        return MethodSpec.methodBuilder("needLogin")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L", classInfo.needLogin)
                .build();
    }

    private MethodSpec createGetNameMethod(ClassInfo classInfo) {
        return MethodSpec.methodBuilder("getName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(String.class))
                .addStatement("return $S", classInfo.typeElement.getQualifiedName().toString())
                .build();
    }

    private MethodSpec createIsFragmentMethod(ClassInfo classInfo) {
        return MethodSpec.methodBuilder("isFragment")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L", isFragment(classInfo.typeElement.asType()))
                .build();
    }


    private MethodSpec createBindMethod(ClassInfo classInfo) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(TypeName.get(classInfo.typeElement.asType()), "obj").build())
                .addParameter(ParameterSpec.builder(TypeName.get(Bundle.class), "bundle").build());
        for (VariableElement element : classInfo.variableElements) {
            TypeName typeName = TypeName.get(element.asType());

            if (typeName.equals(TypeName.BOOLEAN)) {
                builder.addStatement("obj.$L = bundle.getBoolean($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.BYTE)) {
                builder.addStatement("obj.$L = bundle.getByte($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.CHAR)) {
                builder.addStatement("obj.$L = bundle.getChar($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.DOUBLE)) {
                builder.addStatement("obj.$L = bundle.getDouble($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.FLOAT)) {
                builder.addStatement("obj.$L = bundle.getFloat($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.INT)) {
                builder.addStatement("obj.$L = bundle.getInt($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.LONG)) {
                builder.addStatement("obj.$L = bundle.getLong($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.SHORT)) {
                builder.addStatement("obj.$L = bundle.getShort($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(boolean[].class))) {
                builder.addStatement("obj.$L = bundle.getBooleanArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(byte[].class))) {
                builder.addStatement("obj.$L = bundle.getByteArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(char[].class))) {
                builder.addStatement("obj.$L = bundle.getCharArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(double[].class))) {
                builder.addStatement("obj.$L = bundle.getDoubleArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(float[].class))) {
                builder.addStatement("obj.$L = bundle.getFloatArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(int[].class))) {
                builder.addStatement("obj.$L = bundle.getIntArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(long[].class))) {
                builder.addStatement("obj.$L = bundle.getLongArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(short[].class))) {
                builder.addStatement("obj.$L = bundle.getShortArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(String.class))) {
                builder.addStatement("obj.$L = bundle.getString($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.equals(TypeName.get(String[].class))) {
                builder.addStatement("obj.$L = bundle.getStringArray($L)", element.getSimpleName(), getBundleKey(element.getSimpleName().toString()));
            } else if (typeName.toString().contains("List<")) {
                ParameterizedTypeName name = ((ParameterizedTypeName) ParameterizedTypeName.get(element.asType()));
                if (name.typeArguments.get(0).equals(TypeName.get(Integer.class)) || name.typeArguments.get(0).equals(TypeName.get(String.class))) {
                    builder.addStatement("obj.$L = bundle.get$LArrayList($L)", element.getSimpleName(), getSimpleName(name.typeArguments.get(0).toString()), getBundleKey(element.getSimpleName().toString()));
                } else {
                    if (isParcelable(getTypeArgument(element))) {
                        builder.addStatement("obj.$L = ($T)(bundle.getParcelableArrayList($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                    } else if (isSerializable(element.asType())) {  // for Serializable list
                        builder.addStatement("obj.$L = ($T)(bundle.getSerializable($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                    }
                }
            } else if (typeName.toString().contains("SparseArray<")) {
                if (isParcelable(getTypeArgument(element))) {
                    builder.addStatement("obj.$L = ($T)(bundle.getSparseParcelableArray($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                }
            } else if (typeName.toString().contains("[]")) {
                ArrayType arrayType = (ArrayType) (element.asType());
                debug(arrayType.toString() + "()&87");
                debug(arrayType.getComponentType() + "()&87");
                if (isParcelable(arrayType.getComponentType())) {
                    builder.addStatement("obj.$L = ($T)(bundle.getParcelableArray($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                }
            } else {

                if (isParcelable(element.asType())) {
                    builder.addStatement("obj.$L = ($T)(bundle.getParcelable($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                } else if (isSerializable(element.asType())) {
                    builder.addStatement("obj.$L = ($T)(bundle.getSerializable($L))", element.getSimpleName(), element.asType(), getBundleKey(element.getSimpleName().toString()));
                }
            }


        }
        return builder.build();
    }

    private MethodSpec createGetBundleMethod(ClassInfo classInfo) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("wrapBundle")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(Bundle.class));
        builder.addStatement("android.os.Bundle bundle = new android.os.Bundle()");

        for (VariableElement element : classInfo.variableElements) {
            TypeName typeName = TypeName.get(element.asType());

            if (typeName.equals(TypeName.BOOLEAN)) {
                builder.addStatement("bundle.putBoolean($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.BYTE)) {
                builder.addStatement("bundle.putByte($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.CHAR)) {
                builder.addStatement("bundle.putChar($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.DOUBLE)) {
                builder.addStatement("bundle.putDouble($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.FLOAT)) {
                builder.addStatement("bundle.putFloat($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.INT)) {
                builder.addStatement("bundle.putInt($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.LONG)) {
                builder.addStatement("bundle.putLong($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.SHORT)) {
                builder.addStatement("bundle.putShort($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(String.class))) {
                builder.addStatement("bundle.putString($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(boolean[].class))) {
                builder.addStatement("bundle.putBooleanArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(byte[].class))) {
                builder.addStatement("bundle.putByteArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(char[].class))) {
                builder.addStatement("bundle.putCharArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(double[].class))) {
                builder.addStatement("bundle.putDoubleArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(float[].class))) {
                builder.addStatement("bundle.putFloatArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(int[].class))) {
                builder.addStatement("bundle.putIntArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(long[].class))) {
                builder.addStatement("bundle.putLongArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(short[].class))) {
                builder.addStatement("bundle.putShortArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.equals(TypeName.get(String[].class))) {
                builder.addStatement("bundle.putStringArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
            } else if (typeName.toString().contains("List<")) {
                ParameterizedTypeName name = ((ParameterizedTypeName) ParameterizedTypeName.get(element.asType()));
                if (name.typeArguments.get(0).equals(TypeName.get(Integer.class)) || name.typeArguments.get(0).equals(TypeName.get(String.class))) {
                    builder.addStatement("bundle.put$LArrayList($L, this.$L)", getSimpleName(name.typeArguments.get(0).toString()), getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                } else {
                    debug(element.asType().toString());
                    if (isParcelable(getTypeArgument(element))) {
                        builder.addStatement("bundle.putParcelableArrayList($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                    } else if (isSerializable(element.asType())) {
                        builder.addStatement("bundle.putSerializable($L, ($L)(this.$L))", getBundleKey(element.getSimpleName().toString()), Serializable.class, element.getSimpleName());
                    }
                }
            } else if (typeName.toString().contains("SparseArray<")) {
                if (isParcelable(getTypeArgument(element))) {
                    builder.addStatement("bundle.putSparseParcelableArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                }
            } else if (typeName.toString().contains("[]")) {
                ArrayType arrayType = (ArrayType) (element.asType());
                if (isParcelable(arrayType.getComponentType())) {
                    builder.addStatement("bundle.putParcelableArray($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                }
            } else {

                if (isParcelable(element.asType())) {
                    builder.addStatement("bundle.putParcelable($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                } else if (isSerializable(element.asType())) {
                    builder.addStatement("bundle.putSerializable($L, this.$L)", getBundleKey(element.getSimpleName().toString()), element.getSimpleName());
                }
            }

        }
        builder.addStatement("return bundle");
        return builder.build();
    }

    private boolean isParcelable(TypeMirror mirror) {
        if (mirror == null) return false;
        if (mirror.toString().equals("android.os.Parcelable")) return true;

        List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(mirror);
        for (TypeMirror typeMirror : typeMirrors) {
            if (typeMirror.toString().equals("android.os.Parcelable")) {
                return true;
            } else {
                List<? extends TypeMirror> supertypes = typeUtils.directSupertypes(typeMirror);
                if (supertypes.size() != 0) {
                    return isParcelable(typeMirror);
                }
            }
        }
        return false;
    }

    private boolean isSerializable(TypeMirror mirror) {
        if (mirror == null) return false;
        if (mirror.toString().equals("java.io.Serializable")) return true;

        List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(mirror);
        for (TypeMirror typeMirror : typeMirrors) {
            if (typeMirror.toString().equals("java.io.Serializable")) {
                return true;
            } else {
                List<? extends TypeMirror> supertypes = typeUtils.directSupertypes(typeMirror);
                if (supertypes.size() != 0) {
                    return isSerializable(typeMirror);
                }
            }
        }
        return false;
    }

    private boolean isFragment(TypeMirror mirror) {
        if (mirror == null) return false;

        if (mirror.toString().equals("android.support.v4.app.Fragment")) return true;

        List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(mirror);
        for (TypeMirror typeMirror : typeMirrors) {
            if (typeMirror.toString().equals("android.support.v4.app.Fragment")) {
                return true;
            } else {
                List<? extends TypeMirror> supertypes = typeUtils.directSupertypes(typeMirror);
                if (supertypes.size() != 0) {
                    return isFragment(typeMirror);
                }
            }
        }
        return false;
    }

    private TypeMirror getTypeArgument(Element e) {
        if (e.asType().getKind().equals(TypeKind.DECLARED)) {
            return ((DeclaredType) e.asType()).getTypeArguments().get(0);
        }
        return null;
    }

    private MethodSpec createDefaultConstructor(ClassInfo classInfo) {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
    }

    private MethodSpec createConstructor(ClassInfo classInfo) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (VariableElement element : classInfo.variableElements) {
            boolean fieldOptional = isFieldOptional(element);
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(TypeName.get(element.asType()), element.getSimpleName().toString());
            if (fieldOptional) {
                paramBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("android.support.annotation", "Nullable")).build());
            }
            ParameterSpec parameterSpec = paramBuilder.build();
            builder.addParameter(parameterSpec);
        }

        for (VariableElement element : classInfo.variableElements) {
            builder.addStatement("this.$L = $L", element.getSimpleName(), element.getSimpleName());
        }
        return builder.build();
    }

    private ClassInfo createClassInfo(TypeElement typeElement, Map<String, ClassInfo> classInfos) {
        Router router = typeElement.getAnnotation(Router.class);
        String routerName = router == null ? null : router.name();
        boolean needLogin = router != null && router.needLogin();

        String typeName = typeElement.getSimpleName().toString();
        ClassInfo classInfo = new ClassInfo();
        classInfo.needLogin = needLogin;
        classInfo.routerName = routerName;
        classInfo.typeElement = typeElement;
        classInfo.className = getClassName(typeElement, routerName);
        classInfo.packageName = getPackageName(typeElement);

        for (Map.Entry<String, ClassInfo> entry : classInfos.entrySet()) {
            if (entry.getValue().routerName != null && entry.getValue().routerName.equals(routerName)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "has same router name: " + classInfo.routerName + " in " + classInfo.typeElement.getSimpleName() + " and " + entry.getValue().typeElement.getSimpleName());
                throw new RuntimeException();
            }
        }

        classInfos.put(typeName, classInfo);

        return classInfo;
    }

    private String getBundleKey(String elementName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, elementName);
    }

    private String getPackageName(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        int i = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, i);
    }

    private String getPackageName(String qualifiedName) {
        int i = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, i);
    }

    private String getSimpleName(String qualifiedName) {
        int i = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(i + 1, qualifiedName.length());
    }

    private String getClassName(TypeElement typeElement, String routerName) {
        String simpleName = typeElement.getSimpleName().toString();
        if (routerName == null) {
            return simpleName + "Node";
        } else {
            routerName = routerName.substring(0, 1).toUpperCase() + routerName.substring(1, routerName.length());
            return routerName + "Node";
        }
    }

    private static boolean hasAnnotationWithName(Element element, String simpleName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFieldOptional(Element element) {
        return hasAnnotationWithName(element, NULLABLE_ANNOTATION_NAME);
    }

    private void debug(String msg) {
        if (DEBUG) {
            messager.printMessage(Diagnostic.Kind.NOTE, "RouterProcessor : " + msg);
        }
    }

    private class ClassInfo {
        TypeElement typeElement;
        String packageName;
        String className;

        String routerName;
        boolean needLogin;
        List<VariableElement> variableElements = new ArrayList<>();

        @Override
        public String toString() {
            return "ClassInfo{" +
                    "typeElement=" + typeElement +
                    ", routerName='" + routerName + '\'' +
                    ", needLogin=" + needLogin +
                    ", variableElements=" + variableElements +
                    '}';
        }
    }
}

