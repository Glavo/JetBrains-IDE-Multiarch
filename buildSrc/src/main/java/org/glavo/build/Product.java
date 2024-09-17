package org.glavo.build;

public enum Product {
    INTELLIJ_COMMUNITY_EDITION("IC", "idea", "ideaIC"),
    INTELLIJ_ULTIMATE("IU", "idea", "ideaIU"),
    GOLAND("GO", "go", "goland");

    private final String code;
    private final String downloadLinkPrefix;
    private final String fileNameBase;

    Product(String code, String downloadLinkPrefix, String fileNameBase) {
        this.code = code;
        this.downloadLinkPrefix = downloadLinkPrefix;
        this.fileNameBase = fileNameBase;
    }

    public String getDownloadLink(String version, Arch arch) {
        return "https://download.jetbrains.com/%s/%s-%s-%s.tar.gz".formatted(downloadLinkPrefix, fileNameBase, version, arch.normalize());
    }

    public String getCode() {
        return code;
    }
}
