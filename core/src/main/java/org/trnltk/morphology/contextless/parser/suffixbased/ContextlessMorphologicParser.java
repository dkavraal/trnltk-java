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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.trnltk.model.letter.TurkishSequence;
import org.trnltk.model.lexicon.Root;
import org.trnltk.model.morpheme.MorphemeContainer;
import org.trnltk.model.suffix.Suffix;
import org.trnltk.model.suffix.SuffixGroup;
import org.trnltk.morphology.contextless.parser.MandatoryTransitionApplier;
import org.trnltk.morphology.contextless.parser.MorphologicParser;
import org.trnltk.morphology.contextless.parser.PredefinedPaths;
import org.trnltk.morphology.contextless.parser.SuffixApplier;
import org.trnltk.morphology.contextless.rootfinder.RootFinderChain;
import org.trnltk.morphology.morphotactics.SuffixEdge;
import org.trnltk.morphology.morphotactics.SuffixGraph;
import org.trnltk.morphology.morphotactics.SuffixGraphState;
import org.trnltk.morphology.morphotactics.SuffixGraphStateType;
import org.trnltk.util.MorphemeContainerFormatter;

import java.util.*;

/**
 * @deprecated Use {@link org.trnltk.morphology.contextless.parser.formbased.ContextlessMorphologicParser}
 */
public class ContextlessMorphologicParser implements MorphologicParser {
    protected final Logger logger = Logger.getLogger(ContextlessMorphologicParser.class);
    private final MandatoryTransitionApplier mandatoryTransitionApplier;

    private SuffixGraph suffixGraph;
    private PredefinedPaths predefinedPaths;
    private RootFinderChain rootFinderChain;
    private SuffixApplier suffixApplier;

    public ContextlessMorphologicParser(final SuffixGraph suffixGraph, final PredefinedPaths predefinedPaths, final RootFinderChain rootFinderChain, final SuffixApplier suffixApplier) {
        this.suffixGraph = suffixGraph;
        this.predefinedPaths = predefinedPaths;
        this.rootFinderChain = rootFinderChain;
        this.suffixApplier = suffixApplier;
        this.mandatoryTransitionApplier = new MandatoryTransitionApplier(suffixGraph, suffixApplier);
    }

    @Override
    public List<List<MorphemeContainer>> parseAll(List<TurkishSequence> inputs) {
        return new ArrayList<List<MorphemeContainer>>(Lists.transform(inputs, new Function<TurkishSequence, List<MorphemeContainer>>() {
            @Override
            public List<MorphemeContainer> apply(TurkishSequence input) {
                return parse(input);
            }
        }));
    }

    @Override
    public List<List<MorphemeContainer>> parseAllStr(List<String> inputs) {
        return new ArrayList<List<MorphemeContainer>>(Lists.transform(inputs, new Function<String, List<MorphemeContainer>>() {
            @Override
            public List<MorphemeContainer> apply(String input) {
                return parseStr(input);
            }
        }));
    }

    @Override
    public List<MorphemeContainer> parseStr(String input) {
        return this.parse(new TurkishSequence(input));
    }

    @Override
    public LinkedList<MorphemeContainer> parse(final TurkishSequence input) {
        if (logger.isDebugEnabled())
            logger.debug("Parsing input " + input);

        final List<MorphemeContainer> candidateMorphemeContainers = this.findInitialMorphemeContainers(input);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Found %d candidate morpheme containers", candidateMorphemeContainers.size()));
            for (MorphemeContainer morphemeContainer : candidateMorphemeContainers) {
                logger.debug("\t " + morphemeContainer.toString());
            }
        }

        logger.debug("Applying mandatory transitions to candidates");

        final List<MorphemeContainer> candidateMorphemeContainersWithMandatoryTransitions = mandatoryTransitionApplier.applyMandatoryTransitionsToMorphemeContainers(candidateMorphemeContainers, input);

        final LinkedList<MorphemeContainer> results = new LinkedList<MorphemeContainer>();
        final LinkedList<MorphemeContainer> newCandidates = this.traverseCandidates(candidateMorphemeContainersWithMandatoryTransitions, results, input);

