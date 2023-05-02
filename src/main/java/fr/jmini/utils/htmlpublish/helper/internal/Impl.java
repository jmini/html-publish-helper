package fr.jmini.utils.htmlpublish.helper.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog;
import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog.OutputAction;
import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog.Strategy;
import fr.jmini.utils.htmlpublish.helper.ConfigurationHolder;
import fr.jmini.utils.htmlpublish.helper.ConfigurationOptions;
import fr.jmini.utils.htmlpublish.helper.ConfigurationPage;
import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;
import fr.jmini.utils.htmlpublish.helper.IndexHandling;
import fr.jmini.utils.htmlpublish.helper.LinkToIndexHtmlStrategy;
import fr.jmini.utils.htmlpublish.helper.RewriteStrategy;
import fr.jmini.utils.pathorder.AbsolutePathComparator;
import fr.jmini.utils.pathorder.Order;
import fr.jmini.utils.pathorder.Pages;
import fr.jmini.utils.pathorder.SortConfig;

public class Impl {

    private static final String DEFAULT_JS_NAME = "site.js";
    private static final String DEFAULT_CSS_NAME = "site.css";

    private static final List<String> SITE_IMAGES = Arrays.asList(
            "back.svg",
            "caret.svg",
            "chevron.svg",
            "home.svg",
            "home-o.svg",
            "menu.svg");

    private static final List<String> SITE_FONTS = Arrays.asList(
            "roboto-latin-400.woff",
            "roboto-latin-400.woff2",
            "roboto-latin-400italic.woff",
            "roboto-latin-400italic.woff2",
            "roboto-latin-500.woff",
            "roboto-latin-500.woff2",
            "roboto-latin-500italic.woff",
            "roboto-latin-500italic.woff2",
            "roboto-mono-latin-400.woff",
            "roboto-mono-latin-400.woff2",
            "roboto-mono-latin-500.woff",
            "roboto-mono-latin-500.woff2");

    public static void run(ConfigurationHolder configuration) {
        Parameters param = prepareParameters(configuration);

        clearOutputRootFolder(param);
        for (PageHolder holder : param.getAllPageHolders()) {
            if (holder.isInputFileExists()) {
                publishHtmlFile(param, holder);
            }
        }
        for (ConfigurationCatalog catalog : param.getCatalogs()) {
            writeCatalog(param, catalog);
        }
    }

    static Parameters prepareParameters(ConfigurationHolder config) {
        Parameters param = new Parameters();
        if (config.getOutputRootFolder() == null) {
            throw new IllegalStateException("The outputRootFolder can not be null");
        }
        param.setOutputRootFolder(config.getOutputRootFolder()
                .toAbsolutePath());

        ConfigurationOptions originalOptions = config.getOptions();
        if (originalOptions != null) {
            if (originalOptions.getImagesOutputFolder() != null) {
                originalOptions.setImagesOutputFolder(addTrailingSlash(originalOptions.getImagesOutputFolder()));
            }
            if (originalOptions.getJavascriptOutputFolder() != null) {
                originalOptions.setJavascriptOutputFolder(addTrailingSlash(originalOptions.getJavascriptOutputFolder()));
            }
            if (originalOptions.getCssOutputFolder() != null) {
                originalOptions.setCssOutputFolder(addTrailingSlash(originalOptions.getCssOutputFolder()));
            }
            if (originalOptions.getFontOutputFolder() != null) {
                originalOptions.setFontOutputFolder(addTrailingSlash(originalOptions.getFontOutputFolder()));
            }
            if (originalOptions.getLinkToIndexHtmlStrategy() == null) {
                originalOptions.setLinkToIndexHtmlStrategy(LinkToIndexHtmlStrategy.TO_PARENT_FOLDER);
            }
            param.setOptions(originalOptions);
        }

        ConfigurationOptions configOptions = param.getOptions();
        if (config.getInputRootFolder() != null) {
            param.setInputRootFolder(config.getInputRootFolder()
                    .toAbsolutePath());

            param.setDefaultPageOptions(fixPageOptionsDefaultValues(
                    config.getDefaultPageOptions()
                            .orElse(new ConfigurationPageOptions())));

            List<PageMapping> pageMappings;
            List<ConfigurationPage> pages = config.getPages();
            if (pages == null || pages.isEmpty()) {
                pageMappings = createPageMappingsFromRoot(param);
            } else {
                pageMappings = createPageMappings(param, pages);
            }
            List<PageHolder> pageHolders = createPageHolders(pageMappings, null, configOptions.getLinkToIndexHtmlStrategy());
            setPreviousAndNext(pageHolders);
            param.setPageHolders(pageHolders);
        }
        if (config.getCatalogs() != null) {
            for (ConfigurationCatalog catalog : config.getCatalogs()) {
                if (catalog.getOutputFile() == null) {
                    throw new IllegalStateException("The outputFile in the catalog can not be null");
                }
                if (catalog.getFolder() == null) {
                    catalog.setFolder(param.getOutputRootFolder());
                }
                if (catalog.getStrategy() == null) {
                    catalog.setStrategy(Strategy.PUBLISH_OUTPUT);
                }
                if (catalog.getOutputAction() == null) {
                    catalog.setOutputAction(OutputAction.REPLACE_EXISTING);
                }
                param.addCatalog(catalog);
            }
        }

        if (configOptions.getSiteName() != null) {
            param.setSiteName(configOptions.getSiteName());
        }
        String siteHomePath = configOptions.getSiteHomePath();
        if (siteHomePath == null) {
            setFallbackSiteTitle(param);
        } else {
            if (isUrlAbsolute(siteHomePath)) {
                param.setSiteHomeLink(new RemoteLink(siteHomePath));
                if (param.getSiteName() == null) {
                    param.setSiteName(param.getInputRootFolder()
                            .getFileName()
                            .toString());
                }
            } else {
                Path absolutePath = param.getInputRootFolder()
                        .resolve(siteHomePath);
                Optional<PageHolder> find = param.getPageHolders()
                        .stream()
                        .flatMap(PageHolder::flattened)
                        .filter(h -> Objects.equals(absolutePath, h.getInputFile()))
                        .findAny();
                if (find.isPresent()) {
                    param.setSiteHomeLink(find.get());
                    if (param.getSiteName() == null) {
                        param.setSiteName(find.get()
                                .getTitle());
                    }
                } else {
                    setFallbackSiteTitle(param);
                }
            }
        }
        return param;
    }

