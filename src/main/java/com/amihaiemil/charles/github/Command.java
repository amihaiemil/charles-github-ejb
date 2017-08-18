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

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import com.jcabi.github.Issue;
import com.jcabi.github.User;
import com.jcabi.http.Request;
import com.jcabi.http.response.JsonResponse;

/**
 * Command for the github agent.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 */
public abstract class Command {

    /**
     * Comment json.
     */
    private JsonObject comment;

    /**
     * Github issue.
     */
    private Issue issue;
    
    /**
     * Command type.
     */
    private String type;

    /**
     * Command language.
     */
    private Language lang;
    
    /**
     * Ctor.
     * @param issue Github issue where this command is given.
     * @param comment Command contents.
     */
    public Command(Issue issue, JsonObject comment) {
        this(issue, comment, "unknown", new English());
    }
    
    /**
     * Ctor.
     * @param issue Github issue where this command is given.
     * @param comment Command contents.
     * @param type Command type.
     * @param lang Language of the command.
     */
    public Command(Issue issue, JsonObject comment, String type, Language lang) {
        this.issue = issue;
        this.comment = comment;
        this.type = type;
        this.lang = lang;
    }

    /**
     * The json comment.
     * @return Json Object representing the comment on Github issue.
     */
    public JsonObject json() {
        return this.comment;
    }

    /**
     * Specify the command's type.
     */
    protected void type(final String type) {
    	this.type = type;
    }
    
    /**
     * Specify the command's language.
     */
    protected void language(Language lang) {
    	this.lang = lang;
    }
    
    /**
     * Specify the comment json of this command.
     * @param com
     */
    protected void comment(JsonObject com) {
        this.comment = com;
    }
    
    /**
     * Parent issue.
     * @return com.jcabi.github.Issue
     */
    public Issue issue() {
        return this.issue;
    }

    /**
     * Username of the Github agent.
     * @return Github agent's String username.
     * @throws IOException 
     */
    public String agentLogin() throws IOException {
        return this.issue.repo()
                   .github()
                   .users()
                   .self()
                   .login();
    }

    /**
     * Username of this command's author.
     * @return String Github username.
     */
    public String authorLogin() {
        return this.comment.getJsonObject("user").getString("login");
    }

    /**
     * Email address of this command's author.
     * @return String email address.
     * @throws IOException if there is an error while making the HTTP call
     * to get the author's email address.
     */
    public String authorEmail() throws IOException {
        String email = "";
        User author = this.issue
        		          .repo()
        		          .github()
        		          .users()
        		          .get(this.authorLogin());
        for(final String address : author.emails().iterate()) {
        	email = address;
        }
        return email;
    }

    /**
     * Get the commands' author org membership.
     * @return Json membership as described
     * <a href="https://developer.github.com/v3/orgs/members/#get-organization-membership">here</a>
     * or an empty Json object if the repo owner is not an organization.
     * @throws IOException
     */
    public JsonObject authorOrgMembership() throws IOException {
        if(this.repo().isOwnedByOrganization()) {
            Request req = this.issue.repo().github().entry()
                .uri().path("/orgs/")
                .path(this.repo().ownerLogin())
                .path("/memberships/")
                .path(this.authorLogin()).back();
            return req.fetch().as(JsonResponse.class).json().readObject();
        } else {
            return Json.createObjectBuilder().build();
        }
            
    }

    /**
     * Returns the commanded repository.
     * @return CommandedRepo.
     */
    public CachedRepo repo() {
        return new CachedRepo(this.issue.repo());
    }

    /**
     * Returns the elasticsearch index name for this command.
     * @return String indexname.
     * @throws IOException If something goes wrong while reading
     *  the repo json form the Github API.
     */
    public String indexName() throws IOException {
        return this.repo().ownerLogin().toLowerCase() + "x" + this.repo().name().toLowerCase();
    }

    /**
     * Type of this command.
     * @return String.
     */
    public String type() {
    	return this.type;
    }
    
    /**
     * Language of this command.
     * @return Language.
     */
    public Language language() {
    	return this.lang;
    }
    
}
