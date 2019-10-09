package fr.jmini.asciidoctorj.htmlpublish.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import fr.jmini.asciidoctorj.htmlpublish.helper.HtmlPublishHelper.PathHolder;

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
        assertThat(content).contains("<script src=\"js/empty.js\"></script>");
        assertThat(content).doesNotContain("<script src=\"js/\">");
        assertThat(content).contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">");
        assertThat(content).contains("<a href=\"folder/page.html\">Other page</a>");
    }

    @Test
    void testCase1ToOtherFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder, outputFolder.resolve("other/page.html"));

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── file.css
        // ├── images
        // │   ├── image.png
        // │   └── img.svg
        // ├── other
        // │   └── page.html
        // └── js
        //     └── empty.js

        assertThat(outputFolder).isDirectory();
        Path indexFile = outputFolder.resolve("index.html");
        assertThat(indexFile).doesNotExist();
        Path pageFile = outputFolder.resolve("other/page.html");
        assertThat(pageFile).isRegularFile();
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

        String content = HtmlPublishHelper.readFile(pageFile);
        assertThat(content).contains("<img src=\"../images/image.png\" alt=\"a test png image\">");
        assertThat(content).contains("<img src=\"../images/img.svg\" alt=\"a test svg image\">");
        assertThat(content).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/file.css\">");
        assertThat(content).contains("<script src=\"../js/empty.js\"></script>");
        assertThat(content).doesNotContain("<script src=\"js/\">");
        assertThat(content).contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">");
        assertThat(content).contains("<a href=\"../folder/page.html\">Other page</a>");
    }

    @Test
    void testCase1ToFolderFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder, outputFolder.resolve("folder/index.html"));

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── file.css
        // ├── images
        // │   ├── image.png
        // │   └── img.svg
        // ├── folder
        // │   └── page.html
        // └── js
        //     └── empty.js

        assertThat(outputFolder).isDirectory();
        Path indexFile = outputFolder.resolve("index.html");
        assertThat(indexFile).doesNotExist();
        Path pageFile = outputFolder.resolve("folder/index.html");
        assertThat(pageFile).isRegularFile();
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

        String content = HtmlPublishHelper.readFile(pageFile);
        assertThat(content).contains("<img src=\"../images/image.png\" alt=\"a test png image\">");
        assertThat(content).contains("<img src=\"../images/img.svg\" alt=\"a test svg image\">");
        assertThat(content).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/file.css\">");
        assertThat(content).contains("<script src=\"../js/empty.js\"></script>");
        assertThat(content).doesNotContain("<script src=\"js/\">");
        assertThat(content).contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">");
        assertThat(content).contains("<a href=\"page.html\">Other page</a>");
    }

    @Test
    void testCase1ToSubOtherFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder, outputFolder.resolve("sub/other/page.html"));

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── file.css
        // ├── images
        // │   ├── image.png
        // │   └── img.svg
        // ├── sub
        // │   └── other
        // │       └── page.html
        // └── js
        //     └── empty.js

        assertThat(outputFolder).isDirectory();
        Path indexFile = outputFolder.resolve("index.html");
        assertThat(indexFile).doesNotExist();
        Path pageFile = outputFolder.resolve("sub/other/page.html");
        assertThat(pageFile).isRegularFile();
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

        String content = HtmlPublishHelper.readFile(pageFile);
        assertThat(content).contains("<img src=\"../../images/image.png\" alt=\"a test png image\">");
        assertThat(content).contains("<img src=\"../../images/img.svg\" alt=\"a test svg image\">");
        assertThat(content).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../css/file.css\">");
        assertThat(content).contains("<script src=\"../../js/empty.js\"></script>");
        assertThat(content).doesNotContain("<script src=\"js/\">");
        assertThat(content).contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">");
        assertThat(content).contains("<a href=\"../../folder/page.html\">Other page</a>");
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
        assertThat(content1).contains("<a href=\"sub/page2.html\">Page 2</a>");

        String content2 = HtmlPublishHelper.readFile(page2);
        assertThat(content2).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ");
        assertThat(content2).contains("<a href=\"../page1.html\">Page 1</a>");
    }

    @Test
    void testCase2ReworkOutputTree() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFilesInFolder(CASE2_FOLDER, outputFolder, "page1.html:other/p1.html", "sub/page2.html:other/p2.html");

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── page.css
        // ├── images
        // │   └── img.svg
        // ├── js
        // │   └── empty.js
        // └── other
        //    ├── p1.html
        //    └── p2.html

        assertThat(outputFolder).isDirectory();
        Path page1 = outputFolder.resolve("page1.html");
        assertThat(page1).doesNotExist();
        Path page2 = outputFolder.resolve("sub")
                .resolve("page2.html");
        assertThat(page2).doesNotExist();
        Path p1 = outputFolder.resolve("other")
                .resolve("p1.html");
        assertThat(p1).isRegularFile();
        Path p2 = outputFolder.resolve("other")
                .resolve("p2.html");
        assertThat(p2).isRegularFile();
        Path css = outputFolder.resolve("css");
        assertThat(css).isDirectory();
        assertThat(css.resolve("page.css")).isRegularFile();
        Path images = outputFolder.resolve("images");
        assertThat(images).isDirectory();
        assertThat(images.resolve("img.svg")).isRegularFile();
        Path js = outputFolder.resolve("js");
        assertThat(js).isDirectory();
        assertThat(js.resolve("empty.js")).isRegularFile();

        String content1 = HtmlPublishHelper.readFile(p1);
        assertThat(content1).contains("<p><img src=\"../images/img.svg\" alt=\"a test image\"></p>");
        assertThat(content1).contains("<script type=\"text/javascript\" src=\"../js/empty.js\"></script>");
        assertThat(content1).contains("<a href=\"p2.html\">Page 2</a>");

        String content2 = HtmlPublishHelper.readFile(p2);
        assertThat(content2).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ");
        assertThat(content2).contains("<a href=\"p1.html\">Page 1</a>");
    }

    @Test
    void testToPathHolder() throws Exception {
        Path inputFolder = Paths.get("input");
        Path outputFolder = Paths.get("output");

        PathHolder holder1 = HtmlPublishHelper.toPathHolder(inputFolder, outputFolder, "file.html");
        assertThat(holder1.getInputFile()).isEqualTo(inputFolder.resolve("file.html"));
        assertThat(holder1.getOutputFile()).isEqualTo(outputFolder.resolve("file.html"));

        PathHolder holder2 = HtmlPublishHelper.toPathHolder(inputFolder, outputFolder, "page.html:index.html");
        assertThat(holder2.getInputFile()).isEqualTo(inputFolder.resolve("page.html"));
        assertThat(holder2.getOutputFile()).isEqualTo(outputFolder.resolve("index.html"));
    }
}
