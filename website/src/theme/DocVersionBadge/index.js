import React from 'react';
import clsx from 'clsx';
import {useDocsVersion} from '@docusaurus/plugin-content-docs/client';
import {ThemeClassNames} from '@docusaurus/theme-common';

export default function DocVersionBadge({className}) {
  const versionMetadata = useDocsVersion();
  if (!versionMetadata.badge) {
    return null;
  }

  return (
    <span
      className={clsx(
        className,
        ThemeClassNames.docs.docVersionBadge,
        'badge badge--secondary',
      )}>
      {versionMetadata.label}
    </span>
  );
}
