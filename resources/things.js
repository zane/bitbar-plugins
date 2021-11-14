#!/usr/bin/env osascript -l JavaScript

const things = Application("Things");
const todos = things.lists.byName("Today").toDos();
const toObj = (todo) => {
  const entries = ["id", "name", "dueDate", "status"]
    .map(s => [s, todo[s]()])
    .filter(([_, v]) => !(v === null || v === undefined));
  return Object.fromEntries(entries);
};
const objs = todos.map(toObj);
console.log(JSON.stringify(objs, null, 2));
