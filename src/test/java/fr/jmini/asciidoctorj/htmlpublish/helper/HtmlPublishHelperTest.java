package fr.jmini.asciidoctorj.htmlpublish.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class HtmlPublishHelperTest {
    private static final Path CASE1_FOLDER = Paths.get("src/test/resources/input/case1");
    private static final Path CASE1_FILE = CASE1_FOLDER.resolve("index.html");
    private static final Path CASE2_FOLDER = Paths.get("src/test/resources/input/case2/");

    @Test
    void testCase1() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder);

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── file.css
        // ├── images
        // │   ├── image.png
        // │   └── img.svg
        // ├── index.html
        // └── js
        //     └── empty.js

        assertThat(outputFolder).isDirectory();
        Path indexFile = outputFolder.resolve("index.html");
        assertThat(indexFile).isRegularFile();
        Path css = outputFolder.resolve("css");
        assertThat(css).isDirectory();
        assertThat(css.resolve("file.css")).isRegularFile();
        Path images = outputFolder.resolve("images");
        assertThat(images).isDirectory();
        assertThat(images.resolve("image.png")).isRegularFile();
        assertThat(images.resolve("img.svg")).isRegularFile();
        Path js = outputFolder.resolve("js");
        assertThat(js).isDirectory();
        assertThat(js.resolve("empty.js")).isRegularFile();

        String content = HtmlPublishHelper.readFile(indexFile);
        assertThat(content).contains("<img src=\"images/image.png\" alt=\"a test png image\">");
        assertThat(content).contains("<img src=\"images/img.svg\" alt=\"a test svg image\">");
        assertThat(content).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/file.css\">");
        assertThat(content).contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>");
    }

    @Test
    void testCase2() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFolder(CASE2_FOLDER, outputFolder);

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── page.css
        // ├── images
        // │   └── img.svg
        // ├── js
        // │   └── empty.js
        // ├── page1.html
        // └── sub
        //    └── page2.html

        assertThat(outputFolder).isDirectory();
        Path page1 = outputFolder.resolve("page1.html");
        assertThat(page1).isRegularFile();
        Path page2 = outputFolder.resolve("sub")
                .resolve("page2.html");
        assertThat(page2).isRegularFile();
        Path css = outputFolder.resolve("css");
        assertThat(css).isDirectory();
        assertThat(css.resolve("page.css")).isRegularFile();
        Path images = outputFolder.resolve("images");
        assertThat(images).isDirectory();
        assertThat(images.resolve("img.svg")).isRegularFile();
        Path js = outputFolder.resolve("js");
        assertThat(js).isDirectory();
        assertThat(js.resolve("empty.js")).isRegularFile();

        String content1 = HtmlPublishHelper.readFile(page1);
        assertThat(content1).contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>");
        assertThat(content1).contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>");

        String content2 = HtmlPublishHelper.readFile(page2);
        assertThat(content2).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/page.css\"> ");
    }

}
