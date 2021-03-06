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

package org.trnltk.model.suffix;

import org.trnltk.common.specification.Specification;
import org.trnltk.model.morpheme.MorphemeContainer;

/**
 * A form of a {@link Suffix} which is applied to a surface.
 * <p/>
 * For example, <code>SuffixForm</code>s for the suffix <i>Causative</i> could be <i>t</i>, <i>dIr</i>, <i>Ar</i>, ...
 * <p/>
 * A <code>SuffixForm</code> holds the conditions for a transition:
 * <ul>
 * <li>precondition : Condition to permit transition of <code>Suffix</code> with the {@link SuffixForm}
 * <li>postCondition: Condition to permit transitions after <code>Suffix</code> with the {@link SuffixForm}.</li>
 * <li>postDerivativeCondition Condition to permit derivational transitions after <code>Suffix</code> with the {@link SuffixForm}</li>
 * </ul>
 *
 * @see SuffixFormSequence
 */
public class SuffixForm {

    private final Suffix suffix;
    private final SuffixFormSequence form;
    private final Specification<MorphemeContainer> precondition;
    private final Specification<MorphemeContainer> postCondition;
    private final Specification<MorphemeContainer> postDerivativeCondition;

    public SuffixForm(Suffix suffix, String form, Specification<MorphemeContainer> precondition,
                      Specification<MorphemeContainer> postCondition, Specification<MorphemeContainer> postDerivativeCondition) {
        this.suffix = suffix;
        this.form = new SuffixFormSequence(form);
        this.precondition = precondition;
        this.postCondition = postCondition;
        this.postDerivativeCondition = postDerivativeCondition;
    }

    /**
     * @return {@link Suffix} which current <code>SuffixForm</code> belongs to.
     */
    public Suffix getSuffix() {
        return suffix;
    }

    /**
     * @return Suffix form sequence which is created from string representation of the SuffixForm.
     */
    public SuffixFormSequence getForm() {
        return form;
    }

    /**
     * @return some condition
     * @see SuffixForm
     */
    public Specification<MorphemeContainer> getPostCondition() {
        return postCondition;
    }

    /**
     * @return some condition
     * @see SuffixForm
     */
    public Specification<MorphemeContainer> getPrecondition() {
        return precondition;
    }

    /**
     * @return some condition
     * @see SuffixForm
     */
    public Specification<MorphemeContainer> getPostDerivativeCondition() {
        return postDerivativeCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuffixForm that = (SuffixForm) o;

        if (!form.equals(that.form)) return false;
        if (postCondition != null ? !postCondition.equals(that.postCondition) : that.postCondition != null)
            return false;
        if (postDerivativeCondition != null ? !postDerivativeCondition.equals(that.postDerivativeCondition) : that.postDerivativeCondition != null)
            return false;
        if (precondition != null ? !precondition.equals(that.precondition) : that.precondition != null) return false;
        if (!suffix.equals(that.suffix)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = suffix.hashCode();
        result = 31 * result + form.hashCode();
        result = 31 * result + (precondition != null ? precondition.hashCode() : 0);
        result = 31 * result + (postCondition != null ? postCondition.hashCode() : 0);
        result = 31 * result + (postDerivativeCondition != null ? postDerivativeCondition.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SuffixForm{" +
                "suffix=" + suffix +
                ", form='" + form + '\'' +
                ", precondition=" + precondition +
                ", postCondition=" + postCondition +
                ", postDerivativeCondition=" + postDerivativeCondition +
                '}';
    }

}
