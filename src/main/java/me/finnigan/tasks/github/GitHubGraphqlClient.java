package me.finnigan.tasks.github;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.vertx.core.json.JsonObject;

@RegisterRestClient(baseUri = "https://api.github.com/graphql", configKey = "githubClient")
public interface GitHubGraphqlClient {
  @POST
  JsonObject executeQuery(@HeaderParam(HttpHeaders.AUTHORIZATION) String authentication, JsonObject graphqlQuery);
}
