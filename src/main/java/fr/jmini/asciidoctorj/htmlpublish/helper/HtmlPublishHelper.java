package fr.jmini.asciidoctorj.htmlpublish.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

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
     *            root folder where the input HTML file is located.
     * @param outputFolder
     *            directory where the post-processed HTML file is saved.
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
            moveAndCopy(doc, inputFile, outputFolder, relPathToOutputFolder, JAVASCIPT_SUB_PATH, "script", (e) -> "text/javascript".equalsIgnoreCase(e.attr("type")), "src");
        } catch (IOException e) {
            throw new IllegalStateException("Could move file: " + inputFile, e);
        }

        String content = doc.toString();
        writeFile(outputFile, content);
    }

    private static void moveAndCopy(Document doc, Path inputFile, Path outputFolder, String relPathToOutputFolder, String subPath, String tagName, Function<Element, Boolean> filter, String attributeName) throws IOException {
        Elements elements = doc.getElementsByTag(tagName);
        for (Element element : elements) {
            if (filter.apply(element)) {
                String attr = element.attr(attributeName);
                if (attr != null && !attr.startsWith("http://") && !attr.startsWith("https://")) {
                    //consider that the attribute is relative to the inputFile:
                    Path fromFile = inputFile.getParent()
                            .resolve(attr);
                    //if no file exists at this location, consider that the attribute contains an absolute path to the image:
                    if (!Files.exists(fromFile) || !Files.isRegularFile(fromFile)) {
                        fromFile = Paths.get(attr);
                    }
                    String newSrc = relPathToOutputFolder + subPath + fromFile.getFileName();
                    element.attr(attributeName, newSrc);
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

}
