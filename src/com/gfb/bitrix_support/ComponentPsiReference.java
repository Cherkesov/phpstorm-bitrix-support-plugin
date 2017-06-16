package com.gfb.bitrix_support;

import com.intellij.codeInsight.completion.CompletionProcess;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.FilteringProcessor;
import com.jetbrains.php.lang.psi.resolve.PhpScopeProcessor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class ComponentPsiReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
    private static final Logger logger = Logger.getInstance(ComponentPsiReference.class);

    protected String namespace;
    protected String name;
    protected String template;

    public ComponentPsiReference(
            PsiElement element,
            String namespace, String name, String template
    ) {
        super(element);
        this.namespace = namespace;
        this.name = name;
        this.template = template;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        results.addAll(multiResolveFromSiteTemplates(b));
        results.addAll(multiResolveFromSystemComponents(b));

        // Remove repeated elements
        results = new ArrayList<ResolveResult>(new LinkedHashSet<ResolveResult>(results));

        return results.toArray(new ResolveResult[results.size()]);
    }

    /**
     * Try to find files in site templates
     *
     * @param b ???
     * @return references list
     */
    @NotNull
    private List<ResolveResult> multiResolveFromSiteTemplates(boolean b) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();
        Project project = myElement.getProject();

        String regPath = "%site_template%/components/%namespace%/%name%/%template%/";

        String[] siteTemplatesComponentsFiles = new String[]{
                "template.php",
                "style.css",
                "script.js",
        };

        for (VirtualFile templateDir : getSiteTemplates()) {
            String relSiteTemplatePath = templateDir.toString().replace(
                    project.getBaseDir().toString() + "/", ""
            );

            String path = regPath;
            path = path.replace("%site_template%", relSiteTemplatePath);
            path = path.replace("%namespace%", this.namespace);
            path = path.replace("%name%", this.name);
            path = path.replace("%template%", this.template);

            VirtualFile componentDir = project.getBaseDir().findFileByRelativePath(path);
            if (null != componentDir) {
                results.addAll(
                        getComponentVirtualFiles(componentDir, siteTemplatesComponentsFiles)
                );
            }
        }

        return results;
    }

    /**
     * Try to find files in system directories
     *
     * @param b ???
     * @return references list
     */
    @NotNull
    private List<ResolveResult> multiResolveFromSystemComponents(boolean b) {
        Project project = myElement.getProject();
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        String[] paths = new String[]{
                "local/components/%namespace%/%name%/templates/",
                "bitrix/components/%namespace%/%name%/templates/",
        };

        String[] componentFiles = new String[]{
                this.template + "/template.php",
                this.template + "/script.js",
                this.template + "/style.css",
                "../component.php",
        };

        for (VirtualFile templateDir : getSiteTemplates()) {
            String tempRelDir = templateDir.toString().replace(
                    project.getBaseDir().toString() + "/", ""
            );

            for (String pathTemplate : paths) {
                String path = pathTemplate;

                path = path.replace("%site_template%", tempRelDir);
                path = path.replace("%namespace%", this.namespace);
                path = path.replace("%name%", this.name);

                VirtualFile componentDir = project.getBaseDir().findFileByRelativePath(path);
                if (null != componentDir) {
                    results.addAll(
                            getComponentVirtualFiles(componentDir, componentFiles)
                    );
                }
            }
        }

        return results;
    }

    /**
     * Check component dir and try to find component or component template files
     *
     * @param componentDir   - is target dir
     * @param componentFiles - files names list
     * @return list of virtual files
     */
    private List<ResolveResult> getComponentVirtualFiles(VirtualFile componentDir, String[] componentFiles) {
        Project project = myElement.getProject();
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        for (String compFileName : componentFiles) {
            VirtualFile templateFile = componentDir.findFileByRelativePath(compFileName);
            if (templateFile == null) {
                continue;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(templateFile);
            if (psiFile == null) {
                continue;
            }
            PsiElementResolveResult result = new PsiElementResolveResult(psiFile);
            if (!results.contains(result)) {
                results.add(result);
            }
        }

        return results;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        final CompletionProcess process = CompletionService.getCompletionService().getCurrentCompletion();
        if (process != null && process.isAutopopupCompletion() && isSoft()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        /*final String s = this.name;
        if (s != null && s.equals("/")) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }*/

        /*final CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector = new CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem>();
        final PsiElementProcessor<PsiFileSystemItem> processor = new PsiElementProcessor<PsiFileSystemItem>() {
            public boolean execute(PsiFileSystemItem fileSystemItem) {
                return new FilteringProcessor<PsiFileSystemItem>(myFileReferenceSet.getReferenceCompletionFilter(), collector).process(getOriginalFile(fileSystemItem));
            }
        };
        for (PsiFileSystemItem context : getContexts()) {
            for (final PsiElement child : context.getChildren()) {
                if (child instanceof PsiFileSystemItem) {
                    processor.execute((PsiFileSystemItem) child);
                }
            }
        }
        final THashSet<PsiElement> set = new THashSet<PsiElement>(collector.getResults(), VARIANTS_HASHING_STRATEGY);
        final PsiElement[] candidates = set.toArray(new PsiElement[set.size()]);

        final Object[] variants = new Object[candidates.length];
        for (int i = 0; i < candidates.length; i++) {
            variants[i] = createLookupItem(candidates[i]);
        }
        if (!myFileReferenceSet.isUrlEncoded()) {
            return variants;
        }
        List<Object> encodedVariants = new ArrayList<Object>(variants.length);
        for (int i = 0; i < candidates.length; i++) {
            final PsiElement element = candidates[i];
            if (element instanceof PsiNamedElement) {
                final PsiNamedElement psiElement = (PsiNamedElement) element;
                String name = psiElement.getName();
                final String encoded = encode(name, psiElement);
                if (encoded == null) continue;
                if (!encoded.equals(name)) {
                    final Icon icon = psiElement.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
                    LookupElementBuilder item = FileInfoManager.getFileLookupItem(candidates[i], encoded, icon);
                    encodedVariants.add(item.setTailText(" (" + name + ")"));
                } else {
                    encodedVariants.add(variants[i]);
                }
            }
        }*/
//        return new Object[0];

        List<Object> encodedVariants = new ArrayList<Object>();
        try {
            final PsiElement element = this.getElement();
            encodedVariants.add(element);
        } catch (Exception e) {
            logger.warn("Exception in getReferencesByElement - " + e.getMessage());
        }
        return ArrayUtil.toObjectArray(encodedVariants);
    }

    public List<VirtualFile> getSiteTemplates() {
        Project project = myElement.getProject();

        String[] dirs = new String[]{
                "bitrix/templates",
                "local/templates",
        };

        List<VirtualFile> templates = new ArrayList<VirtualFile>();
        for (String dir : dirs) {
            VirtualFile dirPath = project.getBaseDir().findFileByRelativePath(dir);
            if (dirPath == null) {
                continue;
            }

            VirtualFile[] virtualFiles = dirPath.getChildren();
            Collections.addAll(templates, virtualFiles);
        }
        return templates;
    }
}