package sonia.scm.directfilelink.api;

import com.github.sdorra.spotter.ContentType;
import com.github.sdorra.spotter.ContentTypes;
import com.google.inject.Inject;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.util.IOUtil;
import sonia.scm.web.VndMediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@OpenAPIDefinition(tags = {
  @Tag(name = "Direct File Link Plugin", description = "Direct File Link plugin provided endpoints")
})
@Slf4j
@Path(DirectFileLinkResource.PATH)
public class DirectFileLinkResource {

  public static final String PATH = "v2/plugins/directFileLink";
  private static final int HEAD_BUFFER_SIZE = 1024;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public DirectFileLinkResource(RepositoryServiceFactory serviceFactory) {
    this.serviceFactory = serviceFactory;
  }

  @GET
  @Path("/{namespace}/{name}/{path: .*}")
  @Operation(summary = "Download file", description = "Downloads file by path.", tags = "Direct File Link Plugin")
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized /  the current user does not have the \"repository:read\" privilege")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  public Response get(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("path") String path) throws IOException {
    try (RepositoryService repositoryService = serviceFactory.create(new NamespaceAndName(namespace, name))) {
      Repository repository = repositoryService.getRepository();
      RepositoryPermissions.read(repository).check();

      StreamingOutput stream = createStreamingOutput(namespace, name, path);
      Response.ResponseBuilder responseBuilder = Response.ok(stream);
      return createContentHeader(namespace, name, path, repositoryService, responseBuilder);
    }
  }

  private StreamingOutput createStreamingOutput(String namespace, String name, String path) {
    return os -> {
      try (RepositoryService repositoryService = serviceFactory.create(new NamespaceAndName(namespace, name))) {
        repositoryService.getCatCommand().retriveContent(os, path);
        os.close();
      } catch (NotFoundException e) {
        log.debug(e.getMessage());
        throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
    };
  }

  private Response createContentHeader(String namespace, String name, String path, RepositoryService repositoryService, Response.ResponseBuilder responseBuilder) {
    try {
      appendContentHeader(path, getHead(path, repositoryService), responseBuilder);
    } catch (IOException e) {
      log.info("error reading repository resource {} from {}/{}", path, namespace, name, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return responseBuilder.build();
  }

  private void appendContentHeader(String path, byte[] head, Response.ResponseBuilder responseBuilder) {
    ContentType contentType = ContentTypes.detect(path, head);
    responseBuilder.header("Content-Type", contentType.getRaw());
    contentType.getLanguage().ifPresent(language -> responseBuilder.header("X-Programming-Language", language));
  }

  private byte[] getHead(String path, RepositoryService repositoryService) throws IOException {
    InputStream stream = repositoryService.getCatCommand().getStream(path);
    try {
      byte[] buffer = new byte[HEAD_BUFFER_SIZE];
      int length = stream.read(buffer);
      if (length < 0) {
        return new byte[]{};
      } else if (length < buffer.length) {
        return Arrays.copyOf(buffer, length);
      } else {
        return buffer;
      }
    } finally {
      IOUtil.close(stream);
    }
  }

}
