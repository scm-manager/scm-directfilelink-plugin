package sonia.scm.directfilelink;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;

import java.net.URI;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@SubjectAware(configuration = "classpath:sonia/scm/directfilelink/shiro-001.ini", username = "user_1", password = "secret")
public class FileObjectLinkEnricherTest {

  private Provider<ScmPathInfoStore> scmPathInfoStoreProvider;

  @Rule
  public ShiroRule shiro = new ShiroRule();

  @Mock
  private HalAppender appender;
  private FileObjectLinkEnricher enricher;

  public FileObjectLinkEnricherTest() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    ThreadContext.remove();
  }

  @Before
  public void setUp() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("https://scm-manager.org/scm/api/"));
    scmPathInfoStoreProvider = Providers.of(scmPathInfoStore);
  }

  @Test
  @SubjectAware(username = "admin", password = "secret")
  public void shouldEnrich() {
    enricher = new FileObjectLinkEnricher(scmPathInfoStoreProvider);
    NamespaceAndName repo = new NamespaceAndName("space", "name");
    FileObject fileObject = new FileObject();
    fileObject.setPath("a.txt");
    HalEnricherContext context = HalEnricherContext.of(fileObject, repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("directFileLink", "https://scm-manager.org/scm/api/v2/plugins/directFileLink/space/name/a.txt");
  }

}
