package fr.jmini.utils.htmlpublish.helper;

public enum RewriteStrategy {

    /**
     * The resource name is kept unmodified
     */
    NO_MODIFICATION,

    /**
     * The sha1 is added just before the extension: `picture.png` -&gt; `picture_&lt;sha1&gt;.png`
     */
    SHA1_SUFFIX,

    /**
     * The 7 first characters of the sha1 are added just before the extension: `picture.png` -&gt; `picture_&lt;short-sha1&gt;.png`
     */
    SHORT_SHA1_SUFFIX,

    /**
     * The resource is moved to a folder named as with sha1 of the file: `picture.png` -&gt; `&lt;sha1&gt;/picture.png`
     */
    SHA1_SUB_FOLDER,

    /**
     * The resource is moved to a folder named with the 7 first characters of the sha1: `picture.png` -&gt; `&lt;short-sha1&gt;/picture.png`
     */
    SHORT_SHA1_SUB_FOLDER

}
