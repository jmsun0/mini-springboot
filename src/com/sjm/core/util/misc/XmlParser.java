package com.sjm.core.util.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.function.Function;

import com.sjm.core.util.core.Lex;
import com.sjm.core.util.core.MyStringBuilder;

public class XmlParser {
    public static void main(String[] args) throws Exception {
        XmlLex lex =
                getLex(new BufferedReader(new FileReader("test/RDSTodayDataChangeMapper.xml")));
        while (lex.next() != XmlKey.EOF) {
            System.out.println(new MyStringBuilder().append(lex.getKey()).append("   ----   \"")
                    .appendEscape(lex.getString(), -1, -1).append("\""));
        }
    }

    public static XmlLex getLex(String str) {
        XmlStringLex lex = new XmlStringLex();
        lex.reset(str);
        return lex;
    }

    public static XmlLex getLex(Reader reader) {
        XmlReaderLex lex = new XmlReaderLex();
        lex.reset(reader);
        return lex;
    }

    public static void skipToRoot(XmlLex lex) {
        while (true) {
            switch (lex.next()) {
                case TAG_LEFT:
                    return;
                case EOF:
                    throw lex.newError();
                default:
                    break;
            }
        }
    }

    public static enum XmlKey {
        TAG_LEFT(lex -> lex.getSubString(1, 0)), //
        TAG_RIGHT_FINISH(lex -> lex.getSubString(0, 2)), //
        TAG_RIGHT_UNFINISH(lex -> lex.getSubString(0, 1)), //
        TAG_END(lex -> lex.getSubString(2, 1)), //
        QTAG_LEFT(lex -> lex.getSubString(2, 0)), //
        QTAG_RIGHT(lex -> lex.getSubString(0, 2)), //
        TTAG_LEFT(lex -> lex.getSubString(2, 0)), //
        TTAG_RIGHT(lex -> lex.getSubString(0, 1)), //
        STRING(lex -> lex.getOriginString()), //
        EQUAL(lex -> "="), //
        LITERAL(lex -> lex.getLiteralString()), //
        EOF(lex -> ""), //
        TEXT(lex -> lex.getOriginString()), //
        CDATA(lex -> lex.getSubString(9, 3)), //
        COMMENT(lex -> lex.getSubString(4, 3)),//

        ;
        final Function<InternalXmlLex, String> func;

        private XmlKey(Function<InternalXmlLex, String> func) {
            this.func = func;
        }

        private XmlKey() {
            this(null);
        }
    }

    public interface XmlLex extends CharSequence {
        public XmlKey next();

        public XmlKey getKey();

        public String getString();

        public RuntimeException newError();
    }

    interface InternalXmlLex extends XmlLex, Lex {
        public void finish(XmlKey result);

        public void switchStart(int index);

        public String getOriginString();

        public String getSubString(int left, int right);

        public String getLiteralString();
    }

    static class XmlLexBuilder extends Lex.Builder<InternalXmlLex> {
        static final Lex.DFAState[] START = new XmlLexBuilder().build();

