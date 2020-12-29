package fr.jmini.utils.htmlpublish.helper;

import java.nio.file.Path;

public class ConfigurationCatalog {

    /**
     * The folder that is scanned to create the catalog, if omitted {@link ConfigurationHolder#getOutputRootFolder()} is used.
     */
    private Path folder;

    /**
     * Strategy to create the catalog.
     */
    private Strategy strategy;

    /**
     * The file where the output is stored
     */
    private Path outputFile;

    /**
     * The action that is executed with the catalog output
     */
    private OutputAction outputAction;

    public Path getFolder() {
        return folder;
    }

    public void setFolder(Path folder) {
        this.folder = folder;
    }

    public ConfigurationCatalog folder(Path path) {
        setFolder(path);
        return this;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public ConfigurationCatalog strategy(Strategy s) {
        setStrategy(s);
        return this;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
    }

    public ConfigurationCatalog outputFile(Path path) {
        setOutputFile(path);
        return this;
    }

    public OutputAction getOutputAction() {
        return outputAction;
    }

    public void setOutputAction(OutputAction outputAction) {
        this.outputAction = outputAction;
    }

    public ConfigurationCatalog outputAction(OutputAction action) {
        setOutputAction(action);
        return this;
    }

    public static enum Strategy {
        /**
         * Read the files from the folder (this is interesting if the {@link #folder} contains html files that are not managed by the publish task).
         */
        SCAN_FOLDER,

        /**
         * Add only the files published by the tool to the catalog.
         */
        PUBLISH_OUTPUT
    }

    public static enum OutputAction {
        /**
         * Replace the existing catalog.
         */
        REPLACE_EXISTING,

        /**
         * Merge with the content of the existing catalog without any additional checks.
         */
        MERGE_SILENTLY,

        /**
         * Merge with the content of the existing catalog and fail if one of the entry of the catalog is not present in the existing catalog.
         */
        MERGE_AND_FAIL_IF_ABSENT
    }

}
