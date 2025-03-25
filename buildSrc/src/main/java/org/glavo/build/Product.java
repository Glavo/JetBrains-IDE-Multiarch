/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.build;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum Product {
    IDEA_IC("IC", "idea", "ideaIC", "idea"),
    IDEA_IU("IU", "idea", "ideaIU", "idea"),
    PYCHARM_COMMUNITY("PC", "python", "pycharm-community", "pycharm"),
    PYCHARM_PROFESSIONAL("PY", "python", "pycharm-professional", "pycharm"),
    GOLAND("GO", "go", "goland", "goland");

    private final String productCode;
    private final String downloadLinkPrefix;
    private final String fileNamePrefix;
    private final String launcherName;

    Product(String productCode, String downloadLinkPrefix, String fileNamePrefix, String launcherName) {
        this.productCode = productCode;
        this.downloadLinkPrefix = downloadLinkPrefix;
        this.fileNamePrefix = fileNamePrefix;
        this.launcherName = launcherName;
    }

    public boolean isOpenSource() {
        return this == IDEA_IC || this == PYCHARM_COMMUNITY;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getFileNameBase(String version, Arch arch) {
        return "%s-%s-%s".formatted(fileNamePrefix, version, arch.normalize());
    }

    public String getDownloadLink(String version, Arch arch) {
        return "https://download.jetbrains.com/%s/%s.tar.gz".formatted(downloadLinkPrefix, getFileNameBase(version, arch));
    }

    public String getLauncherName() {
        return launcherName;
    }

    public Map<String, String> resolveProperties(Map<String, String> properties) {
        var result = new HashMap<String, String>();
        properties.forEach((key, value) -> {
            if (key.startsWith("default.")) {
                result.put(key.substring("default.".length()), value);
            }
        });

        var prefix = productCode.toLowerCase(Locale.ROOT) + ".";
        properties.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), value);
            }
        });
        return result;
    }
}
