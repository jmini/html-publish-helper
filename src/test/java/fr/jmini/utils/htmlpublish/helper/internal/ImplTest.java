package fr.jmini.utils.htmlpublish.helper.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog;
import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog.OutputAction;
import fr.jmini.utils.htmlpublish.helper.ConfigurationCatalog.Strategy;
import fr.jmini.utils.htmlpublish.helper.ConfigurationHolder;
import fr.jmini.utils.htmlpublish.helper.ConfigurationOptions;
import fr.jmini.utils.htmlpublish.helper.ConfigurationPage;
import fr.jmini.utils.htmlpublish.helper.ConfigurationPageOptions;
import fr.jmini.utils.htmlpublish.helper.IndexHandling;
import fr.jmini.utils.htmlpublish.helper.RewriteStrategy;
import fr.jmini.utils.htmlpublish.helper.internal.Impl.HrefHolder;

class ImplTest {
    private static final Path INPUT_FOLDER = Paths.get("src/test/resources/input");
    private static final Path CASE1_FOLDER = INPUT_FOLDER.resolve("case1");
    private static final String CASE1_FILE = "index.html";
    private static final Path CASE2_FOLDER = INPUT_FOLDER.resolve("case2");
    private static final Path CASE3_FOLDER = INPUT_FOLDER.resolve("case3");
    private static final String CASE3_ONE = "one.html";
    private static final String CASE3_TWO = "two.html";
    private static final String CASE3_THREE = "three.html";
    private static final String CASE3_FOUR = "four.html";
    private static final String CASE3_CHAPTER1 = "chapter1";
    private static final String CASE3_CHAPTER1_INDEX = "chapter1/index.html";
    private static final String CASE3_CHAPTER1_SEC1 = "chapter1/sec1.html";
    private static final String CASE3_CHAPTER1_SEC5 = "chapter1/sec5.html";
    private static final String CASE3_CHAPTER1_SEC10 = "chapter1/sec10.html";
    private static final String CASE3_CHAPTER2 = "chapter2";
    private static final String CASE3_CHAPTER2_INDEX = "chapter2/index.html";
    private static final String CASE3_CHAPTER2_SUB_B = "chapter2/sub-b/index.html";
    private static final String CASE3_CHAPTER2_SUB_A = "chapter2/sub-a/index.html";

    @Test
    void testConfig() throws Exception {
        //tag::configuration[]
        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(Paths.get("inputFolder"))
                .outputRootFolder(Paths.get("outputFolder"))
                .addPage(new ConfigurationPage()
                        .input("case1/index.html")
                        .output("page.html"))
                .options(new ConfigurationOptions()
                        .resourcesRewriteStrategy(RewriteStrategy.SHORT_SHA1_SUFFIX)
                        .cssOutputFolder("static/css/")
                        .javascriptOutputFolder("static/scripts/")
                        .imagesOutputFolder("static/images/")
                        .fontOutputFolder("static/fonts/"));
        //end::configuration[]

        assertThat(config).isNotNull();
    }

    @Test
    void testCase1() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        assertThat(outputFolder).isDirectory();

        Path catalog1 = outputFolder.resolve("meta")
                .resolve("catalog.txt");
        assertThat(catalog1).doesNotExist();

