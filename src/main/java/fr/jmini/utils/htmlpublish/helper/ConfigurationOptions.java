package fr.jmini.utils.htmlpublish.helper;

public class ConfigurationOptions {

    /**
     * Indicates if the {@link ConfigurationHolder#getOutputRootFolder()} must be deleted before starting the publishing or not.
     */
    private boolean clearOutputRootFolder = false;

    /**
     * Name of the folder relative to the {@link ConfigurationHolder#getInputRootFolder()} that is used as base folder to get the `pages.yaml` file that controls page ordering.
     */
    private String pagesBaseFolder;

    /**
     * Strategy for links to `index.html` files.
     */
    private LinkToIndexHtmlStrategy linkToIndexHtmlStrategy = LinkToIndexHtmlStrategy.TO_PARENT_FOLDER;

    /**
     * Name of folder relative to the {@link ConfigurationHolder#getOutputRootFolder()} for images.
     */
    private String imagesOutputFolder = "images/";

    /**
     * Name of folder relative to the {@link ConfigurationHolder#getOutputRootFolder()} for javascript resources.
     */
    private String javascriptOutputFolder = "js/";

    /**
     * Name of folder relative to the {@link ConfigurationHolder#getOutputRootFolder()} for css resources.
     */
    private String cssOutputFolder = "css/";

    /**
     * Name of folder relative to the {@link ConfigurationHolder#getOutputRootFolder()} for font resources.
     */
    private String fontOutputFolder = "font/";

    /**
     * Strategy used to rename the resources
     */
    private RewriteStrategy resourcesRewriteStrategy = RewriteStrategy.NO_MODIFICATION;

    /**
     * Publish the page as a complete site (menu, navigation, ...)
     */
    private boolean completeSite;

    /**
     * Indicates if the default css ('site.css') is included during publication of the complete site
     */
    private boolean includeDefaultCss = true;

    /**
     * Indicates if the default javascript ('site.js') is included during publication of the complete site
     */
    private boolean includeDefaultJs = true;

    /**
     * Indicates if the table of content is created during publication of the complete site, note that the default javascript is also creating the table of content (see {@link #isIncludeDefaultJs()})
     */
    private boolean createToc = false;

    /**
     * Name of the site, if omitted the title of the page corresponding to {@link #siteHomePath} is used or if the path is a distant URL the name of {@link ConfigurationHolder#getInputRootFolder()} is used.
     */
    private String siteName;

    /**
     * Either an absolute URL, or a page relative to the {@link ConfigurationHolder#getInputRootFolder()}
     */
    private String siteHomePath;

    /**
     * Footer of the site
     */
    private String footer;

    public boolean isClearOutputRootFolder() {
        return clearOutputRootFolder;
    }

    public void setClearOutputRootFolder(boolean clearOutputRootFolder) {
        this.clearOutputRootFolder = clearOutputRootFolder;
    }

    public ConfigurationOptions clearOutputRootFolder(boolean clear) {
        setClearOutputRootFolder(clear);
        return this;
    }

    public String getPagesBaseFolder() {
        return pagesBaseFolder;
    }

    public void setPagesBaseFolder(String pagesBaseFolder) {
        this.pagesBaseFolder = pagesBaseFolder;
    }

    public ConfigurationOptions pagesBaseFolder(String baseFolder) {
        setPagesBaseFolder(baseFolder);
        return this;
    }

    public LinkToIndexHtmlStrategy getLinkToIndexHtmlStrategy() {
        return linkToIndexHtmlStrategy;
    }

    public void setLinkToIndexHtmlStrategy(LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        this.linkToIndexHtmlStrategy = linkToIndexHtmlStrategy;
    }

    public ConfigurationOptions linkToIndexHtmlStrategy(LinkToIndexHtmlStrategy strategy) {
        setLinkToIndexHtmlStrategy(strategy);
        return this;
    }

    public String getImagesOutputFolder() {
        return imagesOutputFolder;
    }

    public void setImagesOutputFolder(String imagesOutputFolder) {
        this.imagesOutputFolder = imagesOutputFolder;
    }

    public ConfigurationOptions imagesOutputFolder(String imagesOutput) {
        setImagesOutputFolder(imagesOutput);
        return this;
    }

    public String getJavascriptOutputFolder() {
        return javascriptOutputFolder;
    }

    public void setJavascriptOutputFolder(String javascriptOutputFolder) {
        this.javascriptOutputFolder = javascriptOutputFolder;
    }

    public ConfigurationOptions javascriptOutputFolder(String javascriptOutput) {
        setJavascriptOutputFolder(javascriptOutput);
        return this;
    }

    public String getCssOutputFolder() {
        return cssOutputFolder;
    }

    public void setCssOutputFolder(String cssOutputFolder) {
        this.cssOutputFolder = cssOutputFolder;
    }

    public ConfigurationOptions cssOutputFolder(String cssOutput) {
        setCssOutputFolder(cssOutput);
        return this;
    }

    public String getFontOutputFolder() {
        return fontOutputFolder;
    }

    public void setFontOutputFolder(String fontOutputFolder) {
        this.fontOutputFolder = fontOutputFolder;
    }

    public ConfigurationOptions fontOutputFolder(String fontOutput) {
        setFontOutputFolder(fontOutput);
        return this;
    }

    public RewriteStrategy getResourcesRewriteStrategy() {
        return resourcesRewriteStrategy;
    }

    public void setResourcesRewriteStrategy(RewriteStrategy resourcesRewriteStrategy) {
        this.resourcesRewriteStrategy = resourcesRewriteStrategy;
    }

    public ConfigurationOptions resourcesRewriteStrategy(RewriteStrategy strategy) {
        setResourcesRewriteStrategy(strategy);
        return this;
    }

    public boolean isCompleteSite() {
        return completeSite;
    }

    public void setCompleteSite(boolean completeSite) {
        this.completeSite = completeSite;
    }

    public ConfigurationOptions completeSite(boolean completeSiteValue) {
        setCompleteSite(completeSiteValue);
        return this;
    }

    public boolean isIncludeDefaultCss() {
        return includeDefaultCss;
    }

    public void setIncludeDefaultCss(boolean includeDefaultCss) {
        this.includeDefaultCss = includeDefaultCss;
    }

    public ConfigurationOptions includeDefaultCss(boolean isIncludeDefaultCss) {
        setIncludeDefaultCss(isIncludeDefaultCss);
        return this;
    }

    public boolean isIncludeDefaultJs() {
        return includeDefaultJs;
    }

    public void setIncludeDefaultJs(boolean includeDefaultJs) {
        this.includeDefaultJs = includeDefaultJs;
    }

    public ConfigurationOptions includeDefaultJs(boolean isIncludeDefaultJs) {
        setIncludeDefaultJs(isIncludeDefaultJs);
        return this;
    }

    public boolean isCreateToc() {
        return createToc;
    }

    public void setCreateToc(boolean createToc) {
        this.createToc = createToc;
    }

    public ConfigurationOptions createToc(boolean isCreateToc) {
        setCreateToc(isCreateToc);
        return this;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public ConfigurationOptions siteName(String name) {
        setSiteName(name);
        return this;
    }

    public String getSiteHomePath() {
        return siteHomePath;
    }

    public void setSiteHomePath(String siteHomePath) {
        this.siteHomePath = siteHomePath;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public ConfigurationOptions footer(String footerValue) {
        setFooter(footerValue);
        return this;
    }

    public ConfigurationOptions siteHomePath(String path) {
        setSiteHomePath(path);
        return this;
    }
}
