// @ts-check
const {themes} = require('prism-react-renderer');
const lightCodeTheme = themes.github;
const darkCodeTheme = themes.dracula;

const baseUrl = '/jmx_exporter/';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Prometheus JMX Exporter',
  tagline: 'A collector to capture JMX MBean values.',
  favicon: 'img/favicon.ico',

  url: 'https://prometheus.github.io',
  baseUrl,

  onBrokenLinks: 'throw',

  headTags: [
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        type: 'image/svg+xml',
        href: `${baseUrl}img/logo.svg`,
      },
    },
  ],

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          path: 'docs',
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          includeCurrentVersion: false,
          lastVersion: '1.6.0',
          versions: {
            '1.6.0': { label: '1.6.0', banner: 'none', badge: true },
            '1.5.0': { label: '1.5.0', banner: 'none', badge: true },
            '1.4.0': { label: '1.4.0', banner: 'none', badge: true },
            '1.3.0': { label: '1.3.0', banner: 'none', badge: true },
            '1.2.0': { label: '1.2.0', banner: 'none', badge: true },
            '1.1.0': { label: '1.1.0', banner: 'none', badge: true },
            'pre-1.1.0': { label: 'pre-1.1.0', banner: 'none', badge: true },
          },
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      docs: {
        sidebar: {
          hideable: true,
          autoCollapseCategories: true,
        },
      },
      navbar: {
        title: 'Prometheus JMX Exporter',
        logo: {
          alt: 'Prometheus JMX Exporter Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Documentation',
          },
          {
            type: 'docsVersionDropdown',
            position: 'left',
          },
          {
            href: 'https://github.com/prometheus/jmx_exporter',
            label: 'GitHub',
            position: 'right',
          },
          {
            href: 'https://github.com/prometheus/jmx_exporter/releases',
            label: 'Releases',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Documentation',
            items: [
              { label: 'Java Agent', to: '/java-agent/' },
              { label: 'Standalone', to: '/standalone/' },
              { label: 'HTTP Mode Configuration', to: '/http-mode/' },
            ],
          },
          {
            title: 'Community',
            items: [
              { label: 'GitHub', href: 'https://github.com/prometheus/jmx_exporter' },
              { label: 'Releases', href: 'https://github.com/prometheus/jmx_exporter/releases' },
              { label: 'Prometheus Community', href: 'https://prometheus.io/community/' },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} The Prometheus jmx_exporter Authors. Licensed under Apache License 2.0.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['java', 'bash', 'yaml', 'json', 'markdown', 'properties'],
      },
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
    }),
};

module.exports = config;
