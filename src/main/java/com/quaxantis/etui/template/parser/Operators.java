package com.quaxantis.etui.template.parser;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.teeing;

final class Operators {
    static Map<String, Object> BY_DELIMITER = Stream.<Supplier<Stream<? extends Map.Entry<String, ?>>>>
                    of(InfixOp::operators, GroupOp::operators)
            .flatMap(Supplier::get)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    private Operators() {
        throw new UnsupportedOperationException();
    }

    enum InfixOp {
        ELVIS("?:", Expression.Elvis::new),
        OPT_SUFFIX("?+", Expression.OptSuffix::new),
        OPT_PREFIX("+?", Expression.OptPrefix::new),
        ;

        private final String operator;
        private final BinaryOperator<Expression> combiner;

        InfixOp(String operator, BinaryOperator<Expression> combiner) {
            this.operator = operator;
            this.combiner = combiner;
        }

        Expression combine(Expression left, Expression right) {
            return this.combiner.apply(left, right);
        }

        private static final Map<String, InfixOp> OPERATORS = Arrays.stream(InfixOp.values())
                .collect(Collectors.toUnmodifiableMap(io -> io.operator, io -> io));

        static Stream<Map.Entry<String, InfixOp>> operators() {
            return OPERATORS.entrySet().stream();
        }
    }

    enum GroupOp {
        // ${} is not extensible with an infix as it is only used in a Text which does not support any operators but the expression group.
        EXPRESSION("${", "}", false),
        PARENTHESES("(", ")", true),
        STRING_DOUBLE("\"", true),
        STRING_SINGLE("'", true),
        ;

        private final Start start;
        private final End end;
        private final String startDelimiter;
        private final String endDelimiter;
        private final boolean extensible;

        GroupOp(String startAndEndDelimiter, boolean extensible) {
            this.startDelimiter = startAndEndDelimiter;
            this.endDelimiter = startAndEndDelimiter;
            StartAndEnd startAndEndOperator = StartAndEnd.of(this);
            this.start = startAndEndOperator;
            this.end = startAndEndOperator;
            this.extensible = extensible;
        }

        GroupOp(String startDelimiter, String endDelimiter, boolean extensible) {
            this.startDelimiter = startDelimiter;
            this.endDelimiter = endDelimiter;
            this.start = Start.of(this);
            this.end = End.of(this);
            this.extensible = extensible;
        }

        public String startDelimiter() {
            return this.startDelimiter;
        }

        public Start startOp() {
            return this.start;
        }

        public String endDelimiter() {
            return this.endDelimiter;
        }

        public End endOp() {
            return this.end;
        }

        public boolean isExtensible() {
            return this.extensible;
        }

        @Override
        public String toString() {
            return this.startDelimiter + name() + this.endDelimiter;
        }

        private static Stream<Map.Entry<String, ?>> operators() {
            return Arrays.stream(GroupOp.values())
                    .collect(teeing(
                            mapping((GroupOp op) -> Map.entry(op.startDelimiter(), op.startOp()), Collectors.toList()),
                            mapping((GroupOp op) -> Map.entry(op.endDelimiter(), op.endOp()), Collectors.toList()),
                            (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).distinct()
                    ));
        }


        interface Start {
            GroupOp group();

            default boolean matches(GroupOp group) {
                return group().equals(group);
            }

            static Start of(GroupOp group) {
                return new Start() {
                    @Override
                    public GroupOp group() {
                        return group;
                    }

                    @Override
                    public String toString() {
                        return group.startDelimiter();
                    }
                };
            }
        }

        interface End {
            GroupOp group();

            default boolean matches(Start start) {
                return group().startOp().equals(start);
            }

            static End of(GroupOp group) {
                return new End() {
                    @Override
                    public GroupOp group() {
                        return group;
                    }

                    @Override
                    public String toString() {
                        return group.endDelimiter();
                    }
                };
            }
        }

        interface StartAndEnd extends Start, End {
            static StartAndEnd of(GroupOp group) {
                return new StartAndEnd() {
                    @Override
                    public GroupOp group() {
                        return group;
                    }

                    @Override
                    public String toString() {
                        return group.startDelimiter();
                    }
                };
            }
        }
    }

}
