package com.quaxantis.etui.template.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;

class Tokenizer {

    private final Map<String, Object> operators;

    Tokenizer(Map<String, Object> operators) {
        this.operators = operators;
    }

    public Iterator<Token> tokenize(String string) {
        var list = new ArrayList<Token>();
        CharType currentCharType = null;
        int pos = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            CharType charType = CharType.of(c);
            if (charType != currentCharType) {
                if (currentCharType != null) {
                    list.add(createToken(currentCharType, string.substring(pos, i)));
                    pos = i;
                }
                currentCharType = charType;
            } else if (currentCharType == CharType.OTHER) {
                // If we start with an operator
                if (!operators.containsKey(string.substring(pos, i + 1))
                    && operators.containsKey(string.substring(pos, i))) {
                    String token = string.substring(pos, i);
                    list.add(new Token.Operator(operators.get(token), token));
                    pos = i;
                } else {
                    // If we end with an operator
                    int opl = (i > 1 && operators.containsKey(string.substring(i-2, i))) ? 2
                                : (operators.containsKey(string.substring(i-1, i))) ? 1
                            : 0;
                    if (opl > 0) {
                        String other = string.substring(pos, i-opl);
                        list.add(new Token.Other(other));
                        String op = string.substring(i-opl, i);
                        list.add(new Token.Operator(operators.get(op), op));
                        pos = i;
                    } else if (pos < i-1 && operators.containsKey(string.substring(i-1, i+1))) {
                        String other = string.substring(pos, i-1);
                        list.add(new Token.Other(other));
                        pos = i-1;
                    }
                }
            }
        }
        if (pos < string.length()) {
            list.add(createToken(currentCharType, string.substring(pos)));
        }
        return list.iterator();
    }

    private Token createToken(CharType charType, String tokenString) {
        if (charType == CharType.OTHER && operators.containsKey(tokenString)) {
            return new Token.Operator(operators.get(tokenString), tokenString);
        } else {
            return charType.asToken(tokenString);
        }
    }

    private enum CharType {
        ALPHANUMERIC(Character::isLetterOrDigit, Token.AlphaNumeric::new),
        WHITESPACE(Character::isWhitespace, Token.Whitespace::new),
        OTHER(ignored -> true, Token.Other::new);

        private final IntPredicate predicate;
        private final Function<String, Token> tokenCreator;

        CharType(IntPredicate predicate, Function<String, Token> tokenCreator) {
            this.predicate = predicate;
            this.tokenCreator = tokenCreator;
        }

        Token asToken(String token) {
            return tokenCreator.apply(token);
        }

        public static CharType of(char c) {
            for (CharType charType : CharType.values()) {
                if (charType.predicate.test(c)) {
                    return charType;
                }
            }
            return OTHER;
        }
    }

    public static void main(String[] args) {
        System.out.println(Character.isLetterOrDigit('\''));
        System.out.println(Character.isLetterOrDigit('\"'));
    }
}
