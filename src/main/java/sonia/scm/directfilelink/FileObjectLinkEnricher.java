package sonia.scm.directfilelink;

import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.directfilelink.api.DirectFileLinkResource;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.inject.Provider;

@Extension
@Enrich(FileObject.class)
public class FileObjectLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStoreProvider;

  @Inject
  public FileObjectLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStoreProvider) {
    this.scmPathInfoStoreProvider = scmPathInfoStoreProvider;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    NamespaceAndName repository = context.oneRequireByType(NamespaceAndName.class);
    FileObject fileObject = context.oneRequireByType(FileObject.class);
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStoreProvider.get().get(), DirectFileLinkResource.class);
    String href = linkBuilder.method("get").parameters(repository.getNamespace(), repository.getName(), fileObject.getPath()).href();
    appender.appendLink("directLink", href);
  }
}
