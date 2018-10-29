const chai = require('chai');
// const sinon = require('sinon');
// const sinonChai = require('sinon-chai');
// const clone = require('clone');

// chai.use(sinonChai);
chai.should();

describe('basic', () => {
  it('2 + 2 = 4', () => {
    const sum = 2 + 2;
    sum.should.be.equal(4);
  });

  it('2 + 2 = 5', () => {
    const sum = 2 + 2;
    sum.should.be.equal(5);
  });
});
