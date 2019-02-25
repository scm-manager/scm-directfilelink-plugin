package sonia.scm.directfilelink.api;

import com.github.sdorra.spotter.ContentType;
import com.github.sdorra.spotter.ContentTypes;
import com.google.inject.Inject;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.util.IOUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;


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
  @Produces(MediaType.APPLICATION_JSON)
  @StatusCodes({
    @ResponseCode(code = 200, condition = "success"),
    @ResponseCode(code = 401, condition = "not authenticated / invalid credentials"),
    @ResponseCode(code = 403, condition = "not authorized, the current user does not have the privilege"),
    @ResponseCode(code = 500, condition = "internal server error")
  })
  public Response get(@PathParam("namespace") String namespace, @PathParam("name") String name, @PathParam("path") String path) throws IOException {
    try (RepositoryService repositoryService = serviceFactory.create(new NamespaceAndName(namespace, name))) {
      Repository repository = repositoryService.getRepository();
      RepositoryPermissions.read(repository).check();
      ChangesetPagingResult changesets = repositoryService.getLogCommand()
        .getChangesets();
      if (changesets != null && changesets.getChangesets() != null && !changesets.getChangesets().isEmpty()) {
        Optional<Changeset> changeset = changesets.getChangesets().stream().max(Comparator.comparing(Changeset::getDate));
        if (!changeset.isPresent()) {
          return Response.status(Response.Status.NOT_FOUND).build();
        }
        String revision = changeset.get().getId();
        StreamingOutput stream = createStreamingOutput(namespace, name, revision, path);
        Response.ResponseBuilder responseBuilder = Response.ok(stream);
        return createContentHeader(namespace, name, revision, path, repositoryService, responseBuilder);
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }
  }

  private StreamingOutput createStreamingOutput(String namespace, String name, String revision, String path) {
    return os -> {
      try (RepositoryService repositoryService = serviceFactory.create(new NamespaceAndName(namespace, name))) {
        repositoryService.getCatCommand().setRevision(revision).retriveContent(os, path);
        os.close();
      } catch (NotFoundException e) {
        log.debug(e.getMessage());
        throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
    };
  }

  private Response createContentHeader(String namespace, String name, String revision, String path, RepositoryService repositoryService, Response.ResponseBuilder responseBuilder) {
    try {
      appendContentHeader(path, getHead(revision, path, repositoryService), responseBuilder);
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

  private byte[] getHead(String revision, String path, RepositoryService repositoryService) throws IOException {
    InputStream stream = repositoryService.getCatCommand().setRevision(revision).getStream(path);
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
