package org.glavo.build;

public enum Product {
    IDEA_IC("IC", "idea", "ideaIC"),
    IDEA_IU("IU", "idea", "ideaIU"),
    PYCHARM_COMMUNITY("PC", "python", "pycharm-community"),
    PYCHARM_PROFESSIONAL("PY", "python", "pycharm-professional"),
    GOLAND("GO", "go", "goland");

    private final String productCode;
    private final String downloadLinkPrefix;
    private final String fileNamePrefix;

    Product(String productCode, String downloadLinkPrefix, String fileNamePrefix) {
        this.productCode = productCode;
        this.downloadLinkPrefix = downloadLinkPrefix;
        this.fileNamePrefix = fileNamePrefix;
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
}
