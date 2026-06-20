package com.sherwinzeng.cardiology.cardiologysession.support;

import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GuideReferenceSupport {

    private GuideReferenceSupport() {
    }

    public static String encode(List<String> guideReferences) {
        if (guideReferences == null || guideReferences.isEmpty()) {
            return null;
        }
        return JsonSerialization.toJson(guideReferences);
    }

    public static List<String> decode(String guideReferencesJson) {
        if (!StringUtils.hasText(guideReferencesJson)) {
            return Collections.emptyList();
        }
        String[] values = JsonSerialization.fromJson(guideReferencesJson, String[].class);
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(values);
    }
}
