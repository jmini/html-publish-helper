package fr.jmini.utils.htmlpublish.helper;

public class ConfigurationPageOptions {

    /**
     * If {@link ConfigurationPage#input} is a folder, tells how to deal with the index page.
     */
    private IndexHandling indexHandling;

    /**
     * If {@link ConfigurationOptions#isCompleteSite()} and {@link ConfigurationPage#getTitle()} is not set, tells how to compute the tile of the page. If not set the <code>title</code> html tag is used.
     */
    private String titleSelector;

    /**
     * If {@link ConfigurationOptions#isCompleteSite()}, tells which part of the page should be selected. If not set the <code>body</code> html tag is used.
     */
    private String sitePageSelector;

    public IndexHandling getIndexHandling() {
        return indexHandling;
    }

    public void setIndexHandling(IndexHandling indexHandling) {
        this.indexHandling = indexHandling;
    }

    public ConfigurationPageOptions indexHandling(IndexHandling h) {
        setIndexHandling(h);
        return this;
    }

    public String getTitleSelector() {
        return titleSelector;
    }

    public void setTitleSelector(String titleSelector) {
        this.titleSelector = titleSelector;
    }

    public ConfigurationPageOptions titleSelector(String selector) {
        setTitleSelector(selector);
        return this;
    }

    public String getSitePageSelector() {
        return sitePageSelector;
    }

    public void setSitePageSelector(String sitePageSelector) {
        this.sitePageSelector = sitePageSelector;
    }

    public ConfigurationPageOptions sitePageSelector(String selector) {
        setSitePageSelector(selector);
        return this;
    }

}
