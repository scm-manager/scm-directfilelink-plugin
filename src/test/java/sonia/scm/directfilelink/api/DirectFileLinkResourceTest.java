/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package sonia.scm.directfilelink.api;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Lists;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.CatCommandBuilder;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SubjectAware(configuration = "classpath:sonia/scm/directfilelink/shiro-001.ini", username = "user_1", password = "secret")
public class DirectFileLinkResourceTest {

  private Dispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  @Rule
  public ShiroRule shiro = new ShiroRule();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private RepositoryServiceFactory factory;

  @Mock
  private RepositoryService repoService;

  @Before
  public void init() {
    DirectFileLinkResource resource = new DirectFileLinkResource(factory);
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getRegistry().addSingletonResource(resource);
    when(factory.create(any(NamespaceAndName.class))).thenReturn(repoService);
    Repository repo = new Repository("id", "git", "space", "name");
    when(repoService.getRepository()).thenReturn(repo);
  }

  public DirectFileLinkResourceTest() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    ThreadContext.remove();
  }

  @Test
  @SubjectAware(username = "admin", password = "secret")
  public void shouldGetFile() throws URISyntaxException, IOException {
    LogCommandBuilder builder = mock(LogCommandBuilder.class );
    when(repoService.getLogCommand()).thenReturn(builder);
    ChangesetPagingResult changesets = mock(ChangesetPagingResult.class);

    when(builder.getChangesets()).thenReturn(changesets);
    Changeset changeset1 = new Changeset("1", new Date().getTime(), new Person("author"));
    List<Changeset> list = Lists.newArrayList(changeset1);
    when(changesets.getChangesets()).thenReturn(list);

    CatCommandBuilder catBuilder = mock(CatCommandBuilder.class);
    when(repoService.getCatCommand()).thenReturn(catBuilder);
    when(catBuilder.setRevision(any())).thenReturn(catBuilder);
    InputStream stream = mock(InputStream.class);
    when(catBuilder.getStream("a.txt")).thenReturn(stream);

    MockHttpRequest request = MockHttpRequest
      .get("/" + DirectFileLinkResource.PATH + "/space/repo/a.txt")
      .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
    assertThat(response.getStatus())
      .isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  @SubjectAware(username = "admin", password = "secret")
  public void shouldGetLatestRevision() throws URISyntaxException, IOException {
    LogCommandBuilder builder = mock(LogCommandBuilder.class );
    when(repoService.getLogCommand()).thenReturn(builder);
    ChangesetPagingResult changesets = mock(ChangesetPagingResult.class);

    when(builder.getChangesets()).thenReturn(changesets);

    Changeset changeset1 = new Changeset("1", Instant.MIN.getEpochSecond(), new Person("author"));
    Changeset changeset2 = new Changeset("2", Instant.now().getEpochSecond(), new Person("author"));
    Changeset changeset3 = new Changeset("3", Instant.MAX.getEpochSecond(), new Person("author"));
    List<Changeset> list = Lists.newArrayList(changeset1, changeset2, changeset3);
    when(changesets.getChangesets()).thenReturn(list);

    CatCommandBuilder catBuilder = mock(CatCommandBuilder.class);
    when(repoService.getCatCommand()).thenReturn(catBuilder);
    when(catBuilder.setRevision("3")).thenReturn(catBuilder);
    InputStream stream = mock(InputStream.class);
    when(catBuilder.getStream("a.txt")).thenReturn(stream);

    MockHttpRequest request = MockHttpRequest
      .get("/" + DirectFileLinkResource.PATH + "/space/repo/a.txt")
      .contentType(MediaType.APPLICATION_JSON);


    dispatcher.invoke(request, response);
    assertThat(response.getStatus())
      .isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  public void shouldForbidNotPermittedUser() throws URISyntaxException {
    thrown.expectMessage("Subject does not have permission [repository:read:id]");

    MockHttpRequest request = MockHttpRequest
      .get("/" + DirectFileLinkResource.PATH + "/space/repo/a.txt")
      .contentType(MediaType.APPLICATION_JSON);

    dispatcher.invoke(request, response);
  }

}
