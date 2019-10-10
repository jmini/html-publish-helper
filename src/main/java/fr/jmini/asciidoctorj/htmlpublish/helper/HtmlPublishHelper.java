package fr.jmini.asciidoctorj.htmlpublish.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlPublishHelper {
    private static final String IMAGES_SUB_PATH = "images/";
    private static final String JAVASCIPT_SUB_PATH = "js/";
    private static final String CSS_SUB_PATH = "css/";

    /**
     * Take all HTML files of a folder and sub-folders and publish them to an output folder. Images, CSS and JavaScript resources are moved.
     *
     * @param inputFolder
     *            root folder where the input HTML files are located.
     * @param outputFolder
     *            folder where the post-processed HTML files are saved.
     */
    public static void publishHtmlFolder(Path inputFolder, Path outputFolder) {
        try {
            Files.walk(inputFolder)
                    .filter(f -> f.toFile()
                            .isFile()
                            && f.toFile()
                                    .getName()
                                    .endsWith("html"))
                    .forEach(f -> publishHtmlFile(inputFolder, f, outputFolder));
        } catch (IOException e) {
            System.err.println("Could not walk through '" + inputFolder + "'.");
        }
    }

    /**
     * Take a list of HTML files in a folder and publish them to an output folder. Images, CSS and JavaScript resources are moved.
     *
     * @param inputFolder
     *            root folder where the input HTML files are located.
     * @param outputFolder
     *            folder where the post-processed HTML files are saved.
     * @param files
     *            relative path to the root folders. If a <code>:</code> is used, then you can specify different relative path for input and output files.
     */
    public static void publishHtmlFilesInFolder(Path inputFolder, Path outputFolder, String... files) {
        List<PathHolder> fileMappings = Arrays.stream(files)
                .map(subPath -> toPathHolder(inputFolder, outputFolder, subPath))
                .collect(Collectors.toList());
        fileMappings.stream()
                .forEach(holder -> publishHtmlFile(inputFolder, holder.inputFile, outputFolder, holder.outputFile, fileMappings));
    }

    /**
     * Take a single HTML file and publish it an output folder. Images, CSS and JavaScript resources are moved.
     *
     * @param inputFolder
     *            root folder where the input HTML file is located.
     * @param inputFile
     *            input HTML that is published.
     * @param outputFolder
     *            directory where the post-processed HTML file is saved.
     */
    public static void publishHtmlFile(Path inputFolder, Path inputFile, Path outputFolder) {
        Path inputRelPath = inputFolder.relativize(inputFile);
        Path outputFile = outputFolder.resolve(inputRelPath);
        publishHtmlFile(inputFolder, inputFile, outputFolder, outputFile);
    }

    /**
     * Take a single HTML file and publish it an output folder. Images, CSS and JavaScript resources are moved.
     *
     * @param inputFolder
     *            root folder where the input HTML file is located.
     * @param inputFile
     *            input HTML that is published.
     * @param outputFolder
     *            directory where the post-processed HTML file is saved.
     * @param outputFile
     *            output HTML file.
     */
    public static void publishHtmlFile(Path inputFolder, Path inputFile, Path outputFolder, Path outputFile) {
        publishHtmlFile(inputFolder, inputFile, outputFolder, outputFile, Collections.singletonList(new PathHolder(inputFile, outputFile)));
    }

    static void publishHtmlFile(Path inputFolder, Path inputFile, Path outputFolder, Path outputFile, List<PathHolder> fileMappings) {
        String relPathToOutputFolder = outputFile.getParent()
                .relativize(outputFolder)
                .toString();
        if (!relPathToOutputFolder.isEmpty()) {
            relPathToOutputFolder = relPathToOutputFolder + "/";
        }

        String html = readFile(inputFile);
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .charset("ASCII");

        try {
            Files.createDirectories(outputFile.getParent());
            moveAndCopy(doc, inputFile, outputFolder, relPathToOutputFolder, IMAGES_SUB_PATH, "img", (e) -> true, "src");
            moveAndCopy(doc, inputFile, outputFolder, relPathToOutputFolder, CSS_SUB_PATH, "link", (e) -> "stylesheet".equalsIgnoreCase(e.attr("rel")), "href");
            moveAndCopy(doc, inputFile, outputFolder, relPathToOutputFolder, JAVASCIPT_SUB_PATH, "script", (e) -> true, "src");
        } catch (IOException e) {
            throw new IllegalStateException("Could move file: " + inputFile, e);
        }

        Path inputRelPath = inputFolder.getParent()
                .relativize(inputFile);
        Path outputRelPath = outputFolder.getParent()
                .relativize(outputFile);
        if (!Objects.equals(inputRelPath, outputRelPath)) {
            rewriteLinks(doc, inputFolder, inputFile, outputFolder, outputFile, fileMappings);
        }

        String content = doc.toString();
        writeFile(outputFile, content);
    }

    private static void moveAndCopy(Document doc, Path inputFile, Path outputFolder, String relPathToOutputFolder, String subPath, String tagName, Function<Element, Boolean> filter, String attributeName) throws IOException {
        Elements elements = doc.getElementsByTag(tagName);
        for (Element element : elements) {
            if (filter.apply(element)) {
                String attr = element.attr(attributeName);
                if (attr != null && !attr.isEmpty() && !attr.startsWith("http://") && !attr.startsWith("https://") && !attr.startsWith("data")) {
                    //consider that the attribute is relative to the inputFile:
                    Path fromFile = inputFile.getParent()
                            .resolve(attr);
                    //if no file exists at this location, consider that the attribute contains an absolute path to the image:
                    if (!Files.exists(fromFile) || !Files.isRegularFile(fromFile)) {
                        fromFile = Paths.get(attr);
                    }
                    String newAttr = relPathToOutputFolder + subPath + fromFile.getFileName();
                    element.attr(attributeName, newAttr);
                    Path toFile = outputFolder.resolve(subPath)
                            .resolve(fromFile.getFileName());
                    if (Files.isRegularFile(fromFile)) {
                        if (!Files.exists(toFile)) {
                            Files.createDirectories(toFile.getParent());
                            Files.copy(fromFile, toFile);
                        }
                    } else {
                        System.err.println("File '" + fromFile + "' is missing");
                    }
                }
            }
        }
    }

    static void rewriteLinks(Document doc, Path inputFolder, Path inputFile, Path outputFolder, Path outputFile, List<PathHolder> fileMappings) {
        Elements elements = doc.getElementsByTag("a");
        for (Element element : elements) {
            String attr = element.attr("href");
            if (attr != null && !attr.isEmpty() && !attr.startsWith("http://") && !attr.startsWith("https://") && !attr.startsWith("file:")) {
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
                    Path outputTargetFile = fileMappings.stream()
                            .filter(h -> {
                                Path absolutePath = h.getInputFile()
                                        .normalize()
                                        .toAbsolutePath();
                                return Objects.equals(absolutePath, inputTargetFile);
                            })
                            .findAny()
                            .map(PathHolder::getOutputFile)
                            .orElseGet(() -> {
                                //relative path to the input Folder:
                                Path inputRelPath = inputFolderAbsolute.relativize(inputTargetFile);

                                //corresponding location in the output folder:
                                return outputFolder.resolve(inputRelPath);
                            });

                    //relative path to the outFile is the new value for href:
                    String newAttr = outputFile.getParent()
                            .relativize(outputTargetFile)
                            .toString();
                    if (implicitIndex && newAttr.endsWith("/index.html")) {
                        newAttr = newAttr.substring(0, newAttr.length() - "index.html".length());
                    }
                    if (href.getAnchor() != null) {
                        newAttr = newAttr + href.getAnchor();
                    }
                    element.attr("href", newAttr);
                }
            }
        }
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

    static PathHolder toPathHolder(Path inputFolder, Path outputFolder, String subPath) {
        String inputSubPath;
        String outputSubPath;
        int index = subPath.indexOf(':');
        if (index > -1) {
            inputSubPath = subPath.substring(0, index);
            outputSubPath = subPath.substring(index + 1);
        } else {
            inputSubPath = subPath;
            outputSubPath = subPath;
        }
        return new PathHolder(inputFolder.resolve(inputSubPath), outputFolder.resolve(outputSubPath));
    }

    static class PathHolder {
        private Path inputFile;
        private Path outputFile;

        public PathHolder(Path inputFile, Path outputFile) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        public Path getInputFile() {
            return inputFile;
        }

        public Path getOutputFile() {
            return outputFile;
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

}
