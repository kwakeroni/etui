package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Expression.*;
import org.junit.jupiter.api.Test;
import org.slf4j.simple.SimpleLogger;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleParserTest {

    static {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + SimpleParser.class.getName() + ".Token", "info");
    }

    SimpleParser parser = new SimpleParser();

    @Test
    void parsesEmptyExpression() {
        assertThat(parser.parse(""))
                .isEqualTo(new Concat(List.of()));
    }

    @Test
    void parsesLiteralExpression() {
        assertThat(parser.parse("literal"))
                .isEqualTo(new Text("literal"));
    }

    @Test
    void parsesLiteralExpressionWithWhitespace() {
        assertThat(parser.parse("literal  twice"))
                .isEqualTo(new Concat(
                        new Text("literal"),
                        new Text("  "),
                        new Text("twice")
                ));
    }

    @Test
    void parsesLiteralExpressionWithUnknownOperator() {
        assertThat(parser.parse("literal!!twice"))
                .isEqualTo(new Concat(
                        new Text("literal"),
                        new Text("!!"),
                        new Text("twice")
                ));
    }

    @Test
    void ignoresWhitespaceExpressionGroup() {
        assertThat(parser.parse("literal${ }twice"))
                .isEqualTo(new Concat(
                        new Text("literal"),
                        new Text("twice")
                ));
    }

    @Test
    void ignoresEmptyExpressionGroup() {
        assertThat(parser.parse("literal${}twice"))
                .isEqualTo(new Concat(
                        new Text("literal"),
                        new Text("twice")
                ));
    }

    @Test
    void parsesIdentifierExpression() {
        assertThat(parser.parse("${myVariable}"))
                .isEqualTo(new Identifier("myVariable"));
    }

    @Test
    void parsesIdentifierExpressionWithWhitespace() {
        assertThat(parser.parse("${  myVariable  }"))
                .isEqualTo(new Identifier("myVariable"));
    }

    @Test
    void parsesLiteralsAndExpressions() {
        assertThat(parser.parse("FirstLiteral${myVariable}SecondLiteral"))
                .isEqualTo(new Concat(
                        new Text("FirstLiteral"),
                        new Identifier("myVariable"),
                        new Text("SecondLiteral")
                ));
    }


    @Test
    void parsesOtherLiteralTokensAndExpressions() {
        assertThat(parser.parse("${myVariable}.${myVariable}"))
                .isEqualTo(new Concat(
                        new Identifier("myVariable"),
                        new Text("."),
                        new Identifier("myVariable")
                ));
    }

    @Test
    void parsesStringLiteralExpressionWithDoubleQuotes() {
        assertThat(parser.parse("${\"myString\"}"))
                .isEqualTo(new Text("myString"));
    }

    @Test
    void parsesStringLiteralExpressionWithSingleQuotes() {
        assertThat(parser.parse("${'myString'}"))
                .isEqualTo(new Text("myString"));
    }

    @Test
    void parsesStringLiteralExpressionWithEmbeddedExpression() {
        assertThat(parser.parse("${\"my${identified}String\"}"))
                .isEqualTo(new Concat(
                        new Text("my"),
                        new Identifier("identified"),
                        new Text("String")
                ));
    }

    @Test
    void parsesStringLiteralExpressionWithEmbeddedElvisExpression() {
        assertThat(parser.parse("${\"${left}${right}\"?:\"unidentified\"}"))
                .isEqualTo(new Elvis(
                        new Concat(new Identifier("left"), new Identifier("right")),
                        new Text("unidentified")
                ));
    }

    @Test
    void parsesElvisExpression() {
        assertThat(parser.parse("${myVariable?:myDefault}"))
                .isEqualTo(new Elvis(new Identifier("myVariable"), new Identifier("myDefault")));
    }


    @Test
    void parsesElvisExpressionWithWhitespace() {
        assertThat(parser.parse("${myVariable ?: myDefault}"))
                .isEqualTo(new Elvis(new Identifier("myVariable"), new Identifier("myDefault")));
    }

    @Test
    void parsesElvisExpressionWithStringFallback() {
        assertThat(parser.parse("${myVariable?:\"myDefaultString\"}"))
                .isEqualTo(new Elvis(new Identifier("myVariable"), new Text("myDefaultString")));
    }

    @Test
    void parsesDoubleElvisExpression() {
        assertThat(parser.parse("${myVariable?:myDefault?:myOtherDefault}"))
                .isEqualTo(new Elvis(new Identifier("myVariable"),
                                     new Elvis(new Identifier("myDefault"), new Identifier("myOtherDefault"))));
    }

    @Test
    void parsesOptionalSuffixExpression() {
        assertThat(parser.parse("${myVariable?+mySuffix}"))
                .isEqualTo(new OptSuffix(new Identifier("myVariable"), new Identifier("mySuffix")));
    }

    @Test
    void parsesOptionalPrefixExpression() {
        assertThat(parser.parse("${myPrefix+?myVariable}"))
                .isEqualTo(new OptPrefix(new Identifier("myPrefix"), new Identifier("myVariable")));
    }

    @Test
    void parsesOptionalPrefixAndSuffixExpression() {
        assertThat(parser.parse("${myPrefix+?myVariable?+mySuffix}"))
                .isEqualTo(new OptPrefix(
                        new Identifier("myPrefix"),
                        new OptSuffix(new Identifier("myVariable"),
                                      new Identifier("mySuffix"))));
    }

    @Test
    void parseStringWithDot() {
        assertThat(parser.parse("${\".\"}"))
                .isEqualTo(new Text("."));
    }

    @Test
    void parseComplexExpression() {
        assertThat(parser.parse("${author?+'.'}${' '+?title?+'.'}${' jg. '+?volume}${', nr. '+?number}${', '+?coverDisplayDate}${', p. '+?pageRange}"))
                .isEqualTo(new Concat(
                        new OptSuffix(new Identifier("author"), new Text(".")),
                        new OptPrefix(new Text(" "), new OptSuffix(new Identifier("title"), new Text("."))),
                        new OptPrefix(new Concat(new Text(" "),new Text("jg"),new Text("."),new Text(" ")), new Identifier("volume")),
                        new OptPrefix(new Concat(new Text(","),new Text(" "),new Text("nr"),new Text("."),new Text(" ")), new Identifier("number")),
                        new OptPrefix(new Concat(new Text(","),new Text(" ")), new Identifier("coverDisplayDate")),
                        new OptPrefix(new Concat(new Text(","),new Text(" "),new Text("p"),new Text("."),new Text(" ")), new Identifier("pageRange"))
                ));
    }

    @Test
    void rootExpressionCannotBeGroupWithParenthesis() {
        assertThat(parser.parse("(myVariable)"))
                .isEqualTo(new Concat(
                        new Text("("),
                        new Text("myVariable"),
                        new Text(")")
                ));
    }

    @Test
    void parsesGroupWithParenthesis() {
        assertThat(parser.parse("${(myVariable)}"))
                .isEqualTo(new Identifier("myVariable"));
    }

    @Test
    void parsesExpressionStartingWithParenthesis() {
        assertThat(parser.parse("${(publicationDate?:copyrightYear)?+'.'}"))
                .isEqualTo(new OptSuffix(new Elvis(new Identifier("publicationDate"), new Identifier("copyrightYear")), new Text(".")));
    }
}
