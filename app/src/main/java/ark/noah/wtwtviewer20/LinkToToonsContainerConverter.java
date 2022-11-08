package ark.noah.wtwtviewer20;

public class LinkToToonsContainerConverter {
    public static LinkToToonsContainerConverter Instance;

    public LinkToToonsContainerConverter() {
        if (Instance == null) Instance = this;
    }

//    public ToonsContainer convertToToonsContainer(String urlInString) {
//        LinkValidater.Info info = LinkValidater.extractInfo(urlInString);
//    }
}
