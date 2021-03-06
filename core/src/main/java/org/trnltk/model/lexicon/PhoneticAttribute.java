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

package org.trnltk.model.lexicon;

import org.trnltk.common.structure.StringEnum;
import org.trnltk.common.structure.StringEnumMap;

public enum PhoneticAttribute implements StringEnum<PhoneticAttribute> {
    LastLetterVowel("LLV"),
    LastLetterConsonant("LLC"),

    LastVowelFrontal("LVF"),
    LastVowelBack("LVB"),
    LastVowelRounded("LVR"),
    LastVowelUnrounded("LVuR"),

    LastLetterVoiceless("LLVless"),
    LastLetterNotVoiceless("LLNotVless"),

    LastLetterVoicelessStop("LLStop"),

    FirstLetterVowel("FLV"),
    FirstLetterConsonant("FLC"),

    HasNoVowel("NoVow");

    private final static StringEnumMap<PhoneticAttribute> shortFormToPosMap = StringEnumMap.get(PhoneticAttribute.class);

    private final String shortForm;

    private PhoneticAttribute(String shortForm) {
        this.shortForm = shortForm;
    }

    @Override
    public String getStringForm() {
        return shortForm;
    }

    public static StringEnumMap<PhoneticAttribute> converter() {
        return shortFormToPosMap;
    }
}
