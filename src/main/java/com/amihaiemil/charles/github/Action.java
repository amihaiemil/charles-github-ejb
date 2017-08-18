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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcabi.github.Issue;

/**
 * Action that the agent takes once it finds a Github issue where it's been mentioned.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 * 
 */
public class Action {

    /**
     * This action's logger. Each action has its own logger
     * so any action is logged separately in its own log file.
     */
    private Logger logger;

    /**
     * Id.
     */
    private String id;

    /**
     * Github issue where the command was given.
     */
    private Issue issue;

    /**
     * Location of the logs.
     */
    private LogsLocation logs;

    /**
     * Constructor.
     * @param issue - The Github issue where the agent was mentioned.
     * @param agentLogin - the Github agent's login (which is the same for all actions), to save http calls.
     * @throws IOException If the file appender cannot be instantiated.
     */
    public Action(Issue issue) throws IOException {
        this.id = UUID.randomUUID().toString();
        this.issue = issue;
        this.setupLog4jForAction();
        this.logs = new LogsOnServer(
            System.getProperty("charles.rest.logs.endpoint"), this.id + ".log"
        );
    }
    
    
    public void perform() {
        Command command;
        try {
            this.logger.info("Started action " + this.id);
            command = new CachedCommand(
                new ValidCommand(
                    new LastComment(issue)
                )
            );
            String commandBody = command.json().getString("body");
            this.logger.info("Received command: " + commandBody);
            
            final Knowledge knowledge = new Conversation(
                this.logs,
                new Hello(
                    new IndexSiteKn(
                        this.logs,
                        new IndexSitemapKn(
                            this.logs,
                            new IndexPageKn(
                                this.logs,
                                new DeleteIndexKn(
                                    this.logs,
                                    new Confused()
                                )
                            )
                        )
                    )
                )
            );
            final Step steps = knowledge.handle(command);
            steps.perform(command, logger);
        } catch (final IllegalArgumentException e) {
            this.logger.warn("No command found in the issue or the agent has already replied to the last command!");
        } catch (final IOException e) {
            this.logger.error("Action failed entirely with exception: ",  e);
            this.sendReply(
                new ErrorReply(logs.address(), this.issue)
            );
        }
    }

    /**
     * Send the reply to Github issue.
     * @param reply
     */
    private void sendReply(Reply reply) {
        try {
            reply.send();
        } catch (IOException e) {
            this.logger.error("FAILED TO REPLY!", e);
        }
    }

    /**
     * Setup the Log4J logger for this action thread.
     * @return String path to log file
     * @throws IOException If there's something wrong with the FileAppender.
     */
    private void setupLog4jForAction() throws IOException {
        String loggerName = "Action_" + this.id;
        org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("Action_" + this.id);
        String logRoot = System.getProperty("LOG_ROOT");
        if(logRoot == null) {
            logRoot = ".";
        }
        String logFilePath = logRoot + "/charles-rest/ActionsLogs/" + this.id + ".log";
        
        File logFile = new File(logFilePath);
        logFile.getParentFile().mkdirs();
        logFile.createNewFile();//you have to create the file yourself since FileAppender acts funny under linux if the file doesn't already exist.

        FileAppender fa = new FileAppender(new PatternLayout("%d %p - %m%n"), logFilePath);
        fa.setName(this.id + "_appender");
        fa.setThreshold(Level.DEBUG);
        log4jLogger.addAppender(fa);
        log4jLogger.setLevel(Level.DEBUG);
        
        this.logger = LoggerFactory.getLogger(loggerName);
        
    }
}
