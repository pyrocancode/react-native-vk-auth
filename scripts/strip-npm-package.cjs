'use strict';

const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const pkgPath = path.join(root, 'package.json');
const backupPath = path.join(root, '.package.json.orig');

const original = fs.readFileSync(pkgPath, 'utf8');
fs.writeFileSync(backupPath, original);

const pkg = JSON.parse(original);
const stripKeys = [
  'devDependencies',
  'scripts',
  'jest',
  'commitlint',
  'release-it',
  'eslintConfig',
  'eslintIgnore',
  'prettier',
  'react-native-builder-bob',
];

for (const key of stripKeys) {
  delete pkg[key];
}

fs.writeFileSync(pkgPath, `${JSON.stringify(pkg, null, 2)}\n`);
