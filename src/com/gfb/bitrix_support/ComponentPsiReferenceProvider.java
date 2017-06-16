package com.gfb.bitrix_support;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ComponentPsiReferenceProvider extends PsiReferenceProvider {
    private static final Logger logger = Logger.getInstance(ComponentPsiReferenceProvider.class);

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
//        logger.warn("getReferencesByElement call");

//        Project project = psiElement.getProject();
//        PropertiesComponent properties = PropertiesComponent.getInstance(project);
//        String kohanaAppDir = properties.getValue("kohanaAppPath", "application/");

        String className = psiElement.getClass().getName();
        if (className.endsWith("StringLiteralExpressionImpl")) {
            try {
                PsiReference ref = buildComponentReference(psiElement);
                if (ref != null) {
                    return new PsiReference[]{ref};
                }
            } catch (Exception e) {
                logger.warn("Exception in getReferencesByElement - " + e.getMessage());
            }
        }

        return PsiReference.EMPTY_ARRAY;
    }

    public static PsiReference buildComponentReference(PsiElement psiElement) {
        try {
            String str = getText(psiElement);
            int colonIndex = str.indexOf(":");
            if (colonIndex < 0) {
                return null;
            }

            PsiElement paramsList = psiElement.getParent();
            if (!paramsList.getClass().getName().endsWith("ParameterListImpl")) {
                return null;
            }

            if (!paramsList.getParent().getClass().getName().endsWith("MethodReferenceImpl")) {
                return null;
            }

            PsiElement methodRef = paramsList.getParent();

            Method getNameMethod = methodRef.getClass().getMethod("getName");
            String methodName = (String) getNameMethod.invoke(methodRef);
            if (!"IncludeComponent".equals(methodName)) {
                return null;
            }

            String template = ".default";

            Method method = paramsList.getClass().getMethod("getParameters");
            PsiElement[] psiElements = (PsiElement[]) method.invoke(paramsList);

            if (!psiElements[0].getText().equals(psiElement.getText())) {
                return null;
            }

            if (psiElements.length > 1) {
                String param2Content = getText(psiElements[1]);
                if (!param2Content.isEmpty()) {
                    template = param2Content;
                }
            }

            String componentNamespace = str.substring(0, colonIndex);
            String componentName = str.substring(colonIndex + 1);

            return new ComponentPsiReference(
                    psiElement,
                    componentNamespace,
                    componentName,
                    template
            );
        } catch (Exception e) {
            logger.warn("Exception in buildComponentReference - " + e.toString());
        }
        return null;
    }

    /**
     * Get text content from string literal psiElement
     * @param psiElement - subject
     * @return result string
     */
    public static String getText(@NotNull PsiElement psiElement) {
        if (psiElement.getClass().getName().endsWith("StringLiteralExpressionImpl")) {
            try {
                Method getTextRangeMethod = psiElement.getClass().getMethod("getValueRange");
                TextRange textRange = (TextRange) getTextRangeMethod.invoke(psiElement);

                Method phpPsiElementGetText = psiElement.getClass().getMethod("getText");
                String text = (String) phpPsiElementGetText.invoke(psiElement);

                return text.substring(textRange.getStartOffset(), textRange.getEndOffset());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return "";
    }
}
