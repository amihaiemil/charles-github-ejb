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

import javax.json.JsonObject;

import org.slf4j.Logger;

import com.amihaiemil.charles.GraphCrawl;
import com.amihaiemil.charles.IgnoredPatterns;
import com.amihaiemil.charles.WebCrawl;
import com.amihaiemil.charles.aws.AmazonEsRepository;
import com.amihaiemil.charles.steps.IndexPage;
import com.amihaiemil.charles.steps.IndexSite;
import com.amihaiemil.charles.steps.Step;

/**
 * Step taken by the Github agent to perform an index command.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 *
 */
public class IndexSteps implements Step {

	/**
	 * Index site command.
	 */
	private Command com;

	/**
	 * Json repository as returned by the Github API.
	 */
	private JsonObject repoJson;

	/**
	 * Action's logger.
	 */
	private Logger logger;

    /**
     * To perform after the index command has been executed successfully.
     */
    private Step followup;

    /**
     * Was the index command given for a single page or the whole website?
     */
    private boolean singlePage;

    /**
     * Constructor.
     * @param com Command.
     */
    public IndexSteps(Command com, JsonObject repo, Step followup, Logger logger, boolean singlePage) {
        this.com = com;
        this.repoJson = repo;
        this.followup = followup;
        this.logger = logger;
        this.singlePage = singlePage;
    }

    /**
     * Perform the step.
     */
    @Override
    public boolean perform() {
        if(this.indexStep(this.com).perform()) {
            return followup.perform();
        }
        return false;
	}

    /**
     * Return an IndexSite Step for the given repo.
     * @param ownerLogin Username of the repo's owner.
     * @return IndexSite instance.
     */
    Step indexStep(Command com) {
    	String repoName = repoJson.getString("name");
    	String phantomJsExecPath =  System.getProperty("phantomjsExec");
	    if(phantomJsExecPath == null || "".equals(phantomJsExecPath)) {
	        phantomJsExecPath = "/usr/local/bin/phantomjs";
	    }

    	if(!this.singlePage) {
    		String repoSiteName = com.authorLogin() + ".github.io/";
            String url;
            
            
            if(repoSiteName.equals(repoName)) {//if the repo is a site on its own, named owner.github.io
    	    	url = "http://" + repoName;
    	    } else {//the repo has a gh-pages branch
    	    	url = "http://" + repoSiteName + repoName;
    	    }
    	    WebCrawl siteCrawl = new GraphCrawl(
    	        url, phantomJsExecPath, new IgnoredPatterns(),
    	        new AmazonEsRepository(com.authorLogin() + "/" + repoName), 20
    	    );
    	    return new IndexSite(siteCrawl, logger);
    	}
        return new IndexPage(
            com.json().getString("body"),
            com.authorLogin() + "/" + repoName,
            phantomJsExecPath,
            logger
        );

    }

}
