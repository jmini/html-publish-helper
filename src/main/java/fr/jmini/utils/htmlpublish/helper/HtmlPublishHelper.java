package fr.jmini.utils.htmlpublish.helper;

import fr.jmini.utils.htmlpublish.helper.internal.Impl;

public class HtmlPublishHelper {

    /**
     * Publish the HTML files as configured
     *
     * @param configuration
     *            the holder containing the configuration.
     */
    public static void publish(ConfigurationHolder configuration) {
        Impl.run(configuration);
    }

}
