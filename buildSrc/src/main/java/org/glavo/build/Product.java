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
    IDEA_IC("IC"),
    IDEA_IU("IU"),
    PYCHARM("PY"),
    PYCHARM_COMMUNITY("PC"),
    WEBSTORM("WS"),
    CLION("CL"), // FIXME
    GOLAND("GO"),
    RUSTROVER("RR"),
    RUBYMINE("RM"),
    PHPSTORM("PS"),
    ;

    private final String productCode;

    Product(String productCode) {
        this.productCode = productCode;
    }

    public boolean isOpenSource() {
        return this == IDEA_IC || this == PYCHARM_COMMUNITY;
    }

    public String getProductCode() {
        return productCode;
    }

    private String getFileNamePrefix() {
        return switch (this) {
            case IDEA_IC -> "ideaIC";
            case IDEA_IU -> "ideaIU";
            case PYCHARM -> "pycharm";
            case PYCHARM_COMMUNITY -> "pycharm-community";
            case WEBSTORM -> "WebStorm";
            case CLION -> "CLion";
            case GOLAND -> "goland";
            case RUSTROVER -> "RustRover";
            case RUBYMINE -> "RubyMine";
            case PHPSTORM -> "PhpStorm";
        };
    }

    public String getFileNameBase(String version, Arch arch) {
        return getFileNameBase(version, arch.normalize());
    }

    public String getFileNameBase(String version, String arch) {
        return "%s-%s-%s".formatted(getFileNamePrefix(), version, arch);
    }

    public String getDownloadLink(String version, Arch arch) {
        String downloadLinkPrefix = switch (this) {
            case IDEA_IC, IDEA_IU -> "idea";
            case PYCHARM, PYCHARM_COMMUNITY -> "python";
            case WEBSTORM -> "webstorm";
            case CLION -> "cpp";
            case GOLAND -> "go";
            case RUSTROVER -> "rustrover";
            case RUBYMINE -> "ruby";
            case PHPSTORM -> "webide";
        };
        return "https://download.jetbrains.com/%s/%s.tar.gz".formatted(downloadLinkPrefix, getFileNameBase(version, arch));
    }

    public String getLauncherName() {
        return switch (this) {
            case IDEA_IC, IDEA_IU -> "idea";
            case PYCHARM, PYCHARM_COMMUNITY -> "pycharm";
            case WEBSTORM -> "webstorm";
            case CLION -> "clion";
            case GOLAND -> "goland";
            case RUSTROVER -> "rustrover";
            case RUBYMINE -> "rubymine";
            case PHPSTORM -> "phpstorm";
        };
    }

    public String getPackageName() {
        // https://github.com/JonasGroeger/jetbrains-ppa
        return switch (this) {
            case IDEA_IC -> "intellij-idea-community";
            case IDEA_IU -> "intellij-idea-ultimate";
            case PYCHARM -> "pycharm";
            case PYCHARM_COMMUNITY -> "pycharm-community";
            case WEBSTORM -> "webstorm";
            case CLION -> "clion";
            case GOLAND -> "goland";
            case RUSTROVER -> "rustrover";
            case RUBYMINE -> "rubymine";
            case PHPSTORM -> "phpstorm";
        };
    }

    public String getFullName() {
        return switch (this) {
            case IDEA_IC -> "IntelliJ IDEA Community Edition";
            case IDEA_IU -> "IntelliJ IDEA Ultimate";
            case PYCHARM -> "PyCharm";
            case PYCHARM_COMMUNITY -> "PyCharm Community";
            case WEBSTORM -> "WebStorm";
            case CLION -> "CLion";
            case GOLAND -> "GoLand";
            case RUSTROVER -> "RustRover";
            case RUBYMINE -> "RubyMine";
            case PHPSTORM -> "PhpStorm";
        };
    }

    public String getDescription() {
        return switch (this) {
            case IDEA_IC -> "The IDE for Java and Kotlin enthusiasts";
            case IDEA_IU -> "The IDE for Pro Java and Kotlin developers";
            case PYCHARM -> "The only Python IDE you need";
            case PYCHARM_COMMUNITY -> "The pure Python IDE";
            case WEBSTORM -> "A JavaScript and TypeScript IDE";
            case CLION -> "A cross-platform C and C++ IDE";
            case GOLAND -> "An IDE for Go and Web";
            case RUSTROVER -> "A powerful IDE for Rust";
            case RUBYMINE -> "A Ruby and Rails IDE";
            case PHPSTORM -> "A smart IDE for PHP and Web";
        };
    }

    public long getPriority() {
        return switch (this) {
            case IDEA_IC, PYCHARM_COMMUNITY -> 50L;
            default -> 100L;
        };
    }

    // https://specifications.freedesktop.org/menu-spec/latest/category-registry.html
    public String getDesktopCategories() {
        return "Development;Debugger;IDE;";
    }

    public String getDesktopKeywords() {
        return "jetbrains;" + (switch (this) {
            case IDEA_IC, IDEA_IU -> "intellij;idea;java;kotlin;scala;";
            case PYCHARM, PYCHARM_COMMUNITY -> "python;";
            case WEBSTORM -> "js;javascript;typescript;html;";
            case CLION -> "cpp;";
            case GOLAND -> "golang;";
            case RUSTROVER -> "rust;";
            case RUBYMINE -> "ruby;rails;";
            case PHPSTORM -> "php;";
        });
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