        if (CollectionUtils.isNotEmpty(newCandidates))
            throw new IllegalStateException("There are still parse morpheme containers to traverse, but traversing is finished : " + newCandidates.toString());

        return results;
    }

    private LinkedList<MorphemeContainer> traverseCandidates(final List<MorphemeContainer> candidates, final List<MorphemeContainer> results, final TurkishSequence input) {
        if (logger.isDebugEnabled()) {
            logger.debug("Gonna traverse " + candidates.size() + " candidates:");
            for (MorphemeContainer candidate : candidates) {
                logger.debug("\t " + candidate);
            }
        }

        LinkedList<MorphemeContainer> newCandidates = new LinkedList<MorphemeContainer>();
        for (MorphemeContainer candidateMorphemeContainer : candidates) {
            if (logger.isDebugEnabled())
                logger.debug(" Traversing candidate: %s" + candidateMorphemeContainer);
            final List<MorphemeContainer> morphemeContainersForCandidate = this.traverseCandidate(candidateMorphemeContainer, input);
            for (MorphemeContainer morphemeContainerForCandidate : morphemeContainersForCandidate) {
                if (SuffixGraphStateType.TERMINAL.equals(morphemeContainerForCandidate.getLastState().getType())) {
                    if (StringUtils.isBlank(morphemeContainerForCandidate.getRemainingSurface())) {
                        results.add(morphemeContainerForCandidate);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found a terminal result --------------------->");
                            logger.debug(morphemeContainerForCandidate);
                            logger.debug(MorphemeContainerFormatter.formatMorphemeContainerWithForms(morphemeContainerForCandidate));
                        }
                    } else {
                        if (logger.isDebugEnabled())
                            logger.debug("Found a terminal result, but there is still remaining to parse : " + morphemeContainerForCandidate);
                    }
                } else {
                    newCandidates.add(morphemeContainerForCandidate);
                }
            }
        }

        // call recursively until nothing to traverse!
        if (CollectionUtils.isNotEmpty(newCandidates))
            newCandidates = this.traverseCandidates(newCandidates, results, input);

        return newCandidates;
    }

    private LinkedList<MorphemeContainer> traverseCandidate(final MorphemeContainer morphemeContainer, final TurkishSequence input) {
        if (SuffixGraphStateType.TERMINAL.equals(morphemeContainer.getLastState().getType()))
            return Lists.newLinkedList(Arrays.asList(morphemeContainer));

        final LinkedList<MorphemeContainer> newCandidates = new LinkedList<MorphemeContainer>();

        final SuffixGraphState fromState = morphemeContainer.getLastState();
        final Set<SuffixEdge> stateApplicableSuffixEdges = this.getApplicableSuffixesOfStateForMorphemeContainer(fromState, morphemeContainer);

        if (logger.isDebugEnabled())
            logger.debug(String.format("  Found applicable suffixes for morpheme_container from state %s: %s", fromState, stateApplicableSuffixEdges));

        for (SuffixEdge suffixEdge : stateApplicableSuffixEdges) {
            final Suffix suffix = suffixEdge.getSuffix();
            final SuffixGraphState targetState = suffixEdge.getTargetState();
            if (logger.isDebugEnabled())
                logger.debug(String.format("   Going to try suffix %s to state %s", suffix, targetState));

            final List<MorphemeContainer> morphemeContainersForSuffix = this.suffixApplier.trySuffix(morphemeContainer, suffix, targetState, input);
            if (CollectionUtils.isNotEmpty(morphemeContainersForSuffix))
                newCandidates.addAll(morphemeContainersForSuffix);
        }

        return newCandidates;
    }

    private Set<SuffixEdge> getApplicableSuffixesOfStateForMorphemeContainer(final SuffixGraphState fromState, final MorphemeContainer morphemeContainer) {
        if (logger.isDebugEnabled()) {
            logger.debug("  Finding applicable suffixes for morpheme_container from state " + fromState + " : " + morphemeContainer);
            logger.debug("   Found outputs " + fromState.getOutEdges());
        }


        Set<SuffixEdge> outEdges = fromState.getOutEdges();

        // filter out suffixes which are already added since last derivation
        outEdges = Sets.filter(outEdges, new com.google.common.base.Predicate<SuffixEdge>() {
            @Override
            public boolean apply(SuffixEdge input) {
                return !morphemeContainer.getSuffixesSinceDerivationSuffix().contains(input.getSuffix());
            }
        });
        if (logger.isDebugEnabled())
            logger.debug("   Filtered out the applied suffixes since last derivation " + morphemeContainer.getSuffixesSinceDerivationSuffix() + " : " + outEdges);

        //TODO: following seems unnecessary!
        // filter out suffixes if one of the suffixes of whose group is already added since last derivation
        outEdges = Sets.filter(outEdges, new Predicate<SuffixEdge>() {
            @Override
            public boolean apply(SuffixEdge input) {
                SuffixGroup suffixGroup = input.getSuffix().getSuffixGroup();
                if (suffixGroup == null)
                    return true;
                else
                    return !morphemeContainer.getSuffixGroupsSinceLastDerivationSuffix().contains(suffixGroup);
            }
        });
        if (logger.isDebugEnabled())
            logger.debug("   Filtered out the suffixes that has one applied in their groups: " + outEdges);

        return outEdges;
    }

    private LinkedList<MorphemeContainer> findInitialMorphemeContainers(final TurkishSequence input) {
        final LinkedList<MorphemeContainer> candidates = new LinkedList<MorphemeContainer>();

        for (int i = 1; i < input.length() + 1; i++) {
            final TurkishSequence partialInput = input.subsequence(0, i);

            final List<Root> roots = this.rootFinderChain.findRootsForPartialInput(partialInput, input);


            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Found %d root candidates for partial input '%s':", roots.size(), partialInput));
                for (Root root : roots) {
                    logger.debug("\t " + root.toString());
                }
            }

            if (this.predefinedPaths == null) {
                for (Root root : roots) {
                    final String remainingInput = input.substring(root.getSequence().length());
                    final SuffixGraphState defaultStateForRoot = this.suffixGraph.getDefaultStateForRoot(root);
                    if (defaultStateForRoot == null)
                        throw new IllegalStateException("No default state found for root " + root);

                    final MorphemeContainer morphemeContainer = new MorphemeContainer(root, defaultStateForRoot, remainingInput);
                    candidates.add(morphemeContainer);
                }
            } else {
                for (Root root : roots) {
                    final String remainingInput = input.substring(root.getSequence().length());

                    final SuffixGraphState defaultStateForRoot = this.suffixGraph.getDefaultStateForRoot(root);
                    if (defaultStateForRoot == null)
                        throw new IllegalStateException("No default state found for root " + root);

                    if (this.predefinedPaths.hasPathsForRoot(root)) {
                        final Set<MorphemeContainer> predefinedMorphemeContainers = this.predefinedPaths.getPaths(root);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found predefined morpheme containers for root candidate " + root + " : " + predefinedMorphemeContainers);
                        }
                        for (MorphemeContainer predefinedMorphemeContainer : predefinedMorphemeContainers) {
                            if (input.startsWith(predefinedMorphemeContainer.getSurfaceSoFar())) {
                                if (logger.isDebugEnabled())
                                    logger.debug("Predefined morpheme_container is applicable " + predefinedMorphemeContainer);
                                //entry is cloned and since the remaining surface can be different, it is set.
                                MorphemeContainer clone = new MorphemeContainer(predefinedMorphemeContainer, input);
                                candidates.add(clone);
                            } else {
                                if (logger.isDebugEnabled())
                                    logger.debug("Predefined morpheme container is not applicable, skipping " + predefinedMorphemeContainer);
                            }
                        }
                    } else {
                        candidates.add(new MorphemeContainer(root, defaultStateForRoot, remainingInput));
                    }
                }
            }
        }

        return candidates;
    }
}
