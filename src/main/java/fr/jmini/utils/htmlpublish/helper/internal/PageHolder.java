package fr.jmini.utils.htmlpublish.helper.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;

import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;

class PageHolder implements Link {
    private PageMapping pageMapping;
    private Document document;

    private PageHolder parent;
    private List<PageHolder> children;
    private PageHolder previous;
    private PageHolder next;
    private String title;

    public PageHolder(PageHolder parent, PageMapping pageMapping, Document document, String title) {
        this.parent = parent;
        this.children = new ArrayList<>();
        this.pageMapping = pageMapping;
        this.document = document;
        this.title = title;
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
        return title;
    }

    @Override
    public String getHrefValue(Path fromCurrentOutputPath) {
        return Impl.createLinkHrefValue(fromCurrentOutputPath, getOutputFile(), null);
    }

    public Stream<PageHolder> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream()
                        .flatMap(PageHolder::flattened));
    }
}