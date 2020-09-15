package com.sjm.core.util.misc;

import com.sjm.core.util.core.Lex;

public class XmlParser {
    public static void main(String[] args) {

    }

    public static enum XmlKey {
        TAG_LEFT, TAG_RIGHT_FINISH, TAG_RIGHT_UNFINISH, TAG_END, //
        QTAG_LEFT, QTAG_RIGHT, TTAG_LEFT, TTAG_RIGHT, TTAG_VALUE, //
        ATTR_KEY, ATTR_EQ, ATTR_VALUE, //
        TEXT, CDATA, COMMENT,//
    }

    static class XmlLex extends Lex.StringLex<XmlKey> {
        private static final DFAState[] START = new XmlLexBuilder().build();

        public XmlLex() {
            super(START[0]);
        }

        static class XmlLexBuilder extends Lex.Builder<XmlLex> {
            public DFAState[] build() {
                initNFA();

                defineActionTemplate("finish", (lex, a) -> lex.finish((XmlKey) a[0]));
                defineActionTemplate("switchStart", (lex, a) -> lex.start = START[(int) a[0]]);

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
                addPattern("START_0", "<${STR}/>#{finish(TAG_END)}");
                addPattern("START_0", "<?${STR}#{finish(QTAG_LEFT)}");
                addPattern("START_0", "?>#{finish(QTAG_RIGHT)}");
                addPattern("START_0", "<!${STR}#{finish(TTAG_LEFT)}");
                addPattern("START_0", "[^(<$)]+#{finish(TEXT)}");

                // addTransfer("START_0", conditionStr, "");
                addPattern("START_0", "<!\\[CDATA\\[[^]*\\]\\]>#{finish(CDATA)}");

                addPattern("START_1", "/>#{switchStart(0)}#{finish(TAG_RIGHT_FINISH)}");
                addPattern("START_1", ">#{switchStart(0)}#{finish(TAG_RIGHT_UNFINISH)}");
                addPattern("START_1", "${BLANK}#{finish(null)}");
                addPattern("START_1", "${STR}#{finish(ATTR_KEY)}");
                addPattern("START_1", "=#{finish(ATTR_EQ)}");
                addPattern("START_1", "${LITERAL}#{finish(ATTR_VALUE)}");


                addPattern("START", "<!${STR}#{finish(TTAG_VALUE)}");


                return buildDFA("START_0", "");
            }
        }
    }

}
