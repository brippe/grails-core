/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.compiler.web;

import grails.artefact.Artefact;
import grails.web.controllers.ControllerMethod;
import groovy.lang.Closure;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AnnotatedClassInjector;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi;

/**
 * Adds the withFormat and other mime related methods to controllers at compile time.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class MimeTypesTransformer implements GrailsArtefactClassInjector, AnnotatedClassInjector {

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
              GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    public static final String FIELD_MIME_TYPES_API = "mimeTypesApi";
    public static final Parameter[] CLOSURE_PARAMETER = new Parameter[]{ new Parameter(new ClassNode(Closure.class), "callable")};
    public static final String WITH_FORMAT_METHOD = "withFormat";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        // don't inject if already an @Artefact annotation is applied
        if(!classNode.getAnnotations(new ClassNode(Artefact.class)).isEmpty()) return;

        performInjectionOnAnnotatedClass(source, context,classNode);
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source,null, classNode);
    }

    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, null,classNode);
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }

    public String[] getArtefactTypes() {
        return new String[]{ControllerArtefactHandler.TYPE};
    }
    
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }    

    public void performInjectionOnAnnotatedClass(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if(classNode instanceof InnerClassNode) return;
        if(classNode.isEnum()) return;
        FieldNode field = classNode.getField(FIELD_MIME_TYPES_API);
        if (field == null) {
            final ClassNode mimeTypesApiClass = new ClassNode(ControllersMimeTypesApi.class);
            field = new FieldNode(FIELD_MIME_TYPES_API, PRIVATE_STATIC_MODIFIER, mimeTypesApiClass, classNode, new ConstructorCallExpression(mimeTypesApiClass, GrailsArtefactClassInjector.ZERO_ARGS));

            classNode.addField(field);

            if(!classNode.hasMethod(WITH_FORMAT_METHOD, CLOSURE_PARAMETER)) {
                final BlockStatement methodBody = new BlockStatement();
                final ArgumentListExpression args = new ArgumentListExpression();
                args.addExpression(new VariableExpression("this", classNode))
                    .addExpression(new VariableExpression("callable", new ClassNode(Closure.class)));
                MethodCallExpression methodCall = new MethodCallExpression(new AttributeExpression(new VariableExpression("this"), new ConstantExpression(FIELD_MIME_TYPES_API)),  WITH_FORMAT_METHOD, args);
                methodCall.setMethodTarget(mimeTypesApiClass.getMethods(WITH_FORMAT_METHOD).get(0));
                methodBody.addStatement(new ReturnStatement(methodCall));
                MethodNode methodNode = new MethodNode(WITH_FORMAT_METHOD, Modifier.PUBLIC, new ClassNode(Object.class), CLOSURE_PARAMETER, null, methodBody);
                methodNode.addAnnotation(getMarkerAnnotation());
                classNode.addMethod(methodNode);
                GrailsASTUtils.addCompileStaticAnnotation(methodNode);
            }
        }
    }
}