    private static void setPreviousAndNext(List<PageHolder> pageHolders) {
        List<PageHolder> allPageHolders = pageHolders.stream()
                .flatMap(PageHolder::flattened)
                .collect(Collectors.toList());
        PageHolder previous = null;
        for (PageHolder h : allPageHolders) {
            if (h.isInputFileExists()) {
                if (previous != null) {
                    h.setPrevious(previous);
                    previous.setNext(h);
                }
                previous = h;
            }
        }
    }

    private static void setFallbackSiteTitle(Parameters param) {
        if (param.getPageHolders()
                .isEmpty()) {
            throw new IllegalStateException("Option 'siteHomePath' is null and there is no pages to publish");
        } else {
            PageHolder firstPage = param.getAllPageHolders()
                    .stream()
                    .filter(h -> h.isInputFileExists())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Option 'siteHomePath' is null and there is no pages that exists to publish"));
            param.setSiteHomeLink(firstPage);
            if (param.getSiteName() == null) {
                PageHolder firstTitle = param.getAllPageHolders()
                        .stream()
                        .filter(h -> h.isTitleSet())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Option 'siteName' is null and there is no pages that contains a title to publish"));
                param.setSiteName(firstTitle.getTitle());
            }
        }
    }

    private static List<PageHolder> createPageHolders(List<PageMapping> pageMappings, PageHolder parent, LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        boolean uniqueRoot = (parent == null && pageMappings.size() == 1);
        List<PageHolder> pageHolders = pageMappings.stream()
                .map(m -> createPageHolder(m, parent, uniqueRoot, linkToIndexHtmlStrategy))
                .collect(Collectors.toList());
        return pageHolders;
    }

    private static PageHolder createPageHolder(PageMapping pageMapping, PageHolder parent, boolean uniqueRoot, LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        if (pageMapping.isInputFileExists()) {
            Document doc = createDocument(pageMapping.getInputFile());
            String title;
            if (pageMapping.getTitle() == null) {
                title = readTitleFromDoc(doc, pageMapping.getPageOptions(), pageMapping.getInputFile());
            } else {
                title = null;
            }
            PageHolder pageHolder = new PageHolder(pageMapping, parent, uniqueRoot, doc, title, linkToIndexHtmlStrategy);
            pageHolder.setChildren(createPageHolders(pageMapping.getChildren(), pageHolder, linkToIndexHtmlStrategy));
            return pageHolder;
        }

        PageHolder pageHolder = new PageHolder(pageMapping, parent, uniqueRoot, null, null, linkToIndexHtmlStrategy);
        pageHolder.setChildren(createPageHolders(pageMapping.getChildren(), pageHolder, linkToIndexHtmlStrategy));
        return pageHolder;
    }

    private static Document createDocument(Path path) {
        String content = readFile(path);
        return Jsoup.parse(content);
    }

    private static String readTitleFromDoc(Document doc, ConfigurationPageOptions pageOptions, Path inputFile) {
        String title;
        String selector = pageOptions
                .getTitleSelector();
        if (selector == null) {
            selector = "title";
        }
        Element element = doc.selectFirst(selector);
        if (element != null) {
            title = element.text();
        } else {
            title = inputFile
                    .getFileName()
                    .toString();
        }
        return title;
    }

