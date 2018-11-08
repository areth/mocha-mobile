'use strict';

const spawn = require('child_process').spawn;

const spawnPromise = (cmd, arg) => {
  return new Promise(function(resolve, reject) {
    let execution = spawn(cmd, arg || [], { cwd: __dirname, stdio: 'inherit' });

    execution.on('close', (code) => {
      if (code !== 0) {
        return reject(code);
      }
      resolve();
    });
  });
};

const defaultOptions = {
  doPrepare: true,
  doRun: true,
  mochaParams: [],
};

class MochaMobile {
  constructor(sourcePath, options) {
    this.sourcePath = sourcePath || process.cwd();
    
    this.options = Object.assign({}, defaultOptions, options);
    if(!this.options.arch) {
      throw new Error('Unknown architecture');
    }

    if(this.options.arch !== 'android') {
      throw new Error('Sorry, only android architecture implemented so far.');
    }

    // force use colors due to colors are switched off by dafault 
    // on alternative platforms
    const colorOptions = ['--colors', '-c', '--no-colors', '-C'];
    if(!this.options.mochaParams.filter(opt => colorOptions.includes(opt)).length) {
      this.options.mochaParams.push('--colors');
    }
  }

  run() {
    let execution = Promise.resolve();
            
    if(this.options.doPrepare) {
      execution = execution.then(() => {
        console.log("> Build and install mobile app");
        return spawnPromise(`../${this.options.arch}/prepare-${this.options.arch}-test.sh`,
          [this.sourcePath]);
      });
    }

    if(this.options.doRun) {
      execution = execution.then(() => {
        console.log("> Run test");
        return spawnPromise(`../${this.options.arch}/node-${this.options.arch}-proxy.sh`,
          this.options.mochaParams);
      });
    }

    return execution;
  }
}

module.exports = MochaMobile;