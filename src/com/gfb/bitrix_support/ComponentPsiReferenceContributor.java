package com.gfb.bitrix_support;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Sergey on 27.08.2015.
 */
public class ComponentPsiReferenceContributor extends PsiReferenceContributor {
//    private static final Logger logger = Logger.getInstance(BitrixModulePsiReferenceContributor.class);
    private static final Logger logger = Logger.getInstance(ComponentPsiReferenceContributor.class);

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        logger.warn("Providers registered");

        ComponentPsiReferenceProvider provider = new ComponentPsiReferenceProvider();
        psiReferenceRegistrar.registerReferenceProvider(StandardPatterns.instanceOf(PsiElement.class), provider);
    }
}