package com.quaxantis.etui.swing.taginfo;

import com.quaxantis.etui.HasDescription;
import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

public class TagInfoPanel extends JEditorPane {

    private static final Logger log = LoggerFactory.getLogger(TagInfoPanel.class);

//    public static void main() {
//
//        Tag search = Tag.of("XMP", "Date");
//        Tag tag = new TagRepository().getFamilies()
//                .stream()
//                .map(TagFamily::tags)
//                .flatMap(List::stream)
//                .filter(search::isSameTagAs)
//                .findAny()
//                .orElseThrow();
//
//        {
//            JFrame jFrame = new JFrame("TagInfo original");
//            jFrame.getContentPane().add(createTagInfoPanel(tag));
//            jFrame.setPreferredSize(new Dimension(320, 200));
//            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            jFrame.pack();
//            jFrame.setVisible(true);
//        }
//
//        {
//            JFrame jFrame = new JFrame("TagInfoPanel");
//            TagInfoPanel panel = new TagInfoPanel();
//            panel.setTag(tag);
//            jFrame.getContentPane().add(panel);
//            jFrame.setPreferredSize(new Dimension(320, 200));
//            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            jFrame.pack();
//            jFrame.setVisible(true);
//        }
//
//
//        {
//            JFrame jFrame = new JFrame("TagFormat original");
//            jFrame.getContentPane().add(createTagFormatPanel(tag));
//            jFrame.setPreferredSize(new Dimension(480, 320));
//            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            jFrame.pack();
//            jFrame.setVisible(true);
//        }
//        {
//            JFrame jFrame = new JFrame("TagInfoPanel");
//            TagInfoPanel panel = new TagInfoPanel(StandardInfoItem.FORMAT_DESCRIPTION, StandardInfoItem.FORMAT_EXAMPLES.wrapped("<p><strong>examples</strong>", "</p>"));
//            panel.setTag(tag);
//            panel.setExamplePatternAction(pattern -> System.out.println("Pattern activated : " + pattern));
//            panel.setHyperlinkAction(url -> System.out.println("Url activated : " + url));
//
//            jFrame.getContentPane().add(new JScrollPane(panel));
//            jFrame.setPreferredSize(new Dimension(480, 320));
//            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            jFrame.pack();
//            jFrame.setVisible(true);
//        }
//
//    }

    private final List<InfoItem> items;
    private Consumer<String> examplePatternAction;
    private Consumer<URL> hyperlinkAction;

    public TagInfoPanel() {
        this(List.of(StandardInfoItem.LABEL, StandardInfoItem.DESCRIPTION));
    }

    public TagInfoPanel(InfoItem... items) {
        this.items = List.of(items);
        initialize();
        setTag(null);
    }

    public TagInfoPanel(List<InfoItem> items) {
        this.items = List.copyOf(items);
        initialize();
        setTag(null);
    }

    private void initialize() {
        this.setContentType("text/html");
        this.setEditable(false);
        this.setBackground(null);
        this.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (this.examplePatternAction != null && e.getDescription().startsWith("pattern:")) {
                    this.examplePatternAction.accept(e.getDescription().substring("pattern:".length()));
                } else if (this.hyperlinkAction != null && e.getURL() != null) {
                    this.hyperlinkAction.accept(e.getURL());
                }
            }
        });
    }

    private String generateContents(Object element) {
        if (element == null) {
            return "";
        } else {
            return "<html><body>"
                   + this.items.stream()
                           .map(item -> item.formatAsHtml(element))
                           .collect(joining())
                   + "</body></html>";
        }
    }

    public void setTag(Object element) {
        setText(generateContents(element));
        setCaretPosition(0);
    }

    public interface InfoItem {
        String formatAsHtml(Object element);

        default InfoItem wrapped(String prefix, String suffix) {
            return element -> {
                String html = this.formatAsHtml(element);
                return (html == null || html.isBlank())? "" : prefix + html + suffix;
            };
        }
    }

    public enum StandardInfoItem implements InfoItem {
        // TODO: right aligned qualified tag with copy-to-clipboard handle
        LABEL(element -> (element instanceof Tag tag) ?
                (element instanceof HasLabel hasLabel && hasLabel.label() != null && !hasLabel.label().equalsIgnoreCase(tag.tagName())) ?
                        "<b>%s:%s</b> (%s)".formatted(tag.groupName(), tag.tagName(), hasLabel.label()) :
                        "<b>%s:%s</b>".formatted(tag.groupName(), tag.tagName()) :
                (element instanceof HasLabel hasLabel && hasLabel.label() != null) ?
                        "<b>%s</b>".formatted(hasLabel.label()) :
                        ""
        ),
        DESCRIPTION(HasDescription.class, hasDescription ->
                paragraphs(hasDescription.description())),
        FORMAT_DESCRIPTION(TagDescriptor.class, tag ->
                (tag.formatDescription() instanceof String formatDescription) ? paragraphs(formatDescription) : ""),
        FORMAT_EXAMPLES(TagDescriptor.class, tag ->
                (tag.examples() instanceof List<? extends TagDescriptor.Example> examples && !examples.isEmpty()) ?
                        "<ul style='padding: 0px; margin: 0px; columns: 2;'>"
                        + examples.stream().map(example -> "<li>" + exampleLink(example) + "</li>").collect(joining())
                        + "</ul>" :
                        ""
        );
        private final Function<Object, String> formatter;

        StandardInfoItem(Function<Object, String> formatter) {
            this.formatter = formatter;
        }

        <T> StandardInfoItem(Class<T> type, Function<T, String> formatter) {
            this.formatter = element -> (type.isInstance(element)) ? formatter.apply(type.cast(element)) : "";
        }

        @Override
        public String formatAsHtml(Object element) {
            return formatter.apply(element);
        }

        private static String paragraphs(String string) {
            if (string == null || string.isBlank()) {
                return "";
            }
            return "<p>" + string.strip().replaceAll("\\v+", "</p><p>") + "</p>";
        }
    }

    public void setExamplePatternAction(Consumer<String> patternAction) {
        this.examplePatternAction = patternAction;
    }

    public void setHyperlinkAction(Consumer<URL> hyperlinkAction) {
        this.hyperlinkAction = hyperlinkAction;
    }

    public static void openWithDesktop(URL url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(url.toURI());
            } catch (IOException | URISyntaxException ex) {
                log.error("Unable to open URL " + url);
            }
        }
    }

