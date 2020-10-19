package com.sjm.core.mybatis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sjm.core.util.core.ArrayController;
import com.sjm.core.util.core.Strings;
import com.sjm.core.util.misc.XmlParser;

public class MybatisXml {
    public static void main(String[] args) throws FileNotFoundException {
        MybatisXmlParser.parse(new FileReader("test/RDSTodayDataChangeMapper.xml"));
    }

    public static class MybatisXmlContext {
        public StringBuilder sql = new StringBuilder();
        public List<Object> args = new ArrayList<>();
        public Ognl.OgnlContext ognlCtx;
    }

    public interface MybatisXmlNode {
        public void appendTo(MybatisXmlContext ctx);
    }

    public static class MybatisXmlFrameNode implements MybatisXmlNode {
        public MybatisXmlNode[] nodes;

        public MybatisXmlFrameNode(MybatisXmlNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            for (MybatisXmlNode node : nodes) {
                node.appendTo(ctx);
            }
        }
    }

    public static class MybatisXmlTextNode implements MybatisXmlNode {
        public String text;

        public MybatisXmlTextNode(String text) {
            this.text = text;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            ctx.sql.append(text);
        }
    }

    public static class MybatisXmlDollerNode implements MybatisXmlNode {
        public Ognl.OgnlExpression expr;

        public MybatisXmlDollerNode(Ognl.OgnlExpression expr) {
            this.expr = expr;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            ctx.sql.append(expr.apply(ctx.ognlCtx));
        }
    }

    public static class MybatisXmlWellNode implements MybatisXmlNode {
        public Ognl.OgnlExpression expr;

        public MybatisXmlWellNode(Ognl.OgnlExpression expr) {
            this.expr = expr;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            ctx.sql.append('?');
            ctx.args.add(expr.apply(ctx.ognlCtx));
        }
    }

    public static class MybatisXmlIncludeNode implements MybatisXmlNode {
        public String refid;
        public MybatisXmlFrameNode frame;

        public MybatisXmlIncludeNode(String refid) {
            this.refid = refid;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            frame.appendTo(ctx);
        }
    }

    public static class MybatisXmlIfNode implements MybatisXmlNode {
        public Ognl.OgnlExpression test;
        public MybatisXmlFrameNode frame;

        public MybatisXmlIfNode(Ognl.OgnlExpression test, MybatisXmlFrameNode frame) {
            this.test = test;
            this.frame = frame;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            if (Ognl.toBoolean(test.apply(ctx.ognlCtx))) {
                frame.appendTo(ctx);
            }
        }
    }

    public static class MybatisXmlForeachNode implements MybatisXmlNode {
        public Ognl.OgnlExpression collection;
        public String item;
        public String index;
        public String open;
        public String close;
        public String separator;
        public MybatisXmlFrameNode frame;

        public MybatisXmlForeachNode(Ognl.OgnlExpression collection, String item, String index,
                String open, String close, String separator, MybatisXmlFrameNode frame) {
            this.collection = collection;
            this.item = item;
            this.index = index;
            this.open = open;
            this.close = close;
            this.separator = separator;
            this.frame = frame;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            appendTo(ctx, ctx.sql, item, index, open, close, separator, frame);
        }

        private void appendTo(MybatisXmlContext ctx, StringBuilder sql, String item, String index,
                String open, String close, String separator, MybatisXmlFrameNode frame) {
            Ognl.OgnlContext ognlCtx = ctx.ognlCtx;
            Object result = collection.apply(ognlCtx);
            if (result == null) {
            } else if (result instanceof List) {
                List<?> list = (List<?>) result;
                int len = list.size();
                if (len > 0) {
                    for (int i = 0; i < len; i++)
                        appendElement(ctx, sql, item, index, open, close, separator, frame, ognlCtx,
                                i, list.get(i));
                    deleteSuffix(sql, separator);
                }
            } else if (result instanceof Iterable) {
                int i = 0;
                for (Object obj : (Iterable<?>) result)
                    appendElement(ctx, sql, item, index, open, close, separator, frame, ognlCtx,
                            i++, obj);
                if (i > 0)
                    deleteSuffix(sql, separator);
            } else if (result instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) result;
                if (!map.isEmpty()) {
                    for (Map.Entry<?, ?> e : map.entrySet())
                        appendElement(ctx, sql, item, index, open, close, separator, frame, ognlCtx,
                                e.getKey(), e.getValue());
                    deleteSuffix(sql, separator);
                }
            } else {
                Class<?> clazz = result.getClass();
                if (clazz.isArray()) {
                    ArrayController<Object, Object> ctr =
                            ArrayController.valueOf(clazz.getComponentType());
                    int len = ctr.getLength(result);
                    if (len > 0) {
                        for (int i = 0; i < len; i++)
                            appendElement(ctx, sql, item, index, open, close, separator, frame,
                                    ognlCtx, i, ctr.get(result, i));
                        deleteSuffix(sql, separator);
                    }
                }
            }
        }

