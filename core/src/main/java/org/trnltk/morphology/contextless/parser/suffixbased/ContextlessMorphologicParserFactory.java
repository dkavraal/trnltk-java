/*
 * Copyright  2013  Ali Ok (aliokATapacheDOTorg)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.trnltk.morphology.contextless.parser.suffixbased;

import com.google.common.collect.Multimap;
import org.trnltk.morphology.contextless.parser.PredefinedPaths;
import org.trnltk.morphology.contextless.parser.SuffixApplier;
import org.trnltk.morphology.contextless.rootfinder.RootFinder;
import org.trnltk.morphology.contextless.rootfinder.RootFinderChain;
import org.trnltk.morphology.contextless.rootfinder.*;
import org.trnltk.morphology.lexicon.CircumflexConvertingRootGenerator;
import org.trnltk.morphology.lexicon.DictionaryLoader;
import org.trnltk.morphology.lexicon.RootMapGenerator;
import org.trnltk.model.lexicon.Lexeme;
import org.trnltk.model.lexicon.Root;
import org.trnltk.morphology.morphotactics.*;
import org.trnltk.morphology.phonetics.PhoneticsEngine;

import java.util.Collection;
import java.util.HashSet;

/**
 * @deprecated Use {@link org.trnltk.morphology.contextless.parser.formbased.ContextlessMorphologicParserBuilder}
 */
public class ContextlessMorphologicParserFactory {

    private SuffixGraph suffixGraph;
    private RootFinderChain rootFinderChain = new RootFinderChain(new RootValidator());
    private PredefinedPaths predefinedPaths;
    private SuffixApplier suffixApplier;

    public ContextlessMorphologicParserFactory suffixGraph(SuffixGraph suffixGraph) {
        this.suffixGraph = suffixGraph;
        return this;
    }

    public ContextlessMorphologicParserFactory rootFinder(RootFinder rootFinder, RootFinderChain.RootFinderPolicy rootFinderPolicy) {
        rootFinderChain.offer(rootFinder, rootFinderPolicy);
        return this;
    }

    public ContextlessMorphologicParserFactory suffixApplier(SuffixApplier suffixApplier) {
        this.suffixApplier = suffixApplier;
        return this;
    }

    public ContextlessMorphologicParserFactory predefinedPaths(PredefinedPaths predefinedPaths) {
        this.predefinedPaths = predefinedPaths;
        return this;
    }

    public ContextlessMorphologicParser build() {
        return new ContextlessMorphologicParser(this.suffixGraph, this.predefinedPaths, this.rootFinderChain, this.suffixApplier);
    }