//    private static Container createTagFormatPanel(Tag tag) {
//
//        JEditorPane pane = new JEditorPane();
//        pane.setContentType("text/html");
//        pane.setEditable(false);
//        pane.setBackground(null);
//        pane.addHyperlinkListener(e -> {
//            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//                if (e.getDescription().startsWith("pattern:")) {
//                    System.out.println("Pattern activated : " + e.getDescription().substring("pattern:".length()));
////                    form.modifyData(bean -> bean.setValue(bean.value() + e.getDescription().substring("pattern:".length())));
//                } else if (e.getURL() != null) {
//                    System.out.println("Url activated : " + e.getURL());
////                    if (Desktop.isDesktopSupported()) {
////                        try {
////                            Desktop.getDesktop().browse(e.getURL().toURI());
////                        } catch (IOException | URISyntaxException ex) {
////                            log.error("Unable to open URL " + e.getURL());
////                        }
////                    }
//                }
//            }
//        });
//
////        form.addFormChangeListener(f -> {
////            Tag enteredTag = f.getData().tag();
//        String text = Optional.of(tag)
//                .filter(TagDescriptor.class::isInstance)
//                .map(TagDescriptor.class::cast)
//                .flatMap(TagInfoPanel::getFormatInfoAsHtml)
//                .orElse("");
//        pane.setText(text);
//        pane.setCaretPosition(0);
////        });
//        return new JScrollPane(pane);
//    }


//    private static Optional<String> getFormatInfoAsHtml(TagDescriptor tag) {
//        StringBuilder builder = new StringBuilder();
//
//        if (tag.formatDescription() instanceof String formatDescription) {
//            builder.append("<p>").append(formatDescription).append("</p>");
//        }
//        if (tag.examples() instanceof List<? extends TagDescriptor.Example> examples && !examples.isEmpty()) {
//            builder.append("<p><strong>examples</strong><ul style='padding: 0px; margin: 0px; columns: 2;'>");
//            examples.forEach(example -> builder.append("<li>").append(exampleLink(example)).append("</li>"));
//            builder.append("</ul></p>");
//        }
//
//        var html = Optional.of(builder)
//                .filter(Predicate.not(StringBuilder::isEmpty))
//                .map(content -> "<html>" + content + "</html>");
//        System.out.println("-----");
//        System.out.println(html.orElse("empty"));
//        return html;
//    }

    private static String exampleLink(TagDescriptor.Example example) {
        if (example.pattern() != null) {
            if (example.value() != null) {
                if ("now".equals(example.value())) {
                    String value = DateTimeFormatter.ofPattern(example.pattern()).format(OffsetDateTime.now());
                    if (StringUtils.isEmpty(example.text())) {
                        return "<a href='pattern:%1$s'>%1$s</a>".formatted(value);
                    } else {
                        return "<a href='pattern:%1$s'>%1$s</a> : %2$s".formatted(value, example.text());
                    }
                } else {
                    return "<a href='pattern:%1$s'>%2$s</a> : %3$s".formatted(example.pattern(), example.value(), example.text());
                }
            } else {
                return "<a href='pattern:%1$s'>%2$s</a>".formatted(example.pattern(), example.text());
            }
        } else if (example.value() != null) {
            return "<a href='pattern:%1$s'>%1$s</a> : %2$s".formatted(example.value(), example.text());
        } else {
            return example.text();
        }
    }
}