        Path catalog2 = Files.createTempFile("test", ".txt");
        Files.write(catalog2, "extra.html".getBytes(StandardCharsets.UTF_8));

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE1_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage().input(CASE1_FILE))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog2)
                        .outputAction(OutputAction.MERGE_SILENTLY));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──file.css\n"
                + "├──images\n"
                + "│   ├──image.png\n"
                + "│   └──img.svg\n"
                + "├──index.html\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "└──meta\n"
                + "    └──catalog.txt\n");

        assertThat(catalog1).hasContent("index.html");
        assertThat(catalog2).hasContent("extra.html\nindex.html");

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

        String content = Impl.readFile(indexFile);
        assertThat(content).isNotEmpty()
                .contains("<img src=\"images/image.png\" alt=\"a test png image\">")
                .contains("<img src=\"images/img.svg\" alt=\"a test svg image\">")
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/file.css\">")
                .contains("<script src=\"js/empty.js\"></script>")
                .doesNotContain("<script src=\"js/\">")
                .contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">")
                .contains("<a href=\"folder/external.html\">ext page</a>")
                .contains("<a href=\"folder/external.html#top\">top section</a>")
                .contains("<a href=\"references/\">references</a>")
                .contains("<a href=\"references/#property\">referenced property</a>")
                .contains("<a href=\"references/private.html\">private references</a>")
                .contains("<a href=\"#title\">this page</a>")
                .contains("<a href=\"http://github.com/jmini\">")
                .contains("<a href=\"https://github.com/jmini\">")
                .contains("<a href=\"file:///tmp/file.txt\">file.txt</a>")
                .contains("<a href=\"mailto:info@company.com\">");

    }

    @Test
    void testCase1ToOtherFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");

        Path extraFile = outputFolder.resolve("extra.html");
        Impl.writeFile(extraFile, "<!-- placeholder -->");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE1_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input(CASE1_FILE)
                        .output("other/page.html"));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──file.css\n"
                + "├──extra.html\n"
                + "├──images\n"
                + "│   ├──image.png\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "└──other\n"
                + "    └──page.html\n");

        assertThat(outputFolder).isDirectory();
        assertThat(extraFile).isRegularFile();
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

        String content = Impl.readFile(pageFile);
        assertThat(content).isNotEmpty()
                .contains("<img src=\"../images/image.png\" alt=\"a test png image\">")
                .contains("<img src=\"../images/img.svg\" alt=\"a test svg image\">")
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/file.css\">")
                .contains("<script src=\"../js/empty.js\"></script>")
                .doesNotContain("<script src=\"js/\">")
                .contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">")
                .contains("<a href=\"../folder/external.html\">ext page</a>")
                .contains("<a href=\"../folder/external.html#top\">top section</a>")
                .contains("<a href=\"../references/\">references</a>")
                .contains("<a href=\"../references/#property\">referenced property</a>")
                .contains("<a href=\"../references/private.html\">private references</a>")
                .contains("<a href=\"#title\">this page</a>");
    }

    @Test
    void testCase1ToFolderFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        String outputFile = "folder/page.html";

        Path extraFile = outputFolder.resolve("extra.html");
        Impl.writeFile(extraFile, "<!-- placeholder -->");
        assertThat(extraFile).isRegularFile();

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE1_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input(CASE1_FILE)
                        .output(outputFile))
                .addPage(new ConfigurationPage()
                        .input("references/index.html")
                        .output("folder/index.html"))
                .addPage(new ConfigurationPage()
                        .input("references/private.html")
                        .output("folder/p.html"))
                .options(new ConfigurationOptions().clearOutputRootFolder(true));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──file.css\n"
                + "├──folder\n"
                + "│   └──page.html\n"
                + "├──images\n"
                + "│   ├──image.png\n"
                + "│   └──img.svg\n"
                + "└──js\n"
                + "    └──empty.js\n");

        assertThat(outputFolder).isDirectory();
        assertThat(extraFile).doesNotExist();
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

        String content = Impl.readFile(pageFile);
        assertThat(content).isNotEmpty()
                .contains("<img src=\"../images/image.png\" alt=\"a test png image\">")
                .contains("<img src=\"../images/img.svg\" alt=\"a test svg image\">")
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/file.css\">")
                .contains("<script src=\"../js/empty.js\"></script>")
                .doesNotContain("<script src=\"js/\">")
                .contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">")
                .contains("<a href=\"external.html\">ext page</a>")
                .contains("<a href=\"external.html#top\">top section</a>")
                .contains("<a href=\"./\">references</a>")
                .contains("<a href=\"./#property\">referenced property</a>")
                .contains("<a href=\"p.html\">private references</a>")
                .contains("<a href=\"#title\">this page</a>");
    }

    @Test
    void testCase1ToSubOtherFile() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        String outputFile = "sub/other/page.html";

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE1_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input(CASE1_FILE)
                        .output(outputFile))
                .addPage(new ConfigurationPage()
                        .input("references/index.html")
                        .output("ref/ref.html"))
                .addPage(new ConfigurationPage()
                        .input("references/private.html")
                        .output("sub/private/ref.html"));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──file.css\n"
                + "├──images\n"
                + "│   ├──image.png\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "└──sub\n"
                + "    └──other\n"
                + "        └──page.html\n");

        assertThat(outputFolder).isDirectory();
        Path indexFile = outputFolder.resolve("index.html");
        assertThat(indexFile).doesNotExist();
        Path pageFile = outputFolder.resolve(outputFile);
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

        String content = Impl.readFile(pageFile);
        assertThat(content).isNotEmpty()
                .contains("<img src=\"../../images/image.png\" alt=\"a test png image\">")
                .contains("<img src=\"../../images/img.svg\" alt=\"a test svg image\">")
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../css/file.css\">")
                .contains("<script src=\"../../js/empty.js\"></script>")
                .doesNotContain("<script src=\"js/\">")
                .contains("<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">")
                .contains("<a href=\"../../folder/external.html\">ext page</a>")
                .contains("<a href=\"../../folder/external.html#top\">top section</a>")
                .contains("<a href=\"../../ref/ref.html\">references</a>")
                .contains("<a href=\"../../ref/ref.html#property\">referenced property</a>")
                .contains("<a href=\"../private/ref.html\">private references</a>")
                .contains("<a href=\"#title\">this page</a>");
    }

    @Test
    void testCase1ToCompleteSite() throws Exception {
        Path outputFolder = Files.createTempDirectory("test")
                .resolve("output");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE1_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input(CASE1_FILE)
                        .title("Page")
                        .output("page.html")
                        .sitePageSelector("body"))
                .options(new ConfigurationOptions()
                        .completeSite(true)
                        .createToc(true)
                        .includeDefaultCss(false)
                        .includeDefaultJs(false)
                        .resourcesRewriteStrategy(RewriteStrategy.SHORT_SHA1_SUFFIX)
                        .cssOutputFolder("static/css")
                        .javascriptOutputFolder("static/js")
                        .imagesOutputFolder("static/img"));

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).isEqualTo(""
                + "output\n"
                + "├──page.html\n"
                + "└──static\n"
                + "    ├──css\n"
                + "    │   └──file_f65f9d4.css\n"
                + "    ├──img\n"
                + "    │   ├──image_482e919.png\n"
                + "    │   └──img_4eacad7.svg\n"
                + "    └──js\n"
                + "        └──empty_9f7f886.js\n");

        assertThat(outputFolder).isDirectory();
        Path page1File = outputFolder.resolve("page.html");
        assertThat(page1File).isRegularFile();

        String content1 = Impl.readFile(page1File);
        assertThat(content1).isNotEmpty()
                .doesNotContain("href=\"static/css/site.css\"") // include 'site.css'
                .doesNotContain("src=\"assets/js/site.js\"") // include 'site.js'
                .contains("<title>Page</title>")
                .contains("<a class=\"navbar-item\" href=\"page.html\">Page</a>")
                .contains("<a class=\"home-link is-current\" href=\"page.html\"></a>")
                .contains("<li><a href=\"page.html\">Page</a></li>") //breadcrumbs
                .doesNotContain("<span class=\"prev\">")
                .doesNotContain("<span class=\"next\">");
    }

    @Test
    void testCase2() throws Exception {
        Path outputFolder = Files.createTempDirectory("test")
                .resolve("out")
                .resolve("subOut");
        assertThat(outputFolder).doesNotExist();

        Path catalog1 = Files.createTempFile("test", ".txt");
        Files.write(catalog1, "<some content>".getBytes(StandardCharsets.UTF_8));

        Path catalog2 = Files.createTempFile("test", ".txt");
        Files.write(catalog2, "extra.html\npage9.html".getBytes(StandardCharsets.UTF_8));

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE2_FOLDER)
                .outputRootFolder(outputFolder)
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog2)
                        .outputAction(OutputAction.MERGE_SILENTLY));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).isEqualTo(""
                + "subOut\n"
                + "├──css\n"
                + "│   └──page.css\n"
                + "├──images\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "├──page1.html\n"
                + "└──sub\n"
                + "    └──page2.html\n");

        assertThat(catalog1).hasContent("page1.html\nsub/page2.html");
        assertThat(catalog2).hasContent("extra.html\npage1.html\npage9.html\nsub/page2.html");

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

        String content1 = Impl.readFile(page1);
        assertThat(content1).isNotEmpty()
                .contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>")
                .contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>")
                .contains("<a href=\"sub/page2.html\">Page 2</a>");

        String content2 = Impl.readFile(page2);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ");
        assertThat(content2).isNotEmpty()
                .contains("<a href=\"../page1.html\">Page 1</a>");
    }

    @Test
    void testCase2ReworkOutputTree() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE2_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input("page1.html")
                        .output("other/p1.html"))
                .addPage(new ConfigurationPage()
                        .input("sub/page2.html")
                        .output("other/p2.html"));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──page.css\n"
                + "├──images\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "└──other\n"
                + "    ├──p1.html\n"
                + "    └──p2.html\n");

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

        String content1 = Impl.readFile(p1);
        assertThat(content1).isNotEmpty()
                .contains("<p><img src=\"../images/img.svg\" alt=\"a test image\"></p>")
                .contains("<script type=\"text/javascript\" src=\"../js/empty.js\"></script>")
                .contains("<a href=\"p2.html\">Page 2</a>");

        String content2 = Impl.readFile(p2);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ")
                .contains("<a href=\"p1.html\">Page 1</a>");
    }

    @Test
    void testCase2ReworkOutputTreeWithFolder() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE2_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input("page1.html")
                        .output("p1.html"))
                .addPage(new ConfigurationPage()
                        .input("sub")
                        .output("other"));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──page.css\n"
                + "├──images\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "├──other\n"
                + "│   └──page2.html\n"
                + "└──p1.html\n");

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

        String content1 = Impl.readFile(p1);
        assertThat(content1).isNotEmpty()
                .contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>")
                .contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>")
                .contains("<a href=\"other/page2.html\">Page 2</a>");

        String content2 = Impl.readFile(page2);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ")
                .contains("<a href=\"../p1.html\">Page 1</a>");
    }

    @Test
    void testCase2ReworkOutputTreeEnsureRewrittenLinks() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE2_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage()
                        .input("page1.html")
                        .output("p1.html"))
                .addPage(new ConfigurationPage()
                        .input("sub/page2.html"));
        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──css\n"
                + "│   └──page.css\n"
                + "├──images\n"
                + "│   └──img.svg\n"
                + "├──js\n"
                + "│   └──empty.js\n"
                + "├──p1.html\n"
                + "└──sub\n"
                + "    └──page2.html\n");

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

        String content1 = Impl.readFile(p1);
        assertThat(content1).isNotEmpty()
                .contains("<p><img src=\"images/img.svg\" alt=\"a test image\"></p>")
                .contains("<script type=\"text/javascript\" src=\"js/empty.js\"></script>")
                .contains("<a href=\"sub/page2.html\">Page 2</a>");

        String content2 = Impl.readFile(page2);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/page.css\"> ")
                .contains("<a href=\"../p1.html\">Page 1</a>");
    }

    @Test
    void testCase3() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        assertThat(outputFolder).isDirectory();
        Path catalog1 = outputFolder.resolve("catalog.txt");

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE3_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage().title("Navigation Root")
                        .addChild(new ConfigurationPage().input(CASE3_ONE)
                                .title("Page I")
                                .output("page1.html")
                                .sitePageSelector("body"))
                        .addChild(new ConfigurationPage().input(CASE3_TWO)
                                .title("Page II")
                                .output("page2.html")
                                .sitePageSelector("div.paragraph"))
                        .addChild(new ConfigurationPage().input(CASE3_THREE)
                                .title("Page III")
                                .output("page3.html")
                                .sitePageSelector("div.sect1"))
                        .addChild(new ConfigurationPage().input(CASE3_CHAPTER1)
                                .indexHandling(IndexHandling.USE_TITLE_ONLY)
                                .titleSelector("h2")
                                .sitePageSelector("div.main"))
                        .addChild(new ConfigurationPage().input(CASE3_CHAPTER2)
                                .indexHandling(IndexHandling.USE_PAGE_AS_PARENT)
                                .includeChildFolders(false)
                                .title("CHAPTER 2!")))
                .options(new ConfigurationOptions()
                        .completeSite(true)
                        .createToc(true))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1));

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──catalog.txt\n"
                + "├──chapter1\n"
                + "│   ├──sec1.html\n"
                + "│   ├──sec10.html\n"
                + "│   └──sec5.html\n"
                + "├──chapter2\n"
                + "│   └──index.html\n"
                + "├──css\n"
                + "│   └──site.css\n"
                + "├──font\n"
                + "│   ├──roboto-latin-400.woff\n"
                + "│   ├──roboto-latin-400.woff2\n"
                + "│   ├──roboto-latin-400italic.woff\n"
                + "│   ├──roboto-latin-400italic.woff2\n"
                + "│   ├──roboto-latin-500.woff\n"
                + "│   ├──roboto-latin-500.woff2\n"
                + "│   ├──roboto-latin-500italic.woff\n"
                + "│   ├──roboto-latin-500italic.woff2\n"
                + "│   ├──roboto-mono-latin-400.woff\n"
                + "│   ├──roboto-mono-latin-400.woff2\n"
                + "│   ├──roboto-mono-latin-500.woff\n"
                + "│   └──roboto-mono-latin-500.woff2\n"
                + "├──images\n"
                + "│   ├──back.svg\n"
                + "│   ├──caret.svg\n"
                + "│   ├──chevron.svg\n"
                + "│   ├──home-o.svg\n"
                + "│   ├──home.svg\n"
                + "│   └──menu.svg\n"
                + "├──js\n"
                + "│   └──site.js\n"
                + "├──page1.html\n"
                + "├──page2.html\n"
                + "└──page3.html\n");

        assertThat(catalog1).hasContent("" +
                "chapter1/sec1.html\n" +
                "chapter1/sec10.html\n" +
                "chapter1/sec5.html\n" +
                CASE3_CHAPTER2_INDEX + "\n" +
                "page1.html\n" +
                "page2.html\n" +
                "page3.html");

        assertThat(outputFolder).isDirectory();
        Path page1File = outputFolder.resolve("page1.html");
        assertThat(page1File).isRegularFile();
        Path page2File = outputFolder.resolve("page2.html");
        assertThat(page2File).isRegularFile();
        Path page3File = outputFolder.resolve("page3.html");
        assertThat(page3File).isRegularFile();
        Path chapter1Sec1File = outputFolder.resolve(CASE3_CHAPTER1_SEC1);
        assertThat(chapter1Sec1File).isRegularFile();
        Path chapter1Sec5File = outputFolder.resolve(CASE3_CHAPTER1_SEC5);
        assertThat(chapter1Sec5File).isRegularFile();
        Path chapter1Sec10File = outputFolder.resolve(CASE3_CHAPTER1_SEC10);
        assertThat(chapter1Sec10File).isRegularFile();
        Path chapter2File = outputFolder.resolve(CASE3_CHAPTER2_INDEX);
        assertThat(chapter2File).isRegularFile();
        Path css = outputFolder.resolve("css");
        assertThat(css).isDirectory();
        assertThat(css.resolve("site.css")).isRegularFile();
        Path js = outputFolder.resolve("js");
        assertThat(js).isDirectory();
        assertThat(js.resolve("site.js")).isRegularFile();

        String cssContent = Impl.readFile(css.resolve("site.css"));
        assertThat(cssContent).isNotEmpty()
                .contains("url(../images/back.svg)")
                .contains("url(../images/caret.svg)")
                .contains("url(../images/chevron.svg)")
                .contains("url(../images/home-o.svg)")
                .contains("url(../images/home.svg)")
                .contains("url(../images/menu.svg)")
                .contains("url(../font/roboto-latin-400.woff)")
                .contains("url(../font/roboto-latin-400.woff2)")
                .contains("url(../font/roboto-latin-500.woff)")
                .contains("url(../font/roboto-latin-500.woff2)")
                .contains("url(../font/roboto-latin-500italic.woff)")
                .contains("url(../font/roboto-latin-500italic.woff2)")
                .contains("url(../font/roboto-latin-400italic.woff)")
                .contains("url(../font/roboto-latin-400italic.woff2)")
                .contains("url(../font/roboto-mono-latin-400.woff)")
                .contains("url(../font/roboto-mono-latin-400.woff2)")
                .contains("url(../font/roboto-mono-latin-500.woff)")
                .contains("url(../font/roboto-mono-latin-500.woff2)");

        String content1 = Impl.readFile(page1File);
        assertThat(content1)
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .doesNotContain("<body id=\"original-page\">")
                .contains("<title>Page I</title>")
                .contains("<a class=\"home-link is-current\" href=\"page1.html\">")
                .doesNotContain("<span class=\"prev\">")
                .contains("<span class=\"next\"><a href=\"page2.html\">Page II</a></span>")
                .doesNotContain("<footer class=\"footer\">") //footer
                .contains("<li data-level=\"1\"><a href=\"#h2\">Header Level 2</a></li>") // toc
                .contains("<li data-level=\"2\"><a href=\"#h3\">Header Level 3</a></li>") // toc
        ;

        String content2 = Impl.readFile(page2File);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .contains("<title>Page II</title>")
                .contains("<a class=\"home-link\" href=\"page1.html\">")
                .contains("<a href=\"page2.html\">Page II</a>")
                .contains("<span class=\"prev\"><a href=\"page1.html\">Page I</a></span>")
                .contains("<span class=\"next\"><a href=\"page3.html\">Page III</a></span>")
                .doesNotContain("<li data-level") // no toc because the sitePageSelector selects only a div
        ;

        String content3 = Impl.readFile(page3File);
        assertThat(content3).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .contains("<title>Page III</title>")
                .contains("<a class=\"home-link\" href=\"page1.html\">")
                .contains("<span class=\"prev\"><a href=\"page2.html\">Page II</a></span>")
                .contains("<span class=\"next\"><a href=\"chapter1/sec5.html\">The section 5</a></span>")
                .contains("<li data-level=\"1\"><a href=\"#_page_three\">Page - Three</a></li>") // toc
                .contains("<li data-level=\"2\"><a href=\"#_header_level_2\">Header Level 2</a></li>") // toc
                .contains("<li data-level=\"3\"><a href=\"#_header_level_3\">Header Level 3</a></li>") // toc
        ;

        String content4 = Impl.readFile(chapter1Sec5File);
        assertThat(content4).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site.css\">") // include 'site.css'
                .contains("<script src=\"../js/site.js\"></script>") // include 'site.js'
                .contains("<title>The section 5</title>")
                .contains("<a class=\"home-link\" href=\"../page1.html\">")
                .contains("<li>First Chapter</li>")
                .contains("<li><a href=\"sec5.html\">The section 5</a></li>")
                .contains("<span class=\"prev\"><a href=\"../page3.html\">Page III</a></span>")
                .contains("<span class=\"next\"><a href=\"sec10.html\">The section 10</a></span>")
                .doesNotContain("This is in the content but not in main section");

        String content5 = Impl.readFile(chapter1Sec10File);
        assertThat(content5).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site.css\">") // include 'site.css'
                .contains("<script src=\"../js/site.js\"></script>") // include 'site.js'
                .contains("<title>The section 10</title>")
                .contains("<a class=\"home-link\" href=\"../page1.html\">")
                .contains("<li>First Chapter</li>")
                .contains("<li><a href=\"sec10.html\">The section 10</a></li>")
                .contains("<span class=\"prev\"><a href=\"sec5.html\">The section 5</a></span>")
                .contains("<span class=\"next\"><a href=\"sec1.html\">The section 1</a></span>")
                .doesNotContain("This is in the content but not in main section");

        String content6 = Impl.readFile(chapter1Sec1File);
        assertThat(content6).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site.css\">") // include 'site.css'
                .contains("<script src=\"../js/site.js\"></script>") // include 'site.js'
                .contains("<title>The section 1</title>")
                .contains("<a class=\"home-link\" href=\"../page1.html\">")
                .contains("<li>First Chapter</li>")
                .contains("<li><a href=\"sec1.html\">The section 1</a></li>")
                .contains("<span class=\"prev\"><a href=\"sec10.html\">The section 10</a></span>")
                .contains("<span class=\"next\"><a href=\"../chapter2/\">CHAPTER 2!</a></span>")
                .doesNotContain("This is in the content but not in main section");

        String content7 = Impl.readFile(chapter2File);
        assertThat(content7).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site.css\">") // include 'site.css'
                .contains("<script src=\"../js/site.js\"></script>") // include 'site.js'
                .contains("<title>CHAPTER 2!</title>")
                .contains("<li><a href=\"./\">CHAPTER 2!</a></li>")
                .contains("<a class=\"home-link\" href=\"../page1.html\">")
                .contains("<span class=\"prev\"><a href=\"../chapter1/sec1.html\">The section 1</a></span>")
                .doesNotContain("<span class=\"next\">");
    }

    @Test
    void testCase3b() throws Exception {
        Path outputFolder = Files.createTempDirectory("test");
        assertThat(outputFolder).isDirectory();

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE3_FOLDER)
                .outputRootFolder(outputFolder)
                .defaultPageOptions(new ConfigurationPageOptions().sitePageSelector("div#content"))
                .options(new ConfigurationOptions().completeSite(true)
                        .imagesOutputFolder("imgs/")
                        .footer("This is a footer"));

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).endsWith(""
                + "├──chapter1\n"
                + "│   ├──index.html\n"
                + "│   ├──sec1.html\n"
                + "│   ├──sec10.html\n"
                + "│   └──sec5.html\n"
                + "├──chapter2\n"
                + "│   ├──index.html\n"
                + "│   ├──sub-a\n"
                + "│   │   └──index.html\n"
                + "│   └──sub-b\n"
                + "│       └──index.html\n"
                + "├──css\n"
                + "│   └──site.css\n"
                + "├──font\n"
                + "│   ├──roboto-latin-400.woff\n"
                + "│   ├──roboto-latin-400.woff2\n"
                + "│   ├──roboto-latin-400italic.woff\n"
                + "│   ├──roboto-latin-400italic.woff2\n"
                + "│   ├──roboto-latin-500.woff\n"
                + "│   ├──roboto-latin-500.woff2\n"
                + "│   ├──roboto-latin-500italic.woff\n"
                + "│   ├──roboto-latin-500italic.woff2\n"
                + "│   ├──roboto-mono-latin-400.woff\n"
                + "│   ├──roboto-mono-latin-400.woff2\n"
                + "│   ├──roboto-mono-latin-500.woff\n"
                + "│   └──roboto-mono-latin-500.woff2\n"
                + "├──four.html\n"
                + "├──imgs\n"
                + "│   ├──back.svg\n"
                + "│   ├──caret.svg\n"
                + "│   ├──chevron.svg\n"
                + "│   ├──home-o.svg\n"
                + "│   ├──home.svg\n"
                + "│   └──menu.svg\n"
                + "├──js\n"
                + "│   └──site.js\n"
                + "├──one.html\n"
                + "├──three.html\n"
                + "└──two.html\n");

        assertThat(outputFolder).isDirectory();
        Path oneFile = outputFolder.resolve(CASE3_ONE);
        assertThat(oneFile).isRegularFile();
        Path twoFile = outputFolder.resolve(CASE3_TWO);
        assertThat(twoFile).isRegularFile();
        Path threeFile = outputFolder.resolve(CASE3_THREE);
        assertThat(threeFile).isRegularFile();
        Path fourFile = outputFolder.resolve(CASE3_FOUR);
        assertThat(fourFile).isRegularFile();
        Path chapter1 = outputFolder.resolve(CASE3_CHAPTER1_INDEX);
        assertThat(chapter1).isRegularFile();
        Path css = outputFolder.resolve("css");
        assertThat(css).isDirectory();
        assertThat(css.resolve("site.css")).isRegularFile();
        Path js = outputFolder.resolve("js");
        assertThat(js).isDirectory();
        assertThat(js.resolve("site.js")).isRegularFile();

        String cssContent = Impl.readFile(css.resolve("site.css"));
        assertThat(cssContent).isNotEmpty()
                .contains("url(../imgs/back.svg)")
                .contains("url(../imgs/caret.svg)")
                .contains("url(../imgs/chevron.svg)")
                .contains("url(../imgs/home-o.svg)")
                .contains("url(../imgs/home.svg)")
                .contains("url(../imgs/menu.svg)");

        String content1 = Impl.readFile(oneFile);
        assertThat(content1)
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .doesNotContain("<body id=\"original-page\">")
                .contains("<title>Page - One</title>")
                .contains("<span class=\"next\"><a href=\"two.html\">Page - Two</a></span>")
                .contains("<footer class=\"footer\">") //footer
                .contains("<p>This is a footer</p>") //footer
        ;

        String content2 = Impl.readFile(twoFile);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - Two</title>")
                .contains("<span class=\"prev\"><a href=\"one.html\">Page - One</a></span>")
                .contains("<span class=\"next\"><a href=\"three.html\">Page - Three</a></span>");

        String content3 = Impl.readFile(threeFile);
        assertThat(content3).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - Three</title>")
                .contains("<span class=\"prev\"><a href=\"two.html\">Page - Two</a></span>")
                .contains("<span class=\"next\"><a href=\"four.html\">Page - Four</a></span>");

        String content4 = Impl.readFile(fourFile);
        assertThat(content4).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site.css\">") // include 'site.css'
                .contains("<script src=\"js/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - Four</title>")
                .contains("<span class=\"prev\"><a href=\"three.html\">Page - Three</a></span>")
                .contains("<span class=\"next\"><a href=\"chapter1/\">Chapter 1</a></span>");

        String content5 = Impl.readFile(chapter1);
        assertThat(content5).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site.css\">") // include 'site.css'
                .contains("<script src=\"../js/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1</title>")
                .contains("<span class=\"prev\"><a href=\"../four.html\">Page - Four</a></span>")
                .contains("<span class=\"next\"><a href=\"sec5.html\">Chapter 1 - section 5</a></span>");

    }

    @Test
    void testCase3c() throws Exception {
        Path outputFolder = Files.createTempDirectory("test")
                .resolve("output");
        assertThat(outputFolder).doesNotExist();
        Path catalog1 = outputFolder.resolve("../test/catalog.txt");
        assertThat(catalog1).doesNotExist();

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE3_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage().input(CASE3_ONE))
                .addPage(new ConfigurationPage().input(CASE3_CHAPTER1)
                        .indexHandling(IndexHandling.USE_PAGE_IN_THE_LIST)
                        .title("CHAPTER 1")
                        .includeChildFolders(true))
                .addPage(new ConfigurationPage().input(CASE3_CHAPTER2)
                        .indexHandling(IndexHandling.USE_PAGE_AS_PARENT)
                        .title("CHAPTER 2")
                        .includeChildFolders(true))
                .defaultPageOptions(new ConfigurationPageOptions()
                        .sitePageSelector("div.sect1"))
                .options(new ConfigurationOptions().completeSite(true)
                        .siteHomePath(CASE3_CHAPTER1_INDEX)
                        .resourcesRewriteStrategy(RewriteStrategy.SHORT_SHA1_SUFFIX)
                        .footer(""))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1));

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).isEqualTo(""
                + "output\n"
                + "├──chapter1\n"
                + "│   ├──index.html\n"
                + "│   ├──sec1.html\n"
                + "│   ├──sec10.html\n"
                + "│   └──sec5.html\n"
                + "├──chapter2\n"
                + "│   ├──index.html\n"
                + "│   ├──sub-a\n"
                + "│   │   └──index.html\n"
                + "│   └──sub-b\n"
                + "│       └──index.html\n"
                + "├──css\n"
                + "│   └──site_22ae394.css\n"
                + "├──font\n"
                + "│   ├──roboto-latin-400_42c8ae7.woff\n"
                + "│   ├──roboto-latin-400_81c7967.woff2\n"
                + "│   ├──roboto-latin-400italic_e9e842d.woff\n"
                + "│   ├──roboto-latin-400italic_f94000b.woff2\n"
                + "│   ├──roboto-latin-500_8f956a3.woff2\n"
                + "│   ├──roboto-latin-500_fb1bd3b.woff\n"
                + "│   ├──roboto-latin-500italic_10f24f8.woff2\n"
                + "│   ├──roboto-latin-500italic_96a90b4.woff\n"
                + "│   ├──roboto-mono-latin-400_9867cbb.woff\n"
                + "│   ├──roboto-mono-latin-400_b0c4753.woff2\n"
                + "│   ├──roboto-mono-latin-500_d889ce2.woff2\n"
                + "│   └──roboto-mono-latin-500_eaeb892.woff\n"
                + "├──images\n"
                + "│   ├──back_a61d85b.svg\n"
                + "│   ├──caret_61336b7.svg\n"
                + "│   ├──chevron_b716e1f.svg\n"
                + "│   ├──home-o_3100c5e.svg\n"
                + "│   ├──home_1964ae1.svg\n"
                + "│   └──menu_a61d85b.svg\n"
                + "├──js\n"
                + "│   └──site_99eac35.js\n"
                + "└──one.html\n");

        assertThat(catalog1).hasContent(""
                + CASE3_CHAPTER1_INDEX + "\n"
                + CASE3_CHAPTER1_SEC1 + "\n"
                + CASE3_CHAPTER1_SEC10 + "\n"
                + CASE3_CHAPTER1_SEC5 + "\n"
                + CASE3_CHAPTER2_INDEX + "\n"
                + CASE3_CHAPTER2_SUB_A + "\n"
                + CASE3_CHAPTER2_SUB_B + "\n"
                + CASE3_ONE);

        assertThat(outputFolder).isDirectory();
        Path page1File = outputFolder.resolve(CASE3_ONE);
        assertThat(page1File).isRegularFile();
        Path chapter1IndexFile = outputFolder.resolve(CASE3_CHAPTER1_INDEX);
        assertThat(chapter1IndexFile).isRegularFile();
        Path chapter1Sec1File = outputFolder.resolve(CASE3_CHAPTER1_SEC1);
        assertThat(chapter1Sec1File).isRegularFile();
        Path chapter1Sec5File = outputFolder.resolve(CASE3_CHAPTER1_SEC5);
        assertThat(chapter1Sec5File).isRegularFile();
        Path chapter1Sec10File = outputFolder.resolve(CASE3_CHAPTER1_SEC10);
        assertThat(chapter1Sec10File).isRegularFile();
        Path chapter2File = outputFolder.resolve(CASE3_CHAPTER2_INDEX);
        assertThat(chapter2File).isRegularFile();
        Path chapter2SubA = outputFolder.resolve(CASE3_CHAPTER2_SUB_A);
        assertThat(chapter2SubA).isRegularFile();
        Path chapter2SubB = outputFolder.resolve(CASE3_CHAPTER2_SUB_B);
        assertThat(chapter2SubB).isRegularFile();

        Path css = outputFolder.resolve("css");
        String cssContent = Impl.readFile(css.resolve("site_22ae394.css"));
        assertThat(cssContent).isNotEmpty()
                .contains("url(../images/back_a61d85b.svg)")
                .contains("url(../images/caret_61336b7.svg)")
                .contains("url(../images/chevron_b716e1f.svg)")
                .contains("url(../images/home-o_3100c5e.svg)")
                .contains("url(../images/home_1964ae1.svg)")
                .contains("url(../images/menu_a61d85b.svg)")
                .contains("url(../font/roboto-latin-400_42c8ae7.woff)")
                .contains("url(../font/roboto-latin-400_81c7967.woff2)")
                .contains("url(../font/roboto-latin-500_fb1bd3b.woff)")
                .contains("url(../font/roboto-latin-500_8f956a3.woff2)")
                .contains("url(../font/roboto-latin-500italic_96a90b4.woff)")
                .contains("url(../font/roboto-latin-500italic_10f24f8.woff2)")
                .contains("url(../font/roboto-latin-400italic_e9e842d.woff)")
                .contains("url(../font/roboto-latin-400italic_f94000b.woff2)")
                .contains("url(../font/roboto-mono-latin-400_9867cbb.woff)")
                .contains("url(../font/roboto-mono-latin-400_b0c4753.woff2)")
                .contains("url(../font/roboto-mono-latin-500_eaeb892.woff)")
                .contains("url(../font/roboto-mono-latin-500_d889ce2.woff2)");

        String content1 = Impl.readFile(page1File);
        assertThat(content1).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Page - One</title>")
                .contains("<a class=\"navbar-item\" href=\"chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"chapter1/\">")
                .doesNotContain("<span class=\"prev\">")
                .contains("<span class=\"next\"><a href=\"chapter1/\">Chapter 1</a></span>")
                .contains("<li class=\"nav-item is-active is-current-page\" data-depth=\"0\"><a class=\"nav-link\" href=\"one.html\">Page - One</a></li>") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><span class=\"nav-text\">CHAPTER 1</span>") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><a class=\"nav-link\" href=\"chapter2/\">CHAPTER 2</a>") //nav-list
                .contains("<footer class=\"footer\">") //footer
                .contains("<p></p>") //footer
        ;

        String content2 = Impl.readFile(chapter1IndexFile);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1</title>")
                .doesNotContain("<p>This is outside of sect1</p>") //because only "div.sect1" is used as page selector
                .contains("<a class=\"navbar-item\" href=\"./\">Chapter 1</a>")
                .contains("<a class=\"home-link is-current\" href=\"./\">")
                .contains("<li>CHAPTER 1</li>") //breadcrumbs
                .contains("<li><a href=\"./\">Chapter 1</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../one.html\">Page - One</a></span>")
                .contains("<span class=\"next\"><a href=\"sec5.html\">Chapter 1 - section 5</a></span>")
                .contains("<li class=\"nav-item\" data-depth=\"0\"><a class=\"nav-link\" href=\"../one.html\">Page - One</a></li>") //nav-list
                .contains("<li class=\"nav-item is-active is-current-path\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><span class=\"nav-text\">CHAPTER 1</span>") //nav-list
                .contains("<li class=\"nav-item is-active is-current-page\" data-depth=\"1\"><a class=\"nav-link\" href=\"./\">Chapter 1</a></li>") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"1\"><a class=\"nav-link\" href=\"sec5.html\">Chapter 1 - section 5</a></li>\n") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><a class=\"nav-link\" href=\"../chapter2/\">CHAPTER 2</a>"); //nav-list;

        String content3 = Impl.readFile(chapter1Sec5File);
        assertThat(content3).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1 - section 5</title>")
                .contains("<a class=\"navbar-item\" href=\"./\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"./\">")
                .contains("<li>CHAPTER 1</li>") //breadcrumbs
                .contains("<li><a href=\"sec5.html\">Chapter 1 - section 5</a></li>") //breadcrumbs
                .contains("This is in the content but not in main section") // the page `div.sect1` is not found, fallback on `body`
                .contains("<span class=\"prev\"><a href=\"./\">Chapter 1</a></span>")
                .contains("<span class=\"next\"><a href=\"sec10.html\">Chapter 1 - section 10</a></span>")
                .contains("<li class=\"nav-item\" data-depth=\"0\"><a class=\"nav-link\" href=\"../one.html\">Page - One</a></li>") //nav-list
                .contains("<li class=\"nav-item is-active is-current-path\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><span class=\"nav-text\">CHAPTER 1</span>") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"1\"><a class=\"nav-link\" href=\"./\">Chapter 1</a></li>") //nav-list
                .contains("<li class=\"nav-item is-active is-current-page\" data-depth=\"1\"><a class=\"nav-link\" href=\"sec5.html\">Chapter 1 - section 5</a></li>\n") //nav-list
                .contains("<li class=\"nav-item\" data-depth=\"0\"><button class=\"nav-item-toggle\"></button><a class=\"nav-link\" href=\"../chapter2/\">CHAPTER 2</a>"); //nav-list;

        String content4 = Impl.readFile(chapter1Sec10File);
        assertThat(content4).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1 - section 10</title>")
                .contains("<a class=\"navbar-item\" href=\"./\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"./\">")
                .contains("<li>CHAPTER 1</li>") //breadcrumbs
                .contains("<li><a href=\"sec10.html\">Chapter 1 - section 10</a></li>") //breadcrumbs
                .contains("This is in the content but not in main section") // the page `div.sect1` is not found, fallback on `body`
                .contains("<span class=\"prev\"><a href=\"sec5.html\">Chapter 1 - section 5</a></span>")
                .contains("<span class=\"next\"><a href=\"sec1.html\">Chapter 1 - section 1</a></span>");

        String content5 = Impl.readFile(chapter1Sec1File);
        assertThat(content5).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1 - section 1</title>")
                .contains("<a class=\"home-link\" href=\"./\">")
                .contains("<li>CHAPTER 1</li>") //breadcrumbs
                .contains("<li><a href=\"sec1.html\">Chapter 1 - section 1</a></li>") //breadcrumbs
                .contains("This is in the content but not in main section") // the page `div.sect1` is not found, fallback on `body`
                .contains("<span class=\"prev\"><a href=\"sec10.html\">Chapter 1 - section 10</a></span>")
                .contains("<span class=\"next\"><a href=\"../chapter2/\">CHAPTER 2</a></span>");

        String content6 = Impl.readFile(chapter2File);
        assertThat(content6).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>CHAPTER 2</title>")
                .contains("<a class=\"navbar-item\" href=\"../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../chapter1/\">")
                .contains("<li><a href=\"./\">CHAPTER 2</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../chapter1/sec1.html\">Chapter 1 - section 1</a></span>")
                .contains("<span class=\"next\"><a href=\"sub-a/\">Article A</a></span>");

        String content7 = Impl.readFile(chapter2SubA);
        assertThat(content7).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Article A</title>")
                .contains("<a class=\"navbar-item\" href=\"../../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../../chapter1/\">")
                .contains("<li><a href=\"../\">CHAPTER 2</a></li>") //breadcrumbs
                .contains("<li><a href=\"./\">Article A</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../\">CHAPTER 2</a></span>")
                .contains("<span class=\"next\"><a href=\"../sub-b/\">Article B</a></span>");

        String content8 = Impl.readFile(chapter2SubB);
        assertThat(content8).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../../css/site_22ae394.css\">") // include 'site.css'
                .contains("<script src=\"../../js/site_99eac35.js\"></script>") // include 'site.js'
                .contains("<title>Article B</title>")
                .contains("<a class=\"navbar-item\" href=\"../../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../../chapter1/\">")
                .contains("<li><a href=\"../\">CHAPTER 2</a></li>") //breadcrumbs
                .contains("<li><a href=\"./\">Article B</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../sub-a/\">Article A</a></span>")
                .doesNotContain("<span class=\"next\">");
    }

    @Test
    void testCase3d() throws Exception {
        // flat all pages in the output folder
        Path outputFolder = Files.createTempDirectory("test")
                .resolve("output");
        assertThat(outputFolder).doesNotExist();
        Path catalog1 = outputFolder.resolve("../test/catalog.txt");
        assertThat(catalog1).doesNotExist();

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE3_FOLDER)
                .outputRootFolder(outputFolder)
                .addPage(new ConfigurationPage().title("Chapter A")
                        .addChild(new ConfigurationPage().title("A1")
                                .input(CASE3_ONE)
                                .output("page1.html"))
                        .addChild(new ConfigurationPage().title("A2")
                                .input(CASE3_TWO)
                                .output("page2.html"))
                        .addChild(new ConfigurationPage().title("A3")
                                .input(CASE3_THREE)
                                .output("page3.html")))
                .addPage(new ConfigurationPage().title("Chapter B")
                        .input(CASE3_CHAPTER2_INDEX)
                        .output("page4.html")
                        .addChild(new ConfigurationPage().title("B1")
                                .input(CASE3_CHAPTER2_SUB_A)
                                .output("page5.html"))
                        .addChild(new ConfigurationPage().title("B2")
                                .input(CASE3_CHAPTER2_SUB_B)
                                .output("page6.html")))
                .addPage(new ConfigurationPage().title("Chapter C")
                        .addChild(new ConfigurationPage().title("C1")
                                .input(CASE3_CHAPTER1_SEC1)
                                .output("page7.html"))
                        .addChild(new ConfigurationPage().title("C2")
                                .input(CASE3_CHAPTER1_SEC5)
                                .output("page8.html")))
                .options(new ConfigurationOptions().completeSite(true)
                        .resourcesRewriteStrategy(RewriteStrategy.SHORT_SHA1_SUB_FOLDER)
                        .siteHomePath(CASE3_CHAPTER2_INDEX))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1));

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).isEqualTo(""
                + "output\n"
                + "├──css\n"
                + "│   └──c6068cd\n"
                + "│       └──site.css\n"
                + "├──font\n"
                + "│   ├──10f24f8\n"
                + "│   │   └──roboto-latin-500italic.woff2\n"
                + "│   ├──42c8ae7\n"
                + "│   │   └──roboto-latin-400.woff\n"
                + "│   ├──81c7967\n"
                + "│   │   └──roboto-latin-400.woff2\n"
                + "│   ├──8f956a3\n"
                + "│   │   └──roboto-latin-500.woff2\n"
                + "│   ├──96a90b4\n"
                + "│   │   └──roboto-latin-500italic.woff\n"
                + "│   ├──9867cbb\n"
                + "│   │   └──roboto-mono-latin-400.woff\n"
                + "│   ├──b0c4753\n"
                + "│   │   └──roboto-mono-latin-400.woff2\n"
                + "│   ├──d889ce2\n"
                + "│   │   └──roboto-mono-latin-500.woff2\n"
                + "│   ├──e9e842d\n"
                + "│   │   └──roboto-latin-400italic.woff\n"
                + "│   ├──eaeb892\n"
                + "│   │   └──roboto-mono-latin-500.woff\n"
                + "│   ├──f94000b\n"
                + "│   │   └──roboto-latin-400italic.woff2\n"
                + "│   └──fb1bd3b\n"
                + "│       └──roboto-latin-500.woff\n"
                + "├──images\n"
                + "│   ├──1964ae1\n"
                + "│   │   └──home.svg\n"
                + "│   ├──3100c5e\n"
                + "│   │   └──home-o.svg\n"
                + "│   ├──61336b7\n"
                + "│   │   └──caret.svg\n"
                + "│   ├──a61d85b\n"
                + "│   │   ├──back.svg\n"
                + "│   │   └──menu.svg\n"
                + "│   └──b716e1f\n"
                + "│       └──chevron.svg\n"
                + "├──js\n"
                + "│   └──99eac35\n"
                + "│       └──site.js\n"
                + "├──page1.html\n"
                + "├──page2.html\n"
                + "├──page3.html\n"
                + "├──page4.html\n"
                + "├──page5.html\n"
                + "├──page6.html\n"
                + "├──page7.html\n"
                + "└──page8.html\n");

        assertThat(catalog1).hasContent(""
                + "page1.html\n"
                + "page2.html\n"
                + "page3.html\n"
                + "page4.html\n"
                + "page5.html\n"
                + "page6.html\n"
                + "page7.html\n"
                + "page8.html\n");

        assertThat(outputFolder).isDirectory();
        Path page1File = outputFolder.resolve("page1.html");
        assertThat(page1File).isRegularFile();
        Path page2File = outputFolder.resolve("page2.html");
        assertThat(page2File).isRegularFile();
        Path page3File = outputFolder.resolve("page3.html");
        assertThat(page3File).isRegularFile();
        Path page4File = outputFolder.resolve("page4.html");
        assertThat(page4File).isRegularFile();
        Path page5File = outputFolder.resolve("page5.html");
        assertThat(page5File).isRegularFile();
        Path page6File = outputFolder.resolve("page6.html");
        assertThat(page6File).isRegularFile();
        Path page7File = outputFolder.resolve("page7.html");
        assertThat(page7File).isRegularFile();
        Path page8File = outputFolder.resolve("page8.html");
        assertThat(page8File).isRegularFile();

        Path css = outputFolder.resolve("css");
        String cssContent = Impl.readFile(css.resolve("c6068cd/site.css"));
        assertThat(cssContent).isNotEmpty()
                .contains("url(../../images/a61d85b/back.svg)")
                .contains("url(../../images/61336b7/caret.svg)")
                .contains("url(../../images/b716e1f/chevron.svg)")
                .contains("url(../../images/3100c5e/home-o.svg)")
                .contains("url(../../images/1964ae1/home.svg)")
                .contains("url(../../images/a61d85b/menu.svg)")
                .contains("url(../../font/42c8ae7/roboto-latin-400.woff)")
                .contains("url(../../font/81c7967/roboto-latin-400.woff2)")
                .contains("url(../../font/fb1bd3b/roboto-latin-500.woff)")
                .contains("url(../../font/8f956a3/roboto-latin-500.woff2)")
                .contains("url(../../font/96a90b4/roboto-latin-500italic.woff)")
                .contains("url(../../font/10f24f8/roboto-latin-500italic.woff2)")
                .contains("url(../../font/e9e842d/roboto-latin-400italic.woff)")
                .contains("url(../../font/f94000b/roboto-latin-400italic.woff2)")
                .contains("url(../../font/9867cbb/roboto-mono-latin-400.woff)")
                .contains("url(../../font/b0c4753/roboto-mono-latin-400.woff2)")
                .contains("url(../../font/eaeb892/roboto-mono-latin-500.woff)")
                .contains("url(../../font/d889ce2/roboto-mono-latin-500.woff2)");

        String content1 = Impl.readFile(page1File);
        assertThat(content1).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>A1</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li>Chapter A</li>") //breadcrumbs
                .contains("<li><a href=\"page1.html\">A1</a></li>") //breadcrumbs
                .doesNotContain("<span class=\"prev\">")
                .contains("<span class=\"next\"><a href=\"page2.html\">A2</a></span>");

        String content2 = Impl.readFile(page2File);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>A2</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li>Chapter A</li>") //breadcrumbs
                .contains("<li><a href=\"page2.html\">A2</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page1.html\">A1</a></span>")
                .contains("<span class=\"next\"><a href=\"page3.html\">A3</a></span>");

        String content3 = Impl.readFile(page3File);
        assertThat(content3).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<title>A3</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li>Chapter A</li>") //breadcrumbs
                .contains("<li><a href=\"page3.html\">A3</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page2.html\">A2</a></span>")
                .contains("<span class=\"next\"><a href=\"page4.html\">Chapter B</a></span>");

        String content4 = Impl.readFile(page4File);
        assertThat(content4).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter B</title>")
                .contains("<a class=\"home-link is-current\" href=\"page4.html\"></a>") //home-link
                .contains("<li><a href=\"page4.html\">Chapter B</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page3.html\">A3</a></span>")
                .contains("<span class=\"next\"><a href=\"page5.html\">B1</a></span>");

        String content5 = Impl.readFile(page5File);
        assertThat(content5).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>B1</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li><a href=\"page4.html\">Chapter B</a></li>") //breadcrumbs
                .contains("<li><a href=\"page5.html\">B1</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page4.html\">Chapter B</a></span>")
                .contains("<span class=\"next\"><a href=\"page6.html\">B2</a></span>");

        String content6 = Impl.readFile(page6File);
        assertThat(content6).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>B2</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li><a href=\"page4.html\">Chapter B</a></li>") //breadcrumbs
                .contains("<li><a href=\"page6.html\">B2</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page5.html\">B1</a></span>")
                .contains("<span class=\"next\"><a href=\"page7.html\">C1</a></span>");

        String content7 = Impl.readFile(page7File);
        assertThat(content7).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>C1</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li>Chapter C</li>") //breadcrumbs
                .contains("<li><a href=\"page7.html\">C1</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page6.html\">B2</a></span>")
                .contains("<span class=\"next\"><a href=\"page8.html\">C2</a></span>");

        String content8 = Impl.readFile(page8File);
        assertThat(content8).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"css/c6068cd/site.css\">") // include 'site.css'
                .contains("<script src=\"js/99eac35/site.js\"></script>") // include 'site.js'
                .contains("<title>C2</title>")
                .contains("<a class=\"home-link\" href=\"page4.html\"></a>") //home-link
                .contains("<li>Chapter C</li>") //breadcrumbs
                .contains("<li><a href=\"page8.html\">C2</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"page7.html\">C1</a></span>")
                .doesNotContain("<span class=\"next\">");
    }

    @Test
    void testCase3e() throws Exception {
        //use an other root folder for `pages.yaml` sources:
        Path outputFolder = Files.createTempDirectory("test")
                .resolve("output");
        assertThat(outputFolder).doesNotExist();
        Path catalog1 = outputFolder.resolve("../test/catalog.txt");
        assertThat(catalog1).doesNotExist();

        ConfigurationHolder config = new ConfigurationHolder()
                .inputRootFolder(CASE3_FOLDER)
                .outputRootFolder(outputFolder)
                .defaultPageOptions(new ConfigurationPageOptions()
                        .indexHandling(IndexHandling.USE_PAGE_AS_PARENT)
                        .sitePageSelector("div.sect1"))
                .options(new ConfigurationOptions()
                        .completeSite(true)
                        .pagesBaseFolder("pagesSrc")
                        .cssOutputFolder("assets/c")
                        .javascriptOutputFolder("assets/j")
                        .imagesOutputFolder("assets/i")
                        .fontOutputFolder("assets/f")
                        .siteHomePath(CASE3_CHAPTER1_INDEX))
                .addCatalog(new ConfigurationCatalog().outputFile(catalog1));

        // The 'pagesSrc' folder defines:
        // Root: chapter2, chapter1, one.html
        // Root/chapter1: other defaultValue (NATURAL_REVERSED)
        // Root/chapter2: other defaultValue (LEXI_REVERSED)

        Impl.run(config);

        // expected tree:
        assertThat(renderFolder(outputFolder)).isEqualTo(""
                + "output\n"
                + "├──assets\n"
                + "│   ├──c\n"
                + "│   │   └──site.css\n"
                + "│   ├──f\n"
                + "│   │   ├──roboto-latin-400.woff\n"
                + "│   │   ├──roboto-latin-400.woff2\n"
                + "│   │   ├──roboto-latin-400italic.woff\n"
                + "│   │   ├──roboto-latin-400italic.woff2\n"
                + "│   │   ├──roboto-latin-500.woff\n"
                + "│   │   ├──roboto-latin-500.woff2\n"
                + "│   │   ├──roboto-latin-500italic.woff\n"
                + "│   │   ├──roboto-latin-500italic.woff2\n"
                + "│   │   ├──roboto-mono-latin-400.woff\n"
                + "│   │   ├──roboto-mono-latin-400.woff2\n"
                + "│   │   ├──roboto-mono-latin-500.woff\n"
                + "│   │   └──roboto-mono-latin-500.woff2\n"
                + "│   ├──i\n"
                + "│   │   ├──back.svg\n"
                + "│   │   ├──caret.svg\n"
                + "│   │   ├──chevron.svg\n"
                + "│   │   ├──home-o.svg\n"
                + "│   │   ├──home.svg\n"
                + "│   │   └──menu.svg\n"
                + "│   └──j\n"
                + "│       └──site.js\n"
                + "├──chapter1\n"
                + "│   ├──index.html\n"
                + "│   ├──sec1.html\n"
                + "│   ├──sec10.html\n"
                + "│   └──sec5.html\n"
                + "├──chapter2\n"
                + "│   ├──index.html\n"
                + "│   ├──sub-a\n"
                + "│   │   └──index.html\n"
                + "│   └──sub-b\n"
                + "│       └──index.html\n"
                + "├──four.html\n"
                + "├──one.html\n"
                + "├──three.html\n"
                + "└──two.html\n");

        assertThat(catalog1).hasContent(""
                + CASE3_CHAPTER1_INDEX + "\n"
                + CASE3_CHAPTER1_SEC1 + "\n"
                + CASE3_CHAPTER1_SEC10 + "\n"
                + CASE3_CHAPTER1_SEC5 + "\n"
                + CASE3_CHAPTER2_INDEX + "\n"
                + CASE3_CHAPTER2_SUB_A + "\n"
                + CASE3_CHAPTER2_SUB_B + "\n"
                + CASE3_FOUR + "\n"
                + CASE3_ONE + "\n"
                + CASE3_THREE + "\n"
                + CASE3_TWO);

        assertThat(outputFolder).isDirectory();
        Path page1File = outputFolder.resolve(CASE3_ONE);
        assertThat(page1File).isRegularFile();
        Path page2File = outputFolder.resolve(CASE3_TWO);
        assertThat(page2File).isRegularFile();
        Path page3File = outputFolder.resolve(CASE3_THREE);
        assertThat(page3File).isRegularFile();
        Path page4File = outputFolder.resolve(CASE3_FOUR);
        assertThat(page4File).isRegularFile();
        Path chapter1IndexFile = outputFolder.resolve(CASE3_CHAPTER1_INDEX);
        assertThat(chapter1IndexFile).isRegularFile();
        Path chapter1Sec1File = outputFolder.resolve(CASE3_CHAPTER1_SEC1);
        assertThat(chapter1Sec1File).isRegularFile();
        Path chapter1Sec10File = outputFolder.resolve(CASE3_CHAPTER1_SEC10);
        assertThat(chapter1Sec10File).isRegularFile();
        Path chapter2File = outputFolder.resolve(CASE3_CHAPTER2_INDEX);
        assertThat(chapter2File).isRegularFile();
        Path chapter2SubA = outputFolder.resolve(CASE3_CHAPTER2_SUB_A);
        assertThat(chapter2SubA).isRegularFile();
        Path chapter2SubB = outputFolder.resolve(CASE3_CHAPTER2_SUB_B);
        assertThat(chapter2SubB).isRegularFile();

        String cssContent = Impl.readFile(outputFolder.resolve("assets/c/site.css"));
        assertThat(cssContent).isNotEmpty()
                .contains("url(../../assets/i/back.svg)")
                .contains("url(../../assets/i/caret.svg)")
                .contains("url(../../assets/i/chevron.svg)")
                .contains("url(../../assets/i/home-o.svg)")
                .contains("url(../../assets/i/home.svg)")
                .contains("url(../../assets/i/menu.svg)")
                .contains("url(../../assets/f/roboto-latin-400.woff)")
                .contains("url(../../assets/f/roboto-latin-400.woff2)")
                .contains("url(../../assets/f/roboto-latin-400italic.woff)")
                .contains("url(../../assets/f/roboto-latin-400italic.woff2)")
                .contains("url(../../assets/f/roboto-latin-500.woff)")
                .contains("url(../../assets/f/roboto-latin-500.woff2)")
                .contains("url(../../assets/f/roboto-latin-500italic.woff)")
                .contains("url(../../assets/f/roboto-latin-500italic.woff2)")
                .contains("url(../../assets/f/roboto-mono-latin-400.woff)")
                .contains("url(../../assets/f/roboto-mono-latin-400.woff2)")
                .contains("url(../../assets/f/roboto-mono-latin-500.woff)")
                .contains("url(../../assets/f/roboto-mono-latin-500.woff2)");

        String content1 = Impl.readFile(page2File);
        assertThat(content1).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - Two</title>")
                .contains("<a class=\"navbar-item\" href=\"chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"chapter1/\">")
                .contains("<li><a href=\"two.html\">Page - Two</a></li>") //breadcrumbs
                .doesNotContain("<span class=\"prev\">")
                .contains("<span class=\"next\"><a href=\"chapter2/\">Chapter 2</a></span>");

        String content2 = Impl.readFile(chapter2File);
        assertThat(content2).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 2</title>")
                .contains("<a class=\"navbar-item\" href=\"../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../chapter1/\">")
                .contains("<li><a href=\"./\">Chapter 2</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../two.html\">Page - Two</a></span>")
                .contains("<span class=\"next\"><a href=\"sub-b/\">Article B</a></span>");

        String content3 = Impl.readFile(chapter2SubB);
        assertThat(content3).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Article B</title>")
                .contains("<a class=\"navbar-item\" href=\"../../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../../chapter1/\">")
                .contains("<li><a href=\"../\">Chapter 2</a></li>") //breadcrumbs
                .contains("<li><a href=\"./\">Article B</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../\">Chapter 2</a></span>")
                .contains("<span class=\"next\"><a href=\"../sub-a/\">Article A</a></span>");

        String content4 = Impl.readFile(chapter2SubA);
        assertThat(content4).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Article A</title>")
                .contains("<a class=\"navbar-item\" href=\"../../chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"../../chapter1/\">")
                .contains("<li><a href=\"../\">Chapter 2</a></li>") //breadcrumbs
                .contains("<li><a href=\"./\">Article A</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../sub-b/\">Article B</a></span>")
                .contains("<span class=\"next\"><a href=\"../../chapter1/\">Chapter 1</a></span>");

        String content5 = Impl.readFile(chapter1IndexFile);
        assertThat(content5).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1</title>")
                .doesNotContain("<p>This is outside of sect1</p>") //because only "div.sect1" is used as page selector
                .contains("<a class=\"navbar-item\" href=\"./\">Chapter 1</a>")
                .contains("<a class=\"home-link is-current\" href=\"./\">")
                .contains("<li><a href=\"./\">Chapter 1</a></li>") //breadcrumbs
                .contains("<span class=\"prev\"><a href=\"../chapter2/sub-a/\">Article A</a></span>")
                .contains("<span class=\"next\"><a href=\"sec10.html\">Chapter 1 - section 10</a></span>");

        String content6 = Impl.readFile(chapter1Sec10File);
        assertThat(content6).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1 - section 10</title>")
                .contains("<a class=\"home-link\" href=\"./\">")
                .contains("<li><a href=\"./\">Chapter 1</a></li>") //breadcrumbs
                .contains("<li><a href=\"sec10.html\">Chapter 1 - section 10</a></li>") //breadcrumbs
                .contains("This is in the content but not in main section") // the page `div.sect1` is not found, fallback on `body`
                .contains("<span class=\"prev\"><a href=\"./\">Chapter 1</a></span>")
                .contains("<span class=\"next\"><a href=\"sec5.html\">Chapter 1 - section 5</a></span>");

        String content7 = Impl.readFile(chapter1Sec1File);
        assertThat(content7).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"../assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"../assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Chapter 1 - section 1</title>")
                .contains("<a class=\"navbar-item\" href=\"./\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"./\">")
                .contains("<li><a href=\"./\">Chapter 1</a></li>") //breadcrumbs
                .contains("<li><a href=\"sec1.html\">Chapter 1 - section 1</a></li>") //breadcrumbs
                .contains("This is in the content but not in main section") // the page `div.sect1` is not found, fallback on `body`
                .contains("<span class=\"prev\"><a href=\"sec5.html\">Chapter 1 - section 5</a></span>")
                .contains("<span class=\"next\"><a href=\"../one.html\">Page - One</a></span>");

        String content8 = Impl.readFile(page1File);
        assertThat(content8).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - One</title>")
                .contains("<a class=\"navbar-item\" href=\"chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"chapter1/\">")
                .contains("<span class=\"prev\"><a href=\"chapter1/sec1.html\">Chapter 1 - section 1</a></span>")
                .contains("<span class=\"next\"><a href=\"three.html\">Page - Three</a></span>");

        String content9 = Impl.readFile(page4File);
        assertThat(content9).isNotEmpty()
                .contains("<link rel=\"stylesheet\" href=\"assets/c/site.css\">") // include 'site.css'
                .contains("<script src=\"assets/j/site.js\"></script>") // include 'site.js'
                .contains("<title>Page - Four</title>")
                .contains("<a class=\"navbar-item\" href=\"chapter1/\">Chapter 1</a>")
                .contains("<a class=\"home-link\" href=\"chapter1/\">")
                .contains("<span class=\"prev\"><a href=\"three.html\">Page - Three</a></span>")
                .doesNotContain("<span class=\"next\">");
    }

    @Test
    void testToHrefHolder() throws Exception {
        HrefHolder holder1 = Impl.toHrefHolder("folder/page.html#anchor");
        assertThat(holder1.getPath()).isEqualTo("folder/page.html");
        assertThat(holder1.getAnchor()).isEqualTo("#anchor");

        HrefHolder holder2 = Impl.toHrefHolder("folder/page.html");
        assertThat(holder2.getPath()).isEqualTo("folder/page.html");
        assertThat(holder2.getAnchor()).isNull();

        HrefHolder holder3 = Impl.toHrefHolder("folder/page.html#");
        assertThat(holder3.getPath()).isEqualTo("folder/page.html");
        assertThat(holder3.getAnchor()).isEqualTo("#");

        HrefHolder holder4 = Impl.toHrefHolder("");
        assertThat(holder4.getPath()).isEqualTo("");
        assertThat(holder4.getAnchor()).isNull();

        HrefHolder holder5 = Impl.toHrefHolder("#section");
        assertThat(holder5.getPath()).isEqualTo("");
        assertThat(holder5.getAnchor()).isEqualTo("#section");
    }

    @Test
    void testIsUrlAbsolute() throws Exception {
        assertThat(Impl.isUrlAbsolute("https://example.com")).isTrue();
        assertThat(Impl.isUrlAbsolute("https://example.com/test/index.html")).isTrue();
        assertThat(Impl.isUrlAbsolute("http://test.example.com")).isTrue();
        assertThat(Impl.isUrlAbsolute("//example.com")).isTrue();
        assertThat(Impl.isUrlAbsolute("//example.com/test/index.html")).isTrue();
        assertThat(Impl.isUrlAbsolute("ftp://ftp.example.com")).isTrue();
        assertThat(Impl.isUrlAbsolute("mailto:info@test.com")).isTrue();
        assertThat(Impl.isUrlAbsolute("vscode:/file/tmp/file.txt")).isTrue();

        assertThat(Impl.isUrlAbsolute("test/index.html")).isFalse();
        assertThat(Impl.isUrlAbsolute("/test/index.html")).isFalse();
    }

    @Test
    void testWriteCatalog() throws Exception {
        String case2Expected = "page1.html\n"
                + "sub/page2.html";

        runWriteCatalog(CASE1_FOLDER, null, OutputAction.REPLACE_EXISTING, "index.html");
        runWriteCatalog(CASE2_FOLDER, null, OutputAction.REPLACE_EXISTING, case2Expected);
        runWriteCatalog(INPUT_FOLDER, null, OutputAction.REPLACE_EXISTING, "case1/index.html\n"
                + "case2/page1.html\n"
                + "case2/sub/page2.html\n"
                + "case3/" + CASE3_CHAPTER1_INDEX + "\n"
                + "case3/" + CASE3_CHAPTER1_SEC1 + "\n"
                + "case3/" + CASE3_CHAPTER1_SEC10 + "\n"
                + "case3/" + CASE3_CHAPTER1_SEC5 + "\n"
                + "case3/" + CASE3_CHAPTER2_INDEX + "\n"
                + "case3/" + CASE3_CHAPTER2_SUB_A + "\n"
                + "case3/" + CASE3_CHAPTER2_SUB_B + "\n"
                + "case3/" + CASE3_FOUR + "\n"
                + "case3/" + CASE3_ONE + "\n"
                + "case3/" + CASE3_THREE + "\n"
                + "case3/" + CASE3_TWO + "\n");

        runWriteCatalog(CASE2_FOLDER, "extra.html", OutputAction.REPLACE_EXISTING, case2Expected);

        runWriteCatalog(CASE2_FOLDER, null, OutputAction.MERGE_SILENTLY, case2Expected);
        runWriteCatalog(CASE2_FOLDER, case2Expected, OutputAction.MERGE_SILENTLY, case2Expected);
        runWriteCatalog(CASE2_FOLDER, "a-file.html", OutputAction.MERGE_SILENTLY, "a-file.html\n" + case2Expected);
        runWriteCatalog(CASE2_FOLDER, "a-file.html\nz-page.html", OutputAction.MERGE_SILENTLY, "a-file.html\n" + case2Expected + "\nz-page.html");

        runWriteCatalog(CASE2_FOLDER, case2Expected, OutputAction.MERGE_AND_FAIL_IF_ABSENT, case2Expected);
        runWriteCatalog(CASE2_FOLDER, case2Expected + "\na-file.html", OutputAction.MERGE_AND_FAIL_IF_ABSENT, "a-file.html\n" + case2Expected);
        runWriteCatalog(CASE2_FOLDER, "a-file.html\nz-page.html\n" + case2Expected, OutputAction.MERGE_AND_FAIL_IF_ABSENT, "a-file.html\n" + case2Expected + "\nz-page.html");

        assertThatThrownBy(() -> runWriteCatalog(CASE2_FOLDER, null, OutputAction.MERGE_AND_FAIL_IF_ABSENT, case2Expected)).isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(" should contains following entries:\n" + case2Expected);
        assertThatThrownBy(() -> runWriteCatalog(CASE2_FOLDER, "a-file.html", OutputAction.MERGE_AND_FAIL_IF_ABSENT, "a-file.html\n" + case2Expected)).isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(" should contains following entries:\n" + case2Expected);
        assertThatThrownBy(() -> runWriteCatalog(CASE2_FOLDER, "a-file.html\npage1.html", OutputAction.MERGE_AND_FAIL_IF_ABSENT, "a-file.html\n" + case2Expected)).isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(" should contains following entries:\n" + "sub/page2.html");

    }

    private void runWriteCatalog(Path folder, String existingContent, OutputAction action, String expectedContent) throws IOException {
        Path outputFile = Files.createTempFile("test", ".txt");
        if (existingContent != null) {
            Files.write(outputFile, existingContent.getBytes(StandardCharsets.UTF_8));
        }
        Parameters param = new Parameters();
        param.setOutputRootFolder(folder);
        ConfigurationCatalog catalog = new ConfigurationCatalog();
        catalog.setFolder(folder);
        catalog.setStrategy(Strategy.SCAN_FOLDER);
        catalog.setOutputFile(outputFile);
        catalog.setOutputAction(action);
        try {
            Impl.writeCatalog(param, catalog);
        } catch (IllegalStateException e) {
            assertThat(outputFile).hasContent(expectedContent);
            throw e;
        }
        assertThat(outputFile).hasContent(expectedContent);
    }

    @Test
    void testCreateContentToc() throws Exception {
        runCreateContentToc("this is a test", "<body>\n" +
                " <aside class=\"toc sidebar\" data-title=\"Contents\" data-levels=\"3\">\n" +
                "  <div class=\"toc-menu\">\n" +
                "   <h3>Contents</h3>\n" +
                "   <ul></ul>\n" +
                "  </div>\n" +
                " </aside>\n" +
                "</body>");
        runCreateContentToc("<h2 id=\"title\">Title</h2>", "<body>\n" +
                " <aside class=\"toc sidebar\" data-title=\"Contents\" data-levels=\"3\">\n" +
                "  <div class=\"toc-menu\">\n" +
                "   <h3>Contents</h3>\n" +
                "   <ul>\n" +
                "    <li data-level=\"1\"><a href=\"#title\">Title</a></li>\n" +
                "   </ul>\n" +
                "  </div>\n" +
                " </aside>\n" +
                "</body>");
        String html = "<h1 id=\"title\">Page</h1>\n" +
                "<p>Lorem</p>\n" +
                "<h2 id=\"l2\">Header Level 2</h2>\n" +
                "<p>Lorem</p>\n" +
                "<h3 id=\"l3\">Header Level 3</h3>\n" +
                "";
        runCreateContentToc(2, 1, 3, html, "" +
                "<body>\n" +
                " <aside class=\"toc sidebar\" data-title=\"Contents\" data-levels=\"3\">\n" +
                "  <div class=\"toc-menu\">\n" +
                "   <h3>Contents</h3>\n" +
                "   <ul>\n" +
                "    <li data-level=\"1\"><a href=\"#l2\">Header Level 2</a></li>\n" +
                "    <li data-level=\"2\"><a href=\"#l3\">Header Level 3</a></li>\n" +
                "   </ul>\n" +
                "  </div>\n" +
                " </aside>\n" +
                "</body>");
        runCreateContentToc(1, 1, 3, html, "" +
                "<body>\n" +
                " <aside class=\"toc sidebar\" data-title=\"Contents\" data-levels=\"3\">\n" +
                "  <div class=\"toc-menu\">\n" +
                "   <h3>Contents</h3>\n" +
                "   <ul>\n" +
                "    <li data-level=\"1\"><a href=\"#title\">Page</a></li>\n" +
                "    <li data-level=\"2\"><a href=\"#l2\">Header Level 2</a></li>\n" +
                "    <li data-level=\"3\"><a href=\"#l3\">Header Level 3</a></li>\n" +
                "   </ul>\n" +
                "  </div>\n" +
                " </aside>\n" +
                "</body>");
        runCreateContentToc(1, 1, 2, html, "" +
                "<body>\n" +
                " <aside class=\"toc sidebar\" data-title=\"Contents\" data-levels=\"2\">\n" +
                "  <div class=\"toc-menu\">\n" +
                "   <h3>Contents</h3>\n" +
                "   <ul>\n" +
                "    <li data-level=\"1\"><a href=\"#title\">Page</a></li>\n" +
                "    <li data-level=\"2\"><a href=\"#l2\">Header Level 2</a></li>\n" +
                "   </ul>\n" +
                "  </div>\n" +
                " </aside>\n" +
                "</body>");
    }

    private void runCreateContentToc(String documentHtml, String expectedContent) {
        runCreateContentToc(2, 1, 3, documentHtml, expectedContent);
    }

    private void runCreateContentToc(String documentHtml, String expectedContent, int hLevel, int dataLevel, int dataLevelEnd) {
        runCreateContentToc(hLevel, dataLevel, dataLevelEnd, documentHtml, expectedContent);
    }

    private void runCreateContentToc(int hLevel, int dataLevel, int dataLevelEnd, String documentHtml, String expectedContent) {
        ConfigurationOptions options = new ConfigurationOptions().createToc(true);

        Element elementToInsert = Jsoup.parseBodyFragment("<div id=\"id\">" + documentHtml + "</div>")
                .getElementById("id");

        Element body = Document.createShell("")
                .body();
        Impl.createContentToc(options, elementToInsert, hLevel, dataLevel, dataLevelEnd, body);

        String content = body.toString();
        assertThat(content).isEqualTo(expectedContent);
    }

    @Test
    void testFindId() {
        assertThat(runFindId("h1", "<h1>title</h1>")).isNotPresent();
        assertThat(runFindId("h3", "<h3 class=\"test-class\" >title</h3>")).isNotPresent();
        assertThat(runFindId("h2", "<h2 id=\"test-id\" >title</h2>")).hasValue("test-id");
        assertThat(runFindId("h3", "<h3><a id=\"my-id\">title</a></h3>")).hasValue("my-id");
        assertThat(runFindId("h3", "<h3><a /><a id=\"a-id\"></a>title</h3>")).hasValue("a-id");
        assertThat(runFindId("h3", "<h3><a id=\"x-id\"></a><a id=\"y-id\"></a>test</h3>")).hasValue("x-id");
    }

    private Optional<String> runFindId(String selector, String html) {
        Element element = Jsoup.parseBodyFragment(html)
                .selectFirst(selector);
        return Impl.findId(element);
    }

    @Test
    void testFileHash() {
        byte[] bytes = new byte[] { 0, 1, 2, 3, 4 };

        assertThat(Impl.createFileHash(RewriteStrategy.NO_MODIFICATION, null)).isNull();
        assertThat(Impl.createFileHash(RewriteStrategy.SHA1_SUFFIX, null)).isNull();
        assertThat(Impl.createFileHash(RewriteStrategy.SHORT_SHA1_SUFFIX, null)).isNull();
        assertThat(Impl.createFileHash(RewriteStrategy.SHA1_SUB_FOLDER, null)).isNull();
        assertThat(Impl.createFileHash(RewriteStrategy.SHORT_SHA1_SUB_FOLDER, null)).isNull();

        assertThat(Impl.createFileHash(RewriteStrategy.NO_MODIFICATION, bytes)).isNull();
        assertThat(Impl.createFileHash(RewriteStrategy.SHA1_SUFFIX, bytes)).isEqualTo("1cf251472d59f8fadeb3ab258e90999d8491be19");
        assertThat(Impl.createFileHash(RewriteStrategy.SHORT_SHA1_SUFFIX, bytes)).isEqualTo("1cf2514");
        assertThat(Impl.createFileHash(RewriteStrategy.SHA1_SUB_FOLDER, bytes)).isEqualTo("1cf251472d59f8fadeb3ab258e90999d8491be19");
        assertThat(Impl.createFileHash(RewriteStrategy.SHORT_SHA1_SUB_FOLDER, bytes)).isEqualTo("1cf2514");
    }

    @Test
    void testCreateRelativeFilePath() {
        String sha1 = "1cf251472d59f8fadeb3ab258e90999d8491be19";
        String shortSha1 = "1cf2514";

        assertThat(Impl.createRelativeFilePath(RewriteStrategy.NO_MODIFICATION, "sub/", "image.png", null)).isEqualTo("sub/image.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUFFIX, "folder/", "image.png", null)).isEqualTo("folder/image.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUFFIX, "sub/", "image.svg", null)).isEqualTo("sub/image.svg");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUB_FOLDER, "folder/", "image.png", null)).isEqualTo("folder/image.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUB_FOLDER, "folder/sub/", "image.svg", null)).isEqualTo("folder/sub/image.svg");

        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUFFIX, "folder/", "image.png", sha1)).isEqualTo("folder/image_1cf251472d59f8fadeb3ab258e90999d8491be19.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUFFIX, "folder/", "this.image.png", sha1)).isEqualTo("folder/this.image_1cf251472d59f8fadeb3ab258e90999d8491be19.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUFFIX, "folder/", "file", sha1)).isEqualTo("folder/file_1cf251472d59f8fadeb3ab258e90999d8491be19");

        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUFFIX, "folder/sub/", "image.png", shortSha1)).isEqualTo("folder/sub/image_1cf2514.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUFFIX, "folder/sub/", "file", shortSha1)).isEqualTo("folder/sub/file_1cf2514");

        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUB_FOLDER, "folder/", "image.png", sha1)).isEqualTo("folder/1cf251472d59f8fadeb3ab258e90999d8491be19/image.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHA1_SUB_FOLDER, "folder/", "file", sha1)).isEqualTo("folder/1cf251472d59f8fadeb3ab258e90999d8491be19/file");

        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUB_FOLDER, "sub/", "image.png", shortSha1)).isEqualTo("sub/1cf2514/image.png");
        assertThat(Impl.createRelativeFilePath(RewriteStrategy.SHORT_SHA1_SUB_FOLDER, "sub/", "file", shortSha1)).isEqualTo("sub/1cf2514/file");
    }

    private static String renderFolder(Path folder) {
        StringBuilder sb = new StringBuilder();
        renderFolder(folder, sb, false, new ArrayList<>());
        return sb.toString();
    }

    private static void renderFolder(Path folder, StringBuilder sb, boolean isLast, List<Boolean> hierarchyTree) {
        indent(sb, isLast, hierarchyTree);
        sb.append(folder.getFileName())
                .append("\n");

        if (Files.isDirectory(folder)) {
            List<Path> objects;
            try (Stream<Path> s = Files.list(folder)) {
                objects = s.sorted()
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IllegalStateException("Could not list the files in: " + folder, e);
            }

            for (int i = 0; i < objects.size(); i++) {
                boolean last = ((i + 1) == objects.size());

                hierarchyTree.add(i != objects.size() - 1);
                renderFolder(objects.get(i), sb, last, hierarchyTree);

                hierarchyTree.remove(hierarchyTree.size() - 1);
            }
        }
    }

    private static void indent(StringBuilder sb, boolean isLast, List<Boolean> hierarchyTree) {
        for (int i = 0; i < hierarchyTree.size() - 1; ++i) {
            if (hierarchyTree.get(i)) {
                sb.append("\u2502   ");
            } else {
                sb.append("    ");
            }
        }

        if (hierarchyTree.size() > 0) {
            sb.append(isLast ? "\u2514\u2500\u2500" : "\u251c\u2500\u2500");
        }
    }

}
