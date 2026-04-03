'use strict';

const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const pkgPath = path.join(root, 'package.json');
const backupPath = path.join(root, '.package.json.orig');

if (!fs.existsSync(backupPath)) {
  process.exit(0);
}

fs.copyFileSync(backupPath, pkgPath);
fs.unlinkSync(backupPath);
