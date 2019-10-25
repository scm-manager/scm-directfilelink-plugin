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
