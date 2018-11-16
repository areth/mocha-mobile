#!/usr/bin/env node

'use strict';

const program = require('commander');
const fs = require('fs');
const MochaMobile = require('./');

program
  .version('0.0.6')
  .usage('--arch <architecture> [options] [mocha params ...]')
  .option('-a, --arch <architecture>', 'Architecture to run test (android|ios)')
  .option('--ignore <pattern>', 'Files pattern to ignore during mobile app assembling `**/build/*`')
  .option('--onlyprep', 'Only prepare test, don\'t run it')
  .option('--onlyrun', 'Only run test, don\'t prepare it (test has to be prepared)');

// treat unknown options as Mocha args
const testArgs = [];
const mochaArgs = [];
const [...args] = process.argv;
let lastArgIsTest = false;
args.forEach((arg, i) => {
  const isOptionArg = arg.charAt(0) !== '-';
  if (i < 2 || program.optionFor(arg) || (lastArgIsTest && isOptionArg)) {
    testArgs.push(arg);
    lastArgIsTest = true;
  } else {
    mochaArgs.push(arg);
    lastArgIsTest = false;
  }
});
program.parse(testArgs);

if (!program.arch) {
  program.help(() => 'error: option `-a, --arch <architecture>` argument missing\n');
}

let ignores = [];
if (program.ignore) {
  ignores = ignores.concat(program.ignore.split(','));
}
const ignoresFile = '.mmignore';
if (fs.existsSync(ignoresFile)) {
  try {
    ignores = ignores.concat(fs.readFileSync(ignoresFile).toString().split('\n')
      .map(line => line.split('#')[0].trim())
      .filter(line => line.length));
  } catch (err) {
    throw new Error(`mocha-mobile failed to read .mmignore: ${err}`);
  }
}

const test = new MochaMobile(process.cwd(), {
  arch: program.arch,
  doPrepare: program.onlyprep || !program.onlyrun,
  doRun: program.onlyrun || !program.onlyprep,
  ignores,
  mochaParams: mochaArgs,
});
test.run()
  .catch((err) => {
    console.log(`mocha-mobile failed: ${err}`);
    process.exitCode = 1;
  });