    public static ContextlessMorphologicParser createSimple() {
        final HashSet<Lexeme> lexemes = DictionaryLoader.loadDefaultMasterDictionary();
        final CircumflexConvertingRootGenerator rootGenerator = new CircumflexConvertingRootGenerator();
        Collection<? extends Root> roots = rootGenerator.generateAll(lexemes);
        final Multimap<String, ? extends Root> rootMap = new RootMapGenerator().generate(roots);

        final BasicSuffixGraph basicSuffixGraph = new BasicSuffixGraph();
        basicSuffixGraph.initialize();

        final SuffixFormSequenceApplier suffixFormSequenceApplier = new SuffixFormSequenceApplier();
        final SuffixApplier suffixApplier = new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier));
        final PredefinedPaths predefinedPaths = new PredefinedPaths(basicSuffixGraph, rootMap, new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier)));

        predefinedPaths.initialize();

        return new ContextlessMorphologicParserFactory()
                .suffixGraph(basicSuffixGraph)
                .rootFinder(new DictionaryRootFinder(rootMap), RootFinderChain.RootFinderPolicy.CONTINUE_ON_CHAIN)
                .predefinedPaths(predefinedPaths)
                .suffixApplier(suffixApplier)
                .build();
    }

    public static ContextlessMorphologicParser createSimpleWithRootMap(Multimap<String, ? extends Root> rootMap) {
        final BasicSuffixGraph basicSuffixGraph = new BasicSuffixGraph();
        basicSuffixGraph.initialize();

        final SuffixFormSequenceApplier suffixFormSequenceApplier = new SuffixFormSequenceApplier();
        final SuffixApplier suffixApplier = new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier));
        final PredefinedPaths predefinedPaths = new PredefinedPaths(basicSuffixGraph, rootMap, new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier)));
        predefinedPaths.initialize();

        return new ContextlessMorphologicParserFactory()
                .suffixGraph(basicSuffixGraph)
                .rootFinder(new DictionaryRootFinder(rootMap), RootFinderChain.RootFinderPolicy.CONTINUE_ON_CHAIN)
                .predefinedPaths(predefinedPaths)
                .suffixApplier(suffixApplier)
                .build();
    }

    public static ContextlessMorphologicParser createWithBigGraphForRootMap(Multimap<String, ? extends Root> rootMap) {
        final CopulaSuffixGraph copulaSuffixGraph = new CopulaSuffixGraph(new ProperNounSuffixGraph(new NumeralSuffixGraph(new BasicSuffixGraph())));
        copulaSuffixGraph.initialize();

        final SuffixFormSequenceApplier suffixFormSequenceApplier = new SuffixFormSequenceApplier();
        final SuffixApplier suffixApplier = new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier));
        final PredefinedPaths predefinedPaths = new PredefinedPaths(copulaSuffixGraph, rootMap, new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier)));
        predefinedPaths.initialize();

        final DictionaryRootFinder dictionaryRootFinder = new DictionaryRootFinder(rootMap);
        final RangeDigitsRootFinder rangeDigitsRootFinder = new RangeDigitsRootFinder();
        final OrdinalDigitsRootFinder ordinalDigitsRootFinder = new OrdinalDigitsRootFinder();
        final CardinalDigitsRootFinder cardinalDigitsRootFinder = new CardinalDigitsRootFinder();
        final ProperNounFromApostropheRootFinder properNounFromApostropheRootFinder = new ProperNounFromApostropheRootFinder();
        final ProperNounWithoutApostropheRootFinder properNounWithoutApostropheRootFinder = new ProperNounWithoutApostropheRootFinder();
        final PuncRootFinder puncRootFinder = new PuncRootFinder();

        return new ContextlessMorphologicParserFactory()
                .suffixGraph(copulaSuffixGraph)

                .rootFinder(puncRootFinder, RootFinderChain.RootFinderPolicy.STOP_CHAIN_WHEN_INPUT_IS_HANDLED)
                .rootFinder(rangeDigitsRootFinder, RootFinderChain.RootFinderPolicy.STOP_CHAIN_WHEN_INPUT_IS_HANDLED)
                .rootFinder(ordinalDigitsRootFinder, RootFinderChain.RootFinderPolicy.STOP_CHAIN_WHEN_INPUT_IS_HANDLED)
                .rootFinder(cardinalDigitsRootFinder, RootFinderChain.RootFinderPolicy.STOP_CHAIN_WHEN_INPUT_IS_HANDLED)
                .rootFinder(properNounFromApostropheRootFinder, RootFinderChain.RootFinderPolicy.STOP_CHAIN_WHEN_INPUT_IS_HANDLED)
                .rootFinder(properNounWithoutApostropheRootFinder, RootFinderChain.RootFinderPolicy.CONTINUE_ON_CHAIN)
                .rootFinder(dictionaryRootFinder, RootFinderChain.RootFinderPolicy.CONTINUE_ON_CHAIN)

                .predefinedPaths(predefinedPaths)
                .suffixApplier(suffixApplier)
                .build();
    }

    public static ContextlessMorphologicParser createWithBigGraphAndSuffixFormApplicationCachingForRootMap(Multimap<String, ? extends Root> rootMap) {
        final CopulaSuffixGraph copulaSuffixGraph = new CopulaSuffixGraph(new NumeralSuffixGraph(new BasicSuffixGraph()));
        copulaSuffixGraph.initialize();

        final SuffixFormSequenceApplier suffixFormSequenceApplier = new SuffixFormSequenceApplier();
        final PrecachingSuffixFormSequenceApplier precachingSuffixFormSequenceApplier = new PrecachingSuffixFormSequenceApplier(copulaSuffixGraph, suffixFormSequenceApplier);
        final SuffixApplier suffixApplierWithCaching = new SuffixApplier(new PhoneticsEngine(precachingSuffixFormSequenceApplier));
        final PredefinedPaths predefinedPaths = new PredefinedPaths(copulaSuffixGraph, rootMap, new SuffixApplier(new PhoneticsEngine(suffixFormSequenceApplier)));
        predefinedPaths.initialize();

        return new ContextlessMorphologicParserFactory()
                .suffixGraph(copulaSuffixGraph)
                .rootFinder(new DictionaryRootFinder(rootMap), RootFinderChain.RootFinderPolicy.CONTINUE_ON_CHAIN)
                .predefinedPaths(predefinedPaths)
                .suffixApplier(suffixApplierWithCaching)
                .build();
    }
}
