#!/usr/bin/env osascript -l JavaScript

function run(argv) {
  const exists = Application("System Events")
    .processes.byName(argv[0])
    .menuBars()[0]
    .menuBarItems.byName(argv[1])
    .menus()[0]
    .menuItems.byName(argv[2])
    .exists();
  console.log(exists);
}
