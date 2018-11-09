'use strict';

const { spawn } = require('child_process');

const spawnPromise = (cmd, arg) => new Promise(((resolve, reject) => {
  const execution = spawn(cmd, arg || [], { cwd: __dirname, stdio: 'inherit' });

  execution.on('close', (code) => {
    if (code !== 0) {
      return reject(code);
    }
    return resolve();
  });
}));

const defaultOptions = {
  doPrepare: true,
  doRun: true,
  ignorePattern: '',
  mochaParams: [],
};

class MochaMobile {
  constructor(sourcePath, options) {
    this.sourcePath = sourcePath || process.cwd();

    this.options = Object.assign({}, defaultOptions, options);
    if (!this.options.arch) {
      throw new Error('Unknown architecture');
    }

    if (this.options.arch !== 'android') {
      throw new Error('Sorry, only android architecture implemented so far.');
    }

    // force use colors due to colors are switched off by dafault
    // on alternative platforms
    const colorOptions = ['--colors', '-c', '--no-colors', '-C'];
    if (!this.options.mochaParams.filter(opt => colorOptions.includes(opt)).length) {
      this.options.mochaParams.push('--colors');
    }
  }

  run() {
    let execution = Promise.resolve();

    if (this.options.doPrepare) {
      execution = execution.then(() => {
        console.log('> Build and install mobile app');
        const params = [this.sourcePath];
        if (this.options.ignorePattern) {
          params.push(`--ignore=${this.options.ignorePattern}`);
        }
        return spawnPromise(`../mocha-mobile-apps/${this.options.arch}/prepare-${this.options.arch}-test.sh`,
          params);
      });
    }

    if (this.options.doRun) {
      execution = execution.then(() => {
        console.log('> Run test');
        return spawnPromise(`../mocha-mobile-apps/${this.options.arch}/node-${this.options.arch}-proxy.sh`,
          this.options.mochaParams);
      });
    }

    return execution;
  }
}

module.exports = MochaMobile;
