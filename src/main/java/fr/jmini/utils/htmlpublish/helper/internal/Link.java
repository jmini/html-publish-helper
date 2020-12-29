package fr.jmini.utils.htmlpublish.helper.internal;

import java.nio.file.Path;

public interface Link {

    String getHrefValue(Path fromCurrentOutputPath);

}
