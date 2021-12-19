#!/usr/bin/env osascript -l JavaScript

// https://developer.apple.com/library/archive/documentation/AppleApplications/Conceptual/CalendarScriptingGuide/Calendar-LocateanEvent.html#//apple_ref/doc/uid/TP40016646-CH95-SW7

const app = Application("Calendar");

const today = new Date(Date.now());
const oneDay = 12 * 60 * 60 * 1000;
const tomorrow = new Date(today + oneDay);

const calendar = app.calendars.byName("Zane");
var events = calendar.events.whose({
  _and: [
    {startDate: { _greaterThan: today}},
    {startDate: { _lessThan: tomorrow}}
  ]
})();

console.log(JSON.stringify(events, null, 2));
