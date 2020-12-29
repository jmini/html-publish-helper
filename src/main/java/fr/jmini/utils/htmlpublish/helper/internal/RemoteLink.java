package fr.jmini.utils.htmlpublish.helper.internal;

import java.nio.file.Path;

public class RemoteLink implements Link {

    private String absoluteUrl;

    public RemoteLink(String absoluteUrl) {
        this.absoluteUrl = absoluteUrl;
    }

    public String getAbsoluteUrl() {
        return absoluteUrl;
    }

    @Override
    public String getHrefValue(Path fromCurrentOutputPath) {
        return absoluteUrl;
    }

}
