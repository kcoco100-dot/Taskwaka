package com.tk.app.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.AbstractSimpleMarkupHandler;
import org.attoparser.simple.SimpleMarkupParser;
import org.springframework.web.util.HtmlUtils;
import org.unbescape.html.HtmlEscape;

/** Thymeleaf에 포함된 HTML 파서로 읽고, 허용한 요소와 속성만 다시 출력한다. */
final class NoticeHtml extends AbstractSimpleMarkupHandler {
    private static final Set<String> TAGS = Set.of("a", "b", "blockquote", "br", "cite", "code",
            "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "i", "img", "li", "ol", "p",
            "pre", "q", "s", "small", "span", "strike", "strong", "sub", "sup", "u", "ul");
    private static final Set<String> OMIT_CONTENT = Set.of("script", "style", "iframe", "object", "template", "svg", "math");
    private static final List<String> IMAGE_INFO = List.of("alt", "title", "align", "height", "width");
    private static final Pattern EDITOR_IMAGE = Pattern.compile("/uploads/editor/(?!\\.{1,2}$)[a-zA-Z0-9._-]+");
    private static final Set<String> LINK_SCHEMES = Set.of("http", "https", "ftp", "mailto");
    private static final SimpleMarkupParser PARSER = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());

    private final StringBuilder output = new StringBuilder();
    private int omittedDepth;

    static String clean(String html) {
        if (html == null || html.isEmpty()) return html;
        NoticeHtml handler = new NoticeHtml();
        try {
            PARSER.parse(html, handler);
            return handler.output.toString();
        } catch (ParseException ex) {
            return HtmlUtils.htmlEscape(html);
        }
    }

    @Override
    public void handleText(char[] text, int offset, int len, int line, int col) {
        if (omittedDepth == 0) {
            output.append(HtmlUtils.htmlEscape(HtmlEscape.unescapeHtml(new String(text, offset, len))));
        }
    }

    @Override
    public void handleOpenElement(String name, Map<String, String> attributes, int line, int col) {
        String tag = name.toLowerCase(Locale.ROOT);
        if (OMIT_CONTENT.contains(tag)) omittedDepth++;
        if (omittedDepth != 0 || !TAGS.contains(tag)) return;
        if (tag.equals("img")) {
            String src = attribute(attributes, "src");
            if (!EDITOR_IMAGE.matcher(src).matches()) return;
            output.append("<img");
            writeAttribute("src", src);
            for (String key : IMAGE_INFO) {
                writeAttribute(key, attribute(attributes, key));
            }
        } else {
            output.append('<').append(tag);
        }
        if (tag.equals("a")) {
            writeAttribute("href", safeUrl(attribute(attributes, "href"), LINK_SCHEMES));
            writeAttribute("rel", "nofollow");
        } else if (tag.equals("blockquote") || tag.equals("q")) {
            writeAttribute("cite", safeUrl(attribute(attributes, "cite"), Set.of("http", "https")));
        }
        output.append('>');
    }

    @Override
    public void handleCloseElement(String name, int line, int col) {
        String tag = name.toLowerCase(Locale.ROOT);
        if (OMIT_CONTENT.contains(tag)) {
            if (omittedDepth > 0) omittedDepth--;
        } else if (omittedDepth == 0 && TAGS.contains(tag) && !tag.equals("img") && !tag.equals("br")) {
            output.append("</").append(tag).append('>');
        }
    }

    @Override
    public void handleAutoCloseElement(String name, int line, int col) {
        handleCloseElement(name, line, col);
    }

    @Override
    public void handleStandaloneElement(String name, Map<String, String> attrs, boolean slash, int line, int col) {
        handleOpenElement(name, attrs, line, col);
        handleCloseElement(name, line, col);
    }

    private static String attribute(Map<String, String> attrs, String key) {
        if (attrs != null) {
            for (var entry : attrs.entrySet()) {
                if (key.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                    return HtmlEscape.unescapeHtml(entry.getValue());
                }
            }
        }
        return "";
    }

    private static String safeUrl(String value, Set<String> schemes) {
        try {
            URI uri = new URI(value.trim());
            if (!uri.isAbsolute() || schemes.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
                return uri.toString();
            }
        } catch (URISyntaxException ignored) {
            // 브라우저가 다른 방식으로 해석할 수 있는 주소는 출력하지 않는다.
        }
        return "";
    }

    private void writeAttribute(String key, String value) {
        if (!value.isEmpty()) {
            output.append(' ').append(key).append("=\"").append(HtmlUtils.htmlEscape(value)).append('"');
        }
    }
}
