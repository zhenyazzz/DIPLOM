package com.portfolio.common.web.config;

import com.portfolio.common.web.web.CommonWebProblemDetailsHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class CommonWebImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableCommonWeb.class.getName());
        boolean problemDetails = attrs == null || Boolean.TRUE.equals(attrs.get("problemDetails"));

        List<String> imports = new ArrayList<>();
        imports.add(CommonWebConfiguration.class.getName());
        if (problemDetails) {
            imports.add(CommonWebProblemDetailsHandler.class.getName());
        }
        return imports.toArray(String[]::new);
    }
}
