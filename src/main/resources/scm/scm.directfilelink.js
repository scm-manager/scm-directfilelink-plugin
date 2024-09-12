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