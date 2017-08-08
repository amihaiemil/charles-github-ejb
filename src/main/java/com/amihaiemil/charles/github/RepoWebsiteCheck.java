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

import org.slf4j.Logger;

/**
 * Checks the indexing preconditions.
 * 
 * In order for a repo to be indexed, it must meet one of the following
 * conditions in order to give a command:
 * <ul>
 *   <li>Has to be a website hosted on gh-pages (named owner.github.io)</li>
 *   <li>Has to have a gh-pages branch.</li>
 * </ul>
 * 
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.1
 */
public final class RepoWebsiteCheck extends PreconditionCheckStep {

    /**
     * Check if the Repo is indeed a website hosted on Github Pages.
     */
    private Step check;

    /**
     * Ctor.
     * @param com Command.
     * @param onTrue Step to perform in case the check passes.
     * @param onFalse Step to perform in case the check fails.
     * @throws IOException In case the Repo data cannot be read from Github.
     */
    public RepoWebsiteCheck(Command com, Step onTrue, Step onFalse) throws IOException {
        super(onTrue, onFalse);
        this.check = new RepoNameCheck(
            com.repo().json(), super.onTrue(),
            new GhPagesBranchCheck(
                super.onTrue(),
                super.onFalse()
            )
        );
    }

    @Override
    public void perform(Command command, Logger logger) {
        this.check.perform(command, logger);
    }

}
