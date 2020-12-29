package fr.jmini.utils.htmlpublish.helper;

public enum IndexHandling {

    /**
     * The index file inside the folder is not used.
     */
    SKIP,

    /**
     * Use the tile of the page (using the {@link ConfigurationPageOptions#getTitleSelector()}). The page content is not included.
     */
    USE_TITLE_ONLY,

    /**
     * Use the index page as root node
     */
    USE_PAGE_AS_PARENT,

    /**
     * Use the index page as any other page in the list
     */
    USE_PAGE_IN_THE_LIST

}
