/**
 * Copyright (c) 2016-2017, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1)Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3)Neither the name of charles-rest nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.amihaiemil.charles.github;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import java.io.IOException;

/**
 * Unit tests for {@link Conversation}
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.1
 */
public final class ConversationTestCase {

    /**
     * Conversation can understand the command using the first language.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void understandsFirstLanguage() throws Exception {
        final Command com = Mockito.mock(Command.class);
        Mockito.when(com.issue()).thenReturn(null);
        Mockito.when(com.json()).thenReturn(null);

        final Language english = Mockito.mock(Language.class);
        Mockito.when(english.categorize(com)).thenReturn("yes");
        final Language french = Mockito.mock(Language.class);
        Mockito.when(french.categorize(com)).thenReturn("unknown");

        final Knowledge conversation = new Conversation(
            Mockito.mock(LogsLocation.class),
            new Knowledge() {
                @Override
                public Step handle(final Command com) throws IOException {
                    MatcherAssert.assertThat(
                        com.type(),
                        Matchers.equalTo("yes")
                    );
                    return null;
                }
            }, english, french
        );
        conversation.handle(com);
    }

    /**
     * Conversation can understand the command using the second language.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void understandsSecondLanguage() throws Exception {
        final Command com = Mockito.mock(Command.class);
        Mockito.when(com.issue()).thenReturn(null);
        Mockito.when(com.json()).thenReturn(null);

        final Language english = Mockito.mock(Language.class);
        Mockito.when(english.categorize(com)).thenReturn("unknown");
        final Language french = Mockito.mock(Language.class);
        Mockito.when(french.categorize(com)).thenReturn("oui");

        final Knowledge conversation = new Conversation(
                Mockito.mock(LogsLocation.class),
                new Knowledge() {
                    @Override
                    public Step handle(final Command com) throws IOException {
                        MatcherAssert.assertThat(
                                com.type(),
                                Matchers.equalTo("oui")
                        );
                        return null;
                    }
                }, english, french
        );
        conversation.handle(com);
    }

    /**
     * Conversation cannot understand the command, its type is "unknwon"
     * @throws Exception If something goes wrong.
     */
    @Test
    public void doesNotUnderstand() throws Exception {
        final Command com = Mockito.mock(Command.class);
        Mockito.when(com.issue()).thenReturn(null);
        Mockito.when(com.json()).thenReturn(null);

        final Language english = Mockito.mock(Language.class);
        Mockito.when(english.categorize(com)).thenReturn("unknown");

        final Knowledge conversation = new Conversation(
                Mockito.mock(LogsLocation.class),
                new Knowledge() {
                    @Override
                    public Step handle(final Command com) throws IOException {
                        MatcherAssert.assertThat(
                                com.type(),
                                Matchers.equalTo("unknown")
                        );
                        return null;
                    }
                }, english
        );
        conversation.handle(com);
    }
}