        public Lex.DFAState[] build() {
            initNFA();

            defineActionTemplate("finish", (lex, a) -> lex.finish((XmlKey) a[0]));
            defineActionTemplate("switchStart", (lex, a) -> lex.switchStart((int) a[0]));

            for (XmlKey key : XmlKey.values())
                defineVariable(key.name(), key);

            definePattern("STR", "[a-zA-Z_][a-zA-Z_0-9]*");
            definePattern("BLANK", "[\\r\\n\\t\\b\\f ]+");
            definePattern("ESCAPE", "\\\\(u[0-9a-fA-F]{4})|([^u])");
            definePattern("LITERAL_1", "\'(${ESCAPE}|[^(\\\\\\\')])*\'");
            definePattern("LITERAL_2", "\"(${ESCAPE}|[^(\\\\\\\")])*\"");
            definePattern("LITERAL", "${LITERAL_1}|${LITERAL_2}");

            addPattern("START_0", "[$]#{finish(EOF)}");
            addPattern("START_0", "<${STR}#{switchStart(1)}#{finish(TAG_LEFT)}");
            addPattern("START_0", "</${STR}>#{finish(TAG_END)}");
            addPattern("START_0", "<[?]${STR}#{switchStart(2)}#{finish(QTAG_LEFT)}");
            addPattern("START_0", "<!${STR}#{switchStart(3)}#{finish(TTAG_LEFT)}");
            addPattern("START_0", "[^(<$)]+#{finish(TEXT)}");

            addPattern("START_0", "<!\\[CDATA\\[", "CDATA_0");
            addTransfer("CDATA_0", "]", "CDATA_1");
            addTransfer("CDATA_0", "[^(\\]$)]", "CDATA_0");
            addTransfer("CDATA_1", "]", "CDATA_2");
            addTransfer("CDATA_1", "[^(\\]$)]", "CDATA_0");
            addTransfer("CDATA_2", ">", "CDATA_3");
            addTransfer("CDATA_2", "]", "CDATA_2");
            addTransfer("CDATA_2", "[^(>\\]$)]", "CDATA_0");
            bindAction("CDATA_3", "#{finish(CDATA)}");

            addPattern("START_0", "<!--", "COMMENT_0");
            addTransfer("COMMENT_0", "-", "COMMENT_1");
            addTransfer("COMMENT_0", "[^(\\-$)]", "COMMENT_0");
            addTransfer("COMMENT_1", "-", "COMMENT_2");
            addTransfer("COMMENT_1", "[^(\\-$)]", "COMMENT_0");
            addTransfer("COMMENT_2", ">", "COMMENT_3");
            addTransfer("COMMENT_2", "-", "COMMENT_2");
            addTransfer("COMMENT_2", "[^(>\\-$)]", "COMMENT_0");
            bindAction("COMMENT_3", "#{finish(COMMENT)}");

            addPattern("START_1", "/>#{switchStart(0)}#{finish(TAG_RIGHT_FINISH)}");
            addPattern("START_1", ">#{switchStart(0)}#{finish(TAG_RIGHT_UNFINISH)}");
            addPattern("START_1", "${BLANK}#{finish(null)}");
            addPattern("START_1", "${STR}#{finish(STRING)}");
            addPattern("START_1", "=#{finish(EQUAL)}");
            addPattern("START_1", "${LITERAL}#{finish(LITERAL)}");

            addPattern("START_2", "[?]>#{switchStart(0)}#{finish(QTAG_RIGHT)}");
            addPattern("START_2", "${BLANK}#{finish(null)}");
            addPattern("START_2", "${STR}#{finish(STRING)}");
            addPattern("START_2", "=#{finish(EQUAL)}");
            addPattern("START_2", "${LITERAL}#{finish(LITERAL)}");

            addPattern("START_3", ">#{switchStart(0)}#{finish(TTAG_RIGHT)}");
            addPattern("START_3", "${BLANK}#{finish(null)}");
            addPattern("START_3", "${STR}#{finish(STRING)}");
            addPattern("START_3", "${LITERAL}#{finish(LITERAL)}");

            return buildDFA("START_0", "START_1", "START_2", "START_3");
        }
    }

    static class XmlStringLex extends Lex.StringLex<XmlKey> implements InternalXmlLex {
        public XmlStringLex() {
            super(XmlLexBuilder.START[0]);
        }

        @Override
        public void switchStart(int index) {
            start = XmlLexBuilder.START[index];
        }

        @Override
        public String getString() {
            return key.func.apply(this);
        }

        @Override
        public String getOriginString() {
            return str.substring(begin, index);
        }

        @Override
        public String getSubString(int left, int right) {
            return str.substring(begin + left, index - right);
        }

        private MyStringBuilder mybuffer = new MyStringBuilder();

        @Override
        public String getLiteralString() {
            return mybuffer.clear().appendUnEscape(str, begin + 1, index - 1).toString();
        }

        @Override
        public RuntimeException newError(String message) {
            return new XmlParseException(message);
        }

        @Override
        public char charAt(int index) {
            return str.charAt(begin + index);
        }

        @Override
        public int length() {
            return index - begin;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return str.substring(begin + start, begin + end);
        }
    }

    static class XmlReaderLex extends Lex.ReaderLex<XmlKey> implements InternalXmlLex {
        public XmlReaderLex() {
            super(XmlLexBuilder.START[0]);
        }

        @Override
        public void switchStart(int index) {
            start = XmlLexBuilder.START[index];
        }

        @Override
        public String getString() {
            return key.func.apply(this);
        }

        @Override
        public String getOriginString() {
            return new String(buffer, begin, index - begin);
        }

        @Override
        public String getSubString(int left, int right) {
            return new String(buffer, begin + left, index - begin - left - right);
        }

        private MyStringBuilder mybuffer = new MyStringBuilder();

        @Override
        public String getLiteralString() {
            return mybuffer.clear().appendUnEscape(buffer, begin + 1, index - 1).toString();
        }

        @Override
        public RuntimeException newError(String message) {
            return new XmlParseException(message);
        }

        @Override
        public char charAt(int index) {
            return buffer[begin + index];
        }

        @Override
        public int length() {
            return index - begin;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new String(buffer, begin + start, end - start);
        }
    }
    public static class XmlParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public XmlParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public XmlParseException(Throwable cause) {
            super(cause);
        }

        public XmlParseException(String message) {
            super(message);
        }

        public XmlParseException() {
            super();
        }
    }
}
