/**
 * Copyright (c) 2010, Sebastian Sdorra All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of SCM-Manager;
 * nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.directfilelink;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.util.HttpUtil;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Sebastian Sdorra
 */
@Singleton
public class DirectFileLinkServlet extends HttpServlet
{

  /** Field description */
  private static final Pattern PATTERN = Pattern.compile("/([^/]+)/(.*)");

  /** Field description */
  private static final long serialVersionUID = -4766582966425203440L;

  /**
   *   the logger for DirectFileLinkServlet
   */
  private static final Logger logger =
    LoggerFactory.getLogger(DirectFileLinkServlet.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param serviceFactory
   */
  @Inject
  public DirectFileLinkServlet(RepositoryServiceFactory serviceFactory)
  {
    this.serviceFactory = serviceFactory;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param request
   * @param response
   *
   * @throws IOException
   * @throws ServletException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    Subject subject = SecurityUtils.getSubject();

    if (!subject.isAuthenticated())
    {
      HttpUtil.sendUnauthorized(response);
    }
    else
    {
      String uri = request.getPathInfo();
      Matcher matcher = PATTERN.matcher(uri);

      if (matcher.matches())
      {
        String repositoryId = matcher.group(1);
        String path = matcher.group(2);

            handleRequest(response, repositoryId, path);

      }
      else
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param response
   * @param repo
   * @param path
   *
   * @throws IOException
   */
  private void handleRequest(HttpServletResponse response, String repo,
    String path)
    throws IOException
  {

    // decode path, see http://goo.gl/H869J6
    path = HttpUtil.decode(path);
    logger.trace("load file {} from repository {}", path, repo);

    RepositoryService service = null;
    ServletOutputStream stream = null;

    try
    {
      service = serviceFactory.create(repo);
      stream = response.getOutputStream();

      service.getCatCommand().retriveContent(stream, path);
    }
    finally
    {
      Closeables.close(service, true);
      Closeables.close(stream, true);
    }
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private final RepositoryServiceFactory serviceFactory;
}
