/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.pretestedintegration.scm.git;

import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 *
 * @author andrius
 */
public class GetAllCommitsFromBranchCallback extends RepositoryListenerAwareCallback<String> {
    public final ObjectId id;
    public final String branch;
    
    public GetAllCommitsFromBranchCallback(TaskListener listener, final ObjectId id, final String branch ) {
        super(listener);
        this.id = id; 
        this.branch = branch;
    }

    @Override
    public String invoke(Repository repo, VirtualChannel channel) throws IOException, InterruptedException {        
        StringBuilder sb = new StringBuilder();
        RevWalk walk = new RevWalk(repo);
        
        // commit on our branch, resolved from the jGit object id
        RevCommit commit = walk.parseCommit(id);

        // walk tree starting from the integration commit
        walk.markStart(commit);
       
        // limit the tree walk to keep away from master commits
        // Reference for this idea is: https://wiki.eclipse.org/JGit/User_Guide#Restrict_the_walked_revision_graph
        ObjectId to = repo.resolve("master");
        walk.markUninteresting(walk.parseCommit(to));

        // build the complete commit message, to look like squash commit msg
        // iterating over the commits that will be integrated
        for (RevCommit rev : walk) {

            sb.append(String.format("commit %s",rev.getName()));
            sb.append(String.format("%n"));
            // In the commit message overview, the author is right one to give credit (author wrote the code)
            sb.append(String.format("Author: %s <%s>",rev.getAuthorIdent().getName(), rev.getAuthorIdent().getEmailAddress()));
            sb.append(String.format("%n"));
            
            Integer secondsSinceUnixEpoch = rev.getCommitTime();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy ZZZZ"); //yyyy-MM-dd'T'hh:mm:ssZZZZ");
            Date commitTime = new Date(secondsSinceUnixEpoch * 1000L); // seconds to milis
            String asString = formatter.format(commitTime);
            sb.append(String.format("Date:   %s", asString ));
            sb.append(String.format("%n"));
            sb.append(String.format("%n"));

            String newlinechar = System.getProperty("line.separator");
            // Using spaces in git commit message formatting, to avoid inconsistent
            // results based on tab with, and to mimic normal recommendations
            // on writing commit message (indented bullet lists with space)
            // following (same) examples:
            // http://chris.beams.io/posts/git-commit/
            // http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
            // 4 spaces are used, as this is how the squashed commit message looks like
            Integer numberOfSpaces = 4;
            String indentation = String.format("%" + numberOfSpaces + "s", "");
            String fullMessage = rev.getFullMessage();
            Pattern myregexp = Pattern.compile(newlinechar, Pattern.MULTILINE);
            
            String newstring = myregexp.matcher(fullMessage).replaceAll(newlinechar + indentation);
            
            sb.append(String.format(indentation + "%s", newstring));
            sb.append(String.format("%n"));
            sb.append(String.format("%n"));
        }
        
        walk.dispose();
        
        return sb.toString();
    }    
}