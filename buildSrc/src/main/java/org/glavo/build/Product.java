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
    IDEA("IU"),
    IDEA_COMMUNITY("IC"),
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
        return this == IDEA_COMMUNITY || this == PYCHARM_COMMUNITY;
    }

    public String getProductCode() {
        return productCode;
    }

    private String getFileNamePrefix() {
        return switch (this) {
            case IDEA -> "idea";
            case IDEA_COMMUNITY -> "ideaIC";
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
        if (this == IDEA_COMMUNITY || this == PYCHARM_COMMUNITY) {
            String githubName = switch (this) {
                case IDEA_COMMUNITY -> "idea";
                case PYCHARM_COMMUNITY -> "pycharm";
                default -> throw new AssertionError("Unreachable code");
            };

            return "https://github.com/JetBrains/intellij-community/releases/download/%1$s%%2F%2$s/%1$s-%2$s-%3$s.tar.gz"
                    .formatted(githubName, version, arch.normalize());
        } else {
            String downloadLinkPrefix = switch (this) {
                //noinspection DataFlowIssue
                case IDEA_COMMUNITY, IDEA -> "idea";
                //noinspection DataFlowIssue
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
    }

    public String getLauncherName() {
        return switch (this) {
            case IDEA_COMMUNITY, IDEA -> "idea";
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
            case IDEA -> "intellij-idea-ultimate";
            case IDEA_COMMUNITY -> "intellij-idea-community";
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
            case IDEA -> "IntelliJ IDEA Ultimate";
            case IDEA_COMMUNITY -> "IntelliJ IDEA Community Edition";
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
            case IDEA -> "The IDE for Pro Java and Kotlin developers";
            case IDEA_COMMUNITY -> "The IDE for Java and Kotlin enthusiasts";
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
            case IDEA_COMMUNITY, PYCHARM_COMMUNITY -> 50L;
            default -> 100L;
        };
    }

    // https://specifications.freedesktop.org/menu-spec/latest/category-registry.html
    public String getDesktopCategories() {
        return "Development;Debugger;IDE;";
    }

    public String getDesktopKeywords() {
        return "jetbrains;" + (switch (this) {
            case IDEA_COMMUNITY, IDEA -> "intellij;idea;java;kotlin;scala;";
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