        private static void appendElement(MybatisXmlContext ctx, StringBuilder sql, String item,
                String index, String open, String close, String separator,
                MybatisXmlFrameNode frame, Ognl.OgnlContext ognlCtx, Object _index, Object _item) {
            if (index != null)
                ognlCtx.put(index, _index);
            if (item != null)
                ognlCtx.put(item, _item);
            if (open != null)
                sql.append(open);
            frame.appendTo(ctx);
            if (close != null)
                sql.append(close);
            if (separator != null)
                sql.append(separator);
        }

        private static void deleteSuffix(StringBuilder sql, String separator) {
            if (separator != null)
                sql.delete(sql.length() - separator.length(), sql.length());
        }
    }

    public static class MybatisXmlChooseNode implements MybatisXmlNode {
        public Ognl.OgnlExpression[] whens;
        public MybatisXmlFrameNode[] frames;
        public MybatisXmlFrameNode otherwise;

        public MybatisXmlChooseNode(Ognl.OgnlExpression[] whens, MybatisXmlFrameNode[] frames,
                MybatisXmlFrameNode otherwise) {
            this.whens = whens;
            this.frames = frames;
            this.otherwise = otherwise;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            for (int i = 0; i < whens.length; i++) {
                if (Ognl.toBoolean(whens[i].apply(ctx.ognlCtx))) {
                    frames[i].appendTo(ctx);
                    return;
                }
            }
            if (otherwise != null)
                otherwise.appendTo(ctx);
        }
    }

    public static class MybatisXmlBindNode implements MybatisXmlNode {
        public String name;
        public Ognl.OgnlExpression value;

        public MybatisXmlBindNode(String name, Ognl.OgnlExpression value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            Ognl.OgnlContext ognlCtx = ctx.ognlCtx;
            ognlCtx.put(name, value.apply(ognlCtx));
        }
    }

    public static class MybatisXmlTrimNode implements MybatisXmlNode {
        public String prefix;
        public String suffix;
        public String prefixOverrides;
        public String suffixOverrides;
        public MybatisXmlFrameNode frame;

        public MybatisXmlTrimNode(String prefix, String suffix, String prefixOverrides,
                String suffixOverrides, MybatisXmlFrameNode frame) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.prefixOverrides = prefixOverrides;
            this.suffixOverrides = suffixOverrides;
            this.frame = frame;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            StringBuilder sql = ctx.sql;
            if (prefix != null)
                sql.append(prefix);
            int from = sql.length();
            frame.appendTo(ctx);
            if (prefixOverrides != null) {
                int i = indexOf(sql, from, prefixOverrides);
                if (i != -1)
                    sql.delete(i, i + prefixOverrides.length());
            }
            if (suffixOverrides != null) {
                int i = lastIndexOf(sql, sql.length() - 1, suffixOverrides);
                if (i != -1)
                    sql.delete(i, i + suffixOverrides.length());
            }
            if (suffix != null)
                sql.append(suffix);
        }

        static int indexOf(StringBuilder sql, int from, String prefix) {
            for (int i = from, len = sql.length(); i < len; i++)
                if (!Strings.isBlank(sql.charAt(i)))
                    return sql.length() - from >= prefix.length() && equals(sql, i, prefix) ? i
                            : -1;
            return -1;
        }

        static int lastIndexOf(StringBuilder sql, int from, String suffix) {
            for (int i = from; i >= 0; i--)
                if (!Strings.isBlank(sql.charAt(i))) {
                    int left = i - suffix.length();
                    return left >= 0 && equals(sql, left, suffix) ? left : -1;
                }
            return -1;
        }

        private static boolean equals(StringBuilder sql, int from, String str) {
            for (int i = 0, len = str.length(); i < len; i++)
                if (sql.charAt(from + i) != str.charAt(i))
                    return false;
            return true;
        }
    }

    public static class MybatisXmlWhereNode implements MybatisXmlNode {
        public MybatisXmlFrameNode frame;

        public MybatisXmlWhereNode(MybatisXmlFrameNode frame) {
            this.frame = frame;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            StringBuilder sql = ctx.sql;
            sql.append(" where ");
            int from = sql.length();
            frame.appendTo(ctx);
            for (int i = from, len = sql.length(); i < len; i++) {
                char ch = sql.charAt(i);
                if (!Strings.isBlank(ch)) {
                    if (ch == 'a' && sql.charAt(i + 1) == 'n' && sql.charAt(i + 2) == 'd'
                            && Strings.isBlank(sql.charAt(i + 3)))
                        sql.delete(i, i + 4);
                    break;
                }
            }
        }
    }

    public static class MybatisXmlSetNode implements MybatisXmlNode {
        public MybatisXmlFrameNode frame;

        public MybatisXmlSetNode(MybatisXmlFrameNode frame) {
            this.frame = frame;
        }

        @Override
        public void appendTo(MybatisXmlContext ctx) {
            StringBuilder sql = ctx.sql;
            sql.append(" set ");
            frame.appendTo(ctx);
            for (int i = sql.length() - 1; i >= 0; i--) {
                char ch = sql.charAt(i);
                if (!Strings.isBlank(ch)) {
                    if (ch == ',')
                        sql.deleteCharAt(i);
                    break;
                }
            }
        }
    }

    public static class MybatisXmlNamespace {
        public String namespace;
        public Map<String, MybatisXmlFrameNode> sqls = new HashMap<>();
        public Map<String, MybatisXmlTag> statements = new HashMap<>();
    }

    public static class MybatisXmlTag {
        public String tagName;
        public String id;
        public String resultType;
        public String resultMap;
        public Mybatis.StatementType statementType;
        public int fetchSize;
        public int timeout;
        public String[] keyProperty;
        public boolean useGeneratedKeys;
        public String[] keyColumn;
        public MybatisXmlFrameNode frame;
    }

    static class MybatisXmlParser extends XmlParser {
        public static MybatisXmlNamespace parse(Reader reader) {
            XmlLex lex = XmlParser.getLex(reader);
            XmlParser.skipToRoot(lex);
            return matchNamespace(lex);
        }

        public static MybatisXmlNamespace matchNamespace(XmlLex lex) {
            matchKeyValueAndNext(lex, XmlKey.TAG_LEFT, "mapper");
            Map<String, String> attrs = matchAttrsAndTagRight(lex);
            lex.next();
            List<MybatisXmlTag> tags = matchTags(lex);
            matchKeyValueAndNext(lex, XmlKey.TAG_END, "mapper");
            return null;
        }

        private static List<MybatisXmlTag> matchTags(XmlLex lex) {
            List<MybatisXmlTag> tags = new ArrayList<>();
            while (true) {
                skipTagSeprator(lex);
                if (lex.getKey() == XmlKey.TAG_END)
                    break;
                String tagName = matchKeyForValueAndNext(lex, XmlKey.TAG_LEFT);
                Map<String, String> attrs = matchAttrsAndTagRight(lex);
                lex.next();
                MybatisXmlFrameNode frame = matchFrame(lex);
                matchKeyValueAndNext(lex, XmlKey.TAG_END, tagName);

                MybatisXmlTag tag = new MybatisXmlTag();
            }
            return tags;
        }

        private static MybatisXmlFrameNode matchFrame(XmlLex lex) {
            List<MybatisXmlNode> nodes = new ArrayList<>();
            L0: while (true) {
                switch (lex.getKey()) {
                    case TEXT:
                    case CDATA:
                        String text = lex.getString();
                        lex.next();
                        parseTextNode(text, nodes);
                        break;
                    case TAG_LEFT:
                        nodes.add(matchTagNode(lex));
                        break;
                    case COMMENT:
                        lex.next();
                        break;
                    case TAG_END:
                        break L0;
                    default:
                        throw lex.newError();
                }
            }
            return new MybatisXmlFrameNode(nodes.toArray(new MybatisXmlNode[nodes.size()]));
        }

        private static void parseTextNode(String text, List<MybatisXmlNode> nodes) {
            for (int i = 0, len = text.length(); i < len; i++) {
                char ch = text.charAt(i);
                if (ch == '#' || ch == '$') {

                }
            }
        }

        private static MybatisXmlNode matchTagNode(XmlLex lex) {
            String tagName = lex.getString();
            lex.next();
            Map<String, String> attrs = matchAttrsAndTagRight(lex);
            MybatisXmlFrameNode frame = null;
            if (tagName.equals("include") || tagName.equals("bind")) {
                if (lex.getKey() == XmlKey.TAG_RIGHT_UNFINISH) {
                    lex.next();
                    matchKeyValueAndNext(lex, XmlKey.TAG_END, tagName);
                } else {
                    lex.next();
                }
                if (tagName.equals("include")) {
                    String refid = getRequired(attrs, "refid");
                    return new MybatisXmlIncludeNode(refid);
                } else {
                    String name = getRequired(attrs, "name");
                    String value = getRequired(attrs, "value");
                    return new MybatisXmlBindNode(name, Ognl.parseExpression(value));
                }
            } else if (tagName.equals("choose")) {
                return matchChooseNode(lex);
            } else {
                matchKeyAndNext(lex, XmlKey.TAG_RIGHT_UNFINISH);
                frame = matchFrame(lex);
                matchKeyValueAndNext(lex, XmlKey.TAG_END, tagName);
            }
            switch (tagName) {
                case "if": {
                    String test = getRequired(attrs, "test");
                    return new MybatisXmlIfNode(Ognl.parseExpression(test), frame);
                }
                case "foreach": {
                    String collection = getRequired(attrs, "collection");
                    String item = getOrEmptyToNull(attrs, "item");
                    String index = getOrEmptyToNull(attrs, "index");
                    String open = getOrEmptyToNull(attrs, "open");
                    String close = getOrEmptyToNull(attrs, "close");
                    String separator = getOrEmptyToNull(attrs, "separator");
                    return new MybatisXmlForeachNode(Ognl.parseExpression(collection), item, index,
                            open, close, separator, frame);
                }
                case "trim": {
                    String prefix = getOrEmptyToNull(attrs, "prefix");
                    String suffix = getOrEmptyToNull(attrs, "suffix");
                    String prefixOverrides = getOrEmptyToNull(attrs, "prefixOverrides");
                    String suffixOverrides = getOrEmptyToNull(attrs, "suffixOverrides");
                    return new MybatisXmlTrimNode(prefix, suffix, prefixOverrides, suffixOverrides,
                            frame);
                }
                case "where":
                    return new MybatisXmlWhereNode(frame);
                case "set":
                    return new MybatisXmlSetNode(frame);
                default:
                    throw new IllegalArgumentException(tagName);
            }
        }

        private static MybatisXmlChooseNode matchChooseNode(XmlLex lex) {
            List<Ognl.OgnlExpression> whens = new ArrayList<>();
            List<MybatisXmlFrameNode> frames = new ArrayList<>();
            MybatisXmlFrameNode otherwise = null;
            matchKeyAndNext(lex, XmlKey.TAG_RIGHT_UNFINISH);
            while (true) {
                skipTagSeprator(lex);
                if (lex.getKey() == XmlKey.TAG_END)
                    break;
                String tagName = matchKeyForValueAndNext(lex, XmlKey.TAG_LEFT);
                Map<String, String> attrs = matchAttrsAndTagRight(lex);
                lex.next();
                MybatisXmlFrameNode frame = matchFrame(lex);
                matchKeyValueAndNext(lex, XmlKey.TAG_END, tagName);
                switch (tagName) {
                    case "when":
                        String test = getRequired(attrs, "test");
                        whens.add(Ognl.parseExpression(test));
                        frames.add(frame);
                        break;
                    case "otherwise":
                        if (otherwise != null)
                            throw new IllegalArgumentException("duplicate otherwise");
                        otherwise = frame;
                        break;
                    default:
                        throw new IllegalArgumentException(tagName);
                }
            }
            matchKeyValueAndNext(lex, XmlKey.TAG_END, "choose");
            return new MybatisXmlChooseNode(whens.toArray(new Ognl.OgnlExpression[whens.size()]),
                    frames.toArray(new MybatisXmlFrameNode[frames.size()]), otherwise);
        }

        private static Map<String, String> matchAttrsAndTagRight(XmlLex lex) {
            Map<String, String> attrs = new HashMap<>();
            L0: while (true) {
                switch (lex.getKey()) {
                    case STRING:
                        String key = lex.getString();
                        lex.next();
                        matchKeyAndNext(lex, XmlKey.EQUAL);
                        String value = matchKeyForValueAndNext(lex, XmlKey.LITERAL);
                        attrs.put(key, value);
                        break;
                    case TAG_RIGHT_FINISH:
                    case TAG_RIGHT_UNFINISH:
                        break L0;
                    default:
                        throw lex.newError();
                }
            }
            return attrs;
        }

        private static void matchKeyAndNext(XmlLex lex, XmlKey key) {
            if (lex.getKey() != key)
                throw lex.newError();
            lex.next();
        }

        private static void matchKeyValueAndNext(XmlLex lex, XmlKey key, String value) {
            if (lex.getKey() != key || !lex.getString().equals(value))
                throw lex.newError();
            lex.next();
        }

        private static String matchKeyForValueAndNext(XmlLex lex, XmlKey key) {
            if (lex.getKey() != key)
                throw lex.newError();
            String value = lex.getString();
            lex.next();
            return value;
        }

        private static void skipTagSeprator(XmlLex lex) {
            L0: while (true) {
                switch (lex.getKey()) {
                    case COMMENT:
                        lex.next();
                        break;
                    case TEXT:
                        if (!Strings.isBlank(lex, -1, -1))
                            throw lex.newError();
                        lex.next();
                        break;
                    default:
                        break L0;
                }
            }
        }

        private static String getRequired(Map<String, String> attrs, String key) {
            String value = attrs.get(key);
            if (value == null || value.isEmpty())
                throw new IllegalArgumentException(key);
            return value;
        }

        private static String getOrEmptyToNull(Map<String, String> attrs, String key) {
            String value = attrs.get(key);
            if (value != null && value.isEmpty())
                value = null;
            return value;
        }
    }

}
