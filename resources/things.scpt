#!/usr/bin/osascript

-- https://culturedcode.com/things/download/Things3AppleScriptGuide.pdf

use framework "Foundation"
use scripting additions

-- https://forum.latenightsw.com/t/writing-json-data-with-nsjsonserialization/1130
property NSJSONWritingPrettyPrinted : a reference to 1
property NSJSONSerialization : a reference to current application's NSJSONSerialization
property NSOutputStream : a reference to current application's NSOutputStream

tell application "Things3"
    set |todos| to to dos of list "Today"

    set |records| to {}
    repeat with todo in |todos|
        set |record| to {|id|: id of todo, |name|: name of todo, |status|: status of todo as string}
        if due date of todo is not missing value
            set |record| to |record| & {|due|: my formatDate(due date of todo)}
        end
        if completion date of todo is not missing value
            set |record| to |record| & {|completion|: my formatDate(completion date of todo)}
        end
        set end of |records| to |record|
    end repeat

    set |json| to NSJSONSerialization's dataWithJSONObject:|records| options:NSJSONWritingPrettyPrinted |error|:(missing value)
    return (current application's NSString's alloc()'s initWithData:|json| encoding:(current application's NSUTF8StringEncoding)) as text
end tell

-- https://forum.latenightsw.com/t/formatting-dates/841
on formatDate(aDate)
	set theFormatter to current application's NSDateFormatter's new()
	theFormatter's setLocale:(current application's NSLocale's localeWithLocaleIdentifier:"en_US_POSIX")
	theFormatter's setDateFormat:"yyyy'-'MM'-'dd'"
	return (theFormatter's stringFromDate:aDate) as text
end formatDate:
