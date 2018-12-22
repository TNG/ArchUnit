process.on('unhandledRejection', report => {
    /* eslint-disable no-console */
    console.error(report);
    /* eslint-enable no-console */
    process.exit(1);
});