'use strict';

const { spawn } = require('child_process');
const archiver = require('archiver');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const glob = require('glob');

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
    const appSrcPath = path.resolve(__dirname, '../mocha-mobile-apps/sources/');
    const appSrcTmpPath = path.resolve(__dirname, '../mocha-mobile-apps/tmp/');

    if (this.options.doPrepare) {
      const tmpFilename = `${appSrcTmpPath}/appsrctmp.zip`;
      const sourceStatFilename = `${appSrcTmpPath}/sourcestat`;
      const archFilename = `${appSrcPath}/appsrc.zip`;

      let ignores = ['**/node_modules/mocha-mobile/mocha-mobile-apps/**/*'];
      if (this.options.ignorePattern) {
        ignores = ignores.concat(this.options.ignorePattern.split(','));
      }

      let sourceStat;

      execution = execution.then(() => new Promise((resolve, reject) => {
        console.log('> Prepare app sources');
        glob('**/*', {
          cwd: this.sourcePath, ignore: ignores, follow: true, mark: true,
        },
        (error, files) => {
          if (error) {
            reject(error);
            return;
          }

          const folderStat = files.map(file => [file, fs.statSync(file)]);
          sourceStat = crypto.createHash('sha1')
            .update(JSON.stringify(folderStat)).digest('hex');

          fs.readFile(sourceStatFilename, 'utf8', (err, data) => resolve(err || data !== sourceStat));
        });
      }))
        .then((sourceTouched) => {
          if (sourceTouched) {
            console.log('  Archive sources');
            const archive = archiver('zip');
            const appSrcTmp = fs.createWriteStream(tmpFilename);
            archive.pipe(appSrcTmp);

            return archive
              .glob('**/*', {
                cwd: this.sourcePath, ignore: ignores, follow: true, mark: true,
              })
              .finalize();
          }

          return 'skip';
        })
        .then((result) => {
          if (result === 'skip') {
            console.log('  Source didn`t change, skip app building');
            return result;
          }

          console.log('> Build and install mobile app');
          fs.renameSync(tmpFilename, archFilename);
          return spawnPromise(`../mocha-mobile-apps/${this.options.arch}/prepare-${this.options.arch}-test.sh`,
            [appSrcPath]);
        })
        .then((result) => {
          if (result !== 'skip') {
            fs.writeFile(sourceStatFilename, sourceStat, (err) => {
              if (err) {
                console.log(err);
              }
            });
          }
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
