// @flow

import React from "react";
import { translate } from "react-i18next";
import type { File, Repository } from "@scm-manager/ui-types";

type Props = {
  file: File,
  repository: Repository,
  // context prop
  t: string => string
};

class DirectLink extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

  render() {
    const { t, file } = this.props;
    const link = file._links.directLink.href;
    return (
      <tr>
        <td>{t("scm-directfilelink-plugin.lastVersion")}</td>
        <td className="is-word-break">
          <a href={link} target="_blank">
            {link}
          </a>
        </td>
      </tr>
    );
  }
}

export default translate("plugins")(DirectLink);
