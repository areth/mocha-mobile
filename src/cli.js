#!/usr/bin/env node
'use strict';

const program = require('commander');
const MochaMobile = require('./');

program
  .version('0.0.1')
  .usage('--arch <architecture> [options] [mocha params ...]')
  .option('-a, --arch <architecture>', 'Architecture to run test (android|ios)')
  .option('--onlyprep', 'Only prepare test, don\'t run it')
  .option('--onlyrun', 'Only run test, don\'t prepare it (test have to be prepared)');

// treat unknown options as Mocha args
const testArgs = [];
const mochaArgs = [];
const [...args] = process.argv;
let lastArgIsTest = false;
args.forEach((arg, i) => {
  let isOptionArg = arg.charAt(0) !== '-';
  if(i < 2 || program.optionFor(arg) || (lastArgIsTest && isOptionArg)) {
    testArgs.push(arg);
    lastArgIsTest = true;
  } else {
    mochaArgs.push(arg);
    lastArgIsTest = false;
  }
});
program.parse(testArgs);

if(!program.arch) {
  program.help(() => 'error: option `-a, --arch <architecture>` argument missing\n');
}

const test = new MochaMobile(process.cwd(), {
  arch: program.arch,
  doPrepare: program.onlyprep || !program.onlyrun,
  doRun: program.onlyrun || !program.onlyprep,
  mochaParams: mochaArgs,
});
test.run()
  .catch((err) => {
    console.log(`mocha-mobile failed`);
    process.exitCode = 1;
  });
