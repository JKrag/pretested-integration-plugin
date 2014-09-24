/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.pretestedintegration.integration.scm.git;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.CleanCheckout;
import hudson.plugins.git.extensions.impl.PruneStaleBranch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.jenkinsci.plugins.pretestedintegration.exceptions.UnsupportedConfigurationException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author Mads
 */
public class GeneralBehaviourIT {
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    private Repository repository;
    
    @After
    public void tearDown() throws Exception {
        repository.close();
        if (repository.getDirectory().getParentFile().exists()) {
            FileUtils.deleteQuietly(repository.getDirectory().getParentFile());
        }
    }

    @Test
    public void failWhenRepNameIsBlankAndGitHasMoreThanOneRepo() throws Exception {
        repository = TestUtilsFactory.createValidRepository("test-repo");
        Repository repository2 = TestUtilsFactory.createValidRepository("test-repo2");
        
        Git git = new Git(repository);
        git.checkout().setName("master").call();

        List<UserRemoteConfig> config = Arrays.asList(new UserRemoteConfig("file://" + repository.getDirectory().getAbsolutePath(), null, null, null), new UserRemoteConfig("file://" + repository2.getDirectory().getAbsolutePath(), null, null, null));
        
        FreeStyleProject project = TestUtilsFactory.configurePretestedIntegrationPlugin(jenkinsRule.createFreeStyleProject(), TestUtilsFactory.STRATEGY_TYPE.ACCUMULATED, config, null);
        
        assertEquals(1, jenkinsRule.jenkins.getQueue().getItems().length);

        QueueTaskFuture<Queue.Executable> future = jenkinsRule.jenkins.getQueue().getItems()[0].getFuture();

        do {
            Thread.sleep(1000);
        } while (!future.isDone());

        int nextBuildNumber = project.getNextBuildNumber();
        FreeStyleBuild build = project.getBuildByNumber(nextBuildNumber - 1);

        //Show the log for the latest build
        String text = jenkinsRule.createWebClient().getPage(build, "console").asText();
        System.out.println("=====BUILD-LOG=====");
        System.out.println(text);
        System.out.println("=====BUILD-LOG=====");
                
        assertTrue(text.contains(UnsupportedConfigurationException.ILLEGAL_CONFIG_NO_REPO_NAME_DEFINED));
            
        assertTrue(build.getResult().isWorseOrEqualTo(Result.FAILURE));
        repository2.close();
        if (repository2.getDirectory().getParentFile().exists()) {
            FileUtils.deleteQuietly(repository2.getDirectory().getParentFile());
        }
    }
    
    @Test
    public void remoteOrigin1WithMoreThan1RepoShouldBeSuccessful() throws Exception {
        repository = TestUtilsFactory.createValidRepository("test-repo");
        Repository repository2 = TestUtilsFactory.createValidRepository("test-repo2");
        
        Git git = new Git(repository);
        git.checkout().setName("master").call();

        List<UserRemoteConfig> config = Arrays.asList(new UserRemoteConfig("file://" + repository.getDirectory().getAbsolutePath(), null, null, null), new UserRemoteConfig("file://" + repository2.getDirectory().getAbsolutePath(), null, null, null));
        
        List<GitSCMExtension> gitSCMExtensions = new ArrayList<GitSCMExtension>();
        gitSCMExtensions.add(new PruneStaleBranch());
        gitSCMExtensions.add(new CleanCheckout());
                        
        GitSCM gitSCM = new GitSCM(config,
        Collections.singletonList(new BranchSpec("ready/**")),
            false, Collections.<SubmoduleConfig>emptyList(),
            null, null, gitSCMExtensions);
        
        FreeStyleProject project = TestUtilsFactory.configurePretestedIntegrationPlugin(jenkinsRule.createFreeStyleProject(), TestUtilsFactory.STRATEGY_TYPE.ACCUMULATED, config, "origin1");
        project.setScm(gitSCM);

        
        assertEquals(1, jenkinsRule.jenkins.getQueue().getItems().length);

        QueueTaskFuture<Queue.Executable> future = jenkinsRule.jenkins.getQueue().getItems()[0].getFuture();

        do {
            Thread.sleep(1000);
        } while (!future.isDone());

        int nextBuildNumber = project.getNextBuildNumber();
        FreeStyleBuild build = project.getBuildByNumber(nextBuildNumber - 1);

        //Show the log for the latest build
        String text = jenkinsRule.createWebClient().getPage(build, "console").asText();
        System.out.println("=====BUILD-LOG=====");
        System.out.println(text);
        System.out.println("=====BUILD-LOG=====");
                
        assertTrue(text.contains(UnsupportedConfigurationException.ILLEGAL_CONFIG_NO_REPO_NAME_DEFINED));
            
        assertTrue(build.getResult().isWorseOrEqualTo(Result.FAILURE));
        repository2.close();
        if (repository2.getDirectory().getParentFile().exists()) {
            FileUtils.deleteQuietly(repository2.getDirectory().getParentFile());
        }
    }
    
    @Test
    public void testOperationWithMultiSCM() throws Exception {
        repository = TestUtilsFactory.createValidRepository("test-repo");
        
        List<UserRemoteConfig> config = Arrays.asList(new UserRemoteConfig("file://" + repository.getDirectory().getAbsolutePath(), null, null, null), new UserRemoteConfig("file://" +repository.getDirectory().getAbsolutePath(), null, null, null));
        
        FreeStyleProject project = TestUtilsFactory.configurePretestedIntegrationPluginWithMultiSCM(jenkinsRule.createFreeStyleProject(), TestUtilsFactory.STRATEGY_TYPE.SQUASH, config, "origin", repository);
        
        assertEquals(1, jenkinsRule.jenkins.getQueue().getItems().length);

        QueueTaskFuture<Queue.Executable> future = jenkinsRule.jenkins.getQueue().getItems()[0].getFuture();

        do {
            Thread.sleep(1000);
        } while (!future.isDone());

        int nextBuildNumber = project.getNextBuildNumber();
         
        FreeStyleBuild build = project.getBuildByNumber(nextBuildNumber - 1);
        
        //Show the log for the latest build
        String text = jenkinsRule.createWebClient().getPage(build, "console").asText();
        System.out.println("=====BUILD-LOG=====");
        System.out.println(text);
        System.out.println("=====BUILD-LOG=====");
        
        assertTrue(build.getResult().isBetterOrEqualTo(Result.SUCCESS));
    }
    
}