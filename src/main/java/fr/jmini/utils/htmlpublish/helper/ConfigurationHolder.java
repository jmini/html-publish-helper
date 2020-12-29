package fr.jmini.utils.htmlpublish.helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigurationHolder {

    /**
     * The root folder where the input pages are located
     */
    private Path inputRootFolder;

    /**
     * The root folder where the output structure (pages and resources) will be located
     */
    private Path outputRootFolder;

    /**
     * List of pages that are included
     */
    private List<ConfigurationPage> pages = new ArrayList<>();

    /**
     * Page options used when folders are involved
     */
    private ConfigurationPageOptions defaultPageOptions;

    /**
     * List of catalogs that will be created
     */
    private List<ConfigurationCatalog> catalogs = new ArrayList<>();

    /**
     * Options to configure the publication process
     */
    private ConfigurationOptions options;

    public Path getInputRootFolder() {
        return inputRootFolder;
    }

    public void setInputRootFolder(Path inputRootFolder) {
        this.inputRootFolder = inputRootFolder;
    }

    public ConfigurationHolder inputRootFolder(Path path) {
        setInputRootFolder(path);
        return this;
    }

    public Path getOutputRootFolder() {
        return outputRootFolder;
    }

    public void setOutputRootFolder(Path outputRootFolder) {
        this.outputRootFolder = outputRootFolder;
    }

    public ConfigurationHolder outputRootFolder(Path path) {
        setOutputRootFolder(path);
        return this;
    }

    public List<ConfigurationPage> getPages() {
        return pages;
    }

    public void setPages(List<ConfigurationPage> pages) {
        this.pages = pages;
    }

    public ConfigurationHolder addPage(ConfigurationPage page) {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        pages.add(page);
        return this;
    }

    public Optional<ConfigurationPageOptions> getDefaultPageOptions() {
        return Optional.ofNullable(defaultPageOptions);
    }

    public void setDefaultPageOptions(ConfigurationPageOptions defaultPageOptions) {
        this.defaultPageOptions = defaultPageOptions;
    }

    public ConfigurationHolder defaultPageOptions(ConfigurationPageOptions pageOptions) {
        setDefaultPageOptions(pageOptions);
        return this;
    }

    public List<ConfigurationCatalog> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(List<ConfigurationCatalog> catalogs) {
        this.catalogs = catalogs;
    }

    public ConfigurationHolder addCatalog(ConfigurationCatalog catalog) {
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

    public ConfigurationHolder options(ConfigurationOptions configurationOptions) {
        setOptions(configurationOptions);
        return this;
    }

}
