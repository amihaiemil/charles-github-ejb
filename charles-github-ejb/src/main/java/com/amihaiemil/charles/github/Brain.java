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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.json.JsonObject;

import org.slf4j.Logger;

import com.amihaiemil.charles.steps.Step;

/**
 * The "brain" of the Github agent. Can understand commands and 
 * figure out the Steps that need to be performed to fulfill the 
 * command.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 * 
 */
public class Brain {

	/**
	 * All the languages that the chatbot can understang/speaks.
	 */
	private List<Language> languages = new LinkedList<Language>();

	/**
	 * The action's logger.
	 */
	private Logger logger;

	/**
	 * Location of the log file.
	 */
	private LogsLocation logsLoc;

	/**
	 * Constructor.
	 */
	public Brain(Logger logger, LogsLocation logsLoc) {
		this(logger, logsLoc, Arrays.asList((Language) new English()));
	}

	/**
	 * Constructor which takes the responses and languages.
	 * @param resp
	 * @param langs
	 */
	public Brain(Logger logger, LogsLocation logsLoc, List<Language> langs) {
		this.logger = logger;
		this.logsLoc = logsLoc;
		this.languages = langs;
	}
	
	/**
	 * Understand a command.
	 * @param com Given command.
	 * @return Steps.
	 * @throws IOException if something goes worng.
	 */
    public Steps understand(Command com) throws IOException {
    	 String authorLogin = com.authorLogin();
	     logger.info("Command author's login: " + authorLogin);
    	 List<Step> steps = new LinkedList<Step>();
    	 CommandCategory category = this.categorizeCommand(com);

    	 switch (category.type()) {
    	 	case "hello":
    	 		String hello = String.format(category.language().response("hello.comment"), authorLogin);
    	 		logger.info("Prepared response: " + hello);
    	 		steps.add(
    	 			new SendReply(
    	 				new TextReply(com, hello),
    	 				logger
    	 			)
    	 		);
    	 		break;
    	 	case "indexsite":
    	 		steps.addAll(this.indexSteps(com, category, false));
    	 		break;
    	 	case "indexpage":
    	 		steps.addAll(this.indexSteps(com, category, true));
    	 		break;
    	 	default:
    	 		logger.info("Unknwon command!");
    	 		String unknown = String.format(
    	 			category.language().response("unknown.comment"),
    	 			authorLogin);
    	 		logger.info("Prepared response: " + unknown);
    	 		steps.add(
        	 		new SendReply(
            	 		new TextReply(com, unknown),
            	 		this.logger
            	 	)
    	 		);
    	 		break;
		 }
    	 return new Steps(
    	     steps,
    	     new SendReply(
    	         new TextReply(
    	             com,
    	             String.format(
    	                 category.language().response("step.failure.comment"),
    	                 com.authorLogin(), "%s"
    	             ), this.logsLoc
    	         ),
    	         this.logger
    	     )
    	 );
    }

    /**
     * Find out the type and Language of a command.
     * @param com Received Command.
     * @param logger Logger to use.
     * @return CommandCategory, which defaults to unknown command and
     *  first language in the agent's languages list (this.languages)
     */
    private CommandCategory categorizeCommand(Command com) {
        CommandCategory category = new CommandCategory("unknown", languages.get(0));
   	    for(Language l : languages) {
   		    category = l.categorize(com);
   		    if(category.isUnderstood()) {
   		    	this.logger.info("Command type: " + category.type() + ". Language: " + l.getClass().getName());
   			    break;
   		    }
   	    }
   	    return category;
    }
    
    /**
     * Build the list of steps that need to be taken when receiving an index command.
     * @param com Received Command, 
     * @param category Command category, containing language and type.
     * @return List of Step.
     * @throws IOException If something goes wrong.
     */
    private List<Step> indexSteps(Command com, CommandCategory category, boolean singlePage
        ) throws IOException {
        List<Step> steps = new ArrayList<Step>();
		steps.add(
		    new SendReply(
		        new TextReply(
		            com,
		            String.format(
		                category.language().response("index.start.comment"),
		                com.authorLogin()
		            )
		        ),
		        this.logger
		    )		
		);
		JsonObject repo = com.issue().repo().json();
		IndexPreconditionCheck preconditions = new IndexPreconditionCheck.IndexPreconditionCheckBuilder(
		    com, repo, category.language(), this.logger, this.logsLoc
		)
		.repoForkCheck(new RepoForkCheck(repo, this.logger))
		.authorOwnerCheck(new AuthorOwnerCheck(com, repo, this.logger))
		.repoNameCheck(new RepoNameCheck(repo, this.logger))
		.ghPagesBranchCheck(new GhPagesBranchCheck(repo, this.logger))
		.build();

        FollowupSteps followup = new FollowupSteps.FollowupStepsBuilder()
        .starRepo(new StarRepo(com.issue().repo(), this.logger))
        .confirmationReply(
            new SendReply(
                new TextReply(
        		    com,
        			String.format(
        			    category.language().response("index.finished.comment"),
        				com.authorLogin(), repo.getString("name"), "%s"
                    ),
                    this.logsLoc
                ), this.logger
            )		
	    )
	    .build();
		
		steps.add(
		    new IndexSteps(com, repo, preconditions, followup, this.logger, singlePage)
		);

		return steps;
     }
}
