package org.trnltk.tokenizer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.trnltk.tokenizer.*;
import org.trnltk.tokenizer.data.TokenizerTrainingData;
import org.trnltk.tokenizer.data.TokenizerTrainingEntry;

import java.io.FileNotFoundException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TextTokenizerDefaultTraining {

    @Before
    public void setUp() throws Exception {
        // set the appender every time!
        final Enumeration currentLoggers = Logger.getLogger("org.trnltk").getLoggerRepository().getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            final Logger logger = (Logger) currentLoggers.nextElement();
            logger.setLevel(Level.WARN);
        }
    }

    // useful while running tests individually
    protected void turnTokenizerLoggingOn() {
        Logger.getLogger(TextTokenizer.class).setLevel(Level.DEBUG);
    }

    // useful while running tests individually
    protected void turnTrainerLoggingOn() {
        // set the appender every time!
        Logger.getLogger(TextTokenizerTrainer.class).setLevel(Level.DEBUG);
        Logger.getLogger(TokenizationGraph.class).setLevel(Level.DEBUG);
        Logger.getLogger(TokenizationGraphNode.class).setLevel(Level.DEBUG);
    }

    @Test
    public void shouldValidateDefaultRuleEntries() throws FileNotFoundException {
        final TokenizationGraph tokenizationGraph = TextTokenizerTrainer.buildDefaultTokenizationGraph(true);

        final TextTokenizer tokenizer = TextTokenizer.newBuilder()
                .blockSize(2)
                .recordStats()
                .strict()
                .graph(tokenizationGraph).build();


        final TokenizerTrainingData defaultTrainingData = TokenizerTrainingData.createDefaultTrainingData();
        for (TokenizerTrainingEntry tokenizerTrainingEntry : defaultTrainingData.getEntries()) {
            final String text = tokenizerTrainingEntry.getText();
            final String tknz = tokenizerTrainingEntry.getTknz();

            final List<String> tokens = tokenizer.tokenize(text);
            final String join = Joiner.on(" ").join(tokens);
            assertThat(tknz.trim(), equalTo(join.trim()));
        }

        final TextTokenizer.TextTokenizerStats stats = tokenizer.getStats();
        final LinkedHashMap<Pair<TextBlockTypeGroup, TextBlockTypeGroup>, Set<MissingTokenizationRuleException>> failMap = stats.buildSortedFailMap();

        assertThat(failMap.isEmpty(), equalTo(true));
    }

    @Ignore
    @Test
    public void shouldDumpBigTokenizationGraphInDotFormat() {
        final TextTokenizer tokenizer = TextTokenizer.createDefaultTextTokenizer(true);
        dumpTokenizationGraph(tokenizer.graph, Predicates.<TokenizationGraphNode>alwaysTrue(),
                Predicates.<TokenizationGraphNode>alwaysTrue(), Predicates.<TokenizationGraphEdge>alwaysTrue());
    }

    @Ignore
    @Test
    public void shouldDumpSomePortionOfBigTokenizationGraphInDotFormat() {
        final TextTokenizer tokenizer = TextTokenizer.createDefaultTextTokenizer(true);
        final Predicate<TokenizationGraphNode> Type_Word_Matcher = new Predicate<TokenizationGraphNode>() {
            @Override
            public boolean apply(TokenizationGraphNode input) {
                final ImmutableList<TextBlockType> textBlockTypes = input.getData().getTextBlockTypes();
                if (textBlockTypes.iterator().next().equals(TextBlockType.Word)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        dumpTokenizationGraph(tokenizer.graph, Type_Word_Matcher, Type_Word_Matcher, Predicates.<TokenizationGraphEdge>alwaysTrue());
    }

    @Ignore
    @Test
    public void shouldDumpSmallTokenizationGraphInDotFormat() {
        final TextTokenizerTrainer localTokenizer = new TextTokenizerTrainer(2, true);
        localTokenizer.train("ali veli.", "ali veli .");

        dumpTokenizationGraph(localTokenizer.build(), Predicates.<TokenizationGraphNode>alwaysTrue(),
                Predicates.<TokenizationGraphNode>alwaysTrue(), Predicates.<TokenizationGraphEdge>alwaysTrue());
    }

    @Ignore
    @Test
    public void shouldDumpSmallTokenizationGraphInDotFormatWithoutInference() {
        final TextTokenizerTrainer localTokenizer = new TextTokenizerTrainer(2, true);
        localTokenizer.train("ali geldi.", "ali geldi .");

        Predicate<TokenizationGraphEdge> directEdgePredicate = new Predicate<TokenizationGraphEdge>() {
            @Override
            public boolean apply(TokenizationGraphEdge input) {
                return !input.isInferred();
            }
        };
        dumpTokenizationGraph(localTokenizer.build(), Predicates.<TokenizationGraphNode>alwaysTrue(), Predicates.<TokenizationGraphNode>alwaysTrue(), directEdgePredicate);
    }

    @Ignore
    @Test
    public void shouldDumpSmallTokenizationGraph_onlyForWordsAndDot_InDotFormat() {
        final TextTokenizerTrainer localTokenizer = new TextTokenizerTrainer(2, true);
        localTokenizer.train("ali veli.", "ali veli .");

        dumpTokenizationGraph(localTokenizer.build(),
                new Predicate<TokenizationGraphNode>() {
                    @Override
                    public boolean apply(TokenizationGraphNode input) {
                        final ImmutableList<TextBlockType> types = input.getData().getTextBlockTypes();
                        if (types.contains(TextBlockType.Abbreviation) || types.get(1).equals(TextBlockType.Sentence_Start)
                                )
                            return false;
                        return true;
                    }
                },

                new Predicate<TokenizationGraphNode>() {
                    @Override
                    public boolean apply(TokenizationGraphNode input) {
                        final ImmutableList<TextBlockType> types = input.getData().getTextBlockTypes();
                        if (types.contains(TextBlockType.Abbreviation) || types.get(0).equals(TextBlockType.Sentence_End)
                                || types.get(1).equals(TextBlockType.Dot)
                                )
                            return false;
                        return true;
                    }
                },

                Predicates.<TokenizationGraphEdge>alwaysTrue()
        );
    }

    //see http://en.wikipedia.org/wiki/DOT_language
    private void dumpTokenizationGraph(TokenizationGraph graph, Predicate<TokenizationGraphNode> sourceNodePredicate,
                                       Predicate<TokenizationGraphNode> targetNodePredicate, Predicate<TokenizationGraphEdge> edgePredicate) {
        final Map<TextBlockTypeGroup, TokenizationGraphNode> nodeMap = graph.nodeMap;

        int instructedEdgeCount = 0;
        int inferredEdgeCount = 0;

        int addSpaceRuleCount = 0;
        int dontAddSpaceRuleCount = 0;

        System.out.println("digraph tokenizationGraph {");

        for (Map.Entry<TextBlockTypeGroup, TokenizationGraphNode> nodeEntry : nodeMap.entrySet()) {
            //all nodes are available on the map, so we don't need graph traversal
            final TokenizationGraphNode sourceNode = nodeEntry.getValue();

            if (!sourceNodePredicate.apply(sourceNode))
                continue;

            final String sourceNodeName = getNodeName(sourceNode);
            System.out.println("\t" + sourceNodeName);

            final Map<TextBlockTypeGroup, TokenizationGraphEdge> edges = sourceNode.edges;
            for (TokenizationGraphEdge tokenizationGraphEdge : edges.values()) {
                final TokenizationGraphNode targetNode = tokenizationGraphEdge.getTarget();
                if (!targetNodePredicate.apply(targetNode))
                    continue;

                if (!edgePredicate.apply(tokenizationGraphEdge))
                    continue;

                String style = tokenizationGraphEdge.isInferred() ? "dashed" : "solid";
                String color = tokenizationGraphEdge.isAddSpace() ? "blue" : "red";
                final String targetNodeName = getNodeName(targetNode);
                System.out.println("\t" + sourceNodeName + " -> " + targetNodeName + "[style=" + style + " color=" + color + "]");

                if (tokenizationGraphEdge.isInferred())
                    inferredEdgeCount++;
                else
                    instructedEdgeCount++;

                if (tokenizationGraphEdge.isAddSpace())
                    addSpaceRuleCount++;
                else
                    dontAddSpaceRuleCount++;
            }
        }

        System.out.println("// Number of nodes : " + nodeMap.size());
        System.out.println("// Number of edges: " + (inferredEdgeCount + instructedEdgeCount));
        System.out.println("// Instructed : " + instructedEdgeCount + ", inferred " + inferredEdgeCount);
        System.out.println("// Add space : " + addSpaceRuleCount + ", don't add space " + dontAddSpaceRuleCount);
        System.out.println();

        System.out.println("// Full graph node cound would be " + (TextBlockType.values().length * TextBlockType.values().length));
        System.out.println("// and in that case complete graph node count would be " +
                (TextBlockType.values().length * TextBlockType.values().length) * (TextBlockType.values().length * TextBlockType.values().length));

        System.out.println("}");
    }

    private String getNodeName(TokenizationGraphNode node) {
        final String str = Joiner.on('+').join(Lists.transform(node.getData().getTextBlockTypes(), new Function<TextBlockType, String>() {
            @Override
            public String apply(TextBlockType input) {
                return input.name();
            }
        }));
        return "\"" + str + "\"";
    }
}