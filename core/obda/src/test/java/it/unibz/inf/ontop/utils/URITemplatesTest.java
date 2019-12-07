package it.unibz.inf.ontop.utils;

/*
 * #%L
 * ontop-obdalib-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.injection.OntopModelConfiguration;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class URITemplatesTest {

    private static final TermFactory TERM_FACTORY = OntopModelConfiguration.defaultBuilder().build().getTermFactory();
	
	@SuppressWarnings("unchecked")
    @Test
	public void testFormat(){
		assertEquals("http://example.org/A/1", Templates.format("http://example.org/{}/{}", "A", 1));
		
		assertEquals("http://example.org/A", Templates.format("http://example.org/{}", "A"));
		
		assertEquals("http://example.org/A/1", Templates.format("http://example.org/{}/{}", Arrays.asList("A", 1)));
		
		assertEquals("http://example.org/A", Templates.format("http://example.org/{}", Arrays.asList("A")));

        assertEquals("http://example.org/A", Templates.format("{}", Arrays.asList("http://example.org/A")));
	}

    @Test
    public void testGetUriTemplateString1(){
        ImmutableFunctionalTerm f1 = createIRITemplateFunctionalTerm("http://example.org/{}/{}", //
                ImmutableList.of(TERM_FACTORY.getVariable("X"), TERM_FACTORY.getVariable("Y")));
        assertEquals("http://example.org/{X}/{Y}", Templates.getTemplateString(f1));
    }

    @Test
    public void testGetUriTemplateString2(){
        ImmutableFunctionalTerm f1 = createIRITemplateFunctionalTerm("{}",
                ImmutableList.of(TERM_FACTORY.getVariable("X")));
        assertEquals("{X}", Templates.getTemplateString(f1));
    }

    @Test
    public void testGetUriTemplateString3(){

        ImmutableFunctionalTerm f1 = createIRITemplateFunctionalTerm("{}/", //
                ImmutableList.of(TERM_FACTORY.getVariable("X")));
        assertEquals("{X}/", Templates.getTemplateString(f1));
    }

    @Test
    public void testGetUriTemplateString4(){

        ImmutableFunctionalTerm f1 = createIRITemplateFunctionalTerm("http://example.org/{}/{}/", //
                ImmutableList.of(TERM_FACTORY.getVariable("X"), TERM_FACTORY.getVariable("Y")));
        assertEquals("http://example.org/{X}/{Y}/", Templates.getTemplateString(f1));
    }

    @Test
    public void testGetUriTemplateString5(){

        ImmutableFunctionalTerm f1 = createIRITemplateFunctionalTerm("http://example.org/{}/{}/{}", //
                ImmutableList.of(TERM_FACTORY.getVariable("X"), TERM_FACTORY.getVariable("Y"), TERM_FACTORY.getVariable("X")));
        assertEquals("http://example.org/{X}/{Y}/{X}", Templates.getTemplateString(f1));
    }

    /**
     * Functional term corresponding to the lexical term
     */
    private ImmutableFunctionalTerm createIRITemplateFunctionalTerm(String iriTemplate,
                                                                    ImmutableList<? extends ImmutableTerm> arguments) {
        return (ImmutableFunctionalTerm) TERM_FACTORY.getIRIFunctionalTerm(iriTemplate, arguments).getTerm(0);
    }
}
