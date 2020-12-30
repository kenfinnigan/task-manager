package me.finnigan.tasks;

import java.io.IOException;
import java.net.URI;
import java.util.List;

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
import org.kohsuke.github.GHIssue;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

@Path("/")
public class TaskManagerResource {
  @CheckedTemplate(basePath = "")
  public static class Templates {
      public static native TemplateInstance index(List<GHIssue> tasks);

      public static native TemplateInstance viewTask(GHIssue task);

      public static native TemplateInstance editTask(GHIssue task);
  }

  @Inject
  GitHubService githubService;

  @GET
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance homepage() throws IOException {
    return Templates.index(githubService.allTasks());
  }

  @GET
  @Path("/task/{id}")
  @Consumes(MediaType.TEXT_HTML)
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance task(@PathParam("id") int taskNumber, @QueryParam("edit") boolean update) throws IOException {
    if (update) {
      return Templates.editTask(githubService.task(taskNumber));
    } else {
      return Templates.viewTask(githubService.task(taskNumber));
    }
  }


  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/task/{id}")
  public Response updateTask(@PathParam("id") int taskNumber, @MultipartForm TaskForm taskForm) {
    try {
      githubService.updateTask(taskNumber, taskForm.title, taskForm.body);
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getLocalizedMessage())
          .build();
    }
    return Response.status(Status.MOVED_PERMANENTLY)
          .location(URI.create("/"))
          .build();
  }
}
