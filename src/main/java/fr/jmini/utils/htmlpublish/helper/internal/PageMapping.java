package fr.jmini.utils.htmlpublish.helper.internal;

import java.nio.file.Path;
import java.util.List;

import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;

class PageMapping {
    private Path inputFile;
    private boolean inputFileExists;
    private Path outputFile;
    private ConfigurationPageOptions pageOptions;
    private String title;
    private List<PageMapping> children;

    public PageMapping(Path inputFile, boolean inputFileExists, Path outputFile, ConfigurationPageOptions pageOptions, String title, List<PageMapping> children) {
        this.inputFile = inputFile;
        this.inputFileExists = inputFileExists;
        this.outputFile = outputFile;
        this.pageOptions = pageOptions;
        this.title = title;
        this.children = children;
    }

    public Path getInputFile() {
        return inputFile;
    }

    public boolean isInputFileExists() {
        return inputFileExists;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public ConfigurationPageOptions getPageOptions() {
        return pageOptions;
    }

    public String getTitle() {
        return title;
    }

    public List<PageMapping> getChildren() {
        return children;
    }
}