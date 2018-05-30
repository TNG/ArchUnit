module.exports = function (grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    smoosher: {
      all: {
        options: {
          jsDir: 'build/resources/main/com/tngtech/archunit/visual/report/'
        },
        //TODO: create consts for paths
        files: {
          'build/resources/main/com/tngtech/archunit/visual/report/filter.html': 'src/main/app/report/menu/filter.html',
          'build/resources/main/com/tngtech/archunit/visual/report/filter-bar.html': 'src/main/app/report/menu/filter-bar.html',
          'build/resources/main/com/tngtech/archunit/visual/report/legend.html': 'src/main/app/report/menu/legend.html',
          'build/resources/main/com/tngtech/archunit/visual/report/settings.html': 'src/main/app/report/menu/settings.html',
          'build/resources/main/com/tngtech/archunit/visual/report/menu.html': 'src/main/app/report/menu/menu.html',
          'build/resources/main/com/tngtech/archunit/visual/report/violation-menu.html': 'src/main/app/report/violation-menu/violation-menu.html',
          'build/resources/main/com/tngtech/archunit/visual/report/report.html': 'src/main/app/report/report.html'
        }
      }
    },
    inline_web_components: {
      options: {
        components: {
          'visualization-filter-bar': 'build/resources/main/com/tngtech/archunit/visual/report/filter-bar.html',
          'visualization-filter': 'build/resources/main/com/tngtech/archunit/visual/report/filter.html',
          'visualization-settings': 'build/resources/main/com/tngtech/archunit/visual/report/settings.html',
          'visualization-legend': 'build/resources/main/com/tngtech/archunit/visual/report/legend.html',
          'visualization-menu': 'build/resources/main/com/tngtech/archunit/visual/report/menu.html',
          'violation-menu': 'build/resources/main/com/tngtech/archunit/visual/report/violation-menu.html'
        }
      },
      dist: {
        files: [{
          expand: true,
          cwd: 'build/resources/main/com/tngtech/archunit/visual/report',
          src: '{menu,report}.html',
          dest: 'build/resources/main/com/tngtech/archunit/visual/report'
        }]
      },
    }
  });

  grunt.loadNpmTasks('grunt-html-smoosher');
  grunt.loadNpmTasks('grunt-inline-web-components');

  grunt.registerTask('default', ['smoosher', 'inline_web_components']);

};

//TODO: weiteres Vorgehen: mit webpack klappt bundlen von visualiaztion und web-comp-infra bereits --> diese
// werden im build gespeichert;
// jetzt: am besten wäre, js und css in den report und die anderen html-files auch mit webpack zu binden (siehe vorletzter Commit oder so)
// Und: inlinen der web components in die html-files mit grunt --> aber wie??
// Evtl. auch so: mit grunt alle web-components rekursiv inlinen und css inlinen, falls das geht, und das Erzeugnis
// im build ablegen; gleichzeitig mit webpack js-bundlen; dann js mit webpack inlinen (im selben Task)


//TODO: nur noch zu tun: inline web components --> Rest klappt!! :))))
//TODO: evtl. bei WebComponents verschiedene IDs verwenden wegen Inlining!!
//TODO: watch iwie einbauen
