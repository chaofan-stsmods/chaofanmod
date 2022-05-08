package io.chaofan.sts.snippets

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

try {
    def file = PsiDocumentManager.getInstance(_editor.project).getPsiFile(_editor.document);
    PsiMethodCallExpression callExpression = PsiTreeUtil.findElementOfClassAtOffset(file, _editor.caretModel.offset, PsiMethodCallExpression.class, false);
    def type = callExpression.argumentList.expressions[1].type;

    def result = [];
    def jpf = JavaPsiFacade.getInstance(_editor.project);
    def typeName = type.canonicalText;
    def start = typeName.indexOf('<');
    def realTypeName = typeName.substring(start + 1, typeName.length() - 1);
    PsiClass psiClass = jpf.findClass(realTypeName, callExpression.resolveScope);

    for (def field : psiClass.allFields) {
        if (!field.hasModifierProperty('public')) {
            result.add(field.name);
        };
    };
    return result;
} catch (Throwable ignored) {
    return '';
};


/*

import com.intellij.codeInsight.lookup.*;
import com.intellij.util.PlatformIcons;

Set<LookupElement> result = new LinkedHashSet<>();
def jpf = JavaPsiFacade.getInstance(_editor.project);
PsiClass psiClass = jpf.findClass(type.canonicalText, type.resolveScope);

for (def field : psiClass.allFields) {
    if (!field.hasModifierProperty('public')) {
        result.add(LookupElementBuilder.create(field.name).setTypeText(field.type.presentableText).withIcon(PlatformIcons.FIELD_ICON));
    };
};

return result.toArray(LookupElement.EMPTY_ARRAY);*/
