package org.glavo.build;

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
}
