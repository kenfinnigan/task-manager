package me.finnigan.tasks;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.runtime.util.ExceptionUtil;

@TemplateExtension
public class ExceptionTemplateExtension {
  static String stacktrace(Exception exception) {
    return escapeHTML(ExceptionUtil.generateStackTrace(exception));
  }

  static String rootCauseFirstStackTrace(Exception exception) {
    return escapeHTML(ExceptionUtil.rootCauseFirstStackTrace(exception));
  }

  static String escapeHTML(String content) {
    return content.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;");
  }
}
