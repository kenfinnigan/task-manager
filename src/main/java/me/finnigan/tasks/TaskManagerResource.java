package me.finnigan.tasks;

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
      public static native TemplateInstance index();
      public static native TemplateInstance allTasks(Collection<Task> tasks);
      public static native TemplateInstance createTask();
      public static native TemplateInstance viewTask(Task task);
      public static native TemplateInstance editTask(Task task, String fieldFocus);
      public static native TemplateInstance errorMessage(String title, Exception exception);
      public static native TemplateInstance completedTask(int taskNumber);
  }

  @Inject
  GitHubService githubService;

  @GET
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance homepage() {
    return Templates.index();
  }

  @GET
  @Path("/task")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public Response allTasksPage() {
    try {
      return Response
          .ok(Templates.allTasks(githubService.allOpenTasks()))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to retrieve tasks", e))
          .build();
    }
  }

  @GET
  @Path("/task/create")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance createTaskPage() {
    return Templates.createTask();
  }

  @GET
  @Path("/task/{number}")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public Response viewTaskPage(@PathParam("number") int taskNumber) {
    try {
      return Response
          .ok(Templates.viewTask(githubService.task(taskNumber)))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to retrieve task - #" + taskNumber, e))
          .build();
    }
  }

  @GET
  @Path("/task/{number}/edit")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public Response editTaskPage(@PathParam("number") int taskNumber, @QueryParam("focus") String focusFieldName) {
    try {
      return Response
          .ok(Templates.editTask(githubService.task(taskNumber), focusFieldName))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to retrieve task - #" + taskNumber, e))
          .build();
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/task")
  public Response createTask(@MultipartForm TaskForm taskForm) {
    try {
      githubService.createTask(taskForm.title, taskForm.body);
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to create task", e))
          .build();
    }
    return Response.status(Status.MOVED_PERMANENTLY)
          .location(URI.create("/task"))
          .build();
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/task/{number}")
  public Response updateTask(@PathParam("number") int taskNumber, @MultipartForm TaskForm taskForm) {
    try {
      githubService.updateTask(taskNumber, taskForm.title, taskForm.body);
      return Response.status(Status.MOVED_PERMANENTLY)
                      .location(URI.create("/task"))
                      .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to update task - #" + taskNumber, e))
          .build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("/task/{number}/complete")
  public Response completeTask(@PathParam("number") int taskNumber) {
    try {
      githubService.closeTask(taskNumber, "[COMPLETED]");
      return Response
          .ok(Templates.completedTask(taskNumber))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(Templates.errorMessage("Failed to complete task - #" + taskNumber, e))
          .build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("/task/{number}/delete")
  public Response deleteTask(@PathParam("number") int taskNumber) {
    try {
      githubService.closeTask(taskNumber, "[DELETED]");
      return Response
          .ok(Templates.completedTask(taskNumber))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
      .entity(Templates.errorMessage("Failed to delete task - #" + taskNumber, e))
      .build();
    }
  }
}
