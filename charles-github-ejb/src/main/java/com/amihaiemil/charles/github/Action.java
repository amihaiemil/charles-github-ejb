/*
 * Copyright (c) 2016, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1)Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3)Neither the name of charles-github-ejb nor the names of its
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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that the agent takes once it finds a Github issue where it's been mentioned.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 * 
 */
public class Action implements Runnable {

	private Logger logger;

	/**
	 * Thread that runs this.
	 */
	private Thread tr;

	/**
	 * Github issue where the command was given.
	 */
	private GithubIssue issue;

	/**
	 * Github username of the agent.
	 */
	private String agentLogin;

	/**
	 * Brain of the github agent.
	 */
	private Brain br;	
	
	/**
	 * Location of the logs.
	 */
	private LogsLocation logs;
	
	/**
	 * Constructor.
	 * @param issue - The Github issue where the agent was mentioned.
	 * @param logs - Location of the logs.
	 * @param agentLogin - The Github username of the agent.
	 * @param resp Possible responses.
	 * @throws IOException If the file appender cannot be instantiated.
	 */
	public Action(Brain br, GithubIssue issue, String agentLogin) throws IOException {
		this.tr = new Thread(this, UUID.randomUUID().toString());
		this.agentLogin = agentLogin;
		this.issue = issue;
		this.br = br;
		
		String logFilePath = this.setupLog4jForAction();
		String logsEndpoint = System.getProperty("charles.rest.logs.endpoint");
		if(logsEndpoint != null) {
			this.logs = new LogsOnServer(logsEndpoint, this.tr.getName() + ".log");
		} else {
			this.logs = new LogsInGist(
                logFilePath, this.issue.getSelf().repo().github().gists()
			);
		}
	}
	
	
	@Override
	public void run() {
		ValidCommand command;
		try {
			logger.info("Started action " + this.tr.getName());
			LastComment lc = new LastComment(issue, agentLogin);
			command = new ValidCommand(lc);
			String commandBody = command.json().getString("body");
			logger.info("Received command: " + commandBody);
			Steps steps = br.understand(command, logger, logs);
			boolean success = steps.perform();
			if(success){
				logger.info("Finished action " + this.tr.getName());
			} else {
				logger.error("Some steps did not execute successfully! Check above for details.");
			}
		} catch (IllegalArgumentException e) {
			logger.info("No command found in the issue or the agent has already replied to the last command!");
		} catch (IOException e) {
			logger.error("Action failed with IOException: ",  e);
			this.sendReply(
				new ErrorReply(logs.address(), this.issue.getSelf())
			);
		}
	}
	
	/**
	 * Take this action.
	 */
	public void take() { 
		this.tr.start();
	}

	/**
	 * Send the reply to Github issue.
	 * @param reply
	 */
	private void sendReply(Reply reply) {
		try {
			reply.send();
		} catch (IOException e) {
			logger.error("FAILED TO REPLY!", e);
		}
	}

	/**
	 * Setup the Log4J logger for this action thread.
	 * @return String path to log file
	 * @throws IOException If there's something wrong with the FileAppender.
	 */
	private String setupLog4jForAction() throws IOException {
		String loggerName = "Action_" + this.tr.getName();
		org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("Action_" + this.tr.getName());
		String logRoot = System.getProperty("LOG_ROOT");
		if(logRoot == null) {
			logRoot = "";
		}
		String logFilePath = logRoot + "/Charles-Github-Ejb/ActionsLogs/" + this.tr.getName() + ".log";
		
		File logFile = new File(logFilePath);
		logFile.getParentFile().mkdirs();
		logFile.createNewFile();//you have to create the file yourself since FileAppender acts funny under linux if the file doesn't already exist.

		FileAppender fa = new FileAppender(new PatternLayout("%d %c{1} - %m%n"), logFilePath);
		fa.setName(this.tr.getName() + "_appender");
		fa.setThreshold(Level.DEBUG);
		log4jLogger.addAppender(fa);
		log4jLogger.setLevel(Level.DEBUG);
		
		this.logger = LoggerFactory.getLogger(loggerName);
		
		return fa.getFile();
	}
}
