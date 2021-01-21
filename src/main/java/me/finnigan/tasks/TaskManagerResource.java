package me.finnigan.tasks;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import me.finnigan.tasks.github.GitHubService;
import me.finnigan.tasks.model.Task;

@Path("/")
public class TaskManagerResource {
  @CheckedTemplate(basePath = "")
  private static class Templates {
      public static native TemplateInstance index(Collection<Task> tasks);
      public static native TemplateInstance createTask();
      public static native TemplateInstance viewTask(Task task);
      public static native TemplateInstance editTask(Task task, String fieldFocus);
      public static native TemplateInstance errorMessage(String title, String message);
  }

  @Inject
  GitHubService githubService;

  @GET
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance homepage() throws IOException {
    return Templates.index(githubService.allOpenTasks());
  }

  @GET
  @Path("/task")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance createTaskPage() throws IOException {
    return Templates.createTask();
  }

  @GET
  @Path("/task/{number}")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance taskPage(@PathParam("number") int taskNumber, @QueryParam("edit") boolean update, @QueryParam("focus") String focusFieldName) throws IOException {
    if (update) {
      return Templates.editTask(githubService.task(taskNumber), focusFieldName);
    } else {
      return Templates.viewTask(githubService.task(taskNumber));
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/task")
  public Response createTask(@MultipartForm TaskForm taskForm) {
    try {
      githubService.createTask(taskForm.title, taskForm.body);
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getLocalizedMessage())
          .build();
    }
    return Response.status(Status.MOVED_PERMANENTLY)
          .location(URI.create("/"))
          .build();
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/task/{number}")
  public Response updateTask(@PathParam("number") int taskNumber, @MultipartForm TaskForm taskForm) {
    try {
      githubService.updateTask(taskNumber, taskForm.title, taskForm.body);
      return Response.status(Status.MOVED_PERMANENTLY)
                      .location(URI.create("/"))
                      .build();
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getLocalizedMessage())
          .build();
    }
  }

  @POST
  @Path("/task/{number}/complete")
  public Response completeTask(@PathParam("number") int taskNumber) {
    try {
      githubService.closeTask(taskNumber, "[COMPLETED]");
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to complete task - #" + taskNumber, e.getLocalizedMessage()).render())
          .build();
    }
  }

  @POST
  @Path("/task/{number}/delete")
  public Response deleteTask(@PathParam("number") int taskNumber) {
    try {
      githubService.closeTask(taskNumber, "[DELETED]");
      return Response.noContent().build();
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
      .entity(Templates.errorMessage("Failed to delete task - #" + taskNumber, e.getLocalizedMessage()).render())
      .build();
    }
  }
}
