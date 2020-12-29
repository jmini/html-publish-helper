package fr.jmini.utils.htmlpublish.helper.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog;
import fr.jmini.utils.htmlpublish.helper.ConfigurationOptions;
import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;

class Parameters {

    private Path inputRootFolder;
    private Path outputRootFolder;
    private ConfigurationPageOptions defaultPageOptions;
    private List<PageHolder> pageHolders = new ArrayList<>();
    private List<ConfigurationCatalog> catalogs = new ArrayList<>();
    private ConfigurationOptions options = new ConfigurationOptions();
    private Link siteHomeLink;
    private String siteName;

    public Path getInputRootFolder() {
        return inputRootFolder;
    }

    public void setInputRootFolder(Path inputRootFolder) {
        this.inputRootFolder = inputRootFolder;
    }

    public Path getOutputRootFolder() {
        return outputRootFolder;
    }

    public void setOutputRootFolder(Path outputRootFolder) {
        this.outputRootFolder = outputRootFolder;
    }

    public ConfigurationPageOptions getDefaultPageOptions() {
        return defaultPageOptions;
    }

    public void setDefaultPageOptions(ConfigurationPageOptions defaultPageOptions) {
        this.defaultPageOptions = defaultPageOptions;
    }

    public List<PageHolder> getPageHolders() {
        return pageHolders;
    }

    public List<PageHolder> getAllPageHolders() {
        return pageHolders.stream()
                .flatMap(PageHolder::flattened)
                .collect(Collectors.toList());
    }

    public void setPageHolders(List<PageHolder> pageHolders) {
        this.pageHolders = pageHolders;
    }

    public List<ConfigurationCatalog> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(List<ConfigurationCatalog> catalogs) {
        this.catalogs = catalogs;
    }

    public Parameters addCatalog(ConfigurationCatalog catalog) {
        if (catalogs == null) {
            catalogs = new ArrayList<>();
        }
        catalogs.add(catalog);
        return this;
    }

    public ConfigurationOptions getOptions() {
        return options;
    }

    public void setOptions(ConfigurationOptions options) {
        this.options = options;
    }

    public Link getSiteHomeLink() {
        return siteHomeLink;
    }

    public void setSiteHomeLink(Link siteHomeLink) {
        this.siteHomeLink = siteHomeLink;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
}