    static void clearOutputRootFolder(Parameters param) {
        ConfigurationOptions options = param.getOptions();
        if (Files.exists(param.getOutputRootFolder()) && options.isClearOutputRootFolder()) {
            try (Stream<Path> stream = Files.walk(param.getOutputRootFolder())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ConfigurationPageOptions mergePageOption(ConfigurationPageOptions defaultPageOptions, ConfigurationPageOptions pageOptions) {
        if (pageOptions.getIndexHandling() == null) {
            pageOptions.setIndexHandling(defaultPageOptions.getIndexHandling());
        }
        if (pageOptions.getTitleSelector() == null) {
            pageOptions.setTitleSelector(defaultPageOptions.getTitleSelector());
        }
        if (pageOptions.getSitePageSelector() == null) {
            pageOptions.setSitePageSelector(defaultPageOptions.getSitePageSelector());
        }
        return fixPageOptionsDefaultValues(pageOptions);
    }

    private static ConfigurationPageOptions fixPageOptionsDefaultValues(ConfigurationPageOptions pageOptions) {
        if (pageOptions.getIndexHandling() == null) {
            pageOptions.setIndexHandling(IndexHandling.USE_PAGE_AS_PARENT);
        }
        return pageOptions;
    }

    private static List<PageMapping> createPageMappingsFromRoot(Parameters params) {
        ConfigurationPage page = new ConfigurationPage()
                .input("")
                .includeChildFolders(true);
        String pagesBaseFolder = params.getOptions()
                .getPagesBaseFolder();
        return createPageMapping(params.getInputRootFolder(), params.getOutputRootFolder(), pagesBaseFolder, page, params.getDefaultPageOptions(), Collections.emptyList())
                .map(p -> Collections.singletonList(p))
                .orElse(Collections.emptyList());
    }

    private static List<PageMapping> createPageMappings(Parameters param, List<ConfigurationPage> list) {
        String pagesBaseFolder = param.getOptions()
                .getPagesBaseFolder();
        return list.stream()
                .map(page -> {
                    ConfigurationPageOptions pageOptions = mergePageOption(param.getDefaultPageOptions(), page);
                    List<PageMapping> childrenMappings;
                    if (page.getChildren() != null) {
                        childrenMappings = createPageMappings(param, page.getChildren());
                    } else {
                        childrenMappings = Collections.emptyList();
                    }
                    return createPageMapping(param.getInputRootFolder(), param.getOutputRootFolder(), pagesBaseFolder, page, pageOptions, childrenMappings);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static Optional<PageMapping> createPageMapping(Path inputRootFolder, Path outputRootFolder, String pagesBaseFolder, ConfigurationPage page, ConfigurationPageOptions pageOptions, List<PageMapping> childrenFromConfig) {
        if (page.getInput() == null) {
            return Optional.of(new PageMapping(null, false, null, pageOptions, page.getTitle(), childrenFromConfig));
        }
        Path inputPath = inputRootFolder.resolve(page.getInput());
        if (Files.isDirectory(inputPath)) {
            List<PageMapping> childrenFromFolder;
            try (Stream<Path> stream = Files.list(inputPath)) {
                childrenFromFolder = stream
                        .filter(p -> {
                            if (Files.isDirectory(p)) {
                                return page.isIncludeChildFolders();
                            }
                            if ("index.html".equals(p.getFileName()
                                    .toString())) {
                                return pageOptions.getIndexHandling() == IndexHandling.USE_PAGE_IN_THE_LIST;
                            }
                            return isHtmlFile(p);
                        })
                        .map(Path::toAbsolutePath)
                        .sorted(new AbsolutePathComparator(p -> loadPageOrder(inputRootFolder, pagesBaseFolder, p), null, Order.NATURAL))
                        .map(p -> {
                            if (Files.isDirectory(p)) {

                                ConfigurationPage childPage = new ConfigurationPage()
                                        .input(relativizeToString(inputRootFolder, p))
                                        .includeChildFolders(true);
                                return createPageMapping(inputRootFolder, outputRootFolder, pagesBaseFolder, childPage, pageOptions, Collections.emptyList());
                            }
                            Path inputFolder;
                            Path outputFolder;
                            if (page.getOutput() != null) {
                                inputFolder = inputPath;
                                outputFolder = outputRootFolder.resolve(page.getOutput());
                            } else {
                                inputFolder = inputRootFolder;
                                outputFolder = outputRootFolder;
                            }
                            Path inputRelPath = inputFolder.relativize(p);
                            Path outputPath = outputFolder.resolve(inputRelPath);
                            return Optional.of(new PageMapping(p, true, outputPath, pageOptions, null, Collections.emptyList()));
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IllegalStateException("Could not get the content of folder input '" + page.getInput() + "' in input root folder '" + inputRootFolder + "' : " + e.getMessage());
            }

            List<PageMapping> children = new ArrayList<>();
            children.addAll(childrenFromFolder);
            children.addAll(childrenFromConfig);
            Path indexPath = inputPath.resolve("index.html");
            if (Files.isRegularFile(indexPath)) {
                if (pageOptions.getIndexHandling() == IndexHandling.USE_PAGE_AS_PARENT) {
                    return Optional.of(createPageMappingForFile(inputRootFolder, outputRootFolder, page, pageOptions, children, indexPath));
                } else if (pageOptions.getIndexHandling() == IndexHandling.USE_TITLE_ONLY) {
                    Document doc = createDocument(indexPath);
                    String title = readTitleFromDoc(doc, pageOptions, indexPath);
                    return Optional.of(new PageMapping(inputPath, false, null, pageOptions, title, children));
                }
            }
            if (children.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new PageMapping(inputPath, false, null, pageOptions, page.getTitle(), children));
        } else {
            return Optional.of(createPageMappingForFile(inputRootFolder, outputRootFolder, page, pageOptions, childrenFromConfig, inputPath));
        }
    }

    static Optional<SortConfig> loadPageOrder(Path inputRootFolder, String pagesBaseFolder, Path inputPath) {
        Path path = computePathBasedOnPagesBaseFolder(inputRootFolder, pagesBaseFolder, inputPath);
        if (Files.isDirectory(path)) {
            Path yamlFile = path.resolve("pages.yaml");
            if (Files.isReadable(yamlFile)) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = Files.newInputStream(yamlFile)) {
                    return Optional.of(yaml.loadAs(inputStream, Pages.class));
                } catch (YAMLException e) {
                    System.err.println("Syntax error in the '" + yamlFile.toAbsolutePath() + "' file");
                } catch (IOException e) {
                    System.err.println("Could not read the '" + yamlFile.toAbsolutePath() + "' file");
                }
            }
        }
        return Optional.empty();
    }

    private static Path computePathBasedOnPagesBaseFolder(Path inputRootFolder, String pagesBaseFolder, Path inputPath) {
        if (pagesBaseFolder != null) {
            Path relPath = inputRootFolder.relativize(inputPath);
            return inputRootFolder.resolve(pagesBaseFolder)
                    .resolve(relPath);
        }
        return inputPath;
    }

    private static PageMapping createPageMappingForFile(Path inputRootFolder, Path outputRootFolder, ConfigurationPage page, ConfigurationPageOptions pageOptions, List<PageMapping> childrenFromConfig, Path inputPath) {
        Path outputPath;
        if (page.getOutput() != null) {
            if (endsWithHtml(page.getOutput())) {
                outputPath = outputRootFolder.resolve(page.getOutput());
            } else {
                outputPath = outputRootFolder.resolve(page.getOutput())
                        .resolve(inputPath.getFileName());
            }
        } else {
            Path inputRelPath = inputRootFolder.relativize(inputPath);
            outputPath = outputRootFolder.resolve(inputRelPath);
        }
        boolean regularFile = Files.isRegularFile(inputPath);
        return new PageMapping(inputPath, regularFile, outputPath, pageOptions, page.getTitle(), childrenFromConfig);
    }

    private static String addTrailingSlash(String value) {
        return value + (value.endsWith("/") ? "" : "/");
    }

    static void publishHtmlFile(Parameters param, PageHolder current) {
        String relPathToOutputFolder = relativizeToString(current.getOutputFile()
                .getParent(), param.getOutputRootFolder());
        if (!relPathToOutputFolder.isEmpty()) {
            relPathToOutputFolder = relPathToOutputFolder + "/";
        }

        Document doc = current.getDocument();

        ConfigurationOptions options = param.getOptions();
        List<Element> cssElements;
        List<Element> jsElements;
        try {
            Files.createDirectories(current.getOutputFile()
                    .getParent());
            moveAndCopy(doc, current.getInputFile(), param, relPathToOutputFolder, options.getImagesOutputFolder(), "img", (e) -> true, "src");
            if (!options.isCompleteSite() || options.isIncludeOriginalCss()) {
                cssElements = moveAndCopy(doc, current.getInputFile(), param, relPathToOutputFolder, options.getCssOutputFolder(), "link", (e) -> "stylesheet".equalsIgnoreCase(e.attr("rel")), "href");
            } else {
                cssElements = Collections.emptyList();
            }
            if (!options.isCompleteSite() || options.isIncludeOriginalJs()) {
                jsElements = moveAndCopy(doc, current.getInputFile(), param, relPathToOutputFolder, options.getJavascriptOutputFolder(), "script", (e) -> true, "src");
            } else {
                jsElements = Collections.emptyList();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could move file: " + current.getInputFile(), e);
        }

        rewriteLinks(doc, param.getInputRootFolder(), current.getInputFile(), param.getOutputRootFolder(), current.getOutputFile(), param.getPageHolders(), options.getLinkToIndexHtmlStrategy());

        Document outDoc;
        if (options.isCompleteSite()) {
            Map<String, String> resourceMapping = copyResources(param);
            final Element elementToInsert;
            if (current.getPageOptions()
                    .getSitePageSelector() != null) {
                Element findElement = doc.selectFirst(current.getPageOptions()
                        .getSitePageSelector());
                if (findElement == null) {
                    elementToInsert = doc.selectFirst("body");
                } else {
                    elementToInsert = findElement;
                }
            } else {
                elementToInsert = doc.selectFirst("body");
            }

            List<Element> additionalElements = new ArrayList<>();
            addElementsNotInElementToInsert(cssElements, elementToInsert, additionalElements);
            addElementsNotInElementToInsert(jsElements, elementToInsert, additionalElements);
            outDoc = createOutDoc(param, relPathToOutputFolder, resourceMapping, current, elementToInsert, additionalElements);

        } else {
            outDoc = doc;
        }

        outDoc.outputSettings()
                .charset("ASCII");
        String content = outDoc.toString();
        writeFile(current.getOutputFile(), content);
    }

    private static void addElementsNotInElementToInsert(List<Element> elements, final Element elementToInsert, List<Element> additionalElements) {
        elements.stream()
                .filter(e -> !e.parents()
                        .contains(elementToInsert))
                .forEach(additionalElements::add);
    }

    static Document createOutDoc(Parameters param, String relPathToOutputFolder, Map<String, String> resourceMapping, PageHolder current, Element elementToInsert, List<Element> additionalElements) {
        ConfigurationOptions options = param.getOptions();

        String home = param.getSiteHomeLink()
                .getHrefValue(current.getOutputFile());

        Document doc = new Document("");
        doc.appendChild(new DocumentType("html", "", ""));
        Element html = doc.appendElement("html");
        html.attr("lang", "en");
        Element head = html.appendElement("head");
        head.appendElement("meta")
                .attr("name", "viewport")
                .attr("content", "width=device-width,initial-scale=1");
        head.appendElement("title")
                .text(current.getTitle());
        if (options.isIncludeDefaultCss()) {
            head.appendElement("link")
                    .attr("rel", "stylesheet")
                    .attr("href", createFilePath(relPathToOutputFolder, resourceMapping.get(DEFAULT_CSS_NAME)));
        }
        Element body = html.appendElement("body")
                .addClass("article");
        body.appendElement("header")
                .addClass("header")
                .appendElement("nav")
                .addClass("navbar")
                .appendElement("div")
                .addClass("navbar-brand")
                .appendElement("a")
                .addClass("navbar-item")
                .attr("href", home)
                .text(param.getSiteName());
        Element bodyDiv = body.appendElement("div")
                .addClass("body");
        Element nav = bodyDiv.appendElement("div")
                .addClass("nav-container")
                .appendElement("aside")
                .addClass("nav");
        Element navMenu = nav.appendElement("div")
                .addClass("panels")
                .appendElement("div")
                .addClass("nav-panel-menu")
                .addClass("is-active")
                .attr("data-panel", "menu")
                .appendElement("nav")
                .addClass("nav-menu");
        navMenu.appendElement("h3")
                .addClass("title")
                .appendElement("a")
                .attr("href", "#");
        List<PageHolder> pageHolders = param.getPageHolders();
        List<PageHolder> pages;
        if (pageHolders.size() == 1) {
            pages = pageHolders
                    .get(0)
                    .getChildren();
        } else {
            pages = pageHolders;
        }
        appendNavList(0, pages, current, navMenu);

        nav.appendElement("div")
                .addClass("nav-panel-explore")
                .attr("data-panel", "explore")
                .appendElement("div")
                .addClass("context");

        Element main = bodyDiv.appendElement("main")
                .addClass("article");
        Element toolbar = main.appendElement("div")
                .addClass("toolbar")
                .attr("role", "navigation");
        toolbar.appendElement("button")
                .addClass("nav-toggle");
        Element homeLink = toolbar.appendElement("a")
                .addClass("home-link")
                .attr("href", home);
        if (param.getSiteHomeLink() == current) {
            homeLink.addClass("is-current");
        }
        Element breadcrumbsList = toolbar.appendElement("nav")
                .addClass("breadcrumbs")
                .attr("aria-label", "breadcrumbs")
                .appendElement("ul");

        List<PageHolder> list = calculateBreadcrumbsList(current);
        for (PageHolder p : list) {
            if (p.isInputFileExists()) {
                breadcrumbsList.appendElement("li")
                        .appendElement("a")
                        .attr("href", p.getHrefValue(current.getOutputFile()))
                        .text(p.getTitle());
            } else {
                breadcrumbsList.appendElement("li")
                        .text(p.getTitle());
            }
        }

        Element content = main.appendElement("div")
                .addClass("content");
        Element article = content.appendElement("article")
                .addClass("doc");
        article.appendElement("h1")
                .addClass("page")
                .text(current.getTitle());

        for (Element e : elementToInsert.children()) {
            article.appendChild(e.clone());
        }

        Element pagination = article.appendElement("nav")
                .addClass("pagination");
        PageHolder previous = current.getPrevious();
        if (previous != null) {
            pagination.appendElement("span")
                    .addClass("prev")
                    .appendElement("a")
                    .attr("href", previous.getHrefValue(current.getOutputFile()))
                    .text(previous.getTitle());
        }
        PageHolder next = current.getNext();
        if (next != null) {
            pagination.appendElement("span")
                    .addClass("next")
                    .appendElement("a")
                    .attr("href", next.getHrefValue(current.getOutputFile()))
                    .text(next.getTitle());
        }

        createContentToc(options, elementToInsert, 2, 1, 3, content);

        String footer = options.getFooter();
        if (footer != null) {
            body.appendElement("footer")
                    .addClass("footer")
                    .appendElement("p")
                    .text(footer);
        }
        if (options.isIncludeDefaultJs()) {
            body.appendElement("script")
                    .attr("src", createFilePath(relPathToOutputFolder, resourceMapping.get(DEFAULT_JS_NAME)));
        }
        for (Element element : additionalElements) {
            Element target = findTagetElement(element, doc);
            if (target != null) {
                target.appendChild(element.clone());
            } else {
                String parentChain = element.parents()
                        .stream()
                        .map(Element::tagName)
                        .collect(Collectors.joining("->"));
                throw new IllegalStateException("Can not add element '" + element + "' with parents: '" + parentChain + "' into the target document");
            }
        }
        return doc;
    }

    private static Element findTagetElement(Element element, Document doc) {
        Element target = doc;
        Elements parents = element.parents();
        for (int i = parents.size() - 1; i >= 0; i--) {
            Element e = parents.get(i);
            Optional<Element> find = target.children()
                    .stream()
                    .filter(c -> Objects.equals(e.tagName(), c.tagName()))
                    .findFirst();
            if (find.isPresent()) {
                target = find.get();
            } else {
                return null;
            }
        }
        return target;
    }

    private static void appendNavList(int level, List<PageHolder> pages, PageHolder current, Element parent) {
        Element ul = parent.appendElement("ul")
                .addClass("nav-list");
        for (PageHolder page : pages) {
            Element li = ul.appendElement("li")
                    .addClass("nav-item")
                    .attr("data-depth", "" + level);
            String calculatedState = calculateState(page, current);
            if (calculatedState != null) {
                li.addClass("is-active");
                li.addClass(calculatedState);
            }

            if (!page.getChildren()
                    .isEmpty()) {
                li.appendElement("button")
                        .addClass("nav-item-toggle");
            }

            if (page.getOutputFile() != null) {
                li.appendElement("a")
                        .addClass("nav-link")
                        .attr("href", page.getHrefValue(current.getOutputFile()))
                        .text(page.getTitle());
            } else {
                li.appendElement("span")
                        .addClass("nav-text")
                        .text(page.getTitle());
            }

            if (!page.getChildren()
                    .isEmpty()) {
                appendNavList(level + 1, page.getChildren(), current, li);
            }
        }
    }

    private static String calculateState(PageHolder page, PageHolder current) {
        if (Objects.equals(page.getOutputFile(), current.getOutputFile())) {
            return "is-current-page";
        }
        PageHolder parent = current.getParent();
        while (parent != null) {
            if (Objects.equals(page.getOutputFile(), parent.getOutputFile())) {
                return "is-current-path";
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static List<PageHolder> calculateBreadcrumbsList(PageHolder current) {
        List<PageHolder> list = new ArrayList<>();
        PageHolder p = current;
        while (p.getParent() != null) {
            list.add(0, p);
            p = p.getParent();
        }
        if (!p.isUniqueRoot()) {
            list.add(0, p);
        }
        return list;
    }

    static void createContentToc(ConfigurationOptions options, Element elementToInsert, int hLevel, int dataLevel, int dataLevelEnd, Element content) {
        Element div = content.appendElement("aside")
                .addClass("toc")
                .addClass("sidebar")
                .attr("data-title", "Contents")
                .attr("data-levels", "" + dataLevelEnd)
                .appendElement("div")
                .addClass("toc-menu");

        if (options.isCreateToc()) {
            div.appendElement("h3")
                    .text("Contents");
            Element tocUl = div.appendElement("ul");

            Elements elements = elementToInsert.getAllElements();
            for (Element element : elements) {
                int hMin = Math.max(1, hLevel);
                int hMax = Math.min(6, hLevel + dataLevelEnd - dataLevel);
                if (element.nodeName()
                        .matches("h[" + hMin + "-" + hMax + "]")) {
                    Optional<String> id = findId(element);
                    String anchor;
                    if (id.isPresent()) {
                        anchor = "#" + id.get();
                    } else {
                        System.err.println("id is not found for node " + element.nodeName() + " '" + element.text() + "'");
                        anchor = "#";
                    }

                    int level = Integer.parseInt(element.nodeName()
                            .substring(1)) - hLevel + dataLevel;

                    tocUl.appendElement("li")
                            .attr("data-level", "" + level)
                            .appendElement("a")
                            .attr("href", anchor)
                            .text(element.text());
                }
            }
        }
    }

    static Optional<String> findId(Element element) {
        Optional<String> findId = findIdForElement(element);
        if (findId.isPresent()) {
            return findId;
        }

        Elements childElements = element.getElementsByTag("a");
        for (Element child : childElements) {
            Optional<String> findIdOfChild = findIdForElement(child);
            if (findIdOfChild.isPresent()) {
                return findIdOfChild;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findIdForElement(Element element) {
        if (element.id() != null && !element.id()
                .isEmpty()) {
            return Optional.of(element.id());
        }
        return Optional.empty();
    }

    private static Map<String, String> copyResources(Parameters param) {
        ConfigurationOptions options = param.getOptions();

        Map<String, String> resourceMapping = new HashMap<>();
        if (options.isIncludeDefaultJs()) {
            copyResource(param, options.getJavascriptOutputFolder(), DEFAULT_JS_NAME, resourceMapping);
        }
        if (options.isIncludeDefaultCss()) {
            for (String image : SITE_IMAGES) {
                copyResource(param, options.getImagesOutputFolder(), image, resourceMapping);
            }
            for (String font : SITE_FONTS) {
                copyResource(param, options.getFontOutputFolder(), font, resourceMapping);
            }
            String relativeFileName = createRelativeFilePath(options.getResourcesRewriteStrategy(), options.getCssOutputFolder(), DEFAULT_CSS_NAME, "dummyHash");
            Path root = Paths.get("");
            Path file = root.resolve(relativeFileName);
            String relativePathToRoot = relativizeToString(file.getParent(), root) + "/";

            UnaryOperator<String> modifier;
            if (Objects.equals("../", relativePathToRoot) && "images/".equals(options.getImagesOutputFolder()) && "font/".equals(options.getFontOutputFolder()) && options.getResourcesRewriteStrategy() == RewriteStrategy.NO_MODIFICATION) {
                modifier = null;
            } else {
                modifier = (content) -> {
                    String result = content;
                    for (String image : SITE_IMAGES) {
                        result = result.replace("images/" + image + ")", resourceMapping.get(image) + ")");
                    }
                    for (String font : SITE_FONTS) {
                        result = result.replace("font/" + font + ")", resourceMapping.get(font) + ")");
                    }
                    if (!Objects.equals("../", relativePathToRoot)) {
                        result = result.replace("url(../", "url(" + relativePathToRoot);
                    }
                    return result;
                };
            }
            copyResource(param, options.getCssOutputFolder(), DEFAULT_CSS_NAME, resourceMapping, modifier);
        }
        return resourceMapping;
    }

    private static void copyResource(Parameters param, String outputFolder, String resourceName, Map<String, String> mapping) {
        copyResource(param, outputFolder, resourceName, mapping, null);
    }

    private static void copyResource(Parameters param, String outputFolder, String resourceName, Map<String, String> mapping, UnaryOperator<String> modifier) {
        RewriteStrategy strategy = param.getOptions()
                .getResourcesRewriteStrategy();

        byte[] inBytes;
        try (InputStream inputStream = Impl.class.getResourceAsStream("/" + resourceName)) {
            inBytes = readBytes(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource " + resourceName, e);
        }

        byte[] outBytes;
        if (modifier != null) {
            String in = new String(inBytes, StandardCharsets.UTF_8);
            String out = modifier.apply(in);
            outBytes = out.getBytes(StandardCharsets.UTF_8);
        } else {
            outBytes = inBytes;
        }

        String fileHash = createFileHash(strategy, outBytes);
        String relativeFileName = createRelativeFilePath(strategy, outputFolder, resourceName, fileHash);
        mapping.put(resourceName, relativeFileName);
        Path toFile = param.getOutputRootFolder()
                .resolve(relativeFileName);

        try {
            if (!Files.exists(toFile)) {
                Files.createDirectories(toFile.getParent());
                Files.write(toFile, outBytes);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write file:'" + toFile + "' for resource '" + resourceName + "'", e);
        }
    }

    private static List<Element> moveAndCopy(Document doc, Path inputFile, Parameters param, String relPathToOutputFolder, String subPath, String tagName, Function<Element, Boolean> filter, String attributeName) throws IOException {
        List<Element> result = new ArrayList<>();

        Path outputRootFolder = param.getOutputRootFolder();
        RewriteStrategy strategy = param.getOptions()
                .getResourcesRewriteStrategy();

        Elements elements = doc.getElementsByTag(tagName);
        for (Element element : elements) {
            if (filter.apply(element)) {
                String attr = element.attr(attributeName);
                if (attr != null && !attr.isEmpty() && !isUrlAbsolute(attr)) {
                    //consider that the attribute is relative to the inputFile:
                    Path fromFile = inputFile.getParent()
                            .resolve(attr);
                    //if no file exists at this location, consider that the attribute contains an absolute path to the image:
                    if (!Files.exists(fromFile) || !Files.isRegularFile(fromFile)) {
                        fromFile = Paths.get(attr);
                    }
                    Path fromFileName = fromFile.getFileName();
                    String relativeFileName;
                    if (Files.isRegularFile(fromFile)) {
                        byte[] bytes;
                        try (InputStream is = Files.newInputStream(fromFile)) {
                            bytes = readBytes(is);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read file " + fromFile, e);
                        }
                        String fileHash = createFileHash(strategy, bytes);
                        relativeFileName = createRelativeFilePath(strategy, subPath, fromFileName.toString(), fileHash);
                        Path toFile = outputRootFolder.resolve(relativeFileName);
                        if (!Files.exists(toFile)) {
                            Files.createDirectories(toFile.getParent());
                            Files.copy(fromFile, toFile);
                        }
                    } else {
                        relativeFileName = createRelativeFilePath(strategy, subPath, fromFileName.toString(), null);
                        System.err.println("File '" + fromFile + "' is missing");
                    }
                    String newAttr = createFilePath(relPathToOutputFolder, relativeFileName);
                    element.attr(attributeName, newAttr);
                }
                result.add(element);
            }
        }
        return result;
    }

    static String createFileHash(RewriteStrategy strategy, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        switch (strategy) {
        case SHA1_SUFFIX:
        case SHA1_SUB_FOLDER:
            return toSHA1(bytes);
        case SHORT_SHA1_SUFFIX:
        case SHORT_SHA1_SUB_FOLDER: {
            return toSHA1(bytes).substring(0, 7);
        }
        case NO_MODIFICATION:
        default:
            return null;
        }
    }

    private static String toSHA1(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Can not compute SHA-1", e);
        }
        return byteToHex(md.digest(bytes));
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    static String createRelativeFilePath(RewriteStrategy strategy, String subPath, String fileName, String fileHash) {
        if (fileHash == null) {
            return subPath + fileName;
        }
        String subFilePath;
        switch (strategy) {
        case SHA1_SUFFIX:
        case SHORT_SHA1_SUFFIX: {
            subFilePath = insertFileHashBeforeExtension(fileName, fileHash);
            break;
        }
        case SHA1_SUB_FOLDER:
        case SHORT_SHA1_SUB_FOLDER: {
            subFilePath = fileHashAsFolder(fileName, fileHash);
            break;
        }
        case NO_MODIFICATION:
        default: {
            subFilePath = fileName;
            break;
        }
        }
        return subPath + subFilePath;
    }

    private static String insertFileHashBeforeExtension(String fileName, String fileHash) {
        int i = fileName.lastIndexOf('.');
        if (i > -1) {
            return fileName.substring(0, i) + "_" + fileHash + fileName.substring(i);
        } else {
            return fileName + "_" + fileHash;
        }
    }

    private static String fileHashAsFolder(String fileName, String token) {
        return token + "/" + fileName;
    }

    private static String createFilePath(String relPathToOutputFolder, String relativeFileName) {
        return relPathToOutputFolder + relativeFileName;
    }

    // Starting with Java 9, we can achieve the same with a dedicated method on InputStream:
    private static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    static void rewriteLinks(Document doc, Path inputFolder, Path inputFile, Path outputFolder, Path outputFile, List<PageHolder> pageHolders, LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        Elements elements = doc.getElementsByTag("a");
        for (Element element : elements) {
            String attr = element.attr("href");
            if (attr != null && !attr.isEmpty() && !isUrlAbsolute(attr)) {
                HrefHolder href = toHrefHolder(attr);
                if (!href.getPath()
                        .isEmpty()) {
                    boolean implicitIndex = href.getPath()
                            .endsWith("/");

                    //consider that the attribute is relative to the inputFile:
                    Path inputTargetFile = inputFile.getParent()
                            .resolve(href.getPath() + (implicitIndex ? "index.html" : ""))
                            .normalize()
                            .toAbsolutePath();

                    Path inputFolderAbsolute = inputFolder.normalize()
                            .toAbsolutePath();

                    //corresponding file:
                    Path outputTargetFile = pageHolders.stream()
                            .flatMap(PageHolder::flattened)
                            .filter(h -> {
                                Path absolutePath = h.getInputFile()
                                        .normalize()
                                        .toAbsolutePath();
                                return Objects.equals(absolutePath, inputTargetFile);
                            })
                            .findAny()
                            .map(PageHolder::getOutputFile)
                            .orElseGet(() -> {
                                //relative path to the input Folder:
                                Path inputRelPath = inputFolderAbsolute.relativize(inputTargetFile);

                                //corresponding location in the output folder:
                                return outputFolder.resolve(inputRelPath);
                            });

                    //relative path to the outFile is the new value for href:
                    String newAttr = createLinkHrefValue(outputFile, outputTargetFile, href.getAnchor(), linkToIndexHtmlStrategy);
                    element.attr("href", newAttr);
                }
            }
        }
    }

    static String createLinkHrefValue(Path outputFile, Path outputTargetFile, String anchor, LinkToIndexHtmlStrategy linkToIndexHtmlStrategy) {
        String hrefValue = relativizeToStringWithTrailingSlash(outputFile.getParent(), outputTargetFile);
        if (linkToIndexHtmlStrategy == LinkToIndexHtmlStrategy.TO_PARENT_FOLDER) {
            if (hrefValue.equals("index.html")) {
                hrefValue = "./";
            }
            if (hrefValue.endsWith("/index.html")) {
                hrefValue = hrefValue.substring(0, hrefValue.length() - "index.html".length());
            }
        }
        if (linkToIndexHtmlStrategy == LinkToIndexHtmlStrategy.TO_FILE) {
            if (hrefValue.equals("./")) {
                hrefValue = "index.html";
            }
            if (hrefValue.endsWith("/")) {
                hrefValue = hrefValue + "index.html";
            }

        }
        if (anchor != null) {
            hrefValue = hrefValue + anchor;
        }
        return hrefValue;
    }

    static boolean isUrlAbsolute(String url) {
        return url.matches("(?:^[a-z][a-z0-9+.-]*:|\\/\\/).+");
    }

    static String readFile(Path file) {
        String content;
        try {
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read file: " + file, e);
        }
        return content;
    }

    static void writeFile(Path file, String content) {
        try {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write file: " + file, e);
        }
    }

    static HrefHolder toHrefHolder(String href) {
        String fileName;
        String anchor;
        int index = href.lastIndexOf('#');
        if (index > -1) {
            fileName = href.substring(0, index);
            anchor = href.substring(index);
        } else {
            fileName = href;
            anchor = null;
        }
        return new HrefHolder(fileName, anchor);
    }

    static class HrefHolder {
        private String path;
        private String anchor;

        public HrefHolder(String path, String anchor) {
            super();
            this.path = path;
            this.anchor = anchor;
        }

        public String getPath() {
            return path;
        }

        public String getAnchor() {
            return anchor;
        }
    }

    static void writeCatalog(Parameters param, ConfigurationCatalog catalog) {
        List<Path> files;
        switch (catalog.getStrategy()) {
        case PUBLISH_OUTPUT:
            files = param.getPageHolders()
                    .stream()
                    .flatMap(PageHolder::flattened)
                    .filter(m -> m.isInputFileExists())
                    .map(m -> m.getOutputFile())
                    .collect(Collectors.toList());
            break;
        case SCAN_FOLDER:
            try (Stream<Path> walk = Files.walk(catalog.getFolder())) {
                files = walk
                        .filter(path -> isHtmlFile(path))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IllegalStateException("Could not walk folder: " + catalog.getFolder(), e);
            }
            break;
        default:
            throw new IllegalStateException("Unknown strategy: " + catalog.getStrategy());
        }

        List<String> catalogList = files.stream()
                .map(path -> relativizeToString(param.getOutputRootFolder(), path))
                .sorted()
                .collect(Collectors.toList());

        List<String> catalogOut;
        List<String> missing;
        if (catalog.getOutputAction() == OutputAction.REPLACE_EXISTING) {
            catalogOut = catalogList;
            missing = Collections.emptyList();
        } else {
            List<String> catalogExisting;
            if (Files.isRegularFile(catalog.getOutputFile())) {
                try {
                    catalogExisting = Files.readAllLines(catalog.getOutputFile());
                } catch (IOException e) {
                    throw new IllegalStateException("Can not read file: " + catalog.getOutputFile(), e);
                }
            } else {
                catalogExisting = new ArrayList<>();
            }

            if (catalog.getOutputAction() == OutputAction.MERGE_AND_FAIL_IF_ABSENT) {
                missing = catalogList.stream()
                        .filter(s -> !catalogExisting.contains(s))
                        .collect(Collectors.toList());
            } else {
                missing = Collections.emptyList();
            }

            Set<String> merge = new HashSet<>();
            merge.addAll(catalogList);
            merge.addAll(catalogExisting);
            catalogOut = merge.stream()
                    .sorted()
                    .collect(Collectors.toList());
        }

        String content = String.join("\n", catalogOut);
        try {
            Files.createDirectories(catalog.getOutputFile()
                    .getParent());
            Files.write(catalog.getOutputFile(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new IllegalStateException("Can not write file: " + catalog.getOutputFile(), e);
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("The output file '" + catalog.getOutputFile() + "' should contains following entries:\n" + String.join("\n", missing));
        }
    }

    static String relativizeToString(Path rootFolder, Path p) {
        return rootFolder.relativize(p)
                .toString()
                .replace('\\', '/');
    }

    static String relativizeToStringWithTrailingSlash(Path rootFolder, Path p) {
        String result = relativizeToString(rootFolder, p);
        if (result.isEmpty()) {
            return "./";
        }
        if (Files.isDirectory(p)) {
            return result + "/";
        }
        return result;
    }

    private static boolean isHtmlFile(Path path) {
        return path.toFile()
                .isFile()
                && endsWithHtml(path.toFile()
                        .getName());
    }

    private static boolean endsWithHtml(String name) {
        return name.endsWith(".html");
    }

}
