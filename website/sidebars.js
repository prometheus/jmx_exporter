// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  "docsSidebar": [
    "intro",
    {
      "type": "category",
      "label": "Getting Started",
      "collapsed": false,
      "items": ["getting-started/quick-start"]
    },
    {
      "type": "category",
      "label": "Deployment",
      "collapsed": false,
      "items": [
        "deployment/modes",
        "deployment/java-agent",
        "deployment/standalone",
        "deployment/isolator-java-agent"
      ]
    },
    {
      "type": "category",
      "label": "Configuration",
      "collapsed": false,
      "items": [
        "configuration/index",
        "configuration/rules",
        "configuration/object-names",
        "configuration/metric-customizers",
        "configuration/http-server",
        "configuration/authentication",
        "configuration/ssl",
        "configuration/opentelemetry"
      ]
    },
    {
      "type": "category",
      "label": "Examples",
      "collapsed": true,
      "items": ["examples/index"]
    },
    {
      "type": "category",
      "label": "Reference",
      "collapsed": true,
      "items": [
        "reference/configuration",
        "reference/artifacts",
        "reference/collector-api"
      ]
    },
    "building_testing/index",
    "contributing",
    "license"
  ]
};

module.exports = sidebars;
