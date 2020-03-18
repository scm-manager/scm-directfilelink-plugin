/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
Sonia.repository.ContentPanel.prototype.appendRepositoryPropertiesDirectFileLink 
        = Sonia.repository.ContentPanel.prototype.appendRepositoryProperties;

Ext.override(Sonia.repository.ContentPanel, {
  
  appendRepositoryProperties: function(bar){
    // encode path, see http://goo.gl/H869J6
    var url = 'directfilelink/' + this.repository.id + '/' + encodeURIComponent(this.path);
    bar.push(' ',{
      xtype: 'tbtext', 
      html: '<a style="color: white; font-weight: bold;" href="' + url + '">(Download Latest Version)</a>'
    });
    this.appendRepositoryPropertiesDirectFileLink(bar);
  }
  
});