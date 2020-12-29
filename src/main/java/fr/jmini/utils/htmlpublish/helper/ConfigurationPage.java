package fr.jmini.utils.htmlpublish.helper;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationPage extends ConfigurationPageOptions {

    /**
     * Name of the input file or folder, relative to the {@link ConfigurationHolder#getInputRootFolder()}.
     */
    private String input;

    /**
     * If {@link ConfigurationPage#input} is a folder, tells if sub-folders must be considered or not.
     */
    private boolean includeChildFolders = false;

    /**
     * Title of the page, in the navigation. If omitted the value defined {@link #titleSelector(String)} in the corresponding page is used.
     */
    private String title;

    /**
     * Name of output relative to the {@link ConfigurationHolder#getOutputRootFolder}. If omitted the same value as the input is used. <br>
     * If the input is a file then the output value can be either a file or a folder (In this second case the filename is preserved). <br>
     * If the input is a folder then the output must be a folder.
     */
    private String output;

    /**
     * Chidren pages for this page
     */
    private List<ConfigurationPage> children;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public ConfigurationPage input(String inputValue) {
        setInput(inputValue);
        return this;
    }

    public boolean isIncludeChildFolders() {
        return includeChildFolders;
    }

    public void setIncludeChildFolders(boolean includeChildFolders) {
        this.includeChildFolders = includeChildFolders;
    }

    public ConfigurationPage includeChildFolders(boolean includeChildFoldersValue) {
        setIncludeChildFolders(includeChildFoldersValue);
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ConfigurationPage title(String titleValue) {
        setTitle(titleValue);
        return this;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public ConfigurationPage output(String outputValue) {
        setOutput(outputValue);
        return this;
    }

    public List<ConfigurationPage> getChildren() {
        return children;
    }

    public ConfigurationPage addChild(ConfigurationPage page) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(page);
        return this;
    }

    @Override
    public ConfigurationPage indexHandling(IndexHandling h) {
        super.indexHandling(h);
        return this;
    }

    @Override
    public ConfigurationPage sitePageSelector(String selector) {
        super.sitePageSelector(selector);
        return this;
    }

    @Override
    public ConfigurationPage titleSelector(String selector) {
        super.titleSelector(selector);
        return this;
    }

}
