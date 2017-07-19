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
 * Checks the repo ownership preconditions.
 * In order to give certain commands, the commader has to be a reoi
 * owner, unless his/her username is specified in .charles.yml under
 * commanders section.
 *
 * A user is considered owner of the repo if the following are true:
 * <ul>
 *   <li>The repo is under his/her name OR he/she is an active admin of the organization</li>
 *   <li>The repo is NOT a fork.</li>
 * </ul>
 * 
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.1 *
 */
public final class RepoOwnershipCheck extends PreconditionCheckStep {

    /**
     * Check if the commander is owner of the repo.
     */
    private Step check;

    /**
     * Ctor.
     * @param com Command.
     * @param onTrue Step to perform in case the check passes.
     * @param onFalse Step to perform in case the check fails.
     * @throws IOException In case the Repo data cannot be read from Github.
     */
    public RepoOwnershipCheck(Command com, Step onTrue, Step onFalse) throws IOException {
        super(onTrue, onFalse);
        PreconditionCheckStep repoForkCheck = new RepoForkCheck(
            com.repo().json(), super.onTrue(),
            super.onFalse()
        );
        this.check = new AuthorOwnerCheck(
            repoForkCheck,
            new OrganizationAdminCheck(
                 repoForkCheck,
                 super.onFalse()
            )
        );
        
        
    }

    @Override
    public void perform(Command command, Logger logger) {
        this.check.perform(command, logger);
    }

}
