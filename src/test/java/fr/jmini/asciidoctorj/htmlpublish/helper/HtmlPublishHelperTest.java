package fr.jmini.asciidoctorj.htmlpublish.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.jmini.asciidoctorj.htmlpublish.helper.HtmlPublishHelper.HrefHolder;
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
        assertThat(content).contains("<a href=\"folder/external.html\">ext page</a>");
        assertThat(content).contains("<a href=\"folder/external.html#top\">top section</a>");
        assertThat(content).contains("<a href=\"references/\">references</a>");
        assertThat(content).contains("<a href=\"references/#property\">referenced property</a>");
        assertThat(content).contains("<a href=\"references/private.html\">private references</a>");
        assertThat(content).contains("<a href=\"#title\">this page</a>");
        assertThat(content).contains("<a href=\"http://github.com/jmini\">");
        assertThat(content).contains("<a href=\"https://github.com/jmini\">");
        assertThat(content).contains("<a href=\"file:///tmp/file.txt\">file.txt</a>");
        assertThat(content).contains("<a href=\"mailto:info@company.com\">");
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
        assertThat(content).contains("<a href=\"../folder/external.html\">ext page</a>");
        assertThat(content).contains("<a href=\"../folder/external.html#top\">top section</a>");
        assertThat(content).contains("<a href=\"../references/\">references</a>");
        assertThat(content).contains("<a href=\"../references/#property\">referenced property</a>");
        assertThat(content).contains("<a href=\"../references/private.html\">private references</a>");
        assertThat(content).contains("<a href=\"#title\">this page</a>");
    }

    @Test
    void testCase1ToFolderFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        Path outputFile = outputFolder.resolve("folder/page.html");
        List<PathHolder> fileMappings = Arrays.asList(
                new PathHolder(CASE1_FILE, outputFile),
                new PathHolder(CASE1_FOLDER.resolve("references/index.html"), outputFolder.resolve("folder/index.html")),
                new PathHolder(CASE1_FOLDER.resolve("references/private.html"), outputFolder.resolve("folder/p.html")));
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder, outputFile, fileMappings);

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
        Path pageFile = outputFolder.resolve("folder/page.html");
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
        assertThat(content).contains("<a href=\"external.html\">ext page</a>");
        assertThat(content).contains("<a href=\"external.html#top\">top section</a>");
        assertThat(content).contains("<a href=\"index.html\">references</a>");
        assertThat(content).contains("<a href=\"index.html#property\">referenced property</a>");
        assertThat(content).contains("<a href=\"p.html\">private references</a>");
        assertThat(content).contains("<a href=\"#title\">this page</a>");
    }

    @Test
    void testCase1ToSubOtherFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        Path outputFile = outputFolder.resolve("sub/other/page.html");
        List<PathHolder> fileMappings = Arrays.asList(
                new PathHolder(CASE1_FILE, outputFile),
                new PathHolder(CASE1_FOLDER.resolve("references/index.html"), outputFolder.resolve("ref/ref.html")),
                new PathHolder(CASE1_FOLDER.resolve("references/private.html"), outputFolder.resolve("sub/private/ref.html")));
        HtmlPublishHelper.publishHtmlFile(CASE1_FOLDER, CASE1_FILE, outputFolder, outputFile, fileMappings);

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
        Path pageFile = outputFile;
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
        assertThat(content).contains("<a href=\"../../folder/external.html\">ext page</a>");
        assertThat(content).contains("<a href=\"../../folder/external.html#top\">top section</a>");
        assertThat(content).contains("<a href=\"../../ref/ref.html\">references</a>");
        assertThat(content).contains("<a href=\"../../ref/ref.html#property\">referenced property</a>");
        assertThat(content).contains("<a href=\"../private/ref.html\">private references</a>");
        assertThat(content).contains("<a href=\"#title\">this page</a>");
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
        //     └── page2.html

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
        //     ├── p1.html
        //     └── p2.html

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
    void testCase2ReworkOutputTreeWithFolder() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFilesInFolder(CASE2_FOLDER, outputFolder, "page1.html:p1.html", "sub:other");

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── page.css
        // ├── images
        // │   └── img.svg
        // ├── js
        // │   └── empty.js
        // ├── p1.html
        // └── other
        //     └── page2.html

        assertThat(outputFolder).isDirectory();
        Path page1 = outputFolder.resolve("page1.html");
        assertThat(page1).doesNotExist();
        Path subPage2 = outputFolder.resolve("sub")
                .resolve("page2.html");
        assertThat(subPage2).doesNotExist();
        Path p1 = outputFolder.resolve("p1.html");
        assertThat(p1).isRegularFile();
        Path page2 = outputFolder.resolve("other")
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

        String content1 = HtmlPublishHelper.readFile(p1);
        assertThat(content1).contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>");
        assertThat(content1).contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>");
        assertThat(content1).contains("<a href=\"other/page2.html\">Page 2</a>");

        String content2 = HtmlPublishHelper.readFile(page2);
        assertThat(content2).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ");
        assertThat(content2).contains("<a href=\"../p1.html\">Page 1</a>");
    }

    @Test
    void testCase2ReworkOutputTreeEnsureRewrittenLinks() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        HtmlPublishHelper.publishHtmlFilesInFolder(CASE2_FOLDER, outputFolder, "page1.html:p1.html", "sub/page2.html");

        // expected tree:
        // outputFolder
        // ├── css
        // │   └── page.css
        // ├── images
        // │   └── img.svg
        // ├── js
        // │   └── empty.js
        // ├── p1.html
        // └── sub
        //     └── page2.html

        assertThat(outputFolder).isDirectory();
        Path page1 = outputFolder.resolve("page1.html");
        assertThat(page1).doesNotExist();
        Path p1 = outputFolder.resolve("p1.html");
        assertThat(p1).isRegularFile();
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

        String content1 = HtmlPublishHelper.readFile(p1);
        assertThat(content1).contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>");
        assertThat(content1).contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>");
        assertThat(content1).contains("<a href=\"sub/page2.html\">Page 2</a>");

        String content2 = HtmlPublishHelper.readFile(page2);
        assertThat(content2).contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ");
        assertThat(content2).contains("<a href=\"../p1.html\">Page 1</a>");
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

    @Test
    void testToHrefHolder() throws Exception {
        HrefHolder holder1 = HtmlPublishHelper.toHrefHolder("folder/page.html#anchor");
        assertThat(holder1.getPath()).isEqualTo("folder/page.html");
        assertThat(holder1.getAnchor()).isEqualTo("#anchor");

        HrefHolder holder2 = HtmlPublishHelper.toHrefHolder("folder/page.html");
        assertThat(holder2.getPath()).isEqualTo("folder/page.html");
        assertThat(holder2.getAnchor()).isNull();

        HrefHolder holder3 = HtmlPublishHelper.toHrefHolder("folder/page.html#");
        assertThat(holder3.getPath()).isEqualTo("folder/page.html");
        assertThat(holder3.getAnchor()).isEqualTo("#");

        HrefHolder holder4 = HtmlPublishHelper.toHrefHolder("");
        assertThat(holder4.getPath()).isEqualTo("");
        assertThat(holder4.getAnchor()).isNull();

        HrefHolder holder5 = HtmlPublishHelper.toHrefHolder("#section");
        assertThat(holder5.getPath()).isEqualTo("");
        assertThat(holder5.getAnchor()).isEqualTo("#section");
    }

    @Test
    void testIsUrlAbsolute() throws Exception {
        assertThat(HtmlPublishHelper.isUrlAbsolute("https://example.com")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("https://example.com/test/index.html")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("http://test.example.com")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("//example.com")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("//example.com/test/index.html")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("ftp://ftp.example.com")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("mailto:info@test.com")).isTrue();
        assertThat(HtmlPublishHelper.isUrlAbsolute("vscode:/file/tmp/file.txt")).isTrue();

        assertThat(HtmlPublishHelper.isUrlAbsolute("test/index.html")).isFalse();
        assertThat(HtmlPublishHelper.isUrlAbsolute("/test/index.html")).isFalse();
    }
}
