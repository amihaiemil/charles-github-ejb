/**
 * Copyright (c) 2016-2017, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1)Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3)Neither the name of charles-rest nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.amihaiemil.charles.github;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.jcabi.github.Comment;
import com.jcabi.github.Comments;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Repos.RepoCreate;
import com.jcabi.github.mock.MkGithub;
import com.jcabi.github.mock.MkStorage;

/**
 * Unit tests for {@link action}
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 * 
 */
public class ActionTestCase {
    
    /**
     * More Actions are executed on separate threads.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void actionsExecute() throws Exception {
        Language english = (Language)new English();
        Issue issue1 = this.githubIssue("amihaiemil", "@charlesmike hello there");
        Issue issue2 = this.githubIssue("jeff", "@charlesmike hello");
        Issue issue3 = this.githubIssue("vlad", "@charlesmike, hello");
        Issue issue4 = this.githubIssue("marius", "@charlesmike hello");
        final Action ac1 = new Action(issue1);
        final Action ac2 = new Action(issue2);
        final Action ac3 = new Action(issue3);
        final Action ac4 = new Action(issue4);
        
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future> futures = new ArrayList<Future>();
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac1.perform();
            }
        }));
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac2.perform();
            }
        }));
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac3.perform();
                
            }
        }));
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac4.perform();
            }
        }));

        for(Future f : futures) {
            assertTrue(f.get()==null);
        }
        
        List<Comment> commentsWithReply1 = Lists.newArrayList(issue1.comments().iterate());
        List<Comment> commentsWithReply2 = Lists.newArrayList(issue2.comments().iterate());
        List<Comment> commentsWithReply3 = Lists.newArrayList(issue3.comments().iterate());
        List<Comment> commentsWithReply4 = Lists.newArrayList(issue4.comments().iterate());
        String expectedReply1 = "> @charlesmike hello there\n\n" + String.format(english.response("hello.comment"),"amihaiemil");
        assertTrue(commentsWithReply1.get(1).json().getString("body")
                .equals(expectedReply1)); //there should be only 2 comments - the command and the reply.
        
        String expectedReply2 = "> @charlesmike hello\n\n" + String.format(english.response("hello.comment"),"jeff");
        assertTrue(commentsWithReply2.get(1).json().getString("body")
                .equals(expectedReply2)); //there should be only 2 comments - the command and the reply.
        
        String expectedReply3 = "> @charlesmike, hello\n\n" + String.format(english.response("hello.comment"),"vlad");
        assertTrue(commentsWithReply3.get(1).json().getString("body")
                .equals(expectedReply3)); //there should be only 2 comments - the command and the reply.
        
        String expectedReply4 = "> @charlesmike hello\n\n" + String.format(english.response("hello.comment"),"marius");
        assertTrue(commentsWithReply4.get(1).json().getString("body")
                .equals(expectedReply4)); //there should be only 2 comments - the command and the reply.
        
    }
    
    /**
     * Start 2 actions but each of them fail with IOException
     * when the comments are first listed (within LastComment(...))
     * @throws Exception If something goes wrong
     */
    @Test
    public void actionsFail() throws Exception {
        Language english = (Language)new English();
        Issue issue1 = this.githubIssue("amihaiemil", "@charlesmike index");
        Issue issue2 = this.githubIssue("jeff", "@charlesmike index");
        
        Comments comments = Mockito.mock(Comments.class);
        Comment com = Mockito.mock(Comment.class);
        Mockito.when(com.json()).thenThrow(new IOException("expected IOException..."));
        Mockito.when(comments.iterate()).thenReturn(Arrays.asList(com));
        
        Issue mockedIssue1 = Mockito.mock(Issue.class);
        Mockito.when(mockedIssue1.comments())
            .thenReturn(comments)
            .thenReturn(issue1.comments());

        Issue mockedIssue2 = Mockito.mock(Issue.class);
        Mockito.when(mockedIssue2.comments())
            .thenReturn(comments)
            .thenReturn(issue2.comments());
        
        final Action ac1 = new Action(mockedIssue1);
        final Action ac2 = new Action(mockedIssue2);
        
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future> futures = new ArrayList<Future>();
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac1.perform();;
            }
        }));
        futures.add(executorService.submit(new Runnable() {
            @Override
            public void run() {
                ac2.perform();
            }
        }));

        for(Future f : futures) {
            assertTrue(f.get()==null);
        }
        
        List<Comment> commentsWithReply1 = Lists.newArrayList(issue1.comments().iterate());
        List<Comment> commentsWithReply2 = Lists.newArrayList(issue2.comments().iterate());

        String expectedStartsWith = "There was an error when processing your command. [Here](/";
        assertTrue(commentsWithReply1.get(1).json().getString("body")
                .startsWith(expectedStartsWith)); //there should be only 2 comments - the command and the reply.
        
        assertTrue(commentsWithReply2.get(1).json().getString("body")
                .startsWith(expectedStartsWith)); //there should be only 2 comments - the command and the reply.
        
    }
    
    /**
     * Creates an Issue with the given command.
     * @param commander Author of the comment;
     * @param command The comment's body;
     * @return Github issue
     */
    public Issue githubIssue(String commander, String command) throws Exception {
        MkStorage storage = new MkStorage.InFile();
        Github commanderGh = new MkGithub(storage, commander);
        RepoCreate repoCreate = new RepoCreate(commander + ".github.io", false);
        commanderGh.repos().create(repoCreate);
        Coordinates repoCoordinates = new Coordinates.Simple(commander, commander + ".github.io");
        Issue issue = commanderGh.repos().get(repoCoordinates).issues().create("Test issue for commands", "test body");
        issue.comments().post(command);
        Github agentGh = new MkGithub(storage, "charlesmike");
        return agentGh.repos().get(repoCoordinates).issues().get(issue.number());
        
    }

}
