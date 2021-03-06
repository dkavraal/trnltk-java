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

package org.trnltk.morphology.contextless.rootfinder;

import org.trnltk.model.lexicon.DynamicRoot;
import org.trnltk.model.lexicon.LexemeAttribute;
import org.trnltk.model.letter.TurkishSequence;
import org.trnltk.model.letter.TurkishAlphabet;
import org.trnltk.model.letter.TurkishChar;
import org.trnltk.model.letter.TurkicLetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Finds roots that seem like a compoundP3sg noun.
 * <p/>
 * A compoundP3sg noun is for example 'acemborusu' or 'keçiboynuzu', but not 'akarsu'.
 * <p/>
 * This class is basically trying to explain why a noun would get Accusative form -nI
 * when the root is not in the dictionary.
 * <p/>
 * These are supported:
 * <p/>
 * bacakkalemi, adamotu, aslankuyruğu, dünyahali, yaşhaddi etc.
 * <p/>
 * These are not supported:
 * <p/>
 * soboruları
 * soborum
 * çeşmesuyunu
 * ademoğlunu
 * <br/>
 * >>> rf = BruteForceCompoundNounRootFinder()
 * <br/>
 * >>> rf.findRootsForPartialInput(u'suborusu', u'suborusuna')
 * <br/>
 * Root : 'suboru', Lexeme:'soburusu'
 */
public class BruteForceCompoundNounRootFinder implements RootFinder {
    private final BruteForceNounRootFinder bruteForceNounRootFinder = new BruteForceNounRootFinder();

    @Override
    public boolean handles(TurkishSequence partialInput, TurkishSequence wholeSurface) {
        if (partialInput == null || partialInput.isBlank())
            return false;

        if (wholeSurface == null || wholeSurface.isBlank())
            return false;

        if (wholeSurface.length() == partialInput.length())
            return false;

        // no compound should be found an input shorter than sth like "atsu-yu". even that doesn't make sense
        if (partialInput.length() < 5)
            return false;

        final TurkishChar lastChar = partialInput.charAt(partialInput.length() - 1);
        final TurkishChar previousChar = partialInput.charAt(partialInput.length() - 2);
        if (Character.isUpperCase(lastChar.getCharValue()) || Character.isUpperCase(previousChar.getCharValue()))
            return false;

        final TurkicLetter lastLetter = lastChar.getLetter();

        if (!Arrays.asList(TurkishAlphabet.L_i, TurkishAlphabet.L_u, TurkishAlphabet.L_ii, TurkishAlphabet.L_uu).contains(lastLetter))
            return false;


        final TurkishChar firstCharAfterPartialInput = wholeSurface.charAt(partialInput.length());
        final TurkicLetter firstLetterAfterPartialInput = firstCharAfterPartialInput.getLetter();

        if (Character.isUpperCase(firstCharAfterPartialInput.getCharValue()))
            return false;

        if (!firstLetterAfterPartialInput.equals(TurkishAlphabet.L_n))
            return false;

        if (wholeSurface.length() < partialInput.length() + 2)    //need a char after char 'n'
            return false;

        return true;
    }

    @Override
    public Collection<DynamicRoot> findRootsForPartialInput(TurkishSequence partialInput, TurkishSequence wholeSurface) {

        final TurkishChar previousChar = partialInput.charAt(partialInput.length() - 2);

        final List<DynamicRoot> compoundResults = new ArrayList<DynamicRoot>();

        final Collection<DynamicRoot> resultsWithPartialInputOneCharMissing =
                this.bruteForceNounRootFinder.findRootsForPartialInput(partialInput.subsequence(0, partialInput.length() - 1), wholeSurface);


        // illustrate:
        // partial_input = suborusu, whole_surface = suborusuna
        // results_with_partial_input_one_char_missing : <'suborus','suborus'>
        // partial_input = bacakkalemi, whole_surface = bacakkalemini
        // results_with_partial_input_one_char_missing : <'bacakkalem','bacakkalem'>
        // partial_input = atkuyrugu, whole_surface=atkuyruguna
        // results_with_partial_input_one_char_missing : <'atkuyruk', 'atkuyrug', 'atkuyrugh'>

        // these are all roots of noun compound. so the lexeme can be derived from any of
        // atkuyruk+u, atkuyrug+u, atkuyrugh+u

        for (DynamicRoot normalNounResult : resultsWithPartialInputOneCharMissing) {
            final DynamicRoot cloneResult = new DynamicRoot(normalNounResult);
            cloneResult.setSequence(new TurkishSequence(cloneResult.getLexeme().getLemmaRoot()));
            cloneResult.getLexeme().setLemmaRoot(partialInput.getUnderlyingString());
            cloneResult.getLexeme().setLemma(partialInput.getUnderlyingString());

            compoundResults.add(cloneResult);
        }

        final TurkicLetter previousLetter = previousChar.getLetter();

        if (previousLetter.equals(TurkishAlphabet.L_s)) {
            final Collection<DynamicRoot> resultsWithPartialInputTwoCharMissing =
                    this.bruteForceNounRootFinder.findRootsForPartialInput(partialInput.subsequence(0, partialInput.length() - 2), wholeSurface);

            // illustrate:
            // partial_input = suborusu, whole_surface = suborusuna
            // results_with_partial_input_two_chars_missing : <'suboru','suboru'>

            for (DynamicRoot normalNounResult : resultsWithPartialInputTwoCharMissing) {
                final DynamicRoot cloneResult = new DynamicRoot(normalNounResult);
                cloneResult.getLexeme().setLemmaRoot(partialInput.getUnderlyingString());
                cloneResult.getLexeme().setLemma(partialInput.getUnderlyingString());

                compoundResults.add(cloneResult);
            }
        }

        for (DynamicRoot compoundResult : compoundResults) {
            compoundResult.getLexeme().getAttributes().add(LexemeAttribute.CompoundP3sg);
        }

        return compoundResults;
    }

}
