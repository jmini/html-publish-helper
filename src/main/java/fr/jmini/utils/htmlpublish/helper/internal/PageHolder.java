package fr.jmini.utils.htmlpublish.helper.internal;

import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;
import fr.jmini.utils.htmlpublish.helper.LinkToIndexHtmlStrategy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.nodes.Document;



class PageHolder implements Link {
    private PageMapping pageMapping;
    private Document document;

    private PageHolder parent;
    private boolean uniqueRoot;
    private List<PageHolder> children;
    private PageHolder previous;
    private PageHolder next;
    private String title;
    private List<String> cssFileNames;
    private List<String> jsFileNames;
    private LinkToIndexHtmlStrategy linkToIndexHtmlStrategy;

    public PageHolder(PageMapping pageMapping, PageHolder parent, boolean uniqueRoot, Document document, String title, LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        this.pageMapping = pageMapping;
        this.parent = parent;
        this.uniqueRoot = uniqueRoot;
        this.children = new ArrayList<>();
        this.document = document;
        this.title = title;
        this.linkToIndexHtmlStrategy = linkToIndexHtmlStrategy;
    }

    public Path getInputFile() {
        return pageMapping.getInputFile();
    }

    public boolean isInputFileExists() {
        return pageMapping.isInputFileExists();
    }

    public Path getOutputFile() {
        return pageMapping.getOutputFile();
    }

    public ConfigurationPageOptions getPageOptions() {
        return pageMapping.getPageOptions();
    }

    public Document getDocument() {
        return document;
    }

    public PageHolder getParent() {
        return parent;
    }

    public boolean isUniqueRoot() {
        return uniqueRoot;
    }

    public void setChildren(List<PageHolder> children) {
        this.children = children;
    }

    public List<PageHolder> getChildren() {
        return children;
    }

    public void setPrevious(PageHolder previous) {
        this.previous = previous;
    }

    public PageHolder getPrevious() {
        return previous;
    }

    public void setNext(PageHolder next) {
        this.next = next;
    }

    public PageHolder getNext() {
        return next;
    }

    public String getTitle() {
        if (pageMapping.getTitle() != null) {
            return pageMapping.getTitle();
        } else if (title != null) {
            return title;
        }
        return pageMapping.getInputFile()
                .getFileName()
                .toString();
    }

    public boolean isTitleSet() {
        return pageMapping.getTitle() != null || title != null;
    }

    public void setCssFileNames(List<String> cssFileNames) {
        this.cssFileNames = cssFileNames;
    }

    public List<String> getCssFileNames() { return cssFileNames; }

    public void setJsFileNames(List<String> jsFileNames) {
        this.jsFileNames = jsFileNames;
    }
    public List<String> getJsFileNames() { return jsFileNames; }

    @Override
    public String getHrefValue(Path fromCurrentOutputPath) {
        return Impl.createLinkHrefValue(fromCurrentOutputPath, getOutputFile(), null, linkToIndexHtmlStrategy);
    }

    public Stream<PageHolder> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream()
                        .flatMap(PageHolder::flattened));
    }
}