package me.finnigan.tasks.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import me.finnigan.tasks.model.Task;

@ApplicationScoped
public class GitHubService {

  @CheckedTemplate(basePath = "github")
  private static class Templates {
    public static native TemplateInstance repoDetails(String user, String repository);
    public static native TemplateInstance allOpenIssues(String user, String repository);
    public static native TemplateInstance taskDetail(String user, String repository, int taskNumber);
    public static native TemplateInstance updateTask(String id, String title, String body);
    public static native TemplateInstance createTask(String repositoryId, String issueTemplate, String title, String body);
  }

  private final String token;
  private final String user;
  private final String repository;
  private final String defaultIssueTemplate;

  private String repositoryGlobalId;

  @Inject
  @RestClient
  GitHubGraphqlClient client;

  @Inject
  public GitHubService(@ConfigProperty(name = "github.token") String token,
      @ConfigProperty(name = "github.user") String ghUser,
      @ConfigProperty(name = "github.repository") String ghRepo,
      @ConfigProperty(name = "github.task-template.default") String defaultIssueTemplate) {
    this.user = ghUser;
    this.repository = ghRepo;
    this.token = "Bearer " + token;
    this.defaultIssueTemplate = defaultIssueTemplate;
  }

  @PostConstruct
  void getRepositoryId() {
    String query = Templates.repoDetails(user, repository).render();
    JsonObject response = client.executeQuery(token, new JsonObject().put("query", query));

    if (response.getJsonArray("errors") != null) {
        throw new RuntimeException(response.toString());
    }

    repositoryGlobalId = response.getJsonObject("data").getJsonObject("repository").getString("id");
  }

  public Collection<Task> allOpenTasks() throws IOException {
    String query = Templates.allOpenIssues(user, repository).render();
    JsonObject response = client.executeQuery(token, new JsonObject().put("query", query));

    if (response.getJsonArray("errors") != null) {
        throw new IOException(response.toString());
    }

    List<Task> tasks = new ArrayList<>();
    JsonArray issues = response.getJsonObject("data")
            .getJsonObject("repository")
            .getJsonObject("issues")
            .getJsonArray("nodes");
    for (int i = 0; i < issues.size(); i++) {
        tasks.add(issues.getJsonObject(i).mapTo(Task.class));
    }
    return tasks;
  }

  public Task task(int taskNumber) throws IOException {
    String query = Templates.taskDetail(user, repository, taskNumber).render();
    JsonObject response = client.executeQuery(token, new JsonObject().put("query", query));

    if (response.getJsonArray("errors") != null) {
        throw new IOException(response.toString());
    }
    return response.getJsonObject("data").getJsonObject("repository").getJsonObject("issue").mapTo(Task.class);
  }

  public void createTask(String title, String body) throws IOException {
    String query = Templates.createTask(repositoryGlobalId, defaultIssueTemplate, title, body).render();
    JsonObject response = client.executeQuery(token, new JsonObject().put("query", query));

    if (response.getJsonArray("errors") != null) {
        throw new IOException(response.toString());
    }
  }
  
  public void updateTask(int taskNumber, String title, String body) throws IOException {
    Task task = task(taskNumber);

    String query = Templates.updateTask(task.id, title, body).render();
    JsonObject response = client.executeQuery(token, new JsonObject().put("query", query));

    if (response.getJsonArray("errors") != null) {
        throw new IOException(response.toString());
    }
  }
}
