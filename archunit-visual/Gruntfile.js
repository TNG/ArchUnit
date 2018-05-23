module.exports = function (grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    smoosher: {
      all: {
        files: {
          'build/resources/main/com/tngtech/archunit/visual/report/report.html': 'src/main/app/report/report.html'
        }
      }
    }
  });

  grunt.loadNpmTasks('grunt-html-smoosher');

  grunt.registerTask('default', ['smoosher']);

};

//TODO: weiteres Vorgehen: mit webpack klappt bundlen von visualiaztion und web-comp-infra bereits --> diese
// werden im build gespeichert;
// jetzt: am besten wäre, js und css in den report und die anderen html-files auch mit webpack zu binden (siehe vorletzter Commit oder so)
// Und: inlinen der web components in die html-files mit grunt --> aber wie??
// Evtl. auch so: mit grunt alle web-components rekursiv inlinen und css inlinen, falls das geht, und das Erzeugnis
// im build ablegen; gleichzeitig mit webpack js-bundlen; dann js mit webpack inlinen (im selben Task)