package me.finnigan.tasks;

import javax.ws.rs.FormParam;

public class TaskForm {
  public @FormParam("title") String title;
  public @FormParam("body") String body;
}
