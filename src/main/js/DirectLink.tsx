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

import React from "react";
import { withTranslation, WithTranslation } from "react-i18next";
import { File, Link } from "@scm-manager/ui-types";

type Props = WithTranslation & {
  file: File;
};

class DirectLink extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  render() {
    const { t, file } = this.props;
    const link = file._links.directLink as Link;
    return (
      <tr>
        <td>{t("scm-directfilelink-plugin.latestVersion")}</td>
        <td className="is-word-break">
          <a href={link.href} target="_blank">
            {link.href}
          </a>
        </td>
      </tr>
    );
  }
}

export default withTranslation("plugins")(DirectLink);
