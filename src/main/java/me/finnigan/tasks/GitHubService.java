package me.finnigan.tasks;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;

import okhttp3.OkHttpClient;

@ApplicationScoped
public class GitHubService {

  private final GHRepository repository;

  @Inject
  public GitHubService(@ConfigProperty(name = "github.token") String token,
      @ConfigProperty(name = "github.user") String ghUser, @ConfigProperty(name = "github.repository") String ghRepo) {

    GitHub github = null;
    GHRepository repo = null;
    try {
      github = new GitHubBuilder()
                  .withOAuthToken(token, ghUser)
                  .withConnector(new OkHttpConnector(new OkHttpClient()))
                  .build();
      repo = github.getRepository(ghRepo);
    } catch (IOException e) {
      //TODO Fix this
      e.printStackTrace();
    } finally {
      this.repository = repo;
    }
  }

  public List<GHIssue> allTasks() throws IOException {
    return repository.getIssues(GHIssueState.OPEN);
  }

  public GHIssue task(int taskNumber) throws IOException {
    return repository.getIssue(taskNumber);
  }

  public void updateTask(int taskNumber, String title, String body) throws IOException {
    GHIssue task = task(taskNumber);
    task.setTitle(title);
    task.setBody(body);
  }
}
